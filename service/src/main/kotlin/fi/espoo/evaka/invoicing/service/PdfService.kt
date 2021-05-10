// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import com.lowagie.text.pdf.BaseFont
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.invoicing.domain.FeeDecisionDetailed
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.MailAddress
import fi.espoo.evaka.invoicing.domain.PlacementType
import fi.espoo.evaka.invoicing.domain.ServiceNeed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionDetailed
import org.springframework.stereotype.Component
import org.thymeleaf.ITemplateEngine
import org.thymeleaf.context.Context
import org.xhtmlrenderer.pdf.ITextRenderer
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Paths
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class Template(val value: String)
class Page(val template: Template, val context: Context)

data class FeeDecisionPdfData(
    val decision: FeeDecisionDetailed,
    val lang: String
)

data class VoucherValueDecisionPdfData(
    val decision: VoucherValueDecisionDetailed,
    val lang: String
)

fun BigDecimal.toDecimalString(): String = this.toString().replace('.', ',')

fun formatCents(amountInCents: Int?): String? =
    if (amountInCents != null) BigDecimal(amountInCents).divide(
        BigDecimal(100),
        2,
        RoundingMode.HALF_UP
    ).toDecimalString() else null

fun dateFmt(date: LocalDate?): String = date?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?: ""

fun instantFmt(instant: Instant?): String =
    instant?.atZone(ZoneId.of("Europe/Helsinki"))?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?: ""

@Component
class PDFService(
    private val templateEngine: ITemplateEngine
) {
    fun processPage(page: Page): String = templateEngine.process(page.template.value, page.context)

    fun render(page: Page): ByteArray {
        val os = ByteArrayOutputStream()

        renderHtmlPages(processPage(page), os)
        return os.toByteArray()
    }

    fun renderHtml(page: String): ByteArray {
        val os = ByteArrayOutputStream()
        renderHtmlPages(page, os)
        return os.toByteArray()
    }

    private fun renderHtmlPages(pages: String, os: OutputStream) {
        val textRenderer = ITextRenderer()

        val res = javaClass.classLoader.getResource("ttf/OpenSans-Regular.ttf")
        val file: File = Paths.get(res.toURI()).toFile()
        val url: String = file.absolutePath

        val boldRes = javaClass.classLoader.getResource("ttf/OpenSans-Bold.ttf")
        val boldFile: File = Paths.get(boldRes.toURI()).toFile()
        val boldUrl: String = boldFile.absolutePath

        textRenderer.setDocumentFromString(pages)
        textRenderer.fontResolver.addFont(url, BaseFont.IDENTITY_H, true)
        textRenderer.fontResolver.addFont(boldUrl, BaseFont.IDENTITY_H, true)
        textRenderer.layout()
        textRenderer.createPDF(os, false)
        textRenderer.finishPDF()
    }

    fun generateFeeDecisionPdf(data: FeeDecisionPdfData): ByteArray {

        val page = Page(Template("fee-decision/decision"), createFeeDecisionPdfContext(data))

        return render(page)
    }

    fun generateVoucherValueDecisionPdf(data: VoucherValueDecisionPdfData): ByteArray {

        val page = Page(Template("fee-decision/voucher-value-decision"), createVoucherValueDecisionPdfContext(data))

        return render(page)
    }

    private fun createVoucherValueDecisionPdfContext(data: VoucherValueDecisionPdfData): Context {
        return Context().apply {
            locale = Locale.Builder().setLanguage(data.lang).build()
            setVariables(getVoucherValueDecisionPdfVariables(data))
        }
    }

    private fun getVoucherValueDecisionPdfVariables(data: VoucherValueDecisionPdfData): Map<String, Any?> {
        val (decision, lang) = data

        val totalIncome = listOfNotNull(decision.headOfFamilyIncome?.total, decision.partnerIncome?.total).sum()
        val hideTotalIncome =
            (decision.headOfFamilyIncome == null || decision.headOfFamilyIncome.effect != IncomeEffect.INCOME) ||
                (decision.partnerIncome != null && decision.partnerIncome.effect != IncomeEffect.INCOME)

        val sendAddress = MailAddress.fromPerson(decision.headOfFamily, lang)
        return mapOf(
            "child" to decision.child,
            "approvedAt" to instantFmt(decision.approvedAt),
            "validFrom" to decision.validFrom,
            "placementUnit" to decision.placementUnit,
            "placement" to decision.placement,
            "familySize" to decision.familySize,
            "value" to formatCents(decision.value),
            "serviceNeed" to decision.placement.serviceNeed,
            "headIncomeTotal" to formatCents(decision.headOfFamilyIncome?.total),
            "headIncomeEffect" to (decision.headOfFamilyIncome?.effect?.name ?: IncomeEffect.NOT_AVAILABLE.name),
            "hasPartner" to (decision.partner != null),
            "partner" to decision.partner,
            "partnerFullName" to decision.partner?.let { "${it.firstName} ${it.lastName}" },
            "partnerIncomeEffect" to (decision.partnerIncome?.effect?.name ?: IncomeEffect.NOT_AVAILABLE.name),
            "partnerIncomeTotal" to formatCents(decision.partnerIncome?.total),
            "totalIncome" to formatCents(totalIncome),
            "showTotalIncome" to !hideTotalIncome,
            "coPayment" to formatCents(decision.coPayment),
            "decisionNumber" to decision.decisionNumber,
            "sendAddress" to sendAddress,
            "headFullName" to with(decision.headOfFamily) { "$firstName $lastName" },
            "serviceProviderValue" to formatCents(decision.value - (decision.coPayment + decision.feeAlterations.sumBy { it.effect })),

            "approverFirstName" to (
                decision.financeDecisionHandlerName?.split(" ")?.get(0)
                    ?: decision.approvedBy?.firstName
                ),
            "approverLastName" to (
                decision.financeDecisionHandlerName?.split(" ")?.get(1)
                    ?: decision.approvedBy?.lastName
                )

        )
    }

    private fun createFeeDecisionPdfContext(
        data: FeeDecisionPdfData

    ): Context {
        return Context().apply {
            locale = Locale.Builder().setLanguage(data.lang).build()
            setVariables(getFeeDecisionPdfVariables(data))
        }
    }

    fun getFeeDecisionPdfVariables(data: FeeDecisionPdfData): Map<String, Any?> {
        data class FeeDecisionPdfPart(
            val childName: String,
            val placementType: PlacementType,
            val serviceNeed: ServiceNeed,
            val feeAlterations: List<Pair<FeeAlteration.Type, String>>,
            val finalFeeFormatted: String,
            val feeFormatted: String
        )

        val (decision, lang) = data

        val totalIncome = listOfNotNull(decision.headOfFamilyIncome?.total, decision.partnerIncome?.total).sum()

        val sendAddress = MailAddress.fromPerson(decision.headOfFamily, lang)

        val hideTotalIncome =
            (decision.headOfFamilyIncome == null || decision.headOfFamilyIncome.effect != IncomeEffect.INCOME) ||
                (decision.partnerIncome != null && decision.partnerIncome.effect != IncomeEffect.INCOME)

        return mapOf(
            "approvedAt" to instantFmt(decision.approvedAt),
            "decisionNumber" to decision.decisionNumber,
            "isReliefDecision" to (decision.decisionType !== FeeDecisionType.NORMAL),
            "decisionType" to decision.decisionType.toString(),
            "hasPartner" to (decision.partner != null),
            "hasPoBox" to (sendAddress.poBox != null),
            "headFullName" to with(decision.headOfFamily) { "$firstName $lastName" },
            "headIncomeEffect" to (decision.headOfFamilyIncome?.effect?.name ?: IncomeEffect.NOT_AVAILABLE.name),
            "headIncomeTotal" to formatCents(decision.headOfFamilyIncome?.total),
            "partnerFullName" to decision.partner?.let { "${it.firstName} ${it.lastName}" },
            "partnerIncomeEffect" to (decision.partnerIncome?.effect?.name ?: IncomeEffect.NOT_AVAILABLE.name),
            "partnerIncomeTotal" to formatCents(decision.partnerIncome?.total),
            "parts" to decision.parts.map {
                FeeDecisionPdfPart(
                    "${it.child.firstName} ${it.child.lastName}",
                    it.placement.type,
                    it.placement.serviceNeed,
                    it.feeAlterations.map { fa -> fa.type to formatCents(fa.effect)!! },
                    formatCents(it.finalFee())!!,
                    formatCents(it.fee)!!
                )
            },
            "sendAddress" to sendAddress,
            "totalFee" to formatCents(decision.totalFee()),
            "totalIncome" to formatCents(totalIncome),
            "showTotalIncome" to !hideTotalIncome,
            "validFor" to with(decision) { "${dateFmt(validFrom)} - ${dateFmt(validTo)}" },
            "validFrom" to dateFmt(decision.validFrom),
            "feePercent" to decision.feePercent().toDecimalString(),
            "pricingMinThreshold" to formatCents(-1 * decision.minThreshold()),
            "familySize" to decision.familySize,
            "showValidTo" to (decision.validTo?.isBefore(LocalDate.now()) ?: false),
            "approverFirstName" to (
                decision.financeDecisionHandlerName?.split(" ")?.get(0)
                    ?: decision.approvedBy?.firstName
                ),
            "approverLastName" to (
                decision.financeDecisionHandlerName?.split(" ")?.get(1)
                    ?: decision.approvedBy?.lastName
                ),
        ).mapValues {
            it.value ?: ""
        }
    }
}
