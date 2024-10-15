// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pdfgen

import com.lowagie.text.pdf.BaseFont
import fi.espoo.evaka.invoicing.domain.FeeAlterationType
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionType
import fi.espoo.evaka.invoicing.service.FeeDecisionPdfData
import fi.espoo.evaka.invoicing.service.VoucherValueDecisionPdfData
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.setting.SettingType
import fi.espoo.evaka.shared.domain.OfficialLanguage
import fi.espoo.evaka.shared.domain.europeHelsinki
import fi.espoo.evaka.shared.message.IMessageProvider
import fi.espoo.evaka.shared.noopTracer
import fi.espoo.evaka.shared.template.ITemplateProvider
import fi.espoo.evaka.shared.withSpan
import io.opentelemetry.api.trace.Tracer
import java.io.ByteArrayOutputStream
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import org.springframework.stereotype.Component
import org.thymeleaf.ITemplateEngine
import org.thymeleaf.context.Context
import org.xhtmlrenderer.pdf.ITextFontResolver
import org.xhtmlrenderer.pdf.ITextRenderer

class Template(val name: String)

class Page(val template: Template, val context: Context)

@Component
class PdfGenerator(
    private val messageProvider: IMessageProvider,
    private val templateProvider: ITemplateProvider,
    private val templateEngine: ITemplateEngine,
    private val tracer: Tracer = noopTracer(),
) {
    fun render(page: Page): ByteArray =
        tracer.withSpan("render pdf ${page.template.name}") {
            val html =
                tracer.withSpan("process") {
                    templateEngine.process(page.template.name, page.context)
                }

            render(html)
        }

    fun render(html: String): ByteArray {
        val output = ByteArrayOutputStream()
        tracer.withSpan("render html") {
            with(ITextRenderer()) {
                fontResolver.addFontDirectory(getResourceFile("ttf"), BaseFont.IDENTITY_H, true)
                setDocumentFromString(html)
                layout()
                createPDF(output, true)
            }
        }
        return output.toByteArray()
    }

    fun generateFeeDecisionPdf(data: FeeDecisionPdfData): ByteArray {
        val template = Template(templateProvider.getFeeDecisionPath())
        val page = Page(template, createFeeDecisionPdfContext(data))

        return render(page)
    }

    fun generateVoucherValueDecisionPdf(data: VoucherValueDecisionPdfData): ByteArray {
        val template = Template(templateProvider.getVoucherValueDecisionPath())
        val page = Page(template, createVoucherValueDecisionPdfContext(data))

        return render(page)
    }

    private fun createVoucherValueDecisionPdfContext(data: VoucherValueDecisionPdfData): Context {
        return Context().apply {
            locale = data.lang.isoLanguage.toLocale()
            setVariables(getVoucherValueDecisionPdfVariables(data))
        }
    }

    private data class FeeAlterationPdfPart(
        val type: FeeAlterationType,
        val amount: Int,
        val isAbsolute: Boolean,
        val effectFormatted: String,
    )

    private fun getVoucherValueDecisionPdfVariables(
        data: VoucherValueDecisionPdfData
    ): Map<String, Any?> {
        val (decision, settings, lang) = data

        val totalIncome =
            listOfNotNull(
                    decision.headOfFamilyIncome?.total,
                    decision.partnerIncome?.total,
                    decision.childIncome?.total,
                )
                .sum()
        val hideTotalIncome =
            (decision.headOfFamilyIncome == null ||
                decision.headOfFamilyIncome.effect != IncomeEffect.INCOME) ||
                (decision.partnerIncome != null &&
                    decision.partnerIncome.effect != IncomeEffect.INCOME)

        val isReliefDecision = decision.decisionType != VoucherValueDecisionType.NORMAL

        val hasChildIncome = decision.childIncome != null && decision.childIncome.total > 0

        return mapOf(
            "child" to decision.child,
            "approvedAt" to dateFmt(decision.approvedAt?.toLocalDate()),
            "validFrom" to dateFmt(decision.validFrom),
            "validTo" to dateFmt(decision.validTo),
            "placementUnit" to decision.placement.unit,
            "placementType" to decision.placement.type,
            "familySize" to decision.familySize,
            "value" to formatCents(decision.voucherValue),
            "voucherValueDescription" to
                when (lang) {
                    OfficialLanguage.FI -> decision.serviceNeed.voucherValueDescriptionFi
                    OfficialLanguage.SV -> decision.serviceNeed.voucherValueDescriptionSv
                },
            "headIncomeTotal" to formatCents(decision.headOfFamilyIncome?.total),
            "headIncomeEffect" to
                (decision.headOfFamilyIncome?.effect?.name ?: IncomeEffect.NOT_AVAILABLE.name),
            "hasPartner" to (decision.partner != null),
            "partner" to decision.partner,
            "partnerFullName" to decision.partner?.let { "${it.firstName} ${it.lastName}" },
            "partnerIsCodebtor" to (decision.partner != null && decision.partnerIsCodebtor == true),
            "partnerIncomeEffect" to
                (decision.partnerIncome?.effect?.name ?: IncomeEffect.NOT_AVAILABLE.name),
            "partnerIncomeTotal" to formatCents(decision.partnerIncome?.total),
            "totalIncome" to formatCents(totalIncome),
            "showTotalIncome" to !hideTotalIncome,
            "feeAlterations" to
                decision.feeAlterations.map { fa ->
                    FeeAlterationPdfPart(
                        fa.type,
                        fa.amount,
                        fa.isAbsolute,
                        formatCents(abs(fa.effect))!!,
                    )
                },
            "coPayment" to formatCents(decision.finalCoPayment),
            "decisionNumber" to decision.decisionNumber,
            "isReliefDecision" to isReliefDecision,
            "decisionType" to decision.decisionType.toString(),
            "headFullName" to with(decision.headOfFamily) { "$firstName $lastName" },
            "serviceProviderValue" to formatCents(decision.voucherValue - decision.finalCoPayment),
            "showValidTo" to
                (isReliefDecision || decision.validTo.isBefore(LocalDate.now(europeHelsinki))),
            "approverFirstName" to
                (decision.financeDecisionHandlerFirstName ?: decision.approvedBy?.firstName),
            "approverLastName" to
                (decision.financeDecisionHandlerLastName ?: decision.approvedBy?.lastName),
            "decisionMakerName" to settings[SettingType.DECISION_MAKER_NAME],
            "decisionMakerTitle" to settings[SettingType.DECISION_MAKER_TITLE],
            "hasChildIncome" to hasChildIncome,
            "childIncomeTotal" to formatCents(decision.childIncome?.total),
            "childFullName" to with(decision.child) { "$firstName $lastName" },
            "childIncomeEffect" to
                (decision.childIncome?.effect?.name ?: IncomeEffect.NOT_AVAILABLE.name),
        )
    }

    private fun createFeeDecisionPdfContext(data: FeeDecisionPdfData): Context {
        return Context().apply {
            locale = data.lang.isoLanguage.toLocale()
            setVariables(getFeeDecisionPdfVariables(data))
        }
    }

    fun getFeeDecisionPdfVariables(data: FeeDecisionPdfData): Map<String, Any?> {
        data class FeeDecisionPdfPart(
            val childName: String,
            val placementType: PlacementType,
            val serviceNeedDescription: String,
            val feeAlterations: List<FeeAlterationPdfPart>,
            val finalFeeFormatted: String,
            val feeFormatted: String,
            val siblingDiscount: Int,
            val incomeTotal: String?, // head of family + partner + child income
            val childIncomeTotal: String?,
            val hasChildIncome: Boolean,
        )

        val (decision, settings, lang) = data

        val totalIncome =
            listOfNotNull(decision.headOfFamilyIncome?.total, decision.partnerIncome?.total).sum()

        val hideTotalIncome =
            (decision.headOfFamilyIncome == null ||
                decision.headOfFamilyIncome.effect != IncomeEffect.INCOME) ||
                (decision.partnerIncome != null &&
                    decision.partnerIncome.effect != IncomeEffect.INCOME)

        val isReliefDecision = decision.decisionType != FeeDecisionType.NORMAL

        val hasChildIncome =
            decision.children.any { it.childIncome != null && it.childIncome.total > 0 }

        val distinctPlacementTypes = decision.children.map { it.placementType }.distinct()

        return mapOf(
                "approvedAt" to dateFmt(decision.approvedAt?.toLocalDate()),
                "decisionNumber" to decision.decisionNumber,
                "isReliefDecision" to isReliefDecision,
                "decisionType" to decision.decisionType.toString(),
                "hasPartner" to (decision.partner != null),
                "partnerIsCodebtor" to
                    (decision.partner != null && decision.partnerIsCodebtor == true),
                "headFullName" to with(decision.headOfFamily) { "$firstName $lastName" },
                "headIncomeEffect" to
                    (decision.headOfFamilyIncome?.effect?.name ?: IncomeEffect.NOT_AVAILABLE.name),
                "headIncomeTotal" to formatCents(decision.headOfFamilyIncome?.total),
                "partnerFullName" to decision.partner?.let { "${it.firstName} ${it.lastName}" },
                "partnerIncomeEffect" to
                    (decision.partnerIncome?.effect?.name ?: IncomeEffect.NOT_AVAILABLE.name),
                "partnerIncomeTotal" to formatCents(decision.partnerIncome?.total),
                "distinctPlacementTypes" to distinctPlacementTypes,
                "parts" to
                    decision.children.map {
                        FeeDecisionPdfPart(
                            "${it.child.firstName} ${it.child.lastName}",
                            it.placementType,
                            if (lang == OfficialLanguage.SV) it.serviceNeedDescriptionSv
                            else it.serviceNeedDescriptionFi,
                            it.feeAlterations.map { fa ->
                                FeeAlterationPdfPart(
                                    fa.type,
                                    fa.amount,
                                    fa.isAbsolute,
                                    formatCents(fa.effect)!!,
                                )
                            },
                            formatCents(it.finalFee)!!,
                            formatCents(it.fee)!!,
                            it.siblingDiscount,
                            formatCents(totalIncome + (it.childIncome?.total ?: 0)),
                            formatCents(it.childIncome?.total),
                            it.childIncome != null && it.childIncome.total > 0,
                        )
                    },
                "totalFee" to formatCents(decision.totalFee),
                "totalIncome" to formatCents(totalIncome),
                "showTotalIncome" to !hideTotalIncome,
                "validFor" to
                    with(decision) {
                        "${dateFmt(validDuring.start)} - ${dateFmt(validDuring.end)}"
                    },
                "validFrom" to dateFmt(decision.validDuring.start),
                "validTo" to dateFmt(decision.validDuring.end),
                "feePercent" to
                    (decision.feeThresholds.incomeMultiplier * BigDecimal(100))
                        .setScale(1, RoundingMode.HALF_UP)
                        .toDecimalString(),
                "incomeMinThreshold" to formatCents(-1 * decision.feeThresholds.minIncomeThreshold),
                "familySize" to decision.familySize,
                "showValidTo" to
                    (isReliefDecision ||
                        decision.validDuring.end.isBefore(LocalDate.now(europeHelsinki))),
                "approverFirstName" to
                    (decision.financeDecisionHandlerFirstName ?: decision.approvedBy?.firstName),
                "approverLastName" to
                    (decision.financeDecisionHandlerLastName ?: decision.approvedBy?.lastName),
                "decisionMakerName" to settings[SettingType.DECISION_MAKER_NAME],
                "decisionMakerTitle" to settings[SettingType.DECISION_MAKER_TITLE],
                "hasChildIncome" to hasChildIncome,
            )
            .mapValues { it.value ?: "" }
    }

    private fun getResourceFile(fileName: String): File {
        val res = javaClass.classLoader.getResource(fileName)
        return Paths.get(res.toURI()).toFile()
    }
}

private fun BigDecimal.toDecimalString(): String = this.toString().replace('.', ',')

private fun formatCents(amountInCents: Int?): String? =
    if (amountInCents != null) {
        BigDecimal(amountInCents).divide(BigDecimal(100), 2, RoundingMode.HALF_UP).toDecimalString()
    } else {
        null
    }

private fun dateFmt(date: LocalDate?): String =
    date?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?: ""

/** mostly copy from [ITextFontResolver.addFontDirectory] to add encoding support */
fun ITextFontResolver.addFontDirectory(f: File, encoding: String, embedded: Boolean) {
    if (f.isDirectory) {
        f.listFiles { _, name ->
                val lower = name.lowercase(Locale.getDefault())
                lower.endsWith(".otf") || lower.endsWith(".ttf")
            }
            .forEach { file -> addFont(file.absolutePath, encoding, embedded) }
    }
}
