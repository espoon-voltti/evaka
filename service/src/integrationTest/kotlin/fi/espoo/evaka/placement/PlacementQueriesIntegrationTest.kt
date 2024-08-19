// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.shared.AbsenceId
import fi.espoo.evaka.shared.AttendanceReservationId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class PlacementQueriesIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val today = LocalDate.of(2024, 3, 4) // mon
    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)

    @Test
    fun `deleteFutureReservationsAndAbsencesOutsideValidPlacements for club placement`() {
        `deleteFutureReservationsAndAbsencesOutsideValidPlacements works`(
            placementType = PlacementType.CLUB,
            hasReservations = false,
            hasBillableAbsences = false,
            hasNonbillableAbsences = true
        )
    }

    @Test
    fun `deleteFutureReservationsAndAbsencesOutsideValidPlacements for daycare placement`() {
        `deleteFutureReservationsAndAbsencesOutsideValidPlacements works`(
            placementType = PlacementType.DAYCARE,
            hasReservations = true,
            hasBillableAbsences = true,
            hasNonbillableAbsences = false
        )
    }

    @Test
    fun `deleteFutureReservationsAndAbsencesOutsideValidPlacements for preschool placement`() {
        `deleteFutureReservationsAndAbsencesOutsideValidPlacements works`(
            placementType = PlacementType.PRESCHOOL,
            hasReservations = false,
            hasBillableAbsences = false,
            hasNonbillableAbsences = true
        )
    }

    @Test
    fun `deleteFutureReservationsAndAbsencesOutsideValidPlacements for preschool_daycare placement`() {
        `deleteFutureReservationsAndAbsencesOutsideValidPlacements works`(
            placementType = PlacementType.PRESCHOOL_DAYCARE,
            hasReservations = true,
            hasBillableAbsences = true,
            hasNonbillableAbsences = true
        )
    }

    @Test
    fun `deleteFutureReservationsAndAbsencesOutsideValidPlacements for preparatory placement`() {
        `deleteFutureReservationsAndAbsencesOutsideValidPlacements works`(
            placementType = PlacementType.PREPARATORY,
            hasReservations = false,
            hasBillableAbsences = false,
            hasNonbillableAbsences = true
        )
    }

    @Test
    fun `deleteFutureReservationsAndAbsencesOutsideValidPlacements for preparatory_daycare placement`() {
        `deleteFutureReservationsAndAbsencesOutsideValidPlacements works`(
            placementType = PlacementType.PREPARATORY_DAYCARE,
            hasReservations = true,
            hasBillableAbsences = true,
            hasNonbillableAbsences = true
        )
    }

    @Test
    fun `deleteFutureReservationsAndAbsencesOutsideValidPlacements for daycare_five_year_olds placement`() {
        `deleteFutureReservationsAndAbsencesOutsideValidPlacements works`(
            placementType = PlacementType.DAYCARE_FIVE_YEAR_OLDS,
            hasReservations = true,
            hasBillableAbsences = true,
            hasNonbillableAbsences = true
        )
    }

    private fun `deleteFutureReservationsAndAbsencesOutsideValidPlacements works`(
        placementType: PlacementType,
        hasReservations: Boolean,
        hasBillableAbsences: Boolean,
        hasNonbillableAbsences: Boolean,
    ) {
        val child = DevPerson()
        val placementPeriod = FiniteDateRange(today.minusYears(1), today.plusDays(3))
        val placement =
            DevPlacement(
                type = placementType,
                unitId = daycare.id,
                startDate = placementPeriod.start,
                endDate = placementPeriod.end,
                childId = child.id
            )

        val reservationYesterday =
            DevReservation(
                childId = child.id,
                date = today.minusDays(1),
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(17, 0),
                createdBy = AuthenticatedUser.SystemInternalUser.evakaUserId
            )
        val reservationDuringPlacement =
            DevReservation(
                childId = child.id,
                date = today.plusDays(1),
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(17, 0),
                createdBy = AuthenticatedUser.SystemInternalUser.evakaUserId
            )
        val reservationAfterPlacement =
            DevReservation(
                childId = child.id,
                date = placementPeriod.end.plusDays(1),
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(17, 0),
                createdBy = AuthenticatedUser.SystemInternalUser.evakaUserId
            )

        val billableAbsenceYesterday =
            DevAbsence(
                childId = child.id,
                absenceType = AbsenceType.PLANNED_ABSENCE,
                absenceCategory = AbsenceCategory.BILLABLE,
                date = today.minusDays(1)
            )
        val billableAbsenceDuringPlacement =
            DevAbsence(
                childId = child.id,
                absenceType = AbsenceType.PLANNED_ABSENCE,
                absenceCategory = AbsenceCategory.BILLABLE,
                date = today.plusDays(1)
            )
        val billableAbsenceAfterPlacement =
            DevAbsence(
                childId = child.id,
                absenceType = AbsenceType.PLANNED_ABSENCE,
                absenceCategory = AbsenceCategory.BILLABLE,
                date = placementPeriod.end.plusDays(1)
            )

        val nonbillableAbsenceYesterday =
            DevAbsence(
                childId = child.id,
                absenceType = AbsenceType.PLANNED_ABSENCE,
                absenceCategory = AbsenceCategory.NONBILLABLE,
                date = today.minusDays(1)
            )
        val nonbillableAbsenceDuringPlacement =
            DevAbsence(
                childId = child.id,
                absenceType = AbsenceType.PLANNED_ABSENCE,
                absenceCategory = AbsenceCategory.NONBILLABLE,
                date = today.plusDays(1)
            )
        val nonbillableAbsenceAfterPlacement =
            DevAbsence(
                childId = child.id,
                absenceType = AbsenceType.PLANNED_ABSENCE,
                absenceCategory = AbsenceCategory.NONBILLABLE,
                date = placementPeriod.end.plusDays(1)
            )

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(placement)
            tx.insert(reservationYesterday)
            tx.insert(reservationDuringPlacement)
            tx.insert(reservationAfterPlacement)
            tx.insert(billableAbsenceYesterday)
            tx.insert(billableAbsenceDuringPlacement)
            tx.insert(billableAbsenceAfterPlacement)
            tx.insert(nonbillableAbsenceYesterday)
            tx.insert(nonbillableAbsenceDuringPlacement)
            tx.insert(nonbillableAbsenceAfterPlacement)
        }

        db.transaction { tx ->
            tx.deleteFutureReservationsAndAbsencesOutsideValidPlacements(child.id, today)
        }

        val reservationIds =
            db.read {
                it.createQuery { sql("SELECT id FROM attendance_reservation") }
                    .toSet<AttendanceReservationId>()
            }
        assertTrue(reservationIds.contains(reservationYesterday.id))
        assertEquals(hasReservations, reservationIds.contains(reservationDuringPlacement.id))
        assertFalse(reservationIds.contains(reservationAfterPlacement.id))

        val absenceIds =
            db.read { it.createQuery { sql("SELECT id FROM absence") }.toSet<AbsenceId>() }
        assertTrue(absenceIds.contains(billableAbsenceYesterday.id))
        assertTrue(absenceIds.contains(nonbillableAbsenceYesterday.id))
        assertEquals(hasBillableAbsences, absenceIds.contains(billableAbsenceDuringPlacement.id))
        assertEquals(
            hasNonbillableAbsences,
            absenceIds.contains(nonbillableAbsenceDuringPlacement.id)
        )
        assertFalse(absenceIds.contains(billableAbsenceAfterPlacement.id))
        assertFalse(absenceIds.contains(nonbillableAbsenceAfterPlacement.id))
    }
}
