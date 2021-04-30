// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.DevAssistanceNeed
import fi.espoo.evaka.shared.dev.insertTestAssistanceNeed
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDecisionMaker_1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class AssistanceNeedIntegrationTest : FullApplicationTest() {
    private val assistanceWorker = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))

    @BeforeEach
    private fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            tx.insertGeneralTestFixtures()
        }
    }

    @Test
    fun `post first assistance need, no bases`() {
        val assistanceNeed = whenPostAssistanceNeedThenExpectSuccess(
            AssistanceNeedRequest(
                startDate = testDate(10),
                endDate = testDate(20),
                capacityFactor = 1.5
            )
        )

        assertEquals(
            AssistanceNeed(
                id = assistanceNeed.id,
                childId = testChild_1.id,
                startDate = testDate(10),
                endDate = testDate(20),
                capacityFactor = 1.5,
                description = "",
                bases = emptySet(),
                otherBasis = ""
            ),
            assistanceNeed
        )
    }

    @Test
    fun `post first assistance need, with bases`() {
        val allBases = setOf(
            AssistanceBasis.AUTISM,
            AssistanceBasis.DEVELOPMENTAL_DISABILITY_1,
            AssistanceBasis.DEVELOPMENTAL_DISABILITY_2,
            AssistanceBasis.FOCUS_CHALLENGE,
            AssistanceBasis.LINGUISTIC_CHALLENGE,
            AssistanceBasis.DEVELOPMENT_MONITORING,
            AssistanceBasis.DEVELOPMENT_MONITORING_PENDING,
            AssistanceBasis.MULTI_DISABILITY,
            AssistanceBasis.LONG_TERM_CONDITION,
            AssistanceBasis.REGULATION_SKILL_CHALLENGE,
            AssistanceBasis.DISABILITY,
            AssistanceBasis.OTHER
        )

        val assistanceNeed = whenPostAssistanceNeedThenExpectSuccess(
            AssistanceNeedRequest(
                startDate = testDate(10),
                endDate = testDate(20),
                capacityFactor = 1.5,
                bases = allBases,
                otherBasis = "foo"
            )
        )

        assertEquals(
            AssistanceNeed(
                id = assistanceNeed.id,
                childId = testChild_1.id,
                startDate = testDate(10),
                endDate = testDate(20),
                capacityFactor = 1.5,
                description = "",
                bases = allBases,
                otherBasis = "foo"
            ),
            assistanceNeed
        )
    }

    @Test
    fun `post assistance need, no overlap`() {
        givenAssistanceNeed(1, 15)
        whenPostAssistanceNeedThenExpectSuccess(
            AssistanceNeedRequest(
                startDate = testDate(16),
                endDate = testDate(30),
                capacityFactor = 1.5
            )
        )

        val assistanceNeeds = db.transaction { tx -> getAssistanceNeedsByChild(tx.handle, testChild_1.id) }
        assertEquals(2, assistanceNeeds.size)
        assertTrue(assistanceNeeds.any { it.startDate == testDate(1) && it.endDate == testDate(15) })
        assertTrue(assistanceNeeds.any { it.startDate == testDate(16) && it.endDate == testDate(30) })
    }

    @Test
    fun `post assistance need, fully encloses previous - responds 409`() {
        givenAssistanceNeed(10, 20)
        whenPostAssistanceNeedThenExpectError(
            AssistanceNeedRequest(
                startDate = testDate(1),
                endDate = testDate(30),
                capacityFactor = 1.5
            ),
            409
        )
    }

    @Test
    fun `post assistance need, starts on same day, ends later - responds 409`() {
        givenAssistanceNeed(10, 20)
        whenPostAssistanceNeedThenExpectError(
            AssistanceNeedRequest(
                startDate = testDate(10),
                endDate = testDate(25),
                capacityFactor = 1.5
            ),
            409
        )
    }

    @Test
    fun `post assistance need, overlaps start of previous - responds 409`() {
        givenAssistanceNeed(10, 20)
        whenPostAssistanceNeedThenExpectError(
            AssistanceNeedRequest(
                startDate = testDate(1),
                endDate = testDate(10),
                capacityFactor = 1.5
            ),
            409
        )
    }

    @Test
    fun `post assistance need, overlaps end of previous - previous gets shortened`() {
        givenAssistanceNeed(10, 20)
        whenPostAssistanceNeedThenExpectSuccess(
            AssistanceNeedRequest(
                startDate = testDate(20),
                endDate = testDate(30),
                capacityFactor = 1.5
            )
        )

        val assistanceNeeds = db.transaction { tx -> getAssistanceNeedsByChild(tx.handle, testChild_1.id) }
        assertEquals(2, assistanceNeeds.size)
        assertTrue(assistanceNeeds.any { it.startDate == testDate(10) && it.endDate == testDate(19) })
        assertTrue(assistanceNeeds.any { it.startDate == testDate(20) && it.endDate == testDate(30) })
    }

    @Test
    fun `post assistance need, is within previous - previous gets shortened`() {
        givenAssistanceNeed(10, 20)
        whenPostAssistanceNeedThenExpectSuccess(
            AssistanceNeedRequest(
                startDate = testDate(11),
                endDate = testDate(15),
                capacityFactor = 1.5
            )
        )

        val assistanceNeeds = db.transaction { tx -> getAssistanceNeedsByChild(tx.handle, testChild_1.id) }
        assertEquals(2, assistanceNeeds.size)
        assertTrue(assistanceNeeds.any { it.startDate == testDate(10) && it.endDate == testDate(10) })
        assertTrue(assistanceNeeds.any { it.startDate == testDate(11) && it.endDate == testDate(15) })
    }

    @Test
    fun `get assistance needs`() {
        givenAssistanceNeed(1, 5, testChild_1.id)
        givenAssistanceNeed(25, 30, testChild_1.id)
        givenAssistanceNeed(25, 30, testChild_2.id)

        val assistanceNeeds = whenGetAssistanceNeedsThenExpectSuccess(testChild_1.id)
        assertEquals(2, assistanceNeeds.size)
        with(assistanceNeeds[0]) {
            assertEquals(testChild_1.id, childId)
            assertEquals(testDate(25), startDate)
            assertEquals(testDate(30), endDate)
        }
        with(assistanceNeeds[1]) {
            assertEquals(testChild_1.id, childId)
            assertEquals(testDate(1), startDate)
            assertEquals(testDate(5), endDate)
        }
    }

    @Test
    fun `update assistance need`() {
        val id1 = givenAssistanceNeed(1, 5)
        val id2 = givenAssistanceNeed(10, 20)
        val updated = whenPutAssistanceNeedThenExpectSuccess(
            id2,
            AssistanceNeedRequest(
                startDate = testDate(9),
                endDate = testDate(22),
                capacityFactor = 1.5
            )
        )

        assertEquals(id2, updated.id)
        assertEquals(testDate(9), updated.startDate)
        assertEquals(testDate(22), updated.endDate)
        assertEquals(1.5, updated.capacityFactor)

        val fetched = whenGetAssistanceNeedsThenExpectSuccess(testChild_1.id)
        assertEquals(2, fetched.size)
        assertTrue(fetched.any { it.id == id1 && it.startDate == testDate(1) && it.endDate == testDate(5) })
        assertTrue(fetched.any { it.id == id2 && it.startDate == testDate(9) && it.endDate == testDate(22) })
    }

    @Test
    fun `update assistance need, not found responds 404`() {
        whenPutAssistanceNeedThenExpectError(
            UUID.randomUUID(),
            AssistanceNeedRequest(
                startDate = testDate(9),
                endDate = testDate(22),
                capacityFactor = 20.0
            ),
            404
        )
    }

    @Test
    fun `update assistance need, conflict returns 409`() {
        givenAssistanceNeed(1, 5)
        val id2 = givenAssistanceNeed(10, 20)

        whenPutAssistanceNeedThenExpectError(
            id2,
            AssistanceNeedRequest(
                startDate = testDate(5),
                endDate = testDate(22),
                capacityFactor = 20.0
            ),
            409
        )
    }

    @Test
    fun `delete assistance need`() {
        val id1 = givenAssistanceNeed(1, 5)
        val id2 = givenAssistanceNeed(10, 20)

        whenDeleteAssistanceNeedThenExpectSuccess(id2)

        val assistanceNeeds = whenGetAssistanceNeedsThenExpectSuccess(testChild_1.id)
        assertEquals(1, assistanceNeeds.size)
        assertEquals(id1, assistanceNeeds.first().id)
    }

    @Test
    fun `delete assistance need, not found responds 404`() {
        whenDeleteAssistanceNeedThenExpectError(UUID.randomUUID(), 404)
    }

    private fun testDate(day: Int) = LocalDate.of(2000, 1, day)

    private fun givenAssistanceNeed(start: Int, end: Int, childId: UUID = testChild_1.id): UUID {
        return db.transaction {
            it.handle.insertTestAssistanceNeed(
                DevAssistanceNeed(
                    childId = childId,
                    startDate = testDate(start),
                    endDate = testDate(end),
                    updatedBy = assistanceWorker.id
                )
            )
        }
    }

    private fun whenPostAssistanceNeedThenExpectSuccess(request: AssistanceNeedRequest): AssistanceNeed {
        val (_, res, result) = http.post("/children/${testChild_1.id}/assistance-needs")
            .jsonBody(objectMapper.writeValueAsString(request))
            .asUser(assistanceWorker)
            .responseObject<AssistanceNeed>(objectMapper)

        assertEquals(201, res.statusCode)
        return result.get()
    }

    private fun whenPostAssistanceNeedThenExpectError(request: AssistanceNeedRequest, status: Int) {
        val (_, res, _) = http.post("/children/${testChild_1.id}/assistance-needs")
            .jsonBody(objectMapper.writeValueAsString(request))
            .asUser(assistanceWorker)
            .response()

        assertEquals(status, res.statusCode)
    }

    private fun whenGetAssistanceNeedsThenExpectSuccess(childId: UUID): List<AssistanceNeed> {
        val (_, res, result) = http.get("/children/$childId/assistance-needs")
            .asUser(assistanceWorker)
            .responseObject<List<AssistanceNeed>>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun whenPutAssistanceNeedThenExpectSuccess(id: UUID, request: AssistanceNeedRequest): AssistanceNeed {
        val (_, res, result) = http.put("/assistance-needs/$id")
            .jsonBody(objectMapper.writeValueAsString(request))
            .asUser(assistanceWorker)
            .responseObject<AssistanceNeed>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun whenPutAssistanceNeedThenExpectError(id: UUID, request: AssistanceNeedRequest, status: Int) {
        val (_, res, _) = http.put("/assistance-needs/$id")
            .jsonBody(objectMapper.writeValueAsString(request))
            .asUser(assistanceWorker)
            .response()

        assertEquals(status, res.statusCode)
    }

    private fun whenDeleteAssistanceNeedThenExpectSuccess(id: UUID) {
        val (_, res, _) = http.delete("/assistance-needs/$id")
            .asUser(assistanceWorker)
            .response()

        assertEquals(204, res.statusCode)
    }

    private fun whenDeleteAssistanceNeedThenExpectError(id: UUID, status: Int) {
        val (_, res, _) = http.delete("/assistance-needs/$id")
            .asUser(assistanceWorker)
            .response()

        assertEquals(status, res.statusCode)
    }
}
