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
import fi.espoo.evaka.shared.security.Action
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
    val hasCurriculums: Boolean,
)

data class ChildAndPermittedActions(
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
    val hasCurriculums: Boolean,
    val permittedActions: Set<Action.Citizen.Child>,
) {
    companion object {
        fun fromChild(child: Child, permittedActions: Set<Action.Citizen.Child>) =
            ChildAndPermittedActions(
                id = child.id,
                firstName = child.firstName,
                preferredName = child.preferredName,
                lastName = child.lastName,
                duplicateOf = child.duplicateOf,
                imageId = child.imageId,
                group = child.group,
                unit = child.unit,
                upcomingPlacementType = child.upcomingPlacementType,
                hasPedagogicalDocuments = child.hasPedagogicalDocuments,
                hasCurriculums = child.hasCurriculums,
                permittedActions = permittedActions,
            )
    }
}
