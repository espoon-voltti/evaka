// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.espoo.decision.service

import evaka.core.daycare.UnitManager
import evaka.core.daycare.domain.Language
import evaka.core.daycare.domain.ProviderType
import evaka.core.decision.Decision
import evaka.core.decision.DecisionStatus
import evaka.core.decision.DecisionType
import evaka.core.decision.DecisionUnit
import evaka.core.decision.PdfReasoning
import evaka.core.decision.generateDecisionPages
import evaka.core.identity.ExternalIdentifier
import evaka.core.pis.service.PersonDTO
import evaka.core.setting.SettingType
import evaka.core.shared.ApplicationId
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.DecisionId
import evaka.core.shared.PersonId
import evaka.core.shared.config.pdfTemplateEngine
import evaka.core.shared.domain.OfficialLanguage
import evaka.core.shared.template.EvakaTemplateProvider
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertContains
import kotlin.test.assertFalse
import org.junit.jupiter.api.Test

class DecisionReasoningPdfRenderTest {
    private val settings =
        mapOf(
            SettingType.DECISION_MAKER_NAME to "Paula Päättäjä",
            SettingType.DECISION_MAKER_TITLE to "Asiakaspalvelupäällikkö",
        )
    private val templateEngine = pdfTemplateEngine("espoo")
    private val templateProvider = EvakaTemplateProvider()
    private val manager = UnitManager("Päivi Johtaja", "paivi.johtaja@example.com", "0451231234")

    private fun render(template: String, type: DecisionType, reasoning: PdfReasoning?): String {
        val page =
            generateDecisionPages(
                template = template,
                lang = OfficialLanguage.FI,
                settings = settings,
                decision = validDecision(type),
                child = validChild(),
                unitManager = manager,
                preschoolManager = manager,
                isPartTimeDecision = false,
                serviceNeed = null,
                reasoning = reasoning,
            )
        return templateEngine.process(page.template.name, page.context)
    }

    private fun renderPreschool(reasoning: PdfReasoning?): String =
        render(templateProvider.getPreschoolDecisionPath(), DecisionType.PRESCHOOL, reasoning)

    private fun renderPreparatory(reasoning: PdfReasoning?): String =
        render(
            templateProvider.getPreparatoryDecisionPath(),
            DecisionType.PREPARATORY_EDUCATION,
            reasoning,
        )

    private fun renderDaycare(reasoning: PdfReasoning?): String =
        render(templateProvider.getDaycareDecisionPath(), DecisionType.DAYCARE, reasoning)

    private fun renderVoucher(reasoning: PdfReasoning?): String =
        render(templateProvider.getDaycareVoucherDecisionPath(), DecisionType.DAYCARE, reasoning)

    private fun renderTransfer(reasoning: PdfReasoning?): String =
        render(templateProvider.getDaycareTransferDecisionPath(), DecisionType.DAYCARE, reasoning)

    private fun renderClub(reasoning: PdfReasoning?): String =
        render(templateProvider.getClubDecisionPath(), DecisionType.CLUB, reasoning)

    @Test
    fun `renders generic and individual reasoning when present`() {
        val html =
            renderPreschool(
                PdfReasoning(
                    generic = "Yleinen perustelu lapselle",
                    individual = listOf("Erityinen perustelu"),
                )
            )

        assertContains(html, "Yleinen perustelu lapselle")
        assertContains(html, "Erityinen perustelu")
        assertContains(html, "Toimivalta")
    }

    @Test
    fun `omits the reasoning section and shows the hard-coded instructions when reasoning is null`() {
        val html = renderPreschool(null)

        assertFalse(html.contains("Päätöksen perustelut"))
        assertContains(html, "Sovelletut oikeusohjeet")
        assertContains(html, "Perusopetuslaki")
        assertContains(html, "Toimivalta")
    }

    @Test
    fun `renders generic and individual reasoning in the preparatory decision when present`() {
        val html =
            renderPreparatory(
                PdfReasoning(
                    generic = "Yleinen perustelu lapselle",
                    individual = listOf("Erityinen perustelu"),
                )
            )

        assertContains(html, "Yleinen perustelu lapselle")
        assertContains(html, "Erityinen perustelu")
        assertContains(html, "Toimivalta")
    }

    @Test
    fun `omits the reasoning section in the preparatory decision when reasoning is null`() {
        val html = renderPreparatory(null)

        assertFalse(html.contains("Päätöksen perustelut"))
        assertContains(html, "Sovelletut oikeusohjeet")
        assertContains(html, "Perusopetuslaki")
        assertContains(html, "Toimivalta")
    }

    @Test
    fun `renders reasoning and moves the first instruction paragraph into the daycare acceptance section`() {
        val html =
            renderDaycare(
                PdfReasoning(
                    generic = "Yleinen perustelu lapselle",
                    individual = listOf("Erityinen perustelu"),
                )
            )

        assertContains(html, "Yleinen perustelu lapselle")
        assertContains(html, "Erityinen perustelu")
        // first instruction paragraph is preserved as part of the acceptance section
        assertContains(html, "tulee hyväksyä/hylätä viimeistään kahden viikon")
        // the static legal-references paragraph is replaced by the reasoning fragment
        assertFalse(html.contains("Sovelletut oikeusohjeet"))
    }

    @Test
    fun `omits the reasoning section in the daycare decision when reasoning is null`() {
        val html = renderDaycare(null)

        assertFalse(html.contains("Päätöksen perustelut"))
        assertContains(html, "Sovelletut oikeusohjeet")
        assertContains(html, "tulee hyväksyä/hylätä viimeistään kahden viikon")
    }

    @Test
    fun `lifts the legal references to the top via the reasoning fragment in the voucher decision`() {
        val html =
            renderVoucher(
                PdfReasoning(
                    generic = "Yleinen perustelu lapselle",
                    individual = listOf("Erityinen perustelu"),
                )
            )

        assertContains(html, "Yleinen perustelu lapselle")
        assertContains(html, "Erityinen perustelu")
        // page-1 instructions are preserved as the acceptance section
        assertContains(html, "tulee hyväksyä/hylätä viimeistään kahden viikon")
        // the page-2 legal-references paragraph is replaced by the reasoning fragment
        assertFalse(html.contains("Sovelletut oikeusohjeet"))
        // the authority paragraph is kept on page 2
        assertContains(html, "Toimivalta")
    }

    @Test
    fun `omits the reasoning section in the voucher decision when reasoning is null`() {
        val html = renderVoucher(null)

        assertFalse(html.contains("Päätöksen perustelut"))
        assertContains(html, "Sovelletut oikeusohjeet")
        assertContains(html, "Toimivalta")
        assertContains(html, "tulee hyväksyä/hylätä viimeistään kahden viikon")
    }

    @Test
    fun `renders reasoning above the first paragraphs in the transfer decision`() {
        val html =
            renderTransfer(
                PdfReasoning(
                    generic = "Yleinen perustelu lapselle",
                    individual = listOf("Erityinen perustelu"),
                )
            )

        assertContains(html, "Yleinen perustelu lapselle")
        assertContains(html, "Erityinen perustelu")
        // the leading instruction paragraphs are preserved in the acceptance section
        assertContains(html, "tulee hyväksyä/hylätä viimeistään kahden viikon")
        assertContains(html, "Tämä päätös poistaa lapsen siirtohakemuksen")
        // the static legal-references paragraph is replaced by the reasoning fragment
        assertFalse(html.contains("Sovelletut oikeusohjeet"))
        // the authority paragraph is kept on page 2
        assertContains(html, "Toimivalta")
    }

    @Test
    fun `omits the reasoning section in the transfer decision when reasoning is null`() {
        val html = renderTransfer(null)

        assertFalse(html.contains("Päätöksen perustelut"))
        assertContains(html, "Sovelletut oikeusohjeet")
        assertContains(html, "Toimivalta")
        assertContains(html, "tulee hyväksyä/hylätä viimeistään kahden viikon")
    }

    @Test
    fun `replaces the first instruction paragraph with the reasoning in the club decision`() {
        val html =
            renderClub(
                PdfReasoning(
                    generic = "Yleinen perustelu lapselle",
                    individual = listOf("Erityinen perustelu"),
                )
            )

        assertContains(html, "Yleinen perustelu lapselle")
        assertContains(html, "Erityinen perustelu")
        // the remaining instruction paragraphs are kept in the acceptance section
        assertContains(html, "Kerhopaikan vastaanottamisesta tai kieltäytymisestä on ilmoitettava")
        assertContains(html, "Toimivalta")
        // the first paragraph (the legal basis) is replaced by the reasoning fragment
        assertFalse(html.contains("Päätös perustuu"))
    }

    @Test
    fun `omits the reasoning section in the club decision when reasoning is null`() {
        val html = renderClub(null)

        assertFalse(html.contains("Päätöksen perustelut"))
        assertContains(html, "Päätös perustuu")
        assertContains(html, "Kerhopaikan vastaanottamisesta tai kieltäytymisestä on ilmoitettava")
        assertContains(html, "Toimivalta")
    }
}

private fun validDecision(type: DecisionType = DecisionType.PRESCHOOL) =
    Decision(
        DecisionId(UUID.randomUUID()),
        createdBy = "Matti Käsittelijä",
        type,
        startDate = LocalDate.now(),
        endDate = LocalDate.now().plusMonths(3),
        validDecisionUnit(),
        applicationId = ApplicationId(UUID.randomUUID()),
        childId = ChildId(UUID.randomUUID()),
        childName = "Matti",
        documentKey = null,
        decisionNumber = 12345,
        sentDate = LocalDate.now(),
        DecisionStatus.ACCEPTED,
        requestedStartDate = null,
        resolved = null,
        resolvedByName = null,
        documentContainsContactInfo = false,
        archivedAt = null,
    )

private fun validDecisionUnit() =
    DecisionUnit(
        DaycareId(UUID.randomUUID()),
        name = "Esiopetusyksikkö",
        daycareDecisionName = "Esiopetusyksikkö",
        preschoolDecisionName = "Esiopetusyksikkö",
        manager = null,
        streetAddress = "Esikatu 1",
        postalCode = "02100",
        postOffice = "Espoo",
        phone = "+35850 1234564",
        decisionHandler = "Käsittelijä",
        decisionHandlerAddress = "Toritie 2, 02100 Espoo",
        ProviderType.MUNICIPAL,
        language = Language.fi,
    )

private fun validChild() =
    PersonDTO(
        PersonId(UUID.randomUUID()),
        duplicateOf = null,
        ExternalIdentifier.SSN.getInstance("010115A9532"),
        ssnAddingDisabled = false,
        firstName = "Matti",
        lastName = "Meikäläinen",
        preferredName = "Matti",
        email = null,
        phone = "",
        backupPhone = "",
        language = null,
        dateOfBirth = LocalDate.of(2015, 1, 1),
        streetAddress = "Kokinpellonraitti 3",
        postalCode = "02100",
        postOffice = "Espoo",
        residenceCode = "",
        restrictedDetailsEnabled = false,
        municipalityOfResidence = "Espoo",
    )
