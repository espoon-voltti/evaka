// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.dao

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.daycare.getGroupStats
import fi.espoo.evaka.daycare.getUnitStats
import fi.espoo.evaka.daycare.service.Caretakers
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.domain.BadRequest
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CaretakerQueriesIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    val careAreaId = UUID.randomUUID()
    val daycareId = DaycareId(UUID.randomUUID())
    val daycareId2 = DaycareId(UUID.randomUUID())
    val groupId1 = GroupId(UUID.randomUUID())
    val groupId2 = GroupId(UUID.randomUUID())
    val groupId3 = GroupId(UUID.randomUUID())
    val groupId4 = GroupId(UUID.randomUUID())

    @BeforeEach
    fun setup() {
        db.transaction { tx ->
            tx.execute {
                sql(
                    "INSERT INTO care_area (id, name, short_name) VALUES ('$careAreaId', 'foo', 'foo')"
                )
            }
            tx.execute {
                sql(
                    "INSERT INTO daycare (id, name, care_area_id) VALUES ('$daycareId', 'foo', '$careAreaId')"
                )
            }
            tx.execute {
                sql(
                    "INSERT INTO daycare (id, name, care_area_id) VALUES ('$daycareId2', 'empty daycare', '$careAreaId')"
                )
            }

            tx.execute {
                sql(
                    "INSERT INTO daycare_group (id, name, daycare_id, start_date, end_date) VALUES ('$groupId1', 'foo', '$daycareId', '2000-01-01', '9999-01-01')"
                )
            }
            tx.execute {
                sql(
                    "INSERT INTO daycare_group (id, name, daycare_id, start_date, end_date) VALUES ('$groupId2', 'bar', '$daycareId', '2000-01-01', '9999-01-01')"
                )
            }
            tx.execute {
                sql(
                    "INSERT INTO daycare_group (id, name, daycare_id, start_date, end_date) VALUES ('$groupId3', 'baz', '$daycareId', '2000-01-01', '9999-01-01')"
                )
            }
            tx.execute {
                sql(
                    "INSERT INTO daycare_group (id, name, daycare_id, start_date, end_date) VALUES ('$groupId4', 'empty', '$daycareId', '2000-01-01', '9999-01-01')"
                )
            }

            // date             01-01   02-02   02-06   03-02   03-04   05-06   09-04
            // g1               3       5       5       4       4       4       4
            // g2               3       3       1       1       3       3       3
            // g3               4       4       4       4       4       6       0
            // g4               0       0       0       0       0       0       0
            // total            10      12      10      9       11      13      7

            tx.execute {
                sql(
                    "INSERT INTO daycare_caretaker (group_id, amount, start_date, end_date) VALUES ('$groupId1', 3, '2000-01-01', '2000-02-01')"
                )
            }
            tx.execute {
                sql(
                    "INSERT INTO daycare_caretaker (group_id, amount, start_date, end_date) VALUES ('$groupId1', 5, '2000-02-02', '2000-03-01')"
                )
            }
            tx.execute {
                sql(
                    "INSERT INTO daycare_caretaker (group_id, amount, start_date, end_date) VALUES ('$groupId1', 4, '2000-03-02', '9999-01-01')"
                )
            }

            tx.execute {
                sql(
                    "INSERT INTO daycare_caretaker (group_id, amount, start_date, end_date) VALUES ('$groupId2', 3, '2000-01-01', '2000-02-05')"
                )
            }
            tx.execute {
                sql(
                    "INSERT INTO daycare_caretaker (group_id, amount, start_date, end_date) VALUES ('$groupId2', 1, '2000-02-06', '2000-03-03')"
                )
            }
            tx.execute {
                sql(
                    "INSERT INTO daycare_caretaker (group_id, amount, start_date, end_date) VALUES ('$groupId2', 3, '2000-03-04', '9999-01-01')"
                )
            }

            tx.execute {
                sql(
                    "INSERT INTO daycare_caretaker (group_id, amount, start_date, end_date) VALUES ('$groupId3', 4, '2000-01-01', '2000-05-05')"
                )
            }
            tx.execute {
                sql(
                    "INSERT INTO daycare_caretaker (group_id, amount, start_date, end_date) VALUES ('$groupId3', 6, '2000-05-06', '2000-09-03')"
                )
            }
        }
    }

    @Test
    fun `test getGroupStats`() =
        db.transaction { tx ->
            val groupStats =
                tx.getGroupStats(daycareId, LocalDate.of(2000, 1, 1), LocalDate.of(2000, 6, 1))
            assertEquals(4, groupStats.keys.size)
            assertEquals(Caretakers(3.0, 5.0), groupStats.get(groupId1))
            assertEquals(Caretakers(1.0, 3.0), groupStats.get(groupId2))
            assertEquals(Caretakers(4.0, 6.0), groupStats.get(groupId3))
            assertEquals(Caretakers(0.0, 0.0), groupStats.get(groupId4))
        }

    @Test
    fun `test getGroupStats with limited range`() =
        db.transaction { tx ->
            val groupStats =
                tx.getGroupStats(daycareId, LocalDate.of(2000, 2, 3), LocalDate.of(2000, 4, 1))
            assertEquals(4, groupStats.keys.size)
            assertEquals(Caretakers(4.0, 5.0), groupStats.get(groupId1))
            assertEquals(Caretakers(1.0, 3.0), groupStats.get(groupId2))
            assertEquals(Caretakers(4.0, 4.0), groupStats.get(groupId3))
            assertEquals(Caretakers(0.0, 0.0), groupStats.get(groupId4))
        }

    @Test
    fun `test getGroupStats with limited range 2`() =
        db.transaction { tx ->
            val singleDate = LocalDate.of(2000, 2, 1)
            val groupStats = tx.getGroupStats(daycareId, singleDate, singleDate)
            assertEquals(Caretakers(3.0, 3.0), groupStats.get(groupId1))
        }

    @Test
    fun `test getGroupStats with limited range 3`() =
        db.transaction { tx ->
            val singleDate = LocalDate.of(2000, 2, 2)
            val groupStats = tx.getGroupStats(daycareId, singleDate, singleDate)
            assertEquals(Caretakers(5.0, 5.0), groupStats.get(groupId1))
        }

    @Test
    fun `test getUnitStats`() =
        db.transaction { tx ->
            val unitStats =
                tx.getUnitStats(daycareId, LocalDate.of(2000, 1, 1), LocalDate.of(2000, 6, 1))
            assertEquals(Caretakers(9.0, 13.0), unitStats)
        }

    @Test
    fun `test getUnitStats with no groups`() =
        db.transaction { tx ->
            val unitStats =
                tx.getUnitStats(daycareId2, LocalDate.of(2000, 1, 1), LocalDate.of(2000, 6, 1))
            assertEquals(Caretakers(0.0, 0.0), unitStats)
        }

    @Test
    fun `test getUnitStats with limited range 1`() =
        db.transaction { tx ->
            val unitStats =
                tx.getUnitStats(daycareId, LocalDate.of(2000, 3, 1), LocalDate.of(2000, 3, 2))
            assertEquals(Caretakers(9.0, 10.0), unitStats)
        }

    @Test
    fun `test getUnitStats with limited range 2`() =
        db.transaction { tx ->
            val unitStats =
                tx.getUnitStats(daycareId, LocalDate.of(2000, 3, 1), LocalDate.of(2000, 3, 1))
            assertEquals(Caretakers(10.0, 10.0), unitStats)
        }

    @Test
    fun `test getUnitStats with limited range 3`() =
        db.transaction { tx ->
            val unitStats =
                tx.getUnitStats(daycareId, LocalDate.of(2000, 3, 2), LocalDate.of(2000, 3, 2))
            assertEquals(Caretakers(9.0, 9.0), unitStats)
        }

    @Test
    fun `test getUnitStats with long time range`() =
        db.transaction { tx ->
            tx.getUnitStats(daycareId, LocalDate.of(2005, 1, 1), LocalDate.of(2010, 1, 1))
        }

    @Test
    fun `test getUnitStats with too long time range`() =
        db.transaction { tx ->
            assertThrows<BadRequest> {
                tx.getUnitStats(daycareId, LocalDate.of(2005, 1, 1), LocalDate.of(2010, 1, 2))
            }
        }
}
