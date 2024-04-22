// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.service

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.daycare.getCaretakers
import fi.espoo.evaka.daycare.insertCaretakers
import fi.espoo.evaka.daycare.updateCaretakers
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.testDaycare
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CaretakerServiceIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val daycareId = testDaycare.id
    private val groupId = GroupId(UUID.randomUUID())
    private val groupStart = LocalDate.of(2000, 1, 1)

    @BeforeEach
    fun setup() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insert(DevDaycareGroup(id = groupId, daycareId = daycareId, startDate = groupStart))
            tx.execute {
                sql(
                    "INSERT INTO daycare_caretaker (group_id, start_date, end_date, amount) VALUES (${bind(groupId)}, ${bind(groupStart)}, NULL, 3)"
                )
            }
        }
    }

    @Test
    fun `group initially has one row`() {
        val caretakers = db.transaction { getCaretakers(it, groupId) }
        assertEquals(1, caretakers.size)
        assertEquals(groupId, caretakers[0].groupId)
        assertEquals(groupStart, caretakers[0].startDate)
        assertEquals(null, caretakers[0].endDate)
        assertEquals(3.0, caretakers[0].amount)
    }

    @Test
    fun `inserting caretaker row`() {
        val start2 = LocalDate.of(2000, 6, 1)
        val caretakers =
            db.transaction { tx ->
                insertCaretakers(
                    tx,
                    groupId = groupId,
                    startDate = start2,
                    endDate = null,
                    amount = 5.0
                )
                getCaretakers(tx, groupId)
            }
        assertEquals(2, caretakers.size)

        assertEquals(start2, caretakers[0].startDate)
        assertEquals(null, caretakers[0].endDate)
        assertEquals(5.0, caretakers[0].amount)

        assertEquals(groupStart, caretakers[1].startDate)
        assertEquals(start2.minusDays(1), caretakers[1].endDate)
        assertEquals(3.0, caretakers[1].amount)
    }

    @Test
    fun `updating caretaker row`() {
        val rows =
            db.transaction { tx ->
                val id = getCaretakers(tx, groupId).first().id
                updateCaretakers(
                    tx,
                    groupId = groupId,
                    id = id,
                    startDate = LocalDate.of(2000, 7, 1),
                    endDate = LocalDate.of(2000, 8, 1),
                    amount = 2.0
                )

                getCaretakers(tx, groupId)
            }
        assertEquals(1, rows.size)
        val updated = rows.first()
        assertEquals(LocalDate.of(2000, 7, 1), updated.startDate)
        assertEquals(LocalDate.of(2000, 8, 1), updated.endDate)
        assertEquals(2.0, updated.amount)
    }

    @Test
    fun `creating two caretaker rows with overlapping dates should conflict`() {
        assertThrows<Conflict> {
            db.transaction { tx ->
                insertCaretakers(
                    tx,
                    groupId = groupId,
                    startDate = groupStart.minusDays(3),
                    endDate = groupStart.plusDays(3),
                    amount = 5.0
                )
            }
        }
    }

    @Test
    fun `creating two caretaker rows with the same start date should conflict`() {
        assertThrows<Conflict> {
            db.transaction { tx ->
                insertCaretakers(
                    tx,
                    groupId = groupId,
                    startDate = groupStart,
                    endDate = null,
                    amount = 5.0
                )
            }
        }
    }
}
