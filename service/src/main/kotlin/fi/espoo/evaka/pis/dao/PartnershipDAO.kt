// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.dao

import fi.espoo.evaka.pis.createPartnership
import fi.espoo.evaka.pis.deletePartnership
import fi.espoo.evaka.pis.getPartnership
import fi.espoo.evaka.pis.getPartnershipsForPerson
import fi.espoo.evaka.pis.retryPartnership
import fi.espoo.evaka.pis.service.Partnership
import fi.espoo.evaka.pis.updatePartnershipDuration
import fi.espoo.evaka.shared.db.withSpringHandle
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

@Component
@Transactional(readOnly = true)
class PartnershipDAO(private val dataSource: DataSource) {
    @Transactional
    fun createPartnership(
        personId1: UUID,
        personId2: UUID,
        startDate: LocalDate,
        endDate: LocalDate?,
        conflict: Boolean = false
    ): Partnership = withSpringHandle(dataSource) { h ->
        h.createPartnership(personId1, personId2, startDate, endDate, conflict)
    }

    fun getPartnership(partnershipId: UUID): Partnership? = withSpringHandle(dataSource) { h ->
        h.getPartnership(partnershipId)
    }

    fun getPartnershipsForPerson(personId: UUID, includeConflicts: Boolean = false): Set<Partnership> =
        withSpringHandle(dataSource) { h -> h.getPartnershipsForPerson(personId, includeConflicts) }.toSet()

    @Transactional
    fun updatePartnershipDuration(partnershipId: UUID, startDate: LocalDate, endDate: LocalDate?): Boolean =
        withSpringHandle(dataSource) { h -> h.updatePartnershipDuration(partnershipId, startDate, endDate) }

    @Transactional
    fun retryPartnership(partnershipId: UUID) = withSpringHandle(dataSource) { h -> h.retryPartnership(partnershipId) }

    @Transactional
    fun deletePartnership(partnershipId: UUID): Boolean =
        withSpringHandle(dataSource) { h -> h.deletePartnership(partnershipId) }
}
