// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import com.fasterxml.jackson.databind.ObjectMapper
import fi.espoo.evaka.invoicing.client.S3DocumentClient
import fi.espoo.evaka.invoicing.data.getVoucherValueDecision
import fi.espoo.evaka.invoicing.data.getVoucherValueDecisionDocumentKey
import fi.espoo.evaka.invoicing.data.updateVoucherValueDecisionDocumentKey
import fi.espoo.evaka.invoicing.data.updateVoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionDetailed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionPartDetailed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.message.IEvakaMessageClient
import fi.espoo.evaka.shared.message.SuomiFiMessage
import org.jdbi.v3.core.Handle
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.UUID

@Component
class VoucherValueDecisionService(
    private val objectMapper: ObjectMapper,
    private val pdfService: PDFService,
    private val s3Client: S3DocumentClient,
    private val messageClient: IEvakaMessageClient,
    env: Environment
) {
    private val bucket = env.getRequiredProperty("fi.espoo.voltti.document.bucket.vouchervaluedecision")

    fun createDecisionPdf(h: Handle, decisionId: UUID) {
        val decision = getDecision(h, decisionId)
        check(decision.documentKey.isNullOrBlank()) { "Voucher value decision $decisionId has document key already!" }

        val pdf = generatePdf(decision)
        val key = uploadPdf(decision.id, pdf)
        h.updateVoucherValueDecisionDocumentKey(decision.id, key)
    }

    fun getDecisionPdf(h: Handle, decisionId: UUID): Pair<String, ByteArray> {
        val key = h.getVoucherValueDecisionDocumentKey(decisionId)
            ?: throw NotFound("No voucher value decision found with ID ($decisionId)")
        return key to s3Client.getPdf(bucket, key)
    }

    fun sendDecision(h: Handle, decisionId: UUID) {
        val decision = getDecision(h, decisionId)
        check(decision.status == VoucherValueDecisionStatus.WAITING_FOR_SENDING) {
            "Cannot send voucher value decision ${decision.id} - has status ${decision.status}"
        }
        checkNotNull(decision.documentKey) {
            "Cannot send fee decision ${decision.id} - missing document key"
        }

        if (decision.requiresManualSending()) {
            h.updateVoucherValueDecisionStatus(decision.id, VoucherValueDecisionStatus.WAITING_FOR_MANUAL_SENDING)
            return
        }

        val lang = if (decision.headOfFamily.language == "sv") "sv" else "fi"
        val documentDisplayName = suomiFiDocumentFileName(lang)
        val messageHeader = suomiFiMessageHeader(lang)
        val messageContent = suomiFiMessageContent(lang)
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

        h.updateVoucherValueDecisionStatus(decision.id, VoucherValueDecisionStatus.SENT)
    }

    private fun getDecision(h: Handle, decisionId: UUID): VoucherValueDecisionDetailed =
        h.getVoucherValueDecision(objectMapper, decisionId)
            ?: error("No voucher value decision found with ID ($decisionId)")

    private val key = { id: UUID -> "value_decision_$id.pdf" }
    private fun uploadPdf(decisionId: UUID, file: ByteArray): String {
        val key = key(decisionId)
        s3Client.uploadPdfToS3(bucket, key, file)
        return key
    }

    private fun generatePdf(decision: VoucherValueDecisionDetailed): ByteArray {
        val pages = listOf(coverPage(decision))
        return pdfService.renderHtml(pages)
    }
}

private fun suomiFiDocumentFileName(lang: String) =
    if (lang == "sv") "Beslut_om_avgift_för_småbarnspedagogik.pdf"
    else "Varhaiskasvatuksen_maksupäätös.pdf"

private fun suomiFiMessageHeader(lang: String) =
    if (lang == "sv") "Beslut gällande Esbos småbarnspedagogik"
    else "Espoon varhaiskasvatukseen liittyvät päätökset"

private fun suomiFiMessageContent(lang: String) =
    if (lang == "sv") """
Klientavgifterna för kommunal småbarnspedagogik varierar enligt familjens storlek och inkomster samt tiden för småbarnspedagogiken. Vårdnadshavarna får ett skriftligt beslut om avgifterna för småbarnspedagogik. Avgifterna faktureras i mitten av den månad som följer på den månad då servicen getts.

Klientavgiften för småbarnspedagogik gäller tills vidare och familjen är skyldig att meddela om familjens inkomster väsentligt förändras (+/- 10 %). Eftersom du har tagit Suomi.fi-tjänsten i bruk, kan du läsa beslutet i bilagorna nedan.
"""
    else """
Kunnallisen varhaiskasvatuksen asiakasmaksut vaihtelevat perheen koon ja tulojen sekä varhaiskasvatusajan mukaan. Huoltajat saavat varhaiskasvatuksen maksuista kirjallisen päätöksen. Maksut laskutetaan palvelun antamisesta seuraavan kuukauden puolivälissä.

Varhaiskasvatuksen asiakasmaksu on voimassa toistaiseksi ja perheellä on velvollisuus ilmoittaa, mikäli perheen tulot olennaisesti muuttuvat (+/- 10 %). Koska olette ottanut Suomi.fi -palvelun käyttöönne, on päätös luettavissa alla olevista liitteistä.

In English:

The client fees for municipal early childhood education vary according to family size, income and the number of hours the child spends attending early childhood education. The City of Espoo sends the guardians a written decision on early childhood education fees. The fees are invoiced in the middle of the month following the provision of the service.

The early childhood education fee will remain in force until further notice. Your family has an obligation to notify the City of Espoo if the family’s income changes substantially (+/– 10%). As you are a user of Suomi.fi, you can find the decision in the attachments below.
"""

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
                ${decision.parts.joinToString(transform = ::tablePart)}
            </table>
        </div>
    </body>
</html>
    """.trimIndent()
}

private fun tablePart(part: VoucherValueDecisionPartDetailed): String {
    // language=html
    return """
        <tr>
            <td colspan="2">${part.child.firstName} ${part.child.lastName}</td>
        </tr>
        <tr>
            <td>Yksikkö</td>
            <td>${part.placementUnit.name}</td>
        </tr>
        <tr>
            <td>Palveluntarve</td>
            <td>${part.placement.hours ?: "ei asetettu"}</td>
        </tr>
        <tr>
            <td>Palvelusetelin enimmäisarvo</td>
            <td>${part.value / 100},${part.value % 100} €</td>
        </tr>
        <tr>
            <td>Omavastuu</td>
            <td>${part.coPayment / 100},${part.coPayment % 100} €</td>
        </tr>
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
