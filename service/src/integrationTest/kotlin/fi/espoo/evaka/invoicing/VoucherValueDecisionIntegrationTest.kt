// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.objectBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.data.PagedVoucherValueDecisionSummaries
import fi.espoo.evaka.invoicing.data.approveValueDecisionDraftsForSending
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionType
import fi.espoo.evaka.pis.controllers.ParentshipController
import fi.espoo.evaka.placement.Placement
import fi.espoo.evaka.placement.PlacementCreateRequestBody
import fi.espoo.evaka.placement.PlacementResponse
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.PlacementUpdateRequestBody
import fi.espoo.evaka.sficlient.MockSfiMessagesClient
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestParentship
import fi.espoo.evaka.shared.dev.insertTestPartnership
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testAdult_3
import fi.espoo.evaka.testAdult_5
import fi.espoo.evaka.testAdult_7
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testDecisionMaker_2
import fi.espoo.evaka.testVoucherDaycare
import fi.espoo.evaka.testVoucherDaycare2
import fi.espoo.evaka.withMockedTime
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class VoucherValueDecisionIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    @BeforeEach
    fun beforeEach() {
        MockSfiMessagesClient.clearMessages()

        db.transaction {
            it.insertGeneralTestFixtures()
            it.insertTestParentship(
                headOfChild = testAdult_1.id,
                childId = testChild_1.id,
                startDate = testChild_1.dateOfBirth,
                endDate = testChild_1.dateOfBirth.plusYears(18).minusDays(1)
            )
            it.insertTestPartnership(adult1 = testAdult_1.id, adult2 = testAdult_2.id)
        }
    }

    private val now = HelsinkiDateTime.of(LocalDate.of(2023, 1, 1), LocalTime.of(9, 0))
    private val startDate = now.toLocalDate().minusMonths(1)
    private val endDate = now.toLocalDate().plusMonths(6)

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
    fun `send voucher value decisions returns bad request when some decisions being in the future`() {
        val startDate =
            now.toLocalDate().plusDays(evakaEnv.nrOfDaysVoucherValueDecisionCanBeSentInAdvance + 1)
        val endDate = startDate.plusMonths(6)
        createPlacement(startDate, endDate)
        sendAllValueDecisions(400, "voucherValueDecisions.confirmation.tooFarInFuture")

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.DRAFT, decisions.first().status)
        }
    }

    @Test
    fun `send voucher value decisions when decision at last possible confirmation date exists`() {
        val startDate =
            now.toLocalDate().plusDays(evakaEnv.nrOfDaysVoucherValueDecisionCanBeSentInAdvance)
        val endDate = startDate.plusMonths(6)
        createPlacement(startDate, endDate)
        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.SENT, decisions.first().status)
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

        val newEndDate = now.toLocalDate().minusDays(1)
        updatePlacement(placementId, startDate, newEndDate)
        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.SENT, decisions.first().status)
            assertEquals(newEndDate, decisions.first().validTo)
        }
    }

    @Test
    fun `sent value decision is annulled when a child's head of family changes`() {
        createPlacement(startDate, endDate)
        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.SENT, decisions.first().status)
            assertEquals(testAdult_1.id, decisions.first().headOfFamilyId)
        }

        changeHeadOfFamily(testChild_1, testAdult_5.id)
        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(2, decisions.size)
            val annulled = decisions.find { it.status == VoucherValueDecisionStatus.ANNULLED }
            assertNotNull(annulled)
            assertEquals(testAdult_1.id, annulled.headOfFamilyId)
            val sent = decisions.find { it.status == VoucherValueDecisionStatus.SENT }
            assertNotNull(sent)
            assertEquals(testAdult_5.id, sent.headOfFamilyId)
        }
    }

    @Test
    fun `value decision handler is set to approver for relief decision`() {
        createPlacement(startDate, endDate)
        db.transaction {
            @Suppress("DEPRECATION")
            it.createUpdate(
                    "UPDATE voucher_value_decision d SET decision_type = :decisionType WHERE child_id = :childId"
                )
                .bind("decisionType", VoucherValueDecisionType.RELIEF_ACCEPTED)
                .bind("childId", testChild_1.id)
                .execute()

            it.execute(
                "UPDATE daycare SET finance_decision_handler = ? WHERE id = ?",
                testDecisionMaker_2.id,
                testVoucherDaycare.id
            )
        }

        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(
                VoucherValueDecisionStatus.WAITING_FOR_MANUAL_SENDING,
                decisions.first().status
            )
            assertEquals(financeWorker.id.raw, decisions.first().decisionHandler)
        }
    }

    @Test
    fun `value decision drafts not overlapping the period have the same created date after generating new value decision drafts`() {
        createPlacement(startDate, endDate)
        val initialDecisionDraft = getAllValueDecisions().first()

        createPlacement(startDate.plusYears(1), endDate.plusYears(1))
        getAllValueDecisions().let { decisions ->
            assertEquals(2, decisions.size)
            assertEquals(1, decisions.count { it.created == initialDecisionDraft.created })
        }
    }

    @Test
    fun `value decision is replaced when child is placed into another voucher unit`() {
        createPlacement(startDate, endDate)
        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.SENT, decisions.first().status)
            assertEquals(endDate, decisions.first().validTo)
        }

        val newStartDate = now.toLocalDate().minusDays(1)
        createPlacement(newStartDate, endDate, testVoucherDaycare2.id)

        sendAllValueDecisions()
        getAllValueDecisions()
            .sortedBy { it.validFrom }
            .let { decisions ->
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

        val newStartDate = now.toLocalDate().minusDays(1)
        createPlacement(newStartDate, endDate, testDaycare.id)
        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(startDate, decisions.first().validFrom)
            assertEquals(newStartDate.minusDays(1), decisions.first().validTo)
        }
    }

    @Test
    fun `value decision cleanup works when child's placement is deleted`() {
        val placementId = createPlacement(startDate, endDate)
        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.SENT, decisions.first().status)
            assertEquals(testAdult_1.id, decisions.first().headOfFamilyId)
        }

        deletePlacement(placementId)
        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.ANNULLED, decisions.first().status)
            assertEquals(testAdult_1.id, decisions.first().headOfFamilyId)
        }
    }

    @Test
    fun `value decision is ended when child switches to preschool`() {
        createPlacement(startDate, endDate, type = PlacementType.DAYCARE)
        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.SENT, decisions.first().status)
            assertEquals(endDate, decisions.first().validTo)
        }

        val newStartDate = now.toLocalDate().minusDays(1)
        createPlacement(newStartDate, endDate, type = PlacementType.PRESCHOOL)
        sendAllValueDecisions()

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
        assertEquals(
            1,
            searchValueDecisions(status = "DRAFT", searchTerms = "Ricky").total
        ) // child
        assertEquals(1, searchValueDecisions(status = "DRAFT", searchTerms = "John").total) // head
        assertEquals(
            1,
            searchValueDecisions(status = "DRAFT", searchTerms = "Joan").total
        ) // partner
        assertEquals(
            0,
            searchValueDecisions(status = "DRAFT", searchTerms = "Foobar").total
        ) // no match
        assertEquals(0, searchValueDecisions(status = "SENT").total)

        sendAllValueDecisions()
        assertEquals(0, searchValueDecisions(status = "DRAFT").total)
        assertEquals(1, searchValueDecisions(status = "SENT").total)
        assertEquals(1, searchValueDecisions(status = "SENT", searchTerms = "Ricky").total)
        assertEquals(0, searchValueDecisions(status = "SENT", searchTerms = "Foobar").total)
    }

    @Test
    fun `filter out starting placements`() {
        val placementId =
            createPlacement(now.toLocalDate().minusMonths(1), now.toLocalDate().plusMonths(1))

        sendAllValueDecisions()

        searchValueDecisions("SENT", "", """["NO_STARTING_PLACEMENTS"]""").let { decisions ->
            assertEquals(1, decisions.data.size)
        }

        db.transaction {
            @Suppress("DEPRECATION")
            it.createUpdate("UPDATE placement SET start_date = :now WHERE id = :placementId")
                .bind("placementId", placementId)
                .bind("now", now.toLocalDate())
                .execute()
        }

        searchValueDecisions("SENT", "", """["NO_STARTING_PLACEMENTS"]""").let { decisions ->
            assertEquals(0, decisions.data.size)
        }
    }

    @Test
    fun `PDF can be downloaded`() {
        createPlacement(startDate, endDate)
        val decisionIds = sendAllValueDecisions()

        assertEquals(200, getPdfStatus(decisionIds[0], financeWorker))
    }

    @Test
    fun `PDF can not be downloaded if head of family has restricted details`() {
        db.transaction {
            // testAdult_7 has restricted details on
            it.insertTestParentship(
                headOfChild = testAdult_7.id,
                childId = testChild_2.id,
                startDate = testChild_2.dateOfBirth,
                endDate = testChild_2.dateOfBirth.plusYears(18).minusDays(1)
            )
            it.insertTestPartnership(adult1 = testAdult_7.id, adult2 = testAdult_3.id)
        }
        createPlacement(startDate, endDate, childId = testChild_2.id)
        val decisionIds = sendAllValueDecisions()

        assertEquals(403, getPdfStatus(decisionIds[0], financeWorker))

        // Check that message is still sent via sfi
        asyncJobRunner.runPendingJobsSync(MockEvakaClock(now))
        assertEquals(1, MockSfiMessagesClient.getMessages().size)
    }

    @Test
    fun `PDF can not be downloaded if child has restricted details`() {
        val testChildRestricted =
            testChild_1.copy(
                id = PersonId(UUID.randomUUID()),
                ssn = "010617A125W",
                restrictedDetailsEnabled = true
            )

        db.transaction {
            it.insert(testChildRestricted, DevPersonType.RAW_ROW)
            it.insertTestParentship(
                headOfChild = testAdult_3.id,
                childId = testChildRestricted.id,
                startDate = testChildRestricted.dateOfBirth,
                endDate = testChildRestricted.dateOfBirth.plusYears(18).minusDays(1)
            )
        }
        createPlacement(startDate, endDate, childId = testChildRestricted.id)
        val decisionIds = sendAllValueDecisions()

        assertEquals(403, getPdfStatus(decisionIds[0], financeWorker))
    }

    @Test
    fun `PDF can be downloaded by admin even if someone in the family has restricted details`() {
        db.transaction {
            // testAdult_7 has restricted details on
            it.insertTestParentship(
                headOfChild = testAdult_7.id,
                childId = testChild_2.id,
                startDate = testChild_2.dateOfBirth,
                endDate = testChild_2.dateOfBirth.plusYears(18).minusDays(1)
            )
            it.insertTestPartnership(adult1 = testAdult_7.id, adult2 = testAdult_3.id)
        }
        createPlacement(startDate, endDate, childId = testChild_2.id)
        val decisionIds = sendAllValueDecisions()

        assertEquals(200, getPdfStatus(decisionIds[0], adminUser))
    }

    @Test
    fun `VoucherValueDecision handler is set to the user when decision is not normal`() {
        val approvedDecision = createReliefDecision(false)
        assertEquals(testDecisionMaker_1.id.raw, approvedDecision.decisionHandler)
    }

    @Test
    fun `VoucherValueDecision handler is set to the daycare handler when forced when decision is not normal`() {
        val approvedDecision = createReliefDecision(true)
        assertEquals(
            testVoucherDaycare.financeDecisionHandler?.raw,
            approvedDecision.decisionHandler
        )
    }

    fun createReliefDecision(forceDaycareHandler: Boolean): VoucherValueDecision {
        createPlacement(startDate, endDate)
        val decision = getAllValueDecisions().getOrNull(0)!!

        db.transaction {
            @Suppress("DEPRECATION")
            it.createUpdate(
                    "UPDATE voucher_value_decision SET decision_type='RELIEF_ACCEPTED' WHERE id = :id"
                )
                .bind("id", decision.id)
                .execute()
        }

        db.transaction {
            it.approveValueDecisionDraftsForSending(
                listOf(decision.id),
                testDecisionMaker_1.id,
                HelsinkiDateTime.now(),
                null,
                forceDaycareHandler
            )
        }

        return getAllValueDecisions().getOrNull(0)!!
    }

    private val serviceWorker =
        AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))
    private val financeWorker =
        AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.FINANCE_ADMIN))
    private val adminUser =
        AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.ADMIN))

    private fun createPlacement(
        startDate: LocalDate,
        endDate: LocalDate,
        unitId: DaycareId = testVoucherDaycare.id,
        childId: ChildId = testChild_1.id,
        type: PlacementType = PlacementType.DAYCARE
    ): PlacementId {
        val body =
            PlacementCreateRequestBody(
                type = type,
                childId = childId,
                unitId = unitId,
                startDate = startDate,
                endDate = endDate,
                placeGuarantee = false
            )

        http
            .post("/placements")
            .asUser(serviceWorker)
            .withMockedTime(now)
            .objectBody(body, mapper = jsonMapper)
            .response()
            .also { (_, res, _) -> assertEquals(200, res.statusCode) }

        val (_, _, data) =
            http
                .get("/placements", listOf("childId" to childId))
                .asUser(serviceWorker)
                .responseObject<PlacementResponse>(jsonMapper)

        asyncJobRunner.runPendingJobsSync(MockEvakaClock(now))

        return data.get().placements.first().id
    }

    private fun updatePlacement(id: PlacementId, startDate: LocalDate, endDate: LocalDate) {
        val body = PlacementUpdateRequestBody(startDate = startDate, endDate = endDate)

        http
            .put("/placements/$id")
            .asUser(serviceWorker)
            .withMockedTime(now)
            .objectBody(body, mapper = jsonMapper)
            .responseObject<Placement>(jsonMapper)
            .also { (_, res, _) -> assertEquals(200, res.statusCode) }

        asyncJobRunner.runPendingJobsSync(MockEvakaClock(now))
    }

    private fun deletePlacement(id: PlacementId) {
        http.delete("/placements/$id").asUser(serviceWorker).withMockedTime(now).response().also {
            (_, res, _) ->
            assertEquals(200, res.statusCode)
        }

        asyncJobRunner.runPendingJobsSync(MockEvakaClock(now))
    }

    private fun changeHeadOfFamily(child: DevPerson, headOfFamilyId: PersonId) {
        db.transaction { it.execute("DELETE FROM fridge_child WHERE child_id = ?", child.id) }

        val body =
            ParentshipController.ParentshipRequest(
                childId = child.id,
                headOfChildId = headOfFamilyId,
                startDate = child.dateOfBirth,
                endDate = child.dateOfBirth.plusYears(18).minusDays(1)
            )

        http
            .post("/parentships")
            .asUser(serviceWorker)
            .withMockedTime(now)
            .objectBody(body, mapper = jsonMapper)
            .response()

        asyncJobRunner.runPendingJobsSync(MockEvakaClock(now))
    }

    private fun searchValueDecisions(
        status: String,
        searchTerms: String = "",
        distinctionsString: String = "[]"
    ): PagedVoucherValueDecisionSummaries {
        val (_, _, data) =
            http
                .post("/value-decisions/search")
                .jsonBody(
                    """{"page": 0, "pageSize": 100, "statuses": ["$status"], "searchTerms": "$searchTerms", "distinctions": $distinctionsString}"""
                )
                .withMockedTime(now)
                .asUser(financeWorker)
                .responseObject<PagedVoucherValueDecisionSummaries>(jsonMapper)
        return data.get()
    }

    private fun sendAllValueDecisions(
        expectedStatusCode: Int = 200,
        expectedErrorCode: String? = null
    ): List<VoucherValueDecisionId> {
        val (_, _, data) =
            http
                .post("/value-decisions/search")
                .jsonBody("""{"page": 0, "pageSize": 100, "statuses": ["DRAFT"]}""")
                .withMockedTime(now)
                .asUser(financeWorker)
                .responseObject<PagedVoucherValueDecisionSummaries>(jsonMapper)
                .also { (_, res, _) -> assertEquals(200, res.statusCode) }

        val decisionIds = data.get().data.map { it.id }
        http
            .post("/value-decisions/send")
            .objectBody(decisionIds, mapper = jsonMapper)
            .withMockedTime(now)
            .asUser(financeWorker)
            .response()
            .also { (_, res, _) ->
                assertEquals(expectedStatusCode, res.statusCode)
                if (expectedStatusCode == 400) {
                    val responseJson = res.body().asString("application/json")
                    val errorCode = jsonMapper.readTree(responseJson).get("errorCode").textValue()
                    assertEquals(expectedErrorCode, errorCode)
                }
            }

        asyncJobRunner.runPendingJobsSync(MockEvakaClock(now))
        return decisionIds
    }

    private fun getAllValueDecisions(): List<VoucherValueDecision> {
        return db.read {
                @Suppress("DEPRECATION")
                it.createQuery("SELECT * FROM voucher_value_decision")
                    .toList<VoucherValueDecision>()
            }
            .shuffled() // randomize order to expose assumptions
    }

    private fun getPdfStatus(
        decisionId: VoucherValueDecisionId,
        user: AuthenticatedUser.Employee
    ): Int {
        val (_, response, _) = http.get("/value-decisions/pdf/$decisionId").asUser(user).response()
        return response.statusCode
    }
}
