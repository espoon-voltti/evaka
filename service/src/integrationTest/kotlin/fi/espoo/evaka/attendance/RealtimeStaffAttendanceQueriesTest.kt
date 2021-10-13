// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.FixtureBuilder
import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.testDaycare
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RealtimeStaffAttendanceQueriesTest : PureJdbiTest() {
    private lateinit var employee1Id: EmployeeId
    private lateinit var employee2Id: EmployeeId
    private lateinit var employee3Id: EmployeeId
    private val group1 = DevDaycareGroup(daycareId = testDaycare.id, name = "Koirat")
    private val group2 = DevDaycareGroup(daycareId = testDaycare.id, name = "Kissat")
    private val group3 = DevDaycareGroup(daycareId = testDaycare.id, name = "Tyhjät")

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insertTestDaycareGroup(group1)
            tx.insertTestDaycareGroup(group2)
            tx.insertTestDaycareGroup(group3)
            FixtureBuilder(tx)
                .addEmployee()
                .withName("One", "in group 1")
                .withGroupAccess(testDaycare.id, group1.id)
                .withScopedRole(UserRole.STAFF, testDaycare.id)
                .saveAnd {
                    employee1Id = employeeId
                }
                .addEmployee()
                .withName("Two", "in group 2")
                .withGroupAccess(testDaycare.id, group2.id)
                .withScopedRole(UserRole.STAFF, testDaycare.id)
                .saveAnd {
                    employee2Id = employeeId
                }
                .addEmployee()
                .withName("Three", "in group 1")
                .withGroupAccess(testDaycare.id, group1.id)
                .withScopedRole(UserRole.SPECIAL_EDUCATION_TEACHER, testDaycare.id)
                .saveAnd {
                    employee3Id = employeeId
                }
        }
    }

    @AfterEach
    fun afterEach() {
        db.transaction { tx -> tx.resetDatabase() }
    }

    @Test
    fun realtimeAttendanceQueries() {
        val now = HelsinkiDateTime.now().atStartOfDay().plusHours(8)
        db.transaction { tx ->
            tx.markStaffArrival(employee1Id, group1.id, now.minusDays(1)).let {
                tx.markStaffDeparture(it, now.minusDays(1).plusHours(8))
            }
            tx.markStaffArrival(employee1Id, group1.id, now).let {
                tx.markStaffDeparture(it, now.plusHours(2))
            }
            tx.markStaffArrival(employee3Id, group1.id, now.plusHours(1)).let {
                tx.markStaffDeparture(it, now.plusHours(7))
            }
            tx.markStaffArrival(employee2Id, group2.id, now.plusHours(2)).let {
                tx.markStaffDeparture(it, now.plusHours(8))
            }
        }

        db.read {
            val attendances = it.getStaffAttendances(testDaycare.id, now)
            assertEquals(3, attendances.size)
            assertEquals(listOf("One", "Three", "Two"), attendances.map { a -> a.firstName })
            assertEquals(listOf(group1.id, group1.id, group2.id), attendances.flatMap { a -> a.groupIds })
        }
    }

    @Test
    fun externalAttendanceQueries() {
        val now = HelsinkiDateTime.now().atStartOfDay().plusHours(8)
        db.transaction { tx ->
            tx.markExternalStaffArrival(ExternalStaffArrival("Foo Absent", group1.id, now.minusDays(1))).let {
                tx.markExternalStaffDeparture(ExternalStaffDeparture(it, now.minusDays(1).plusHours(8)))
            }
            tx.markExternalStaffArrival(ExternalStaffArrival("Foo Present", group1.id, now.minusDays(1)))
        }
        val externalAttendances = db.read { it.getExternalStaffAttendances(testDaycare.id) }

        assertEquals(1, externalAttendances.size)
        assertEquals("Foo Present", externalAttendances[0].name)
        assertEquals(group1.id, externalAttendances[0].groupId)
        assertEquals(now.minusDays(1), externalAttendances[0].arrived)
    }
}
