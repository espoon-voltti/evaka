// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.resetDatabase
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class PlacementControllerCitizenIntegrationTest : FullApplicationTest() {
    final val child = testChild_1
    final val parent = testAdult_1
    final val authenticatedParent = AuthenticatedUser.Citizen(parent.id)

    final val daycareId = testDaycare.id
    final val testDaycareGroup = DevDaycareGroup(daycareId = daycareId)
    final val groupId = testDaycareGroup.id

    final val today = LocalDate.now()

    final val placementStart = today.minusDays(100)
    final val placementEnd = placementStart.plusDays(200)
    lateinit var testPlacement: DaycarePlacementDetails

    @BeforeEach
    fun setUp() {
        db.transaction { tx ->
            tx.resetDatabase()
            tx.insertGeneralTestFixtures()
            tx.insertTestPlacement(
                childId = child.id,
                unitId = daycareId,
                startDate = placementStart,
                endDate = placementEnd
            )
            tx.insertTestDaycareGroup(testDaycareGroup)
            testPlacement = tx.getDaycarePlacements(daycareId, null, null, null).first()
            tx.insertGuardian(parent.id, child.id)
        }
    }

    @Test
    fun `child placements are returned`() {
        val (_, res, result) = http.get("/citizen/children/${child.id}/placements")
            .asUser(authenticatedParent)
            .responseObject<PlacementControllerCitizen.ChildPlacementResponse>(objectMapper)

        assertEquals(200, res.statusCode)

        val childPlacements = result.get().placements
        assertEquals(1, childPlacements.size)
        assertEquals(placementStart, childPlacements[0].startDate)
        assertEquals(placementEnd, childPlacements[0].endDate)
        assertEquals(PlacementControllerCitizen.TerminatablePlacementType.DAYCARE, childPlacements[0].type)
        assertEquals(1, childPlacements[0].placements.size)
        assertEquals(PlacementType.DAYCARE, childPlacements[0].placements[0].type)
        assertEquals(null, childPlacements[0].placements[0].terminationRequestedDate)
        assertEquals(null, childPlacements[0].placements[0].terminatedBy)
    }

    @Test
    fun `citizen can terminate own child's placement starting from tomorrow`() {
        val placementTerminationDate = today.plusDays(1)

        val (_, postRes, _) = http.post("/citizen/children/${child.id}/placements/terminate")
            .jsonBody(
                objectMapper.writeValueAsString(
                    PlacementControllerCitizen.PlacementTerminationRequestBody(
                        type = PlacementControllerCitizen.TerminatablePlacementType.DAYCARE,
                        terminationDate = placementTerminationDate,
                        unitId = daycareId,
                        terminateDaycareOnly = false,
                    )
                )
            )
            .asUser(authenticatedParent)
            .response()

        assertEquals(200, postRes.statusCode)

        val (_, res, result) = http.get("/citizen/children/${child.id}/placements")
            .asUser(authenticatedParent)
            .responseObject<PlacementControllerCitizen.ChildPlacementResponse>(objectMapper)

        assertEquals(200, res.statusCode)

        val response = result.get()
        val childPlacements = response.placements
        assertEquals(1, childPlacements.size)
        assertEquals(placementStart, childPlacements[0].startDate)
        assertEquals(placementTerminationDate, childPlacements[0].endDate)
        assertEquals(PlacementControllerCitizen.TerminatablePlacementType.DAYCARE, childPlacements[0].type)
        assertEquals(1, childPlacements[0].placements.size)
        assertEquals(today, childPlacements[0].placements[0].terminationRequestedDate)
        assertEquals("${parent.firstName} ${parent.lastName}", childPlacements[0].placements[0].terminatedBy?.name)
    }
}
