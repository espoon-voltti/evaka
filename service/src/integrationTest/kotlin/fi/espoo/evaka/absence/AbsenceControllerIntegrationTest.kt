// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.absence

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.holidayperiod.insertHolidayPeriod
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class AbsenceControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var absenceController: AbsenceController

    private val now = HelsinkiDateTime.of(LocalDate.of(2023, 6, 1), LocalTime.of(8, 0))
    private val today = now.toLocalDate()

    private lateinit var daycare: DevDaycare
    private lateinit var group: DevDaycareGroup
    private lateinit var employee: DevEmployee
    private lateinit var child1: DevPerson

    @BeforeEach
    fun beforeEach() {
        val area = DevCareArea()
        daycare = DevDaycare(areaId = area.id)
        group = DevDaycareGroup(daycareId = daycare.id)
        employee = DevEmployee()

        child1 = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)

            tx.insert(employee)
            tx.insertDaycareAclRow(daycare.id, employee.id, UserRole.UNIT_SUPERVISOR)

            tx.insert(child1, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = today,
                    endDate = today.plusYears(1)
                )
            )
        }
    }

    @Test
    fun `creating absences removes reservations from the unconfirmed range`() {
        val confirmedDate = today
        val unconfirmedDate = today.plusDays(14)

        db.transaction { tx ->
            listOf(confirmedDate, unconfirmedDate).forEach { date ->
                tx.insert(
                    DevReservation(
                        childId = child1.id,
                        date = date,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(16, 0),
                        createdBy = employee.evakaUserId
                    )
                )
            }
        }

        upsertAbsences(
            listOf(
                AbsenceUpsert(
                    childId = child1.id,
                    date = confirmedDate,
                    absenceType = AbsenceType.OTHER_ABSENCE,
                    category = AbsenceCategory.BILLABLE
                ),
                AbsenceUpsert(
                    childId = child1.id,
                    date = unconfirmedDate,
                    absenceType = AbsenceType.OTHER_ABSENCE,
                    category = AbsenceCategory.BILLABLE
                )
            )
        )

        // Confirmed reservation was kept, unconfirmed was removed
        assertEquals(
            listOf(
                Reservation(
                    childId = child1.id,
                    date = confirmedDate,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(16, 0)
                )
            ),
            getAllReservations()
        )
    }

    @Test
    fun `correct absences are deleted`() {
        val child2 = DevPerson()

        val firstAbsenceDate = today
        val lastAbsenceDate = today.plusDays(1)
        db.transaction { tx ->
            tx.insert(child2, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child2.id,
                    unitId = daycare.id,
                    startDate = today,
                    endDate = today.plusYears(1)
                )
            )

            FiniteDateRange(firstAbsenceDate, lastAbsenceDate).dates().forEach { date ->
                tx.insert(
                    DevAbsence(
                        childId = child1.id,
                        date = date,
                        absenceCategory = AbsenceCategory.BILLABLE,
                        modifiedAt = now
                    )
                )
                tx.insert(
                    DevAbsence(
                        childId = child1.id,
                        date = date,
                        absenceCategory = AbsenceCategory.NONBILLABLE,
                        modifiedAt = now
                    )
                )
            }

            // Unrelated absence
            tx.insert(
                DevAbsence(
                    childId = child2.id,
                    date = firstAbsenceDate,
                    absenceCategory = AbsenceCategory.BILLABLE,
                    modifiedAt = now
                )
            )
        }

        addPresences(
            listOf(
                Presence(
                    childId = child1.id,
                    date = firstAbsenceDate,
                    category = AbsenceCategory.BILLABLE
                ),
                Presence(
                    childId = child1.id,
                    date = lastAbsenceDate,
                    category = AbsenceCategory.NONBILLABLE
                )
            )
        )

        assertEquals(
            listOf(
                Absence(
                    childId = child1.id,
                    date = firstAbsenceDate,
                    category = AbsenceCategory.NONBILLABLE,
                    absenceType = AbsenceType.OTHER_ABSENCE,
                    modifiedByStaff = true,
                    modifiedAt = now
                ),
                Absence(
                    childId = child1.id,
                    date = lastAbsenceDate,
                    category = AbsenceCategory.BILLABLE,
                    absenceType = AbsenceType.OTHER_ABSENCE,
                    modifiedByStaff = true,
                    modifiedAt = now
                )
            ),
            getAbsencesOfChild(child1.id).sortedWith(compareBy({ it.date }, { it.category }))
        )
        assertEquals(
            listOf(
                Absence(
                    childId = child2.id,
                    date = firstAbsenceDate,
                    category = AbsenceCategory.BILLABLE,
                    absenceType = AbsenceType.OTHER_ABSENCE,
                    modifiedByStaff = true,
                    modifiedAt = now
                )
            ),
            getAbsencesOfChild(child2.id)
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
                    childId = child1.id,
                    date = startDate.plusDays(1),
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(16, 0),
                    createdBy = employee.evakaUserId
                )
            )
            tx.insert(
                DevReservation(
                    childId = child1.id,
                    date = startDate.plusDays(2),
                    startTime = null,
                    endTime = null,
                    createdBy = employee.evakaUserId
                )
            )
        }

        addPresences(
            FiniteDateRange(startDate, endDate)
                .dates()
                .map { date ->
                    Presence(childId = child1.id, date = date, category = AbsenceCategory.BILLABLE)
                }.toList()
        )

        assertEquals(
            listOf(
                Reservation(
                    childId = child1.id,
                    date = startDate.plusDays(1),
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(16, 0)
                ),
                Reservation(
                    childId = child1.id,
                    date = startDate.plusDays(2),
                    startTime = null,
                    endTime = null
                ),
                // This was added
                Reservation(
                    childId = child1.id,
                    date = startDate.plusDays(3),
                    startTime = null,
                    endTime = null
                )
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
                    childId = child1.id,
                    date = startDate,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(16, 0),
                    createdBy = employee.evakaUserId
                )
            )
            tx.insert(
                DevReservation(
                    childId = child1.id,
                    date = startDate.plusDays(1),
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(16, 0),
                    createdBy = employee.evakaUserId
                )
            )
            tx.insert(
                DevReservation(
                    childId = child1.id,
                    date = startDate.plusDays(2),
                    startTime = null,
                    endTime = null,
                    createdBy = employee.evakaUserId
                )
            )

            tx.insert(
                DevAbsence(
                    childId = child1.id,
                    date = startDate,
                    absenceCategory = AbsenceCategory.BILLABLE,
                    modifiedAt = now
                )
            )
            tx.insert(
                DevAbsence(
                    childId = child1.id,
                    date = startDate.plusDays(1),
                    absenceCategory = AbsenceCategory.BILLABLE,
                    modifiedAt = now
                )
            )
        }

        deleteHolidayReservations(
            FiniteDateRange(startDate, endDate)
                .dates()
                .map { date ->
                    AbsenceController.HolidayReservationsDelete(
                        childId = child1.id,
                        date = date
                    )
                }.toList()
        )

        assertEquals(
            listOf(
                // This was kept
                Reservation(
                    childId = child1.id,
                    date = startDate,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(16, 0)
                )
            ),
            getAllReservations()
        )

        assertEquals(
            listOf(
                // This was kept
                Absence(
                    childId = child1.id,
                    date = startDate,
                    category = AbsenceCategory.BILLABLE,
                    absenceType = AbsenceType.OTHER_ABSENCE,
                    modifiedByStaff = true,
                    modifiedAt = now
                )
            ),
            getAbsencesOfChild(child1.id)
        )
    }

    private fun getAbsencesOfChild(childId: ChildId): List<Absence> =
        absenceController.getAbsencesOfChild(
            dbInstance(),
            employee.user,
            MockEvakaClock(now),
            childId,
            today.year,
            today.monthValue
        )

    private fun upsertAbsences(absences: List<AbsenceUpsert>) {
        absenceController.upsertAbsences(
            dbInstance(),
            employee.user,
            MockEvakaClock(now),
            absences,
            group.id
        )
    }

    private fun addPresences(presences: List<Presence>) {
        absenceController.addPresences(
            dbInstance(),
            employee.user,
            MockEvakaClock(now),
            group.id,
            presences
        )
    }

    private fun deleteHolidayReservations(deletions: List<AbsenceController.HolidayReservationsDelete>) {
        absenceController.deleteHolidayReservations(
            dbInstance(),
            employee.user,
            MockEvakaClock(now),
            group.id,
            deletions
        )
    }

    private data class Reservation(
        val childId: ChildId,
        val date: LocalDate,
        val startTime: LocalTime?,
        val endTime: LocalTime?
    )

    private fun getAllReservations(): List<Reservation> =
        db.read { tx ->
            @Suppress("DEPRECATION")
            tx
                .createQuery(
                    """
                SELECT child_id, date, start_time, end_time
                FROM attendance_reservation
                ORDER BY child_id, date
                """
                ).toList<Reservation>()
        }
}
