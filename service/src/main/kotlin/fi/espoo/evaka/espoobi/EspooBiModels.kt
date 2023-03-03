// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoobi

import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PlacementId
import java.time.LocalDate

data class BiArea(val id: AreaId, val name: String)

data class BiUnit(val id: DaycareId, val area: AreaId, val name: String)

data class BiChild(val id: ChildId, val firstName: String, val lastName: String)

data class BiPlacement(
    val id: PlacementId,
    val child: ChildId,
    val unit: DaycareId,
    val type: PlacementType,
    val startDate: LocalDate,
    val endDate: LocalDate
)
