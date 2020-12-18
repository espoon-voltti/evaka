// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import java.util.UUID

data class PlacementPlanDraft(
    val type: PlacementType,
    val child: PlacementDraftChild,
    val preferredUnits: List<PlacementDraftUnit>,
    val period: FiniteDateRange,
    val preschoolDaycarePeriod: FiniteDateRange?,
    val placements: List<PlacementDraftPlacement>
)

data class PlacementDraftChild(val id: UUID, val firstName: String, val lastName: String, val dob: LocalDate)
data class PlacementDraftUnit(val id: UUID, val name: String)

data class PlacementDraftPlacement(
    val id: UUID,
    val type: PlacementType,
    val childId: UUID,
    val unit: PlacementDraftUnit,
    val startDate: LocalDate,
    val endDate: LocalDate
)
