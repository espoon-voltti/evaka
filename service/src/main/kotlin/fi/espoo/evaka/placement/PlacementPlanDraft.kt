// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate

data class PlacementPlanDraft(
    val type: PlacementType,
    val child: PlacementDraftChild,
    val preferredStartDate: LocalDate,
    val dueDate: LocalDate?,
    val preferredUnits: List<PlacementDraftUnit>,
    val placementDraft: PlacementDraftSummary?,
    val period: FiniteDateRange,
    val preschoolDaycarePeriod: FiniteDateRange?,
    val placements: List<PlacementSummary>,
    val guardianHasRestrictedDetails: Boolean,
)

data class PlacementDraftSummary(val unit: PlacementDraftUnit, val startDate: LocalDate)

data class PlacementDraftChild(
    val id: ChildId,
    val firstName: String,
    val lastName: String,
    val dob: LocalDate,
)

data class PlacementDraftUnit(val id: DaycareId, val name: String)
