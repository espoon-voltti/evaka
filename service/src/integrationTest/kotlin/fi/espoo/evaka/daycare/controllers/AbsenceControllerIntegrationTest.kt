// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.service.Absence
import fi.espoo.evaka.daycare.service.AbsenceCategory
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.daycare.service.Presence
import fi.espoo.evaka.holidayperiod.insertHolidayPeriod
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AbsenceId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.insert
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
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testDaycareGroup)

            tx.insert(testDecisionMaker_1)
            tx.insertDaycareAclRow(testDaycare.id, testDecisionMaker_1.id, UserRole.UNIT_SUPERVISOR)

            tx.insert(testChild_1, DevPersonType.CHILD)

            tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = today,
                endDate = today.plusYears(1),
                type = PlacementType.PRESCHOOL_DAYCARE
            )

            tx.insert(testChild_2, DevPersonType.CHILD)

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
                tx.insert(
                    DevAbsence(
                        childId = testChild_1.id,
                        date = date,
                        absenceCategory = AbsenceCategory.BILLABLE
                    )
                )
                tx.insert(
                    DevAbsence(
                        childId = testChild_1.id,
                        date = date,
                        absenceCategory = AbsenceCategory.NONBILLABLE
                    )
                )
            }

            // Unrelated absence
            tx.insert(
                DevAbsence(
                    childId = testChild_2.id,
                    date = firstAbsenceDate,
                    absenceCategory = AbsenceCategory.BILLABLE
                )
            )
        }

        addPresences(
            listOf(
                Presence(
                    childId = testChild_1.id,
                    date = firstAbsenceDate,
                    category = AbsenceCategory.BILLABLE
                ),
                Presence(
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

    @Test
    fun `deleting absences adds missing holiday reservations`() {
        //                   0 1 2 3
        // holiday period:   - x x x
        // reservations:     - x x -
        // -------------------------
        // result:           - x x x

        val startDate = today
        val endDate = today.plusDays(3)
        db.transaction { tx ->
            tx.insertHolidayPeriod(
                period = FiniteDateRange(startDate.plusDays(1), endDate),
                reservationDeadline = today
            )

            tx.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = startDate.plusDays(1),
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(16, 0),
                    createdBy = EvakaUserId(employee.id.raw)
                )
            )
            tx.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = startDate.plusDays(2),
                    startTime = null,
                    endTime = null,
                    createdBy = EvakaUserId(employee.id.raw)
                )
            )
        }

        addPresences(
            FiniteDateRange(startDate, endDate)
                .dates()
                .map { date ->
                    Presence(
                        childId = testChild_1.id,
                        date = date,
                        category = AbsenceCategory.BILLABLE
                    )
                }
                .toList()
        )

        assertEquals(
            listOf(
                Reservation(
                    childId = testChild_1.id,
                    date = startDate.plusDays(1),
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(16, 0)
                ),
                Reservation(
                    childId = testChild_1.id,
                    date = startDate.plusDays(2),
                    startTime = null,
                    endTime = null
                ),

                // This was added
                Reservation(
                    childId = testChild_1.id,
                    date = startDate.plusDays(3),
                    startTime = null,
                    endTime = null
                ),
            ),
            getAllReservations()
        )
    }

    @Test
    fun `deleting holiday reservations deletes reservations and absences`() {
        //                   0 1 2 3
        // holiday period:   - x x x
        // reservations:     r r r -
        // absences:         a a - -
        // -------------------------
        // result:           r - - -
        //                   a - - -

        val startDate = today
        val endDate = today.plusDays(3)
        db.transaction { tx ->
            tx.insertHolidayPeriod(
                period = FiniteDateRange(startDate.plusDays(1), endDate),
                reservationDeadline = today
            )

            tx.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = startDate,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(16, 0),
                    createdBy = EvakaUserId(employee.id.raw)
                )
            )
            tx.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = startDate.plusDays(1),
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(16, 0),
                    createdBy = EvakaUserId(employee.id.raw)
                )
            )
            tx.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = startDate.plusDays(2),
                    startTime = null,
                    endTime = null,
                    createdBy = EvakaUserId(employee.id.raw)
                )
            )

            tx.insert(
                DevAbsence(
                    childId = testChild_1.id,
                    date = startDate,
                    absenceCategory = AbsenceCategory.BILLABLE
                )
            )
            tx.insert(
                DevAbsence(
                    childId = testChild_1.id,
                    date = startDate.plusDays(1),
                    absenceCategory = AbsenceCategory.BILLABLE
                )
            )
        }

        deleteHolidayReservations(
            FiniteDateRange(startDate, endDate)
                .dates()
                .map { date ->
                    AbsenceController.HolidayReservationsDelete(
                        childId = testChild_1.id,
                        date = date,
                    )
                }
                .toList()
        )

        assertEquals(
            listOf(
                // This was kept
                Reservation(
                    childId = testChild_1.id,
                    date = startDate,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(16, 0)
                ),
            ),
            getAllReservations()
        )

        assertEquals(
            listOf(
                // This was kept
                Absence(
                    id = mockId,
                    childId = testChild_1.id,
                    date = startDate,
                    category = AbsenceCategory.BILLABLE,
                    absenceType = AbsenceType.OTHER_ABSENCE
                ),
            ),
            getAbsencesOfChild(testChild_1.id)
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

    private fun addPresences(absences: List<Presence>) {
        absenceController.addPresences(
            dbInstance(),
            employee,
            MockEvakaClock(now),
            testDaycareGroup.id,
            absences
        )
    }

    private fun deleteHolidayReservations(
        deletions: List<AbsenceController.HolidayReservationsDelete>
    ) {
        absenceController.deleteHolidayReservations(
            dbInstance(),
            employee,
            MockEvakaClock(now),
            testDaycareGroup.id,
            deletions
        )
    }

    private data class Reservation(
        val childId: ChildId,
        val date: LocalDate,
        val startTime: LocalTime?,
        val endTime: LocalTime?
    )

    private fun getAllReservations(): List<Reservation> {
        return db.read { tx ->
            tx.createQuery(
                    """
                SELECT child_id, date, start_time, end_time
                FROM attendance_reservation
                ORDER BY child_id, date
                """
                )
                .toList<Reservation>()
        }
    }
}
