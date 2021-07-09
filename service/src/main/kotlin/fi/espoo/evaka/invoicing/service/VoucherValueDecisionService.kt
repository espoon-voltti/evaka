// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.BucketEnv
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
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.message.IEvakaMessageClient
import fi.espoo.evaka.shared.message.IMessageProvider
import fi.espoo.evaka.shared.message.SuomiFiMessage
import fi.espoo.evaka.shared.message.langWithDefault
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate

@Component
class VoucherValueDecisionService(
    private val pdfService: PDFService,
    private val s3Client: S3DocumentClient,
    private val messageProvider: IMessageProvider,
    private val messageClient: IEvakaMessageClient,
    env: BucketEnv
) {
    private val bucket = env.voucherValueDecisions

    fun createDecisionPdf(tx: Database.Transaction, decisionId: VoucherValueDecisionId) {
        val decision = getDecision(tx, decisionId)
        check(decision.documentKey.isNullOrBlank()) { "Voucher value decision $decisionId has document key already!" }

        val pdf = generatePdf(decision)
        val key = uploadPdf(decision.id, pdf)
        tx.updateVoucherValueDecisionDocumentKey(decision.id, key)
    }

    fun getDecisionPdf(tx: Database.Read, decisionId: VoucherValueDecisionId): Pair<String, ByteArray> {
        val key = tx.getVoucherValueDecisionDocumentKey(decisionId)
            ?: throw NotFound("No voucher value decision found with ID ($decisionId)")
        return key to s3Client.getPdf(bucket, key)
    }

    fun sendDecision(tx: Database.Transaction, decisionId: VoucherValueDecisionId) {
        val decision = getDecision(tx, decisionId)
        check(decision.status == VoucherValueDecisionStatus.WAITING_FOR_SENDING) {
            "Cannot send voucher value decision ${decision.id} - has status ${decision.status}"
        }
        checkNotNull(decision.documentKey) {
            "Cannot send voucher value decision ${decision.id} - missing document key"
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
        messageClient.send(
            SuomiFiMessage(
                messageId = decision.id.toString(),
                documentId = decision.id.toString(),
                documentDisplayName = documentDisplayName,
                documentBucket = bucket,
                documentKey = decision.documentKey,
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
    WHERE placement.child_id = decision.child_id
    AND daycare.provider_type = 'PRIVATE_SERVICE_VOUCHER'::unit_provider_type
    AND daterange(decision.valid_from, decision.valid_to, '[]') && daterange(placement.start_date, placement.end_date, '[]')
) placements ON true
WHERE decision.status = 'SENT'::voucher_value_decision_status
AND daterange(decision.valid_from, decision.valid_to, '[]') != placements.combined_range
AND placements.combined_range << daterange(:now, null)
"""
        ).bind("now", now).mapTo<VoucherValueDecisionId>().toList()

        tx.lockValueDecisions(decisionIds)

        tx
            .getValueDecisionsByIds(decisionIds)
            .forEach { decision ->
                val mergedPlacementPeriods = tx
                    .createQuery(
                        """
SELECT daterange(start_date, end_date, '[]')
FROM placement
WHERE child_id = :childId AND unit_id = :unitId AND :dateRange && daterange(start_date, end_date, '[]')
ORDER BY start_date ASC
"""
                    )
                    .bind("childId", decision.child.id)
                    .bind("unitId", decision.placement.unit.id)
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

    private fun getDecision(tx: Database.Read, decisionId: VoucherValueDecisionId): VoucherValueDecisionDetailed =
        tx.getVoucherValueDecision(decisionId)
            ?: error("No voucher value decision found with ID ($decisionId)")

    private val key = { id: VoucherValueDecisionId -> "value_decision_$id.pdf" }
    private fun uploadPdf(decisionId: VoucherValueDecisionId, file: ByteArray): String {
        val key = key(decisionId)
        s3Client.uploadPdfToS3(bucket, key, file)
        return key
    }

    private fun generatePdf(decision: VoucherValueDecisionDetailed): ByteArray {
        val lang = if (decision.headOfFamily.language == "sv") DocumentLang.sv else DocumentLang.fi
        return pdfService.generateVoucherValueDecisionPdf(VoucherValueDecisionPdfData(decision, lang))
    }
}

private fun suomiFiDocumentFileName(lang: String) =
    if (lang == "sv") "Beslut_om_avgift_för_småbarnspedagogik.pdf"
    else "Varhaiskasvatuksen_maksupäätös.pdf"
