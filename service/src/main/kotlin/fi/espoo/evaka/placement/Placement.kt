// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.children.Unit
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.ServiceApplicationId
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import org.jdbi.v3.core.mapper.Nested

enum class PlacementSource : DatabaseEnum {
    APPLICATION,
    SERVICE_APPLICATION,
    PLACEMENT_TERMINATION,
    MANUAL;

    override val sqlType: String = "placement_source"
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
