// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.invoicing.domain.ServiceNeed
import fi.espoo.evaka.invoicing.testDecision1
import fi.espoo.evaka.invoicing.testFridgeFamily
import fi.espoo.evaka.invoicing.testParentship
import fi.espoo.evaka.invoicing.testPeriod
import fi.espoo.evaka.invoicing.testPisFridgeChild
import fi.espoo.evaka.invoicing.testPisFridgeChildId
import fi.espoo.evaka.invoicing.testPisFridgeParent
import fi.espoo.evaka.invoicing.testPisFridgeParentId
import fi.espoo.evaka.invoicing.testPisPerson
import fi.espoo.evaka.pis.service.ParentshipService
import fi.espoo.evaka.pis.service.PartnershipService
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.domain.Period
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.springframework.core.env.Environment
import org.springframework.transaction.PlatformTransactionManager
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FeeDecisionGeneratorTest {
    private val personService: PersonService = Mockito.mock(PersonService::class.java)
    private val parentshipService: ParentshipService = Mockito.mock(ParentshipService::class.java)
    private val partnershipService: PartnershipService =
        Mockito.mock(PartnershipService::class.java)
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private val txManager: PlatformTransactionManager = Mockito.mock(PlatformTransactionManager::class.java)
    private val dataSource: DataSource = Mockito.mock(DataSource::class.java)
    private val asyncJobRunner: AsyncJobRunner = Mockito.mock(AsyncJobRunner::class.java)
    private val env: Environment = Mockito.mock(Environment::class.java)

    private lateinit var generator: FeeDecisionGenerator

    @BeforeAll
    fun beforeAll() {
        Mockito.`when`(env.getRequiredProperty("fee_decision_min_date"))
            .thenReturn("2015-01-01")

        generator = FeeDecisionGenerator(
            personService,
            parentshipService,
            partnershipService,
            txManager,
            dataSource,
            objectMapper,
            env
        )
    }

    @Test
    fun `filterOrUpdateStaleDrafts for one decision with overlap in end`() {
        val decision = testDecision1
        val period = Period(decision.validFrom.plusDays(10), decision.validTo)

        val result = filterOrUpdateStaleDrafts(listOf(decision), period)
        assertEquals(1, result.size)

        val updatedDecision = result[0]
        assertEquals(decision.validFrom, updatedDecision.validFrom)
        assertEquals(period.start.minusDays(1), updatedDecision.validTo)
    }

    @Test
    fun `filterOrUpdateStaleDrafts for one decision with overlap in beginning`() {
        val decision = testDecision1
        val period = Period(decision.validFrom, decision.validTo!!.minusDays(10))

        val result = filterOrUpdateStaleDrafts(listOf(decision), period)
        assertEquals(1, result.size)

        val updatedDecision = result[0]
        assertEquals(period.end!!.plusDays(1), updatedDecision.validFrom)
        assertEquals(decision.validTo, updatedDecision.validTo)
    }

    @Test
    fun `filterOrUpdateStaleDrafts filters out decisions that are within date range`() {
        val decision = testDecision1
        val period = Period(decision.validFrom.minusDays(1), decision.validTo!!.plusDays(1))

        val result = filterOrUpdateStaleDrafts(listOf(decision), period)
        assertEquals(0, result.size)
    }

    @Test
    fun `filterOrUpdateStaleDrafts splits decision into two when date range is within a decision date range`() {
        val decision = testDecision1
        val period = Period(decision.validFrom.plusDays(1), decision.validTo!!.minusDays(1))

        val result = filterOrUpdateStaleDrafts(listOf(decision), period)
        assertEquals(2, result.size)

        result[0].let {
            assertEquals(decision.validFrom, it.validFrom)
            assertEquals(period.start.minusDays(1), it.validTo)
        }

        result[1].let {
            assertEquals(period.end!!.plusDays(1), it.validFrom)
            assertEquals(decision.validTo, it.validTo)
        }
    }

    @Test
    fun `filterOrUpdateStaleDrafts handles open end date correctly when decision is within date range`() {
        val decision = testDecision1
        val period = Period(decision.validFrom, null)

        val result = filterOrUpdateStaleDrafts(listOf(decision), period)
        assertEquals(0, result.size)
    }

    @Test
    fun `filterOrUpdateStaleDrafts handles open end date correctly when start date is after decision start`() {
        val decision = testDecision1
        val period = Period(decision.validFrom.plusDays(5), null)

        val result = filterOrUpdateStaleDrafts(listOf(decision), period)
        assertEquals(1, result.size)

        val updatedDecision = result[0]
        assertEquals(decision.validFrom, updatedDecision.validFrom)
        assertEquals(period.start.minusDays(1), updatedDecision.validTo)
    }

    @Test
    fun `findFamiliesByChild handles one parent with one child for single period`() {
        Mockito.`when`(parentshipService.getParentshipsByChildId(testPisFridgeChildId))
            .thenReturn(setOf(testParentship))

        Mockito.`when`(parentshipService.getParentshipsByHeadOfChildId(testPisFridgeParentId))
            .thenReturn(setOf(testParentship))

        val resultFamilies = generator.findFamiliesByChild(testPisFridgeChildId, testPeriod)

        assertEquals(1, resultFamilies.size)
        assertEquals(testFridgeFamily, resultFamilies[0])
    }

    @Test
    fun `findFamiliesByChild handles one child with two periods and parents`() {
        val firstPeriod = testPeriod
        val firstRelation = testParentship

        val parent2 = PersonData.JustId(UUID.randomUUID())
        val secondPeriod = with(firstPeriod) { this.copy(end!!.plusDays(1), end!!.plusDays(11)) }
        val secondRelation = testParentship(testPisPerson(testPisFridgeChild), testPisPerson(parent2), secondPeriod)

        Mockito.`when`(parentshipService.getParentshipsByChildId(testPisFridgeChildId))
            .thenReturn(setOf(firstRelation, secondRelation))

        Mockito.`when`(parentshipService.getParentshipsByHeadOfChildId(firstRelation.headOfChildId))
            .thenReturn(setOf(firstRelation))

        Mockito.`when`(parentshipService.getParentshipsByHeadOfChildId(secondRelation.headOfChildId))
            .thenReturn(setOf(secondRelation))

        val resultFamilies =
            generator.findFamiliesByChild(testPisFridgeChildId, Period(firstPeriod.start, secondPeriod.end))

        val expectedSecondFamily = testFridgeFamily.copy(
            headOfFamily = parent2,
            period = secondPeriod
        )

        assertEquals(2, resultFamilies.size)

        with(resultFamilies[0]) {
            assertEquals(testFridgeFamily.headOfFamily, headOfFamily)
            assertEquals(testFridgeFamily.children, children)
            assertEquals(testPeriod, period)
        }

        with(resultFamilies[1]) {
            assertEquals(expectedSecondFamily.headOfFamily, headOfFamily)
            assertEquals(expectedSecondFamily.children, children)
            assertEquals(secondPeriod, period)
        }
    }

    @Test
    fun `findFamiliesByChild handles one parent with two children of varying periods`() {
        val firstChildPeriod = with(testPeriod) { Period(start, start.plusYears(3)) }
        val secondChildPeriod = with(testPeriod) { Period(start.plusYears(1), start.plusYears(3)) }

        val firstChildId = testPisFridgeChildId
        val secondChildId = UUID.randomUUID()

        val firstChild = testPisFridgeChild
        val secondChild = testPisFridgeChild.copy(id = secondChildId)

        val firstRelation = testParentship(period = firstChildPeriod)
        val secondRelation = testParentship(child = testPisPerson(secondChild), period = secondChildPeriod)

        Mockito.`when`(parentshipService.getParentshipsByChildId(firstChildId))
            .thenReturn(setOf(firstRelation))

        Mockito.`when`(parentshipService.getParentshipsByHeadOfChildId(testPisFridgeParentId))
            .thenReturn(setOf(firstRelation, secondRelation))

        val resultFamilies =
            generator.findFamiliesByChild(firstChildId, Period(firstChildPeriod.start, secondChildPeriod.end))

        val expectedFamilies = listOf(
            testFridgeFamily(
                testPisFridgeParent,
                listOf(firstChild),
                Period(firstChildPeriod.start, secondChildPeriod.start.minusDays(1))
            ),
            testFridgeFamily(testPisFridgeParent, listOf(firstChild, secondChild), secondChildPeriod)
        )

        assertEquals(2, resultFamilies.size)

        with(resultFamilies[0]) {
            assertEquals(expectedFamilies[0].period, period)
            assertEquals(expectedFamilies[0].children, children)
            assertEquals(expectedFamilies[0].headOfFamily, headOfFamily)
        }

        with(resultFamilies[1]) {
            assertEquals(expectedFamilies[1].period, period)
            assertEquals(expectedFamilies[1].children, children)
            assertEquals(expectedFamilies[1].headOfFamily, headOfFamily)
        }
    }

    @Test
    fun `mergeAndFilterUnnecessaryDrafts does not touch drafts with parts`() {
        val drafts = listOf(
            testDecision1.copy(decisionNumber = null),
            testDecision1.copy(
                decisionNumber = null,
                validFrom = testDecision1.validTo!!.plusDays(1),
                validTo = testDecision1.validTo!!.plusDays(30),
                parts = testDecision1.parts.map { it.copy(placement = it.placement.copy(serviceNeed = ServiceNeed.LTE_25)) }
            )
        )

        val result = mergeAndFilterUnnecessaryDrafts(drafts, listOf())

        assertEqualEnoughDecisions(drafts, result)
    }

    @Test
    fun `mergeAndFilterUnnecessaryDrafts filters out empty decisions when no sent`() {
        val drafts = listOf(
            testDecision1.copy(decisionNumber = null),
            testDecision1.copy(
                decisionNumber = null,
                validFrom = testDecision1.validTo!!.plusDays(1),
                validTo = testDecision1.validTo!!.plusDays(30),
                parts = listOf()
            )
        )

        val result = mergeAndFilterUnnecessaryDrafts(drafts, listOf())

        assertEqualEnoughDecisions(drafts.take(1), result)
    }

    @Test
    fun `mergeAndFilterUnnecessaryDrafts merges similar decisions`() {
        val drafts = listOf(
            testDecision1.copy(decisionNumber = null),
            testDecision1.copy(
                decisionNumber = null,
                validFrom = testDecision1.validTo!!.plusDays(1),
                validTo = testDecision1.validTo!!.plusDays(30)
            )
        )

        val result = mergeAndFilterUnnecessaryDrafts(drafts, listOf())

        assertEqualEnoughDecisions(
            drafts.take(1).map { it.copy(validTo = testDecision1.validTo!!.plusDays(30)) },
            result
        )
    }

    @Test
    fun `mergeAndFilterUnnecessaryDrafts does not touch empty drafts with corresponding sent decisions`() {
        val drafts = listOf(
            testDecision1.copy(decisionNumber = null),
            testDecision1.copy(
                decisionNumber = null,
                validFrom = testDecision1.validTo!!.plusDays(1),
                validTo = testDecision1.validTo!!.plusDays(30),
                parts = listOf()
            )
        )

        val sent = listOf(drafts[1].copy(status = FeeDecisionStatus.SENT, parts = testDecision1.parts))

        val result = mergeAndFilterUnnecessaryDrafts(drafts, sent)

        assertEqualEnoughDecisions(drafts, result)
    }

    @Test
    fun `mergeAndFilterUnnecessaryDrafts only keeps a part of empty draft that overlaps with an sent decision`() {
        val drafts = listOf(testDecision1.copy(decisionNumber = null, parts = listOf()))

        val sentDecisionPeriod = Period(drafts[0].validFrom.plusDays(7), drafts[0].validFrom.plusDays(14))
        val sent = listOf(
            drafts[0].copy(
                status = FeeDecisionStatus.SENT,
                validFrom = sentDecisionPeriod.start,
                validTo = sentDecisionPeriod.end,
                parts = testDecision1.parts.map { it.copy(placement = it.placement.copy(serviceNeed = ServiceNeed.LTE_25)) }
            )
        )

        val result = mergeAndFilterUnnecessaryDrafts(drafts, sent)

        assertEqualEnoughDecisions(
            drafts.take(1).map {
                it.copy(
                    validFrom = sentDecisionPeriod.start,
                    validTo = sentDecisionPeriod.end
                )
            },
            result
        )
    }

    @Test
    fun `mergeAndFilterUnnecessaryDrafts filters out drafts that are equal enough with an sent decision`() {
        val drafts = listOf(testDecision1.copy(decisionNumber = null))
        val sent = listOf(testDecision1.copy(decisionNumber = null, status = FeeDecisionStatus.SENT))

        val result = mergeAndFilterUnnecessaryDrafts(drafts, sent)

        assertEquals(0, result.size)
    }

    fun assertEqualEnoughDecisions(expected: List<FeeDecision>, actual: List<FeeDecision>) {
        val createdAt = Instant.now()
        UUID.randomUUID().let { uuid ->
            assertEquals(expected.map { it.copy(id = uuid, createdAt = createdAt) }, actual.map { it.copy(id = uuid, createdAt = createdAt) })
        }
    }
}
