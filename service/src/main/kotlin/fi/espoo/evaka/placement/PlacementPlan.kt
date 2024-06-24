// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PlacementPlanId
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate

data class PlacementPlan(
    val id: PlacementPlanId,
    val unitId: DaycareId,
    val applicationId: ApplicationId,
    val type: PlacementType,
    val period: FiniteDateRange,
    val preschoolDaycarePeriod: FiniteDateRange?
)

sealed interface PlacementPlanExtent {
    /** Simple placement plan that corresponds to a single placement */
    data class FullSingle(
        val period: FiniteDateRange
    ) : PlacementPlanExtent

    /** Double placement plan, only preschool */
    data class OnlyPreschool(
        val period: FiniteDateRange
    ) : PlacementPlanExtent

    /** Double placement plan, only preschool+daycare */
    data class OnlyPreschoolDaycare(
        val period: FiniteDateRange
    ) : PlacementPlanExtent

    /** Double placement plan, both parts */
    data class FullDouble(
        val period: FiniteDateRange,
        val preschoolDaycarePeriod: FiniteDateRange
    ) : PlacementPlanExtent
}

data class PlacementPlanDetails(
    val id: PlacementPlanId,
    val unitId: DaycareId,
    val applicationId: ApplicationId,
    val type: PlacementType,
    val period: FiniteDateRange,
    val preschoolDaycarePeriod: FiniteDateRange?,
    val child: PlacementPlanChild,
    val unitConfirmationStatus: PlacementPlanConfirmationStatus =
        PlacementPlanConfirmationStatus.PENDING,
    val unitRejectReason: PlacementPlanRejectReason? = null,
    val unitRejectOtherReason: String? = null,
    val rejectedByCitizen: Boolean = false
)

data class PlacementPlanChild(
    val id: ChildId,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate
)

enum class PlacementPlanConfirmationStatus : DatabaseEnum {
    PENDING,
    ACCEPTED,
    REJECTED,
    REJECTED_NOT_CONFIRMED;

    override val sqlType: String = "confirmation_status"
}

enum class PlacementPlanRejectReason : DatabaseEnum {
    OTHER,
    REASON_1,
    REASON_2,
    REASON_3;

    override val sqlType: String = "placement_reject_reason"
}
