// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.placement

import evaka.core.children.Unit
import evaka.core.shared.ApplicationId
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.EvakaUserId
import evaka.core.shared.PlacementId
import evaka.core.shared.ServiceApplicationId
import evaka.core.shared.db.DatabaseEnum
import evaka.core.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import org.jdbi.v3.core.mapper.Nested

enum class PlacementSource : DatabaseEnum {
    APPLICATION,
    SERVICE_APPLICATION,
    PLACEMENT_TERMINATION,
    MANUAL;

    override val sqlType: String = "placement_source"
}

enum class PlacementSourceCreatedBy {
    CITIZEN,
    EMPLOYEE_PAPER,
    EMPLOYEE_MANUAL,
    SYSTEM,
    UNKNOWN,
}

data class Placement(
    val id: PlacementId,
    val type: PlacementType,
    val childId: ChildId,
    val unitId: DaycareId,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val terminationRequestedDate: LocalDate?,
    val terminationRequestedBy: EvakaUserId?,
    val placeGuarantee: Boolean,
    val createdAt: HelsinkiDateTime,
    val createdBy: EvakaUserId?,
    val source: PlacementSource?,
    val sourceApplicationId: ApplicationId?,
    val sourceServiceApplicationId: ServiceApplicationId?,
    val modifiedAt: HelsinkiDateTime?,
    val modifiedBy: EvakaUserId?,
)

data class PlacementSummary(
    val id: PlacementId,
    val type: PlacementType,
    val childId: ChildId,
    @Nested("unit") val unit: Unit,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val reservationsEnabled: Boolean,
)
