// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.children

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildImageId
import fi.espoo.evaka.shared.GroupId
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.mapper.PropagateNull

data class Group(@PropagateNull val id: GroupId, val name: String)

data class Child(
    val id: ChildId,
    val firstName: String,
    val preferredName: String,
    val lastName: String,
    val imageId: ChildImageId?,
    @Nested("group") val group: Group?,
    val hasUpcomingPlacements: Boolean,
    val hasPedagogicalDocuments: Boolean,
    val hasCurriculums: Boolean
)
