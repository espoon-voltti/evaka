// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.result.Result
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.controller.FeeDecisionTypeRequest
import fi.espoo.evaka.invoicing.controller.Wrapper
import fi.espoo.evaka.invoicing.data.upsertFeeDecisions
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionDetailed
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionSummary
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.snDaycareFullDay35
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testAdult_3
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testChild_4
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testDecisionMaker_2
import fi.espoo.evaka.toFeeDecisionServiceNeed
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class FeeDecisionIntegrationTest : FullApplicationTest() {
    @Autowired
    lateinit var asyncJobRunner: AsyncJobRunner

    private val user = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.FINANCE_ADMIN))

    private val testDecisions = listOf(
        createFeeDecisionFixture(
            status = FeeDecisionStatus.DRAFT,
            decisionType = FeeDecisionType.NORMAL,
            headOfFamilyId = testAdult_1.id,
            period = DateRange(LocalDate.of(2018, 5, 1), LocalDate.of(2018, 5, 31)),
            children = listOf(
                createFeeDecisionChildFixture(
                    childId = testChild_1.id,
                    dateOfBirth = testChild_1.dateOfBirth,
                    placementUnitId = testDaycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed()
                ),
                createFeeDecisionChildFixture(
                    childId = testChild_2.id,
                    dateOfBirth = testChild_2.dateOfBirth,
                    placementUnitId = testDaycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                    siblingDiscount = 50,
                    fee = 14500
                )
            )
        ),
        createFeeDecisionFixture(
            status = FeeDecisionStatus.SENT,
            decisionType = FeeDecisionType.NORMAL,
            headOfFamilyId = testAdult_1.id,
            period = DateRange(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 31)),
            children = listOf(
                createFeeDecisionChildFixture(
                    childId = testChild_2.id,
                    dateOfBirth = testChild_2.dateOfBirth,
                    placementUnitId = testDaycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed()
                )
            )
        ),
        createFeeDecisionFixture(
            status = FeeDecisionStatus.SENT,
            decisionType = FeeDecisionType.NORMAL,
            headOfFamilyId = testAdult_2.id,
            period = DateRange(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 31)),
            children = listOf(
                createFeeDecisionChildFixture(
                    childId = testChild_1.id,
                    dateOfBirth = testChild_1.dateOfBirth,
                    placementUnitId = testDaycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed()
                )
            )
        ),
        createFeeDecisionFixture(
            status = FeeDecisionStatus.DRAFT,
            decisionType = FeeDecisionType.NORMAL,
            headOfFamilyId = testAdult_3.id,
            period = DateRange(LocalDate.of(2015, 5, 1), LocalDate.of(2015, 5, 31)),
            children = listOf(
                createFeeDecisionChildFixture(
                    childId = testChild_3.id,
                    dateOfBirth = testChild_3.dateOfBirth,
                    placementUnitId = testDaycare2.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed()
                )
            )
        ),
        createFeeDecisionFixture(
            status = FeeDecisionStatus.DRAFT,
            decisionType = FeeDecisionType.NORMAL,
            headOfFamilyId = testAdult_3.id,
            period = DateRange(LocalDate.of(2016, 5, 1), LocalDate.of(2016, 5, 31)),
            children = listOf(
                createFeeDecisionChildFixture(
                    childId = testChild_4.id,
                    dateOfBirth = testChild_4.dateOfBirth,
                    placementUnitId = testDaycare2.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed()
                )
            )
        ),
        createFeeDecisionFixture(
            status = FeeDecisionStatus.DRAFT,
            decisionType = FeeDecisionType.RELIEF_ACCEPTED,
            headOfFamilyId = testAdult_3.id,
            period = DateRange(LocalDate.of(2017, 5, 1), LocalDate.of(2017, 5, 31)),
            children = listOf(
                createFeeDecisionChildFixture(
                    childId = testChild_4.id,
                    dateOfBirth = testChild_4.dateOfBirth,
                    placementUnitId = testDaycare2.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed()
                )
            )
        ),
        createFeeDecisionFixture(
            status = FeeDecisionStatus.DRAFT,
            decisionType = FeeDecisionType.RELIEF_PARTLY_ACCEPTED,
            headOfFamilyId = testAdult_3.id,
            period = DateRange(LocalDate.of(2018, 5, 1), LocalDate.of(2018, 5, 31)),
            children = listOf(
                createFeeDecisionChildFixture(
                    childId = testChild_4.id,
                    dateOfBirth = testChild_4.dateOfBirth,
                    placementUnitId = testDaycare2.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed()
                )
            )
        ),
        createFeeDecisionFixture(
            status = FeeDecisionStatus.DRAFT,
            decisionType = FeeDecisionType.RELIEF_REJECTED,
            headOfFamilyId = testAdult_3.id,
            period = DateRange(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 31)),
            children = listOf(
                createFeeDecisionChildFixture(
                    childId = testChild_4.id,
                    dateOfBirth = testChild_4.dateOfBirth,
                    placementUnitId = testDaycare2.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed()
                )
            )
        )
    )

    private fun assertEqualEnough(expected: List<FeeDecisionSummary>, actual: List<FeeDecisionSummary>) {
        val created = Instant.now()
        assertEquals(
            expected.map { it.copy(created = created, approvedAt = null, sentAt = null) }.toSet(),
            actual.map { it.copy(created = created, approvedAt = null, sentAt = null) }.toSet()
        )
    }

    private fun assertEqualEnough(expected: FeeDecisionDetailed, actual: FeeDecisionDetailed) {
        val created = Instant.now()
        assertEquals(
            expected.copy(created = created, approvedAt = null, sentAt = null, headOfFamily = expected.headOfFamily.copy(email = null)),
            actual.copy(created = created, approvedAt = null, sentAt = null)
        )
    }

    private fun deserializeListResult(json: String) = objectMapper.readValue<Paged<FeeDecisionSummary>>(json)
    private fun deserializeResult(json: String) = objectMapper.readValue<Wrapper<FeeDecisionDetailed>>(json)

    private fun postJsonData(path: String, payload: String): Result<String, FuelError> {
        val (_, _, result) = http.post(path)
            .jsonBody(payload)
            .asUser(user)
            .responseString()
        return result
    }

    private fun decisionSearch(payload: String): List<FeeDecisionSummary> {
        return deserializeListResult(postJsonData("/decisions/search", payload).get()).data
    }

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
        }
    }

    @AfterEach
    fun afterEach() {
        db.transaction { tx ->
            tx.resetDatabase()
        }
    }

    @Test
    fun `search works with no data in DB`() {
        val (_, _, result) = http.post("/decisions/search")
            .jsonBody("""{"page": 0, "pageSize": "50"}""")
            .asUser(user)
            .responseString()

        assertEqualEnough(
            listOf(),
            deserializeListResult(result.get()).data
        )
    }

    @Test
    fun `search works with test data in DB`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val (_, _, result) = http.post("/decisions/search")
            .jsonBody("""{"page": "0", "pageSize": "50"}""")
            .asUser(user)
            .responseString()

        assertEqualEnough(
            testDecisions.map(::toSummary),
            deserializeListResult(result.get()).data
        )
    }

    @Test
    fun `search works with strings and integers`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val data1 = decisionSearch("""{"page": "0", "pageSize": "1"}""")
        assertEquals(data1.size, 1)

        val data2 = decisionSearch("""{"page": 1, "pageSize": 1}""")
        assertEquals(data2.size, 1)

        assertNotEquals(data1[0].id, data2[0].id)

        val data3 = decisionSearch("""{"page": "1", "pageSize": "1"}""")
        assertEquals(data3.size, 1)

        assertEquals(data2[0].id, data3[0].id)
    }

    @Test
    fun `search works with draft status parameter`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }
        val drafts = testDecisions.filter { it.status === FeeDecisionStatus.DRAFT }

        val (_, _, result) = http.post("/decisions/search")
            .jsonBody("""{"page": "0", "pageSize": "50", "status": "DRAFT"}""")
            .asUser(user)
            .responseString()

        assertEqualEnough(
            drafts.map(::toSummary),
            deserializeListResult(result.get()).data
        )
    }

    @Test
    fun `search works with sent status parameter`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }
        val sent = testDecisions.filter { it.status === FeeDecisionStatus.SENT }

        val (_, _, result) = http.post("/decisions/search")
            .jsonBody("""{"page": "0", "pageSize": "50", "status": "SENT"}""")
            .asUser(user)
            .responseString()

        assertEqualEnough(
            sent.map(::toSummary),
            deserializeListResult(result.get()).data
        )
    }

    @Test
    fun `search works with multiple status parameters`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val (_, _, result) = http.post("/decisions/search")
            .jsonBody("""{"page": "0", "pageSize": "50", "status": "DRAFT,SENT"}""")
            .asUser(user)
            .responseString()

        assertEqualEnough(
            testDecisions.map(::toSummary),
            deserializeListResult(result.get()).data
        )
    }

    @Test
    fun `search works with distinctions param UNCONFIRMED_HOURS`() {
        val testDecision = testDecisions[0]
        val testDecisionMissingServiceNeed = testDecisions[1].copy(
            id = FeeDecisionId(UUID.randomUUID()),
            children = testDecisions[1].children[0].let { part ->
                listOf(part.copy(serviceNeed = part.serviceNeed.copy(missing = true)))
            }
        )

        db.transaction { tx -> tx.upsertFeeDecisions(listOf(testDecision, testDecisionMissingServiceNeed)) }

        val (_, _, result) = http.post("/decisions/search")
            .jsonBody("""{"page": "0", "pageSize": "50", "distinctions": "UNCONFIRMED_HOURS"}""")
            .asUser(user)
            .responseString()

        assertEqualEnough(
            listOf(testDecisionMissingServiceNeed.let(::toSummary)),
            deserializeListResult(result.get()).data
        )
    }

    @Test
    fun `search works with distinctions param EXTERNAL_CHILD`() {
        val testDecisionWithExtChild = testDecisions[4]

        db.transaction { tx ->
            tx.upsertFeeDecisions(listOf(testDecisions[0], testDecisions[3], testDecisionWithExtChild))
        }

        val (_, _, result) = http.post("/decisions/search")
            .jsonBody("""{"page": "0", "pageSize": "50", "distinctions": "EXTERNAL_CHILD"}""")
            .asUser(user)
            .responseString()

        assertEqualEnough(
            listOf(testDecisionWithExtChild.let(::toSummary)),
            deserializeListResult(result.get()).data
        )
    }

    @Test
    fun `search works with distinctions param RETROACTIVE`() {
        val now = LocalDate.now()
        val oldDecision = testDecisions[0].copy(validDuring = DateRange(now.minusMonths(2), now.minusMonths(1)))
        val futureDecision = testDecisions[1].copy(validDuring = DateRange(now.plusMonths(1), now.plusMonths(2)))

        db.transaction { tx -> tx.upsertFeeDecisions(listOf(oldDecision, futureDecision)) }

        val (_, _, result) = http.post("/decisions/search")
            .jsonBody("""{"page": "0", "pageSize": "50", "distinctions": "RETROACTIVE"}""")
            .asUser(user)
            .responseString()

        assertEqualEnough(
            listOf(oldDecision.let(::toSummary)),
            deserializeListResult(result.get()).data
        )
    }

    @Test
    fun `search works as expected with existing area param`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val (_, _, result) = http.post("/decisions/search")
            .jsonBody("""{"page": "0", "pageSize": "50", "area": "test_area"}""")
            .asUser(user)
            .responseString()

        assertEqualEnough(
            testDecisions.map(::toSummary).take(3),
            deserializeListResult(result.get()).data
        )
    }

    @Test
    fun `search works as expected with area and status params`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val (_, _, result) = http.post("/decisions/search")
            .jsonBody("""{"page": "0", "pageSize": "50", "area": "test_area", "status": "DRAFT"}""")
            .asUser(user)
            .responseString()

        assertEqualEnough(
            testDecisions.take(1).map(::toSummary),
            deserializeListResult(result.get()).data
        )
    }

    @Test
    fun `search works as expected with non-existent area param`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val (_, _, result) = http.post("/decisions/search")
            .jsonBody("""{"page": "0", "pageSize": "50", "area": "non_existent"}""")
            .asUser(user)
            .responseString()
        assertEqualEnough(
            listOf(),
            deserializeListResult(result.get()).data
        )
    }

    @Test
    fun `search works as expected with a unit param`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val (_, _, result) = http.post("/decisions/search")
            .jsonBody("""{"page": "0", "pageSize": "50", "unit": "${testDaycare.id}"}""")
            .asUser(user)
            .responseString()

        val expected =
            testDecisions.filter { decision -> decision.children.any { it.placement.unit.id == testDaycare.id } }
        assertEqualEnough(
            expected.map(::toSummary),
            deserializeListResult(result.get()).data
        )
    }

    @Test
    fun `search works as expected with a non-existent unit id param`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val (_, _, result) = http.post("/decisions/search")
            .jsonBody("""{"page": "0", "pageSize": "50", "unit": "${UUID.randomUUID()}"}""")
            .asUser(user)
            .responseString()

        assertEqualEnough(
            listOf(),
            deserializeListResult(result.get()).data
        )
    }

    @Test
    fun `search works as expected with multiple partial search terms`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val (_, _, result) = http.post("/decisions/search")
            .jsonBody(
                """{"page": "0", "pageSize": "50",
                          "searchTerms": "${testAdult_1.streetAddress} ${testAdult_1.firstName.substring(0, 2)}"}"""
            )
            .asUser(user)
            .responseString()

        val expectedDecisions = testDecisions.filter { it.headOfFamily.id != testAdult_3.id }

        assertEqualEnough(
            expectedDecisions.map(::toSummary),
            deserializeListResult(result.get()).data
        )
    }

    @Test
    fun `search works as expected with multiple more specific search terms`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val (_, _, result) = http.post("/decisions/search")
            .jsonBody(
                """{"page": "0", "pageSize": "50",
                          "searchTerms": "${testAdult_1.lastName.substring(0, 2)} ${testAdult_1.firstName}"}"""
            )
            .asUser(user)
            .responseString()

        assertEqualEnough(
            testDecisions.take(2).map(::toSummary),
            deserializeListResult(result.get()).data
        )
    }

    @Test
    fun `search works as expected with multiple search terms where one does not match anything`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val (_, _, result) = http.post("/decisions/search")
            .jsonBody(
                """{"page": "0", "pageSize": "50",
                          "searchTerms": "${testAdult_1.lastName} ${testAdult_1.streetAddress} nomatch"}"""
            )
            .asUser(user)
            .responseString()

        assertEqualEnough(
            listOf(),
            deserializeListResult(result.get()).data
        )
    }

    @Test
    fun `search works as expected with child name as search term`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val (_, _, result) = http.post("/decisions/search")
            .jsonBody(
                """{"page": "0", "pageSize": "50",
                          "searchTerms": "${testChild_2.firstName}",
                          "sortBy": "STATUS", "sortDirection": "ASC"}"""
            )
            .asUser(user)
            .responseString()

        assertEqualEnough(
            testDecisions.take(2).map(::toSummary).sortedBy { it.status.name },
            deserializeListResult(result.get()).data
        )
    }

    @Test
    fun `search works as expected with decision number as search term`() {
        val sentDecision =
            testDecisions.find { it.status == FeeDecisionStatus.SENT }!!.copy(decisionNumber = 123123123L)
        val otherDecision = testDecisions.find { it.status == FeeDecisionStatus.DRAFT }!!
        db.transaction { tx -> tx.upsertFeeDecisions(listOf(sentDecision, otherDecision)) }

        val (_, _, result) = http.post("/decisions/search")
            .jsonBody("""{"page": "0", "pageSize": "50", "searchTerms": "123123123"}""")
            .asUser(user)
            .responseString()

        assertEqualEnough(
            listOf(sentDecision).map(::toSummary),
            deserializeListResult(result.get()).data
        )
    }

    @Test
    fun `search works as expected with ssn as search term`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val (_, _, result) = http.post("/decisions/search")
            .jsonBody(
                """{"page": "0", "pageSize": "50",
                          "searchTerms": "${testAdult_1.ssn}"}"""
            )
            .asUser(user)
            .responseString()

        assertEqualEnough(
            testDecisions.take(2).map(::toSummary),
            deserializeListResult(result.get()).data
        )
    }

    @Test
    fun `search works as expected with date of birth as search term`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val (_, _, result) = http.post("/decisions/search")
            .jsonBody(
                """{"page": "0", "pageSize": "50",
                          "searchTerms": "${testAdult_1.ssn!!.substring(0, 6)}"}"""
            )
            .asUser(user)
            .responseString()

        assertEqualEnough(
            testDecisions.take(2).map(::toSummary),
            deserializeListResult(result.get()).data
        )
    }

    @Test
    fun `getDecision works with existing decision`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }
        val decision = testDecisions[0]

        val (_, _, result) = http.get("/decisions/${decision.id}")
            .asUser(user)
            .responseString()

        assertEqualEnough(
            decision.let(::toDetailed),
            deserializeResult(result.get()).data
        )
    }

    @Test
    fun `getDecision returns not found with non-existent decision`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val (_, response, _) = http.get("/decisions/00000000-0000-0000-0000-000000000000")
            .asUser(user)
            .responseString()
        assertEquals(404, response.statusCode)
    }

    @Test
    fun `getting head of family's fee decisions works`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val (_, _, result) = http.get("/decisions/head-of-family/${testAdult_1.id}")
            .asUser(user)
            .responseString()

        val feeDecisions = objectMapper.readValue<Wrapper<List<FeeDecision>>>(result.get()).data
        assertEquals(2, feeDecisions.size)
        feeDecisions.find { it.status == FeeDecisionStatus.DRAFT }.let { draft ->
            assertNotNull(draft)
            assertEquals(testAdult_1.id, draft.headOfFamily.id)
            assertEquals(28900 + 14500, draft.totalFee())
        }
        feeDecisions.find { it.status == FeeDecisionStatus.SENT }.let { sent ->
            assertNotNull(sent)
            assertEquals(testAdult_1.id, sent.headOfFamily.id)
            assertEquals(28900, sent.totalFee())
        }
    }

    @Test
    fun `confirmDrafts works with draft decision`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }
        val draft = testDecisions.find { it.status === FeeDecisionStatus.DRAFT }!!

        val (_, response, _) = http.post("/decisions/confirm")
            .asUser(user)
            .jsonBody(objectMapper.writeValueAsString(listOf(draft.id)))
            .responseString()
        assertEquals(204, response.statusCode)
    }

    @Test
    fun `confirmDrafts updates status, decision number, approver on draft`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }
        val draft = testDecisions.find { it.status === FeeDecisionStatus.DRAFT }!!

        http.post("/decisions/confirm")
            .asUser(user)
            .jsonBody(objectMapper.writeValueAsString(listOf(draft.id)))
            .responseString()

        asyncJobRunner.runPendingJobsSync(2)

        val activated = draft.copy(
            decisionNumber = 1,
            status = FeeDecisionStatus.SENT,
            approvedBy = PersonData.JustId(testDecisionMaker_1.id),
            decisionHandler = PersonData.JustId(testDecisionMaker_1.id),
            documentKey = "feedecision_${draft.id}_fi.pdf"
        )

        val (_, _, result) = http.get("/decisions/${draft.id}")
            .asUser(user)
            .responseString()

        assertEqualEnough(
            activated.let(::toDetailed),
            deserializeResult(result.get()).data
        )
    }

    @Test
    fun `confirmDrafts updates status, decision number, approver on relief decision`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }
        val draft = testDecisions.find { it.decisionType === FeeDecisionType.RELIEF_ACCEPTED }!!

        http.post("/decisions/confirm")
            .asUser(user)
            .jsonBody(objectMapper.writeValueAsString(listOf(draft.id)))
            .response()

        asyncJobRunner.runPendingJobsSync(2)

        val activated = draft.copy(
            decisionNumber = 1,
            status = FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING,
            approvedBy = PersonData.JustId(testDecisionMaker_1.id),
            decisionHandler = PersonData.JustId(testDecisionMaker_1.id),
            documentKey = "feedecision_${draft.id}_fi.pdf"
        )

        val (_, _, result) = http.get("/decisions/${draft.id}")
            .asUser(user)
            .responseString()

        assertEqualEnough(
            activated.let(::toDetailed),
            deserializeResult(result.get()).data
        )
    }

    @Test
    fun `confirmDrafts updates status, decision number, approver on relief partly accepted`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }
        val draft = testDecisions.find { it.decisionType === FeeDecisionType.RELIEF_PARTLY_ACCEPTED }!!

        http.post("/decisions/confirm")
            .asUser(user)
            .jsonBody(objectMapper.writeValueAsString(listOf(draft.id)))
            .response()

        asyncJobRunner.runPendingJobsSync(2)

        val activated = draft.copy(
            decisionNumber = 1,
            status = FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING,
            approvedBy = PersonData.JustId(testDecisionMaker_1.id),
            decisionHandler = PersonData.JustId(testDecisionMaker_1.id),
            documentKey = "feedecision_${draft.id}_fi.pdf"
        )

        val (_, _, result) = http.get("/decisions/${draft.id}")
            .asUser(user)
            .responseString()

        assertEqualEnough(
            activated.let(::toDetailed),
            deserializeResult(result.get()).data
        )
    }

    @Test
    fun `confirmDrafts updates status, decision number, approver on relief rejected`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }
        val draft = testDecisions.find { it.decisionType === FeeDecisionType.RELIEF_REJECTED }!!

        http.post("/decisions/confirm")
            .asUser(user)
            .jsonBody(objectMapper.writeValueAsString(listOf(draft.id)))
            .response()

        asyncJobRunner.runPendingJobsSync(2)

        val activated = draft.copy(
            decisionNumber = 1,
            status = FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING,
            approvedBy = PersonData.JustId(testDecisionMaker_1.id),
            decisionHandler = PersonData.JustId(testDecisionMaker_1.id),
            documentKey = "feedecision_${draft.id}_fi.pdf"
        )

        val (_, _, result) = http.get("/decisions/${draft.id}")
            .asUser(user)
            .responseString()

        assertEqualEnough(
            activated.let(::toDetailed),
            deserializeResult(result.get()).data
        )
    }

    @Test
    fun `confirmDrafts picks decision handler from unit when decision is not a relief decision nor retroactive`() {
        val draft = testDecisions.find { it.status === FeeDecisionStatus.DRAFT }!!
            .copy(validDuring = DateRange(LocalDate.now().withDayOfMonth(1), LocalDate.now().plusMonths(1)))
        db.transaction { tx -> tx.upsertFeeDecisions(listOf(draft)) }
        db.transaction {
            it.execute(
                "UPDATE daycare SET finance_decision_handler = ? WHERE id = ?",
                testDecisionMaker_2.id,
                draft.children.maxByOrNull { p -> p.child.dateOfBirth }!!.placement.unit.id
            )
        }

        http.post("/decisions/confirm")
            .asUser(user)
            .jsonBody(objectMapper.writeValueAsString(listOf(draft.id)))
            .responseString()

        asyncJobRunner.runPendingJobsSync(2)

        val activated = draft.copy(
            decisionNumber = 1,
            status = FeeDecisionStatus.SENT,
            approvedBy = PersonData.JustId(testDecisionMaker_1.id),
            decisionHandler = PersonData.JustId(testDecisionMaker_2.id),
            documentKey = "feedecision_${draft.id}_fi.pdf"
        )

        val (_, _, result) = http.get("/decisions/${draft.id}")
            .asUser(user)
            .responseString()

        assertEqualEnough(
            activated.let(::toDetailed),
            deserializeResult(result.get()).data
        )
    }

    @Test
    fun `confirmDrafts uses approver as decision handler when decision is a relief decision`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }
        val draft = testDecisions.find { it.decisionType === FeeDecisionType.RELIEF_ACCEPTED }!!
        db.transaction {
            it.execute(
                "UPDATE daycare SET finance_decision_handler = ? WHERE id = ?",
                testDecisionMaker_2.id,
                draft.children.maxByOrNull { p -> p.child.dateOfBirth }!!.placement.unit.id
            )
        }

        http.post("/decisions/confirm")
            .asUser(user)
            .jsonBody(objectMapper.writeValueAsString(listOf(draft.id)))
            .responseString()

        asyncJobRunner.runPendingJobsSync(2)

        val activated = draft.copy(
            decisionNumber = 1,
            status = FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING,
            approvedBy = PersonData.JustId(testDecisionMaker_1.id),
            decisionHandler = PersonData.JustId(testDecisionMaker_1.id),
            documentKey = "feedecision_${draft.id}_fi.pdf"
        )

        val (_, _, result) = http.get("/decisions/${draft.id}")
            .asUser(user)
            .responseString()

        assertEqualEnough(
            activated.let(::toDetailed),
            deserializeResult(result.get()).data
        )
    }

    @Test
    fun `confirmDrafts uses approver as decision handler when decision is retroactive`() {
        val now = LocalDate.now()
        val oldDecision = testDecisions.first().copy(validDuring = DateRange(now.minusMonths(2), now.minusMonths(1)))

        db.transaction { tx -> tx.upsertFeeDecisions(listOf(oldDecision)) }

        db.transaction {
            it.execute(
                "UPDATE daycare SET finance_decision_handler = ? WHERE id = ?",
                testDecisionMaker_2.id,
                oldDecision.children.maxByOrNull { p -> p.child.dateOfBirth }!!.placement.unit.id
            )
        }

        http.post("/decisions/confirm")
            .asUser(user)
            .jsonBody(objectMapper.writeValueAsString(listOf(oldDecision.id)))
            .responseString()

        asyncJobRunner.runPendingJobsSync(2)

        val activated = oldDecision.copy(
            decisionNumber = 1,
            status = FeeDecisionStatus.SENT,
            approvedBy = PersonData.JustId(testDecisionMaker_1.id),
            decisionHandler = PersonData.JustId(testDecisionMaker_1.id),
            documentKey = "feedecision_${oldDecision.id}_fi.pdf"
        )

        val (_, _, result) = http.get("/decisions/${oldDecision.id}")
            .asUser(user)
            .responseString()

        assertEqualEnough(
            activated.let(::toDetailed),
            deserializeResult(result.get()).data
        )
    }

    @Test
    fun `sendDecisions does not send if ssn missing`() {
        val draft =
            testDecisions.find { it.status === FeeDecisionStatus.DRAFT && it.headOfFamily.id === testAdult_3.id }!!

        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        http.post("/decisions/confirm")
            .asUser(user)
            .jsonBody(objectMapper.writeValueAsString(listOf(draft.id)))
            .response()

        asyncJobRunner.runPendingJobsSync(2)

        val (_, _, result) = http.get("/decisions/${draft.id}")
            .asUser(user)
            .responseString()

        val activated = draft.copy(
            decisionNumber = 1,
            status = FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING,
            approvedBy = PersonData.JustId(testDecisionMaker_1.id),
            decisionHandler = PersonData.JustId(testDecisionMaker_1.id),
            documentKey = "feedecision_${draft.id}_fi.pdf"
        )

        assertEqualEnough(
            activated.let(::toDetailed),
            deserializeResult(result.get()).data
        )
    }

    @Test
    fun `confirmDrafts returns bad request when some decisions are not drafts`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val (_, response, _) = http.post("/decisions/confirm")
            .asUser(user)
            .jsonBody(objectMapper.writeValueAsString(testDecisions.map { it.id }))
            .response()
        assertEquals(400, response.statusCode)
    }

    @Test
    fun `confirmDrafts returns bad request when some decisions being in the future`() {
        val draftWithFutureDates = testDecisions.find { it.status == FeeDecisionStatus.DRAFT }!!
            .copy(validDuring = DateRange(LocalDate.now().plusDays(1), LocalDate.now().plusYears(1)))
        db.transaction { tx -> tx.upsertFeeDecisions(listOf(draftWithFutureDates)) }

        val (_, response, _) = http.post("/decisions/confirm")
            .asUser(user)
            .jsonBody(objectMapper.writeValueAsString(draftWithFutureDates.id))
            .response()
        assertEquals(400, response.statusCode)
    }

    @Test
    fun `confirmDrafts updates conflicting sent decision end date on confirm`() {
        val draft = testDecisions.find { it.status == FeeDecisionStatus.DRAFT }!!
        val conflict = draft.copy(
            id = FeeDecisionId(UUID.randomUUID()),
            status = FeeDecisionStatus.SENT,
            validDuring = draft.validDuring.copy(start = draft.validFrom.minusDays(30))
        )
        db.transaction { tx -> tx.upsertFeeDecisions(listOf(draft, conflict)) }

        http.post("/decisions/confirm")
            .asUser(user)
            .jsonBody(objectMapper.writeValueAsString(listOf(draft.id)))
            .response()

        val (_, _, result) = http.get("/decisions/${conflict.id}")
            .asUser(user)
            .responseString()

        val updatedConflict = conflict.copy(validDuring = conflict.validDuring.copy(end = draft.validFrom.minusDays(1)))
        assertEqualEnough(
            updatedConflict.let(::toDetailed),
            deserializeResult(result.get()).data
        )
    }

    @Test
    fun `confirmDrafts updates conflicting decisions with exactly same validity dates to annulled state`() {
        val draft = testDecisions.find { it.status == FeeDecisionStatus.DRAFT }!!
        val conflict = draft.copy(
            id = FeeDecisionId(UUID.randomUUID()),
            status = FeeDecisionStatus.SENT
        )
        db.transaction { tx -> tx.upsertFeeDecisions(listOf(draft, conflict)) }

        http.post("/decisions/confirm")
            .asUser(user)
            .jsonBody(objectMapper.writeValueAsString(listOf(draft.id)))
            .response()

        val (_, _, result) = http.get("/decisions/${conflict.id}")
            .asUser(user)
            .responseString()

        val updatedConflict = conflict.copy(status = FeeDecisionStatus.ANNULLED)
        assertEqualEnough(
            updatedConflict.let(::toDetailed),
            deserializeResult(result.get()).data
        )
    }

    @Test
    fun `confirmDrafts updates completely overlapped conflicting decisions to annulled state`() {
        val draft = testDecisions.find { it.status == FeeDecisionStatus.DRAFT }!!
        val conflict1 = draft.copy(
            id = FeeDecisionId(UUID.randomUUID()),
            status = FeeDecisionStatus.SENT,
            validDuring = DateRange(draft.validFrom.plusDays(5), draft.validFrom.plusDays(10))
        )
        val conflict2 = draft.copy(
            id = FeeDecisionId(UUID.randomUUID()),
            status = FeeDecisionStatus.SENT,
            validDuring = DateRange(draft.validFrom.plusDays(11), draft.validFrom.plusDays(20))
        )
        db.transaction { tx -> tx.upsertFeeDecisions(listOf(draft, conflict1, conflict2)) }

        http.post("/decisions/confirm")
            .asUser(user)
            .jsonBody(objectMapper.writeValueAsString(listOf(draft.id)))
            .response()

        val (_, _, result_1) = http.get("/decisions/${conflict1.id}")
            .asUser(user)
            .responseString()

        val updatedConflict1 = conflict1.copy(status = FeeDecisionStatus.ANNULLED)
        assertEqualEnough(
            updatedConflict1.let(::toDetailed),
            deserializeResult(result_1.get()).data
        )

        val (_, _, result_2) = http.get("/decisions/${conflict2.id}")
            .asUser(user)
            .responseString()

        val updatedConflict2 = conflict2.copy(status = FeeDecisionStatus.ANNULLED)
        assertEqualEnough(
            updatedConflict2.let(::toDetailed),
            deserializeResult(result_2.get()).data
        )
    }

    @Test
    fun `confirmDrafts updates conflicting sent decision to annulled even when the sent decision would end later`() {
        val draft = testDecisions.find { it.status == FeeDecisionStatus.DRAFT }!!
        val conflict = draft.copy(
            id = FeeDecisionId(UUID.randomUUID()),
            status = FeeDecisionStatus.SENT,
            validDuring = draft.validDuring.copy(end = draft.validTo!!.plusDays(10))
        )
        db.transaction { tx -> tx.upsertFeeDecisions(listOf(draft, conflict)) }

        http.post("/decisions/confirm")
            .asUser(user)
            .jsonBody(objectMapper.writeValueAsString(listOf(draft.id)))
            .response()

        val (_, _, result) = http.get("/decisions/${conflict.id}")
            .asUser(user)
            .responseString()

        val updatedConflict = conflict.copy(status = FeeDecisionStatus.ANNULLED)
        assertEqualEnough(
            updatedConflict.let(::toDetailed),
            deserializeResult(result.get()).data
        )
    }

    @Test
    fun `confirmDrafts updates conflicting sent decision to annulled even when the new decisions cover it exactly`() {
        val originalDraft = testDecisions.find { it.status == FeeDecisionStatus.DRAFT }!!
        val splitDrafts = listOf(
            originalDraft.copy(
                id = FeeDecisionId(UUID.randomUUID()),
                validDuring = originalDraft.validDuring.copy(end = originalDraft.validFrom.plusDays(7))
            ),
            originalDraft.copy(
                id = FeeDecisionId(UUID.randomUUID()),
                validDuring = originalDraft.validDuring.copy(start = originalDraft.validFrom.plusDays(8))
            )
        )
        val conflict = originalDraft.copy(
            id = FeeDecisionId(UUID.randomUUID()),
            status = FeeDecisionStatus.SENT
        )
        db.transaction { tx -> tx.upsertFeeDecisions(splitDrafts + conflict) }

        http.post("/decisions/confirm")
            .asUser(user)
            .jsonBody(objectMapper.writeValueAsString(splitDrafts.map { it.id }))
            .response()

        val (_, _, result) = http.get("/decisions/${conflict.id}")
            .asUser(user)
            .responseString()

        val updatedConflict = conflict.copy(status = FeeDecisionStatus.ANNULLED)
        assertEqualEnough(
            updatedConflict.let(::toDetailed),
            deserializeResult(result.get()).data
        )
    }

    @Test
    fun `confirmDrafts updates conflicting sent decision to annulled even when the new decisions cover it`() {
        val originalDraft = testDecisions.find { it.status == FeeDecisionStatus.DRAFT }!!
        val splitDrafts = listOf(
            originalDraft.copy(
                id = FeeDecisionId(UUID.randomUUID()),
                validDuring = DateRange(originalDraft.validFrom.minusDays(1), originalDraft.validFrom.plusDays(7))
            ),
            originalDraft.copy(
                id = FeeDecisionId(UUID.randomUUID()),
                validDuring = DateRange(originalDraft.validFrom.plusDays(8), originalDraft.validTo!!.plusDays(1))
            )
        )
        val conflict = originalDraft.copy(
            id = FeeDecisionId(UUID.randomUUID()),
            status = FeeDecisionStatus.SENT
        )
        db.transaction { tx -> tx.upsertFeeDecisions(splitDrafts + conflict) }

        http.post("/decisions/confirm")
            .asUser(user)
            .jsonBody(objectMapper.writeValueAsString(splitDrafts.map { it.id }))
            .response()

        val (_, _, result) = http.get("/decisions/${conflict.id}")
            .asUser(user)
            .responseString()

        val updatedConflict = conflict.copy(status = FeeDecisionStatus.ANNULLED)
        assertEqualEnough(
            updatedConflict.let(::toDetailed),
            deserializeResult(result.get()).data
        )
    }

    @Test
    fun `date range picker does not return anything with range before first decision`() {
        val now = LocalDate.now()
        val oldDecision = testDecisions[0].copy(validDuring = DateRange(now.minusMonths(2), now.minusMonths(1)))
        val futureDecision = testDecisions[1].copy(validDuring = DateRange(now.plusMonths(1), now.plusMonths(2)))

        db.transaction { tx -> tx.upsertFeeDecisions(listOf(oldDecision, futureDecision)) }

        val (_, _, result) = http.post("/decisions/search")
            .jsonBody("""{"page": "0", "pageSize": "50", "startDate": "1900-01-01", "endDate": "1901-01-01"}""")
            .asUser(user)
            .responseString()

        assertEqualEnough(
            listOf(),
            deserializeListResult(result.get()).data
        )
    }

    @Test
    fun `date range picker finds only one decision in the past`() {
        val now = LocalDate.now()
        val oldDecision = testDecisions[0].copy(validDuring = DateRange(now.minusMonths(2), now.minusMonths(1)))
        val futureDecision = testDecisions[1].copy(validDuring = DateRange(now.plusMonths(1), now.plusMonths(2)))

        db.transaction { tx -> tx.upsertFeeDecisions(listOf(oldDecision, futureDecision)) }

        val (_, _, result) = http.post("/decisions/search")
            .jsonBody(
                """{"page": "0", "pageSize": "50",
                          "startDate": "${now.minusMonths(3)}",
                          "endDate": "${now.minusDays(1)}"}"""
            )
            .asUser(user)
            .responseString()

        assertEqualEnough(
            listOf(oldDecision.let(::toSummary)),
            deserializeListResult(result.get()).data
        )
    }

    @Test
    fun `date range picker finds only one decision in the future`() {
        val now = LocalDate.now()
        val oldDecision = testDecisions[0].copy(validDuring = DateRange(now.minusMonths(2), now.minusMonths(1)))
        val futureDecision = testDecisions[1].copy(validDuring = DateRange(now.plusMonths(1), now.plusMonths(2)))

        db.transaction { tx -> tx.upsertFeeDecisions(listOf(oldDecision, futureDecision)) }

        val (_, _, result) = http.post("/decisions/search")
            .jsonBody(
                """{"page": "0", "pageSize": "50",
                          "startDate": "${now.plusDays(1)}",
                          "endDate": "${now.plusMonths(8)}"}"""
            )
            .asUser(user)
            .responseString()

        assertEqualEnough(
            listOf(futureDecision.let(::toSummary)),
            deserializeListResult(result.get()).data
        )
    }

    @Test
    fun `date range picker finds a decisions with exact start date or end date`() {
        val now = LocalDate.now()
        val oldDecision = testDecisions[0].copy(validDuring = DateRange(now.minusMonths(2), now.minusMonths(1)))
        val futureDecision = testDecisions[1].copy(validDuring = DateRange(now.plusMonths(1), now.plusMonths(2)))

        db.transaction { tx -> tx.upsertFeeDecisions(listOf(oldDecision, futureDecision)) }

        val (_, _, resultPast) = http.post("/decisions/search")
            .jsonBody(
                """{"page": "0", "pageSize": "50",
                          "startDate": "${now.minusMonths(6)}",
                          "endDate": "${now.minusMonths(1)}"}"""
            )
            .asUser(user)
            .responseString()

        val (_, _, resultFuture) = http.post("/decisions/search")
            .jsonBody(
                """{"page": "0",
                          "pageSize": "50",
                          "startDate": "${now.plusMonths(2)}",
                          "endDate": "${now.plusMonths(8)}" }"""
            )
            .asUser(user)
            .responseString()

        assertEqualEnough(
            listOf(oldDecision.let(::toSummary)),
            deserializeListResult(resultPast.get()).data
        )

        assertEqualEnough(
            listOf(futureDecision.let(::toSummary)),
            deserializeListResult(resultFuture.get()).data
        )
    }

    @Test
    fun `date range picker finds all decisions without specifying a start date and having end date in the future`() {
        val now = LocalDate.now()
        val oldDecision = testDecisions[0].copy(validDuring = DateRange(now.minusMonths(2), now.minusMonths(1)))
        val futureDecision = testDecisions[1].copy(validDuring = DateRange(now.plusMonths(1), now.plusMonths(2)))

        db.transaction { tx -> tx.upsertFeeDecisions(listOf(oldDecision, futureDecision)) }

        val (_, _, result) = http.post("/decisions/search")
            .jsonBody(
                """{"page": "0", "pageSize": "50",
                          "endDate": "${now.plusYears(8)}"}"""
            )
            .asUser(user)
            .responseString()

        assertEqualEnough(
            listOf(oldDecision.let(::toSummary), futureDecision.let(::toSummary)),
            deserializeListResult(result.get()).data
        )
    }

    @Test
    fun `date range picker finds all decisions without specifying a end date and having start date in the past`() {
        val now = LocalDate.now()
        val oldDecision = testDecisions[0].copy(validDuring = DateRange(now.minusMonths(2), now.minusMonths(1)))
        val futureDecision = testDecisions[1].copy(validDuring = DateRange(now.plusMonths(1), now.plusMonths(2)))

        db.transaction { tx -> tx.upsertFeeDecisions(listOf(oldDecision, futureDecision)) }

        val (_, _, result) = http.post("/decisions/search")
            .jsonBody(
                """{"page": "0", "pageSize": "50",
                          "startDate": "${now.minusYears(8)}"}"""
            )
            .asUser(user)
            .responseString()

        assertEqualEnough(
            listOf(oldDecision.let(::toSummary), futureDecision.let(::toSummary)),
            deserializeListResult(result.get()).data
        )
    }

    @Test
    fun `set type updates decision type`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }
        val draft = testDecisions.find { it.decisionType == FeeDecisionType.NORMAL }!!
        val requestBody = FeeDecisionTypeRequest(type = FeeDecisionType.RELIEF_ACCEPTED)

        http.post("/decisions/set-type/${draft.id}")
            .asUser(user)
            .jsonBody(objectMapper.writeValueAsString(requestBody))
            .response()

        val modified = draft.copy(
            decisionType = FeeDecisionType.RELIEF_ACCEPTED
        )

        val (_, _, result) = http.get("/decisions/${draft.id}")
            .asUser(user)
            .responseString()

        assertEqualEnough(
            modified.let(::toDetailed),
            deserializeResult(result.get()).data
        )
    }

    @Test
    fun `sorting works with different params`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val (_, _, statusAsc) = http.post("/decisions/search")
            .jsonBody("""{"page": "0", "pageSize": "50", "sortBy": "STATUS", "sortDirection": "ASC"}""")
            .asUser(user)
            .responseString()

        assertEqualEnough(
            testDecisions.map(::toSummary).sortedBy { it.id.toString() }.sortedBy { it.status.name },
            deserializeListResult(statusAsc.get()).data
        )

        val (_, _, statusDesc) = http.post("/decisions/search")
            .jsonBody("""{"page": "0", "pageSize": "50", "sortBy": "STATUS", "sortDirection": "DESC"}""")
            .asUser(user)
            .responseString()

        assertEqualEnough(
            testDecisions.map(::toSummary).sortedBy { it.id.toString() }.sortedByDescending { it.status.name },
            deserializeListResult(statusDesc.get()).data
        )

        val (_, _, headAsc) = http.post("/decisions/search")
            .jsonBody("""{"page": "0", "pageSize": "50", "sortBy": "HEAD_OF_FAMILY", "sortDirection": "ASC"}""")
            .asUser(user)
            .responseString()

        assertEqualEnough(
            testDecisions.map(::toSummary).sortedBy { it.id.toString() }.sortedBy { it.headOfFamily.lastName },
            deserializeListResult(headAsc.get()).data
        )

        val (_, _, headDesc) = http.post("/decisions/search")
            .jsonBody("""{"page": "0", "pageSize": "50", "sortBy": "HEAD_OF_FAMILY", "sortDirection": "DESC"}""")
            .asUser(user)
            .responseString()

        assertEqualEnough(
            testDecisions.map(::toSummary).sortedBy { it.id.toString() }
                .sortedByDescending { it.headOfFamily.lastName },
            deserializeListResult(headDesc.get()).data
        )

        val (_, _, validityAsc) = http.post("/decisions/search")
            .jsonBody("""{"page": "0", "pageSize": "50", "sortBy": "VALIDITY", "sortDirection": "ASC"}""")
            .asUser(user)
            .responseString()

        assertEqualEnough(
            testDecisions.map(::toSummary).sortedBy { it.id.toString() }.sortedBy { it.validDuring.start },
            deserializeListResult(validityAsc.get()).data
        )

        val (_, _, validityDesc) = http.post("/decisions/search")
            .jsonBody("""{"page": "0", "pageSize": "50", "sortBy": "VALIDITY", "sortDirection": "DESC"}""")
            .asUser(user)
            .responseString()

        assertEqualEnough(
            testDecisions.map(::toSummary).sortedBy { it.id.toString() }.sortedByDescending { it.validDuring.start },
            deserializeListResult(validityDesc.get()).data
        )
    }

    @Test
    fun `fee decisions can be forced to be sent manually`() {
        val decision = testDecisions.first()

        db.transaction { tx ->
            tx.upsertFeeDecisions(listOf(decision))
        }

        val (_, _, result0) = http.get("/decisions/${decision.id}")
            .asUser(user)
            .responseString()

        deserializeResult(result0.get()).data.let { beforeForcing ->
            assertEquals(false, beforeForcing.requiresManualSending())
        }

        db.transaction { tx ->
            tx.createUpdate("update person set force_manual_fee_decisions = true where id = :id")
                .bind("id", decision.headOfFamily.id)
                .execute()
        }

        val (_, response, _) = http.post("/decisions/confirm")
            .asUser(user)
            .jsonBody(objectMapper.writeValueAsString(listOf(decision.id)))
            .responseString()

        assertEquals(204, response.statusCode)

        asyncJobRunner.runPendingJobsSync(2)

        val (_, _, result1) = http.get("/decisions/${decision.id}")
            .asUser(user)
            .responseString()

        deserializeResult(result1.get()).data.let { confirmed ->
            assertEquals(true, confirmed.requiresManualSending())
            assertEquals(FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING, confirmed.status)
        }
    }
}
