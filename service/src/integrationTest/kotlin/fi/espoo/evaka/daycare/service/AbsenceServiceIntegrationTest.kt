// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.service

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insertTestBackupCare
import fi.espoo.evaka.shared.dev.insertTestChild
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.testDaycare
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class AbsenceServiceIntegrationTest : FullApplicationTest() {
    @Autowired
    lateinit var absenceService: AbsenceService

    val childId = UUID.randomUUID()
    val testUserId = UUID.randomUUID()
    val daycareId = testDaycare.id
    val daycareName = testDaycare.name
    val groupId = UUID.randomUUID()
    val groupName = "Testiryhmä"
    val placementStart = LocalDate.of(2019, 8, 1)
    val placementEnd = LocalDate.of(2019, 12, 31)

    @BeforeEach
    private fun prepare() {
        db.transaction {
            it.insertGeneralTestFixtures()
            it.insertTestPerson(DevPerson(id = testUserId))
            it.insertTestEmployee(DevEmployee(id = testUserId))
        }
        insertDaycareGroup()
    }

    @AfterEach
    private fun afterEach() {
        db.transaction { tx -> tx.resetDatabase() }
    }

    @Test
    fun `get group with placement`() {
        insertGroupPlacement(childId, LocalDate.of(2013, 1, 1), PlacementType.PRESCHOOL_DAYCARE)

        val placementDate = placementStart
        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, placementDate.year, placementDate.monthValue) }

        assertEquals(groupId, result.groupId)
        assertEquals(daycareName, result.daycareName)
        assertEquals(groupName, result.groupName)
        assertEquals(1, result.children.size)
    }

    @Test
    fun `get group with multiple placements`() {
        insertGroupPlacement(childId, LocalDate.of(2013, 1, 1), PlacementType.PRESCHOOL_DAYCARE)

        val placementDate = placementStart
        val groupSize = 50
        (1 until groupSize).forEach { _ ->
            insertGroupPlacement(UUID.randomUUID(), LocalDate.of(2013, 1, 1), PlacementType.PRESCHOOL_DAYCARE)
        }
        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, placementDate.year, placementDate.monthValue) }

        assertEquals(groupSize, result.children.size)
    }

    @Test
    fun `get group without placements`() {
        insertGroupPlacement(childId, LocalDate.of(2013, 1, 1), PlacementType.PRESCHOOL_DAYCARE)

        val futureDate = placementEnd.plusMonths(1)
        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, futureDate.year, futureDate.monthValue) }

        assertEquals(groupId, result.groupId)
        assertEquals(groupName, result.groupName)
        assertEquals(0, result.children.size)
    }

    @Test
    fun `preschool placement with connected daycare maps correctly`() {
        insertGroupPlacement(childId, LocalDate.of(2013, 1, 1), PlacementType.PRESCHOOL_DAYCARE)

        val placementDate = placementStart
        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, placementDate.year, placementDate.monthValue) }
        val daysInMonth = placementDate.month.length(false)
        val placements = result.children[0].placements
        val careTypes = placements.getValue(placementDate)

        assertEquals(daysInMonth, placements.size)
        assertEquals(2, careTypes.size)
        assertTrue(careTypes.contains(CareType.PRESCHOOL))
        assertTrue(careTypes.contains(CareType.PRESCHOOL_DAYCARE))
    }

    @Test
    fun `preschool placement without connected daycare maps correctly`() {
        insertGroupPlacement(childId, LocalDate.of(2013, 1, 1), PlacementType.PRESCHOOL)

        val placementDate = placementStart
        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, placementDate.year, placementDate.monthValue) }
        val daysInMonth = placementDate.month.length(false)
        val placements = result.children[0].placements
        val careTypes = placements.getValue(placementDate)

        assertEquals(daysInMonth, placements.size)
        assertEquals(1, careTypes.size)
        assertEquals(true, careTypes.contains(CareType.PRESCHOOL))
    }

    @Test
    fun `daycare placement maps correctly`() {
        insertGroupPlacement(childId, LocalDate.of(2013, 1, 1), PlacementType.DAYCARE)

        val placementDate = placementStart
        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, placementDate.year, placementDate.monthValue) }
        val daysInMonth = placementDate.month.length(false)
        val placements = result.children[0].placements
        val careTypes = placements.getValue(placementDate)

        assertEquals(daysInMonth, placements.size)
        assertEquals(1, careTypes.size)
        assertTrue(careTypes.contains(CareType.DAYCARE))
    }

    @Test
    fun `daycare placement maps correctly for 5-year-old children`() {
        insertGroupPlacement(childId, LocalDate.of(2014, 1, 1), PlacementType.DAYCARE_FIVE_YEAR_OLDS)

        val placementDate = LocalDate.of(2019, 8, 1)
        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, placementDate.year, placementDate.monthValue) }
        val daysInMonth = placementDate.month.length(false)
        val placements = result.children[0].placements
        val careTypes = placements.getValue(placementDate)

        assertEquals(daysInMonth, placements.size)
        assertEquals(2, careTypes.size)
        assertTrue(careTypes.contains(CareType.DAYCARE))
        assertTrue(careTypes.contains(CareType.DAYCARE_5YO_FREE))
    }

    @Test
    fun `part time daycare placement maps correctly for 5-year-old children`() {
        insertGroupPlacement(childId, LocalDate.of(2014, 1, 1), PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS)

        val placementDate = LocalDate.of(2019, 8, 1)
        val result =
            db.read { absenceService.getAbsencesByMonth(it, groupId, placementDate.year, placementDate.monthValue) }
        val daysInMonth = placementDate.month.length(false)
        val placements = result.children[0].placements
        val careTypes = placements.getValue(placementDate)

        assertEquals(daysInMonth, placements.size)
        assertEquals(2, careTypes.size)
        assertTrue(careTypes.contains(CareType.DAYCARE))
        assertTrue(careTypes.contains(CareType.DAYCARE_5YO_FREE))
    }

    @Test
    fun `get placements and absences for every month`() {
        insertGroupPlacement(childId, LocalDate.of(2013, 1, 1), PlacementType.PRESCHOOL_DAYCARE)

        var placementDate = placementStart
        while (!placementDate.isAfter(placementEnd)) {
            val result = db.read { absenceService.getAbsencesByMonth(it, groupId, placementDate.year, placementDate.monthValue) }
            val daysInMonth = placementDate.month.length(false)
            val placements = result.children[0].placements
            val absences = result.children[0].absences

            assertEquals(daysInMonth, absences.size)
            assertEquals(daysInMonth, placements.size)

            placementDate = placementDate.plusMonths(1)
        }
    }

    @Test
    fun `upsert absences`() {
        insertGroupPlacement(childId, LocalDate.of(2013, 1, 1), PlacementType.PRESCHOOL_DAYCARE)

        val absenceDate = placementEnd
        val initialAbsence = createAbsence(childId, CareType.PRESCHOOL, AbsenceType.SICKLEAVE, absenceDate)
        val initialAbsenceList = listOf(initialAbsence)

        val result = db.transaction { tx ->
            absenceService.upsertAbsences(tx, initialAbsenceList, groupId, testUserId)
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
        insertGroupPlacement(childId, LocalDate.of(2013, 1, 1), PlacementType.PRESCHOOL_DAYCARE)

        val absenceDate = placementEnd
        val result = db.read { absenceService.getAbsencesByMonth(it, groupId, absenceDate.year, absenceDate.monthValue) }
        val absences = result.children[0].absences

        assertEquals(absences.size, placementEnd.month.length(false))
        absences.values.forEach {
            assertEquals(0, it.size)
        }
    }

    @Test
    fun `get one absence`() {
        insertGroupPlacement(childId, LocalDate.of(2013, 1, 1), PlacementType.PRESCHOOL_DAYCARE)

        val absenceDate = placementEnd
        val initialAbsence = createAbsence(childId, CareType.DAYCARE, AbsenceType.SICKLEAVE, absenceDate)
        val initialAbsenceList = listOf(initialAbsence)

        val result = db.transaction { tx ->
            absenceService.upsertAbsences(tx, initialAbsenceList, groupId, testUserId)
            absenceService.getAbsencesByMonth(tx, groupId, absenceDate.year, absenceDate.monthValue)
        }
        val absence = result.children[0].absences.getValue(absenceDate)[0]
        val modifiedBy = db.read { it.getUserNameById(testUserId) }

        assertEquals(initialAbsence.childId, absence.childId)
        assertEquals(absenceDate, absence.date)
        assertEquals(initialAbsence.careType, absence.careType)
        assertEquals(initialAbsence.absenceType, absence.absenceType)
        assertEquals(modifiedBy, absence.modifiedBy)
    }

    @Test
    fun `get multiple absences`() {
        insertGroupPlacement(childId, LocalDate.of(2013, 1, 1), PlacementType.PRESCHOOL_DAYCARE)

        val absenceDate = placementEnd
        val initialAbsence = createAbsence(childId, CareType.DAYCARE, AbsenceType.SICKLEAVE, absenceDate)
        val initialAbsence2 = createAbsence(childId, CareType.PRESCHOOL, AbsenceType.SICKLEAVE, absenceDate)
        val initialAbsenceList = listOf(initialAbsence, initialAbsence2)

        val result = db.transaction { tx ->
            absenceService.upsertAbsences(tx, initialAbsenceList, groupId, testUserId)
            absenceService.getAbsencesByMonth(tx, groupId, absenceDate.year, absenceDate.monthValue)
        }
        val absences = result.children[0].absences.getValue(absenceDate)

        assertEquals(initialAbsenceList.size, absences.size)
    }

    @Test
    fun `children are sorted by last name`() {
        val children = listOf(
            ChildSeed("Mika", "Buro"),
            ChildSeed("Sari", "Aarnio"),
            ChildSeed("Ilmari", "DeWitt"),
            ChildSeed("Antero", "Cecill")
        )
        val absenceDate = placementEnd
        insertGroupPlacements(children, PlacementType.PRESCHOOL_DAYCARE)

        val initialAbsenceList = children.map {
            createAbsence(it.id, CareType.DAYCARE, AbsenceType.SICKLEAVE, absenceDate)
        }

        assertEquals(4, initialAbsenceList.size)

        val result = db.transaction { tx ->
            absenceService.upsertAbsences(tx, initialAbsenceList, groupId, testUserId)
            absenceService.getAbsencesByMonth(tx, groupId, absenceDate.year, absenceDate.monthValue)
        }

        assertEquals(
            listOf(
                "Sari Aarnio",
                "Mika Buro",
                "Antero Cecill",
                "Ilmari DeWitt"
            ),
            result.children.map { "${it.firstName} ${it.lastName}" }
        )
    }

    @Test
    fun `cannot create multiple absences for same placement type and same date`() {
        insertGroupPlacement(childId, LocalDate.of(2013, 1, 1), PlacementType.PRESCHOOL_DAYCARE)

        val absenceDate = placementEnd
        val initialAbsence = createAbsence(childId, CareType.DAYCARE, AbsenceType.SICKLEAVE, absenceDate)
        val initialAbsence2 = createAbsence(childId, CareType.DAYCARE, AbsenceType.SICKLEAVE, absenceDate)
        val initialAbsenceList = listOf(initialAbsence, initialAbsence2)

        val result = db.transaction { tx ->
            absenceService.upsertAbsences(tx, initialAbsenceList, groupId, testUserId)
            absenceService.getAbsencesByMonth(tx, groupId, absenceDate.year, absenceDate.monthValue)
        }
        val absences = result.children[0].absences.getValue(absenceDate)

        assertEquals(1, absences.size)
    }

    @Test
    fun `modify absence`() {
        insertGroupPlacement(childId, LocalDate.of(2013, 1, 1), PlacementType.PRESCHOOL_DAYCARE)

        val absenceDate = placementEnd
        val initialAbsence = createAbsence(childId, CareType.DAYCARE, AbsenceType.SICKLEAVE, absenceDate)
        val initialAbsenceList = listOf(initialAbsence)

        var result = db.transaction { tx ->
            absenceService.upsertAbsences(tx, initialAbsenceList, groupId, testUserId)
            absenceService.getAbsencesByMonth(tx, groupId, absenceDate.year, absenceDate.monthValue)
        }
        val absence = result.children[0].absences.getValue(absenceDate)[0]

        val newAbsenceType = AbsenceType.UNKNOWN_ABSENCE
        val updatedAbsence = absence.copy(absenceType = newAbsenceType)

        result = db.transaction { tx ->
            absenceService.upsertAbsences(tx, listOf(updatedAbsence), groupId, testUserId)
            absenceService.getAbsencesByMonth(tx, groupId, absenceDate.year, absenceDate.monthValue)
        }
        val absences = result.children[0].absences.getValue(absenceDate)
        val modifiedAbsence = result.children[0].absences.getValue(absenceDate)[0]

        assertEquals(1, absences.size)
        assertEquals(newAbsenceType, modifiedAbsence.absenceType)
    }

    @Test
    fun `get absence by childId`() {
        insertGroupPlacement(childId, LocalDate.of(2013, 1, 1), PlacementType.PRESCHOOL_DAYCARE)

        val absenceDate = placementEnd
        val initialAbsence = createAbsence(childId, CareType.DAYCARE, AbsenceType.SICKLEAVE, absenceDate)
        val initialAbsence2 = createAbsence(childId, CareType.PRESCHOOL, AbsenceType.SICKLEAVE, absenceDate)
        val initialAbsenceList = listOf(initialAbsence, initialAbsence2)

        val result = db.transaction { tx ->
            absenceService.upsertAbsences(tx, initialAbsenceList, groupId, testUserId)
            absenceService.getAbscencesByChild(tx, childId, absenceDate.year, absenceDate.monthValue)
        }

        val absences = result.absences.getValue(absenceDate)

        assertEquals(initialAbsenceList.size, absences.size)
    }

    @Test
    fun `get absence by childId should not find anything with a wrong childId`() {
        val childId2 = UUID.randomUUID()
        insertGroupPlacement(childId, LocalDate.of(2013, 1, 1), PlacementType.PRESCHOOL_DAYCARE)
        db.transaction {
            it.insertTestPerson(DevPerson(id = childId2, dateOfBirth = LocalDate.of(2013, 1, 1)))
            it.insertTestChild(DevChild(childId2))
        }

        val absenceDate = placementEnd
        val initialAbsence = createAbsence(childId, CareType.DAYCARE, AbsenceType.SICKLEAVE, absenceDate)
        val initialAbsence2 = createAbsence(childId, CareType.PRESCHOOL, AbsenceType.SICKLEAVE, absenceDate)
        val initialAbsenceList = listOf(initialAbsence, initialAbsence2)

        val result = db.transaction { tx ->
            absenceService.upsertAbsences(tx, initialAbsenceList, groupId, testUserId)
            absenceService.getAbscencesByChild(tx, childId2, absenceDate.year, absenceDate.monthValue)
        }
        val absences = result.absences.getValue(absenceDate)

        assertEquals(0, absences.size)
    }

    @Test
    fun `get absence by childId find the absences within the date range`() {
        insertGroupPlacement(childId, LocalDate.of(2013, 1, 1), PlacementType.PRESCHOOL_DAYCARE)

        val absenceDate = placementEnd
        val initialAbsence = createAbsence(childId, CareType.DAYCARE, AbsenceType.SICKLEAVE, LocalDate.of(2019, 9, 1))
        val initialAbsence2 = createAbsence(childId, CareType.PRESCHOOL, AbsenceType.SICKLEAVE, absenceDate)
        val initialAbsenceList = listOf(initialAbsence, initialAbsence2)

        val result = db.transaction { tx ->
            absenceService.upsertAbsences(tx, initialAbsenceList, groupId, testUserId)
            absenceService.getAbscencesByChild(tx, childId, 2019, 9)
        }
        val absences = result.absences.getValue(LocalDate.of(2019, 9, 1))

        assertEquals(1, absences.size)
    }

    @Test
    fun `backup care children are returned with correct care types`() {
        db.transaction {
            it.insertTestPerson(DevPerson(id = childId, dateOfBirth = LocalDate.of(2013, 1, 1)))
            it.insertTestChild(DevChild(childId))
        }
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
            setOf(CareType.PRESCHOOL, CareType.PRESCHOOL_DAYCARE),
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

    private fun insertDaycareGroup() {
        db.transaction {
            it.insertTestDaycareGroup(
                DevDaycareGroup(daycareId = daycareId, id = groupId, name = groupName, startDate = placementStart)
            )
        }
    }

    private fun insertGroupPlacement(
        childId: UUID,
        dob: LocalDate,
        placementType: PlacementType,
        placementPeriod: FiniteDateRange = FiniteDateRange(placementStart, placementEnd)
    ) {
        db.transaction {
            it.insertTestPerson(DevPerson(id = childId, dateOfBirth = dob))
            it.insertTestChild(DevChild(childId))
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

    data class ChildSeed(
        val firstName: String,
        val lastName: String,
        val id: UUID = UUID.randomUUID(),
        val dob: LocalDate = LocalDate.of(2019, 1, 1)
    )

    private fun insertGroupPlacements(children: List<ChildSeed>, placementType: PlacementType) {
        children.forEach {
            db.transaction { tx ->
                tx.insertTestPerson(
                    DevPerson(
                        id = it.id,
                        firstName = it.firstName,
                        lastName = it.lastName,
                        dateOfBirth = it.dob
                    )
                )
                tx.insertTestChild(DevChild(it.id))
                val daycarePlacementId = tx.insertTestPlacement(
                    DevPlacement(
                        childId = it.id,
                        unitId = daycareId,
                        type = placementType,
                        startDate = placementStart,
                        endDate = placementEnd
                    )
                )
                tx.insertTestDaycareGroupPlacement(
                    daycarePlacementId = daycarePlacementId, groupId = groupId,
                    startDate = placementStart, endDate = placementEnd
                )
            }
        }
    }

    private fun createAbsence(childId: UUID, careType: CareType, absenceType: AbsenceType, date: LocalDate): Absence {
        return Absence(
            id = null,
            childId = childId,
            date = date,
            careType = careType,
            absenceType = absenceType,
            modifiedAt = null,
            modifiedBy = null
        )
    }
}
