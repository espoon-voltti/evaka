// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.serviceneed.application

import evaka.core.placement.PlacementType
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.EmployeeId
import evaka.core.shared.PersonId
import evaka.core.shared.PlacementId
import evaka.core.shared.ServiceApplicationId
import evaka.core.shared.ServiceNeedOptionId
import evaka.core.shared.db.DatabaseEnum
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.mapper.PropagateNull

enum class ServiceApplicationDecisionStatus : DatabaseEnum {
    ACCEPTED,
    REJECTED;

    override val sqlType: String = "service_application_decision_status"
}

data class ServiceNeedOptionBasics(
    val id: ServiceNeedOptionId,
    val nameFi: String,
    val nameSv: String,
    val nameEn: String,
    val validPlacementType: PlacementType,
    val partWeek: Boolean?,
    val validity: DateRange,
)

data class ServiceApplicationDecision(
    @PropagateNull val status: ServiceApplicationDecisionStatus,
    val decidedBy: EmployeeId,
    val decidedByName: String,
    val decidedAt: HelsinkiDateTime,
    val rejectedReason: String?,
)

data class ServiceApplicationPlacement(
    @PropagateNull val id: PlacementId,
    val type: PlacementType,
    val endDate: LocalDate,
    val unitId: DaycareId,
)

data class ServiceApplication(
    val id: ServiceApplicationId,
    val sentAt: HelsinkiDateTime,
    val personId: PersonId,
    val personName: String,
    val childId: ChildId,
    val childName: String,
    val startDate: LocalDate,
    @Nested("service_need_option") val serviceNeedOption: ServiceNeedOptionBasics,
    val additionalInfo: String,
    @Nested("decision") val decision: ServiceApplicationDecision?,
    @Nested("current_placement") val currentPlacement: ServiceApplicationPlacement?,
)

data class UndecidedServiceApplicationSummary(
    val id: ServiceApplicationId,
    val sentAt: HelsinkiDateTime,
    val childId: ChildId,
    val childName: String,
    val startDate: LocalDate,
    val placementEndDate: LocalDate,
    val currentNeed: String?,
    val newNeed: String,
)

fun isPlacementTypeChangeAllowed(old: PlacementType, new: PlacementType): Boolean {
    if (old == new) return true

    val set = setOf(old, new)
    if (set == setOf(PlacementType.DAYCARE, PlacementType.DAYCARE_PART_TIME)) return true
    if (
        set ==
            setOf(
                PlacementType.DAYCARE_FIVE_YEAR_OLDS,
                PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
            )
    )
        return true

    return false
}
