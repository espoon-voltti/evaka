// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.service

import fi.espoo.evaka.daycare.dao.DaycareCaretakerDAO
import fi.espoo.evaka.daycare.dao.DaycareDAO
import fi.espoo.evaka.daycare.dao.DaycarePlacementDAO
import fi.espoo.evaka.daycare.getDaycareApplyFlags
import fi.espoo.evaka.shared.db.withSpringHandle
import fi.espoo.evaka.shared.db.withSpringTx
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.NotFound
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.postgresql.util.PSQLException
import org.postgresql.util.PSQLState
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

@Service
class DaycareService(
    private val daycareDAO: DaycareDAO,
    private val placementDAO: DaycarePlacementDAO,
    private val caretakerDAO: DaycareCaretakerDAO,
    private val txManager: PlatformTransactionManager,
    private val dataSource: DataSource
) {
    fun getDaycareCapacityStats(
        daycareId: UUID,
        startDate: LocalDate,
        endDate: LocalDate
    ): DaycareCapacityStats = withSpringTx(txManager, readOnly = true) {
        val unitStats = caretakerDAO.getUnitStats(daycareId, startDate, endDate)
        DaycareCapacityStats(
            unitTotalCaretakers = unitStats,
            groupCaretakers = caretakerDAO.getGroupStats(daycareId, startDate, endDate)
        )
    }

    fun getDaycareApplyFlags(ids: Collection<UUID>) = withSpringTx(txManager, readOnly = true) {
        withSpringHandle(dataSource) {
            it.getDaycareApplyFlags(ids)
        }
    }

    fun getDaycareManager(daycareId: UUID): DaycareManager? =
        withSpringTx(txManager, readOnly = true) { daycareDAO.getDaycareManager(daycareId) }

    fun createGroup(
        daycareId: UUID,
        name: String,
        startDate: LocalDate,
        initialCaretakers: Double
    ): DaycareGroup = withSpringTx(txManager) {
        daycareDAO.createGroup(daycareId, name, startDate).also {
            caretakerDAO.initCaretakers(it.id, it.startDate, initialCaretakers)
        }
    }

    fun deleteGroup(daycareId: UUID, groupId: UUID) = try {
        withSpringTx(txManager) {
            val isEmpty = placementDAO.getDaycareGroupPlacements(
                daycareId = daycareId,
                groupId = groupId,
                startDate = null,
                endDate = null
            ).isEmpty()

            if (!isEmpty) throw Conflict("Cannot delete group which has children placed in it")

            daycareDAO.deleteGroup(daycareId, groupId)
        }
    } catch (e: UnableToExecuteStatementException) {
        throw (e.cause as? PSQLException)?.takeIf { it.serverErrorMessage?.sqlState == PSQLState.FOREIGN_KEY_VIOLATION.state }
            ?.let { Conflict("Cannot delete group which is still referred to from other data") }
            ?: e
    }

    fun getDaycareGroups(daycareId: UUID, startDate: LocalDate?, endDate: LocalDate?): List<DaycareGroup> =
        withSpringTx(txManager, readOnly = true) {
            if (!daycareDAO.isValidDaycareId(daycareId)) throw NotFound("No daycare found with id $daycareId")

            daycareDAO.getDaycareGroups(daycareId, startDate, endDate)
        }

    fun getDaycareGroup(groupId: UUID): DaycareGroup? = withSpringTx(txManager, readOnly = true) {
        daycareDAO.getDaycareGroupById(groupId)
    }
}

data class DaycareManager(
    val name: String,
    val email: String,
    val phone: String
)

data class DaycareGroup(
    val id: UUID,
    val daycareId: UUID,
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val deletable: Boolean
)

data class DaycareCapacityStats(
    val unitTotalCaretakers: Stats,
    val groupCaretakers: Map<UUID, Stats>
)

data class Stats(
    val minimum: Double,
    val maximum: Double
)
