// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.outofoffice

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.RealEvakaClock
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired

class OutOfOfficeIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var outOfOfficeController: OutOfOfficeController

    private val clock = RealEvakaClock()

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val employee =
        AuthenticatedUser.Employee(
            id = EmployeeId(UUID.randomUUID()),
            roles = setOf(UserRole.UNIT_SUPERVISOR),
        )

    @BeforeEach
    fun setUp() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(DevEmployee(id = employee.id))
            tx.insertDaycareAclRow(
                daycareId = daycare.id,
                employeeId = employee.id,
                UserRole.UNIT_SUPERVISOR,
            )
        }
    }

    @Test
    fun `out of office periods`() {
        val initialPeriods =
            outOfOfficeController.getOutOfOfficePeriods(dbInstance(), employee, clock)
        assertEquals(0, initialPeriods.size)

        val period =
            OutOfOfficePeriodUpsert(
                id = null,
                period =
                    FiniteDateRange(
                        start = clock.today().plusDays(1),
                        end = clock.today().plusDays(2),
                    ),
            )
        outOfOfficeController.upsertOutOfOfficePeriod(dbInstance(), employee, clock, period)
        val addedPeriod = outOfOfficeController.getOutOfOfficePeriods(dbInstance(), employee, clock)
        assertEquals(listOf(period.period), addedPeriod.map { it.period })

        val updatedPeriod =
            OutOfOfficePeriodUpsert(
                id = addedPeriod.first().id,
                period =
                    FiniteDateRange(
                        start = clock.today().plusDays(3),
                        end = clock.today().plusDays(4),
                    ),
            )
        outOfOfficeController.upsertOutOfOfficePeriod(dbInstance(), employee, clock, updatedPeriod)
        val updatedPeriods =
            outOfOfficeController.getOutOfOfficePeriods(dbInstance(), employee, clock)
        assertEquals(listOf(updatedPeriod.period), updatedPeriods.map { it.period })

        outOfOfficeController.deleteOutOfOfficePeriod(
            dbInstance(),
            employee,
            clock,
            updatedPeriod.id!!,
        )
        val deletedPeriods =
            outOfOfficeController.getOutOfOfficePeriods(dbInstance(), employee, clock)
        assertEquals(0, deletedPeriods.size)
    }
}
