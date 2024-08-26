// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed.decision

import fi.espoo.evaka.shared.AssistanceNeedDecisionGuardianId
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.OfficialLanguage
import java.time.LocalDate
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.mapper.PropagateNull
import org.jdbi.v3.json.Json

data class AssistanceNeedDecision(
    val id: AssistanceNeedDecisionId,
    val decisionNumber: Long? = null,
    @Nested("child") val child: AssistanceNeedDecisionChild?,
    val validityPeriod: DateRange,
    val status: AssistanceNeedDecisionStatus,
    val language: OfficialLanguage,
    val decisionMade: LocalDate?,
    val sentForDecision: LocalDate?,
    @Nested("selected_unit") val selectedUnit: UnitInfo?,
    @Nested("preparer_1") val preparedBy1: AssistanceNeedDecisionEmployee?,
    @Nested("preparer_2") val preparedBy2: AssistanceNeedDecisionEmployee?,
    @Nested("decision_maker") val decisionMaker: AssistanceNeedDecisionMaker?,
    val pedagogicalMotivation: String?,
    @Nested("structural_motivation_opt")
    val structuralMotivationOptions: StructuralMotivationOptions,
    val structuralMotivationDescription: String?,
    val careMotivation: String?,
    @Nested("service_opt") val serviceOptions: ServiceOptions,
    val servicesMotivation: String?,
    val expertResponsibilities: String?,
    val guardiansHeardOn: LocalDate?,
    @Json val guardianInfo: Set<AssistanceNeedDecisionGuardian>,
    val viewOfGuardians: String?,
    val otherRepresentativeHeard: Boolean,
    val otherRepresentativeDetails: String?,
    val assistanceLevels: Set<AssistanceLevel>,
    val motivationForDecision: String?,
    val annulmentReason: String,
    val hasDocument: Boolean,
) {
    fun toForm() =
        AssistanceNeedDecisionForm(
            decisionNumber,
            validityPeriod,
            status,
            language,
            decisionMade,
            sentForDecision,
            selectedUnit = selectedUnit?.toForm(),
            preparedBy1 = preparedBy1?.toForm(),
            preparedBy2 = preparedBy2?.toForm(),
            decisionMaker = decisionMaker?.toForm(),
            pedagogicalMotivation,
            structuralMotivationOptions,
            structuralMotivationDescription,
            careMotivation,
            serviceOptions,
            servicesMotivation,
            expertResponsibilities,
            guardiansHeardOn,
            guardianInfo,
            viewOfGuardians,
            otherRepresentativeHeard,
            otherRepresentativeDetails,
            assistanceLevels,
            motivationForDecision,
        )

    override fun toString(): String = "**REDACTED**"
}

data class AssistanceNeedDecisionForm(
    val decisionNumber: Long? = null,
    val validityPeriod: DateRange,
    val status: AssistanceNeedDecisionStatus,
    val language: OfficialLanguage,
    val decisionMade: LocalDate?,
    val sentForDecision: LocalDate?,
    @Nested("selected_unit") val selectedUnit: UnitIdInfo?,
    @Nested("preparer_1") val preparedBy1: AssistanceNeedDecisionEmployeeForm?,
    @Nested("preparer_2") val preparedBy2: AssistanceNeedDecisionEmployeeForm?,
    @Nested("decision_maker") val decisionMaker: AssistanceNeedDecisionMakerForm?,
    val pedagogicalMotivation: String?,
    @Nested("structural_motivation_opt")
    val structuralMotivationOptions: StructuralMotivationOptions,
    val structuralMotivationDescription: String?,
    val careMotivation: String?,
    @Nested("service_opt") val serviceOptions: ServiceOptions,
    val servicesMotivation: String?,
    val expertResponsibilities: String?,
    val guardiansHeardOn: LocalDate?,
    @Json val guardianInfo: Set<AssistanceNeedDecisionGuardian>,
    val viewOfGuardians: String?,
    val otherRepresentativeHeard: Boolean,
    val otherRepresentativeDetails: String?,
    val assistanceLevels: Set<AssistanceLevel>,
    val motivationForDecision: String?,
) {
    override fun toString(): String = "**REDACTED**"
}

data class AssistanceNeedDecisionBasics(
    val id: AssistanceNeedDecisionId,
    val validityPeriod: DateRange,
    val status: AssistanceNeedDecisionStatus,
    val decisionMade: LocalDate?,
    val sentForDecision: LocalDate?,
    @Nested("selected_unit") val selectedUnit: UnitInfoBasics?,
    val created: HelsinkiDateTime,
)

enum class AssistanceNeedDecisionStatus : DatabaseEnum {
    DRAFT,
    NEEDS_WORK,
    ACCEPTED,
    REJECTED,
    ANNULLED;

    override val sqlType: String = "assistance_need_decision_status"

    fun isDecided() = this in listOf(ACCEPTED, REJECTED, ANNULLED)
}

@Deprecated(
    message = "use OfficialLanguage instead",
    replaceWith =
        ReplaceWith("OfficialLanguage", imports = ["fi.espoo.evaka.shared.domain.OfficialLanguage"]),
)
typealias AssistanceNeedDecisionLanguage = OfficialLanguage

data class AssistanceNeedDecisionEmployee(
    @PropagateNull val employeeId: EmployeeId?,
    val title: String?,
    val name: String? = null,
    val phoneNumber: String?,
) {
    fun toForm() = AssistanceNeedDecisionEmployeeForm(employeeId, title, phoneNumber)
}

data class AssistanceNeedDecisionEmployeeForm(
    @PropagateNull val employeeId: EmployeeId?,
    val title: String?,
    val phoneNumber: String?,
)

data class AssistanceNeedDecisionMaker(
    @PropagateNull val employeeId: EmployeeId?,
    val title: String?,
    val name: String? = null,
) {
    fun toForm() = AssistanceNeedDecisionMakerForm(employeeId, title)
}

data class AssistanceNeedDecisionMakerForm(
    @PropagateNull val employeeId: EmployeeId?,
    val title: String?,
)

data class StructuralMotivationOptions(
    val smallerGroup: Boolean,
    val specialGroup: Boolean,
    val smallGroup: Boolean,
    val groupAssistant: Boolean,
    val childAssistant: Boolean,
    val additionalStaff: Boolean,
)

data class ServiceOptions(
    val consultationSpecialEd: Boolean,
    val partTimeSpecialEd: Boolean,
    val fullTimeSpecialEd: Boolean,
    val interpretationAndAssistanceServices: Boolean,
    val specialAides: Boolean,
)

data class AssistanceNeedDecisionGuardian(
    val id: AssistanceNeedDecisionGuardianId? = null,
    val personId: PersonId?,
    val name: String,
    val isHeard: Boolean,
    val details: String?,
)

enum class AssistanceLevel {
    ASSISTANCE_ENDS,
    ASSISTANCE_SERVICES_FOR_TIME,
    ENHANCED_ASSISTANCE,
    SPECIAL_ASSISTANCE,
}

data class UnitInfo(
    @PropagateNull val id: DaycareId?,
    val name: String? = null,
    val streetAddress: String? = null,
    val postalCode: String? = null,
    val postOffice: String? = null,
) {
    fun toForm() = UnitIdInfo(id)
}

data class UnitIdInfo(val id: DaycareId?)

data class UnitInfoBasics(@PropagateNull val id: DaycareId?, val name: String? = null)

data class AssistanceNeedDecisionChild(
    @PropagateNull val id: ChildId?,
    val name: String?,
    val dateOfBirth: LocalDate?,
)

data class AssistanceNeedDecisionCitizenListItem(
    val id: AssistanceNeedDecisionId,
    val childId: ChildId,
    val validityPeriod: DateRange,
    val status: AssistanceNeedDecisionStatus,
    val decisionMade: LocalDate,
    @Nested("selected_unit") val selectedUnit: UnitInfoBasics?,
    val assistanceLevels: Set<AssistanceLevel>,
    val annulmentReason: String,
    val isUnread: Boolean,
)

data class UnreadAssistanceNeedDecisionItem(val childId: ChildId, val count: Int)
