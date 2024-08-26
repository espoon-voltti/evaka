// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.process

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.user.EvakaUserType
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class ArchivedProcessQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {
    @Test
    fun `inserting process increments the number correctly`() {
        val definition1 = "123.456.789"
        val definition2 = "987.654.321"
        val year1 = 2022
        val year2 = 2023
        val organization = "Espoon kaupungin esiopetus ja varhaiskasvatus"
        val archiveMonths = 120
        assertEquals(
            1,
            db.transaction { it.insertProcess(definition1, year1, organization, archiveMonths) }
                .number,
        )
        assertEquals(
            2,
            db.transaction { it.insertProcess(definition1, year1, organization, archiveMonths) }
                .number,
        )
        assertEquals(
            1,
            db.transaction { it.insertProcess(definition2, year1, organization, archiveMonths) }
                .number,
        )
        assertEquals(
            1,
            db.transaction { it.insertProcess(definition1, year2, organization, archiveMonths) }
                .number,
        )
        assertEquals(
            1,
            db.transaction { it.insertProcess(definition2, year2, organization, archiveMonths) }
                .number,
        )
        assertEquals(
            3,
            db.transaction { it.insertProcess(definition1, year1, organization, archiveMonths) }
                .number,
        )
    }

    @Test
    fun `inserting process history`() {
        val employee = DevEmployee()
        db.transaction { it.insert(employee) }

        val processId =
            db.transaction {
                    it.insertProcess(
                        processDefinitionNumber = "123.456.789",
                        year = 2022,
                        organization = "Espoon kaupungin esiopetus ja varhaiskasvatus",
                        archiveDurationMonths = 120,
                    )
                }
                .id

        val emptyHistory = db.read { it.getProcess(processId)!!.history }
        assertEquals(emptyList(), emptyHistory)

        val now1 = HelsinkiDateTime.of(LocalDate.of(2022, 8, 1), LocalTime.of(14, 0))
        val now2 = HelsinkiDateTime.of(LocalDate.of(2023, 5, 31), LocalTime.of(23, 50))
        db.transaction {
            it.insertProcessHistoryRow(
                processId = processId,
                state = ArchivedProcessState.INITIAL,
                now = now1,
                userId = employee.evakaUserId,
            )
        }
        db.transaction {
            it.insertProcessHistoryRow(
                processId = processId,
                state = ArchivedProcessState.COMPLETED,
                now = now2,
                userId = AuthenticatedUser.SystemInternalUser.evakaUserId,
            )
        }

        val history = db.read { it.getProcess(processId)!!.history }
        assertEquals(2, history.size)
        history[0].also {
            assertEquals(ArchivedProcessState.INITIAL, it.state)
            assertEquals(1, it.rowIndex)
            assertEquals(now1, it.enteredAt)
            assertEquals(EvakaUserType.EMPLOYEE, it.enteredBy.type)
            assertEquals("Person Test", it.enteredBy.name)
        }
        history[1].also {
            assertEquals(ArchivedProcessState.COMPLETED, it.state)
            assertEquals(2, it.rowIndex)
            assertEquals(now2, it.enteredAt)
            assertEquals(EvakaUserType.SYSTEM, it.enteredBy.type)
            assertEquals("eVaka", it.enteredBy.name)
        }
    }
}
