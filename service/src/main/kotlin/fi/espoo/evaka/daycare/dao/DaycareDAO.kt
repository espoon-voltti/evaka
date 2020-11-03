// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.dao

import fi.espoo.evaka.daycare.createDaycareGroup
import fi.espoo.evaka.daycare.deleteDaycareGroup
import fi.espoo.evaka.daycare.getDaycareGroup
import fi.espoo.evaka.daycare.getDaycareGroups
import fi.espoo.evaka.daycare.getUnitManager
import fi.espoo.evaka.daycare.isValidDaycareId
import fi.espoo.evaka.daycare.service.DaycareManager
import fi.espoo.evaka.shared.db.withSpringHandle
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

@Component
class DaycareDAO(private val dataSource: DataSource) {
    fun isValidDaycareId(id: UUID) = withSpringHandle(dataSource) { it.isValidDaycareId(id) }

    fun getDaycareManager(daycareId: UUID): DaycareManager? = withSpringHandle(dataSource) {
        it.getUnitManager(daycareId)
    }

    fun createGroup(daycareId: UUID, name: String, startDate: LocalDate) = withSpringHandle(dataSource) {
        it.createDaycareGroup(daycareId, name, startDate)
    }

    fun getDaycareGroups(daycareId: UUID, startDate: LocalDate?, endDate: LocalDate?) =
        withSpringHandle(dataSource) {
            it.getDaycareGroups(daycareId, startDate, endDate)
        }

    fun getDaycareGroupById(id: UUID) = withSpringHandle(dataSource) {
        it.getDaycareGroup(id)
    }

    fun deleteGroup(daycareId: UUID, groupId: UUID): Unit =
        withSpringHandle(dataSource) { it.deleteDaycareGroup(groupId) }
}
