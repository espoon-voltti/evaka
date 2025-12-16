// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class TitaniaErrorsReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var titaniaErrorsReport: TitaniaErrorReport

    private final val admin = DevEmployee(roles = setOf(UserRole.ADMIN))

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id, name = "Yksikkö")
    private val employee = DevEmployee(firstName = "Eeva", lastName = "Ensimmäinen")
    private val secondDaycare = DevDaycare(areaId = area.id, name = "Kaksikko")
    private val secondEmployee = DevEmployee(firstName = "Tauno", lastName = "Toinen")
    private val thirdEmployee = DevEmployee(firstName = "Katja", lastName = "Kolmas")
    private val fourthEmployee = DevEmployee(firstName = "Niilo", lastName = "Neljäs")
    private val supervisor = DevEmployee()

    @Test
    fun `admin should be able to use the report`() {

        insertTestData()

        titaniaErrorsReport.getTitaniaErrorsReport(
            dbInstance(),
            admin.user,
            MockEvakaClock(2025, 12, 12, 12, 0, 0),
        )
    }

    @Test
    fun `supervisor should be able to use the report`() {

        insertTestData()

        titaniaErrorsReport.getTitaniaErrorsReport(
            dbInstance(),
            supervisor.user,
            MockEvakaClock(2025, 12, 12, 12, 0, 0),
        )
    }

    @Test
    fun `admins should see personnel from all units and those with no ACL to any unit`() {

        insertTestData()

        val result =
            titaniaErrorsReport.getTitaniaErrorsReport(
                dbInstance(),
                admin.user,
                MockEvakaClock(2025, 12, 12, 12, 0, 0),
            )

        assertEquals(3, result.first().units.size)
    }

    @Test
    fun `supervisors should only see personnel of their own unit`() {

        insertTestData()

        val result =
            titaniaErrorsReport.getTitaniaErrorsReport(
                dbInstance(),
                supervisor.user,
                MockEvakaClock(2025, 12, 12, 12, 0, 0),
            )

        assertEquals(1, result.first().units.size)
        assertEquals("Yksikkö", result.first().units.first().unitName)
    }

    @Test
    fun `admin should see if someone has multiple ACLs`() {

        insertTestDataWithFourthEmployee()

        val result =
            titaniaErrorsReport.getTitaniaErrorsReport(
                dbInstance(),
                admin.user,
                MockEvakaClock(2025, 12, 12, 12, 0, 0),
            )

        assert(result.first().units.map { it.unitName }.contains("Kaksikko, Yksikkö"))
    }

    @Test
    fun `supervisor should only see his own unit when someone has multiple ACLs`() {

        insertTestDataWithFourthEmployee()

        val result =
            titaniaErrorsReport.getTitaniaErrorsReport(
                dbInstance(),
                supervisor.user,
                MockEvakaClock(2025, 12, 12, 12, 0, 0),
            )

        assertEquals(1, result.first().units.size)
        assertEquals("Yksikkö", result.first().units.first().unitName)
        val employees = result.first().units.first().employees.map { it.employeeName }
        assert(employees.contains("Ensimmäinen Eeva"))
        assert(employees.contains("Neljäs Niilo"))
    }

    fun insertTestData() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(secondDaycare)
            tx.insert(thirdEmployee)
            tx.insert(employee)
            tx.insertDaycareAclRow(daycare.id, employee.id, UserRole.STAFF)
            tx.insert(secondEmployee)
            tx.insertDaycareAclRow(secondDaycare.id, secondEmployee.id, UserRole.STAFF)
            // thirdEmployee does not have any ACLs
            tx.insert(supervisor)
            tx.insertDaycareAclRow(daycare.id, supervisor.id, UserRole.UNIT_SUPERVISOR)

            insertTitaniaError(
                tx,
                HelsinkiDateTime.of(LocalDate.of(2025, 12, 12), LocalTime.of(12, 0)),
                LocalDate.of(2025, 12, 10),
                employee.id,
                LocalTime.of(8, 0),
                LocalTime.of(12, 0),
                LocalTime.of(10, 0),
                LocalTime.of(14, 0),
            )
            insertTitaniaError(
                tx,
                HelsinkiDateTime.of(LocalDate.of(2025, 12, 12), LocalTime.of(12, 0)),
                LocalDate.of(2025, 12, 9),
                secondEmployee.id,
                LocalTime.of(12, 0),
                LocalTime.of(15, 30),
                LocalTime.of(15, 0),
                LocalTime.of(16, 0),
            )
            insertTitaniaError(
                tx,
                HelsinkiDateTime.of(LocalDate.of(2025, 12, 12), LocalTime.of(12, 0)),
                LocalDate.of(2025, 12, 8),
                thirdEmployee.id,
                LocalTime.of(9, 0),
                LocalTime.of(11, 30),
                LocalTime.of(11, 0),
                LocalTime.of(12, 30),
            )
        }
    }

    fun insertTestDataWithFourthEmployee() {
        insertTestData()

        db.transaction { tx ->
            tx.insert(fourthEmployee)
            tx.insertDaycareAclRow(daycare.id, fourthEmployee.id, UserRole.STAFF)
            tx.insertDaycareAclRow(secondDaycare.id, fourthEmployee.id, UserRole.STAFF)

            insertTitaniaError(
                tx,
                HelsinkiDateTime.of(LocalDate.of(2025, 12, 12), LocalTime.of(12, 0)),
                LocalDate.of(2025, 12, 13),
                fourthEmployee.id,
                LocalTime.of(8, 0),
                LocalTime.of(9, 30),
                LocalTime.of(8, 30),
                LocalTime.of(10, 0),
            )
        }
    }

    fun insertTitaniaError(
        tx: Database.Transaction,
        requestTime: HelsinkiDateTime,
        shiftDate: LocalDate,
        employeeId: EmployeeId,
        shiftBegins: LocalTime,
        shiftEnds: LocalTime,
        overlappingShiftBegins: LocalTime,
        overlappingShiftEnds: LocalTime,
    ) {
        tx.createUpdate {
                val id = UUID.randomUUID()
                sql(
                    """
                    INSERT INTO titania_errors (
                        id,
                        request_time,
                        shift_date,
                        employee_id,
                        shift_begins,
                        shift_ends,
                        overlapping_shift_begins,
                        overlapping_shift_ends
                    )
                    VALUES (
                        ${bind(id)},
                        ${bind(requestTime)},
                        ${bind(shiftDate)},
                        ${bind(employeeId)},
                        ${bind(shiftBegins)},
                        ${bind(shiftEnds)},
                        ${bind(overlappingShiftBegins)},
                        ${bind(overlappingShiftEnds)}
                    )
                """
                        .trimIndent()
                )
            }
            .execute()
    }
}
