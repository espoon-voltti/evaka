// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.service

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimes
import fi.espoo.evaka.dailyservicetimes.TimeRange
import fi.espoo.evaka.dailyservicetimes.upsertChildDailyServiceTimes
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
import java.time.DayOfWeek
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
        val absenceCategories = placements.getValue(placementDate)

        assertEquals(daysInMonth, placements.size)
        assertEquals(setOf(AbsenceCategory.BILLABLE, AbsenceCategory.NONBILLABLE), absenceCategories)
    }

    @Test
    fun `preschool placement without connected daycare maps correctly`() {
        insertGroupPlacement(childId, PlacementType.PRESCHOOL)

        val placementDate = placementStart
        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, placementDate.year, placementDate.monthValue) }
        val daysInMonth = placementDate.month.length(false)
        val placements = result.children[0].placements
        val absenceCategories = placements.getValue(placementDate)

        assertEquals(daysInMonth, placements.size)
        assertEquals(setOf(AbsenceCategory.NONBILLABLE), absenceCategories)
    }

    @Test
    fun `daycare placement maps correctly`() {
        insertGroupPlacement(childId, PlacementType.DAYCARE)

        val placementDate = placementStart
        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, placementDate.year, placementDate.monthValue) }
        val daysInMonth = placementDate.month.length(false)
        val placements = result.children[0].placements
        val absenceCategories = placements.getValue(placementDate)

        assertEquals(daysInMonth, placements.size)
        assertEquals(setOf(AbsenceCategory.BILLABLE), absenceCategories)
    }

    @Test
    fun `daycare placement maps correctly for 5-year-old children`() {
        insertGroupPlacement(childId, PlacementType.DAYCARE_FIVE_YEAR_OLDS)

        val placementDate = LocalDate.of(2019, 8, 1)
        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, placementDate.year, placementDate.monthValue) }
        val daysInMonth = placementDate.month.length(false)
        val placements = result.children[0].placements
        val absenceCategories = placements.getValue(placementDate)

        assertEquals(daysInMonth, placements.size)
        assertEquals(setOf(AbsenceCategory.BILLABLE, AbsenceCategory.NONBILLABLE), absenceCategories)
    }

    @Test
    fun `part time daycare placement maps correctly for 5-year-old children`() {
        insertGroupPlacement(childId, PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS)

        val placementDate = LocalDate.of(2019, 8, 1)
        val result =
            db.read { absenceService.getAbsencesByMonth(it, groupId, placementDate.year, placementDate.monthValue) }
        val daysInMonth = placementDate.month.length(false)
        val placements = result.children[0].placements
        val absenceCategories = placements.getValue(placementDate)

        assertEquals(daysInMonth, placements.size)
        assertEquals(setOf(AbsenceCategory.BILLABLE, AbsenceCategory.NONBILLABLE), absenceCategories)
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
        val initialAbsence = createAbsence(childId, AbsenceCategory.NONBILLABLE, AbsenceType.SICKLEAVE, absenceDate)
        val initialAbsenceList = listOf(initialAbsence)

        val result = db.transaction { tx ->
            tx.upsertAbsences(initialAbsenceList, EvakaUserId(testUserId.raw))
            absenceService.getAbsencesByMonth(tx, groupId, absenceDate.year, absenceDate.monthValue)
        }
        val absence = result.children[0].absences.getValue(absenceDate)[0]

        assertEquals(initialAbsence.childId, absence.childId)
        assertEquals(initialAbsence.category, absence.category)
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
        val initialAbsence = createAbsence(childId, AbsenceCategory.BILLABLE, AbsenceType.SICKLEAVE, absenceDate)
        val initialAbsenceList = listOf(initialAbsence)

        val result = db.transaction { tx ->
            tx.upsertAbsences(initialAbsenceList, EvakaUserId(testUserId.raw))
            absenceService.getAbsencesByMonth(tx, groupId, absenceDate.year, absenceDate.monthValue)
        }
        val absence = result.children[0].absences.getValue(absenceDate)[0]

        assertEquals(initialAbsence.childId, absence.childId)
        assertEquals(absenceDate, absence.date)
        assertEquals(initialAbsence.category, absence.category)
        assertEquals(initialAbsence.absenceType, absence.absenceType)
    }

    @Test
    fun `get multiple absences`() {
        insertGroupPlacement(childId, PlacementType.PRESCHOOL_DAYCARE)

        val absenceDate = placementEnd
        val initialAbsence = createAbsence(childId, AbsenceCategory.BILLABLE, AbsenceType.SICKLEAVE, absenceDate)
        val initialAbsence2 = createAbsence(childId, AbsenceCategory.NONBILLABLE, AbsenceType.SICKLEAVE, absenceDate)
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
        val initialAbsence = createAbsence(childId, AbsenceCategory.BILLABLE, AbsenceType.SICKLEAVE, absenceDate)
        val initialAbsence2 = createAbsence(childId, AbsenceCategory.BILLABLE, AbsenceType.SICKLEAVE, absenceDate)
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
        val initialAbsence = createAbsence(childId, AbsenceCategory.BILLABLE, AbsenceType.SICKLEAVE, absenceDate)
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
            category = absence.category,
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
        val initialAbsence = createAbsence(childId, AbsenceCategory.BILLABLE, AbsenceType.SICKLEAVE, absenceDate)
        val initialAbsence2 = createAbsence(childId, AbsenceCategory.NONBILLABLE, AbsenceType.SICKLEAVE, absenceDate)
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
        val initialAbsence = createAbsence(childId, AbsenceCategory.BILLABLE, AbsenceType.SICKLEAVE, absenceDate)
        val initialAbsence2 = createAbsence(childId, AbsenceCategory.NONBILLABLE, AbsenceType.SICKLEAVE, absenceDate)
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
        val initialAbsence = createAbsence(childId, AbsenceCategory.BILLABLE, AbsenceType.SICKLEAVE, LocalDate.of(2019, 9, 1))
        val initialAbsence2 = createAbsence(childId, AbsenceCategory.NONBILLABLE, AbsenceType.SICKLEAVE, absenceDate)
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
            setOf(AbsenceCategory.BILLABLE, AbsenceCategory.NONBILLABLE),
            result.children.first().placements[placementDate]
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
    fun `reservation sums - daily service times are used to generate missing reservations when none are found`() {
        insertGroupPlacement(childId)
        val dailyServiceTimes = DailyServiceTimes.RegularTimes(TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0)))
        insertDailyServiceTimes(childId, dailyServiceTimes)

        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, 2019, 8) }
        // 22 operational days * 8h
        assertEquals(listOf(176), result.children.map { it.reservationTotalHours })
    }

    @Test
    fun `reservation sums - irregular daily service times are applied according to operational days`() {
        insertGroupPlacement(
            childId,
            placementPeriod = FiniteDateRange(LocalDate.of(2019, 8, 5), LocalDate.of(2019, 8, 11))
        )
        val dailyServiceTimes = DailyServiceTimes.IrregularTimes(
            monday = TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0)),
            tuesday = TimeRange(LocalTime.of(8, 0), LocalTime.of(14, 0)),
            wednesday = null,
            thursday = null,
            friday = null,
            saturday = TimeRange(LocalTime.of(8, 0), LocalTime.of(20, 0)),
            sunday = null
        )
        insertDailyServiceTimes(childId, dailyServiceTimes)

        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, 2019, 8) }
        // 8-16 + 8-14 (saturday is not included because the unit is not operational on saturdays)
        assertEquals(listOf(14), result.children.map { it.reservationTotalHours })
    }

    @Test
    fun `reservation sums - daily service times with inverted start and end`() {
        insertGroupPlacement(childId)
        val dailyServiceTimes = DailyServiceTimes.RegularTimes(TimeRange(LocalTime.of(21, 0), LocalTime.of(9, 0)))
        insertDailyServiceTimes(childId, dailyServiceTimes)

        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, 2019, 8) }
        // 22 operational days * 12h
        assertEquals(listOf(264), result.children.map { it.reservationTotalHours })
    }

    @Test
    fun `reservation sums - daily service times are used only when there is no reservation`() {
        insertGroupPlacement(childId)
        val reservations = generateSequence(placementStart) { it.plusDays(1) }
            .takeWhile { it < LocalDate.of(2019, 8, 30) } // last operational day
            .filter { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }
            .map { HelsinkiDateTime.of(it, LocalTime.of(8, 0)) to HelsinkiDateTime.of(it, LocalTime.of(16, 0)) }
            .toList()
        insertReservations(childId, reservations)
        val dailyServiceTimes = DailyServiceTimes.RegularTimes(TimeRange(LocalTime.of(8, 0), LocalTime.of(20, 0)))
        insertDailyServiceTimes(childId, dailyServiceTimes)

        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, 2019, 8) }
        // 21 operational days * 8h + 12h
        assertEquals(listOf(180), result.children.map { it.reservationTotalHours })
    }

    @Test
    fun `reservation sums - daily service times are cut when they partially overlap with a reservation`() {
        val placementPeriod = FiniteDateRange(LocalDate.of(2019, 8, 5), LocalDate.of(2019, 8, 6))
        insertGroupPlacement(childId, placementPeriod = placementPeriod)
        val reservations = listOf(
            HelsinkiDateTime.of(placementPeriod.start, LocalTime.of(21, 0))
                to HelsinkiDateTime.of(placementPeriod.end, LocalTime.of(9, 0))
        )
        insertReservations(childId, reservations)
        val dailyServiceTimes = DailyServiceTimes.RegularTimes(TimeRange(LocalTime.of(7, 0), LocalTime.of(15, 0)))
        insertDailyServiceTimes(childId, dailyServiceTimes)

        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, 2019, 8) }
        // 21-9 + 9-15 (overlapping 2 hours are left out)
        assertEquals(listOf(18), result.children.map { it.reservationTotalHours })
    }

    @Test
    fun `reservation sums - absences leave out reserved times according to their start time`() {
        insertGroupPlacement(childId)
        val reservations = generateSequence(placementStart) { it.plusDays(1) }
            .map { HelsinkiDateTime.of(it, LocalTime.of(20, 0)) to HelsinkiDateTime.of(it.plusDays(1), LocalTime.of(8, 0)) }
            .take(5).toList()
        insertReservations(childId, reservations)
        db.transaction {
            // the start and end of absence date overlaps with two reservations
            val absence = createAbsence(
                childId,
                AbsenceCategory.BILLABLE,
                AbsenceType.OTHER_ABSENCE,
                placementStart.plusDays(1)
            )
            it.upsertAbsences(listOf(absence), EvakaUserId(testUserId.raw))
        }

        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, 2019, 8) }
        // the start and end of absence date overlaps with two reservations but only one reservation is left out
        assertEquals(listOf(4 * 12), result.children.map { it.reservationTotalHours })
    }

    @Test
    fun `reservation sums - daily service time is within another reservation`() {
        insertGroupPlacement(childId)
        val reservations = listOf(
            HelsinkiDateTime.of(placementStart, LocalTime.of(20, 0))
                to HelsinkiDateTime.of(placementStart.plusDays(1), LocalTime.of(17, 0))
        )
        insertReservations(childId, reservations)
        val dailyServiceTimes = DailyServiceTimes.RegularTimes(TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0)))
        insertDailyServiceTimes(childId, dailyServiceTimes)

        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, 2019, 8) }
        assertEquals(listOf(21 + 20 * 8), result.children.map { it.reservationTotalHours })
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

    private fun createAbsence(childId: ChildId, category: AbsenceCategory, absenceType: AbsenceType, date: LocalDate): AbsenceUpsert {
        return AbsenceUpsert(
            childId = childId,
            date = date,
            category = category,
            absenceType = absenceType
        )
    }

    private fun insertReservations(childId: ChildId, reservations: List<Pair<HelsinkiDateTime, HelsinkiDateTime>>) {
        db.transaction { tx ->
            reservations
                .flatMap { (start, end) ->
                    if (start.toLocalDate().plusDays(1) == end.toLocalDate()) listOf(
                        start to HelsinkiDateTime.of(start.toLocalDate(), LocalTime.of(23, 59)),
                        HelsinkiDateTime.of(end.toLocalDate(), LocalTime.of(0, 0)) to end,
                    ) else listOf(start to end)
                }
                .forEach { (start, end) ->
                    tx.insertTestReservation(
                        DevReservation(
                            childId = childId,
                            date = start.toLocalDate(),
                            startTime = start.toLocalTime(),
                            endTime = end.toLocalTime(),
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

    private fun insertDailyServiceTimes(childId: ChildId, dailyServiceTimes: DailyServiceTimes) {
        db.transaction { tx -> tx.upsertChildDailyServiceTimes(childId, dailyServiceTimes) }
    }
}
