// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import com.github.kittinunf.fuel.core.ResponseResultOf
import com.github.kittinunf.fuel.jackson.objectBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.updateDaycareAclWithEmployee
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testDecisionMaker_2
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class PlacementControllerIntegrationTest : FullApplicationTest() {
    final val childId = testChild_1.id
    final val daycareId = testDaycare.id
    final val testDaycareGroup = DevDaycareGroup(daycareId = daycareId)
    final val groupId = testDaycareGroup.id

    final val placementStart = LocalDate.now().plusDays(300)
    final val placementEnd = placementStart.plusDays(200)
    lateinit var testPlacement: DaycarePlacementDetails

    private val unitSupervisor = AuthenticatedUser(testDecisionMaker_1.id, emptySet())
    private val staff = AuthenticatedUser(testDecisionMaker_2.id, emptySet())
    private val serviceWorker = AuthenticatedUser(UUID.randomUUID(), setOf(UserRole.SERVICE_WORKER))

    @BeforeEach
    fun setUp() {
        jdbi.handle { h ->
            resetDatabase(h)
            insertGeneralTestFixtures(h)
            insertTestPlacement(
                h = h,
                childId = childId,
                unitId = daycareId,
                startDate = placementStart,
                endDate = placementEnd
            )
            h.insertTestDaycareGroup(testDaycareGroup)
            testPlacement = h.getDaycarePlacements(daycareId, null, null, null).first()
            updateDaycareAclWithEmployee(h, daycareId, unitSupervisor.id, UserRole.UNIT_SUPERVISOR)
        }
    }

    @Test
    fun `get placements works with daycareId and without dates`() {
        val (_, res, result) = http.get("/placements?daycareId=$daycareId")
            .asUser(serviceWorker)
            .responseObject<Set<DaycarePlacementWithGroups>>(objectMapper)

        Assertions.assertThat(res.statusCode).isEqualTo(200)

        val placements = result.get().toList()
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
        val (_, res, result) = http.get(
            "/placements?daycareId=$daycareId&from=${LocalDate.now()}&to=${
            LocalDate.now().plusDays(900)
            }"
        )
            .asUser(serviceWorker)
            .responseObject<Set<DaycarePlacementWithGroups>>(objectMapper)

        Assertions.assertThat(res.statusCode).isEqualTo(200)
        Assertions.assertThat(result.get()).hasSize(1)
    }

    @Test
    fun `get placements works with with daycareId and non-matching dates`() {
        val (_, res, result) = http.get(
            "/placements?daycareId=$daycareId&from=${
            LocalDate.now().minusDays(900)
            }&to=${LocalDate.now().minusDays(300)}"
        )
            .asUser(serviceWorker)
            .responseObject<Set<DaycarePlacementWithGroups>>(objectMapper)

        Assertions.assertThat(res.statusCode).isEqualTo(200)
        Assertions.assertThat(result.get()).hasSize(0)
    }

    @Test
    fun `get placements returns an empty list if daycare is not found`() {
        val (_, res, result) = http.get("/placements?daycareId=${UUID.randomUUID()}")
            .asUser(serviceWorker)
            .responseObject<Set<DaycarePlacementWithGroups>>(objectMapper)

        Assertions.assertThat(res.statusCode).isEqualTo(200)
        Assertions.assertThat(result.get()).isEqualTo(setOf<DaycarePlacementWithGroups>())
    }

    @Test
    fun `get placements works with childId and without dates`() {
        val (_, res, result) = http.get("/placements?childId=$childId")
            .asUser(serviceWorker)
            .responseObject<Set<DaycarePlacementWithGroups>>(objectMapper)

        Assertions.assertThat(res.statusCode).isEqualTo(200)

        val placements = result.get().toList()
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
        val (_, res, result) = http.get(
            "/placements?childId=$childId&from=${LocalDate.now()}&to=${
            LocalDate.now().plusDays(900)
            }"
        )
            .asUser(serviceWorker)
            .responseObject<Set<DaycarePlacementWithGroups>>(objectMapper)

        Assertions.assertThat(res.statusCode).isEqualTo(200)
        Assertions.assertThat(result.get()).hasSize(1)
    }

    @Test
    fun `get placements works with with childId and non-matching dates`() {
        val (_, res, result) = http.get(
            "/placements?childId=$childId&from=${
            LocalDate.now().minusDays(900)
            }&to=${LocalDate.now().minusDays(300)}"
        )
            .asUser(serviceWorker)
            .responseObject<Set<DaycarePlacementWithGroups>>(objectMapper)

        Assertions.assertThat(res.statusCode).isEqualTo(200)
        Assertions.assertThat(result.get()).hasSize(0)
    }

    @Test
    fun `get placements throws BadRequest if daycare id and child id is not given`() {
        val (_, res, _) = http.get("/placements")
            .asUser(serviceWorker)
            .response()

        Assertions.assertThat(res.statusCode).isEqualTo(400)
    }

    @Test
    fun `creating group placement works with valid data`() {
        val groupPlacementStart = placementStart.plusDays(1)
        val groupPlacementEnd = placementEnd.minusDays(1)
        val (_, res, _) = createGroupPlacement(
            testPlacement.id,
            GroupPlacementRequestBody(groupId, groupPlacementStart, groupPlacementEnd)
        )
        Assertions.assertThat(res.statusCode).isEqualTo(201)

        val groupPlacements = getGroupPlacements(childId, daycareId)
        Assertions.assertThat(groupPlacements.size).isEqualTo(1)
        Assertions.assertThat(groupPlacements.first().daycarePlacementId).isEqualTo(testPlacement.id)
        Assertions.assertThat(groupPlacements.first().groupId).isEqualTo(groupId)
        Assertions.assertThat(groupPlacements.first().startDate).isEqualTo(groupPlacementStart)
        Assertions.assertThat(groupPlacements.first().endDate).isEqualTo(groupPlacementEnd)
    }

    @Test
    fun `creating group placement right after another merges them`() {
        createGroupPlacement(
            testPlacement.id,
            GroupPlacementRequestBody(
                groupId,
                placementStart,
                placementStart.plusDays(3)
            )
        )
        createGroupPlacement(
            testPlacement.id,
            GroupPlacementRequestBody(
                groupId,
                placementStart.plusDays(4),
                placementEnd
            )
        )

        val groupPlacements = getGroupPlacements(childId, daycareId)
        Assertions.assertThat(groupPlacements.size).isEqualTo(1)
        Assertions.assertThat(groupPlacements.first().daycarePlacementId).isEqualTo(testPlacement.id)
        Assertions.assertThat(groupPlacements.first().groupId).isEqualTo(groupId)
        Assertions.assertThat(groupPlacements.first().startDate).isEqualTo(placementStart)
        Assertions.assertThat(groupPlacements.first().endDate).isEqualTo(placementEnd)
    }

    @Test
    fun `creating group placement right before another merges them`() {
        createGroupPlacement(
            testPlacement.id,
            GroupPlacementRequestBody(
                groupId,
                placementEnd.minusDays(3),
                placementEnd
            )
        )
        createGroupPlacement(
            testPlacement.id,
            GroupPlacementRequestBody(
                groupId,
                placementStart,
                placementEnd.minusDays(4)
            )
        )

        val groupPlacements = getGroupPlacements(childId, daycareId)
        Assertions.assertThat(groupPlacements.size).isEqualTo(1)
        Assertions.assertThat(groupPlacements.first().daycarePlacementId).isEqualTo(testPlacement.id)
        Assertions.assertThat(groupPlacements.first().groupId).isEqualTo(groupId)
        Assertions.assertThat(groupPlacements.first().startDate).isEqualTo(placementStart)
        Assertions.assertThat(groupPlacements.first().endDate).isEqualTo(placementEnd)
    }

    @Test
    fun `creating group placement between two merges them`() {
        createGroupPlacement(
            testPlacement.id,
            GroupPlacementRequestBody(
                groupId,
                placementStart,
                placementStart.plusDays(2)
            )
        )
        createGroupPlacement(
            testPlacement.id,
            GroupPlacementRequestBody(
                groupId,
                placementEnd.minusDays(2),
                placementEnd
            )
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
        Assertions.assertThat(groupPlacements.first().daycarePlacementId).isEqualTo(testPlacement.id)
        Assertions.assertThat(groupPlacements.first().groupId).isEqualTo(groupId)
        Assertions.assertThat(groupPlacements.first().startDate).isEqualTo(placementStart)
        Assertions.assertThat(groupPlacements.first().endDate).isEqualTo(placementEnd)
    }

    @Test
    fun `group placements are not merged if they have gap between`() {
        createGroupPlacement(
            testPlacement.id,
            GroupPlacementRequestBody(
                groupId,
                placementStart,
                placementStart.plusDays(2)
            )
        )
        createGroupPlacement(
            testPlacement.id,
            GroupPlacementRequestBody(
                groupId,
                placementEnd.minusDays(2),
                placementEnd
            )
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
        val (_, res, _) = createGroupPlacement(
            testPlacement.id,
            GroupPlacementRequestBody(
                UUID.randomUUID(),
                placementStart,
                placementEnd
            )
        )

        Assertions.assertThat(res.statusCode).isEqualTo(404)
    }

    @Test
    fun `creating group placement works with the full duration`() {
        val (_, res, _) = createGroupPlacement(
            testPlacement.id,
            GroupPlacementRequestBody(
                groupId,
                placementStart,
                placementEnd
            )
        )

        Assertions.assertThat(res.statusCode).isEqualTo(201)
    }

    @Test
    fun `creating group placement throws BadRequest if group placement starts before the daycare placements`() {
        val (_, res, _) = createGroupPlacement(
            testPlacement.id,
            GroupPlacementRequestBody(
                UUID.randomUUID(),
                placementStart.minusDays(1),
                placementEnd
            )
        )

        Assertions.assertThat(res.statusCode).isEqualTo(400)
    }

    @Test
    fun `creating group placement throws BadRequest if group placement ends after the daycare placements`() {
        val (_, res, _) = createGroupPlacement(
            testPlacement.id,
            GroupPlacementRequestBody(
                UUID.randomUUID(),
                placementStart,
                placementEnd.plusDays(1)
            )
        )

        Assertions.assertThat(res.statusCode).isEqualTo(400)
    }

    @Test
    fun `deleting group placement works`() {
        jdbi.handle { h ->
            val groupPlacementId =
                h.createGroupPlacement(testPlacement.id, groupId, placementStart, placementEnd).id!!

            val (_, res, _) = http.delete("/placements/${testPlacement.id}/group-placements/$groupPlacementId")
                .asUser(unitSupervisor)
                .response()

            Assertions.assertThat(res.statusCode).isEqualTo(204)

            val (_, _, result) = http.get("/placements?daycareId=$daycareId")
                .asUser(unitSupervisor)
                .responseObject<Set<DaycarePlacementWithGroups>>(objectMapper)

            val groupPlacementsAfter = result.get().toList()[0].groupPlacements
            Assertions.assertThat(groupPlacementsAfter).hasSize(1)
            Assertions.assertThat(groupPlacementsAfter.first().groupId).isNull()
        }
    }

    @Test
    fun `unit supervisor sees placements to her unit only`() {
        jdbi.handle { h ->
            val restrictedId = insertTestPlacement(
                h = h,
                childId = childId,
                unitId = testDaycare2.id,
                startDate = LocalDate.now().plusMonths(1).plusDays(1),
                endDate = LocalDate.now().plusMonths(2)
            )

            val (_, res, result) = http.get("/placements?childId=$childId")
                .asUser(unitSupervisor)
                .responseObject<Set<DaycarePlacementWithGroups>>(objectMapper)

            org.junit.jupiter.api.Assertions.assertEquals(200, res.statusCode)

            val placements = result.get().toList()
            val allowed = placements.find { it.id == testPlacement.id }!!
            val restricted = placements.find { it.id == restrictedId }!!

            org.junit.jupiter.api.Assertions.assertEquals(2, placements.size)
            org.junit.jupiter.api.Assertions.assertFalse(allowed.isRestrictedFromUser)
            org.junit.jupiter.api.Assertions.assertTrue(restricted.isRestrictedFromUser)
        }
    }

    @Test
    fun `unit supervisor can modify placements in her daycare only`() {
        jdbi.handle { h ->
            val newStart = placementStart.plusDays(1)
            val newEnd = placementEnd.minusDays(2)
            val allowedId = testPlacement.id
            val restrictedId = insertTestPlacement(
                h = h,
                childId = childId,
                unitId = testDaycare2.id,
                startDate = placementEnd.plusDays(1),
                endDate = placementEnd.plusMonths(2)
            )
            val body = PlacementUpdateRequestBody(startDate = newStart, endDate = newEnd)

            val (_, forbidden, _) = http.put("/placements/$restrictedId")
                .objectBody(bodyObject = body, mapper = objectMapper)
                .asUser(unitSupervisor)
                .response()

            org.junit.jupiter.api.Assertions.assertEquals(403, forbidden.statusCode)

            val (_, allowed, _) = http.put("/placements/$allowedId")
                .objectBody(bodyObject = body, mapper = objectMapper)
                .asUser(unitSupervisor)
                .response()

            org.junit.jupiter.api.Assertions.assertEquals(204, allowed.statusCode)

            val updated = h.getPlacementsForChild(childId).find { it.id == allowedId }!!
            org.junit.jupiter.api.Assertions.assertEquals(newStart, updated.startDate)
            org.junit.jupiter.api.Assertions.assertEquals(newEnd, updated.endDate)
        }
    }

    @Test
    fun `unit supervisor can't modify placement if it overlaps with another that supervisor doesn't have the rights to`() {
        jdbi.handle { h ->
            val newEnd = placementEnd.plusDays(1)
            val allowedId = testPlacement.id
            insertTestPlacement(
                h = h,
                childId = childId,
                unitId = testDaycare2.id,
                startDate = newEnd,
                endDate = newEnd.plusMonths(2)
            )
            val body = PlacementUpdateRequestBody(
                startDate = placementStart,
                endDate = newEnd
            ) // endDate overlaps with another placement

            val (_, res, _) = http.put("/placements/$allowedId")
                .objectBody(bodyObject = body, mapper = objectMapper)
                .asUser(unitSupervisor)
                .response()

            org.junit.jupiter.api.Assertions.assertEquals(409, res.statusCode)
        }
    }

    @Test
    fun `unit supervisor can modify placement if it overlaps with another that supervisor has the rights to`() {
        jdbi.handle { h ->
            val newEnd = placementEnd.plusDays(1)
            val secondPlacement = insertTestPlacement(
                h = h,
                childId = childId,
                unitId = testDaycare2.id,
                startDate = newEnd,
                endDate = newEnd.plusMonths(2)
            )
            updateDaycareAclWithEmployee(h, testDaycare2.id, unitSupervisor.id, UserRole.UNIT_SUPERVISOR)
            val body = PlacementUpdateRequestBody(
                startDate = placementStart,
                endDate = newEnd
            ) // endDate overlaps with another placement

            val (_, res, _) = http.put("/placements/${testPlacement.id}")
                .objectBody(bodyObject = body, mapper = objectMapper)
                .asUser(unitSupervisor)
                .response()

            org.junit.jupiter.api.Assertions.assertEquals(204, res.statusCode)

            val placements = h.getPlacementsForChild(childId)
            val first = placements.find { it.id == testPlacement.id }!!
            val second = placements.find { it.id == secondPlacement }!!

            org.junit.jupiter.api.Assertions.assertEquals(newEnd, first.endDate)
            org.junit.jupiter.api.Assertions.assertEquals(newEnd.plusDays(1), second.startDate)
        }
    }

    @Test
    fun `staff can't modify placements`() {
        jdbi.handle { h ->
            updateDaycareAclWithEmployee(h, daycareId, staff.id, UserRole.STAFF)
            val newStart = placementStart.plusDays(1)
            val newEnd = placementEnd.minusDays(2)
            val body = PlacementUpdateRequestBody(startDate = newStart, endDate = newEnd)

            val (_, res, _) = http.put("/placements/$daycareId")
                .objectBody(bodyObject = body, mapper = objectMapper)
                .asUser(unitSupervisor)
                .response()

            org.junit.jupiter.api.Assertions.assertEquals(403, res.statusCode)
        }
    }

    @Test
    fun `service worker cannot remove placements`() {
        jdbi.handle { h ->
            val groupPlacementId =
                h.createGroupPlacement(testPlacement.id, groupId, placementStart, placementEnd).id!!

            val (_, res, _) = http.delete("/placements/${testPlacement.id}/group-placements/$groupPlacementId")
                .asUser(serviceWorker)
                .response()

            Assertions.assertThat(res.statusCode).isEqualTo(403)
        }
    }

    private fun createGroupPlacement(
        placementId: UUID,
        groupPlacement: GroupPlacementRequestBody
    ): ResponseResultOf<ByteArray> {
        return http.post("/placements/$placementId/group-placements")
            .asUser(unitSupervisor)
            .objectBody(bodyObject = groupPlacement, mapper = objectMapper)
            .response()
    }

    private fun getGroupPlacements(childId: UUID, daycareId: UUID): List<DaycareGroupPlacement> {
        return http.get("/placements?childId=$childId&daycareId=$daycareId")
            .asUser(serviceWorker)
            .responseObject<Set<DaycarePlacementWithGroups>>(objectMapper)
            .third
            .get()
            .toList()
            .first { it.id == testPlacement.id }
            .groupPlacements
            .filter { it.id != null }
    }
}
