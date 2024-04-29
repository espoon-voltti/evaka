// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoo.bi

import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.application.ApplicationOrigin
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.assistance.DaycareAssistanceLevel
import fi.espoo.evaka.assistance.OtherAssistanceMeasureType
import fi.espoo.evaka.assistance.PreschoolAssistanceLevel
import fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionStatus
import fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionType
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.decision.DecisionStatus
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.OfficialLanguage
import fi.espoo.evaka.vasu.CurriculumType
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class BiArea(
    val id: UUID,
    val created: HelsinkiDateTime,
    val updated: HelsinkiDateTime,
    val name: String
)

data class BiUnit(
    val id: UUID,
    val created: HelsinkiDateTime,
    val updated: HelsinkiDateTime,
    val area: UUID,
    val name: String,
    val providerType: ProviderType,
    val costCenter: String?,
    val club: Boolean,
    val daycare: BiUnitDaycareType?,
    val preschool: Boolean,
    val preparatoryEducation: Boolean,
    val openingDate: LocalDate?,
    val closingDate: LocalDate?,
    val language: Language,
    val unitManagerName: String?,
    val roundTheClock: Boolean,
)

enum class BiUnitDaycareType {
    DAYCARE,
    FAMILY,
    GROUP_FAMILY,
}

data class BiGroup(
    val id: UUID,
    val unit: UUID,
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
)

data class BiChild(
    val id: UUID,
    val created: HelsinkiDateTime,
    val updated: HelsinkiDateTime,
    val firstName: String,
    val lastName: String,
    val birthDate: LocalDate,
    val language: String?,
    val languageAtHome: String,
    val vtjNonDisclosure: Boolean,
    val postalCode: String,
    val postOffice: String,
)

data class BiPlacement(
    val id: UUID,
    val created: HelsinkiDateTime,
    val updated: HelsinkiDateTime,
    val child: UUID,
    val unit: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isBackup: Boolean,
    val type: PlacementType?,
)

data class BiGroupPlacement(
    val id: UUID,
    val created: HelsinkiDateTime,
    val updated: HelsinkiDateTime,
    val placement: UUID,
    val group: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate,
)

data class BiAbsence(
    val id: UUID,
    val updated: HelsinkiDateTime,
    val child: UUID,
    val date: LocalDate,
    val category: AbsenceCategory,
)

data class BiGroupCaretakerAllocation(
    val id: UUID,
    val created: HelsinkiDateTime,
    val updated: HelsinkiDateTime,
    val group: UUID,
    val amount: BigDecimal,
    val startDate: LocalDate,
    val endDate: LocalDate?
)

data class BiApplication(
    val id: UUID,
    val created: HelsinkiDateTime,
    val updated: HelsinkiDateTime,
    val type: ApplicationType,
    val transferApplication: Boolean,
    val origin: ApplicationOrigin,
    val status: ApplicationStatus,
    val additionalDaycareApplication: Boolean,
    val sentDate: LocalDate,
    val dueDate: LocalDate?,
    val preferredUnits: List<UUID>,
    val preferredStartDate: LocalDate,
    val urgent: Boolean?,
    val assistanceNeeded: Boolean?,
    val shiftCare: Boolean?,
)

data class BiDecision(
    val id: UUID,
    val created: HelsinkiDateTime,
    val updated: HelsinkiDateTime,
    val application: UUID,
    val unit: UUID,
    val sentDate: LocalDate?,
    val status: DecisionStatus,
    val type: DecisionType,
    val startDate: LocalDate,
    val endDate: LocalDate,
)

data class BiServiceNeedOption(
    val id: UUID,
    val created: HelsinkiDateTime,
    val updated: HelsinkiDateTime,
    val name: String,
    val validPlacementType: PlacementType,
    val defaultOption: Boolean,
    val occupancyCoefficient: BigDecimal,
    val occupancyCoefficientUnder3Y: BigDecimal,
)

data class BiServiceNeed(
    val id: UUID,
    val created: HelsinkiDateTime,
    val updated: HelsinkiDateTime,
    val option: UUID,
    val placement: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val shiftCare: Boolean,
)

data class BiFeeDecision(
    val id: UUID,
    val created: HelsinkiDateTime,
    val updated: HelsinkiDateTime,
    val decisionNumber: Long?,
    val status: FeeDecisionStatus,
    val type: FeeDecisionType,
    val familySize: Int,
    val validFrom: LocalDate,
    val validTo: LocalDate,
)

data class BiFeeDecisionChild(
    val id: UUID,
    val created: HelsinkiDateTime,
    val updated: HelsinkiDateTime,
    val feeDecision: UUID,
    val child: UUID,
    val placementUnit: UUID,
    val serviceNeedOption: UUID?,
    val serviceNeedDescription: String,
    val finalFee: Int,
)

data class BiVoucherValueDecision(
    val id: UUID,
    val created: HelsinkiDateTime,
    val updated: HelsinkiDateTime,
    val decisionNumber: Long?,
    val status: FeeDecisionStatus,
    val type: FeeDecisionType,
    val familySize: Int,
    val validFrom: LocalDate,
    val validTo: LocalDate,
    val placementUnit: UUID,
    val child: UUID,
    val serviceNeedFeeDescription: String,
    val serviceNeedVoucherValueDescription: String,
    val finalCoPayment: Int,
)

data class BiCurriculumTemplate(
    val id: UUID,
    val created: HelsinkiDateTime,
    val updated: HelsinkiDateTime,
    val validFrom: LocalDate,
    val validTo: LocalDate,
    val type: CurriculumType,
    val language: OfficialLanguage,
    val name: String,
)

data class BiCurriculumDocument(
    val id: UUID,
    val created: HelsinkiDateTime,
    val updated: HelsinkiDateTime,
    val child: UUID,
    val template: UUID,
)

data class BiPedagogicalDocument(
    val id: UUID,
    val created: HelsinkiDateTime,
    val updated: HelsinkiDateTime,
    val child: UUID,
)

data class BiAssistanceFactor(
    val id: UUID,
    val created: HelsinkiDateTime,
    val updated: HelsinkiDateTime,
    val child: UUID,
    val capacityFactor: BigDecimal,
    val startDate: LocalDate,
    val endDate: LocalDate,
)

data class BiDaycareAssistanceEntry(
    val id: UUID,
    val created: HelsinkiDateTime,
    val updated: HelsinkiDateTime,
    val child: UUID,
    val level: DaycareAssistanceLevel,
    val startDate: LocalDate,
    val endDate: LocalDate,
)

data class BiPreschoolAssistanceEntry(
    val id: UUID,
    val created: HelsinkiDateTime,
    val updated: HelsinkiDateTime,
    val child: UUID,
    val level: PreschoolAssistanceLevel,
    val startDate: LocalDate,
    val endDate: LocalDate,
)

data class BiOtherAssistanceMeasureEntry(
    val id: UUID,
    val created: HelsinkiDateTime,
    val updated: HelsinkiDateTime,
    val child: UUID,
    val type: OtherAssistanceMeasureType,
    val startDate: LocalDate,
    val endDate: LocalDate,
)

data class BiAssistanceNeedVoucherCoefficient(
    val id: UUID,
    val created: HelsinkiDateTime,
    val updated: HelsinkiDateTime,
    val child: UUID,
    val coefficient: BigDecimal,
    val startDate: LocalDate,
    val endDate: LocalDate,
)

data class BiAssistanceAction(
    val id: UUID,
    val created: HelsinkiDateTime,
    val updated: HelsinkiDateTime,
    val child: UUID,
    val hasOtherAction: Boolean,
    val startDate: LocalDate,
    val endDate: LocalDate,
)

data class BiAssistanceActionOptionRef(
    val action: UUID,
    val option: String,
)

data class BiAssistanceNeedDaycareDecision(
    val id: UUID,
    val created: HelsinkiDateTime,
    val updated: HelsinkiDateTime,
    val child: UUID,
    val unit: UUID?,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
    val status: AssistanceNeedDecisionStatus,
    val assistanceEnds: Boolean,
    val assistanceServicesForTime: Boolean,
    val enhancedAssistance: Boolean,
    val specialAssistance: Boolean,
)

data class BiAssistanceNeedPreschoolDecision(
    val id: UUID,
    val created: HelsinkiDateTime,
    val updated: HelsinkiDateTime,
    val child: UUID,
    val unit: UUID?,
    val type: AssistanceNeedPreschoolDecisionType?,
    val validFrom: LocalDate?,
    val status: AssistanceNeedDecisionStatus,
)
