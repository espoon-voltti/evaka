// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.absence

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimesType
import fi.espoo.evaka.dailyservicetimes.deleteChildDailyServiceTimes
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AbsenceId
import fi.espoo.evaka.shared.DailyServiceTimesId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevDailyServiceTimes
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testChild_4
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.unitSupervisorOfTestDaycare
import fi.espoo.evaka.user.EvakaUserType
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AbsenceGenerationIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val placementPeriod =
        FiniteDateRange(LocalDate.of(2020, 11, 1), LocalDate.of(2020, 11, 30))

    private val today = placementPeriod.start
    private val now = HelsinkiDateTime.of(today, LocalTime.of(0, 0))

    private val mondays = listOf(2, 9, 16, 23, 30).map { today.withDayOfMonth(it) }
    private val tuesdays = listOf(3, 10, 17, 24).map { today.withDayOfMonth(it) }
    private val wednesdays = listOf(4, 11, 18, 25).map { today.withDayOfMonth(it) }

    private val present = TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(
                unitSupervisorOfTestDaycare,
                mapOf(testDaycare.id to UserRole.UNIT_SUPERVISOR),
            )
            listOf(testChild_1, testChild_2, testChild_3, testChild_4).forEach {
                tx.insert(it, DevPersonType.CHILD)
            }
        }
    }

    @Test
    fun `does not generate absences without placement or without irregular daily service times`() {
        db.transaction { tx ->
            // No daily service times at all
            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = placementPeriod.start,
                    endDate = placementPeriod.end,
                )
            )

            // Regular daily service times
            tx.insert(
                DevPlacement(
                    childId = testChild_2.id,
                    unitId = testDaycare.id,
                    startDate = placementPeriod.start,
                    endDate = placementPeriod.end,
                )
            )
            tx.insert(
                DevDailyServiceTimes(
                    childId = testChild_2.id,
                    validityPeriod = DateRange(today, null),
                    type = DailyServiceTimesType.REGULAR,
                    regularTimes = present,
                )
            )

            // Irregular daily service times, no time for any day
            tx.insert(
                DevPlacement(
                    childId = testChild_3.id,
                    unitId = testDaycare.id,
                    startDate = placementPeriod.start,
                    endDate = placementPeriod.end,
                )
            )
            tx.insert(
                DevDailyServiceTimes(
                    childId = testChild_3.id,
                    validityPeriod = DateRange(today, null),
                    type = DailyServiceTimesType.IRREGULAR,
                )
            )

            // Irregular daily service times but no placement
            tx.insert(
                DevDailyServiceTimes(
                    childId = testChild_4.id,
                    validityPeriod = DateRange(today, null),
                    type = DailyServiceTimesType.IRREGULAR,
                    mondayTimes = null,
                    tuesdayTimes = present,
                    wednesdayTimes = present,
                    thursdayTimes = present,
                    fridayTimes = present,
                )
            )
        }

        db.transaction { tx ->
            listOf(testChild_1.id, testChild_2.id).forEach {
                generateAbsencesFromIrregularDailyServiceTimes(tx, now, it)
            }
        }

        assertEquals(emptyList(), getAllAbsences())
    }

    @Test
    fun `generates correct absence categories`() {
        db.transaction { tx ->
            // Absent on mondays
            listOf(testChild_1.id, testChild_2.id, testChild_3.id).forEach {
                tx.insert(
                    DevDailyServiceTimes(
                        childId = it,
                        validityPeriod = DateRange(today, null),
                        type = DailyServiceTimesType.IRREGULAR,
                        mondayTimes = null,
                        tuesdayTimes = present,
                        wednesdayTimes = present,
                        thursdayTimes = present,
                        fridayTimes = present,
                    )
                )
            }

            // Daycare placement -> BILLABLE absences are generated
            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = placementPeriod.start,
                    endDate = placementPeriod.end,
                )
            )

            // Preschool+daycare placement -> BILLABLE and NONBILLABLE absences are generated
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChild_2.id,
                    unitId = testDaycare.id,
                    startDate = placementPeriod.start,
                    endDate = placementPeriod.end,
                )
            )

            // Preschool placement -> NONBILLABLE absences are generated
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL,
                    childId = testChild_3.id,
                    unitId = testDaycare.id,
                    startDate = placementPeriod.start,
                    endDate = placementPeriod.end,
                )
            )
        }

        db.transaction { tx ->
            listOf(testChild_1.id, testChild_2.id, testChild_3.id).forEach {
                generateAbsencesFromIrregularDailyServiceTimes(tx, now, it)
            }
        }

        val absences = getAllAbsences().groupBy { it.childId }
        absences[testChild_1.id]!!.let { child1Absences ->
            child1Absences.forEach {
                assertEquals(AbsenceCategory.BILLABLE, it.category)
                assertEquals(AbsenceType.OTHER_ABSENCE, it.absenceType)
                assertEquals(EvakaUserType.SYSTEM, it.modifiedByType)
                assertEquals(now, it.modifiedAt)
            }
            assertEquals(mondays, child1Absences.map { it.date })
        }
        absences[testChild_2.id]!!.let { child2Absences ->
            val byDate = child2Absences.groupBy { it.date }
            assertEquals(mondays, byDate.keys.sorted())
            byDate.values.forEach { dateAbsences ->
                assertEquals(2, dateAbsences.size)
                assertEquals(AbsenceCategory.BILLABLE, dateAbsences[0].category)
                assertEquals(AbsenceCategory.NONBILLABLE, dateAbsences[1].category)

                dateAbsences.forEach {
                    assertEquals(EvakaUserType.SYSTEM, it.modifiedByType)
                    assertEquals(now, it.modifiedAt)
                    assertEquals(AbsenceType.OTHER_ABSENCE, it.absenceType)
                }
            }
        }
        absences[testChild_3.id]!!.let { child3Absences ->
            child3Absences.forEach {
                assertEquals(AbsenceCategory.NONBILLABLE, it.category)
                assertEquals(AbsenceType.OTHER_ABSENCE, it.absenceType)
                assertEquals(EvakaUserType.SYSTEM, it.modifiedByType)
                assertEquals(now, it.modifiedAt)
            }
            assertEquals(mondays, child3Absences.map { it.date })
        }
    }

    @Test
    fun `does not override existing absences or reservations on non-absence days`() {
        val existingAbsenceId = AbsenceId(UUID.randomUUID())
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = placementPeriod.start,
                    endDate = placementPeriod.end,
                )
            )
            tx.insert(
                DevDailyServiceTimes(
                    childId = testChild_1.id,
                    validityPeriod = DateRange(today, null),
                    type = DailyServiceTimesType.IRREGULAR,
                    mondayTimes = null,
                    tuesdayTimes = present,
                    wednesdayTimes = present,
                    thursdayTimes = present,
                    fridayTimes = present,
                )
            )

            tx.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = mondays[1],
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(12, 0),
                    createdBy = EvakaUserId(unitSupervisorOfTestDaycare.id.raw),
                )
            )
            tx.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = tuesdays[1],
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(12, 0),
                    createdBy = EvakaUserId(unitSupervisorOfTestDaycare.id.raw),
                )
            )
            // Reservation with no times
            tx.insert(
                DevReservation(
                    childId = testChild_1.id,
                    date = tuesdays[2],
                    startTime = null,
                    endTime = null,
                    createdBy = EvakaUserId(unitSupervisorOfTestDaycare.id.raw),
                )
            )
            tx.insert(
                DevAbsence(
                    id = existingAbsenceId,
                    childId = testChild_1.id,
                    date = mondays[3],
                    absenceType = AbsenceType.SICKLEAVE,
                    modifiedBy = EvakaUserId(unitSupervisorOfTestDaycare.id.raw),
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
        }

        db.transaction { tx ->
            generateAbsencesFromIrregularDailyServiceTimes(tx, now, testChild_1.id)
        }

        // The absence created above is not overridden, and absences are created for "empty" mondays
        // only. Monday reservations are cleared, tuesdays not.
        val absences = getAllAbsences()
        assertEquals(mondays.toSet(), absences.map { it.date }.toSet())
        assertEquals(mondays, absences.map { it.date })
        assertTrue(absences.any { it.id == existingAbsenceId })
        assertEquals(setOf(tuesdays[1], tuesdays[2]), getAllReservationDates())
    }

    @Test
    fun `deletes old generated absences`() {
        val existingAbsenceId = AbsenceId(UUID.randomUUID())
        val dailyServiceTimesId = DailyServiceTimesId(UUID.randomUUID())
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = placementPeriod.start,
                    endDate = placementPeriod.end,
                )
            )

            // Absent on mondays and tuesdays
            tx.insert(
                DevDailyServiceTimes(
                    id = dailyServiceTimesId,
                    childId = testChild_1.id,
                    validityPeriod = DateRange(today, null),
                    type = DailyServiceTimesType.IRREGULAR,
                    mondayTimes = null,
                    tuesdayTimes = null,
                    wednesdayTimes = present,
                    thursdayTimes = present,
                    fridayTimes = present,
                )
            )

            tx.insert(
                DevAbsence(
                    id = existingAbsenceId,
                    childId = testChild_1.id,
                    date = wednesdays[0],
                    absenceType = AbsenceType.SICKLEAVE,
                    modifiedAt = now.minusDays(1),
                    modifiedBy = EvakaUserId(unitSupervisorOfTestDaycare.id.raw),
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
        }

        db.transaction { tx ->
            generateAbsencesFromIrregularDailyServiceTimes(tx, now, testChild_1.id)
        }

        run {
            val absences = getAllAbsences()
            val (generated, existing) = absences.partition { it.id != existingAbsenceId }

            assertEquals(mondays.size + tuesdays.size, generated.size)
            assertEquals((mondays + tuesdays).sorted(), generated.map { it.date })
            generated.forEach {
                assertEquals(EvakaUserType.SYSTEM, it.modifiedByType)
                assertEquals(now, it.modifiedAt)
            }

            assertEquals(1, existing.size)
            existing.first().let {
                assertEquals(wednesdays[0], it.date)
                assertEquals(AbsenceCategory.BILLABLE, it.category)
                assertEquals(AbsenceType.SICKLEAVE, it.absenceType)
                assertEquals(EvakaUserType.EMPLOYEE, it.modifiedByType)
                assertEquals(now.minusDays(1), it.modifiedAt)
            }
        }

        db.transaction { tx ->
            tx.deleteChildDailyServiceTimes(dailyServiceTimesId)

            // Absent on mondays and wednesdays
            tx.insert(
                DevDailyServiceTimes(
                    childId = testChild_1.id,
                    validityPeriod = DateRange(today, null),
                    type = DailyServiceTimesType.IRREGULAR,
                    mondayTimes = null,
                    tuesdayTimes = present,
                    wednesdayTimes = null,
                    thursdayTimes = present,
                    fridayTimes = present,
                )
            )
        }

        db.transaction { tx ->
            generateAbsencesFromIrregularDailyServiceTimes(tx, now.plusHours(1), testChild_1.id)
        }

        run {
            val absences = getAllAbsences()
            val (generated, existing) = absences.partition { it.id != existingAbsenceId }

            assertEquals(mondays.size + wednesdays.size - 1, generated.size)
            assertEquals(
                (mondays + wednesdays.takeLast(wednesdays.size - 1)).sorted(),
                generated.map { it.date },
            )
            generated.forEach {
                assertEquals(EvakaUserType.SYSTEM, it.modifiedByType)
                assertEquals(now.plusHours(1), it.modifiedAt)
            }

            // Didn't touch the existing absence
            assertEquals(1, existing.size)
            existing.first().let {
                assertEquals(wednesdays[0], it.date)
                assertEquals(AbsenceCategory.BILLABLE, it.category)
                assertEquals(AbsenceType.SICKLEAVE, it.absenceType)
                assertEquals(EvakaUserType.EMPLOYEE, it.modifiedByType)
                assertEquals(now.minusDays(1), it.modifiedAt)
            }
        }
    }

    private data class Absence(
        val id: AbsenceId,
        val childId: PersonId,
        val date: LocalDate,
        val category: AbsenceCategory,
        val absenceType: AbsenceType,
        val modifiedByType: EvakaUserType,
        val modifiedAt: HelsinkiDateTime,
    )

    private fun getAllAbsences(): List<Absence> {
        return db.read {
            it.createQuery {
                    sql(
                        """
SELECT a.id, a.child_id, a.date, a.category, a.absence_type, eu.type AS modified_by_type, a.modified_at AS modified_at
FROM absence a
LEFT JOIN evaka_user eu ON eu.id = a.modified_by
ORDER BY a.date, a.category
"""
                    )
                }
                .toList<Absence>()
        }
    }

    private fun getAllReservationDates(): Set<LocalDate> {
        return db.read {
            it.createQuery {
                    sql(
                        """
SELECT date FROM attendance_reservation
"""
                    )
                }
                .toSet<LocalDate>()
        }
    }
}
