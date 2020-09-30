// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.dao

import fi.espoo.evaka.placement.DaycareGroupPlacement
import fi.espoo.evaka.placement.DaycarePlacement
import fi.espoo.evaka.placement.DaycarePlacementDetails
import fi.espoo.evaka.placement.createGroupPlacement
import fi.espoo.evaka.placement.deleteGroupPlacement
import fi.espoo.evaka.placement.getDaycareGroupPlacements
import fi.espoo.evaka.placement.getDaycarePlacement
import fi.espoo.evaka.placement.getDaycarePlacements
import fi.espoo.evaka.placement.getIdenticalPostcedingGroupPlacement
import fi.espoo.evaka.placement.getIdenticalPrecedingGroupPlacement
import fi.espoo.evaka.shared.db.withSpringHandle
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

@Component
class DaycarePlacementDAO(private val dataSource: DataSource) {
    fun getDaycarePlacements(
        daycareId: UUID?,
        childId: UUID?,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): Set<DaycarePlacementDetails> = withSpringHandle(dataSource) { h ->
        h.getDaycarePlacements(daycareId, childId, startDate, endDate)
    }.toSet()

    fun getDaycarePlacementById(id: UUID): DaycarePlacement? = withSpringHandle(dataSource) { h ->
        h.getDaycarePlacement(id)
    }

    @Transactional
    fun createGroupPlacement(
        daycarePlacementId: UUID,
        groupId: UUID,
        startDate: LocalDate,
        endDate: LocalDate
    ): DaycareGroupPlacement = withSpringHandle(dataSource) { h ->
        h.createGroupPlacement(daycarePlacementId, groupId, startDate, endDate)
    }

    fun getDaycareGroupPlacements(
        daycareId: UUID,
        startDate: LocalDate?,
        endDate: LocalDate?,
        groupId: UUID? = null
    ): Set<DaycareGroupPlacement> = withSpringHandle(dataSource) { h ->
        h.getDaycareGroupPlacements(daycareId, startDate, endDate, groupId)
    }.toSet()

    fun getIdenticalPrecedingGroupPlacement(
        daycarePlacementId: UUID,
        groupId: UUID,
        startDate: LocalDate
    ): DaycareGroupPlacement? =
        withSpringHandle(dataSource) { h -> h.getIdenticalPrecedingGroupPlacement(daycarePlacementId, groupId, startDate) }

    fun getIdenticalPostcedingGroupPlacement(
        daycarePlacementId: UUID,
        groupId: UUID,
        endDate: LocalDate
    ): DaycareGroupPlacement? =
        withSpringHandle(dataSource) { h -> h.getIdenticalPostcedingGroupPlacement(daycarePlacementId, groupId, endDate) }

    @Transactional
    fun deleteGroupPlacement(groupPlacementId: UUID): Boolean =
        withSpringHandle(dataSource) { h -> h.deleteGroupPlacement(groupPlacementId) }
}
