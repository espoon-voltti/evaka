// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.trevaka.security

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import evaka.core.shared.auth.UserRole
import evaka.core.shared.security.Action
import evaka.core.shared.security.Action.AbsenceApplication
import evaka.core.shared.security.Action.Application
import evaka.core.shared.security.Action.ApplicationNote
import evaka.core.shared.security.Action.AssistanceAction
import evaka.core.shared.security.Action.AssistanceFactor
import evaka.core.shared.security.Action.AssistanceNeedVoucherCoefficient
import evaka.core.shared.security.Action.Attachment
import evaka.core.shared.security.Action.BackupCare
import evaka.core.shared.security.Action.BackupPickup
import evaka.core.shared.security.Action.CalendarEvent
import evaka.core.shared.security.Action.CalendarEventTime
import evaka.core.shared.security.Action.Child
import evaka.core.shared.security.Action.ChildDailyNote
import evaka.core.shared.security.Action.ChildDocument
import evaka.core.shared.security.Action.ChildImage
import evaka.core.shared.security.Action.ChildStickyNote
import evaka.core.shared.security.Action.ClubTerm
import evaka.core.shared.security.Action.DailyServiceTime
import evaka.core.shared.security.Action.DaycareAssistance
import evaka.core.shared.security.Action.Decision
import evaka.core.shared.security.Action.DocumentTemplate
import evaka.core.shared.security.Action.Employee
import evaka.core.shared.security.Action.FeeAlteration
import evaka.core.shared.security.Action.FeeDecision
import evaka.core.shared.security.Action.FeeThresholds
import evaka.core.shared.security.Action.FinanceNote
import evaka.core.shared.security.Action.FosterParent
import evaka.core.shared.security.Action.Global
import evaka.core.shared.security.Action.Group
import evaka.core.shared.security.Action.GroupNote
import evaka.core.shared.security.Action.GroupPlacement
import evaka.core.shared.security.Action.Income
import evaka.core.shared.security.Action.IncomeStatement
import evaka.core.shared.security.Action.Invoice
import evaka.core.shared.security.Action.InvoiceCorrection
import evaka.core.shared.security.Action.MessageAccount
import evaka.core.shared.security.Action.MobileDevice
import evaka.core.shared.security.Action.OtherAssistanceMeasure
import evaka.core.shared.security.Action.Pairing
import evaka.core.shared.security.Action.Parentship
import evaka.core.shared.security.Action.Partnership
import evaka.core.shared.security.Action.Payment
import evaka.core.shared.security.Action.PedagogicalDocument
import evaka.core.shared.security.Action.Person
import evaka.core.shared.security.Action.Placement
import evaka.core.shared.security.Action.PreschoolAssistance
import evaka.core.shared.security.Action.PreschoolTerm
import evaka.core.shared.security.Action.ScopedAction
import evaka.core.shared.security.Action.ServiceApplication
import evaka.core.shared.security.Action.ServiceNeed
import evaka.core.shared.security.Action.UnscopedAction
import evaka.core.shared.security.Action.VoucherValueDecision
import evaka.core.shared.security.actionrule.DatabaseActionRule
import evaka.core.shared.security.actionrule.HasGlobalRole
import evaka.core.shared.security.actionrule.HasGroupRole
import evaka.core.shared.security.actionrule.HasUnitRole
import evaka.core.shared.security.actionrule.ScopedActionRule
import evaka.core.shared.security.actionrule.UnscopedActionRule
import evaka.instance.hameenkyro.security.HameenkyroActionRuleMapping
import evaka.instance.kangasala.security.KangasalaActionRuleMapping
import evaka.instance.lempaala.security.LempaalaActionRuleMapping
import evaka.instance.nokia.security.NokiaActionRuleMapping
import evaka.instance.orivesi.security.OrivesiActionRuleMapping
import evaka.instance.pirkkala.security.PirkkalaActionRuleMapping
import evaka.instance.tampere.security.TampereActionRuleMapping
import evaka.instance.vesilahti.security.VesilahtiActionRuleMapping
import evaka.instance.ylojarvi.security.YlojarviActionRuleMapping
import java.nio.charset.StandardCharsets
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import tools.jackson.dataformat.csv.CsvMapper

@JsonPropertyOrder(
    value =
        [
            "typeName",
            "actionName",
            "action",
            "admin",
            "reportViewer",
            "director",
            "financeAdmin",
            "financeStaff",
            "serviceWorker",
            "messaging",
            "unitSupervisor",
            "staff",
            "specialEducationTeacher",
            "earlyChildhoodEducationSecretary",
        ]
)
private data class CsvRow(
    @get:JsonProperty("Tyyppi") val typeName: String,
    @get:JsonProperty("Toiminto") val actionName: String,
    @get:JsonProperty("Tekninen nimi") val action: String,
    @get:JsonProperty("Pääkäyttäjä") val admin: String,
    @get:JsonProperty("Raportointi") val reportViewer: String,
    @get:JsonProperty("Vaka-päälliköt") val director: String,
    @get:JsonProperty("Talous") val financeAdmin: String,
    @get:JsonProperty("Talouden työntekijä (ulkoinen)") val financeStaff: String,
    @get:JsonProperty("Palveluohjaus") val serviceWorker: String,
    @get:JsonProperty("Viestintä") val messaging: String,
    @get:JsonProperty("Johtaja") val unitSupervisor: String,
    @get:JsonProperty("Henkilökunta") val staff: String,
    @get:JsonProperty("Erityisopettaja") val specialEducationTeacher: String,
    @get:JsonProperty("Varhaiskasvatussihteeri") val earlyChildhoodEducationSecretary: String,
)

private val actionRuleMappings =
    TrevakaActionRuleMapping().let {
        mapOf(
            "TRE" to TampereActionRuleMapping(it),
            "VES" to VesilahtiActionRuleMapping(it),
            "HAM" to HameenkyroActionRuleMapping(it),
            "NOK" to NokiaActionRuleMapping(it),
            "YLO" to YlojarviActionRuleMapping(it),
            "PIR" to PirkkalaActionRuleMapping(it),
            "KAN" to KangasalaActionRuleMapping(it),
            "LEM" to LempaalaActionRuleMapping(it),
            "ORI" to OrivesiActionRuleMapping(it),
        )
    }

class TrevakaActionRuleMappingTest {
    /**
     * Generates csv data based on current action rule mappings.
     *
     * Steps to transform src/test/resources/permissions/tampere-region.csv into tampere-region.xlsx
     * with LibreOffice:
     * 1. View -> Freeze Cells -> Freeze First Row
     * 2. (Select first row) -> Format -> Text -> Bold
     * 3. (Select columns from D to N, i.e. all roles): Format -> Cells... -> Alignment -> Text
     *    Alignment -> Horizontal -> Center -> OK
     * 4. File -> Save As... -> (Select format: Office Open XML Spreadsheet (.xlsx)) -> Save
     */
    @Test
    fun csv() {
        val mapper = CsvMapper()
        val schema =
            mapper
                .schemaFor(CsvRow::class.java)
                .withHeader()
                .withQuoteChar('"')
                .withColumnSeparator(';')
        assertEquals(
            ClassPathResource("permissions/tampere-region.csv")
                .getContentAsString(StandardCharsets.UTF_8),
            "\uFEFF${mapper.writer(schema).writeValueAsString(rows().map { it.toCsvRow() })}",
        )
    }
}

private fun rows() =
    Global.entries
        .map { Row("", translate(it), permissions(it)) }
        .sortedWith(compareBy({ actionType(it) }, { it.action.second })) +
        Application.entries.map { Row("Hakemus", translate(it), permissions(it)) } +
        ApplicationNote.entries.map {
            Row("Hakemuksen muistiinpano", translate(it), permissions(it))
        } +
        FinanceNote.entries.map { Row("Talouden muistiinpano", translate(it), permissions(it)) } +
        Decision.entries.map { Row("Päätös", translate(it), permissions(it)) } +
        AbsenceApplication.entries.map {
            Row("Esiopetuksen poissaolohakemus", translate(it), permissions(it))
        } +
        ServiceApplication.entries.map {
            Row("Palveluntarpeen muutoshakemus", translate(it), permissions(it))
        } +
        Person.entries.map { Row("Henkilö", translate(it), permissions(it)) } +
        Parentship.entries.map { Row("Päämies", translate(it), permissions(it)) } +
        Partnership.entries.map { Row("Puoliso", translate(it), permissions(it)) } +
        IncomeStatement.entries.map { Row("Tuloselvitys", translate(it), permissions(it)) } +
        Income.entries.map { Row("Tulo", translate(it), permissions(it)) } +
        Child.entries.map { Row("Lapsi", translate(it), permissions(it)) } +
        ChildDailyNote.entries.map { Row("Lapsen muistiinpano", translate(it), permissions(it)) } +
        ChildStickyNote.entries.map {
            Row("Lapsen huomiot lähipäivinä", translate(it), permissions(it))
        } +
        ChildImage.entries.map { Row("Lapsen kuva", translate(it), permissions(it)) } +
        BackupPickup.entries.map { Row("Varahakija", translate(it), permissions(it)) } +
        FosterParent.entries.map { Row("Sijaisvanhempi", translate(it), permissions(it)) } +
        Placement.entries.map { Row("Sijoitus", translate(it), permissions(it)) } +
        ServiceNeed.entries.map { Row("Palveluntarve", translate(it), permissions(it)) } +
        BackupCare.entries.map { Row("Varasijoitus", translate(it), permissions(it)) } +
        DailyServiceTime.entries.map {
            Row("Päivittäinen varhaiskasvatusaika", translate(it), permissions(it))
        } +
        AssistanceFactor.entries.map { Row("Tuen kerroin", translate(it), permissions(it)) } +
        AssistanceNeedVoucherCoefficient.entries.map {
            Row("Palvelusetelikerroin", translate(it), permissions(it))
        } +
        DaycareAssistance.entries.map {
            Row("Tuen taso varhaiskasvatuksessa", translate(it), permissions(it))
        } +
        PreschoolAssistance.entries.map {
            Row("Tuki esiopetuksessa", translate(it), permissions(it))
        } +
        AssistanceAction.entries.map {
            Row("Tukitoimet ja tukipalvelut", translate(it), permissions(it))
        } +
        OtherAssistanceMeasure.entries.map { Row("Muut toimet", translate(it), permissions(it)) } +
        PedagogicalDocument.entries.map {
            Row("Pegagoginen dokumentointi", translate(it), permissions(it))
        } +
        ChildDocument.entries.map { Row("Asiakirja", translate(it), permissions(it)) } +
        FeeAlteration.entries.map { Row("Maksumuutos", translate(it), permissions(it)) } +
        Action.Unit.entries.map { Row("Yksikkö", translate(it), permissions(it)) } +
        Group.entries.map { Row("Ryhmä", translate(it), permissions(it)) } +
        GroupNote.entries.map { Row("Ryhmän muistiinpano", translate(it), permissions(it)) } +
        GroupPlacement.entries.map { Row("Ryhmäsijoitus", translate(it), permissions(it)) } +
        CalendarEvent.entries.map { Row("Tapahtuma", translate(it), permissions(it)) } +
        CalendarEventTime.entries.map { Row("Keskusteluaika", translate(it), permissions(it)) } +
        Employee.entries.map { Row("Työntekijä", translate(it), permissions(it)) } +
        MessageAccount.entries.map { Row("Viestitili", translate(it), permissions(it)) } +
        MobileDevice.entries.map { Row("Mobiililaite", translate(it), permissions(it)) } +
        Pairing.entries.map { Row("Mobiiliparitus", translate(it), permissions(it)) } +
        Attachment.entries.map { Row("Liite", translate(it), permissions(it)) } +
        FeeThresholds.entries.map {
            Row("Talouden maksuasetukset", translate(it), permissions(it))
        } +
        PreschoolTerm.entries.map { Row("Esiopetuskausi", translate(it), permissions(it)) } +
        ClubTerm.entries.map { Row("Kerhokausi", translate(it), permissions(it)) } +
        DocumentTemplate.entries.map { Row("Asiakirjapohja", translate(it), permissions(it)) } +
        FeeDecision.entries.map { Row("Maksupäätös", translate(it), permissions(it)) } +
        VoucherValueDecision.entries.map { Row("Arvopäätös", translate(it), permissions(it)) } +
        Invoice.entries.map { Row("Lasku", translate(it), permissions(it)) } +
        InvoiceCorrection.entries.map {
            Row("Laskun korjausrivi", translate(it), permissions(it))
        } +
        Payment.entries.map { Row("Maksu", translate(it), permissions(it)) }

private fun permissions(action: UnscopedAction) =
    permissions(
        actionRuleMappings.mapValues { (_, actionRuleMapping) ->
            actionRuleMapping.rulesOf(action).toList().flatMap { roles(it) }.toSet()
        }
    )

private fun permissions(action: ScopedAction<*>) =
    permissions(
        actionRuleMappings.mapValues { (_, actionRuleMapping) ->
            actionRuleMapping.rulesOf(action).toList().flatMap { roles(it) }.toSet()
        }
    )

private fun permissions(rolesByMunicipality: Map<String, Set<UserRole>>) =
    UserRole.entries.associateWith { role ->
        val enabledMunicipalities =
            rolesByMunicipality.entries
                .filter { (_, roles) -> roles.contains(role) }
                .map { (municipality) -> municipality }
        when {
            enabledMunicipalities.size == rolesByMunicipality.size -> "x"
            enabledMunicipalities.isNotEmpty() -> enabledMunicipalities.joinToString(",")
            else -> ""
        }
    }

private fun roles(rule: UnscopedActionRule) =
    when (rule) {
        is HasGlobalRole -> rule.oneOf

        is DatabaseActionRule.Unscoped<*> -> roles(rule)

        else ->
            with(rule.toString()) {
                when {
                    contains("IsEmployee\$any") -> UserRole.entries
                    contains("IsMobile") -> emptyList()
                    contains("IsCitizen") -> emptyList()
                    else -> throw error("Unsupported UnscopedActionRule $rule")
                }
            }
    }

private fun roles(rule: ScopedActionRule<*>) =
    when (rule) {
        is HasGlobalRole -> rule.oneOf
        is DatabaseActionRule.Unscoped<*> -> roles(rule)
        is DatabaseActionRule.Scoped<*, *> -> roles(rule)
        else -> throw error("Unsupported ScopedActionRule $rule")
    }

private fun roles(rule: DatabaseActionRule.Unscoped<*>) =
    when (val params = rule.params) {
        is HasUnitRole -> params.oneOf

        else ->
            with(params.toString()) {
                when {
                    contains("IsEmployee") -> emptyList()
                    else -> throw error("Unsupported DatabaseActionRule.Unscoped $rule")
                }
            }
    }

private fun roles(rule: DatabaseActionRule.Scoped<*, *>) =
    when (val params = rule.params) {
        is HasGlobalRole -> params.oneOf

        is HasUnitRole -> params.oneOf

        is HasGroupRole -> params.oneOf

        else ->
            with(params.toString()) {
                when {
                    contains("IsEmployee") -> emptyList()
                    contains("IsMobile") -> emptyList()
                    contains("IsCitizen") -> emptyList()
                    else -> throw error("Unsupported DatabaseActionRule.Scoped $rule")
                }
            }
    }

private fun translate(action: Global) =
    action.name to
        when (action) {
            Global.APPLICATIONS_PAGE -> "Hakemukset-sivu"
            Global.DOCUMENT_TEMPLATES_PAGE -> "Asiakirjapohjat-sivu"
            Global.EMPLOYEES_PAGE -> "Työntekijät-sivu"
            Global.FINANCE_BASICS_PAGE -> "Talouden maksuasetukset-sivu"
            Global.FINANCE_PAGE -> "Talous-sivu"
            Global.HOLIDAY_AND_TERM_PERIODS_PAGE -> "Loma-ajat ja toimintakaudet -sivu"
            Global.MESSAGES_PAGE -> "Viestit-sivu"
            Global.PERSON_SEARCH_PAGE -> "Asiakastiedot-sivu"
            Global.REPORTS_PAGE -> "Raportit-sivu"
            Global.SETTINGS_PAGE -> "Asetukset-sivu"
            Global.UNIT_FEATURES_PAGE -> "Toimintojen avaukset -sivu"
            Global.UNITS_PAGE -> "Yksiköt-sivu"
            Global.PERSONAL_MOBILE_DEVICE_PAGE -> "Henkilökohtainen eVaka mobiili -sivu"
            Global.PIN_CODE_PAGE -> "eVaka mobiilin PIN-koodi -sivu"
            Global.CREATE_DOCUMENT_TEMPLATE -> "Asiakirjapohjan luonti"
            Global.READ_DOCUMENT_TEMPLATE -> "Asiakirjapohjien luku"
            Global.FETCH_INCOME_STATEMENTS_AWAITING_HANDLER ->
                "Tuloselvitysten luku (odottaa käsittelyä)"
            Global.CREATE_PAPER_APPLICATION -> "Hakemuksen luonti"
            Global.READ_SERVICE_WORKER_APPLICATION_NOTES -> "Palveluohjauksen muistiinpanojen luku"
            Global.WRITE_SERVICE_WORKER_APPLICATION_NOTES ->
                "Palveluohjauksen muistiinpanojen muokkaus"
            Global.CREATE_PERSON -> "Henkilön luonti"
            Global.CREATE_PERSON_FROM_VTJ -> "Henkilön luonti VTJ:stä"
            Global.SEARCH_PEOPLE -> "Henkilöiden haku"
            Global.SEARCH_PEOPLE_UNRESTRICTED -> "Henkilöiden haku (rajoittamaton)"
            Global.READ_FEE_THRESHOLDS -> "Talouden maksuasetuksien luku"
            Global.CREATE_FEE_THRESHOLDS -> "Talouden maksuasetuksien luonti"
            Global.READ_VOUCHER_VALUES -> "Palveluseteliarvojen luku"
            Global.CREATE_VOUCHER_VALUE -> "Palveluseteliarvon luonti"
            Global.UPDATE_VOUCHER_VALUE -> "Palveluseteliarvon muokkaus"
            Global.DELETE_VOUCHER_VALUE -> "Palveluseteliarvon poisto"
            Global.SEARCH_FEE_DECISIONS -> "Maksupäätösten haku"
            Global.SEARCH_VOUCHER_VALUE_DECISIONS -> "Arvopäätösten haku"
            Global.READ_FINANCE_DECISION_HANDLERS -> "Talouden päätöksen käsittelijöiden luku"
            Global.READ_SELECTABLE_FINANCE_DECISION_HANDLERS ->
                "Talouden päätöksen käsittelijöiden luku"
            Global.READ_PERSONAL_MOBILE_DEVICES -> "Henkilökohtaisten mobiililaitteiden luku"
            Global.CREATE_PERSONAL_MOBILE_DEVICE_PAIRING ->
                "Henkilökohtaisen mobiililaitteen parituksen luonti"
            Global.SEARCH_INVOICES -> "Laskujen haku"
            Global.CREATE_DRAFT_INVOICES -> "Laskuluonnosten luonti"
            Global.SEARCH_PAYMENTS -> "Maksujen haku"
            Global.CREATE_DRAFT_PAYMENTS -> "Maksuluonnosten luonti"
            Global.READ_ASSISTANCE_ACTION_OPTIONS -> "Tukitoimivaihtoehtojen luku"
            Global.READ_SERVICE_NEED_OPTIONS -> "Palveluntarvevaihtoehtojen luku"
            Global.READ_UNITS -> "Yksiköiden luku"
            Global.CREATE_UNIT -> "Yksikön luonti"
            Global.READ_CUSTOMER_FEES_REPORT -> "Asiakasmaksut-raportti"
            Global.READ_DECISION_UNITS -> "Päätösluonnoksen yksiköiden luku"
            Global.READ_CHILD_DOCUMENT_DECISIONS_REPORT -> "Muut päätökset -raportti"
            Global.READ_DECISIONS_REPORT -> "Päätökset-raportti"
            Global.READ_DUPLICATE_PEOPLE_REPORT -> "Monistuneet kuntalaiset -raportti"
            Global.READ_ENDED_PLACEMENTS_REPORT ->
                "Varhaiskasvatuksessa lopettavat lapset -raportti"
            Global.READ_INCOMPLETE_INCOMES_REPORT -> "Puuttuvat tulotiedot -raportti"
            Global.READ_INVOICE_REPORT -> "Laskujen täsmäytys -raportti"
            Global.READ_MISSING_HEAD_OF_FAMILY_REPORT -> "Puuttuvat päämiehet -raportti"
            Global.READ_PLACEMENT_SKETCHING_REPORT ->
                "Esiopetuksen sijoitusten hahmotteluraportti -raportti"
            Global.READ_PLACEMENT_COUNT_REPORT -> "Sijoitusten määrä -raportti"
            Global.READ_RAW_REPORT -> "Raakaraportti"
            Global.READ_SEXTET_REPORT -> "Kuusikkovertailu"
            Global.READ_UNITS_REPORT -> "Yksiköt-raportti"
            Global.READ_VARDA_REPORT -> "Varda-raportit"
            Global.READ_TAMPERE_REGIONAL_SURVEY_REPORT -> "Tampereen alueen seutuselvitys -raportti"
            Global.UPDATE_SETTINGS -> "Asetuksien muokkaus"
            Global.READ_SPECIAL_DIET_LIST -> "Erityisruokavalioiden luku"
            Global.READ_INCOME_TYPES -> "Tulotyyppien luku"
            Global.READ_INCOME_COEFFICIENT_MULTIPLIERS -> "Tulokertoimen luku"
            Global.READ_INVOICE_CODES -> "Tulokoodien luku"
            Global.READ_UNIT_FEATURES -> "Yksikön toimintojen luku"
            Global.CREATE_HOLIDAY_PERIOD -> "Lomakauden luonti"
            Global.READ_HOLIDAY_PERIOD -> "Lomakauden luku"
            Global.READ_HOLIDAY_PERIODS -> "Lomakausien luku"
            Global.DELETE_HOLIDAY_PERIOD -> "Lomakauden poisto"
            Global.UPDATE_HOLIDAY_PERIOD -> "Lomakauden muokkaus"
            Global.READ_HOLIDAY_QUESTIONNAIRE -> "Lomakyselyn luku"
            Global.READ_HOLIDAY_QUESTIONNAIRES -> "Lomakyselyiden luku"
            Global.READ_ACTIVE_HOLIDAY_QUESTIONNAIRES -> "Aktiivisten lomakyselyiden luku"
            Global.CREATE_HOLIDAY_QUESTIONNAIRE -> "Lomakyselyn luonti"
            Global.DELETE_HOLIDAY_QUESTIONNAIRE -> "Lomakyselyn poisto"
            Global.UPDATE_HOLIDAY_QUESTIONNAIRE -> "Lomakyselyn muokkaus"
            Global.SEND_PATU_REPORT -> "Patu-raportin lähetys"
            Global.CREATE_EMPLOYEE -> "Työntekijän luonti"
            Global.READ_EMPLOYEES -> "Työntekijöiden luku"
            Global.SEARCH_EMPLOYEES -> "Työntekijöiden haku"
            Global.SUBMIT_PATU_REPORT -> "Patu-raportin lähetys"
            Global.READ_FUTURE_PRESCHOOLERS -> "Tulevat esikoululaiset -raportti"
            Global.READ_NON_SSN_CHILDREN_REPORT -> "Hetuttomat lapset -raportti"
            Global.VARDA_OPERATIONS -> "Vardan toiminnot"
            Global.CREATE_PRESCHOOL_TERM -> "Esiopetuskauden luonti"
            Global.CREATE_CLUB_TERM -> "Kerhokauden luonti"
            Global.READ_SYSTEM_NOTIFICATIONS -> "Tilapäisten ilmoitusten luku"
            Global.UPDATE_SYSTEM_NOTIFICATION -> "Tilapäisten ilmoitusten luonti"
            Global.SEND_JAMIX_ORDERS -> "Jamixin lähetys"
            Global.PLACEMENT_TOOL -> "Esiopetussijoitusten tuonti"
            Global.OUT_OF_OFFICE_PAGE -> "Poissaoloviesti-sivu"
            Global.READ_AROMI_ORDERS -> "Aromi-raportti"
            Global.READ_PLACEMENT_DESKTOP_DAYCARES -> "Sijoittelutyöpöytä"
            Global.READ_DRAFT_OCCUPANCIES -> "Hahmotellun täyttö/käyttöasteen luku"
            Global.READ_PRESCHOOL_ABSENCE_REPORT_FOR_AREA ->
                "Esiopetuksen poissaoloraportti alueittain"
        }

private fun translate(entry: Enum<*>) =
    entry.name to
        when (entry.name) {
            "READ" -> "Luku"
            "UPDATE" -> "Muokkaus"
            "DELETE" -> "Poisto"
            "DECIDE" -> "Päätöksen luonti"
            "DECIDE_MAX_WEEK" -> "Päätöksen luonti (max viikko)"
            "READ_METADATA" -> "Metadatan luku"
            "READ_IF_HAS_ASSISTANCE_NEED" -> "Tuen tarpeellisen luku"
            "SEND" -> "Lähetys"
            "CANCEL" -> "Hylkäys"
            "MOVE_TO_WAITING_PLACEMENT" -> "Sijoitettaviin siirtäminen"
            "RETURN_TO_SENT" -> "Lähetettyihin palautus"
            "VERIFY" -> "Tarkistus"
            "READ_PLACEMENT_PLAN_DRAFT" -> "Sijoitushahmotelman luku"
            "CREATE_PLACEMENT_PLAN" -> "Sijoitushahmotelman luonti"
            "CANCEL_PLACEMENT_PLAN" -> "Sijoitushahmotelman hylkäys"
            "READ_DECISION_DRAFT" -> "Päätösluonnoksen luku"
            "UPDATE_DECISION_DRAFT" -> "Päätösluonnoksen muokkaus"
            "SEND_DECISIONS_WITHOUT_PROPOSAL" -> "Päätöksen lähetys ilman sijoitusehdotusta"
            "SEND_PLACEMENT_PROPOSAL" -> "Sijoitusehdotuksen lähetys"
            "WITHDRAW_PLACEMENT_PROPOSAL" -> "Sijoitusehdotuksen peruminen"
            "RESPOND_TO_PLACEMENT_PROPOSAL" -> "Sijoitusehdotukseen vastaaminen"
            "CONFIRM_DECISIONS_MAILED" -> "Merkitseminen postitetuksi"
            "ACCEPT_DECISION" -> "Päätöksen hyväksyminen"
            "REJECT_DECISION" -> "Päätöksen hylkääminen"
            "READ_NOTES" -> "Muistiinpanojen luku"
            "CREATE_NOTE" -> "Muistiinpanon luonti"
            "READ_SPECIAL_EDUCATION_TEACHER_NOTES" -> "Erityisopettajan muistiinpanojen luku"
            "UPLOAD_ATTACHMENT" -> "Liitteen lähetys"
            "READ_DECISION_MAKER_OPTIONS" -> "Päätöksentekijöiden luku"
            "REVERT_TO_UNSENT" -> "Takaisin lähettämättömäksi palautus"
            "READ_IN_REPORT" -> "Raportista luku"
            "MARK_AS_OPENED" -> "Avatuksi merkitseminen"
            "UPDATE_DECISION_MAKER" -> "Päätöksentekijän muokkaus"
            "ANNUL" -> "Mitätöinti"
            "READ_ORPHAN_ATTACHMENT" -> "Liitteen luku (orpo)"
            "READ_APPLICATION_ATTACHMENT" -> "Hakemuksen liitteen luku"
            "READ_INCOME_STATEMENT_ATTACHMENT" -> "Tuloselvityksen liitteen luku"
            "READ_INCOME_ATTACHMENT" -> "Tulon liitteen luku"
            "READ_INVOICE_ATTACHMENT" -> "Laskun liitteen luku"
            "READ_PEDAGOGICAL_DOCUMENT_ATTACHMENT" -> "Pedagogisen dokumentoinnin liitteen luku"
            "READ_FEE_ALTERATION_ATTACHMENT" -> "Maksumuutoksen liitteen luku"
            "DELETE_ORPHAN_ATTACHMENT" -> "Liitteen poisto (orpo)"
            "DELETE_APPLICATION_ATTACHMENT" -> "Hakemuksen liitteen poisto"
            "DELETE_INCOME_STATEMENT_ATTACHMENT" -> "Tuloselvityksen liitteen poisto"
            "DELETE_INCOME_ATTACHMENT" -> "Tulon liitteen poisto"
            "DELETE_INVOICE_ATTACHMENT" -> "Laskun liitteen poisto"
            "DELETE_MESSAGE_CONTENT_ATTACHMENT" -> "Viestin liitteen poisto"
            "DELETE_PEDAGOGICAL_DOCUMENT_ATTACHMENT" -> "Pedagogisen dokumentoinnin liitteen poisto"
            "DELETE_FEE_ALTERATION_ATTACHMENTS" -> "Maksumuutoksen liitteen poisto"
            "UPDATE_RESERVATION" -> "Läsnäolovarauksen muokkaus"
            "READ_ABSENCE_APPLICATIONS" -> "Esiopetuksen poissaolohakemuksien luku"
            "CREATE_ABSENCE" -> "Poissaolon luonti"
            "READ_ABSENCES" -> "Poissaolojen luku"
            "READ_FUTURE_ABSENCES" -> "Tulevaisuuden poissaolojen luku"
            "READ_ATTENDANCE_REPORT" -> "Lapsen läsnä- ja poissaolotiedot -raportti"
            "DELETE_ABSENCE" -> "Poissaolon poisto"
            "DELETE_ABSENCE_RANGE" -> "Poissaolojen poisto"
            "READ_NON_RESERVABLE_RESERVATIONS" -> "Läsnäolovarauksien luku (ei-varattavat)"
            "UPDATE_NON_RESERVABLE_RESERVATIONS" -> "Läsnäolovarauksien muokkaus (ei-varattavat)"
            "DELETE_HOLIDAY_RESERVATIONS" -> "Läsnäolovarauksien poisto (loma-aika)"
            "READ_ADDITIONAL_INFO" -> "Lisätietojen luku"
            "UPDATE_ADDITIONAL_INFO" -> "Lisätietojen muokkaus"
            "READ_APPLICATION" -> "Hakemuksen luku"
            "READ_ASSISTANCE" -> "Tuen luku"
            "CREATE_ASSISTANCE_FACTOR" -> "Tuen kertoimen luonti"
            "READ_ASSISTANCE_FACTORS" -> "Tuen kertoimien luku"
            "CREATE_DAYCARE_ASSISTANCE" -> "Tuen taso varhaiskastuksessa luonti"
            "READ_DAYCARE_ASSISTANCES" -> "Tuen tasojen varhaiskasvatuksessa luku"
            "CREATE_PRESCHOOL_ASSISTANCE" -> "Tuen taso esiopetuksessa luonti"
            "READ_PRESCHOOL_ASSISTANCES" -> "Tuen tasojen esiopetuksessa luku"
            "CREATE_ASSISTANCE_ACTION" -> "Tukitoimen luonti"
            "READ_ASSISTANCE_ACTION" -> "Tukitoimien luku"
            "CREATE_OTHER_ASSISTANCE_MEASURE" -> "Muut toimet luonti"
            "READ_OTHER_ASSISTANCE_MEASURES" -> "Muut toimet luku"
            "CREATE_ASSISTANCE_NEED_DECISION" -> "Tuen päätös varhaiskasvatuksessa luonti"
            "READ_ASSISTANCE_NEED_DECISIONS" -> "Tuen päätöksien varhaiskasvatuksessa luku"
            "CREATE_ASSISTANCE_NEED_PRESCHOOL_DECISION" -> "Tuen päätös esiopetuksessa luonti"
            "READ_ASSISTANCE_NEED_PRESCHOOL_DECISIONS" -> "Tuen päätöksien esiopetuksessa luku"
            "READ_ASSISTANCE_NEED_VOUCHER_COEFFICIENTS" -> "Palvelusetelikertoimien luku"
            "CREATE_ASSISTANCE_NEED_VOUCHER_COEFFICIENT" -> "Palvelusetelikertoimen luonti"
            "CREATE_ATTENDANCE_RESERVATION" -> "Läsnäolovarauksen luonti"
            "UPSERT_CHILD_DATE_PRESENCE" -> "Lapsen paikallaolon muokkaus"
            "CREATE_BACKUP_CARE" -> "Varasijoituksen luonti"
            "READ_ONGOING_ATTENDANCE" -> "Läsnäolon luku (meneillään oleva)"
            "READ_BACKUP_CARE" -> "Varasijoituksen luku"
            "CREATE_BACKUP_PICKUP" -> "Varahakijan luonti"
            "READ_BACKUP_PICKUP" -> "Varahakijan luku"
            "CREATE_DAILY_NOTE" -> "Päivittäisen muistiinpanon luonti"
            "CREATE_STICKY_NOTE" -> "Lähipäivien huomion luonti"
            "READ_DAILY_SERVICE_TIMES" -> "Päivittäisten varhaiskasvatusaikojen luku"
            "CREATE_DAILY_SERVICE_TIME" -> "Päivittäisen varhaiskasvatusajan luonti"
            "READ_PLACEMENT" -> "Sijoituksen luku"
            "READ_SERVICE_APPLICATIONS" -> "Palvelutarpeen muutoshakemuksien luku"
            "READ_FAMILY_CONTACTS" -> "Perheen yhteystietojen luku"
            "UPDATE_FAMILY_CONTACT_DETAILS" -> "Perheen yhteystietojen muokkaus"
            "UPDATE_FAMILY_CONTACT_PRIORITY" -> "Perheen yhteystietojen järjestyksen muokkaus"
            "READ_GUARDIANS" -> "Huoltajien luku"
            "READ_BLOCKED_GUARDIANS" -> "Estettyjen huoltajien luku"
            "CREATE_FEE_ALTERATION" -> "Maksumuutoksen luonti"
            "READ_FEE_ALTERATIONS" -> "Maksumuutosten luku"
            "CREATE_CHILD_DOCUMENT" -> "Asiakirjan luonti"
            "READ_CHILD_DOCUMENT" -> "Asiakirjan luku"
            "CREATE_PEDAGOGICAL_DOCUMENT" -> "Pedagogisen dokumentoinnin luonti"
            "READ_PEDAGOGICAL_DOCUMENTS" -> "Pedagogisen dokumentoinnin luku"
            "READ_BASIC_INFO" -> "Perustietojen luku"
            "READ_SENSITIVE_INFO" -> "Arkaluontoisten tietojen luku"
            "UPLOAD_IMAGE" -> "Kuvan luonti"
            "DELETE_IMAGE" -> "Kuvan poisto"
            "DOWNLOAD" -> "Lataus"
            "DOWNLOAD_PDF" -> "Lataus (pdf)"
            "COPY" -> "Kopiointi"
            "FORCE_UNPUBLISH" -> "Julkaisun peruminen"
            "READ_DETAILS" -> "Tietojen luku"
            "UPDATE_GLOBAL_ROLES" -> "Roolien muokkaus"
            "UPDATE_DAYCARE_ROLES" -> "Yksikön roolien muokkaus"
            "DELETE_DAYCARE_ROLES" -> "Yksikön roolies poisto"
            "UPDATE_STAFF_ATTENDANCES" -> "Henkilöstön läsnäolojen muokkaus"
            "ACTIVATE" -> "Aktivointi"
            "DEACTIVATE" -> "Deaktivointi"
            "READ_OPEN_GROUP_ATTENDANCE" -> "Henkilöstön läsnäolojen luku (meneillään oleva)"
            "READ_OUT_OF_OFFICE" -> "Poissaoloviestin luku"
            "UPDATE_OUT_OF_OFFICE" -> "Poissaoloviestin luonti"
            "UPDATE_EMAIL" -> "Sähköpostin muokkaus"
            "IGNORE" -> "Ohitus"
            "UNIGNORE" -> "Ohituksen kumoaminen"
            "CREATE_ABSENCES" -> "Poissaolon luonti"
            "DELETE_ABSENCES" -> "Poissaolon poisto"
            "READ_STAFF_ATTENDANCES" -> "Henkilöstön läsnäolojen luku"
            "READ_CARETAKERS" -> "Henkilökunnan tarpeen luku"
            "CREATE_CARETAKERS" -> "Henkilökunnan tarpeen luonti"
            "UPDATE_CARETAKERS" -> "Henkilökunnan tarpeen muokkaus"
            "DELETE_CARETAKERS" -> "Henkilökunnan tarpeen poisto"
            "MARK_DEPARTURE" -> "Lähteväksi merkitseminen"
            "MARK_EXTERNAL_DEPARTURE" -> "Lähteväksi merkitseminen (muu henkilö)"
            "MARK_ARRIVAL" -> "Saapuneeksi merkitseminen"
            "MARK_EXTERNAL_ARRIVAL" -> "Saapuneeksi merkitseminen (muu henkilö)"
            "RECEIVE_PUSH_NOTIFICATIONS" -> "Ilmoitusten vastaanotto"
            "CREATE_CALENDAR_EVENT" -> "Tapahtuman luonti"
            "CREATE_CHILD_DOCUMENTS" -> "Lapsen asiakirjan luonti"
            "READ_CITIZEN_DOCUMENT_RESPONSE_REPORT" -> "Kuntalaisen asiakirjat -raportti"
            "NEKKU_MANUAL_ORDER" -> "Nekun lähetys"
            "UPDATE_HANDLED" -> "Käsittelyn muokkaus"
            "MARK_SENT" -> "Lähetetyksi merkitseminen"
            "RESEND" -> "Uudelleenlähetys"
            "UPDATE_NOTE" -> "Muistiinpanon muokkaus"
            "ACCESS" -> "Pääsy"
            "UPDATE_NAME" -> "Nimen muokkaus"
            "POST_RESPONSE" -> "Luonti"
            "DELETE_CONFLICTED_PARENTSHIP" -> "Konfliktaavien päämiehien poisto"
            "RETRY" -> "Uudelleenlähetys"
            "CONFIRM" -> "Vahvistus"
            "REVERT_TO_DRAFT" -> "Luonnokseksi palauttaminen"
            "CREATE_ATTACHMENT" -> "Liitteen luonti"
            "ADD_SSN" -> "Henkilötunnuksen lisäys"
            "CREATE_FINANCE_NOTE" -> "Talouden mustiinpanon luonti"
            "CREATE_FOSTER_PARENT_RELATIONSHIP" -> "Sijaisvanhemman luonti"
            "CREATE_INCOME" -> "Tulon luonti"
            "CREATE_INVOICE_CORRECTION" -> "Laskun korjausrivin luonti"
            "CREATE_PARENTSHIP" -> "Huoltajuuden luonti"
            "CREATE_PARTNERSHIP" -> "Puolisosuhteen luonti"
            "CREATE_REPLACEMENT_DRAFT_INVOICES" -> "Oikaisulaskuluonnosten luonti"
            "DOWNLOAD_ADDRESS_PAGE" -> "Osoitesivun lataus"
            "DISABLE_SSN_ADDING" -> "Henkilötunnuksen lisäys pois käytöstä"
            "ENABLE_SSN_ADDING" -> "Henkilötunnuksen lisäys käyttöön"
            "GENERATE_RETROACTIVE_FEE_DECISIONS" -> "Takautuvan maksupäätösluonnoksen luonti"
            "GENERATE_RETROACTIVE_VOUCHER_VALUE_DECISIONS" ->
                "Takautuvan arvopäätösluonnoksen luonti"
            "MERGE" -> "Yhdistäminen"
            "READ_APPLICATIONS" -> "Hakemuksen luku"
            "READ_CHILD_PLACEMENT_PERIODS" -> "Lapsen sijoituksien luku"
            "READ_DECISIONS" -> "Päätösten luku"
            "READ_FAMILY_OVERVIEW" -> "Perheen tietojen koosteen luku"
            "READ_FEE_DECISIONS" -> "Maksupäätösten luku"
            "READ_FINANCE_NOTES" -> "Talouden muistiinpanojen luku"
            "READ_FOSTER_CHILDREN" -> "Sijaislasten luku"
            "READ_FOSTER_PARENTS" -> "Sijaisvanhempien luku"
            "READ_INCOME" -> "Tulon luku"
            "READ_INCOME_STATEMENTS" -> "Tuloselvitysten luku"
            "READ_INCOME_NOTIFICATIONS" -> "Tuloilmoitusten luku"
            "READ_INVOICES" -> "Laskujen luku"
            "READ_INVOICE_ADDRESS" -> "Laskutusosoitteen luku"
            "READ_INVOICE_CORRECTIONS" -> "Laskun korjausrivien luku"
            "READ_OPH_OID" -> "OPH OID:n luku"
            "READ_PARENTSHIPS" -> "Päämiehien luku"
            "READ_PARTNERSHIPS" -> "Puolisoiden luku"
            "READ_DEPENDANTS" -> "Huollettavien luku"
            "READ_TIMELINE" -> "Aikajana luku"
            "READ_VOUCHER_VALUE_DECISIONS" -> "Arvopäätöksien luku"
            "UPDATE_EVAKA_RIGHTS" -> "eVaka-oikeuksien muokkaus"
            "UPDATE_INVOICE_ADDRESS" -> "Laskutusosoitteen muokkaus"
            "UPDATE_OPH_OID" -> "OPH OID:n muokkaus"
            "UPDATE_PERSONAL_DETAILS" -> "Henkilötietojen muokkaus"
            "UPDATE_FROM_VTJ" -> "Henkilötietojen vtj-pävitys"
            "CREATE_GROUP_PLACEMENT" -> "Ryhmäsijoituksen luonti"
            "CREATE_SERVICE_NEED" -> "Palvelutarpeen luonti"
            "ACCEPT" -> "Hyväksyminen"
            "REJECT" -> "Hylkääminen"
            "READ_SERVICE_WORKER_NOTE" -> "Palveluohjauksen muistiinpanon luku"
            "SET_SERVICE_WORKER_NOTE" -> "Palveluohjauksen muistiinpanon muokkaus"
            "READ_GROUP_DETAILS" -> "Ryhmän tietojen luku"
            "READ_ATTENDANCES" -> "Läsnäolojen luku"
            "READ_APPLICATIONS_AND_PLACEMENT_PLANS" -> "Hakemuksien ja sijoitushahmotelmien luku"
            "READ_TRANSFER_APPLICATIONS" -> "Siirtoa muualle hakeneiden luku"
            "READ_GROUPS" -> "Ryhmien luku"
            "READ_CHILD_CAPACITY_FACTORS" -> "Lapsen kertoimen luku"
            "READ_CHILD_ATTENDANCES" -> "Lasten läsnäolojen luku"
            "READ_CHILD_RESERVATIONS" -> "Lasten läsnäolovarausten luku"
            "READ_UNIT_RESERVATION_STATISTICS" -> "Yksikön läsnäolojen tilastojen luku"
            "UPDATE_CHILD_ATTENDANCES" -> "Lasten läsnäolojen muokkaus"
            "READ_REALTIME_STAFF_ATTENDANCES" -> "Henkilöstön läsnäolojen luku"
            "READ_STAFF_OCCUPANCY_COEFFICIENTS" -> "Henkilöstön täyttö/käyttöasteen kertoimien luku"
            "UPSERT_STAFF_OCCUPANCY_COEFFICIENTS" ->
                "Henkilöstön täyttö/käyttöasteen kertoimien muokkaus"
            "READ_OCCUPANCIES" -> "Täyttö/käyttöasteen luku"
            "READ_ATTENDANCE_RESERVATIONS" -> "Läsnäolovarausten luku"
            "CREATE_PLACEMENT" -> "Sijoituksen luonti"
            "ACCEPT_PLACEMENT_PROPOSAL" -> "Sijoitushahmotelman hyväksyminen"
            "CREATE_GROUP" -> "Ryhmän luonti"
            "READ_MOBILE_STATS" -> "Mobiilin tilastojen luku"
            "READ_MOBILE_INFO" -> "Mobiilin tietojen luku"
            "READ_MOBILE_DEVICES" -> "Mobiililaitteiden luku"
            "CREATE_MOBILE_DEVICE_PAIRING" -> "Mobiililaitteen parituksen luonti"
            "READ_ACL" -> "Käyttöoikeuksien luku"
            "INSERT_ACL_UNIT_SUPERVISOR" -> "Johtajan käyttöoikeuden luonti"
            "UPDATE_ACL_UNIT_SUPERVISOR" -> "Johtajan käyttöoikeuden muokkaus"
            "INSERT_ACL_SPECIAL_EDUCATION_TEACHER" -> "Erityisopettajan käyttöoikeuden luonti"
            "UPDATE_ACL_SPECIAL_EDUCATION_TEACHER" -> "Erityisopettajan käyttöoikeuden muokkaus"
            "INSERT_ACL_EARLY_CHILDHOOD_EDUCATION_SECRETARY" ->
                "Varhaiskasvatussihteerin käyttöoikeuden luonti"
            "UPDATE_ACL_EARLY_CHILDHOOD_EDUCATION_SECRETARY" ->
                "Varhaiskasvatussihteerin käyttöoikeuden muokkaus"
            "INSERT_ACL_STAFF" -> "Henkilökunnan käyttöoikeuden luonti"
            "UPDATE_ACL_STAFF" -> "Henkilökunnan käyttöoikeuden muokkaus"
            "UPDATE_ACL_SCHEDULED" -> "Käyttöoikeuden muokkaus (ajastettu)"
            "UPDATE_STAFF_GROUP_ACL" -> "Ryhmän käyttöoikeuden muokkaus"
            "READ_APPLICATIONS_REPORT" -> "Saapuneet hakemukset -raportti"
            "READ_PLACEMENT_GUARANTEE_REPORT" -> "Varhaiskasvatuspaikkatakuu-raportti"
            "READ_ASSISTANCE_NEEDS_AND_ACTIONS_REPORT" ->
                "Lasten tuen tarpeet ja tukitoimet -raportti"
            "READ_ASSISTANCE_NEEDS_AND_ACTIONS_REPORT_BY_CHILD" ->
                "Lasten tuen tarpeet ja tukitoimet -raportti (lapsittain)"
            "READ_ATTENDANCE_RESERVATION_REPORT" ->
                "Päiväkohtaiset lapsen tulo- ja lähtöajat -raportti"
            "READ_CHILD_AGE_AND_LANGUAGE_REPORT" -> "Lasten kielet ja iät yksiköissä -raportti"
            "READ_CHILD_IN_DIFFERENT_ADDRESS_REPORT" -> "Lapsi eri osoitteessa -raportti"
            "READ_EXCEEDED_SERVICE_NEEDS_REPORT" -> "Palveluntarpeen ylitykset -raportti"
            "READ_FAMILY_CONFLICT_REPORT" -> "Perhekonfliktit-raportti"
            "READ_FAMILY_CONTACT_REPORT" -> "Perheen yhteystietokooste -raportti"
            "READ_FAMILY_DAYCARE_MEAL_REPORT" -> "Perhepäivähoidossa olevien lasten ateriaraportti"
            "READ_MISSING_SERVICE_NEED_REPORT" -> "Puuttuvat palveluntarpeet -raportti"
            "READ_OCCUPANCY_REPORT" -> "Täyttö- ja käyttöasteet -raportti"
            "READ_PARTNERS_IN_DIFFERENT_ADDRESS_REPORT" -> "Puoliso eri osoitteessa -raportti"
            "READ_SERVICE_NEED_REPORT" -> "Lasten palvelutarpeet ja iät yksiköissä -raportti"
            "READ_NEKKU_ORDER_REPORT" -> "Nekku tilaukset -raportti"
            "READ_SERVICE_VOUCHER_REPORT" -> "Palveluseteliyksiköt-raportti"
            "READ_SERVICE_VOUCHER_VALUES_REPORT" -> "Palvelusetelilapset yksikössä -raportti"
            "UPDATE_FEATURES" -> "Yksikön toimintojen muokkaus"
            "READ_UNREAD_MESSAGES" -> "Lukemattomien viestien luku"
            "READ_TERMINATED_PLACEMENTS" -> "Irtisanotun sijoituksen luku"
            "READ_MISSING_GROUP_PLACEMENTS" -> "Ryhmää odottavien lasten luku"
            "READ_CALENDAR_EVENTS" -> "Tapahtumien luku"
            "CREATE_TEMPORARY_EMPLOYEE" -> "Tilapäisen sijaisen luonti"
            "READ_TEMPORARY_EMPLOYEE" -> "Tilapäisen sijaisen luku"
            "UPDATE_TEMPORARY_EMPLOYEE" -> "Tilapäisen sijaisen muokkaus"
            "DELETE_TEMPORARY_EMPLOYEE" -> "Tilapäisen sijaisen poisto"
            "READ_MEAL_REPORT" -> "Ruokailijamäärät-raportti"
            "READ_PRESCHOOL_ABSENCE_REPORT_FOR_UNIT" -> "Esiopetuksen poissaoloraportti"
            "READ_CHILD_DOCUMENTS_REPORT" -> "Pedagogiset asiakirjat -raportti"
            "READ_PRESCHOOL_APPLICATION_REPORT" -> "Ehdottava EO-raportti"
            "READ_HOLIDAY_PERIOD_ATTENDANCE_REPORT" -> "Lomakyselyraportti"
            "READ_HOLIDAY_QUESTIONNAIRE_REPORT" -> "Poissaolokyselyraportti"
            "READ_STARTING_PLACEMENTS_REPORT" -> "Varhaiskasvatuksessa aloittavat lapset"
            "READ_STAFF_EMPLOYEE_NUMBER" -> "Työntekijän henkilönumeron luku"
            "READ_ACCEPTED_DECISIONS" -> "Hyväksyttyjen päätöksien luku"
            "DOWNLOAD_VERSION" -> "Version lataus"
            "PUBLISH" -> "Julkaisu"
            "NEXT_STATUS" -> "Seuraavaan tilaan siirtäminen"
            "PREV_STATUS" -> "Edelliseen tilaan siirtäminen"
            "PROPOSE_DECISION" -> "Päätöksen ehdotus"
            "ANNUL_DECISION" -> "Päätöksen mitätöinti"
            "UPDATE_DECISION_VALIDITY" -> "Päätöksen voimassaoloajan muokkaus"
            "ARCHIVE" -> "Arkistointi"
            "UPDATE_PLACEMENT_DRAFT" -> "Sijoitushahmotelman muokkaus"
            "READ_SERVICE_WORKER_ACCOUNT_MESSAGES" -> "Palveluohjauksen viestien luku"
            "CREATE_CHILD_DECISION_DOCUMENT" -> "Muun päätöksen luonti"
            "READ_TITANIA_ERRORS" -> "Titania-virheet -raportti"
            "READ_SERVICE_NEEDS" -> "Palvelutarpeiden luku"
            else -> throw error("Unsupported entry ${entry.name}")
        }

private data class Row(
    val typeName: String,
    val action: Pair<String, String>,
    val permissions: Map<UserRole, String>,
) {
    fun toCsvRow() =
        CsvRow(
            typeName = typeName,
            actionName = action.second,
            action = action.first,
            admin = permissions.getValue(UserRole.ADMIN),
            reportViewer = permissions.getValue(UserRole.REPORT_VIEWER),
            director = permissions.getValue(UserRole.DIRECTOR),
            financeAdmin = permissions.getValue(UserRole.FINANCE_ADMIN),
            financeStaff = permissions.getValue(UserRole.FINANCE_STAFF),
            serviceWorker = permissions.getValue(UserRole.SERVICE_WORKER),
            messaging = permissions.getValue(UserRole.MESSAGING),
            unitSupervisor = permissions.getValue(UserRole.UNIT_SUPERVISOR),
            staff = permissions.getValue(UserRole.STAFF),
            specialEducationTeacher = permissions.getValue(UserRole.SPECIAL_EDUCATION_TEACHER),
            earlyChildhoodEducationSecretary =
                permissions.getValue(UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY),
        )
}

private fun actionType(row: Row) =
    when {
        row.action.first.contains("_PAGE") || row.action.second.contains("-sivu") -> 0
        row.action.first.contains("_REPORT") || row.action.second.contains("-raportti") -> 1
        else -> Int.MAX_VALUE
    }
