// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing

import com.github.kittinunf.fuel.jackson.objectBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionSummary
import fi.espoo.evaka.invoicing.service.VoucherValueDecisionService
import fi.espoo.evaka.placement.DaycarePlacementWithDetails
import fi.espoo.evaka.placement.Placement
import fi.espoo.evaka.placement.PlacementCreateRequestBody
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.PlacementUpdateRequestBody
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.insertTestParentship
import fi.espoo.evaka.shared.dev.insertTestPartnership
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testVoucherDaycare
import fi.espoo.evaka.testVoucherDaycare2
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class VoucherValueDecisionIntegrationTest : FullApplicationTest() {
    @Autowired
    lateinit var asyncJobRunner: AsyncJobRunner

    @Autowired
    lateinit var voucherValueDecisionService: VoucherValueDecisionService

    @BeforeEach
    fun beforeEach() {
        db.transaction {
            it.resetDatabase()
            it.insertGeneralTestFixtures()
            it.insertTestParentship(
                headOfChild = testAdult_1.id,
                childId = testChild_1.id,
                startDate = testChild_1.dateOfBirth,
                endDate = testChild_1.dateOfBirth.plusYears(18).minusDays(1)
            )
            it.insertTestPartnership(
                adult1 = testAdult_1.id,
                adult2 = testAdult_2.id,
                endDate = LocalDate.of(9999, 12, 31)
            )
        }
    }

    private val startDate = LocalDate.now().minusMonths(1)
    private val endDate = LocalDate.now().plusMonths(6)

    @Test
    fun `value decision is created after a placement is created`() {
        createPlacement(startDate, endDate)

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.DRAFT, decisions.first().status)
        }
    }

    @Test
    fun `value decision is created and it can be sent after a placement is created`() {
        createPlacement(startDate, endDate)
        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.SENT, decisions.first().status)
        }
    }

    @Test
    fun `sent value decision validity period ends automatically when corresponding placement has its future end date lowered`() {
        val placementId = createPlacement(startDate, endDate)
        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.SENT, decisions.first().status)
            assertEquals(endDate, decisions.first().validTo)
        }

        val newEndDate = endDate.minusDays(7)
        updatePlacement(placementId, startDate, newEndDate)

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.SENT, decisions.first().status)
            assertEquals(newEndDate, decisions.first().validTo)
        }
    }

    @Test
    fun `sent value decision validity period ends automatically when corresponding placement has its future end date increased`() {
        val placementId = createPlacement(startDate, endDate)
        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.SENT, decisions.first().status)
            assertEquals(endDate, decisions.first().validTo)
        }

        val newEndDate = endDate.plusDays(7)
        updatePlacement(placementId, startDate, newEndDate)

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.SENT, decisions.first().status)
            assertEquals(newEndDate, decisions.first().validTo)
        }
    }

    @Test
    fun `sent value decision validity period ends after cleanup when corresponding placement has its past end date updated`() {
        val placementId = createPlacement(startDate, endDate)
        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.SENT, decisions.first().status)
            assertEquals(endDate, decisions.first().validTo)
        }

        val newEndDate = LocalDate.now().minusDays(1)
        updatePlacement(placementId, startDate, newEndDate)

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.SENT, decisions.first().status)
            assertEquals(endDate, decisions.first().validTo)
        }

        endDecisions(now = newEndDate.plusDays(1))
        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.SENT, decisions.first().status)
            assertEquals(newEndDate, decisions.first().validTo)
        }
    }

    @Test
    fun `value decision cleanup does nothing when child is placed into another voucher unit`() {
        createPlacement(startDate, endDate)
        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.SENT, decisions.first().status)
            assertEquals(endDate, decisions.first().validTo)
        }

        val newStartDate = LocalDate.now().minusDays(1)
        createPlacement(newStartDate, endDate, testVoucherDaycare2.id)
        endDecisions(now = newStartDate.plusDays(1))

        getAllValueDecisions().let { decisions ->
            assertEquals(2, decisions.size)
            decisions
                .find { it.status == VoucherValueDecisionStatus.SENT }!!
                .let {
                    assertEquals(startDate, it.validFrom)
                    assertEquals(endDate, it.validTo)
                }
        }

        sendAllValueDecisions()
        getAllValueDecisions().sortedBy { it.validFrom }.let { decisions ->
            assertEquals(2, decisions.size)

            assertEquals(VoucherValueDecisionStatus.SENT, decisions.first().status)
            assertEquals(startDate, decisions.first().validFrom)
            assertEquals(newStartDate.minusDays(1), decisions.first().validTo)

            assertEquals(VoucherValueDecisionStatus.SENT, decisions.last().status)
            assertEquals(newStartDate, decisions.last().validFrom)
            assertEquals(endDate, decisions.last().validTo)
        }
    }

    @Test
    fun `value decision cleanup works when child is placed into another municipal unit`() {
        createPlacement(startDate, endDate)
        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.SENT, decisions.first().status)
            assertEquals(endDate, decisions.first().validTo)
        }

        val newStartDate = LocalDate.now().minusDays(1)
        createPlacement(newStartDate, endDate, testDaycare.id)
        endDecisions(now = newStartDate.plusDays(1))

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(startDate, decisions.first().validFrom)
            assertEquals(newStartDate.minusDays(1), decisions.first().validTo)
        }
    }

    @Test
    fun `value decision search`() {
        createPlacement(startDate, endDate)

        assertEquals(1, searchValueDecisions(status = "DRAFT").total)
        assertEquals(1, searchValueDecisions(status = "DRAFT", searchTerms = "Ricky").total) // child
        assertEquals(1, searchValueDecisions(status = "DRAFT", searchTerms = "John").total) // head
        assertEquals(1, searchValueDecisions(status = "DRAFT", searchTerms = "Joan").total) // partner
        assertEquals(0, searchValueDecisions(status = "DRAFT", searchTerms = "Foobar").total) // no match
        assertEquals(0, searchValueDecisions(status = "SENT").total)

        sendAllValueDecisions()
        assertEquals(0, searchValueDecisions(status = "DRAFT").total)
        assertEquals(1, searchValueDecisions(status = "SENT").total)
        assertEquals(1, searchValueDecisions(status = "SENT", searchTerms = "Ricky").total)
        assertEquals(0, searchValueDecisions(status = "SENT", searchTerms = "Foobar").total)
    }

    private val serviceWorker = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))
    private val financeWorker = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.FINANCE_ADMIN))

    private fun createPlacement(
        startDate: LocalDate,
        endDate: LocalDate,
        unitId: DaycareId = testVoucherDaycare.id
    ): PlacementId {
        val body = PlacementCreateRequestBody(
            type = PlacementType.DAYCARE,
            childId = testChild_1.id,
            unitId = unitId,
            startDate = startDate,
            endDate = endDate
        )

        http.post("/placements")
            .asUser(serviceWorker)
            .objectBody(body, mapper = objectMapper)
            .response()
            .also { (_, res, _) ->
                assertEquals(204, res.statusCode)
            }

        val (_, _, data) = http.get("/placements", listOf("childId" to testChild_1.id))
            .asUser(serviceWorker)
            .responseObject<List<DaycarePlacementWithDetails>>(objectMapper)

        asyncJobRunner.runPendingJobsSync()

        return data.get().first().id
    }

    private fun updatePlacement(id: PlacementId, startDate: LocalDate, endDate: LocalDate) {
        val body = PlacementUpdateRequestBody(
            startDate = startDate,
            endDate = endDate
        )

        http.put("/placements/$id")
            .asUser(serviceWorker)
            .objectBody(body, mapper = objectMapper)
            .responseObject<Placement>(objectMapper)
            .also { (_, res, _) ->
                assertEquals(204, res.statusCode)
            }

        asyncJobRunner.runPendingJobsSync()
    }

    private fun searchValueDecisions(status: String, searchTerms: String = ""): Paged<VoucherValueDecisionSummary> {
        val searchParams = listOf("page" to 0, "pageSize" to 100, "status" to status, "searchTerms" to searchTerms)
        val (_, _, data) = http.get("/value-decisions/search", searchParams)
            .asUser(financeWorker)
            .responseObject<Paged<VoucherValueDecisionSummary>>(objectMapper)
        return data.get()
    }

    private fun sendAllValueDecisions() {
        val searchParams = listOf("page" to 0, "pageSize" to 100, "status" to "DRAFT")
        val (_, _, data) = http.get("/value-decisions/search", searchParams)
            .asUser(financeWorker)
            .responseObject<Paged<VoucherValueDecisionSummary>>(objectMapper)
            .also { (_, res, _) ->
                assertEquals(200, res.statusCode)
            }

        val decisionIds = data.get().data.map { it.id }
        http.post("/value-decisions/send")
            .objectBody(decisionIds, mapper = objectMapper)
            .asUser(financeWorker)
            .response()
            .also { (_, res, _) ->
                assertEquals(204, res.statusCode)
            }

        asyncJobRunner.runPendingJobsSync()
    }

    private fun getAllValueDecisions(): List<VoucherValueDecision> {
        return db.read {
            it.createQuery("SELECT * FROM voucher_value_decision")
                .mapTo<VoucherValueDecision>()
                .toList()
        }
    }

    private fun endDecisions(now: LocalDate) {
        db.transaction {
            voucherValueDecisionService.endDecisionsWithEndedPlacements(it, now)
        }
    }
}
