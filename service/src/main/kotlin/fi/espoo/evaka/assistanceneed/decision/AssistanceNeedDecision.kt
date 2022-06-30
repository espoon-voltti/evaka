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
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.mapper.PropagateNull
import org.jdbi.v3.json.Json
import java.time.LocalDate

data class AssistanceNeedDecision(
    val id: AssistanceNeedDecisionId,
    val decisionNumber: Long? = null,
    @Nested("child")
    val child: AssistanceNeedDecisionChild?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val status: AssistanceNeedDecisionStatus,

    val language: AssistanceNeedDecisionLanguage,
    val decisionMade: LocalDate?,
    val sentForDecision: LocalDate?,
    @Nested("selected_unit")
    val selectedUnit: UnitInfo?,
    @Nested("preparer_1")
    val preparedBy1: AssistanceNeedDecisionEmployee?,
    @Nested("preparer_2")
    val preparedBy2: AssistanceNeedDecisionEmployee?,
    @Nested("decision_maker")
    val decisionMaker: AssistanceNeedDecisionMaker?,

    val pedagogicalMotivation: String?,
    @Nested("structural_motivation_opt")
    val structuralMotivationOptions: StructuralMotivationOptions,
    val structuralMotivationDescription: String?,
    val careMotivation: String?,
    @Nested("service_opt")
    val serviceOptions: ServiceOptions,
    val servicesMotivation: String?,
    val expertResponsibilities: String?,
    val guardiansHeardOn: LocalDate?,
    @Json
    val guardianInfo: Set<AssistanceNeedDecisionGuardian>,
    val viewOfGuardians: String?,
    val otherRepresentativeHeard: Boolean,
    val otherRepresentativeDetails: String?,

    val assistanceLevel: AssistanceLevel?,
    val assistanceServicesTime: FiniteDateRange?,
    val motivationForDecision: String?
) {
    fun toForm() = AssistanceNeedDecisionForm(
        decisionNumber,
        startDate,
        endDate,
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
        assistanceLevel,
        assistanceServicesTime,
        motivationForDecision
    )
}

data class AssistanceNeedDecisionForm(
    val decisionNumber: Long? = null,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val status: AssistanceNeedDecisionStatus,

    val language: AssistanceNeedDecisionLanguage,
    val decisionMade: LocalDate?,
    val sentForDecision: LocalDate?,
    @Nested("selected_unit")
    val selectedUnit: UnitIdInfo?,
    @Nested("preparer_1")
    val preparedBy1: AssistanceNeedDecisionEmployeeForm?,
    @Nested("preparer_2")
    val preparedBy2: AssistanceNeedDecisionEmployeeForm?,
    @Nested("decision_maker")
    val decisionMaker: AssistanceNeedDecisionMakerForm?,

    val pedagogicalMotivation: String?,
    @Nested("structural_motivation_opt")
    val structuralMotivationOptions: StructuralMotivationOptions,
    val structuralMotivationDescription: String?,
    val careMotivation: String?,
    @Nested("service_opt")
    val serviceOptions: ServiceOptions,
    val servicesMotivation: String?,
    val expertResponsibilities: String?,
    val guardiansHeardOn: LocalDate?,
    @Json
    val guardianInfo: Set<AssistanceNeedDecisionGuardian>,
    val viewOfGuardians: String?,
    val otherRepresentativeHeard: Boolean,
    val otherRepresentativeDetails: String?,

    val assistanceLevel: AssistanceLevel?,
    val assistanceServicesTime: FiniteDateRange?,
    val motivationForDecision: String?
)

data class AssistanceNeedDecisionBasics(
    val id: AssistanceNeedDecisionId,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val status: AssistanceNeedDecisionStatus,

    val decisionMade: LocalDate?,
    val sentForDecision: LocalDate?,
    @Nested("selected_unit")
    val selectedUnit: UnitInfoBasics?,

    val created: HelsinkiDateTime
)

enum class AssistanceNeedDecisionStatus {
    DRAFT, NEEDS_WORK, ACCEPTED, REJECTED
}

enum class AssistanceNeedDecisionLanguage {
    FI, SV
}

data class AssistanceNeedDecisionEmployee(
    @PropagateNull
    val employeeId: EmployeeId?,
    val title: String?,
    val name: String? = null,
    val email: String? = null,
    val phoneNumber: String?
) {
    fun toForm() = AssistanceNeedDecisionEmployeeForm(employeeId, title, phoneNumber)
}

data class AssistanceNeedDecisionEmployeeForm(
    @PropagateNull
    val employeeId: EmployeeId?,
    val title: String?,
    val phoneNumber: String?
)

data class AssistanceNeedDecisionMaker(
    @PropagateNull
    val employeeId: EmployeeId?,
    val title: String?,
    val name: String? = null
) {
    fun toForm() = AssistanceNeedDecisionMakerForm(employeeId, title)
}

data class AssistanceNeedDecisionMakerForm(
    @PropagateNull
    val employeeId: EmployeeId?,
    val title: String?
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
    val details: String?
)

enum class AssistanceLevel {
    ASSISTANCE_ENDS, ASSISTANCE_SERVICES_FOR_TIME, ENHANCED_ASSISTANCE, SPECIAL_ASSISTANCE
}

data class UnitInfo(
    @PropagateNull
    val id: DaycareId?,
    val name: String? = null,
    val streetAddress: String? = null,
    val postalCode: String? = null,
    val postOffice: String? = null
) {
    fun toForm() = UnitIdInfo(id)
}

data class UnitIdInfo(
    val id: DaycareId?
)

data class UnitInfoBasics(
    @PropagateNull
    val id: DaycareId?,
    val name: String? = null
)

data class AssistanceNeedDecisionChild(
    @PropagateNull
    val id: ChildId?,
    val name: String?
)
