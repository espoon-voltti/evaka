// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.children

import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildImageId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PersonId
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.mapper.PropagateNull

data class Group(@PropagateNull val id: GroupId, val name: String)

data class Unit(@PropagateNull val id: DaycareId, val name: String)

data class Child(
    val id: ChildId,
    val firstName: String,
    val preferredName: String,
    val lastName: String,
    val duplicateOf: PersonId?,
    val imageId: ChildImageId?,
    @Nested("group") val group: Group?,
    @Nested("unit") val unit: Unit?,
    val upcomingPlacementType: PlacementType?,
    val hasPedagogicalDocuments: Boolean,
    val hasCurriculums: Boolean
)
