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
        jdbi.handle(::insertGeneralTestFixtures)
    }

    @AfterEach
    fun afterEach() {
        jdbi.handle(::resetDatabase)
    }

    @Test
    fun `getByIds with zero ids`() {
        jdbi.handle { h ->
            upsertFeeDecisions(h, objectMapper, testDecisions)
            val result = getFeeDecisionsByIds(h, objectMapper, listOf())
            assertEquals(0, result.size)
        }
    }

    @Test
    fun `getByIds with one id`() {
        jdbi.handle { h ->
            upsertFeeDecisions(h, objectMapper, testDecisions)
            val result = getFeeDecisionsByIds(h, objectMapper, listOf(testDecisions[0].id))
            assertEquals(1, result.size)
        }
    }

    @Test
    fun `getByIds with two ids`() {
        jdbi.handle { h ->
            upsertFeeDecisions(h, objectMapper, testDecisions)
            val result = getFeeDecisionsByIds(h, objectMapper, listOf(testDecisions[0].id, testDecisions[1].id))
            assertEquals(2, result.size)
        }
    }

    @Test
    fun `getByIds with missing ids`() {
        jdbi.handle { h ->
            upsertFeeDecisions(h, objectMapper, testDecisions)
            val result = getFeeDecisionsByIds(h, objectMapper, listOf(UUID.randomUUID(), UUID.randomUUID()))
            assertEquals(0, result.size)
        }
    }

    @Test
    fun `upsert with existing row`() {
        jdbi.handle { h ->
            upsertFeeDecisions(h, objectMapper, testDecisions)
            val updated = testDecisions[0].copy(decisionNumber = 100)
            upsertFeeDecisions(h, objectMapper, listOf(updated))

            val result = getFeeDecisionsByIds(h, objectMapper, listOf(updated.id))
            assertEquals(100.toLong(), result[0].decisionNumber)
        }
    }

    @Test
    fun `upsert updates parts`() {
        jdbi.handle { h ->
            upsertFeeDecisions(h, objectMapper, testDecisions)
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
            upsertFeeDecisions(h, objectMapper, updated)

            val result = getFeeDecisionsByIds(h, objectMapper, testDecisions.map { it.id })
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
        jdbi.handle { h ->
            upsertFeeDecisions(h, objectMapper, testDecisions)
            upsertFeeDecisions(h, objectMapper, emptyList())

            val result = getFeeDecisionsByIds(h, objectMapper, testDecisions.map { it.id })
            assertEquals(testDecisions.sortedBy { it.status }, result.sortedBy { it.status })
        }
    }

    @Test
    fun `upsert does not affect createdAt`() {
        jdbi.handle { h ->
            upsertFeeDecisions(h, objectMapper, testDecisions)
            val initialCreatedAt = testDecisions[0].createdAt
            val decision = testDecisions[0].copy(decisionNumber = 314159265)
            upsertFeeDecisions(h, objectMapper, listOf(decision))

            val result = getFeeDecisionsByIds(h, objectMapper, listOf(decision.id))
            assertEquals(initialCreatedAt, result[0].createdAt)
        }
    }

    @Test
    fun `getDecision existing id`() {
        jdbi.handle { h ->
            upsertFeeDecisions(h, objectMapper, testDecisions)
            val result = getFeeDecision(h, objectMapper, testDecisions[0].id)
            assertNotNull(result)
            assertEquals(toDetailed(testDecisions[0]), result)
        }
    }

    @Test
    fun `getDecision non-existant id`() {
        jdbi.handle { h ->
            upsertFeeDecisions(h, objectMapper, testDecisions)
            val result = getFeeDecision(h, objectMapper, UUID.randomUUID())
            assertNull(result)
        }
    }

    @Test
    fun `find for head of family`() {
        jdbi.handle { h ->
            upsertFeeDecisions(h, objectMapper, testDecisions)
            val result = findFeeDecisionsForHeadOfFamily(h, objectMapper, testAdult_1.id, testPeriod, null)
            assertEquals(2, result.size)
        }
    }

    @Test
    fun `find for head of family without to date`() {
        jdbi.handle { h ->
            upsertFeeDecisions(h, objectMapper, testDecisions)
            val result =
                findFeeDecisionsForHeadOfFamily(h, objectMapper, testAdult_1.id, DateRange(testPeriod.start, null), null)
            assertEquals(2, result.size)
        }
    }

    @Test
    fun `find for head of family with wrong id`() {
        jdbi.handle { h ->
            upsertFeeDecisions(h, objectMapper, testDecisions)
            val result = findFeeDecisionsForHeadOfFamily(
                h,
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
        jdbi.handle { h ->
            upsertFeeDecisions(h, objectMapper, testDecisions)
            val result =
                findFeeDecisionsForHeadOfFamily(
                    h,
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
        jdbi.handle { h ->
            upsertFeeDecisions(h, objectMapper, testDecisions)
            val result =
                findFeeDecisionsForHeadOfFamily(
                    h,
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
        jdbi.handle { h ->
            val draft = testDecisions.find { it.status == FeeDecisionStatus.DRAFT }!!
            upsertFeeDecisions(h, objectMapper, listOf(draft))
            approveFeeDecisionDraftsForSending(h, listOf(draft.id), testDecisionMaker_1.id)

            val result = getFeeDecision(h, objectMapper, testDecisions[0].id)!!
            assertEquals(FeeDecisionStatus.WAITING_FOR_SENDING, result.status)
            assertEquals(1L, result.decisionNumber)
            assertEquals(testDecisionMaker_1, result.approvedBy)
            assertNotNull(result.approvedAt)
        }
    }

    @Test
    fun `approveDraftsForSending with multiple decisions`() {
        jdbi.handle { h ->
            val decisions = listOf(
                testDecisions[0],
                testDecisions[0].copy(
                    id = UUID.randomUUID(),
                    validFrom = testPeriod.start.minusYears(1),
                    validTo = testPeriod.end!!.minusYears(1)
                )
            )
            upsertFeeDecisions(h, objectMapper, decisions)

            approveFeeDecisionDraftsForSending(h, decisions.map { it.id }, testDecisionMaker_1.id)

            val result = getFeeDecisionsByIds(h, objectMapper, decisions.map { it.id }).sortedBy { it.decisionNumber }
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
        jdbi.handle { h ->
            upsertFeeDecisions(h, objectMapper, testDecisions)
            val before = getFeeDecisionsByIds(h, objectMapper, testDecisions.map { it.id })

            deleteFeeDecisions(h, listOf())

            val after = getFeeDecisionsByIds(h, objectMapper, testDecisions.map { it.id })
            assertEquals(before, after)
        }
    }

    @Test
    fun `delete with one id`() {
        jdbi.handle { h ->
            upsertFeeDecisions(h, objectMapper, testDecisions)
            deleteFeeDecisions(h, listOf(testDecisions[0].id))

            val result = getFeeDecisionsByIds(h, objectMapper, testDecisions.map { it.id })
            assertEquals(testDecisions.drop(1), result)
        }
    }

    @Test
    fun `delete with two ids`() {
        jdbi.handle { h ->
            upsertFeeDecisions(h, objectMapper, testDecisions)
            deleteFeeDecisions(h, testDecisions.map { it.id })

            val result = getFeeDecisionsByIds(h, objectMapper, testDecisions.map { it.id })
            assertEquals(emptyList<FeeDecision>(), result)
        }
    }

    @Test
    fun `getInvoiceable`() {
        jdbi.handle { h ->
            upsertFeeDecisions(h, objectMapper, testDecisions)

            val result =
                getInvoiceableFeeDecisions(h, objectMapper, DateRange(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 31)))
            assertEquals(2, result.size)
        }
    }

    @Test
    fun `searchIgnoresAccent`() {
        jdbi.handle { h ->
            upsertFeeDecisions(h, objectMapper, testDecisions)
            upsertFeeDecisions(
                h, objectMapper,
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

            searchAndAssert("Viren", "Virén")
            searchAndAssert("Virén", "Virén")
            searchAndAssert("Visa Viren", "Virén")
            searchAndAssert("Visa Virén", "Virén")
        }
    }

    private fun searchAndAssert(searchTerms: String, expectedChildLastName: String) {
        val result = jdbi.handle { h ->
            searchFeeDecisions(
                h = h,
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
                feeDecisionManagerId = null
            )
        }
        assertEquals(1, result.second.size)
        assertEquals(expectedChildLastName, result.second.get(0).parts.get(0).child.lastName)
    }
}
