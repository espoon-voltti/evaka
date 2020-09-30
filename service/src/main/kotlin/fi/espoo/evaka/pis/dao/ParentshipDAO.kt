// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.dao

import fi.espoo.evaka.pis.createParentship
import fi.espoo.evaka.pis.deleteParentship
import fi.espoo.evaka.pis.getParentship
import fi.espoo.evaka.pis.getParentships
import fi.espoo.evaka.pis.retryParentship
import fi.espoo.evaka.pis.service.Parentship
import fi.espoo.evaka.pis.updateParentshipDuration
import fi.espoo.evaka.shared.db.withSpringHandle
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

@Component
@Transactional(readOnly = true)
class ParentshipDAO(private val dataSource: DataSource) {
    @Transactional
    fun createParentship(
        childId: UUID,
        headOfChildId: UUID,
        startDate: LocalDate,
        endDate: LocalDate?,
        conflict: Boolean = false
    ): Parentship = withSpringHandle(dataSource) { h ->
        h.createParentship(childId, headOfChildId, startDate, endDate, conflict)
    }

    fun getParentships(
        headOfChildId: UUID?,
        childId: UUID?,
        includeConflicts: Boolean = false
    ): Set<Parentship> = withSpringHandle(dataSource) { h ->
        h.getParentships(headOfChildId, childId, includeConflicts).toSet()
    }

    fun getParentship(id: UUID): Parentship? = withSpringHandle(dataSource) { h -> h.getParentship(id) }

    @Transactional
    fun updateParentshipDuration(id: UUID, startDate: LocalDate, endDate: LocalDate?): Boolean =
        withSpringHandle(dataSource) { h -> h.updateParentshipDuration(id, startDate, endDate) }

    @Transactional
    fun retryParentship(id: UUID) = withSpringHandle(dataSource) { h -> h.retryParentship(id) }

    @Transactional
    fun deleteParentship(id: UUID): Boolean = withSpringHandle(dataSource) { h -> h.deleteParentship(id) }
}
