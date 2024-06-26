// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.backupcare.getBackupCaresForChild
import fi.espoo.evaka.daycare.addUnitFeatures
import fi.espoo.evaka.insertApplication
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.serviceneed.insertServiceNeed
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.snPreschoolDaycare45
import fi.espoo.evaka.snPreschoolDaycarePartDay35
import fi.espoo.evaka.test.getApplicationStatus
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension

@ExtendWith(OutputCaptureExtension::class)
class PlacementControllerCitizenIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var placementControllerCitizen: PlacementControllerCitizen
    @Autowired private lateinit var placementController: PlacementController

    private val child = testChild_1
    private val parent = testAdult_1
    private val authenticatedParent = AuthenticatedUser.Citizen(parent.id, CitizenAuthLevel.STRONG)
    private val admin =
        AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.ADMIN))

    private val daycareId = testDaycare.id
    private val daycare2Id = testDaycare2.id

    private val daycareGroup = DevDaycareGroup(daycareId = daycareId)
    private val daycareGroup2 = DevDaycareGroup(daycareId = daycare2Id)

    private val today = LocalDate.now()

    private val placementStart = today.minusMonths(3)
    private val placementEnd = placementStart.plusMonths(6)

    @BeforeEach
    fun setUp() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insert(testDaycare)
            tx.insert(testDaycare2)
            tx.insert(testAdult_1, DevPersonType.ADULT)
            tx.insert(testChild_1, DevPersonType.CHILD)
            listOf(snPreschoolDaycare45, snPreschoolDaycarePartDay35).forEach { tx.insert(it) }
            tx.insert(daycareGroup)
            tx.insert(daycareGroup2)
            tx.insertGuardian(parent.id, child.id)
        }
    }

    @Test
    fun `child placements are returned`() {
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycareId,
                    startDate = placementStart,
                    endDate = placementEnd
                )
            )
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare2Id,
                    startDate = placementEnd.plusDays(1),
                    endDate = placementEnd.plusMonths(2)
                )
            )
        }

        val childPlacements = getChildPlacements(child.id)
        assertEquals(2, childPlacements.size)
        assertEquals(placementStart, childPlacements[0].startDate)
        assertEquals(placementEnd, childPlacements[0].endDate)
        assertEquals(TerminatablePlacementType.DAYCARE, childPlacements[0].type)
        assertEquals(1, childPlacements[0].placements.size)
        assertEquals(PlacementType.DAYCARE, childPlacements[0].placements[0].type)
        assertEquals(null, childPlacements[0].placements[0].terminationRequestedDate)
        assertEquals(null, childPlacements[0].placements[0].terminatedBy)
        assertTrue(childPlacements[0].terminatable)

        assertFalse(childPlacements[1].terminatable)
        assertEquals(listOf(false), childPlacements[1].placements.map { it.terminatable })
    }

    @Test
    fun `citizen can terminate own child's placement starting from tomorrow`() {
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycareId,
                    startDate = placementStart,
                    endDate = placementEnd
                )
            )
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare2Id,
                    startDate = placementEnd.plusDays(1),
                    endDate = placementEnd.plusMonths(2)
                )
            )
        }

        val placementTerminationDate = today.plusDays(1)

        terminatePlacements(
            child.id,
            PlacementControllerCitizen.PlacementTerminationRequestBody(
                type = TerminatablePlacementType.DAYCARE,
                terminationDate = placementTerminationDate,
                unitId = daycareId,
                terminateDaycareOnly = false
            )
        )

        val childPlacements = getChildPlacements(child.id)
        assertEquals(2, childPlacements.size)
        assertEquals(placementStart, childPlacements[0].startDate)
        assertEquals(placementTerminationDate, childPlacements[0].endDate)
        assertEquals(TerminatablePlacementType.DAYCARE, childPlacements[0].type)
        assertEquals(1, childPlacements[0].placements.size)
        assertEquals(today, childPlacements[0].placements[0].terminationRequestedDate)
        assertEquals(
            "${parent.lastName} ${parent.firstName}",
            childPlacements[0].placements[0].terminatedBy?.name
        )

        assertEquals(false, childPlacements[1].terminatable)
        assertEquals(
            listOf(null),
            childPlacements[1].placements.map { it.terminationRequestedDate }
        )
    }

    @Test
    fun `terminating preschool also terminates upcoming daycare placements`() {
        val placementTerminationDate = today.plusWeeks(1)

        val startPreschool = LocalDate.now().minusWeeks(2)
        val endPreschool = startPreschool.plusMonths(1)
        val startDaycare = endPreschool.plusDays(1)
        val endDaycare = startDaycare.plusMonths(1)
        val startClub = endDaycare.plusDays(1)
        val endClub = startClub.plusMonths(1)
        db.transaction {
            it.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = child.id,
                    unitId = daycareId,
                    // placement in the past unaffected
                    startDate = LocalDate.now().minusYears(3),
                    endDate = LocalDate.now().minusYears(2)
                )
            )
            it.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL,
                    childId = child.id,
                    unitId = daycareId,
                    // placement in the past unaffected
                    startDate = LocalDate.now().minusMonths(12),
                    endDate = LocalDate.now().minusMonths(6)
                )
            )
            it.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL,
                    childId = child.id,
                    unitId = daycareId,
                    startDate = startPreschool,
                    endDate = endPreschool
                )
            )
            it.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = child.id,
                    unitId = daycareId,
                    startDate = startDaycare,
                    endDate = endDaycare
                )
            )
            it.insert(
                DevPlacement(
                    type = PlacementType.CLUB,
                    childId = child.id,
                    unitId = daycareId,
                    startDate = startClub,
                    endDate = endClub
                )
            )
        }

        terminatePlacements(
            child.id,
            PlacementControllerCitizen.PlacementTerminationRequestBody(
                type = TerminatablePlacementType.PRESCHOOL,
                terminationDate = placementTerminationDate,
                unitId = daycareId,
                terminateDaycareOnly = false
            )
        )

        val childPlacements = getChildPlacements(child.id)

        assertEquals(2, childPlacements.size)
        val preschoolPlacement = childPlacements[0]
        assertEquals(TerminatablePlacementType.PRESCHOOL, preschoolPlacement.type)
        assertEquals(startPreschool, preschoolPlacement.startDate)
        assertEquals(placementTerminationDate, preschoolPlacement.endDate)
        assertEquals(1, preschoolPlacement.placements.size)
        assertEquals(today, preschoolPlacement.placements[0].terminationRequestedDate)
        assertEquals(
            "${parent.lastName} ${parent.firstName}",
            preschoolPlacement.placements[0].terminatedBy?.name
        )
        assertEquals(PlacementType.PRESCHOOL, preschoolPlacement.placements[0].type)

        // club placement is unaffected
        val clubPlacement = childPlacements[1]
        assertEquals(startClub, clubPlacement.startDate)
        assertEquals(endClub, clubPlacement.endDate)
        assertEquals(TerminatablePlacementType.CLUB, clubPlacement.type)
        assertEquals(1, clubPlacement.placements.size)
        assertNull(clubPlacement.placements[0].terminationRequestedDate)
        assertEquals(PlacementType.CLUB, clubPlacement.placements[0].type)
    }

    @Test
    fun `terminating PRESCHOOL_DAYCARE with daycare only changes the remainder of the preschool to PRESCHOOL`() {
        val placementTerminationDate = today.plusWeeks(1)

        val startPreschool = today.minusWeeks(2)
        val endPreschool = startPreschool.plusMonths(1)
        db.transaction {
            val id =
                it.insert(
                    DevPlacement(
                        type = PlacementType.PRESCHOOL_DAYCARE,
                        childId = child.id,
                        unitId = daycareId,
                        startDate = startPreschool,
                        endDate = endPreschool
                    )
                )
            it.insertServiceNeed(
                id,
                startPreschool,
                endPreschool.minusDays(10),
                snPreschoolDaycare45.id,
                ShiftCareType.NONE,
                false,
                null,
                null
            )
            it.insertServiceNeed(
                id,
                endPreschool.minusDays(9),
                endPreschool,
                snPreschoolDaycarePartDay35.id,
                ShiftCareType.NONE,
                false,
                null,
                null
            )
        }

        val placementsBefore = getChildPlacements(child.id)
        assertEquals(1, placementsBefore.size)
        val placementBefore = placementsBefore[0]
        assertEquals(TerminatablePlacementType.PRESCHOOL, placementBefore.type)
        assertEquals(startPreschool, placementBefore.startDate)
        assertEquals(endPreschool, placementBefore.endDate)

        assertEquals(
            listOf(PlacementType.PRESCHOOL_DAYCARE),
            placementBefore.placements.map { it.type }
        )
        assertEquals(listOf(), placementBefore.additionalPlacements)

        terminatePlacements(
            child.id,
            PlacementControllerCitizen.PlacementTerminationRequestBody(
                type = TerminatablePlacementType.PRESCHOOL,
                terminationDate = placementTerminationDate,
                unitId = daycareId,
                terminateDaycareOnly = true
            )
        )

        val childPlacements = getChildPlacements(child.id)

        assertEquals(1, childPlacements.size)
        val first = childPlacements[0]
        assertEquals(TerminatablePlacementType.PRESCHOOL, first.type)
        assertEquals(startPreschool, first.startDate)
        assertEquals(endPreschool, first.endDate)
        assertEquals(2, first.placements.size)
        assertEquals(0, first.additionalPlacements.size)

        val currentPlacement = first.placements[0]
        assertEquals(startPreschool, currentPlacement.startDate)
        assertEquals(placementTerminationDate, currentPlacement.endDate)
        assertEquals(PlacementType.PRESCHOOL_DAYCARE, currentPlacement.type)
        assertEquals(today, currentPlacement.terminationRequestedDate)
        assertEquals("${parent.lastName} ${parent.firstName}", currentPlacement.terminatedBy?.name)

        val remainderOfPreschool = first.placements[1]
        assertNull(remainderOfPreschool.terminationRequestedDate)
        assertEquals(PlacementType.PRESCHOOL, remainderOfPreschool.type)
        assertEquals(placementTerminationDate.plusDays(1), remainderOfPreschool.startDate)
        assertEquals(endPreschool, remainderOfPreschool.endDate)
    }

    @Test
    fun `terminating PRESCHOOL_DAYCARE with daycare only changes the remainder of the preschool to PRESCHOOL and terminates upcoming daycare`() {
        /*
        |------------ PRESCHOOL_DAYCARE -------------||--------- DAYCARE --------||--- PRESCHOOL_DAYCARE ---|
        1. terminateDaycareOnly = true
        |--- PRESCHOOL_DAYCARE -------||- PRESCHOOL -|                            |------- PRESCHOOL -------|
        2. terminate again terminateDaycareOnly = true
        |--- PRESCHOOL_DAYCARE --||------ PRESCHOOL -|                            |------- PRESCHOOL -------|
         */

        val startPreschool = today.minusWeeks(2)
        val endPreschool = startPreschool.plusMonths(1)
        val startDaycare = endPreschool.plusDays(1)
        val endDaycare = startDaycare.plusMonths(1)
        val startNextPreschoolDaycare = endDaycare.plusDays(1)
        val endNextPreschoolDaycare = startNextPreschoolDaycare.plusMonths(1)
        insertComplexPlacements(
            child.id,
            startPreschool,
            endPreschool,
            startDaycare,
            endDaycare,
            startNextPreschoolDaycare,
            endNextPreschoolDaycare
        )

        val placementsBefore = getChildPlacements(child.id)
        assertEquals(1, placementsBefore.size)
        val placementBefore = placementsBefore[0]
        assertEquals(TerminatablePlacementType.PRESCHOOL, placementBefore.type)
        assertEquals(startPreschool, placementBefore.startDate)
        assertEquals(endNextPreschoolDaycare, placementBefore.endDate)

        assertEquals(
            listOf(PlacementType.PRESCHOOL_DAYCARE, PlacementType.PRESCHOOL_DAYCARE),
            placementBefore.placements.map { it.type }
        )
        assertEquals(
            listOf(PlacementType.DAYCARE),
            placementBefore.additionalPlacements.map { it.type }
        )

        val allPlacementsBefore = placementBefore.placements + placementBefore.additionalPlacements
        val groupPlacementsBefore = getChildGroupPlacements(child.id)
        assertEquals(3, groupPlacementsBefore.size)
        allPlacementsBefore.forEach { placement ->
            val groupPlacements =
                groupPlacementsBefore.filter { it.daycarePlacementId == placement.id }
            assertEquals(1, groupPlacements.size)
            assertEquals(placement.startDate, groupPlacements[0].startDate)
            assertEquals(placement.endDate, groupPlacements[0].endDate)
        }

        val placementTerminationDate = today.plusWeeks(1)
        terminatePlacements(
            child.id,
            PlacementControllerCitizen.PlacementTerminationRequestBody(
                type = TerminatablePlacementType.PRESCHOOL,
                terminationDate = placementTerminationDate,
                unitId = daycareId,
                terminateDaycareOnly = true
            )
        )

        val childPlacements = getChildPlacements(child.id)

        assertEquals(1, childPlacements.size)
        val first = childPlacements[0]
        assertEquals(TerminatablePlacementType.PRESCHOOL, first.type)
        assertEquals(startPreschool, first.startDate)
        assertEquals(endNextPreschoolDaycare, first.endDate)
        assertEquals(3, first.placements.size)
        assertEquals(0, first.additionalPlacements.size)

        val currentPlacement = first.placements[0]
        assertEquals(startPreschool, currentPlacement.startDate)
        assertEquals(placementTerminationDate, currentPlacement.endDate)
        assertEquals(PlacementType.PRESCHOOL_DAYCARE, currentPlacement.type)
        assertEquals(today, currentPlacement.terminationRequestedDate)
        assertEquals(
            "${parent.lastName} ${parent.firstName}",
            childPlacements[0].placements[0].terminatedBy?.name
        )

        val remainderOfPreschool = first.placements[1]
        assertNull(remainderOfPreschool.terminationRequestedDate)
        assertEquals(PlacementType.PRESCHOOL, remainderOfPreschool.type)
        assertEquals(placementTerminationDate.plusDays(1), remainderOfPreschool.startDate)
        assertEquals(endPreschool, remainderOfPreschool.endDate)

        val childGroupPlacements = getChildGroupPlacements(child.id)
        assertEquals(2, childGroupPlacements.size)

        val currentGroupPlacements =
            childGroupPlacements.filter { it.daycarePlacementId == currentPlacement.id }
        assertNotNull(currentGroupPlacements)
        assertEquals(1, currentGroupPlacements.size)
        assertEquals(currentPlacement.startDate, currentGroupPlacements[0].startDate)
        assertEquals(currentPlacement.endDate, currentGroupPlacements[0].endDate)

        // the next PRESCHOOL_DAYCARE is simply converted to PRESCHOOL
        val nextPreschool = first.placements[2]
        assertNull(nextPreschool.terminationRequestedDate)
        assertEquals(PlacementType.PRESCHOOL, nextPreschool.type)
        assertEquals(startNextPreschoolDaycare, nextPreschool.startDate)
        assertEquals(endNextPreschoolDaycare, nextPreschool.endDate)

        val nextGroupPlacements =
            childGroupPlacements.filter { it.daycarePlacementId == nextPreschool.id }
        assertNotNull(nextGroupPlacements)
        assertEquals(1, nextGroupPlacements.size)
        assertEquals(nextPreschool.startDate, nextGroupPlacements[0].startDate)
        assertEquals(nextPreschool.endDate, nextGroupPlacements[0].endDate)

        // when terminated again with an earlier date
        val terminationDate2 = placementTerminationDate.minusDays(5)
        terminatePlacements(
            child.id,
            PlacementControllerCitizen.PlacementTerminationRequestBody(
                type = TerminatablePlacementType.PRESCHOOL,
                terminationDate = terminationDate2,
                unitId = daycareId,
                terminateDaycareOnly = true
            )
        )

        // then only the preschool+daycare and preschool start and end dates are modified
        val placementsAfterSecondTermination = getChildPlacements(child.id)
        assertEquals(1, placementsAfterSecondTermination.size)
        assertEquals(3, placementsAfterSecondTermination[0].placements.size)
        assertEquals(
            currentPlacement.copy(endDate = terminationDate2),
            placementsAfterSecondTermination[0].placements[0]
        )
        assertEquals(
            remainderOfPreschool.copy(startDate = terminationDate2.plusDays(1)),
            placementsAfterSecondTermination[0].placements[1]
        )

        // untouched
        val lastPreschool = placementsAfterSecondTermination[0].placements[2]
        assertNull(lastPreschool.terminationRequestedDate)
        assertEquals(PlacementType.PRESCHOOL, lastPreschool.type)
        assertEquals(startNextPreschoolDaycare, lastPreschool.startDate)
        assertEquals(endNextPreschoolDaycare, lastPreschool.endDate)
    }

    @Test
    fun `terminating PRESCHOOL_DAYCARE with daycare only does not affect anything before termination date`() {
        /*
        |--- PRESCHOOL_DAYCARE ---||--------- DAYCARE --------||--- PRESCHOOL_DAYCARE ---|
        1. terminateDaycareOnly = true
                                                                 terminationDate   x
        |--- PRESCHOOL_DAYCARE ---||--------- DAYCARE --------||------ P_D --------|--P--|
        2. terminate again terminateDaycareOnly = true
                             terminationDate   x
        |--- PRESCHOOL_DAYCARE ---||- DAYCARE -|               |------- PRESCHOOL -------|
        */

        val startPreschool = today.minusWeeks(2)
        val endPreschool = startPreschool.plusMonths(1)
        val startDaycare = endPreschool.plusDays(1)
        val endDaycare = startDaycare.plusMonths(1)
        val startNextPreschoolDaycare = endDaycare.plusDays(1)
        val endNextPreschoolDaycare = startNextPreschoolDaycare.plusMonths(1)
        insertComplexPlacements(
            child.id,
            startPreschool,
            endPreschool,
            startDaycare,
            endDaycare,
            startNextPreschoolDaycare,
            endNextPreschoolDaycare
        )

        val placementTerminationDate = endNextPreschoolDaycare.minusWeeks(1)
        terminatePlacements(
            child.id,
            PlacementControllerCitizen.PlacementTerminationRequestBody(
                type = TerminatablePlacementType.PRESCHOOL,
                terminationDate = placementTerminationDate,
                unitId = daycareId,
                terminateDaycareOnly = true
            )
        )

        val placementGroups = getChildPlacements(child.id)
        assertEquals(1, placementGroups.size)
        val group1 = placementGroups[0]
        assertEquals(TerminatablePlacementType.PRESCHOOL, group1.type)
        assertEquals(startPreschool, group1.startDate)
        assertEquals(endNextPreschoolDaycare, group1.endDate)
        assertEquals(3, group1.placements.size)
        assertEquals(1, group1.additionalPlacements.size)

        assertPlacements(
            listOf(
                Triple(PlacementType.PRESCHOOL_DAYCARE, startPreschool, endPreschool),
                Triple(PlacementType.DAYCARE, startDaycare, endDaycare),
                Triple(
                    PlacementType.PRESCHOOL_DAYCARE,
                    startNextPreschoolDaycare,
                    placementTerminationDate
                ),
                Triple(
                    PlacementType.PRESCHOOL,
                    placementTerminationDate.plusDays(1),
                    endNextPreschoolDaycare
                )
            ),
            group1
        )

        // when terminated again with an earlier date
        val terminationDate2 = startDaycare.plusWeeks(1)
        terminatePlacements(
            child.id,
            PlacementControllerCitizen.PlacementTerminationRequestBody(
                type = TerminatablePlacementType.PRESCHOOL,
                terminationDate = terminationDate2,
                unitId = daycareId,
                terminateDaycareOnly = true
            )
        )

        val groups = getChildPlacements(child.id)
        assertEquals(1, groups.size)
        val group = groups[0]
        assertEquals(TerminatablePlacementType.PRESCHOOL, group.type)
        assertEquals(startPreschool, group.startDate)
        assertEquals(endNextPreschoolDaycare, group.endDate)

        assertPlacements(
            listOf(
                Triple(PlacementType.PRESCHOOL_DAYCARE, startPreschool, endPreschool),
                Triple(PlacementType.DAYCARE, startDaycare, terminationDate2),
                Triple(PlacementType.PRESCHOOL, startNextPreschoolDaycare, endNextPreschoolDaycare)
            ),
            group
        )
    }

    private fun assertPlacements(
        expected: List<Triple<PlacementType, LocalDate, LocalDate>>,
        group: TerminatablePlacementGroup
    ) {
        val placements = group.placements + group.additionalPlacements

        expected.forEach { (type, start, end) ->
            assertNotNull(
                placements.find { it.type == type && it.startDate == start && it.endDate == end },
                "$type $start $end not found in $placements"
            )
        }

        assertEquals(expected.size, placements.size)
    }

    /*
    |------------ PRESCHOOL_DAYCARE -------------||--------- DAYCARE --------||--- PRESCHOOL_DAYCARE ---|
    */
    private fun insertComplexPlacements(
        childId: PersonId,
        startPreschool: LocalDate,
        endPreschool: LocalDate,
        startDaycare: LocalDate,
        endDaycare: LocalDate,
        startNextPreschoolDaycare: LocalDate,
        endNextPreschoolDaycare: LocalDate
    ) {
        db.transaction {
            it.insert(
                    DevPlacement(
                        type = PlacementType.PRESCHOOL_DAYCARE,
                        childId = childId,
                        unitId = daycareId,
                        startDate = startPreschool,
                        endDate = endPreschool
                    )
                )
                .let { id ->
                    it.insertServiceNeed(
                        id,
                        startPreschool,
                        endPreschool.minusDays(10),
                        snPreschoolDaycare45.id,
                        ShiftCareType.NONE,
                        false,
                        null,
                        null
                    )
                    it.insertServiceNeed(
                        id,
                        endPreschool.minusDays(9),
                        endPreschool,
                        snPreschoolDaycarePartDay35.id,
                        ShiftCareType.NONE,
                        false,
                        null,
                        null
                    )
                    it.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = id,
                            daycareGroupId = daycareGroup.id,
                            startDate = startPreschool,
                            endDate = endPreschool
                        )
                    )
                }
            it.insert(
                    DevPlacement(
                        type = PlacementType.DAYCARE,
                        childId = childId,
                        unitId = daycareId,
                        startDate = startDaycare,
                        endDate = endDaycare
                    )
                )
                .let { id ->
                    it.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = id,
                            daycareGroupId = daycareGroup.id,
                            startDate = startDaycare,
                            endDate = endDaycare
                        )
                    )
                }
            it.insert(
                    DevPlacement(
                        type = PlacementType.PRESCHOOL_DAYCARE,
                        childId = childId,
                        unitId = daycareId,
                        startDate = startNextPreschoolDaycare,
                        endDate = endNextPreschoolDaycare
                    )
                )
                .let { id ->
                    it.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = id,
                            daycareGroupId = daycareGroup.id,
                            startDate = startNextPreschoolDaycare,
                            endDate = endNextPreschoolDaycare
                        )
                    )
                }
        }
    }

    @Test
    fun `placement cannot be terminated if placement termination is not in unit's enabled features`() {
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycareId,
                    startDate = placementStart,
                    endDate = placementEnd
                )
            )
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare2Id,
                    startDate = placementEnd.plusDays(1),
                    endDate = placementEnd.plusMonths(2)
                )
            )
        }

        val body =
            PlacementControllerCitizen.PlacementTerminationRequestBody(
                type = TerminatablePlacementType.DAYCARE,
                terminationDate = today.plusDays(1),
                unitId = daycare2Id,
                terminateDaycareOnly = false
            )
        assertThrows<Forbidden> { terminatePlacements(child.id, body) }
        db.transaction {
            it.addUnitFeatures(listOf(daycare2Id), listOf(PilotFeature.PLACEMENT_TERMINATION))
        }
        terminatePlacements(child.id, body)
    }

    @Test
    fun `All active transfer applications are cancelled`() {
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycareId,
                    startDate = placementStart,
                    endDate = placementEnd
                )
            )
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare2Id,
                    startDate = placementEnd.plusDays(1),
                    endDate = placementEnd.plusMonths(2)
                )
            )
        }

        val placementTerminationDate = today.plusDays(1)

        val applicationBeforeTermination =
            db.transaction { tx ->
                // given
                tx.insertApplication(
                    guardian = parent,
                    child = child,
                    applicationId = ApplicationId(UUID.randomUUID()),
                    preferredStartDate = placementTerminationDate.plusDays(-1),
                    transferApplication = true,
                    status = ApplicationStatus.SENT
                )
            }

        val applicationAfterTermination =
            db.transaction { tx ->
                tx.insertApplication(
                    guardian = parent,
                    child = child,
                    applicationId = ApplicationId(UUID.randomUUID()),
                    preferredStartDate = placementTerminationDate.plusDays(1),
                    transferApplication = true,
                    status = ApplicationStatus.SENT
                )
            }

        terminatePlacements(
            child.id,
            PlacementControllerCitizen.PlacementTerminationRequestBody(
                type = TerminatablePlacementType.DAYCARE,
                terminationDate = placementTerminationDate,
                unitId = daycareId,
                terminateDaycareOnly = false
            )
        )

        assertEquals(
            ApplicationStatus.CANCELLED,
            db.read { it.getApplicationStatus(applicationBeforeTermination.id) }
        )
        assertEquals(
            ApplicationStatus.CANCELLED,
            db.read { it.getApplicationStatus(applicationAfterTermination.id) }
        )

        val childPlacements = getChildPlacements(child.id)
        assertEquals(2, childPlacements.size)
        assertEquals(placementStart, childPlacements[0].startDate)
        assertEquals(placementTerminationDate, childPlacements[0].endDate)
        assertEquals(TerminatablePlacementType.DAYCARE, childPlacements[0].type)
        assertEquals(1, childPlacements[0].placements.size)
        assertEquals(today, childPlacements[0].placements[0].terminationRequestedDate)
        assertEquals(
            "${parent.lastName} ${parent.firstName}",
            childPlacements[0].placements[0].terminatedBy?.name
        )

        assertEquals(
            listOf(null),
            (childPlacements[1].placements + childPlacements[1].additionalPlacements).map {
                it.terminationRequestedDate
            }
        )
    }

    @Test
    fun `terminating placement affects backup care`() {
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycareId,
                    startDate = placementStart,
                    endDate = placementEnd
                )
            )
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare2Id,
                    startDate = placementEnd.plusDays(1),
                    endDate = placementEnd.plusMonths(2)
                )
            )
            tx.insert(
                DevBackupCare(
                    childId = child.id,
                    unitId = daycare2Id,
                    period = FiniteDateRange(today.minusMonths(1), today.plusMonths(1))
                )
            )
        }

        val placementTerminationDate = today.plusDays(1)

        terminatePlacements(
            child.id,
            PlacementControllerCitizen.PlacementTerminationRequestBody(
                type = TerminatablePlacementType.DAYCARE,
                terminationDate = placementTerminationDate,
                unitId = daycareId,
                terminateDaycareOnly = false
            )
        )

        val backupCares = db.transaction { it.getBackupCaresForChild(child.id) }
        assertEquals(1, backupCares.size)
        assertEquals(placementTerminationDate, backupCares[0].period.end)
    }

    @Test
    fun `terminating placement removes a future backup care`() {
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycareId,
                    startDate = placementStart,
                    endDate = placementEnd
                )
            )
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare2Id,
                    startDate = placementEnd.plusDays(1),
                    endDate = placementEnd.plusMonths(2)
                )
            )
            tx.insert(
                DevBackupCare(
                    childId = child.id,
                    unitId = daycare2Id,
                    period = FiniteDateRange(today.plusDays(2), today.plusDays(12))
                )
            )
        }

        val placementTerminationDate = today.plusDays(1)

        terminatePlacements(
            child.id,
            PlacementControllerCitizen.PlacementTerminationRequestBody(
                type = TerminatablePlacementType.DAYCARE,
                terminationDate = placementTerminationDate,
                unitId = daycareId,
                terminateDaycareOnly = false
            )
        )

        val backupCares = db.transaction { it.getBackupCaresForChild(child.id) }
        assertEquals(emptyList(), backupCares)
    }

    @Test
    fun `should log a PlacementTerminate Audit event`(capturedOutput: CapturedOutput) {
        lateinit var terminatedPlacementId: PlacementId

        db.transaction { tx ->
            terminatedPlacementId =
                tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = daycareId,
                        startDate = placementStart,
                        endDate = placementEnd
                    )
                )
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare2Id,
                    startDate = placementEnd.plusDays(1),
                    endDate = placementEnd.plusMonths(2)
                )
            )
        }

        val placementTerminationDate = today.plusDays(1)

        terminatePlacements(
            child.id,
            PlacementControllerCitizen.PlacementTerminationRequestBody(
                type = TerminatablePlacementType.DAYCARE,
                terminationDate = placementTerminationDate,
                unitId = daycareId,
                terminateDaycareOnly = false
            )
        )

        assertTrue(capturedOutput.out.contains("\"targetId\":[\"$daycareId\",\"${child.id}"))
        assertTrue(capturedOutput.out.contains("\"objectId\":[\"$terminatedPlacementId\"]"))
        assertTrue(
            capturedOutput.out.contains(
                "\"meta\":{\"type\":\"DAYCARE\",\"placementIds\":[\"$terminatedPlacementId\"],\"transferApplicationIds\":[]}"
            )
        )
    }

    private fun terminatePlacements(
        childId: ChildId,
        termination: PlacementControllerCitizen.PlacementTerminationRequestBody,
    ) {
        placementControllerCitizen.postPlacementTermination(
            dbInstance(),
            authenticatedParent,
            RealEvakaClock(),
            childId,
            termination
        )
    }

    private fun getChildPlacements(childId: ChildId): List<TerminatablePlacementGroup> {
        return placementControllerCitizen
            .getPlacements(dbInstance(), authenticatedParent, RealEvakaClock(), childId)
            .placements
    }

    private fun getChildGroupPlacements(childId: ChildId): List<DaycareGroupPlacement> {
        return placementController
            .getPlacements(dbInstance(), admin, RealEvakaClock(), childId = childId)
            .placements
            .toList()
            .flatMap { it.groupPlacements }
            .filter { it.id != null }
    }
}
