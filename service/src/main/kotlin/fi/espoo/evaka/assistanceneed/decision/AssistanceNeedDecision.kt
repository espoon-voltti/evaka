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
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.json.Json
import java.time.LocalDate

data class AssistanceNeedDecision(
    val id: AssistanceNeedDecisionId? = null,
    val decisionNumber: Long? = null,
    val childId: ChildId,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val status: AssistanceNeedDecisionStatus,

    val language: AssistanceNeedDecisionLanguage,
    val decisionMade: LocalDate?,
    val sentForDecision: LocalDate?,
    val selectedUnit: DaycareId?,
    @Nested("preparer_1")
    val preparedBy1: AssistanceNeedDecisionEmployee?,
    @Nested("preparer_2")
    val preparedBy2: AssistanceNeedDecisionEmployee?,
    @Nested("decision_maker")
    val decisionMaker: AssistanceNeedDecisionEmployee?,

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

enum class AssistanceNeedDecisionStatus {
    DRAFT, NEEDS_WORK, ACCEPTED, REJECTED
}

enum class AssistanceNeedDecisionLanguage {
    FI, SV
}

data class AssistanceNeedDecisionEmployee(
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
