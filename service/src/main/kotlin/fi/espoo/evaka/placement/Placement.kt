// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PlacementId
import java.time.LocalDate
import java.util.UUID

data class Placement(
    val id: PlacementId,
    val type: PlacementType,
    val childId: UUID,
    val unitId: DaycareId,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val terminationRequestedDate: LocalDate?,
    val terminationRequestedBy: UUID?
)
