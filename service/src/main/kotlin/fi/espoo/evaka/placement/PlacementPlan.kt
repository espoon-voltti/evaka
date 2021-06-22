// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import java.util.UUID

data class PlacementPlan(
    val id: UUID,
    val unitId: UUID,
    val applicationId: UUID,
    val type: PlacementType,
    val period: FiniteDateRange,
    val preschoolDaycarePeriod: FiniteDateRange?
)

data class PlacementPlanDetails(
    val id: UUID,
    val unitId: UUID,
    val applicationId: UUID,
    val type: PlacementType,
    val period: FiniteDateRange,
    val preschoolDaycarePeriod: FiniteDateRange?,
    val child: PlacementPlanChild,
    val unitConfirmationStatus: PlacementPlanConfirmationStatus = PlacementPlanConfirmationStatus.PENDING,
    val unitRejectReason: PlacementPlanRejectReason? = null,
    val unitRejectOtherReason: String? = null
)

data class PlacementPlanChild(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate
)

enum class PlacementPlanConfirmationStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}

enum class PlacementPlanRejectReason {
    OTHER,
    REASON_1,
    REASON_2,
    REASON_3
}
