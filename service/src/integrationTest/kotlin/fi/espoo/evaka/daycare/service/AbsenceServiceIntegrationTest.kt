// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.service

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.insertTestBackupCare
import fi.espoo.evaka.shared.dev.insertTestChild
import fi.espoo.evaka.shared.dev.insertTestChildAttendance
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.insertTestReservation
import fi.espoo.evaka.shared.dev.resetDatabase
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AbsenceServiceIntegrationTest : FullApplicationTest() {
    @Autowired
    lateinit var absenceService: AbsenceService

    val childId = testChild_1.id
    val testUserId = EmployeeId(UUID.randomUUID())
    val daycareId = testDaycare.id
    val daycareName = testDaycare.name
    val groupId = GroupId(UUID.randomUUID())
    val groupName = "Testiryhmä"
    val placementStart = LocalDate.of(2019, 8, 1)
    val placementEnd = LocalDate.of(2019, 12, 31)

    @BeforeEach
    private fun prepare() {
        db.transaction {
            it.insertGeneralTestFixtures()
            it.insertTestEmployee(DevEmployee(id = testUserId))
            it.insertTestChild(DevChild(childId))
            it.insertTestDaycareGroup(
                DevDaycareGroup(daycareId = daycareId, id = groupId, name = groupName, startDate = placementStart)
            )
        }
    }

    @AfterEach
    private fun afterEach() {
        db.transaction { tx -> tx.resetDatabase() }
    }

    @Test
    fun `get group with placement`() {
        insertGroupPlacement(childId, PlacementType.PRESCHOOL_DAYCARE)

        val placementDate = placementStart
        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, placementDate.year, placementDate.monthValue) }

        assertEquals(groupId, result.groupId)
        assertEquals(daycareName, result.daycareName)
        assertEquals(groupName, result.groupName)
        assertEquals(1, result.children.size)
    }

    @Test
    fun `get group with multiple placements`() {
        insertGroupPlacement(childId, PlacementType.PRESCHOOL_DAYCARE)

        val placementDate = placementStart
        val groupSize = 50
        (1 until groupSize).forEach { _ ->
            insertChildAndGroupPlacement(ChildId(UUID.randomUUID()), PlacementType.PRESCHOOL_DAYCARE)
        }
        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, placementDate.year, placementDate.monthValue) }

        assertEquals(groupSize, result.children.size)
    }

    @Test
    fun `get group without placements`() {
        insertGroupPlacement(childId, PlacementType.PRESCHOOL_DAYCARE)

        val futureDate = placementEnd.plusMonths(1)
        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, futureDate.year, futureDate.monthValue) }

        assertEquals(groupId, result.groupId)
        assertEquals(groupName, result.groupName)
        assertEquals(0, result.children.size)
    }

    @Test
    fun `preschool placement with connected daycare maps correctly`() {
        insertGroupPlacement(childId, PlacementType.PRESCHOOL_DAYCARE)

        val placementDate = placementStart
        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, placementDate.year, placementDate.monthValue) }
        val daysInMonth = placementDate.month.length(false)
        val placements = result.children[0].placements
        val careTypes = placements.getValue(placementDate)

        assertEquals(daysInMonth, placements.size)
        assertEquals(2, careTypes.size)
        assertTrue(careTypes.contains(AbsenceCareType.PRESCHOOL))
        assertTrue(careTypes.contains(AbsenceCareType.PRESCHOOL_DAYCARE))
    }

    @Test
    fun `preschool placement without connected daycare maps correctly`() {
        insertGroupPlacement(childId, PlacementType.PRESCHOOL)

        val placementDate = placementStart
        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, placementDate.year, placementDate.monthValue) }
        val daysInMonth = placementDate.month.length(false)
        val placements = result.children[0].placements
        val careTypes = placements.getValue(placementDate)

        assertEquals(daysInMonth, placements.size)
        assertEquals(1, careTypes.size)
        assertEquals(true, careTypes.contains(AbsenceCareType.PRESCHOOL))
    }

    @Test
    fun `daycare placement maps correctly`() {
        insertGroupPlacement(childId, PlacementType.DAYCARE)

        val placementDate = placementStart
        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, placementDate.year, placementDate.monthValue) }
        val daysInMonth = placementDate.month.length(false)
        val placements = result.children[0].placements
        val careTypes = placements.getValue(placementDate)

        assertEquals(daysInMonth, placements.size)
        assertEquals(1, careTypes.size)
        assertTrue(careTypes.contains(AbsenceCareType.DAYCARE))
    }

    @Test
    fun `daycare placement maps correctly for 5-year-old children`() {
        insertGroupPlacement(childId, PlacementType.DAYCARE_FIVE_YEAR_OLDS)

        val placementDate = LocalDate.of(2019, 8, 1)
        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, placementDate.year, placementDate.monthValue) }
        val daysInMonth = placementDate.month.length(false)
        val placements = result.children[0].placements
        val careTypes = placements.getValue(placementDate)

        assertEquals(daysInMonth, placements.size)
        assertEquals(2, careTypes.size)
        assertTrue(careTypes.contains(AbsenceCareType.DAYCARE))
        assertTrue(careTypes.contains(AbsenceCareType.DAYCARE_5YO_FREE))
    }

    @Test
    fun `part time daycare placement maps correctly for 5-year-old children`() {
        insertGroupPlacement(childId, PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS)

        val placementDate = LocalDate.of(2019, 8, 1)
        val result =
            db.read { absenceService.getAbsencesByMonth(it, groupId, placementDate.year, placementDate.monthValue) }
        val daysInMonth = placementDate.month.length(false)
        val placements = result.children[0].placements
        val careTypes = placements.getValue(placementDate)

        assertEquals(daysInMonth, placements.size)
        assertEquals(2, careTypes.size)
        assertTrue(careTypes.contains(AbsenceCareType.DAYCARE))
        assertTrue(careTypes.contains(AbsenceCareType.DAYCARE_5YO_FREE))
    }

    @Test
    fun `get placements and absences for every month`() {
        insertGroupPlacement(childId, PlacementType.PRESCHOOL_DAYCARE)

        var placementDate = placementStart
        while (!placementDate.isAfter(placementEnd)) {
            val result = db.read { absenceService.getAbsencesByMonth(it, groupId, placementDate.year, placementDate.monthValue) }
            val daysInMonth = placementDate.month.length(false)
            val placements = result.children[0].placements
            val absences = result.children[0].absences

            assertEquals(0, absences.size)
            assertEquals(daysInMonth, placements.size)

            placementDate = placementDate.plusMonths(1)
        }
    }

    @Test
    fun `upsert absences`() {
        insertGroupPlacement(childId, PlacementType.PRESCHOOL_DAYCARE)

        val absenceDate = placementEnd
        val initialAbsence = createAbsence(childId, AbsenceCareType.PRESCHOOL, AbsenceType.SICKLEAVE, absenceDate)
        val initialAbsenceList = listOf(initialAbsence)

        val result = db.transaction { tx ->
            tx.upsertAbsences(initialAbsenceList, EvakaUserId(testUserId.raw))
            absenceService.getAbsencesByMonth(tx, groupId, absenceDate.year, absenceDate.monthValue)
        }
        val absence = result.children[0].absences.getValue(absenceDate)[0]

        assertEquals(initialAbsence.childId, absence.childId)
        assertEquals(initialAbsence.careType, absence.careType)
        assertEquals(initialAbsence.absenceType, absence.absenceType)
        assertEquals(initialAbsence.date, absence.date)
    }

    @Test
    fun `get absences for child without absences`() {
        insertGroupPlacement(childId, PlacementType.PRESCHOOL_DAYCARE)

        val absenceDate = placementEnd
        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, absenceDate.year, absenceDate.monthValue) }
        val absences = result.children[0].absences

        assertEquals(absences.size, 0)
    }

    @Test
    fun `get one absence`() {
        insertGroupPlacement(childId, PlacementType.PRESCHOOL_DAYCARE)

        val absenceDate = placementEnd
        val initialAbsence = createAbsence(childId, AbsenceCareType.DAYCARE, AbsenceType.SICKLEAVE, absenceDate)
        val initialAbsenceList = listOf(initialAbsence)

        val result = db.transaction { tx ->
            tx.upsertAbsences(initialAbsenceList, EvakaUserId(testUserId.raw))
            absenceService.getAbsencesByMonth(tx, groupId, absenceDate.year, absenceDate.monthValue)
        }
        val absence = result.children[0].absences.getValue(absenceDate)[0]

        assertEquals(initialAbsence.childId, absence.childId)
        assertEquals(absenceDate, absence.date)
        assertEquals(initialAbsence.careType, absence.careType)
        assertEquals(initialAbsence.absenceType, absence.absenceType)
    }

    @Test
    fun `get multiple absences`() {
        insertGroupPlacement(childId, PlacementType.PRESCHOOL_DAYCARE)

        val absenceDate = placementEnd
        val initialAbsence = createAbsence(childId, AbsenceCareType.DAYCARE, AbsenceType.SICKLEAVE, absenceDate)
        val initialAbsence2 = createAbsence(childId, AbsenceCareType.PRESCHOOL, AbsenceType.SICKLEAVE, absenceDate)
        val initialAbsenceList = listOf(initialAbsence, initialAbsence2)

        val result = db.transaction { tx ->
            tx.upsertAbsences(initialAbsenceList, EvakaUserId(testUserId.raw))
            absenceService.getAbsencesByMonth(tx, groupId, absenceDate.year, absenceDate.monthValue)
        }
        val absences = result.children[0].absences.getValue(absenceDate)

        assertEquals(initialAbsenceList.size, absences.size)
    }

    @Test
    fun `cannot create multiple absences for same placement type and same date`() {
        insertGroupPlacement(childId, PlacementType.PRESCHOOL_DAYCARE)

        val absenceDate = placementEnd
        val initialAbsence = createAbsence(childId, AbsenceCareType.DAYCARE, AbsenceType.SICKLEAVE, absenceDate)
        val initialAbsence2 = createAbsence(childId, AbsenceCareType.DAYCARE, AbsenceType.SICKLEAVE, absenceDate)
        val initialAbsenceList = listOf(initialAbsence, initialAbsence2)

        val result = db.transaction { tx ->
            tx.upsertAbsences(initialAbsenceList, EvakaUserId(testUserId.raw))
            absenceService.getAbsencesByMonth(tx, groupId, absenceDate.year, absenceDate.monthValue)
        }
        val absences = result.children[0].absences.getValue(absenceDate)

        assertEquals(1, absences.size)
    }

    @Test
    fun `modify absence`() {
        insertGroupPlacement(childId, PlacementType.PRESCHOOL_DAYCARE)

        val absenceDate = placementEnd
        val initialAbsence = createAbsence(childId, AbsenceCareType.DAYCARE, AbsenceType.SICKLEAVE, absenceDate)
        val initialAbsenceList = listOf(initialAbsence)

        var result = db.transaction { tx ->
            tx.upsertAbsences(initialAbsenceList, EvakaUserId(testUserId.raw))
            absenceService.getAbsencesByMonth(tx, groupId, absenceDate.year, absenceDate.monthValue)
        }
        val absence = result.children[0].absences.getValue(absenceDate)[0]

        val newAbsenceType = AbsenceType.UNKNOWN_ABSENCE
        val updatedAbsence = AbsenceUpsert(
            childId = absence.childId,
            date = absence.date,
            careType = absence.careType,
            absenceType = newAbsenceType
        )

        result = db.transaction { tx ->
            tx.upsertAbsences(listOf(updatedAbsence), EvakaUserId(testUserId.raw))
            absenceService.getAbsencesByMonth(tx, groupId, absenceDate.year, absenceDate.monthValue)
        }
        val absences = result.children[0].absences.getValue(absenceDate)
        val modifiedAbsence = result.children[0].absences.getValue(absenceDate)[0]

        assertEquals(1, absences.size)
        assertEquals(newAbsenceType, modifiedAbsence.absenceType)
    }

    @Test
    fun `get absence by childId`() {
        insertGroupPlacement(childId, PlacementType.PRESCHOOL_DAYCARE)

        val absenceDate = placementEnd
        val initialAbsence = createAbsence(childId, AbsenceCareType.DAYCARE, AbsenceType.SICKLEAVE, absenceDate)
        val initialAbsence2 = createAbsence(childId, AbsenceCareType.PRESCHOOL, AbsenceType.SICKLEAVE, absenceDate)
        val initialAbsenceList = listOf(initialAbsence, initialAbsence2)

        val absences = db.transaction { tx ->
            tx.upsertAbsences(initialAbsenceList, EvakaUserId(testUserId.raw))
            absenceService.getAbsencesByChild(tx, childId, absenceDate.year, absenceDate.monthValue)
        }
        assertEquals(initialAbsenceList.size, absences.size)
    }

    @Test
    fun `get absence by childId should not find anything with a wrong childId`() {
        val childId2 = ChildId(UUID.randomUUID())
        insertGroupPlacement(childId, PlacementType.PRESCHOOL_DAYCARE)
        db.transaction {
            it.insertTestPerson(DevPerson(id = childId2, dateOfBirth = LocalDate.of(2013, 1, 1)))
            it.insertTestChild(DevChild(childId2))
        }

        val absenceDate = placementEnd
        val initialAbsence = createAbsence(childId, AbsenceCareType.DAYCARE, AbsenceType.SICKLEAVE, absenceDate)
        val initialAbsence2 = createAbsence(childId, AbsenceCareType.PRESCHOOL, AbsenceType.SICKLEAVE, absenceDate)
        val initialAbsenceList = listOf(initialAbsence, initialAbsence2)

        val absences = db.transaction { tx ->
            tx.upsertAbsences(initialAbsenceList, EvakaUserId(testUserId.raw))
            absenceService.getAbsencesByChild(tx, childId2, absenceDate.year, absenceDate.monthValue)
        }
        assertEquals(0, absences.size)
    }

    @Test
    fun `get absence by childId find the absences within the date range`() {
        insertGroupPlacement(childId, PlacementType.PRESCHOOL_DAYCARE)

        val absenceDate = placementEnd
        val initialAbsence = createAbsence(childId, AbsenceCareType.DAYCARE, AbsenceType.SICKLEAVE, LocalDate.of(2019, 9, 1))
        val initialAbsence2 = createAbsence(childId, AbsenceCareType.PRESCHOOL, AbsenceType.SICKLEAVE, absenceDate)
        val initialAbsenceList = listOf(initialAbsence, initialAbsence2)

        val absences = db.transaction { tx ->
            tx.upsertAbsences(initialAbsenceList, EvakaUserId(testUserId.raw))
            absenceService.getAbsencesByChild(tx, childId, 2019, 9)
        }
        assertEquals(1, absences.size)
    }

    @Test
    fun `backup care children are returned with correct care types`() {
        db.transaction { tx ->
            tx.insertTestPlacement(
                childId = childId,
                unitId = daycareId,
                startDate = placementStart,
                endDate = placementEnd,
                type = PlacementType.PRESCHOOL_DAYCARE
            )
            tx.insertTestBackupCare(
                DevBackupCare(
                    childId = childId,
                    unitId = daycareId,
                    groupId = groupId,
                    period = FiniteDateRange(placementStart, placementEnd)
                )
            )
        }

        val placementDate = placementStart
        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, placementDate.year, placementDate.monthValue) }

        assertEquals(groupId, result.groupId)
        assertEquals(daycareName, result.daycareName)
        assertEquals(groupName, result.groupName)
        assertEquals(1, result.children.size)
        assertEquals(2, result.children.first().placements[placementDate]?.size)
        assertEquals(
            setOf(AbsenceCareType.PRESCHOOL, AbsenceCareType.PRESCHOOL_DAYCARE),
            result.children.first().placements[placementDate]?.toSet()
        )
    }

    @Test
    fun `group operational days do not include holidays`() {
        val firstOfJanuary2020 = LocalDate.of(2020, 1, 1)
        val epiphany2020 = LocalDate.of(2020, 1, 6)
        db.transaction { it.execute("INSERT INTO holiday (date) VALUES (?)", epiphany2020) }
        val result = db.read {
            absenceService.getAbsencesByMonth(it, groupId, firstOfJanuary2020.year, firstOfJanuary2020.monthValue)
        }

        assertFalse(result.operationDays.contains(epiphany2020))
    }

    @Test
    fun `group operational days include holidays if the unit is operational on every weekday`() {
        val firstOfJanuary2020 = LocalDate.of(2020, 1, 1)
        val epiphany2020 = LocalDate.of(2020, 1, 6)
        db.transaction {
            it.execute("INSERT INTO holiday (date) VALUES (?)", epiphany2020)
            it.execute("UPDATE daycare SET operation_days = ? WHERE id = ?", arrayOf(1, 2, 3, 4, 5, 6, 7), daycareId)
        }
        val result = db.read {
            absenceService.getAbsencesByMonth(it, groupId, firstOfJanuary2020.year, firstOfJanuary2020.monthValue)
        }

        assertTrue(result.operationDays.contains(epiphany2020))
    }

    @Test
    fun `reservation sums - basic case`() {
        insertGroupPlacement(childId)
        val reservations = generateSequence(placementStart) { it.plusDays(1) }
            .map { HelsinkiDateTime.of(it, LocalTime.of(8, 0)) to HelsinkiDateTime.of(it, LocalTime.of(16, 0)) }
            .take(5).toList()
        insertReservations(childId, reservations)

        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, 2019, 8) }
        assertEquals(listOf(40), result.children.map { it.reservationTotalHours })
    }

    @Test
    fun `reservation sums - placement changes mid reservation`() {
        insertGroupPlacement(childId, placementPeriod = FiniteDateRange(placementStart, placementStart.plusDays(1)))
        insertGroupPlacement(childId, placementPeriod = FiniteDateRange(placementStart.plusDays(2), placementEnd))
        val reservations = generateSequence(placementStart) { it.plusDays(1) }
            .map { HelsinkiDateTime.of(it, LocalTime.of(21, 0)) to HelsinkiDateTime.of(it.plusDays(1), LocalTime.of(9, 0)) }
            .take(5).toList()
        insertReservations(childId, reservations)

        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, 2019, 8) }
        assertEquals(listOf(60), result.children.map { it.reservationTotalHours })
    }

    @Test
    fun `reservation sums - reservations continuing over the end of month are cut at midnight`() {
        insertGroupPlacement(childId)
        val lastDayOfMonth = LocalDate.of(2019, 8, 31)
        insertReservations(
            childId,
            listOf(
                HelsinkiDateTime.of(lastDayOfMonth, LocalTime.of(12, 0))
                    to HelsinkiDateTime.of(lastDayOfMonth.plusDays(1), LocalTime.of(12, 0))
            )
        )

        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, 2019, 8) }
        assertEquals(listOf(12), result.children.map { it.reservationTotalHours })
    }

    @Test
    fun `reservation sums - reservations continuing from last month are included from midnight`() {
        insertGroupPlacement(childId)
        val firstDayOfMonth = LocalDate.of(2019, 8, 1)
        insertReservations(
            childId,
            listOf(
                HelsinkiDateTime.of(firstDayOfMonth.minusDays(1), LocalTime.of(12, 0))
                    to HelsinkiDateTime.of(firstDayOfMonth, LocalTime.of(12, 0))
            )
        )

        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, 2019, 8) }
        assertEquals(listOf(12), result.children.map { it.reservationTotalHours })
    }

    @Test
    fun `reservation sums - reservations longer than placements are cut at placement start`() {
        val placementDate = LocalDate.of(2019, 8, 15)
        insertGroupPlacement(childId, placementPeriod = FiniteDateRange(placementDate, placementDate))
        insertReservations(
            childId,
            listOf(
                HelsinkiDateTime.of(placementDate.minusDays(1), LocalTime.of(12, 0))
                    to HelsinkiDateTime.of(placementDate, LocalTime.of(12, 0))
            )
        )

        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, 2019, 8) }
        assertEquals(listOf(12), result.children.map { it.reservationTotalHours })
    }

    @Test
    fun `reservation sums - reservations longer than placements are cut at placement end`() {
        val placementDate = LocalDate.of(2019, 8, 15)
        insertGroupPlacement(childId, placementPeriod = FiniteDateRange(placementDate, placementDate))
        insertReservations(
            childId,
            listOf(
                HelsinkiDateTime.of(placementDate, LocalTime.of(12, 0))
                    to HelsinkiDateTime.of(placementDate.plusDays(1), LocalTime.of(12, 0))
            )
        )

        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, 2019, 8) }
        assertEquals(listOf(12), result.children.map { it.reservationTotalHours })
    }

    @Test
    fun `reservation sums - reservations during back up placements are included in placement unit sum`() {
        insertGroupPlacement(childId)
        val reservations = generateSequence(placementStart) { it.plusDays(1) }
            .map { HelsinkiDateTime.of(it, LocalTime.of(8, 0)) to HelsinkiDateTime.of(it, LocalTime.of(16, 0)) }
            .take(5).toList()
        insertReservations(childId, reservations)
        val (backupUnit, backupGroup) = createNewUnitAndGroup()
        val backupPeriod = FiniteDateRange(placementStart.plusDays(1), placementStart.plusDays(2))
        insertBackupPlacement(childId, backupUnit, backupGroup, backupPeriod)

        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, 2019, 8) }
        assertEquals(listOf(40), result.children.map { it.reservationTotalHours })
    }

    @Test
    fun `reservation sums - only reservations during back up placements are included in backup unit sum`() {
        insertGroupPlacement(childId)
        val reservations = generateSequence(placementStart) { it.plusDays(1) }
            .map { HelsinkiDateTime.of(it, LocalTime.of(8, 0)) to HelsinkiDateTime.of(it, LocalTime.of(16, 0)) }
            .take(5).toList()
        insertReservations(childId, reservations)
        val (backupUnit, backupGroup) = createNewUnitAndGroup()
        val backupPeriod = FiniteDateRange(placementStart.plusDays(1), placementStart.plusDays(2))
        insertBackupPlacement(childId, backupUnit, backupGroup, backupPeriod)

        val result = db.read { absenceService.getAbsencesByMonth(it, backupGroup, 2019, 8) }
        assertEquals(listOf(16), result.children.map { it.reservationTotalHours })
    }

    @Test
    fun `attendance sums - basic case`() {
        insertGroupPlacement(childId)
        val attendances = generateSequence(placementStart) { it.plusDays(1) }
            .map { HelsinkiDateTime.of(it, LocalTime.of(8, 0)) to HelsinkiDateTime.of(it, LocalTime.of(16, 0)) }
            .take(5).toList()
        insertAttendances(childId, daycareId, attendances)

        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, 2019, 8) }
        assertEquals(listOf(40), result.children.map { it.attendanceTotalHours })
    }

    @Test
    fun `attendance sums - placement changes mid attendance`() {
        insertGroupPlacement(childId, placementPeriod = FiniteDateRange(placementStart, placementStart.plusDays(1)))
        insertGroupPlacement(childId, placementPeriod = FiniteDateRange(placementStart.plusDays(2), placementEnd))
        val attendances = generateSequence(placementStart) { it.plusDays(1) }
            .map { HelsinkiDateTime.of(it, LocalTime.of(21, 0)) to HelsinkiDateTime.of(it.plusDays(1), LocalTime.of(9, 0)) }
            .take(5).toList()
        insertAttendances(childId, daycareId, attendances)

        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, 2019, 8) }
        assertEquals(listOf(60), result.children.map { it.attendanceTotalHours })
    }

    @Test
    fun `attendance sums - attendances continuing over the end of month are cut at midnight`() {
        insertGroupPlacement(childId)
        val lastDayOfMonth = LocalDate.of(2019, 8, 31)
        insertAttendances(
            childId,
            daycareId,
            listOf(
                HelsinkiDateTime.of(lastDayOfMonth, LocalTime.of(12, 0))
                    to HelsinkiDateTime.of(lastDayOfMonth.plusDays(1), LocalTime.of(12, 0))
            )
        )

        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, 2019, 8) }
        assertEquals(listOf(12), result.children.map { it.attendanceTotalHours })
    }

    @Test
    fun `attendance sums - attendances continuing from last month are included from midnight`() {
        insertGroupPlacement(childId)
        val firstDayOfMonth = LocalDate.of(2019, 8, 1)
        insertAttendances(
            childId,
            daycareId,
            listOf(
                HelsinkiDateTime.of(firstDayOfMonth.minusDays(1), LocalTime.of(12, 0))
                    to HelsinkiDateTime.of(firstDayOfMonth, LocalTime.of(12, 0))
            )
        )

        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, 2019, 8) }
        assertEquals(listOf(12), result.children.map { it.attendanceTotalHours })
    }

    @Test
    fun `attendance sums - attendances longer than placements are cut at placement start and end`() {
        val placementDate = LocalDate.of(2019, 8, 15)
        insertGroupPlacement(childId, placementPeriod = FiniteDateRange(placementDate, placementDate))
        insertAttendances(
            childId,
            daycareId,
            listOf(
                HelsinkiDateTime.of(placementDate.minusDays(5), LocalTime.of(12, 0))
                    to HelsinkiDateTime.of(placementDate.plusDays(5), LocalTime.of(12, 0))
            )
        )

        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, 2019, 8) }
        assertEquals(listOf(24), result.children.map { it.attendanceTotalHours })
    }

    @Test
    fun `attendance sums - attendances during back up placements are included in placement unit sum`() {
        insertGroupPlacement(childId)
        val attendances = generateSequence(placementStart) { it.plusDays(1) }
            .map { HelsinkiDateTime.of(it, LocalTime.of(8, 0)) to HelsinkiDateTime.of(it, LocalTime.of(16, 0)) }
            .take(5).toList()
        val (backupUnit, backupGroup) = createNewUnitAndGroup()
        val backupPeriod = FiniteDateRange(placementStart.plusDays(1), placementStart.plusDays(2))
        insertBackupPlacement(childId, backupUnit, backupGroup, backupPeriod)
        insertAttendances(childId, backupUnit, attendances)

        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, 2019, 8) }
        assertEquals(listOf(40), result.children.map { it.attendanceTotalHours })
    }

    @Test
    fun `attendance sums - only attendances during back up placements are included in backup unit sum`() {
        insertGroupPlacement(childId)
        val attendances = generateSequence(placementStart) { it.plusDays(1) }
            .map { HelsinkiDateTime.of(it, LocalTime.of(8, 0)) to HelsinkiDateTime.of(it, LocalTime.of(16, 0)) }
            .take(5).toList()
        val (backupUnit, backupGroup) = createNewUnitAndGroup()
        val backupPeriod = FiniteDateRange(placementStart.plusDays(1), placementStart.plusDays(2))
        insertBackupPlacement(childId, backupUnit, backupGroup, backupPeriod)
        insertAttendances(childId, backupUnit, attendances)

        val result = db.read { absenceService.getAbsencesByMonth(it, backupGroup, 2019, 8) }
        assertEquals(listOf(16), result.children.map { it.attendanceTotalHours })
    }

    @Test
    fun `attendance sums - attendances without a departure time are not included`() {
        insertGroupPlacement(childId)
        val attendances = listOf(HelsinkiDateTime.of(placementStart, LocalTime.of(9, 0)) to null)
        insertAttendances(childId, daycareId, attendances)

        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, 2019, 8) }
        assertEquals(listOf(null), result.children.map { it.attendanceTotalHours })
    }

    private fun insertChildAndGroupPlacement(
        childId: ChildId,
        placementType: PlacementType = PlacementType.DAYCARE,
        placementPeriod: FiniteDateRange = FiniteDateRange(placementStart, placementEnd)
    ) {
        db.transaction {
            it.insertTestPerson(DevPerson(childId))
            it.insertTestChild(DevChild(childId))
        }
        insertGroupPlacement(childId, placementType, placementPeriod)
    }

    private fun insertGroupPlacement(
        childId: ChildId,
        placementType: PlacementType = PlacementType.DAYCARE,
        placementPeriod: FiniteDateRange = FiniteDateRange(placementStart, placementEnd)
    ) {
        db.transaction {
            val daycarePlacementId = it.insertTestPlacement(
                DevPlacement(
                    childId = childId,
                    unitId = daycareId,
                    type = placementType,
                    startDate = placementPeriod.start,
                    endDate = placementPeriod.end
                )
            )
            it.insertTestDaycareGroupPlacement(
                daycarePlacementId = daycarePlacementId, groupId = groupId,
                startDate = placementPeriod.start, endDate = placementPeriod.end
            )
        }
    }

    private fun createNewUnitAndGroup(): Pair<DaycareId, GroupId> {
        return db.transaction {
            val unitId = it.insertTestDaycare(DevDaycare(areaId = testArea.id))
            unitId to it.insertTestDaycareGroup(DevDaycareGroup(daycareId = unitId))
        }
    }

    private fun insertBackupPlacement(
        childId: ChildId,
        unitId: DaycareId,
        groupId: GroupId,
        placementPeriod: FiniteDateRange = FiniteDateRange(placementStart, placementEnd)
    ) {
        db.transaction {
            it.insertTestBackupCare(
                DevBackupCare(childId = childId, unitId = unitId, groupId = groupId, period = placementPeriod)
            )
        }
    }

    private fun createAbsence(childId: ChildId, careType: AbsenceCareType, absenceType: AbsenceType, date: LocalDate): AbsenceUpsert {
        return AbsenceUpsert(
            childId = childId,
            date = date,
            careType = careType,
            absenceType = absenceType
        )
    }

    private fun insertReservations(childId: ChildId, reservations: List<Pair<HelsinkiDateTime, HelsinkiDateTime>>) {
        db.transaction { tx ->
            reservations.forEach {
                tx.insertTestReservation(
                    DevReservation(
                        childId = childId,
                        startTime = it.first,
                        endTime = it.second,
                        createdBy = EvakaUserId(testUserId.raw)
                    )
                )
            }
        }
    }

    private fun insertAttendances(
        childId: ChildId,
        unitId: DaycareId,
        attendances: List<Pair<HelsinkiDateTime, HelsinkiDateTime?>>
    ) {
        db.transaction { tx ->
            attendances.forEach {
                tx.insertTestChildAttendance(
                    childId = childId,
                    unitId = unitId,
                    arrived = it.first,
                    departed = it.second
                )
            }
        }
    }
}
