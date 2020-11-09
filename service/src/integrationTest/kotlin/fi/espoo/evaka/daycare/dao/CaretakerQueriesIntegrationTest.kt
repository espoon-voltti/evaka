// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.dao

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.daycare.getGroupStats
import fi.espoo.evaka.daycare.getUnitStats
import fi.espoo.evaka.daycare.service.Stats
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.domain.BadRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.UUID

class CaretakerQueriesIntegrationTest : PureJdbiTest() {
    val careAreaId = UUID.randomUUID()
    val daycareId = UUID.randomUUID()
    val daycareId2 = UUID.randomUUID()
    val groupId1 = UUID.randomUUID()
    val groupId2 = UUID.randomUUID()
    val groupId3 = UUID.randomUUID()
    val groupId4 = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        jdbi.handle {
            it.execute("INSERT INTO care_area (id, name, short_name) VALUES ('$careAreaId', 'foo', 'foo')")
            it.execute("INSERT INTO daycare (id, name, care_area_id) VALUES ('$daycareId', 'foo', '$careAreaId')")
            it.execute("INSERT INTO daycare (id, name, care_area_id) VALUES ('$daycareId2', 'empty daycare', '$careAreaId')")

            it.execute("INSERT INTO daycare_group (id, name, daycare_id, start_date, end_date) VALUES ('$groupId1', 'foo', '$daycareId', '2000-01-01', '9999-01-01')")
            it.execute("INSERT INTO daycare_group (id, name, daycare_id, start_date, end_date) VALUES ('$groupId2', 'bar', '$daycareId', '2000-01-01', '9999-01-01')")
            it.execute("INSERT INTO daycare_group (id, name, daycare_id, start_date, end_date) VALUES ('$groupId3', 'baz', '$daycareId', '2000-01-01', '9999-01-01')")
            it.execute("INSERT INTO daycare_group (id, name, daycare_id, start_date, end_date) VALUES ('$groupId4', 'empty', '$daycareId', '2000-01-01', '9999-01-01')")

            // date             01-01   02-02   02-06   03-02   03-04   05-06   09-04
            // g1               3       5       5       4       4       4       4
            // g2               3       3       1       1       3       3       3
            // g3               4       4       4       4       4       6       0
            // g4               0       0       0       0       0       0       0
            // total            10      12      10      9       11      13      7

            it.execute("INSERT INTO daycare_caretaker (group_id, amount, start_date, end_date) VALUES ('$groupId1', 3, '2000-01-01', '2000-02-01')")
            it.execute("INSERT INTO daycare_caretaker (group_id, amount, start_date, end_date) VALUES ('$groupId1', 5, '2000-02-02', '2000-03-01')")
            it.execute("INSERT INTO daycare_caretaker (group_id, amount, start_date, end_date) VALUES ('$groupId1', 4, '2000-03-02', '9999-01-01')")

            it.execute("INSERT INTO daycare_caretaker (group_id, amount, start_date, end_date) VALUES ('$groupId2', 3, '2000-01-01', '2000-02-05')")
            it.execute("INSERT INTO daycare_caretaker (group_id, amount, start_date, end_date) VALUES ('$groupId2', 1, '2000-02-06', '2000-03-03')")
            it.execute("INSERT INTO daycare_caretaker (group_id, amount, start_date, end_date) VALUES ('$groupId2', 3, '2000-03-04', '9999-01-01')")

            it.execute("INSERT INTO daycare_caretaker (group_id, amount, start_date, end_date) VALUES ('$groupId3', 4, '2000-01-01', '2000-05-05')")
            it.execute("INSERT INTO daycare_caretaker (group_id, amount, start_date, end_date) VALUES ('$groupId3', 6, '2000-05-06', '2000-09-03')")
        }
    }

    @AfterEach
    fun tearDown() {
        jdbi.handle {
            it.execute("DELETE FROM daycare WHERE id IN ('$daycareId', '$daycareId2')")
            it.execute("DELETE FROM care_area WHERE id = '$careAreaId'")
        }
    }

    @Test
    fun `test getGroupStats`() = jdbi.handle { h ->
        val groupStats = h.getGroupStats(daycareId, LocalDate.of(2000, 1, 1), LocalDate.of(2000, 6, 1))
        assertEquals(4, groupStats.keys.size)
        assertEquals(Stats(3.0, 5.0), groupStats.get(groupId1))
        assertEquals(Stats(1.0, 3.0), groupStats.get(groupId2))
        assertEquals(Stats(4.0, 6.0), groupStats.get(groupId3))
        assertEquals(Stats(0.0, 0.0), groupStats.get(groupId4))
    }

    @Test
    fun `test getGroupStats with limited range`() = jdbi.handle { h ->
        val groupStats = h.getGroupStats(daycareId, LocalDate.of(2000, 2, 3), LocalDate.of(2000, 4, 1))
        assertEquals(4, groupStats.keys.size)
        assertEquals(Stats(4.0, 5.0), groupStats.get(groupId1))
        assertEquals(Stats(1.0, 3.0), groupStats.get(groupId2))
        assertEquals(Stats(4.0, 4.0), groupStats.get(groupId3))
        assertEquals(Stats(0.0, 0.0), groupStats.get(groupId4))
    }

    @Test
    fun `test getGroupStats with limited range 2`() = jdbi.handle { h ->
        val singleDate = LocalDate.of(2000, 2, 1)
        val groupStats = h.getGroupStats(daycareId, singleDate, singleDate)
        assertEquals(Stats(3.0, 3.0), groupStats.get(groupId1))
    }

    @Test
    fun `test getGroupStats with limited range 3`() = jdbi.handle { h ->
        val singleDate = LocalDate.of(2000, 2, 2)
        val groupStats = h.getGroupStats(daycareId, singleDate, singleDate)
        assertEquals(Stats(5.0, 5.0), groupStats.get(groupId1))
    }

    @Test
    fun `test getUnitStats`() = jdbi.handle { h ->
        val unitStats = h.getUnitStats(daycareId, LocalDate.of(2000, 1, 1), LocalDate.of(2000, 6, 1))
        assertEquals(Stats(9.0, 13.0), unitStats)
    }

    @Test
    fun `test getUnitStats with no groups`() = jdbi.handle { h ->
        val unitStats = h.getUnitStats(daycareId2, LocalDate.of(2000, 1, 1), LocalDate.of(2000, 6, 1))
        assertEquals(Stats(0.0, 0.0), unitStats)
    }

    @Test
    fun `test getUnitStats with limited range 1`() = jdbi.handle { h ->
        val unitStats = h.getUnitStats(daycareId, LocalDate.of(2000, 3, 1), LocalDate.of(2000, 3, 2))
        assertEquals(Stats(9.0, 10.0), unitStats)
    }

    @Test
    fun `test getUnitStats with limited range 2`() = jdbi.handle { h ->
        val unitStats = h.getUnitStats(daycareId, LocalDate.of(2000, 3, 1), LocalDate.of(2000, 3, 1))
        assertEquals(Stats(10.0, 10.0), unitStats)
    }

    @Test
    fun `test getUnitStats with limited range 3`() = jdbi.handle { h ->
        val unitStats = h.getUnitStats(daycareId, LocalDate.of(2000, 3, 2), LocalDate.of(2000, 3, 2))
        assertEquals(Stats(9.0, 9.0), unitStats)
    }

    @Test
    fun `test getUnitStats with long time range`() = jdbi.handle { h ->
        assertDoesNotThrow { h.getUnitStats(daycareId, LocalDate.of(2005, 1, 1), LocalDate.of(2010, 1, 1)) }
    }

    @Test
    fun `test getUnitStats with too long time range`() = jdbi.handle { h ->
        assertThrows<BadRequest> { h.getUnitStats(daycareId, LocalDate.of(2005, 1, 1), LocalDate.of(2010, 1, 2)) }
    }
}
