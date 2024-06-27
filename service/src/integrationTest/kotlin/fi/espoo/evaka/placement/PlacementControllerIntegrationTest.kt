// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import com.github.kittinunf.fuel.core.ResponseResultOf
import com.github.kittinunf.fuel.jackson.objectBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.absence.getAbsencesOfChildByRange
import fi.espoo.evaka.backupcare.getBackupCaresForChild
import fi.espoo.evaka.reservations.DailyReservationRequest
import fi.espoo.evaka.reservations.createReservationsAndAbsences
import fi.espoo.evaka.reservations.getReservationsForChildInRange
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.updateDaycareAclWithEmployee
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testDecisionMaker_2
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class PlacementControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {

    @Autowired lateinit var placementController: PlacementController

    private val mockClock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2023, 1, 1), LocalTime.of(12, 0)))

    final val childId = testChild_1.id
    final val daycareId = testDaycare.id
    final val testDaycareGroup = DevDaycareGroup(daycareId = daycareId)
    final val groupId = testDaycareGroup.id

    final val placementStart = LocalDate.of(2020, 1, 1)
    final val placementEnd = placementStart.plusDays(200)
    lateinit var testPlacement: DaycarePlacementDetails

    private val unitSupervisor = AuthenticatedUser.Employee(testDecisionMaker_1.id, emptySet())
    private val staff = AuthenticatedUser.Employee(testDecisionMaker_2.id, emptySet())
    private val serviceWorker =
        AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.SERVICE_WORKER))
    private val admin =
        AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.ADMIN))

    private val citizenReservationThresholdHours: Long = 150

    @BeforeEach
    fun setUp() {
        db.transaction { tx ->
            tx.insert(testDecisionMaker_1)
            tx.insert(testDecisionMaker_2)
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testDaycare2)
            tx.insert(testChild_1, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = childId,
                    unitId = daycareId,
                    startDate = placementStart,
                    endDate = placementEnd
                )
            )
            tx.insert(testDaycareGroup)
            testPlacement = tx.getDaycarePlacements(daycareId, null, null, null).first()
            tx.updateDaycareAclWithEmployee(daycareId, unitSupervisor.id, UserRole.UNIT_SUPERVISOR)
        }
    }

    @Test
    fun `get placements works with daycareId and without dates`() {
        val (_, res, result) =
            http
                .get("/placements?daycareId=$daycareId")
                .asUser(serviceWorker)
                .responseObject<PlacementResponse>(jsonMapper)

        Assertions.assertThat(res.statusCode).isEqualTo(200)

        val placements = result.get().placements.toList()
        Assertions.assertThat(placements).hasSize(1)

        val placement = placements[0]
        Assertions.assertThat(placement.daycare.id).isEqualTo(daycareId)
        Assertions.assertThat(placement.daycare.name).isEqualTo(testDaycare.name)
        Assertions.assertThat(placement.child.id).isEqualTo(childId)
        Assertions.assertThat(placement.startDate).isEqualTo(placementStart)
        Assertions.assertThat(placement.endDate).isEqualTo(placementEnd)
        Assertions.assertThat(placement.groupPlacements).hasSize(1)

        val placeholder = placement.groupPlacements.first()
        Assertions.assertThat(placeholder.startDate).isEqualTo(placementStart)
        Assertions.assertThat(placeholder.endDate).isEqualTo(placementEnd)
        Assertions.assertThat(placeholder.groupId).isNull()
    }

    @Test
    fun `get placements works with with daycareId and matching dates`() {
        val (_, res, result) =
            http
                .get(
                    "/placements?daycareId=$daycareId&from=$placementStart&to=${placementStart.plusDays(900)}"
                )
                .asUser(serviceWorker)
                .responseObject<PlacementResponse>(jsonMapper)

        Assertions.assertThat(res.statusCode).isEqualTo(200)
        Assertions.assertThat(result.get().placements).hasSize(1)
    }

    @Test
    fun `get placements works with with daycareId and non-matching dates`() {
        val (_, res, result) =
            http
                .get(
                    "/placements?daycareId=$daycareId&from=${placementStart.minusDays(900)}&to=${placementEnd.minusDays(300)}"
                )
                .asUser(serviceWorker)
                .responseObject<PlacementResponse>(jsonMapper)

        Assertions.assertThat(res.statusCode).isEqualTo(200)
        Assertions.assertThat(result.get().placements).hasSize(0)
    }

    @Test
    fun `get placements returns an empty list if daycare is not found`() {
        val (_, res, result) =
            http
                .get("/placements?daycareId=${UUID.randomUUID()}")
                .asUser(serviceWorker)
                .responseObject<PlacementResponse>(jsonMapper)

        Assertions.assertThat(res.statusCode).isEqualTo(200)
        Assertions.assertThat(result.get().placements).isEqualTo(setOf<PlacementResponse>())
    }

    @Test
    fun `get placements works with childId and without dates`() {
        val (_, res, result) =
            http
                .get("/placements?childId=$childId")
                .asUser(serviceWorker)
                .responseObject<PlacementResponse>(jsonMapper)

        Assertions.assertThat(res.statusCode).isEqualTo(200)

        val placements = result.get().placements.toList()
        Assertions.assertThat(placements).hasSize(1)

        val placement = placements[0]
        Assertions.assertThat(placement.daycare.id).isEqualTo(daycareId)
        Assertions.assertThat(placement.daycare.name).isEqualTo(testDaycare.name)
        Assertions.assertThat(placement.child.id).isEqualTo(childId)
        Assertions.assertThat(placement.startDate).isEqualTo(placementStart)
        Assertions.assertThat(placement.endDate).isEqualTo(placementEnd)
    }

    @Test
    fun `get placements works with with childId and matching dates`() {
        val (_, res, result) =
            http
                .get(
                    "/placements?childId=$childId&from=$placementStart&to=${placementStart.plusDays(900)}"
                )
                .asUser(serviceWorker)
                .responseObject<PlacementResponse>(jsonMapper)

        Assertions.assertThat(res.statusCode).isEqualTo(200)
        Assertions.assertThat(result.get().placements).hasSize(1)
    }

    @Test
    fun `get placements works with with childId and non-matching dates`() {
        val (_, res, result) =
            http
                .get(
                    "/placements?childId=$childId&from=${placementStart.minusDays(900)}&to=${placementStart.minusDays(300)}"
                )
                .asUser(serviceWorker)
                .responseObject<PlacementResponse>(jsonMapper)

        Assertions.assertThat(res.statusCode).isEqualTo(200)
        Assertions.assertThat(result.get().placements).hasSize(0)
    }

    @Test
    fun `get placements throws BadRequest if daycare id and child id is not given`() {
        val (_, res, _) = http.get("/placements").asUser(serviceWorker).response()

        Assertions.assertThat(res.statusCode).isEqualTo(400)
    }

    @Test
    fun `creating group placement works with valid data`() {
        val groupPlacementStart = placementStart.plusDays(1)
        val groupPlacementEnd = placementEnd.minusDays(1)
        val (_, res, _) =
            createGroupPlacement(
                testPlacement.id,
                GroupPlacementRequestBody(groupId, groupPlacementStart, groupPlacementEnd)
            )
        Assertions.assertThat(res.statusCode).isEqualTo(200)

        val groupPlacements = getGroupPlacements(childId, daycareId)
        Assertions.assertThat(groupPlacements.size).isEqualTo(1)
        Assertions.assertThat(groupPlacements.first().daycarePlacementId)
            .isEqualTo(testPlacement.id)
        Assertions.assertThat(groupPlacements.first().groupId).isEqualTo(groupId)
        Assertions.assertThat(groupPlacements.first().startDate).isEqualTo(groupPlacementStart)
        Assertions.assertThat(groupPlacements.first().endDate).isEqualTo(groupPlacementEnd)
    }

    @Test
    fun `creating group placement right after another merges them`() {
        createGroupPlacement(
            testPlacement.id,
            GroupPlacementRequestBody(groupId, placementStart, placementStart.plusDays(3))
        )
        createGroupPlacement(
            testPlacement.id,
            GroupPlacementRequestBody(groupId, placementStart.plusDays(4), placementEnd)
        )

        val groupPlacements = getGroupPlacements(childId, daycareId)
        Assertions.assertThat(groupPlacements.size).isEqualTo(1)
        Assertions.assertThat(groupPlacements.first().daycarePlacementId)
            .isEqualTo(testPlacement.id)
        Assertions.assertThat(groupPlacements.first().groupId).isEqualTo(groupId)
        Assertions.assertThat(groupPlacements.first().startDate).isEqualTo(placementStart)
        Assertions.assertThat(groupPlacements.first().endDate).isEqualTo(placementEnd)
    }

    @Test
    fun `creating group placement right before another merges them`() {
        createGroupPlacement(
            testPlacement.id,
            GroupPlacementRequestBody(groupId, placementEnd.minusDays(3), placementEnd)
        )
        createGroupPlacement(
            testPlacement.id,
            GroupPlacementRequestBody(groupId, placementStart, placementEnd.minusDays(4))
        )

        val groupPlacements = getGroupPlacements(childId, daycareId)
        Assertions.assertThat(groupPlacements.size).isEqualTo(1)
        Assertions.assertThat(groupPlacements.first().daycarePlacementId)
            .isEqualTo(testPlacement.id)
        Assertions.assertThat(groupPlacements.first().groupId).isEqualTo(groupId)
        Assertions.assertThat(groupPlacements.first().startDate).isEqualTo(placementStart)
        Assertions.assertThat(groupPlacements.first().endDate).isEqualTo(placementEnd)
    }

    @Test
    fun `creating group placement between two merges them`() {
        createGroupPlacement(
            testPlacement.id,
            GroupPlacementRequestBody(groupId, placementStart, placementStart.plusDays(2))
        )
        createGroupPlacement(
            testPlacement.id,
            GroupPlacementRequestBody(groupId, placementEnd.minusDays(2), placementEnd)
        )
        createGroupPlacement(
            testPlacement.id,
            GroupPlacementRequestBody(
                groupId,
                placementStart.plusDays(3),
                placementEnd.minusDays(3)
            )
        )

        val groupPlacements = getGroupPlacements(childId, daycareId)
        Assertions.assertThat(groupPlacements.size).isEqualTo(1)
        Assertions.assertThat(groupPlacements.first().daycarePlacementId)
            .isEqualTo(testPlacement.id)
        Assertions.assertThat(groupPlacements.first().groupId).isEqualTo(groupId)
        Assertions.assertThat(groupPlacements.first().startDate).isEqualTo(placementStart)
        Assertions.assertThat(groupPlacements.first().endDate).isEqualTo(placementEnd)
    }

    @Test
    fun `group placements are not merged if they have gap between`() {
        createGroupPlacement(
            testPlacement.id,
            GroupPlacementRequestBody(groupId, placementStart, placementStart.plusDays(2))
        )
        createGroupPlacement(
            testPlacement.id,
            GroupPlacementRequestBody(groupId, placementEnd.minusDays(2), placementEnd)
        )
        createGroupPlacement(
            testPlacement.id,
            GroupPlacementRequestBody(
                groupId,
                placementStart.plusDays(4),
                placementEnd.minusDays(4)
            )
        )

        val groupPlacements = getGroupPlacements(childId, daycareId)
        Assertions.assertThat(groupPlacements.size).isEqualTo(3)
    }

    @Test
    fun `creating group placement throws NotFound if group does not exist`() {
        val (_, res, _) =
            createGroupPlacement(
                testPlacement.id,
                GroupPlacementRequestBody(GroupId(UUID.randomUUID()), placementStart, placementEnd)
            )

        Assertions.assertThat(res.statusCode).isEqualTo(404)
    }

    @Test
    fun `Creating overlapping future placement with different absence category, should delete future absences with wrong category type that are in the range of new placements period`() {
        val activePlacementStart = mockClock.today().minusMonths(3)
        val activePlacementEnd = mockClock.today().plusMonths(6)

        val activePlacement =
            createPlacementAndGroupPlacement(
                activePlacementStart,
                activePlacementEnd,
                childId,
                daycareId,
                groupId
            )

        // Create absences
        val firstAbsence = activePlacementStart.plusDays(10)
        val secondAbsence = activePlacementStart.plusDays(11)
        val thirdAbsence = mockClock.today().plusDays(5)
        val fourthAbsence = mockClock.today().plusDays(6)
        db.transaction {
            createReservationsAndAbsences(
                it,
                HelsinkiDateTime.of(activePlacementStart, LocalTime.of(12, 0)),
                unitSupervisor,
                listOf(
                    DailyReservationRequest.Absent(
                        childId = childId,
                        date = firstAbsence,
                    ),
                    DailyReservationRequest.Absent(
                        childId = childId,
                        date = secondAbsence,
                    ),
                    DailyReservationRequest.Absent(
                        childId = childId,
                        date = thirdAbsence,
                    ),
                    DailyReservationRequest.Absent(
                        childId = childId,
                        date = fourthAbsence,
                    ),
                ),
                citizenReservationThresholdHours
            )
        }

        // then 2 absences with correct category types
        val absences =
            getAbsencesOfChildByRange(DateRange(activePlacementStart, activePlacementEnd))
        assertEquals(4, absences.size)
        assertEquals(firstAbsence, absences[0].date)
        assertEquals(secondAbsence, absences[1].date)
        assertEquals(thirdAbsence, absences[2].date)
        assertEquals(fourthAbsence, absences[3].date)
        assertContains(activePlacement.type.absenceCategories(), absences[0].category)
        assertContains(activePlacement.type.absenceCategories(), absences[1].category)
        assertContains(activePlacement.type.absenceCategories(), absences[2].category)
        assertContains(activePlacement.type.absenceCategories(), absences[3].category)

        // Create placement with different absence category that overlaps with original placements
        // second absence
        // -> should delete second absence, because its wrong absence type
        val newPlacementStartDate = thirdAbsence
        val newPlacementEndDate = activePlacementEnd.plusWeeks(1)

        placementController.createPlacement(
            dbInstance(),
            unitSupervisor,
            mockClock,
            PlacementCreateRequestBody(
                PlacementType.PRESCHOOL,
                childId,
                daycareId,
                newPlacementStartDate,
                newPlacementEndDate,
                false
            )
        )

        val placements = db.read { r -> r.getPlacementsForChild(childId) }
        assertEquals(3, placements.size)
        val newPlacement =
            placements.find { it.id != testPlacement.id && it.id != activePlacement.id }!!

        val secondGroupPlacementId =
            placementController.createGroupPlacement(
                dbInstance(),
                unitSupervisor,
                mockClock,
                newPlacement.id,
                GroupPlacementRequestBody(groupId, newPlacementStartDate, newPlacementEndDate)
            )
        assertNotNull(secondGroupPlacementId)

        // Verify that the future absences in new placement period has been deleted
        val updatedAbsences =
            getAbsencesOfChildByRange(DateRange(activePlacementStart, activePlacementEnd))
        assertEquals(2, updatedAbsences.size)
        assertEquals(firstAbsence, updatedAbsences[0].date)
        assertEquals(secondAbsence, updatedAbsences[1].date)
        assertContains(activePlacement.type.absenceCategories(), updatedAbsences[0].category)
        assertContains(activePlacement.type.absenceCategories(), updatedAbsences[1].category)
    }

    @Test
    fun `Creating overlapping placement, should delete future attendance reservations of old placement that are in the range of new placements period`() {
        val activePlacementStart = mockClock.today().minusMonths(3)
        val activePlacementEnd = mockClock.today().plusMonths(6)

        val activePlacement =
            createPlacementAndGroupPlacement(
                activePlacementStart,
                activePlacementEnd,
                childId,
                daycareId,
                groupId
            )

        // Create reservations
        val reservationTime = TimeRange(LocalTime.of(9, 0), LocalTime.of(17, 0))
        val firstReservation = activePlacementStart.plusDays(10)
        val secondReservation = activePlacementStart.plusDays(11)
        val thirdReservation = mockClock.today().plusDays(8)
        val fourthReservation = mockClock.today().plusDays(9)
        db.transaction {
            createReservationsAndAbsences(
                it,
                HelsinkiDateTime.of(activePlacementStart, LocalTime.of(12, 0)),
                unitSupervisor,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = childId,
                        date = firstReservation,
                        reservation = reservationTime
                    ),
                    DailyReservationRequest.Reservations(
                        childId = childId,
                        date = secondReservation,
                        reservation = reservationTime
                    ),
                    DailyReservationRequest.Reservations(
                        childId = childId,
                        date = thirdReservation,
                        reservation = reservationTime
                    ),
                    DailyReservationRequest.Reservations(
                        childId = childId,
                        date = fourthReservation,
                        reservation = reservationTime
                    ),
                ),
                citizenReservationThresholdHours
            )
        }

        // then 4 reservathions
        val reservations =
            db.read {
                it.getReservationsForChildInRange(
                    childId,
                    FiniteDateRange(activePlacementStart, activePlacementEnd)
                )
            }
        assertEquals(4, reservations.size)
        assertTrue(reservations.containsKey(firstReservation))
        assertTrue(reservations.containsKey(secondReservation))
        assertTrue(reservations.containsKey(thirdReservation))
        assertTrue(reservations.containsKey(fourthReservation))

        // Create placement that overlaps with original placements third and fourth reservation
        // -> should delete third and fourth reservations, because they are not in the new
        // placements period
        val newPlacementStartDate = thirdReservation
        val newPlacementEndDate = activePlacementEnd.plusWeeks(1)

        placementController.createPlacement(
            dbInstance(),
            unitSupervisor,
            mockClock,
            PlacementCreateRequestBody(
                PlacementType.PRESCHOOL,
                childId,
                daycareId,
                newPlacementStartDate,
                newPlacementEndDate,
                false
            )
        )

        val placements = db.read { r -> r.getPlacementsForChild(childId) }
        assertEquals(3, placements.size)
        val newPlacement =
            placements.find { it.id != testPlacement.id && it.id != activePlacement.id }!!

        val secondGroupPlacementId =
            placementController.createGroupPlacement(
                dbInstance(),
                unitSupervisor,
                mockClock,
                newPlacement.id,
                GroupPlacementRequestBody(groupId, newPlacementStartDate, newPlacementEndDate)
            )
        assertNotNull(secondGroupPlacementId)

        // Verify that the future absences in new placement period has been deleted
        val updatedReservations =
            db.read {
                it.getReservationsForChildInRange(
                    childId,
                    FiniteDateRange(activePlacementStart, activePlacementEnd)
                )
            }
        assertEquals(2, updatedReservations.size)
        assertTrue(updatedReservations.containsKey(firstReservation))
        assertTrue(updatedReservations.containsKey(secondReservation))
    }

    @Test
    fun `Modifying placement period end date, should delete future absences that are not in range`() {
        val activePlacementStart = mockClock.today().minusMonths(3)
        val activePlacementEnd = mockClock.today().plusMonths(6)

        val activePlacement =
            createPlacementAndGroupPlacement(
                activePlacementStart,
                activePlacementEnd,
                childId,
                daycareId,
                groupId
            )

        // Create absences
        val firstAbsence = activePlacementStart.plusDays(10)
        val secondAbsence = mockClock.today().plusDays(8)
        val thirdAbsence = mockClock.today().plusDays(15)
        val fourthAbsence = mockClock.today().plusDays(16)
        db.transaction {
            createReservationsAndAbsences(
                it,
                HelsinkiDateTime.of(activePlacementStart, LocalTime.of(12, 0)),
                unitSupervisor,
                listOf(
                    DailyReservationRequest.Absent(
                        childId = childId,
                        date = firstAbsence,
                    ),
                    DailyReservationRequest.Absent(
                        childId = childId,
                        date = secondAbsence,
                    ),
                    DailyReservationRequest.Absent(
                        childId = childId,
                        date = thirdAbsence,
                    ),
                    DailyReservationRequest.Absent(
                        childId = childId,
                        date = fourthAbsence,
                    ),
                ),
                citizenReservationThresholdHours
            )
        }

        // then 4 absences with correct category types
        val absences =
            getAbsencesOfChildByRange(DateRange(activePlacementStart, activePlacementEnd))
        assertEquals(4, absences.size)
        assertEquals(firstAbsence, absences[0].date)
        assertEquals(secondAbsence, absences[1].date)
        assertEquals(thirdAbsence, absences[2].date)
        assertEquals(fourthAbsence, absences[3].date)
        assertContains(activePlacement.type.absenceCategories(), absences[0].category)
        assertContains(activePlacement.type.absenceCategories(), absences[1].category)
        assertContains(activePlacement.type.absenceCategories(), absences[2].category)
        assertContains(activePlacement.type.absenceCategories(), absences[3].category)

        // Modify placement period so that only first and second absence are in the placements
        // period range
        // -> should delete third and fourth absence
        val newEndDate = mockClock.today().plusDays(10)
        placementController.updatePlacementById(
            dbInstance(),
            unitSupervisor,
            mockClock,
            activePlacement.id,
            PlacementUpdateRequestBody(activePlacementStart, newEndDate)
        )

        val placements = db.read { r -> r.getPlacementsForChild(childId) }
        assertEquals(2, placements.size)
        val updatedPlacement = placements.find { it.id != testPlacement.id }!!
        assertEquals(updatedPlacement.startDate, activePlacementStart)
        assertEquals(updatedPlacement.endDate, newEndDate)

        // Verify that the future absences in new placement period has been deleted
        val updatedAbsences =
            getAbsencesOfChildByRange(DateRange(activePlacementStart, activePlacementEnd))
        assertEquals(2, updatedAbsences.size)
        assertEquals(firstAbsence, updatedAbsences[0].date)
        assertEquals(secondAbsence, updatedAbsences[1].date)
        assertContains(activePlacement.type.absenceCategories(), updatedAbsences[0].category)
        assertContains(activePlacement.type.absenceCategories(), updatedAbsences[1].category)
    }

    @Test
    fun `Modifying placement period end date, should delete future attendance reservations that are not in range`() {
        val activePlacementStart = mockClock.today().minusMonths(3)
        val activePlacementEnd = mockClock.today().plusMonths(6)

        val activePlacement =
            createPlacementAndGroupPlacement(
                activePlacementStart,
                activePlacementEnd,
                childId,
                daycareId,
                groupId
            )

        // Create reservations
        val reservationTime = TimeRange(LocalTime.of(9, 0), LocalTime.of(17, 0))
        val firstReservation = activePlacementStart.plusDays(10)
        val secondReservation = mockClock.today().plusDays(8)
        val thirdReservation = mockClock.today().plusDays(15)
        val fourthReservation = mockClock.today().plusDays(16)
        db.transaction {
            createReservationsAndAbsences(
                it,
                HelsinkiDateTime.of(activePlacementStart, LocalTime.of(12, 0)),
                unitSupervisor,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = childId,
                        date = firstReservation,
                        reservation = reservationTime
                    ),
                    DailyReservationRequest.Reservations(
                        childId = childId,
                        date = secondReservation,
                        reservation = reservationTime
                    ),
                    DailyReservationRequest.Reservations(
                        childId = childId,
                        date = thirdReservation,
                        reservation = reservationTime
                    ),
                    DailyReservationRequest.Reservations(
                        childId = childId,
                        date = fourthReservation,
                        reservation = reservationTime
                    ),
                ),
                citizenReservationThresholdHours
            )
        }

        // then 4 reservathions
        val reservations =
            db.read {
                it.getReservationsForChildInRange(
                    childId,
                    FiniteDateRange(activePlacementStart, activePlacementEnd)
                )
            }
        assertEquals(4, reservations.size)
        assertTrue(reservations.containsKey(firstReservation))
        assertTrue(reservations.containsKey(secondReservation))
        assertTrue(reservations.containsKey(thirdReservation))
        assertTrue(reservations.containsKey(fourthReservation))

        // Modify placement period so that only first and second reservations are in the placements
        // period range
        // -> should delete third and fourth reservation
        val newEndDate = mockClock.today().plusDays(10)
        placementController.updatePlacementById(
            dbInstance(),
            unitSupervisor,
            mockClock,
            activePlacement.id,
            PlacementUpdateRequestBody(activePlacementStart, newEndDate)
        )

        val placements = db.read { r -> r.getPlacementsForChild(childId) }
        assertEquals(2, placements.size)
        val updatedPlacement = placements.find { it.id != testPlacement.id }!!
        assertEquals(updatedPlacement.startDate, activePlacementStart)
        assertEquals(updatedPlacement.endDate, newEndDate)

        // Verify that the future reservations in new placement period has been deleted
        val updatedReservations =
            db.read {
                it.getReservationsForChildInRange(
                    childId,
                    FiniteDateRange(activePlacementStart, activePlacementEnd)
                )
            }
        assertEquals(2, updatedReservations.size)
        assertTrue(updatedReservations.containsKey(firstReservation))
        assertTrue(updatedReservations.containsKey(secondReservation))
    }

    @Test
    fun `Delete placement, should delete future absences that are in range of placement period`() {
        val activePlacementStart = mockClock.today().minusMonths(3)
        val activePlacementEnd = mockClock.today().plusMonths(6)

        val activePlacement =
            createPlacementAndGroupPlacement(
                activePlacementStart,
                activePlacementEnd,
                childId,
                daycareId,
                groupId
            )

        // Create absences
        val firstAbsence = activePlacementStart.plusDays(10)
        val secondAbsence = activePlacementStart.plusDays(11)
        val thirdAbsence = mockClock.today().plusDays(5)
        val fourthAbsence = mockClock.today().plusDays(6)
        db.transaction {
            createReservationsAndAbsences(
                it,
                HelsinkiDateTime.of(activePlacementStart, LocalTime.of(12, 0)),
                unitSupervisor,
                listOf(
                    DailyReservationRequest.Absent(
                        childId = childId,
                        date = firstAbsence,
                    ),
                    DailyReservationRequest.Absent(
                        childId = childId,
                        date = secondAbsence,
                    ),
                    DailyReservationRequest.Absent(
                        childId = childId,
                        date = thirdAbsence,
                    ),
                    DailyReservationRequest.Absent(
                        childId = childId,
                        date = fourthAbsence,
                    ),
                ),
                citizenReservationThresholdHours
            )
        }

        // then 2 absences with correct category types
        val absences =
            getAbsencesOfChildByRange(DateRange(activePlacementStart, activePlacementEnd))
        assertEquals(4, absences.size)
        assertEquals(firstAbsence, absences[0].date)
        assertEquals(secondAbsence, absences[1].date)
        assertEquals(thirdAbsence, absences[2].date)
        assertEquals(fourthAbsence, absences[3].date)
        assertContains(activePlacement.type.absenceCategories(), absences[0].category)
        assertContains(activePlacement.type.absenceCategories(), absences[1].category)
        assertContains(activePlacement.type.absenceCategories(), absences[2].category)
        assertContains(activePlacement.type.absenceCategories(), absences[3].category)

        // Delete placement period, only third and fourth absences are in future and in the
        // placements period range
        // -> should delete third and fourth absences
        placementController.deletePlacement(dbInstance(), admin, mockClock, activePlacement.id)
        assertNull(db.read { r -> r.getPlacement(activePlacement.id) })

        // Verify that the future absences in new placement period has been deleted
        val updatedAbsences =
            getAbsencesOfChildByRange(DateRange(activePlacementStart, activePlacementEnd))
        assertEquals(2, updatedAbsences.size)
        assertEquals(firstAbsence, updatedAbsences[0].date)
        assertEquals(secondAbsence, updatedAbsences[1].date)
        assertContains(activePlacement.type.absenceCategories(), updatedAbsences[0].category)
        assertContains(activePlacement.type.absenceCategories(), updatedAbsences[1].category)
    }

    @Test
    fun `Delete future placement should delete future attendance reservations that are in range of deleted placement period`() {
        val activePlacementStart = mockClock.today().minusMonths(3)
        val activePlacementEnd = mockClock.today().plusMonths(7)

        createPlacementAndGroupPlacement(
            activePlacementStart,
            activePlacementEnd,
            childId,
            daycareId,
            groupId
        )

        val futurePlacementStart = activePlacementEnd.plusDays(1)
        val futurePlacementEnd = futurePlacementStart.plusMonths(1)

        val futurePlacement =
            createPlacementAndGroupPlacement(
                futurePlacementStart,
                futurePlacementEnd,
                childId,
                daycareId,
                groupId
            )

        // Create reservations
        val reservationTime = TimeRange(LocalTime.of(9, 0), LocalTime.of(17, 0))
        val firstReservation = activePlacementEnd
        val secondReservation = futurePlacementStart
        db.transaction {
            createReservationsAndAbsences(
                it,
                HelsinkiDateTime.of(activePlacementStart, LocalTime.of(12, 0)),
                unitSupervisor,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = childId,
                        date = firstReservation,
                        reservation = reservationTime
                    ),
                    DailyReservationRequest.Reservations(
                        childId = childId,
                        date = secondReservation,
                        reservation = reservationTime
                    ),
                ),
                citizenReservationThresholdHours
            )
        }

        val reservations =
            db.read {
                it.getReservationsForChildInRange(
                    childId,
                    FiniteDateRange(activePlacementStart, futurePlacementEnd)
                )
            }
        assertEquals(2, reservations.size)
        assertTrue(reservations.containsKey(firstReservation))
        assertTrue(reservations.containsKey(secondReservation))

        placementController.deletePlacement(dbInstance(), admin, mockClock, futurePlacement.id)
        assertNull(db.read { r -> r.getPlacement(futurePlacement.id) })

        // Verify that the future reservations in new placement period has been deleted
        val updatedReservations =
            db.read {
                it.getReservationsForChildInRange(
                    childId,
                    FiniteDateRange(activePlacementStart, futurePlacementEnd)
                )
            }
        assertEquals(1, updatedReservations.size)
        assertTrue(updatedReservations.containsKey(firstReservation))
    }

    @Test
    fun `Delete placement, should delete future attendance reservations that are in range of placement period`() {
        val activePlacementStart = mockClock.today().minusMonths(3)
        val activePlacementEnd = mockClock.today().plusMonths(6)

        val activePlacement =
            createPlacementAndGroupPlacement(
                activePlacementStart,
                activePlacementEnd,
                childId,
                daycareId,
                groupId
            )

        // Create reservations
        val reservationTime = TimeRange(LocalTime.of(9, 0), LocalTime.of(17, 0))
        val firstReservation = activePlacementStart.plusDays(10)
        val secondReservation = activePlacementStart.plusDays(11)
        val thirdReservation = mockClock.today().plusDays(15)
        val fourthReservation = mockClock.today().plusDays(16)
        db.transaction {
            createReservationsAndAbsences(
                it,
                HelsinkiDateTime.of(activePlacementStart, LocalTime.of(12, 0)),
                unitSupervisor,
                listOf(
                    DailyReservationRequest.Reservations(
                        childId = childId,
                        date = firstReservation,
                        reservation = reservationTime
                    ),
                    DailyReservationRequest.Reservations(
                        childId = childId,
                        date = secondReservation,
                        reservation = reservationTime
                    ),
                    DailyReservationRequest.Reservations(
                        childId = childId,
                        date = thirdReservation,
                        reservation = reservationTime
                    ),
                    DailyReservationRequest.Reservations(
                        childId = childId,
                        date = fourthReservation,
                        reservation = reservationTime
                    ),
                ),
                citizenReservationThresholdHours
            )
        }

        // then 4 reservathions
        val reservations =
            db.read {
                it.getReservationsForChildInRange(
                    childId,
                    FiniteDateRange(activePlacementStart, activePlacementEnd)
                )
            }
        assertEquals(4, reservations.size)
        assertTrue(reservations.containsKey(firstReservation))
        assertTrue(reservations.containsKey(secondReservation))
        assertTrue(reservations.containsKey(thirdReservation))
        assertTrue(reservations.containsKey(fourthReservation))

        // Delete placement period, only third and fourth reservations are in future and in the
        // placements period range
        // -> should delete third and fourth reservation
        placementController.deletePlacement(dbInstance(), admin, mockClock, activePlacement.id)
        assertNull(db.read { r -> r.getPlacement(activePlacement.id) })

        // Verify that the future reservations in new placement period has been deleted
        val updatedReservations =
            db.read {
                it.getReservationsForChildInRange(
                    childId,
                    FiniteDateRange(activePlacementStart, activePlacementEnd)
                )
            }
        assertEquals(2, updatedReservations.size)
        assertTrue(updatedReservations.containsKey(firstReservation))
        assertTrue(updatedReservations.containsKey(secondReservation))
    }

    @Test
    fun `creating group placement works with the full duration`() {
        val (_, res, _) =
            createGroupPlacement(
                testPlacement.id,
                GroupPlacementRequestBody(groupId, placementStart, placementEnd)
            )

        Assertions.assertThat(res.statusCode).isEqualTo(200)
    }

    @Test
    fun `creating group placement throws BadRequest if group placement starts before the daycare placements`() {
        val (_, res, _) =
            createGroupPlacement(
                testPlacement.id,
                GroupPlacementRequestBody(
                    GroupId(UUID.randomUUID()),
                    placementStart.minusDays(1),
                    placementEnd
                )
            )

        Assertions.assertThat(res.statusCode).isEqualTo(400)
    }

    @Test
    fun `creating group placement throws BadRequest if group placement ends after the daycare placements`() {
        val (_, res, _) =
            createGroupPlacement(
                testPlacement.id,
                GroupPlacementRequestBody(
                    GroupId(UUID.randomUUID()),
                    placementStart,
                    placementEnd.plusDays(1)
                )
            )

        Assertions.assertThat(res.statusCode).isEqualTo(400)
    }

    @Test
    fun `deleting group placement works`() {
        val groupPlacementId =
            db.transaction { tx ->
                tx.createGroupPlacement(testPlacement.id, groupId, placementStart, placementEnd)
            }

        val (_, res, _) =
            http.delete("/group-placements/$groupPlacementId").asUser(unitSupervisor).response()

        Assertions.assertThat(res.statusCode).isEqualTo(200)

        val (_, _, result) =
            http
                .get("/placements?daycareId=$daycareId")
                .asUser(unitSupervisor)
                .responseObject<PlacementResponse>(jsonMapper)

        val groupPlacementsAfter = result.get().placements.toList()[0].groupPlacements
        Assertions.assertThat(groupPlacementsAfter).hasSize(1)
        Assertions.assertThat(groupPlacementsAfter.first().groupId).isNull()
    }

    @Test
    fun `unit supervisor sees placements to her unit only`() {
        val allowedId =
            db.transaction { tx ->
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = daycareId,
                        startDate = LocalDate.now(),
                        endDate = LocalDate.now().plusDays(1)
                    )
                )
            }

        val restrictedId =
            db.transaction { tx ->
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = testDaycare2.id,
                        startDate = LocalDate.now().minusDays(2),
                        endDate = LocalDate.now().minusDays(1)
                    )
                )
            }

        val (_, res, result) =
            http
                .get("/placements?childId=$childId")
                .asUser(unitSupervisor)
                .responseObject<PlacementResponse>(jsonMapper)

        org.junit.jupiter.api.Assertions.assertEquals(200, res.statusCode)

        val placements = result.get().placements.toList()
        val allowed = placements.find { it.id == allowedId }!!
        val restricted = placements.find { it.id == restrictedId }!!

        org.junit.jupiter.api.Assertions.assertFalse(allowed.isRestrictedFromUser)
        org.junit.jupiter.api.Assertions.assertTrue(restricted.isRestrictedFromUser)
    }

    @Test
    fun `unit supervisor can modify placements in her daycare only`() {
        val newStart = placementStart.plusDays(1)
        val newEnd = placementEnd.minusDays(2)
        val allowedId = testPlacement.id
        val restrictedId =
            db.transaction { tx ->
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = testDaycare2.id,
                        startDate = placementEnd.plusDays(1),
                        endDate = placementEnd.plusMonths(2)
                    )
                )
            }
        val body = PlacementUpdateRequestBody(startDate = newStart, endDate = newEnd)

        val (_, forbidden, _) =
            http
                .put("/placements/$restrictedId")
                .objectBody(bodyObject = body, mapper = jsonMapper)
                .asUser(unitSupervisor)
                .response()

        org.junit.jupiter.api.Assertions.assertEquals(403, forbidden.statusCode)

        val (_, allowed, _) =
            http
                .put("/placements/$allowedId")
                .objectBody(bodyObject = body, mapper = jsonMapper)
                .asUser(unitSupervisor)
                .response()

        org.junit.jupiter.api.Assertions.assertEquals(200, allowed.statusCode)

        db.read { r ->
            val updated = r.getPlacementsForChild(childId).find { it.id == allowedId }!!
            org.junit.jupiter.api.Assertions.assertEquals(newStart, updated.startDate)
            org.junit.jupiter.api.Assertions.assertEquals(newEnd, updated.endDate)
        }
    }

    @Test
    fun `unit supervisor can't modify placement if it overlaps with another that supervisor doesn't have the rights to`() {
        val newEnd = placementEnd.plusDays(1)
        val allowedId = testPlacement.id
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = childId,
                    unitId = testDaycare2.id,
                    startDate = newEnd,
                    endDate = newEnd.plusMonths(2)
                )
            )
        }
        val body =
            PlacementUpdateRequestBody(
                startDate = placementStart,
                endDate = newEnd
            ) // endDate overlaps with another placement

        val (_, res, _) =
            http
                .put("/placements/$allowedId")
                .objectBody(bodyObject = body, mapper = jsonMapper)
                .asUser(unitSupervisor)
                .response()

        org.junit.jupiter.api.Assertions.assertEquals(409, res.statusCode)
    }

    @Test
    fun `unit supervisor can modify placement if it overlaps with another that supervisor has the rights to`() {
        val newEnd = placementEnd.plusDays(1)
        val secondPlacement =
            db.transaction { tx ->
                tx.insert(
                        DevPlacement(
                            childId = childId,
                            unitId = testDaycare2.id,
                            startDate = newEnd,
                            endDate = newEnd.plusMonths(2)
                        )
                    )
                    .also {
                        tx.updateDaycareAclWithEmployee(
                            testDaycare2.id,
                            unitSupervisor.id,
                            UserRole.UNIT_SUPERVISOR
                        )
                    }
            }

        val body =
            PlacementUpdateRequestBody(
                startDate = placementStart,
                endDate = newEnd
            ) // endDate overlaps with another placement
        val (_, res, _) =
            http
                .put("/placements/${testPlacement.id}")
                .objectBody(bodyObject = body, mapper = jsonMapper)
                .asUser(unitSupervisor)
                .response()

        org.junit.jupiter.api.Assertions.assertEquals(200, res.statusCode)

        val placements = db.read { r -> r.getPlacementsForChild(childId) }
        val first = placements.find { it.id == testPlacement.id }!!
        val second = placements.find { it.id == secondPlacement }!!

        org.junit.jupiter.api.Assertions.assertEquals(newEnd, first.endDate)
        org.junit.jupiter.api.Assertions.assertEquals(newEnd.plusDays(1), second.startDate)
    }

    @Test
    fun `staff can't modify placements`() {
        db.transaction { tx ->
            tx.updateDaycareAclWithEmployee(daycareId, staff.id, UserRole.STAFF)
        }
        val newStart = placementStart.plusDays(1)
        val newEnd = placementEnd.minusDays(2)
        val body = PlacementUpdateRequestBody(startDate = newStart, endDate = newEnd)

        val (_, res, _) =
            http
                .put("/placements/$daycareId")
                .objectBody(bodyObject = body, mapper = jsonMapper)
                .asUser(unitSupervisor)
                .response()

        org.junit.jupiter.api.Assertions.assertEquals(403, res.statusCode)
    }

    @Test
    fun `service worker cannot remove placements`() {
        val groupPlacementId =
            db.transaction { tx ->
                tx.createGroupPlacement(testPlacement.id, groupId, placementStart, placementEnd)
            }

        val (_, res, _) =
            http.delete("/group-placements/$groupPlacementId").asUser(serviceWorker).response()

        Assertions.assertThat(res.statusCode).isEqualTo(403)
    }

    @Test
    fun `moving placement end date moves backup care end date`() {
        db.transaction { tx ->
            tx.insert(
                DevBackupCare(
                    childId = childId,
                    unitId = testDaycare2.id,
                    period = FiniteDateRange(placementEnd.minusDays(5), placementEnd.minusDays(1))
                )
            )
        }

        val newEnd = placementEnd.minusDays(1)

        val body = PlacementUpdateRequestBody(startDate = placementStart, endDate = newEnd)
        val (_, res, _) =
            http
                .put("/placements/${testPlacement.id}")
                .objectBody(bodyObject = body, mapper = jsonMapper)
                .asUser(unitSupervisor)
                .response()

        assertEquals(200, res.statusCode)

        val backupCares = db.transaction { it.getBackupCaresForChild(childId) }
        assertEquals(1, backupCares.size)
        assertEquals(newEnd, backupCares[0].period.end)
    }

    @Test
    fun `moving placement start date moves backup care start date`() {
        db.transaction { tx ->
            tx.insert(
                DevBackupCare(
                    childId = childId,
                    unitId = testDaycare2.id,
                    period = FiniteDateRange(placementStart, placementStart.plusDays(4))
                )
            )
        }

        val newStart = placementStart.plusDays(2)

        val body = PlacementUpdateRequestBody(startDate = newStart, endDate = placementEnd)
        val (_, res, _) =
            http
                .put("/placements/${testPlacement.id}")
                .objectBody(bodyObject = body, mapper = jsonMapper)
                .asUser(unitSupervisor)
                .response()

        assertEquals(200, res.statusCode)

        val backupCares = db.transaction { it.getBackupCaresForChild(childId) }
        assertEquals(1, backupCares.size)
        assertEquals(newStart, backupCares[0].period.start)
    }

    @Test
    fun `can move end date of later of two consecutive placements`() {
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = childId,
                    unitId = daycareId,
                    startDate = placementStart.minusDays(10),
                    endDate = placementStart.minusDays(1)
                )
            )
            tx.insert(
                DevBackupCare(
                    childId = childId,
                    unitId = testDaycare2.id,
                    period =
                        FiniteDateRange(placementStart.minusDays(8), placementStart.plusDays(4))
                )
            )
        }

        val newEnd = placementStart.plusDays(2)

        val body = PlacementUpdateRequestBody(startDate = placementStart, endDate = newEnd)
        val (_, res, _) =
            http
                .put("/placements/${testPlacement.id}")
                .objectBody(bodyObject = body, mapper = jsonMapper)
                .asUser(unitSupervisor)
                .response()

        assertEquals(200, res.statusCode)

        val backupCares = db.transaction { it.getBackupCaresForChild(childId) }
        assertEquals(1, backupCares.size)
        assertEquals(newEnd, backupCares[0].period.end)
    }

    @Test
    fun `deleting placement deletes backup care`() {
        db.transaction { tx ->
            tx.insert(
                DevBackupCare(
                    childId = childId,
                    unitId = testDaycare2.id,
                    period = FiniteDateRange(placementStart.plusDays(1), placementStart.plusDays(5))
                )
            )
        }

        val (_, res, _) =
            http.delete("/placements/${testPlacement.id}").asUser(serviceWorker).response()

        assertEquals(200, res.statusCode)

        val backupCares = db.transaction { it.getBackupCaresForChild(childId) }
        assertEquals(0, backupCares.size)
    }

    private fun createGroupPlacement(
        placementId: PlacementId,
        groupPlacement: GroupPlacementRequestBody
    ): ResponseResultOf<ByteArray> {
        return http
            .post("/placements/$placementId/group-placements")
            .asUser(unitSupervisor)
            .objectBody(bodyObject = groupPlacement, mapper = jsonMapper)
            .response()
    }

    private fun createPlacementAndGroupPlacement(
        placementStartDate: LocalDate,
        placementEndDate: LocalDate,
        childId: ChildId,
        daycareId: DaycareId,
        groupId: GroupId,
        groupPlacementStart: LocalDate = placementStartDate,
        groupPlacementEnd: LocalDate = placementEndDate
    ): DaycarePlacementDetails {
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = childId,
                    unitId = daycareId,
                    startDate = placementStartDate,
                    endDate = placementEndDate
                )
            )
        }
        val placement =
            db.read { r ->
                r.getDaycarePlacements(daycareId, childId, placementStartDate, placementEndDate)
                    .first()
            }
        val groupPlacementId =
            placementController.createGroupPlacement(
                dbInstance(),
                unitSupervisor,
                mockClock,
                placement.id,
                GroupPlacementRequestBody(groupId, groupPlacementStart, groupPlacementEnd)
            )
        assertNotNull(groupPlacementId)

        return placement
    }

    private fun getGroupPlacements(
        childId: ChildId,
        daycareId: DaycareId
    ): List<DaycareGroupPlacement> {
        return http
            .get("/placements?childId=$childId&daycareId=$daycareId")
            .asUser(serviceWorker)
            .responseObject<PlacementResponse>(jsonMapper)
            .third
            .get()
            .placements
            .toList()
            .first { it.id == testPlacement.id }
            .groupPlacements
            .filter { it.id != null }
    }

    private fun getAbsencesOfChildByRange(range: DateRange) =
        db.read {
            it.getAbsencesOfChildByRange(
                    childId,
                    range,
                )
                .sortedWith(compareBy({ it.date }, { it.category }))
        }
}
