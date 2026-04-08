// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.children

import evaka.core.placement.PlacementType
import evaka.core.shared.ChildId
import evaka.core.shared.ChildImageId
import evaka.core.shared.DaycareId
import evaka.core.shared.GroupId
import evaka.core.shared.PersonId
import evaka.core.shared.security.Action
import java.time.LocalDate
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
    val upcomingPlacementStartDate: LocalDate?,
    val upcomingPlacementIsCalendarOpen: Boolean?,
    @Nested("upcoming_unit") val upcomingPlacementUnit: Unit?,
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
    val upcomingPlacementStartDate: LocalDate?,
    val upcomingPlacementIsCalendarOpen: Boolean?,
    @Nested("upcomingPlacementUnit") val upcomingPlacementUnit: Unit?,
    val permittedActions: Set<Action.Citizen.Child>,
    val serviceApplicationCreationPossible: Boolean,
    val absenceApplicationCreationPossible: Boolean,
) {
    companion object {
        fun fromChild(
            child: Child,
            permittedActions: Set<Action.Citizen.Child>,
            serviceApplicationCreationPossible: Boolean,
            absenceApplicationCreationPossible: Boolean,
        ) =
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
                upcomingPlacementStartDate = child.upcomingPlacementStartDate,
                upcomingPlacementIsCalendarOpen = child.upcomingPlacementIsCalendarOpen,
                upcomingPlacementUnit = child.upcomingPlacementUnit,
                permittedActions = permittedActions,
                serviceApplicationCreationPossible = serviceApplicationCreationPossible,
                absenceApplicationCreationPossible = absenceApplicationCreationPossible,
            )
    }
}
