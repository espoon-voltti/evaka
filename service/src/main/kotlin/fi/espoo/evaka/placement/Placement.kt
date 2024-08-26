// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.children.Unit
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.PlacementId
import java.time.LocalDate
import org.jdbi.v3.core.mapper.Nested

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
)

data class PlacementSummary(
    val id: PlacementId,
    val type: PlacementType,
    val childId: ChildId,
    @Nested("unit") val unit: Unit,
    val startDate: LocalDate,
    val endDate: LocalDate,
)
