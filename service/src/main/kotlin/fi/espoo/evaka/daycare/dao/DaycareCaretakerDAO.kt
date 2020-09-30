// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.dao

import fi.espoo.evaka.daycare.getGroupStats
import fi.espoo.evaka.daycare.getUnitStats
import fi.espoo.evaka.daycare.initCaretakers
import fi.espoo.evaka.daycare.service.Stats
import fi.espoo.evaka.shared.db.withSpringHandle
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

@Component
class DaycareCaretakerDAO(private val dataSource: DataSource) {
    fun initCaretakers(groupId: UUID, startDate: LocalDate, amount: Double) = withSpringHandle(dataSource) { h ->
        h.initCaretakers(groupId, startDate, amount)
    }

    fun getUnitStats(daycareId: UUID, startDate: LocalDate, endDate: LocalDate): Stats =
        withSpringHandle(dataSource) { h -> h.getUnitStats(daycareId, startDate, endDate) }

    fun getGroupStats(daycareId: UUID, startDate: LocalDate, endDate: LocalDate): Map<UUID, Stats> =
        withSpringHandle(dataSource) { h -> h.getGroupStats(daycareId, startDate, endDate) }
}
