// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import com.fasterxml.jackson.databind.ObjectMapper
import fi.espoo.evaka.invoicing.client.S3DocumentClient
import fi.espoo.evaka.invoicing.data.getValueDecisionsByIds
import fi.espoo.evaka.invoicing.data.getVoucherValueDecision
import fi.espoo.evaka.invoicing.data.getVoucherValueDecisionDocumentKey
import fi.espoo.evaka.invoicing.data.lockValueDecisions
import fi.espoo.evaka.invoicing.data.markVoucherValueDecisionsSent
import fi.espoo.evaka.invoicing.data.updateVoucherValueDecisionDocumentKey
import fi.espoo.evaka.invoicing.data.updateVoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.data.updateVoucherValueDecisionStatusAndDates
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionDetailed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.message.IEvakaMessageClient
import fi.espoo.evaka.shared.message.IMessageProvider
import fi.espoo.evaka.shared.message.SuomiFiMessage
import fi.espoo.evaka.shared.message.langWithDefault
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.UUID

@Component
class VoucherValueDecisionService(
    private val objectMapper: ObjectMapper,
    private val pdfService: PDFService,
    private val s3Client: S3DocumentClient,
    private val messageProvider: IMessageProvider,
    private val messageClient: IEvakaMessageClient,
    env: Environment
) {
    private val bucket = env.getRequiredProperty("fi.espoo.voltti.document.bucket.vouchervaluedecision")

    fun createDecisionPdf(tx: Database.Transaction, decisionId: UUID) {
        val decision = getDecision(tx, decisionId)
        check(decision.documentKey.isNullOrBlank()) { "Voucher value decision $decisionId has document key already!" }

        val pdf = generatePdf(decision)
        val key = uploadPdf(decision.id, pdf)
        tx.handle.updateVoucherValueDecisionDocumentKey(decision.id, key)
    }

    fun getDecisionPdf(tx: Database.Read, decisionId: UUID): Pair<String, ByteArray> {
        val key = tx.handle.getVoucherValueDecisionDocumentKey(decisionId)
            ?: throw NotFound("No voucher value decision found with ID ($decisionId)")
        return key to s3Client.getPdf(bucket, key)
    }

    fun sendDecision(tx: Database.Transaction, decisionId: UUID) {
        val decision = getDecision(tx, decisionId)
        check(decision.status == VoucherValueDecisionStatus.WAITING_FOR_SENDING) {
            "Cannot send voucher value decision ${decision.id} - has status ${decision.status}"
        }
        checkNotNull(decision.documentKey) {
            "Cannot send fee decision ${decision.id} - missing document key"
        }

        if (decision.requiresManualSending()) {
            tx.updateVoucherValueDecisionStatus(
                listOf(decision.id),
                VoucherValueDecisionStatus.WAITING_FOR_MANUAL_SENDING
            )
            return
        }

        val lang = if (decision.headOfFamily.language == "sv") "sv" else "fi"
        val documentDisplayName = suomiFiDocumentFileName(lang)
        val messageHeader = messageProvider.getVoucherValueDecisionHeader(langWithDefault(lang))
        val messageContent = messageProvider.getVoucherValueDecisionContent(langWithDefault(lang))
        val documentUri = s3Client.getDocumentUri(bucket, decision.documentKey)
        messageClient.send(
            SuomiFiMessage(
                messageId = decision.id.toString(),
                documentId = decision.id.toString(),
                documentDisplayName = documentDisplayName,
                documentUri = documentUri,
                language = lang,
                firstName = decision.headOfFamily.firstName,
                lastName = decision.headOfFamily.lastName,
                streetAddress = decision.headOfFamily.streetAddress!!,
                postalCode = decision.headOfFamily.postalCode!!,
                postOffice = decision.headOfFamily.postOffice!!,
                ssn = decision.headOfFamily.ssn!!,
                messageHeader = messageHeader,
                messageContent = messageContent
            )
        )

        tx.markVoucherValueDecisionsSent(listOf(decision.id), Instant.now())
    }

    fun endDecisionsWithEndedPlacements(tx: Database.Transaction, now: LocalDate) {
        val decisionIds = tx.createQuery(
            """
SELECT id FROM voucher_value_decision decision
LEFT JOIN LATERAL (
    SELECT range_merge(daterange(placement.start_date, placement.end_date, '[]')) combined_range
    FROM placement
    JOIN daycare ON daycare.id = placement.unit_id
    WHERE placement.child_id = decision.child
    AND daycare.provider_type = 'PRIVATE_SERVICE_VOUCHER'::unit_provider_type
    AND daterange(decision.valid_from, decision.valid_to, '[]') && daterange(placement.start_date, placement.end_date, '[]')
) placements ON true
WHERE decision.status = 'SENT'::voucher_value_decision_status
AND daterange(decision.valid_from, decision.valid_to, '[]') != placements.combined_range
AND placements.combined_range << daterange(:now, null)
"""
        ).bind("now", now).mapTo<UUID>().toList()

        tx.handle.lockValueDecisions(decisionIds)

        tx.handle
            .getValueDecisionsByIds(objectMapper, decisionIds)
            .forEach { decision ->
                val mergedPlacementPeriods = tx
                    .createQuery(
                        """
SELECT start_date AS start, end_date AS end
FROM placement
WHERE child_id = :childId AND unit_id = :unitId AND :dateRange && daterange(start_date, end_date, '[]')
ORDER BY start_date ASC
"""
                    )
                    .bind("childId", decision.child.id)
                    .bind("unitId", decision.placement.unit)
                    .bind("dateRange", DateRange(decision.validFrom, decision.validTo))
                    .mapTo<FiniteDateRange>()
                    .fold(listOf<FiniteDateRange>()) { periods, period ->
                        when {
                            periods.isEmpty() -> listOf(period)
                            periods.last().end.plusDays(1) == period.start ->
                                periods.dropLast(1) + periods.last().copy(end = period.end)
                            else -> periods + period
                        }
                    }

                when {
                    mergedPlacementPeriods.isEmpty() -> {
                        val annulled = decision.annul()
                        tx.updateVoucherValueDecisionStatusAndDates(listOf(annulled))
                    }
                    mergedPlacementPeriods.first().end < decision.validTo -> {
                        val withUpdatedEndDate = decision.copy(validTo = mergedPlacementPeriods.first().end)
                        tx.updateVoucherValueDecisionStatusAndDates(listOf(withUpdatedEndDate))
                    }
                }
            }
    }

    private fun getDecision(tx: Database.Read, decisionId: UUID): VoucherValueDecisionDetailed =
        tx.handle.getVoucherValueDecision(objectMapper, decisionId)
            ?: error("No voucher value decision found with ID ($decisionId)")

    private val key = { id: UUID -> "value_decision_$id.pdf" }
    private fun uploadPdf(decisionId: UUID, file: ByteArray): String {
        val key = key(decisionId)
        s3Client.uploadPdfToS3(bucket, key, file)
        return key
    }

    private fun generatePdf(decision: VoucherValueDecisionDetailed): ByteArray {
        val lang = if (decision.headOfFamily.language == "sv") "sv" else "fi"
        return pdfService.generateVoucherValueDecisionPdf(VoucherValueDecisionPdfData(decision, lang))
    }
}

private fun suomiFiDocumentFileName(lang: String) =
    if (lang == "sv") "Beslut_om_avgift_för_småbarnspedagogik.pdf"
    else "Varhaiskasvatuksen_maksupäätös.pdf"

private fun coverPage(decision: VoucherValueDecisionDetailed): String {
    // language=html
    return """
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        $styles
    </head>
    <body class="cover-page">
        ${coverPageHeader(decision)}
        <div class="content">
            <h1>PÄÄTÖS VARHAISKASVATUKSEN PALVELUSETELIN ARVOSTA</h1>
            <table>
                <tr>
                    <td colspan="2">${decision.child.firstName} ${decision.child.lastName}</td>
                </tr>
                <tr>
                    <td>Yksikkö</td>
                    <td>${decision.placementUnit.name}</td>
                </tr>
                <tr>
                    <td>Palveluntarve</td>
                    <td>${decision.placement.hours ?: "ei asetettu"}</td>
                </tr>
                <tr>
                    <td>Palvelusetelin enimmäisarvo</td>
                    <td>${decision.value / 100},${decision.value % 100} €</td>
                </tr>
                <tr>
                    <td>Omavastuu</td>
                    <td>${decision.coPayment / 100},${decision.coPayment % 100} €</td>
                </tr>
            </table>
        </div>
    </body>
</html>
    """.trimIndent()
}

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val formatInstant = { value: Instant -> dateFormatter.format(value.atZone(ZoneId.of("Europe/Helsinki"))) }

private fun coverPageHeader(decision: VoucherValueDecisionDetailed): String {
    val encodedLogo = VoucherValueDecisionService::class.java.getResourceAsStream("/static/espoo-logo.png")
        .readBytes()
        .let { bytes -> Base64.getEncoder().encodeToString(bytes) }

    // language=html
    return """
<header>
    <div class="address-window">
        <div class="logo"><img alt="Espoo logo" src="data:image/png;base64,$encodedLogo" /></div>
        <div class="address">
            <div>${decision.headOfFamily.firstName} ${decision.headOfFamily.lastName}</div>
            <div>${decision.headOfFamily.streetAddress}</div>
            <div>${decision.headOfFamily.postalCode} ${decision.headOfFamily.postOffice}</div>
        </div>
    </div>
    <div class="heading">
        <div class="left">
            <div class="detail">PÄÄTÖS varhaiskasvatuksen</div>
            <div class="detail">palvelusetelin arvosta</div>
            <div class="detail"></div>
            <div class="detail">Päätöspvm ${formatInstant(decision.approvedAt!!)}</div>
        </div>
        <div class="right">
            <div class="detail">Sivu 1</div>
            <div class="detail">Päätös nro</div>
            <div class="detail">${decision.decisionNumber}</div>
        </div>
    </div>
</header>
    """.trimIndent()
}

// language=html
private val styles =
    """
<style>
    * {
        font-family: sans-serif;
        font-size: 14px;
        line-height: 1.4;
    }

    @page {
        size: A4 portrait;
        margin: 5mm 7mm;
    }

    body {
        width: 200mm;
        margin: 0;
    }

    header .address-window {
        display: inline-block;
        vertical-align: top;
        margin-left: 14mm;
        width: 80mm;
    }

    header .address-window .logo {
        margin-top: 6mm;
        height: 18mm;
        width: 80mm;
    }

    header .address-window .logo img {
        height: 18mm;
    }

    header .address-window .address {
        margin-top: 9.6mm;
        height: 20mm;
        width: 66mm;
        overflow: hidden;
        line-height: 1.1mm;
    }

    header .address-window .address * {
        line-height: 1.1;
    }

    header .heading {
        display: inline-block;
        vertical-align: top;
        margin-left: 8mm;
    }

    header .heading .left {
        display: inline-block;
        vertical-align: top;
        width: 68mm;
    }

    header .heading .right {
        display: inline-block;
        vertical-align: top;
        width: 23mm;
        text-align: right;
    }

    .content {
        // margin: 0 5mm;
    }

    .cover-page .content {
        margin-top: 30mm;
    }
</style>
    """.trimIndent()
