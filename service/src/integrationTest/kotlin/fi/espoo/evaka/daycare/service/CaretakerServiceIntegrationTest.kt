// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.service

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.testDaycare
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class CaretakerServiceIntegrationTest : PureJdbiTest() {
    val service = CaretakerService()

    private val daycareId = testDaycare.id
    private val groupId = UUID.randomUUID()
    private val groupStart = LocalDate.of(2000, 1, 1)

    @BeforeEach
    fun setup() {
        jdbi.handle {
            resetDatabase(it)
            insertGeneralTestFixtures(it)
            it.insertTestDaycareGroup(
                DevDaycareGroup(
                    id = groupId,
                    daycareId = daycareId,
                    startDate = groupStart
                )
            )
            it.execute(
                "INSERT INTO daycare_caretaker (group_id, start_date, amount) VALUES (?, ?, 3)",
                groupId,
                groupStart
            )
        }
    }
    @Test
    fun `group initially has one row`() = jdbi.handle { h ->
        val caretakers = service.getCaretakers(h, groupId)
        assertEquals(1, caretakers.size)
        assertEquals(groupId, caretakers[0].groupId)
        assertEquals(groupStart, caretakers[0].startDate)
        assertEquals(null, caretakers[0].endDate)
        assertEquals(3.0, caretakers[0].amount)
    }

    @Test
    fun `inserting caretaker row`() = jdbi.handle { h ->
        val start2 = LocalDate.of(2000, 6, 1)
        service.insert(
            h,
            groupId = groupId,
            startDate = start2,
            endDate = null,
            amount = 5.0
        )
        val caretakers = service.getCaretakers(h, groupId)
        assertEquals(2, caretakers.size)

        assertEquals(start2, caretakers[0].startDate)
        assertEquals(null, caretakers[0].endDate)
        assertEquals(5.0, caretakers[0].amount)

        assertEquals(groupStart, caretakers[1].startDate)
        assertEquals(start2.minusDays(1), caretakers[1].endDate)
        assertEquals(3.0, caretakers[1].amount)
    }

    @Test
    fun `updating caretaker row`() = jdbi.handle { h ->
        val id = service.getCaretakers(h, groupId).first().id
        service.update(
            h,
            groupId = groupId,
            id = id,
            startDate = LocalDate.of(2000, 7, 1),
            endDate = LocalDate.of(2000, 8, 1),
            amount = 2.0
        )

        val rows = service.getCaretakers(h, groupId)
        assertEquals(1, rows.size)
        val updated = rows.first()
        assertEquals(LocalDate.of(2000, 7, 1), updated.startDate)
        assertEquals(LocalDate.of(2000, 8, 1), updated.endDate)
        assertEquals(2.0, updated.amount)
    }
}
