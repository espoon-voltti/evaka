// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.reports

import evaka.core.FullApplicationTest
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevFridgeChild
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FamilyConflictReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var familyConflictReportController: FamilyConflictReportController

    private val today = LocalDate.now()
    private val clock = MockEvakaClock(HelsinkiDateTime.of(today, LocalTime.of(12, 0)))

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val daycare2 = DevDaycare(areaId = area.id)
    private val employee = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val adult = DevPerson(ssn = "010180-1232")
    private val child = DevPerson()

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(daycare2)
            tx.insert(employee)
            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
        }
    }

    @Test
    fun `conflicting child relationship without a placement is not shown`() {
        db.transaction {
            it.insert(
                DevFridgeChild(
                    childId = child.id,
                    startDate = today,
                    endDate = today.plusYears(1),
                    headOfChild = adult.id,
                    conflict = true,
                )
            )
        }

        getAndAssert(listOf())
    }

    @Test
    fun `conflicting child relationship with a placement is shown`() {
        db.transaction {
            it.insert(
                DevFridgeChild(
                    childId = child.id,
                    startDate = today,
                    endDate = today.plusYears(1),
                    headOfChild = adult.id,
                    conflict = true,
                )
            )
        }
        insertPlacement(child.id, today, today)

        getAndAssert(listOf(toReportRow(adult, 0, 1)))
    }

    @Test
    fun `normal child relationship with a placement is not shown`() {
        db.transaction {
            it.insert(
                DevFridgeChild(
                    childId = child.id,
                    startDate = today,
                    endDate = today.plusYears(1),
                    headOfChild = adult.id,
                    conflict = false,
                )
            )
        }
        insertPlacement(child.id, today, today)

        getAndAssert(listOf())
    }

    @Test
    fun `conflicting child relationship with a future placement is shown`() {
        db.transaction {
            it.insert(
                DevFridgeChild(
                    childId = child.id,
                    startDate = today,
                    endDate = today.plusYears(1),
                    headOfChild = adult.id,
                    conflict = true,
                )
            )
        }
        insertPlacement(child.id, today.plusDays(7), today.plusDays(14))

        getAndAssert(listOf(toReportRow(adult, 0, 1)))
    }

    @Test
    fun `conflicting child relationship show the unit of the first placement`() {
        db.transaction {
            it.insert(
                DevFridgeChild(
                    childId = child.id,
                    startDate = today,
                    endDate = today.plusYears(1),
                    headOfChild = adult.id,
                    conflict = true,
                )
            )
        }
        insertPlacement(child.id, today.plusDays(8), today.plusDays(14), daycare.id)
        insertPlacement(child.id, today.plusDays(1), today.plusDays(7), daycare2.id)

        getAndAssert(listOf(toReportRow(adult, 0, 1, daycare2)))
    }

    private fun getAndAssert(expected: List<FamilyConflictReportRow>) {
        val result =
            familyConflictReportController.getFamilyConflictsReport(
                dbInstance(),
                employee.user,
                clock,
            )
        assertEquals(expected, result)
    }

    private fun insertPlacement(
        childId: ChildId,
        startDate: LocalDate,
        endDate: LocalDate,
        unitId: DaycareId = daycare.id,
    ) = db.transaction { tx ->
        tx.insert(
            DevPlacement(
                childId = childId,
                unitId = unitId,
                startDate = startDate,
                endDate = endDate,
            )
        )
    }

    private fun toReportRow(
        person: DevPerson,
        partnerConflictCount: Int,
        childConflictCount: Int,
        unit: DevDaycare = daycare,
    ) =
        FamilyConflictReportRow(
            careAreaName = area.name,
            unitId = unit.id,
            unitName = unit.name,
            id = person.id,
            firstName = person.firstName,
            lastName = person.lastName,
            socialSecurityNumber = person.ssn,
            partnerConflictCount = partnerConflictCount,
            childConflictCount = childConflictCount,
        )
}
