// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.controller.FeeDecisionSortParam
import fi.espoo.evaka.invoicing.controller.SortDirection
import fi.espoo.evaka.invoicing.createFeeDecisionAlterationFixture
import fi.espoo.evaka.invoicing.createFeeDecisionFixture
import fi.espoo.evaka.invoicing.createFeeDecisionPartFixture
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.service.getInvoiceableFeeDecisions
import fi.espoo.evaka.invoicing.toDetailed
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.config.defaultObjectMapper
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testAdult_3
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_5
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class FeeDecisionQueriesTest : PureJdbiTest() {
    val objectMapper = defaultObjectMapper()

    private val testPeriod = DateRange(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 31))
    private val testDecisions = listOf(
        createFeeDecisionFixture(
            status = FeeDecisionStatus.DRAFT,
            decisionType = FeeDecisionType.NORMAL,
            headOfFamilyId = testAdult_1.id,
            period = testPeriod,
            parts = listOf(
                createFeeDecisionPartFixture(
                    childId = testChild_1.id,
                    dateOfBirth = testChild_1.dateOfBirth,
                    daycareId = testDaycare.id
                ),
                createFeeDecisionPartFixture(
                    childId = testChild_2.id,
                    dateOfBirth = testChild_2.dateOfBirth,
                    daycareId = testDaycare.id,
                    siblingDiscount = 50,
                    fee = 145
                )
            )
        ),
        createFeeDecisionFixture(
            status = FeeDecisionStatus.SENT,
            decisionType = FeeDecisionType.NORMAL,
            headOfFamilyId = testAdult_1.id,
            period = testPeriod,
            parts = listOf(
                createFeeDecisionPartFixture(
                    childId = testChild_2.id,
                    dateOfBirth = testChild_2.dateOfBirth,
                    daycareId = testDaycare.id
                )
            )
        ),
        createFeeDecisionFixture(
            status = FeeDecisionStatus.SENT,
            decisionType = FeeDecisionType.NORMAL,
            headOfFamilyId = testAdult_2.id,
            period = testPeriod,
            parts = listOf(
                createFeeDecisionPartFixture(
                    childId = testChild_1.id,
                    dateOfBirth = testChild_1.dateOfBirth,
                    daycareId = testDaycare.id
                )
            )
        )
    )

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
    fun `getByIds with zero ids`() {
        db.transaction { tx ->
            tx.upsertFeeDecisions(objectMapper, testDecisions)
            val result = tx.getFeeDecisionsByIds(objectMapper, listOf())
            assertEquals(0, result.size)
        }
    }

    @Test
    fun `getByIds with one id`() {
        db.transaction { tx ->
            tx.upsertFeeDecisions(objectMapper, testDecisions)
            val result = tx.getFeeDecisionsByIds(objectMapper, listOf(testDecisions[0].id))
            assertEquals(1, result.size)
        }
    }

    @Test
    fun `getByIds with two ids`() {
        db.transaction { tx ->
            tx.upsertFeeDecisions(objectMapper, testDecisions)
            val result = tx.getFeeDecisionsByIds(objectMapper, listOf(testDecisions[0].id, testDecisions[1].id))
            assertEquals(2, result.size)
        }
    }

    @Test
    fun `getByIds with missing ids`() {
        db.transaction { tx ->
            tx.upsertFeeDecisions(objectMapper, testDecisions)
            val result = tx.getFeeDecisionsByIds(objectMapper, listOf(UUID.randomUUID(), UUID.randomUUID()))
            assertEquals(0, result.size)
        }
    }

    @Test
    fun `upsert with existing row`() {
        db.transaction { tx ->
            tx.upsertFeeDecisions(objectMapper, testDecisions)
            val updated = testDecisions[0].copy(decisionNumber = 100)
            tx.upsertFeeDecisions(objectMapper, listOf(updated))

            val result = tx.getFeeDecisionsByIds(objectMapper, listOf(updated.id))
            assertEquals(100.toLong(), result[0].decisionNumber)
        }
    }

    @Test
    fun `upsert updates parts`() {
        db.transaction { tx ->
            tx.upsertFeeDecisions(objectMapper, testDecisions)
            val updated = testDecisions.map {
                it.copy(
                    parts = it.parts.map { part ->
                        part.copy(
                            baseFee = 1000,
                            siblingDiscount = 90,
                            feeAlterations = listOf(
                                createFeeDecisionAlterationFixture(
                                    type = FeeAlteration.Type.DISCOUNT,
                                    amount = 20,
                                    isAbsolute = false,
                                    effect = -5780
                                )
                            )
                        )
                    }
                )
            }
            tx.upsertFeeDecisions(objectMapper, updated)

            val result = tx.getFeeDecisionsByIds(objectMapper, testDecisions.map { it.id })
            with(result[0]) {
                assertEquals(updated[0].parts, parts)
            }

            with(result[1]) {
                assertEquals(updated[1].parts, parts)
            }

            with(result[2]) {
                assertEquals(updated[2].parts, parts)
            }
        }
    }

    @Test
    fun `upsert with empty list`() {
        db.transaction { tx ->
            tx.upsertFeeDecisions(objectMapper, testDecisions)
            tx.upsertFeeDecisions(objectMapper, emptyList())

            val result = tx.getFeeDecisionsByIds(objectMapper, testDecisions.map { it.id })
            assertEquals(testDecisions.sortedBy { it.status }, result.sortedBy { it.status })
        }
    }

    @Test
    fun `upsert does not affect createdAt`() {
        db.transaction { tx ->
            tx.upsertFeeDecisions(objectMapper, testDecisions)
            val initialCreatedAt = testDecisions[0].createdAt
            val decision = testDecisions[0].copy(decisionNumber = 314159265)
            tx.upsertFeeDecisions(objectMapper, listOf(decision))

            val result = tx.getFeeDecisionsByIds(objectMapper, listOf(decision.id))
            assertEquals(initialCreatedAt, result[0].createdAt)
        }
    }

    @Test
    fun `getDecision existing id`() {
        db.transaction { tx ->
            tx.upsertFeeDecisions(objectMapper, testDecisions)
            val result = tx.getFeeDecision(objectMapper, testDecisions[0].id)
            assertNotNull(result)
            assertEquals(toDetailed(testDecisions[0]), result)
        }
    }

    @Test
    fun `getDecision non-existant id`() {
        db.transaction { tx ->
            tx.upsertFeeDecisions(objectMapper, testDecisions)
            val result = tx.getFeeDecision(objectMapper, UUID.randomUUID())
            assertNull(result)
        }
    }

    @Test
    fun `find for head of family`() {
        db.transaction { tx ->
            tx.upsertFeeDecisions(objectMapper, testDecisions)
            val result = tx.findFeeDecisionsForHeadOfFamily(objectMapper, testAdult_1.id, testPeriod, null)
            assertEquals(2, result.size)
        }
    }

    @Test
    fun `find for head of family without to date`() {
        db.transaction { tx ->
            tx.upsertFeeDecisions(objectMapper, testDecisions)
            val result =
                tx.findFeeDecisionsForHeadOfFamily(objectMapper, testAdult_1.id, DateRange(testPeriod.start, null), null)
            assertEquals(2, result.size)
        }
    }

    @Test
    fun `find for head of family with wrong id`() {
        db.transaction { tx ->
            tx.upsertFeeDecisions(objectMapper, testDecisions)
            val result = tx.findFeeDecisionsForHeadOfFamily(
                objectMapper,
                UUID.randomUUID(),
                DateRange(testPeriod.start, null),
                null
            )
            assertEquals(0, result.size)
        }
    }

    @Test
    fun `find for head of family with draft status`() {
        db.transaction { tx ->
            tx.upsertFeeDecisions(objectMapper, testDecisions)
            val result =
                tx.findFeeDecisionsForHeadOfFamily(
                    objectMapper,
                    testAdult_1.id,
                    testPeriod,
                    listOf(FeeDecisionStatus.DRAFT)
                )
            assertEquals(1, result.size)
        }
    }

    @Test
    fun `find for head of family with sent status`() {
        db.transaction { tx ->
            tx.upsertFeeDecisions(objectMapper, testDecisions)
            val result =
                tx.findFeeDecisionsForHeadOfFamily(
                    objectMapper,
                    testAdult_1.id,
                    testPeriod,
                    listOf(FeeDecisionStatus.SENT)
                )
            assertEquals(1, result.size)
        }
    }

    @Test
    fun `activateDrafts with one decision`() {
        db.transaction { tx ->
            val draft = testDecisions.find { it.status == FeeDecisionStatus.DRAFT }!!
            tx.upsertFeeDecisions(objectMapper, listOf(draft))
            tx.approveFeeDecisionDraftsForSending(listOf(draft.id), testDecisionMaker_1.id, approvedAt = Instant.now())

            val result = tx.getFeeDecision(objectMapper, testDecisions[0].id)!!
            assertEquals(FeeDecisionStatus.WAITING_FOR_SENDING, result.status)
            assertEquals(1L, result.decisionNumber)
            assertEquals(testDecisionMaker_1, result.approvedBy)
            assertNotNull(result.approvedAt)
        }
    }

    @Test
    fun `approveDraftsForSending with multiple decisions`() {
        db.transaction { tx ->
            val decisions = listOf(
                testDecisions[0],
                testDecisions[0].copy(
                    id = UUID.randomUUID(),
                    validFrom = testPeriod.start.minusYears(1),
                    validTo = testPeriod.end!!.minusYears(1)
                )
            )
            tx.upsertFeeDecisions(objectMapper, decisions)

            tx.approveFeeDecisionDraftsForSending(decisions.map { it.id }, testDecisionMaker_1.id, approvedAt = Instant.now())

            val result = tx.getFeeDecisionsByIds(objectMapper, decisions.map { it.id }).sortedBy { it.decisionNumber }
            with(result[0]) {
                assertEquals(FeeDecisionStatus.WAITING_FOR_SENDING, status)
                assertEquals(1L, decisionNumber)
            }
            with(result[1]) {
                assertEquals(FeeDecisionStatus.WAITING_FOR_SENDING, status)
                assertEquals(2L, decisionNumber)
            }
        }
    }

    @Test
    fun `delete with empty list`() {
        db.transaction { tx ->
            tx.upsertFeeDecisions(objectMapper, testDecisions)
            val before = tx.getFeeDecisionsByIds(objectMapper, testDecisions.map { it.id })

            tx.deleteFeeDecisions(listOf())

            val after = tx.getFeeDecisionsByIds(objectMapper, testDecisions.map { it.id })
            assertEquals(before, after)
        }
    }

    @Test
    fun `delete with one id`() {
        db.transaction { tx ->
            tx.upsertFeeDecisions(objectMapper, testDecisions)
            tx.deleteFeeDecisions(listOf(testDecisions[0].id))

            val result = tx.getFeeDecisionsByIds(objectMapper, testDecisions.map { it.id })
            assertEquals(testDecisions.drop(1), result)
        }
    }

    @Test
    fun `delete with two ids`() {
        db.transaction { tx ->
            tx.upsertFeeDecisions(objectMapper, testDecisions)
            tx.deleteFeeDecisions(testDecisions.map { it.id })

            val result = tx.getFeeDecisionsByIds(objectMapper, testDecisions.map { it.id })
            assertEquals(emptyList<FeeDecision>(), result)
        }
    }

    @Test
    fun `getInvoiceable`() {
        db.transaction { tx ->
            tx.upsertFeeDecisions(objectMapper, testDecisions)

            val result =
                getInvoiceableFeeDecisions(tx.handle, objectMapper, DateRange(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 31)))
            assertEquals(2, result.size)
        }
    }

    @Test
    fun `searchIgnoresAccent`() {
        db.transaction { tx ->
            tx.upsertFeeDecisions(objectMapper, testDecisions)
            tx.upsertFeeDecisions(
                objectMapper,
                listOf(
                    createFeeDecisionFixture(
                        status = FeeDecisionStatus.DRAFT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_3.id,
                        period = testPeriod,
                        parts = listOf(
                            createFeeDecisionPartFixture(
                                childId = testChild_5.id,
                                dateOfBirth = testChild_5.dateOfBirth,
                                daycareId = testDaycare.id
                            )
                        )
                    )
                )
            )
        }

        searchAndAssert("Viren", "Virén")
        searchAndAssert("Virén", "Virén")
        searchAndAssert("Visa Viren", "Virén")
        searchAndAssert("Visa Virén", "Virén")
    }

    private fun searchAndAssert(searchTerms: String, expectedChildLastName: String) {
        val result = db.read { tx ->
            tx.searchFeeDecisions(
                searchTerms = searchTerms,
                page = 0,
                pageSize = 100,
                statuses = emptyList(),
                areas = emptyList(),
                sortBy = FeeDecisionSortParam.CREATED,
                sortDirection = SortDirection.ASC,
                distinctiveParams = emptyList(),
                unit = null,
                startDate = null,
                endDate = null,
                financeDecisionHandlerId = null
            )
        }
        assertEquals(1, result.data.size)
        assertEquals(expectedChildLastName, result.data.get(0).parts.get(0).child.lastName)
    }
}
