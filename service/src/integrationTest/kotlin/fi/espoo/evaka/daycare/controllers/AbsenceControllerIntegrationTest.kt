// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.service.Absence
import fi.espoo.evaka.daycare.service.AbsenceCategory
import fi.espoo.evaka.daycare.service.AbsenceDelete
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AbsenceId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.insertTestAbsence
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestChild
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycareGroup
import fi.espoo.evaka.testDecisionMaker_1
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class AbsenceControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var absenceController: AbsenceController

    private val now = HelsinkiDateTime.of(LocalDate.of(2023, 6, 1), LocalTime.of(8, 0))
    private val today = now.toLocalDate()
    private val employee = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf())

    private val mockId = AbsenceId(UUID.randomUUID())

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertTestCareArea(testArea)
            tx.insertTestDaycare(testDaycare)
            tx.insertTestDaycareGroup(testDaycareGroup)

            tx.insertTestEmployee(testDecisionMaker_1)
            tx.insertDaycareAclRow(testDaycare.id, testDecisionMaker_1.id, UserRole.UNIT_SUPERVISOR)

            tx.insertTestPerson(testChild_1)
            tx.insertTestChild(DevChild(id = testChild_1.id))

            tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = today,
                endDate = today.plusYears(1),
                type = PlacementType.PRESCHOOL_DAYCARE
            )

            tx.insertTestPerson(testChild_2)
            tx.insertTestChild(DevChild(id = testChild_2.id))

            tx.insertTestPlacement(
                childId = testChild_2.id,
                unitId = testDaycare.id,
                startDate = today,
                endDate = today.plusYears(1),
                type = PlacementType.PRESCHOOL_DAYCARE
            )
        }
    }

    @Test
    fun `correct absences are deleted`() {
        val firstAbsenceDate = today
        val lastAbsenceDate = today.plusDays(1)
        db.transaction { tx ->
            FiniteDateRange(firstAbsenceDate, lastAbsenceDate).dates().forEach { date ->
                tx.insertTestAbsence(
                    DevAbsence(
                        childId = testChild_1.id,
                        date = date,
                        absenceCategory = AbsenceCategory.BILLABLE
                    )
                )
                tx.insertTestAbsence(
                    DevAbsence(
                        childId = testChild_1.id,
                        date = date,
                        absenceCategory = AbsenceCategory.NONBILLABLE
                    )
                )
            }

            // Unrelated absence
            tx.insertTestAbsence(
                DevAbsence(
                    childId = testChild_2.id,
                    date = firstAbsenceDate,
                    absenceCategory = AbsenceCategory.BILLABLE
                )
            )
        }

        deleteAbsences(
            listOf(
                AbsenceDelete(
                    childId = testChild_1.id,
                    date = firstAbsenceDate,
                    category = AbsenceCategory.BILLABLE
                ),
                AbsenceDelete(
                    childId = testChild_1.id,
                    date = lastAbsenceDate,
                    category = AbsenceCategory.NONBILLABLE
                )
            )
        )

        assertEquals(
            listOf(
                Absence(
                    id = mockId,
                    childId = testChild_1.id,
                    date = firstAbsenceDate,
                    category = AbsenceCategory.NONBILLABLE,
                    absenceType = AbsenceType.OTHER_ABSENCE
                ),
                Absence(
                    id = mockId,
                    childId = testChild_1.id,
                    date = lastAbsenceDate,
                    category = AbsenceCategory.BILLABLE,
                    absenceType = AbsenceType.OTHER_ABSENCE
                ),
            ),
            getAbsencesOfChild(testChild_1.id).sortedWith(compareBy({ it.date }, { it.category }))
        )
        assertEquals(
            listOf(
                Absence(
                    id = mockId,
                    childId = testChild_2.id,
                    date = firstAbsenceDate,
                    category = AbsenceCategory.BILLABLE,
                    absenceType = AbsenceType.OTHER_ABSENCE
                ),
            ),
            getAbsencesOfChild(testChild_2.id)
        )
    }

    private fun getAbsencesOfChild(childId: ChildId): List<Absence> {
        return absenceController
            .absencesOfChild(
                dbInstance(),
                employee,
                MockEvakaClock(now),
                childId,
                today.year,
                today.monthValue
            )
            // We're not interested in IDs
            .map { it.copy(id = mockId) }
    }

    private fun deleteAbsences(absences: List<AbsenceDelete>) {
        absenceController.deleteAbsences(
            dbInstance(),
            employee,
            MockEvakaClock(now),
            testDaycareGroup.id,
            absences
        )
    }
}
