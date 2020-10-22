// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.createFeeDecisionFixture
import fi.espoo.evaka.invoicing.createFeeDecisionPartFixture
import fi.espoo.evaka.invoicing.data.feeDecisionQueryBase
import fi.espoo.evaka.invoicing.data.getPricing
import fi.espoo.evaka.invoicing.data.toFeeDecision
import fi.espoo.evaka.invoicing.data.toVoucherValueDecision
import fi.espoo.evaka.invoicing.data.upsertFeeDecisions
import fi.espoo.evaka.invoicing.data.voucherValueDecisionQueryBase
import fi.espoo.evaka.invoicing.domain.DecisionIncome
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.IncomeCoefficient
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.IncomeType
import fi.espoo.evaka.invoicing.domain.IncomeValue
import fi.espoo.evaka.invoicing.domain.PlacementType
import fi.espoo.evaka.invoicing.domain.ServiceNeed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.domain.merge
import fi.espoo.evaka.invoicing.oldTestPricing
import fi.espoo.evaka.invoicing.testPricing
import fi.espoo.evaka.placement.PlacementType.CLUB
import fi.espoo.evaka.placement.PlacementType.DAYCARE
import fi.espoo.evaka.placement.PlacementType.PREPARATORY_DAYCARE
import fi.espoo.evaka.placement.PlacementType.PRESCHOOL_DAYCARE
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestChild
import fi.espoo.evaka.shared.dev.insertTestFeeAlteration
import fi.espoo.evaka.shared.dev.insertTestIncome
import fi.espoo.evaka.shared.dev.insertTestParentship
import fi.espoo.evaka.shared.dev.insertTestPartnership
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.insertTestServiceNeed
import fi.espoo.evaka.shared.domain.Period
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testClub
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycareNotInvoiced
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testVoucherDaycare
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class DecisionGeneratorIntegrationTest : FullApplicationTest() {
    @Autowired
    private lateinit var generator: DecisionGenerator

    @BeforeEach
    fun beforeEach() {
        jdbi.handle(::insertGeneralTestFixtures)
    }

    @AfterEach
    fun afterEach() {
        jdbi.handle(::resetDatabase)
    }

    @Test
    fun `new placement does not create fee decisions when child is missing head of family`() {
        val placementPeriod = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, DAYCARE, testDaycare.id)

        generator.handlePlacement(testChild_1.id, placementPeriod)

        val decisions = getAllFeeDecisions()
        assertTrue(decisions.isEmpty())
    }

    @Test
    fun `new placement to a net budgeted unit does not generate a fee decision`() {
        val placementPeriod = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, DAYCARE, testDaycareNotInvoiced.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)

        generator.handlePlacement(testChild_1.id, placementPeriod)

        val decisions = getAllFeeDecisions()
        assertEquals(0, decisions.size)
    }

    @Test
    fun `placing a child to a net budgeted unit and their sibling to a normal unit will result in only a single fee decision part`() {
        val placementPeriod = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, DAYCARE, testDaycareNotInvoiced.id)
        insertPlacement(testChild_2.id, placementPeriod, DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, testChild_2.id), placementPeriod)

        generator.handlePlacement(testChild_1.id, placementPeriod)

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions[0].parts.size)
    }

    @Test
    fun `child in a club does not affect fee decisions for siblings in any way`() {
        val placementPeriod = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, CLUB, testClub.id!!)
        insertPlacement(testChild_2.id, placementPeriod, DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, testChild_2.id), placementPeriod)

        generator.handlePlacement(testChild_1.id, placementPeriod)

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions[0].parts.size)
        decisions[0].parts[0].let { part ->
            assertEquals(0, part.siblingDiscount)
        }
    }

    @Test
    fun `new placement creates new fee decision when no existing decision`() {
        val placementPeriod = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)

        generator.handlePlacement(testChild_1.id, placementPeriod)

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
    }

    @Test
    fun `new placement overrides existing fee decision`() {
        val placementPeriod = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)

        generator.handlePlacement(testChild_1.id, placementPeriod)

        val original = getAllFeeDecisions()
        assertEquals(1, original.size)

        generator.handlePlacement(testChild_1.id, placementPeriod)

        val result = getAllFeeDecisions()
        assertEquals(1, original.size)
        assertEqualEnoughDecisions(original, result)
    }

    @Test
    fun `drafts not overlapping with the period are not touched when generating new decisions`() {
        val periodInPast = Period(LocalDate.of(2015, 1, 1), LocalDate.of(2015, 12, 31))
        val oldDraft = createFeeDecisionFixture(
            FeeDecisionStatus.DRAFT,
            FeeDecisionType.NORMAL,
            periodInPast,
            testAdult_1.id,
            listOf(createFeeDecisionPartFixture(testChild_2.id, LocalDate.of(2010, 1, 1), testDaycare.id))
        )
        jdbi.handle { h -> upsertFeeDecisions(h, objectMapper, listOf(oldDraft)) }

        val placementPeriod = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)

        generator.handlePlacement(testChild_1.id, placementPeriod)

        val result = getAllFeeDecisions()
        assertEquals(2, result.size)
        result.first().let {
            assertEquals(periodInPast.start, it.validFrom)
            assertEquals(periodInPast.end, it.validTo)
            assertEquals(testAdult_1.id, it.headOfFamily.id)
            assertEquals(oldDraft.parts, it.parts)
        }
        result.last().let {
            assertEquals(placementPeriod.start, it.validFrom)
            assertEquals(placementPeriod.end, it.validTo)
            assertEquals(testAdult_1.id, it.headOfFamily.id)
        }
    }

    @Test
    fun `fee decision placement infers PRESCHOOL_WITH_DAYCARE from placement correctly`() {
        val placementPeriod = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, PRESCHOOL_DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)
        insertServiceNeed(testChild_1.id, listOf(placementPeriod to 30.0))

        generator.handlePlacement(testChild_1.id, placementPeriod)

        val result = getAllFeeDecisions()

        assertEquals(PlacementType.PRESCHOOL_WITH_DAYCARE, result[0].parts[0].placement.type)
    }

    @Test
    fun `fee decision placement infers PREPARATORY_WITH_DAYCARE from placement correctly`() {
        val placementPeriod = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, PREPARATORY_DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)
        insertServiceNeed(testChild_1.id, listOf(placementPeriod to 30.0))

        generator.handlePlacement(testChild_1.id, placementPeriod)

        val result = getAllFeeDecisions()

        assertEquals(PlacementType.PREPARATORY_WITH_DAYCARE, result[0].parts[0].placement.type)
    }

    @Test
    fun `fee decision placement infers FIVE_YEARS_OLD_DAYCARE from age correctly`() {
        val fiveYearOld = jdbi.handle { h ->
            val id = h.insertTestPerson(DevPerson(dateOfBirth = LocalDate.of(2014, 1, 1)))
            h.insertTestChild(DevChild(id))
            id
        }

        val placementPeriod = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(fiveYearOld, placementPeriod, DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(fiveYearOld), placementPeriod)
        insertServiceNeed(fiveYearOld, listOf(placementPeriod to 30.0))

        generator.handlePlacement(fiveYearOld, placementPeriod)

        val result = getAllFeeDecisions()

        assertEquals(2, result.size)
        result[0].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(placementPeriod.start, decision.validFrom)
            assertEquals(LocalDate.of(2019, 7, 31), decision.validTo)
            assertEquals(23100, decision.totalFee())
            assertEquals(1, decision.parts.size)
            decision.parts[0].let { part ->
                assertEquals(fiveYearOld, part.child.id)
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.GT_25_LT_35, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(23100, part.fee)
                assertEquals(23100, part.finalFee())
            }
        }
        result[1].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(LocalDate.of(2019, 8, 1), decision.validFrom)
            assertEquals(placementPeriod.end, decision.validTo)
            assertEquals(10100, decision.totalFee())
            assertEquals(1, decision.parts.size)
            decision.parts[0].let { part ->
                assertEquals(fiveYearOld, part.child.id)
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.FIVE_YEARS_OLD_DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.LTE_15, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(10100, part.fee)
                assertEquals(10100, part.finalFee())
            }
        }
    }

    @Test
    fun `new service need updates existing draft`() {
        val placementPeriod = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)

        generator.handlePlacement(testChild_1.id, placementPeriod)

        val original = getAllFeeDecisions()
        assertEquals(1, original.size)
        with(original[0]) {
            assertEquals(listOf(ServiceNeed.MISSING), parts.map { it.placement.serviceNeed })
        }

        val serviceNeed = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31)) to 20.0
        insertServiceNeed(testChild_1.id, listOf(serviceNeed))
        generator.handleServiceNeed(testChild_1.id, serviceNeed.first)

        val updated = getAllFeeDecisions()
        assertEquals(1, updated.size)
        with(updated[0]) {
            assertEquals(listOf(ServiceNeed.LTE_25), parts.map { it.placement.serviceNeed })
        }
    }

    @Test
    fun `new service need that covers existing decision splits it`() {
        val placementPeriod = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)

        generator.handlePlacement(testChild_1.id, placementPeriod)

        val original = getAllFeeDecisions()
        assertEquals(1, original.size)
        with(original[0]) {
            assertEquals(listOf(ServiceNeed.MISSING), parts.map { it.placement.serviceNeed })
        }

        val serviceNeed = Period(LocalDate.of(2019, 7, 1), null) to 20.0
        insertServiceNeed(testChild_1.id, listOf(serviceNeed))
        generator.handleServiceNeed(testChild_1.id, serviceNeed.first)

        val updated = getAllFeeDecisions()
        assertEquals(2, updated.size)
        with(updated[0]) {
            assertEquals(listOf(ServiceNeed.MISSING), parts.map { it.placement.serviceNeed })
        }
        with(updated[1]) {
            assertEquals(listOf(ServiceNeed.LTE_25), parts.map { it.placement.serviceNeed })
        }
    }

    @Test
    fun `deleted service need that split existing decision merges decisions afterwards`() {
        val serviceNeed = Period(LocalDate.of(2019, 7, 1), null) to 20.0
        val placementPeriod = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)
        insertServiceNeed(testChild_1.id, listOf(serviceNeed))

        generator.handlePlacement(testChild_1.id, placementPeriod)

        val original = getAllFeeDecisions()
        assertEquals(2, original.size)
        with(original[0]) {
            assertEquals(listOf(ServiceNeed.MISSING), parts.map { it.placement.serviceNeed })
        }
        with(original[1]) {
            assertEquals(listOf(ServiceNeed.LTE_25), parts.map { it.placement.serviceNeed })
        }

        jdbi.handle { h ->
            h.createUpdate("DELETE FROM service_need WHERE child_id = :childId")
                .bind("childId", testChild_1.id)
                .execute()
        }
        generator.handleServiceNeed(testChild_1.id, serviceNeed.first)

        val updated = getAllFeeDecisions()
        assertEquals(1, updated.size)
        with(updated[0]) {
            assertEquals(listOf(ServiceNeed.MISSING), parts.map { it.placement.serviceNeed })
        }
    }

    @Test
    fun `get pricing works when from is same as validFrom`() {
        val from = LocalDate.of(2000, 1, 1)
        val result = jdbi.handle { getPricing(it, from) }

        assertEquals(1, result.size)
        result[0].let { (period, pricing) ->
            assertEquals(from, period.start)
            assertEquals(null, period.end)
            assertEquals(BigDecimal("0.1070"), pricing.multiplier)
        }
    }

    @Test
    fun `get pricing works as expected when from is before any pricing configuration`() {
        val from = LocalDate.of(1990, 1, 1)
        val result = jdbi.handle { getPricing(it, from) }

        assertEquals(1, result.size)
        result[0].let { (period, pricing) ->
            assertEquals(LocalDate.of(2000, 1, 1), period.start)
            assertEquals(null, period.end)
            assertEquals(BigDecimal("0.1070"), pricing.multiplier)
        }
    }

    @Test
    fun `new fee decisions are not generated if children have no placements`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)

        generator.handleFeeAlterationChange(testChild_1.id, period)

        val decisions = getAllFeeDecisions()
        assertEquals(0, decisions.size)
    }

    @Test
    fun `new fee decisions are generated if children have no placements but there exists an sent decision for same head of family`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        jdbi.handle { h ->
            upsertFeeDecisions(
                h,
                objectMapper,
                listOf(
                    createFeeDecisionFixture(
                        status = FeeDecisionStatus.SENT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_1.id,
                        period = period,
                        parts = listOf(
                            createFeeDecisionPartFixture(
                                childId = testChild_1.id,
                                dateOfBirth = testChild_1.dateOfBirth,
                                daycareId = testDaycare.id
                            )
                        )
                    )
                )
            )
        }

        generator.handleFamilyUpdate(testAdult_1.id, period)

        val decisions = getAllFeeDecisions()
        assertEquals(2, decisions.size)
        assertEquals(1, decisions.filter { it.status == FeeDecisionStatus.DRAFT }.size)
        assertTrue(decisions.first { it.status == FeeDecisionStatus.DRAFT }.parts.isEmpty())
    }

    @Test
    fun `new empty fee decision is generated if head of family has no children but there exists an sent decision for same head of family`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        jdbi.handle { h ->
            upsertFeeDecisions(
                h,
                objectMapper,
                listOf(
                    createFeeDecisionFixture(
                        status = FeeDecisionStatus.SENT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_1.id,
                        period = period,
                        parts = listOf(
                            createFeeDecisionPartFixture(
                                childId = testChild_1.id,
                                dateOfBirth = testChild_1.dateOfBirth,
                                daycareId = testDaycare.id
                            )
                        )
                    )
                )
            )
        }

        generator.handleFamilyUpdate(testAdult_1.id, period)

        val decisions = getAllFeeDecisions()
        assertEquals(2, decisions.size)
        assertEquals(1, decisions.filter { it.status == FeeDecisionStatus.DRAFT }.size)
        assertTrue(decisions.first { it.status == FeeDecisionStatus.DRAFT }.parts.isEmpty())
    }

    @Test
    fun `fee decision is formed correctly for two children with 45h preschool with daycare`() {
        val placementPeriod = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, PRESCHOOL_DAYCARE, testDaycare.id)
        insertServiceNeed(testChild_1.id, listOf(Period(placementPeriod.start, null) to 45.0))
        insertPlacement(testChild_2.id, placementPeriod, PRESCHOOL_DAYCARE, testDaycare.id)
        insertServiceNeed(testChild_2.id, listOf(Period(placementPeriod.start, null) to 45.0))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, testChild_2.id), placementPeriod)

        generator.handleServiceNeed(testChild_1.id, placementPeriod)

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions.first().let { decision ->
            assertEquals(23100 + 11600, decision.totalFee())
            assertEquals(2, decision.parts.size)
            decision.parts.first().let { part ->
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.PRESCHOOL_WITH_DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.GTE_25, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(23100, part.fee)
                assertEquals(23100, part.finalFee())
            }
            decision.parts.last().let { part ->
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.PRESCHOOL_WITH_DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.GTE_25, part.placement.serviceNeed)
                assertEquals(50, part.siblingDiscount)
                assertEquals(11600, part.fee)
                assertEquals(11600, part.finalFee())
            }
        }
    }

    @Test
    fun `fee decision is formed correctly for two children with 36h preschool with daycare`() {
        val placementPeriod = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, PRESCHOOL_DAYCARE, testDaycare.id)
        insertServiceNeed(testChild_1.id, listOf(placementPeriod to 36.0))
        insertPlacement(testChild_2.id, placementPeriod, PRESCHOOL_DAYCARE, testDaycare.id)
        insertServiceNeed(testChild_2.id, listOf(placementPeriod to 36.0))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, testChild_2.id), placementPeriod)

        generator.handleServiceNeed(testChild_1.id, placementPeriod)

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions.first().let { decision ->
            assertEquals(17300 + 8700, decision.totalFee())
            assertEquals(2, decision.parts.size)
            decision.parts.first().let { part ->
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.PRESCHOOL_WITH_DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.GT_15_LT_25, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(17300, part.fee)
                assertEquals(17300, part.finalFee())
            }
            decision.parts.last().let { part ->
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.PRESCHOOL_WITH_DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.GT_15_LT_25, part.placement.serviceNeed)
                assertEquals(50, part.siblingDiscount)
                assertEquals(8700, part.fee)
                assertEquals(8700, part.finalFee())
            }
        }
    }

    @Test
    fun `fee decision is formed correctly for two children with 35h preschool with daycare`() {
        val placementPeriod = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, PRESCHOOL_DAYCARE, testDaycare.id)
        insertServiceNeed(testChild_1.id, listOf(placementPeriod to 35.0))
        insertPlacement(testChild_2.id, placementPeriod, PRESCHOOL_DAYCARE, testDaycare.id)
        insertServiceNeed(testChild_2.id, listOf(placementPeriod to 35.0))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, testChild_2.id), placementPeriod)

        generator.handleServiceNeed(testChild_1.id, placementPeriod)

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions.first().let { decision ->
            assertEquals(10100 + 5100, decision.totalFee())
            assertEquals(2, decision.parts.size)
            decision.parts.first().let { part ->
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.PRESCHOOL_WITH_DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.LTE_15, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(10100, part.fee)
                assertEquals(10100, part.finalFee())
            }
            decision.parts.last().let { part ->
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.PRESCHOOL_WITH_DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.LTE_15, part.placement.serviceNeed)
                assertEquals(50, part.siblingDiscount)
                assertEquals(5100, part.fee)
                assertEquals(5100, part.finalFee())
            }
        }
    }

    @Test
    fun `fee decision is formed correctly for two children with one having 20h preschool with daycare`() {
        val placementPeriod = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, PRESCHOOL_DAYCARE, testDaycare.id)
        insertServiceNeed(testChild_1.id, listOf(Period(placementPeriod.start, null) to 20.0))
        insertPlacement(testChild_2.id, placementPeriod, PRESCHOOL_DAYCARE, testDaycare.id)
        insertServiceNeed(testChild_2.id, listOf(Period(placementPeriod.start, null) to 30.0))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, testChild_2.id), placementPeriod)

        generator.handleServiceNeed(testChild_1.id, placementPeriod)

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions.first().let { decision ->
            assertEquals(5100, decision.totalFee())
            assertEquals(1, decision.parts.size)
            decision.parts.first().let { part ->
                assertEquals(testChild_2.id, part.child.id)
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.PRESCHOOL_WITH_DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.LTE_15, part.placement.serviceNeed)
                assertEquals(50, part.siblingDiscount)
                assertEquals(5100, part.fee)
                assertEquals(5100, part.finalFee())
            }
        }
    }

    @Test
    fun `fee decision is formed correctly for two children with 50h preparatory education with daycare`() {
        val placementPeriod = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, PREPARATORY_DAYCARE, testDaycare.id)
        insertServiceNeed(
            testChild_1.id,
            listOf(placementPeriod to 50.0)
        )
        insertPlacement(testChild_2.id, placementPeriod, PREPARATORY_DAYCARE, testDaycare.id)
        insertServiceNeed(
            testChild_2.id,
            listOf(placementPeriod to 50.0)
        )
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, testChild_2.id), placementPeriod)

        generator.handleServiceNeed(testChild_1.id, placementPeriod)

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions.first().let { decision ->
            assertEquals(23100 + 11600, decision.totalFee())
            assertEquals(2, decision.parts.size)
            decision.parts.first().let { part ->
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.PREPARATORY_WITH_DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.GTE_25, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(23100, part.fee)
                assertEquals(23100, part.finalFee())
            }
            decision.parts.last().let { part ->
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.PREPARATORY_WITH_DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.GTE_25, part.placement.serviceNeed)
                assertEquals(50, part.siblingDiscount)
                assertEquals(11600, part.fee)
                assertEquals(11600, part.finalFee())
            }
        }
    }

    @Test
    fun `fee decision is formed correctly for two children with 41h preparatory education with daycare`() {
        val placementPeriod = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, PREPARATORY_DAYCARE, testDaycare.id)
        insertServiceNeed(
            testChild_1.id,
            listOf(placementPeriod to 41.0)
        )
        insertPlacement(testChild_2.id, placementPeriod, PREPARATORY_DAYCARE, testDaycare.id)
        insertServiceNeed(
            testChild_2.id,
            listOf(placementPeriod to 41.0)
        )
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, testChild_2.id), placementPeriod)

        generator.handleServiceNeed(testChild_1.id, placementPeriod)

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions.first().let { decision ->
            assertEquals(17300 + 8700, decision.totalFee())
            assertEquals(2, decision.parts.size)
            decision.parts.first().let { part ->
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.PREPARATORY_WITH_DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.GT_15_LT_25, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(17300, part.fee)
                assertEquals(17300, part.finalFee())
            }
            decision.parts.last().let { part ->
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.PREPARATORY_WITH_DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.GT_15_LT_25, part.placement.serviceNeed)
                assertEquals(50, part.siblingDiscount)
                assertEquals(8700, part.fee)
                assertEquals(8700, part.finalFee())
            }
        }
    }

    @Test
    fun `fee decision is formed correctly for two children with 40h preparatory education with daycare`() {
        val placementPeriod = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, PREPARATORY_DAYCARE, testDaycare.id)
        insertServiceNeed(
            testChild_1.id,
            listOf(placementPeriod to 40.0)
        )
        insertPlacement(testChild_2.id, placementPeriod, PREPARATORY_DAYCARE, testDaycare.id)
        insertServiceNeed(
            testChild_2.id,
            listOf(placementPeriod to 40.0)
        )
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, testChild_2.id), placementPeriod)

        generator.handleServiceNeed(testChild_1.id, placementPeriod)

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions.first().let { decision ->
            assertEquals(10100 + 5100, decision.totalFee())
            assertEquals(2, decision.parts.size)
            decision.parts.first().let { part ->
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.PREPARATORY_WITH_DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.LTE_15, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(10100, part.fee)
                assertEquals(10100, part.finalFee())
            }
            decision.parts.last().let { part ->
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.PREPARATORY_WITH_DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.LTE_15, part.placement.serviceNeed)
                assertEquals(50, part.siblingDiscount)
                assertEquals(5100, part.fee)
                assertEquals(5100, part.finalFee())
            }
        }
    }

    @Test
    fun `fee decision is formed correctly for two children with 30h preparatory education with daycare`() {
        val placementPeriod = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, PREPARATORY_DAYCARE, testDaycare.id)
        insertServiceNeed(
            testChild_1.id,
            listOf(Period(placementPeriod.start, null) to 30.0)
        )
        insertPlacement(testChild_2.id, placementPeriod, PREPARATORY_DAYCARE, testDaycare.id)
        insertServiceNeed(
            testChild_2.id,
            listOf(Period(placementPeriod.start, null) to 30.0)
        )
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, testChild_2.id), placementPeriod)

        generator.handleServiceNeed(testChild_1.id, placementPeriod)

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions.first().let { decision ->
            assertEquals(10100 + 5100, decision.totalFee())
            assertEquals(2, decision.parts.size)
            decision.parts.first().let { part ->
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.PREPARATORY_WITH_DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.LTE_15, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(10100, part.fee)
                assertEquals(10100, part.finalFee())
            }
            decision.parts.last().let { part ->
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.PREPARATORY_WITH_DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.LTE_15, part.placement.serviceNeed)
                assertEquals(50, part.siblingDiscount)
                assertEquals(5100, part.fee)
                assertEquals(5100, part.finalFee())
            }
        }
    }

    @Test
    fun `fee decision is formed correctly for two children with one having 25h preparatory education with daycare`() {
        val placementPeriod = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, PREPARATORY_DAYCARE, testDaycare.id)
        insertServiceNeed(
            testChild_1.id,
            listOf(placementPeriod to 25.0)
        )
        insertPlacement(testChild_2.id, placementPeriod, PREPARATORY_DAYCARE, testDaycare.id)
        insertServiceNeed(
            testChild_2.id,
            listOf(placementPeriod to 35.0)
        )
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, testChild_2.id), placementPeriod)

        generator.handleServiceNeed(testChild_1.id, placementPeriod)

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions.first().let { decision ->
            assertEquals(5100, decision.totalFee())
            assertEquals(1, decision.parts.size)
            decision.parts.first().let { part ->
                assertEquals(testChild_2.id, part.child.id)
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.PREPARATORY_WITH_DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.LTE_15, part.placement.serviceNeed)
                assertEquals(50, part.siblingDiscount)
                assertEquals(5100, part.fee)
                assertEquals(5100, part.finalFee())
            }
        }
    }

    @Test
    fun `children over 18 years old are not included in families when determining base fee`() {
        val placementPeriod = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val birthday = LocalDate.of(2001, 7, 1)
        val childTurning18Id = jdbi.handle { it.insertTestPerson(DevPerson(dateOfBirth = birthday)) }

        insertPlacement(testChild_1.id, placementPeriod, DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, childTurning18Id), placementPeriod)
        insertIncome(testAdult_1.id, 330000, placementPeriod)

        generator.handleServiceNeed(testChild_1.id, placementPeriod)

        val decisions = getAllFeeDecisions()
        assertEquals(2, decisions.size)
        decisions.first().let { decision ->
            assertEquals(3, decision.familySize)
            assertEquals(6300, decision.totalFee())
            assertEquals(1, decision.parts.size)
            decision.parts.first().let { part ->
                assertEquals(6300, part.baseFee)
                assertEquals(PlacementType.DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.MISSING, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(6300, part.fee)
                assertEquals(6300, part.finalFee())
            }
        }
        decisions.last().let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(12800, decision.totalFee())
            assertEquals(1, decision.parts.size)
            decision.parts.first().let { part ->
                assertEquals(12800, part.baseFee)
                assertEquals(PlacementType.DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.MISSING, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(12800, part.fee)
                assertEquals(12800, part.finalFee())
            }
        }
    }

    @Test
    fun `new fee decisions are not generated if new draft would be equal to already existing sent decision`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        jdbi.handle { h ->
            upsertFeeDecisions(
                h,
                objectMapper,
                listOf(
                    createFeeDecisionFixture(
                        status = FeeDecisionStatus.SENT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_1.id,
                        period = period,
                        parts = listOf(
                            createFeeDecisionPartFixture(
                                childId = testChild_1.id,
                                dateOfBirth = testChild_1.dateOfBirth,
                                daycareId = testDaycare.id,
                                serviceNeed = ServiceNeed.MISSING
                            )
                        )
                    )
                )
            )
        }

        generator.handleFamilyUpdate(testAdult_1.id, period)

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        assertEquals(0, decisions.filter { it.status == FeeDecisionStatus.DRAFT }.size)
    }

    @Test
    fun `new fee decisions are not generated if new draft would be equal to already existing sent decisions that cover the period`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        jdbi.handle { h ->
            upsertFeeDecisions(
                h,
                objectMapper,
                listOf(
                    createFeeDecisionFixture(
                        status = FeeDecisionStatus.SENT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_1.id,
                        period = period.copy(end = period.start.plusDays(7)),
                        parts = listOf(
                            createFeeDecisionPartFixture(
                                childId = testChild_1.id,
                                dateOfBirth = testChild_1.dateOfBirth,
                                daycareId = testDaycare.id,
                                serviceNeed = ServiceNeed.MISSING
                            )
                        )
                    ),
                    createFeeDecisionFixture(
                        status = FeeDecisionStatus.SENT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_1.id,
                        period = period.copy(start = period.start.plusDays(8)),
                        parts = listOf(
                            createFeeDecisionPartFixture(
                                childId = testChild_1.id,
                                dateOfBirth = testChild_1.dateOfBirth,
                                daycareId = testDaycare.id,
                                serviceNeed = ServiceNeed.MISSING
                            )
                        )
                    )
                )
            )
        }

        generator.handleFamilyUpdate(testAdult_1.id, period)

        val decisions = getAllFeeDecisions()
        assertEquals(2, decisions.size)
        assertEquals(0, decisions.filter { it.status == FeeDecisionStatus.DRAFT }.size)
    }

    @Test
    fun `new fee decisions are generated if new draft would be equal to already existing sent decision but a new draft that is valid before it is generated`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        val shorterPeriod = period.copy(end = period.start.plusDays(30))
        insertServiceNeed(testChild_1.id, listOf(shorterPeriod to 20.0))
        jdbi.handle { h ->
            upsertFeeDecisions(
                h,
                objectMapper,
                listOf(
                    createFeeDecisionFixture(
                        status = FeeDecisionStatus.SENT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_1.id,
                        period = period,
                        parts = listOf(
                            createFeeDecisionPartFixture(
                                childId = testChild_1.id,
                                dateOfBirth = testChild_1.dateOfBirth,
                                daycareId = testDaycare.id,
                                serviceNeed = ServiceNeed.MISSING
                            )
                        )
                    )
                )
            )
        }

        generator.handleFamilyUpdate(testAdult_1.id, period)

        val decisions = getAllFeeDecisions()
        assertEquals(3, decisions.size)

        val drafts = decisions.filter { it.status == FeeDecisionStatus.DRAFT }
        assertEquals(2, drafts.size)
        drafts.first().let { draft ->
            assertEquals(shorterPeriod.start, draft.validFrom)
            assertEquals(shorterPeriod.end, draft.validTo)
            assertEquals(17300, draft.totalFee())
            assertEquals(1, draft.parts.size)
            draft.parts.first().let { part ->
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.LTE_25, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(17300, part.fee)
                assertEquals(17300, part.finalFee())
            }
        }
        drafts.last().let { draft ->
            assertEquals(shorterPeriod.end!!.plusDays(1), draft.validFrom)
            assertEquals(period.end, draft.validTo)
            assertEquals(28900, draft.totalFee())
            assertEquals(1, draft.parts.size)
            draft.parts.first().let { part ->
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.MISSING, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(28900, part.fee)
                assertEquals(28900, part.finalFee())
            }
        }
    }

    @Test
    fun `new fee decision is generated even if it is equal to existing sent decision from it's start but there is a existing draft before it`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        val olderPeriod = period.copy(end = period.start.plusDays(30))
        insertServiceNeed(testChild_1.id, listOf(olderPeriod to 20.0))
        val newerPeriod = period.copy(start = olderPeriod.end!!.plusDays(1))
        jdbi.handle { h ->
            upsertFeeDecisions(
                h,
                objectMapper,
                listOf(
                    createFeeDecisionFixture(
                        status = FeeDecisionStatus.SENT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_1.id,
                        period = period,
                        parts = listOf(
                            createFeeDecisionPartFixture(
                                childId = testChild_1.id,
                                dateOfBirth = testChild_1.dateOfBirth,
                                daycareId = testDaycare.id,
                                serviceNeed = ServiceNeed.MISSING
                            )
                        )
                    ),
                    createFeeDecisionFixture(
                        status = FeeDecisionStatus.DRAFT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_1.id,
                        period = olderPeriod,
                        parts = listOf(
                            createFeeDecisionPartFixture(
                                childId = testChild_1.id,
                                dateOfBirth = testChild_1.dateOfBirth,
                                daycareId = testDaycare.id,
                                serviceNeed = ServiceNeed.LTE_25
                            )
                        )
                    ),
                    createFeeDecisionFixture(
                        status = FeeDecisionStatus.DRAFT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_1.id,
                        period = newerPeriod,
                        parts = listOf(
                            createFeeDecisionPartFixture(
                                childId = testChild_1.id,
                                dateOfBirth = testChild_1.dateOfBirth,
                                daycareId = testDaycare.id,
                                serviceNeed = ServiceNeed.MISSING
                            )
                        )
                    )
                )
            )
        }

        generator.handleFamilyUpdate(testAdult_1.id, newerPeriod)

        val decisions = getAllFeeDecisions()
        assertEquals(3, decisions.size)

        val drafts = decisions.filter { it.status == FeeDecisionStatus.DRAFT }
        assertEquals(2, drafts.size)
        drafts.first().let { draft ->
            assertEquals(olderPeriod.start, draft.validFrom)
            assertEquals(olderPeriod.end, draft.validTo)
            assertEquals(1, draft.parts.size)
            draft.parts.first().let { part ->
                assertEquals(PlacementType.DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.LTE_25, part.placement.serviceNeed)
            }
        }
        drafts.last().let { draft ->
            assertEquals(newerPeriod.start, draft.validFrom)
            assertEquals(newerPeriod.end, draft.validTo)
            assertEquals(1, draft.parts.size)
            draft.parts.first().let { part ->
                assertEquals(PlacementType.DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.MISSING, part.placement.serviceNeed)
            }
        }
    }

    @Test
    fun `fee decision generation works as expected with multiple children with partially overlapping placements`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, testChild_2.id, testChild_3.id), period)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertPlacement(testChild_2.id, period.copy(start = period.start.plusMonths(1)), DAYCARE, testDaycare.id)
        insertPlacement(testChild_3.id, period.copy(start = period.start.plusMonths(2)), DAYCARE, testDaycare.id)

        generator.handleFamilyUpdate(testAdult_1.id, period)

        val decisions = getAllFeeDecisions()
        assertEquals(3, decisions.size)
        decisions[0].let { decision ->
            assertEquals(4, decision.familySize)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.start.plusMonths(1).minusDays(1), decision.validTo)
            assertEquals(28900, decision.totalFee())
            assertEquals(1, decision.parts.size)
            decision.parts[0].let { part ->
                assertEquals(testChild_1.id, part.child.id)
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.MISSING, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(28900, part.fee)
                assertEquals(28900, part.finalFee())
            }
        }
        decisions[1].let { decision ->
            assertEquals(4, decision.familySize)
            assertEquals(period.start.plusMonths(1), decision.validFrom)
            assertEquals(period.start.plusMonths(2).minusDays(1), decision.validTo)
            assertEquals(43400, decision.totalFee())
            assertEquals(2, decision.parts.size)
            decision.parts[0].let { part ->
                assertEquals(testChild_1.id, part.child.id)
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.MISSING, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(28900, part.fee)
                assertEquals(28900, part.finalFee())
            }
            decision.parts[1].let { part ->
                assertEquals(testChild_2.id, part.child.id)
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.MISSING, part.placement.serviceNeed)
                assertEquals(50, part.siblingDiscount)
                assertEquals(14500, part.fee)
                assertEquals(14500, part.finalFee())
            }
        }
        decisions[2].let { decision ->
            assertEquals(4, decision.familySize)
            assertEquals(period.start.plusMonths(2), decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(49200, decision.totalFee())
            assertEquals(3, decision.parts.size)
            decision.parts[0].let { part ->
                assertEquals(testChild_3.id, part.child.id)
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.MISSING, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(28900, part.fee)
                assertEquals(28900, part.finalFee())
            }
            decision.parts[1].let { part ->
                assertEquals(testChild_1.id, part.child.id)
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.MISSING, part.placement.serviceNeed)
                assertEquals(50, part.siblingDiscount)
                assertEquals(14500, part.fee)
                assertEquals(14500, part.finalFee())
            }
            decision.parts[2].let { part ->
                assertEquals(testChild_2.id, part.child.id)
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.MISSING, part.placement.serviceNeed)
                assertEquals(80, part.siblingDiscount)
                assertEquals(5800, part.fee)
                assertEquals(5800, part.finalFee())
            }
        }
    }

    @Test
    fun `fee decision generation works as expected with multiple children with distinct placements`() {
        val period_1 = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val period_2 = Period(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        val period_3 = Period(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31))
        insertFamilyRelations(
            testAdult_1.id,
            listOf(testChild_1.id, testChild_2.id, testChild_3.id),
            period_1.copy(end = period_3.end)
        )
        insertPlacement(testChild_1.id, period_1, DAYCARE, testDaycare.id)
        insertPlacement(testChild_2.id, period_2, DAYCARE, testDaycare.id)
        insertPlacement(testChild_3.id, period_3, DAYCARE, testDaycare.id)

        generator.handleFamilyUpdate(testAdult_1.id, period_1.copy(end = period_3.end))

        val decisions = getAllFeeDecisions()
        assertEquals(3, decisions.size)
        decisions[0].let { decision ->
            assertEquals(4, decision.familySize)
            assertEquals(period_1.start, decision.validFrom)
            assertEquals(period_1.end, decision.validTo)
            assertEquals(28900, decision.totalFee())
            assertEquals(1, decision.parts.size)
            decision.parts[0].let { part ->
                assertEquals(testChild_1.id, part.child.id)
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.MISSING, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(28900, part.fee)
                assertEquals(28900, part.finalFee())
            }
        }
        decisions[1].let { decision ->
            assertEquals(4, decision.familySize)
            assertEquals(period_2.start, decision.validFrom)
            assertEquals(period_2.end, decision.validTo)
            assertEquals(28900, decision.totalFee())
            assertEquals(1, decision.parts.size)
            decision.parts[0].let { part ->
                assertEquals(testChild_2.id, part.child.id)
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.MISSING, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(28900, part.fee)
                assertEquals(28900, part.finalFee())
            }
        }
        decisions[2].let { decision ->
            assertEquals(4, decision.familySize)
            assertEquals(period_3.start, decision.validFrom)
            assertEquals(period_3.end, decision.validTo)
            assertEquals(28900, decision.totalFee())
            assertEquals(1, decision.parts.size)
            decision.parts[0].let { part ->
                assertEquals(testChild_3.id, part.child.id)
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.MISSING, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(28900, part.fee)
                assertEquals(28900, part.finalFee())
            }
        }
    }

    @Test
    fun `fee decision generation works as expected with changing family compositions`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val subPeriod_1 = period.copy(end = period.start.plusMonths(1))
        val subPeriod_2 = period.copy(start = subPeriod_1.end!!.plusDays(1))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), subPeriod_1)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_2.id), subPeriod_2)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertPlacement(testChild_2.id, period, DAYCARE, testDaycare.id)

        generator.handleFamilyUpdate(testAdult_1.id, period)

        val decisions = getAllFeeDecisions()
        assertEquals(2, decisions.size)
        decisions[0].let { decision ->
            assertEquals(subPeriod_1.start, decision.validFrom)
            assertEquals(subPeriod_1.end, decision.validTo)
            assertEquals(28900, decision.totalFee())
            assertEquals(1, decision.parts.size)
            decision.parts[0].let { part ->
                assertEquals(2, decision.familySize)
                assertEquals(testChild_1.id, part.child.id)
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.MISSING, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(28900, part.fee)
                assertEquals(28900, part.finalFee())
            }
        }
        decisions[1].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(subPeriod_2.start, decision.validFrom)
            assertEquals(subPeriod_2.end, decision.validTo)
            assertEquals(28900, decision.totalFee())
            assertEquals(1, decision.parts.size)
            decision.parts[0].let { part ->
                assertEquals(testChild_2.id, part.child.id)
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.MISSING, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(28900, part.fee)
                assertEquals(28900, part.finalFee())
            }
        }
    }

    @Test
    fun `fee decision generation works as expected with changing income`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val subPeriod_1 = period.copy(end = period.start.plusMonths(1))
        val subPeriod_2 = period.copy(start = subPeriod_1.end!!.plusDays(1))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertIncome(testAdult_1.id, 310200, subPeriod_1)

        generator.handleFamilyUpdate(testAdult_1.id, period)

        val decisions = getAllFeeDecisions()
        assertEquals(2, decisions.size)
        decisions[0].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(subPeriod_1.start, decision.validFrom)
            assertEquals(subPeriod_1.end, decision.validTo)
            assertEquals(10700, decision.totalFee())
            assertEquals(1, decision.parts.size)
            decision.parts[0].let { part ->
                assertEquals(testChild_1.id, part.child.id)
                assertEquals(10700, part.baseFee)
                assertEquals(PlacementType.DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.MISSING, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(10700, part.fee)
                assertEquals(10700, part.finalFee())
            }
        }
        decisions[1].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(subPeriod_2.start, decision.validFrom)
            assertEquals(subPeriod_2.end, decision.validTo)
            assertEquals(28900, decision.totalFee())
            assertEquals(1, decision.parts.size)
            decision.parts[0].let { part ->
                assertEquals(testChild_1.id, part.child.id)
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.MISSING, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(28900, part.fee)
                assertEquals(28900, part.finalFee())
            }
        }
    }

    @Test
    fun `fee decision generation works as expected with removed income`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val incomePeriod = period.copy(end = null)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertIncome(testAdult_1.id, 310200, incomePeriod)

        generator.handleFamilyUpdate(testAdult_1.id, period)

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions[0].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(10700, decision.totalFee())
            assertEquals(1, decision.parts.size)
            decision.parts[0].let { part ->
                assertEquals(testChild_1.id, part.child.id)
                assertEquals(10700, part.baseFee)
                assertEquals(PlacementType.DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.MISSING, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(10700, part.fee)
                assertEquals(10700, part.finalFee())
            }
        }

        deleteIncomes()
        generator.handleIncomeChange(testAdult_1.id, incomePeriod)

        val newDecisions = getAllFeeDecisions()
        assertEquals(1, newDecisions.size)
        newDecisions[0].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(28900, decision.totalFee())
            assertEquals(1, decision.parts.size)
            decision.parts[0].let { part ->
                assertEquals(testChild_1.id, part.child.id)
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.MISSING, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(28900, part.fee)
                assertEquals(28900, part.finalFee())
            }
        }
    }

    @Test
    fun `fee decision generation with multiple periods works as expected with removed income`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val subPeriod_1 = period.copy(end = period.start.plusMonths(1))
        val subPeriod_2 = period.copy(start = subPeriod_1.end!!.plusDays(1))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertIncome(testAdult_1.id, 310200, subPeriod_1)

        generator.handleFamilyUpdate(testAdult_1.id, period)

        val decisions = getAllFeeDecisions()
        assertEquals(2, decisions.size)
        decisions[0].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(subPeriod_1.start, decision.validFrom)
            assertEquals(subPeriod_1.end, decision.validTo)
            assertEquals(10700, decision.totalFee())
            assertEquals(1, decision.parts.size)
            decision.parts[0].let { part ->
                assertEquals(testChild_1.id, part.child.id)
                assertEquals(10700, part.baseFee)
                assertEquals(PlacementType.DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.MISSING, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(10700, part.fee)
                assertEquals(10700, part.finalFee())
            }
        }
        decisions[1].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(subPeriod_2.start, decision.validFrom)
            assertEquals(subPeriod_2.end, decision.validTo)
            assertEquals(28900, decision.totalFee())
            assertEquals(1, decision.parts.size)
            decision.parts[0].let { part ->
                assertEquals(testChild_1.id, part.child.id)
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.MISSING, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(28900, part.fee)
                assertEquals(28900, part.finalFee())
            }
        }

        deleteIncomes()
        generator.handleIncomeChange(testAdult_1.id, subPeriod_1)

        val newDecisions = getAllFeeDecisions()
        assertEquals(1, newDecisions.size)
        newDecisions[0].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(28900, decision.totalFee())
            assertEquals(1, decision.parts.size)
            decision.parts[0].let { part ->
                assertEquals(testChild_1.id, part.child.id)
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.MISSING, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(28900, part.fee)
                assertEquals(28900, part.finalFee())
            }
        }
    }

    @Test
    fun `fee decision generation works as expected with changing partner`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val subPeriod_1 = period.copy(end = period.start.plusMonths(1))
        val subPeriod_2 = period.copy(start = subPeriod_1.end!!.plusDays(1))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPartnership(testAdult_1.id, testAdult_2.id, subPeriod_2)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertIncome(testAdult_1.id, 310200, period)
        insertIncome(testAdult_2.id, 310200, period)

        generator.handleFamilyUpdate(testAdult_1.id, period)

        val decisions = getAllFeeDecisions()
        assertEquals(2, decisions.size)
        decisions[0].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(subPeriod_1.start, decision.validFrom)
            assertEquals(subPeriod_1.end, decision.validTo)
            assertEquals(10700, decision.totalFee())
            assertEquals(1, decision.parts.size)
            decision.parts[0].let { part ->
                assertEquals(testChild_1.id, part.child.id)
                assertEquals(10700, part.baseFee)
                assertEquals(PlacementType.DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.MISSING, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(10700, part.fee)
                assertEquals(10700, part.finalFee())
            }
        }
        decisions[1].let { decision ->
            assertEquals(3, decision.familySize)
            assertEquals(subPeriod_2.start, decision.validFrom)
            assertEquals(subPeriod_2.end, decision.validTo)
            assertEquals(28900, decision.totalFee())
            assertEquals(1, decision.parts.size)
            decision.parts[0].let { part ->
                assertEquals(testChild_1.id, part.child.id)
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.MISSING, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(28900, part.fee)
                assertEquals(28900, part.finalFee())
            }
        }
    }

    @Test
    fun `fee decision generation works as expected with a partner that has missing income`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val subPeriod_1 = period.copy(end = period.start.plusMonths(1))
        val subPeriod_2 = period.copy(start = subPeriod_1.end!!.plusDays(1))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPartnership(testAdult_1.id, testAdult_2.id, subPeriod_2)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertIncome(testAdult_1.id, 310200, period)

        generator.handleFamilyUpdate(testAdult_1.id, period)

        val decisions = getAllFeeDecisions()
        assertEquals(2, decisions.size)
        decisions[0].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(subPeriod_1.start, decision.validFrom)
            assertEquals(subPeriod_1.end, decision.validTo)
            assertEquals(10700, decision.totalFee())
            assertEquals(1, decision.parts.size)
            decision.parts[0].let { part ->
                assertEquals(testChild_1.id, part.child.id)
                assertEquals(10700, part.baseFee)
                assertEquals(PlacementType.DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.MISSING, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(10700, part.fee)
                assertEquals(10700, part.finalFee())
            }
        }
        decisions[1].let { decision ->
            assertEquals(3, decision.familySize)
            assertEquals(subPeriod_2.start, decision.validFrom)
            assertEquals(subPeriod_2.end, decision.validTo)
            assertEquals(28900, decision.totalFee())
            assertEquals(1, decision.parts.size)
            decision.parts[0].let { part ->
                assertEquals(testChild_1.id, part.child.id)
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.MISSING, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(28900, part.fee)
                assertEquals(28900, part.finalFee())
            }
        }
    }

    @Test
    fun `fee decision generation works as expected with fee alterations`() {
        val period = Period(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertFeeAlteration(testChild_1.id, 50.0, period)

        generator.handleFamilyUpdate(testAdult_1.id, period)

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions[0].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(14450, decision.totalFee())
            assertEquals(1, decision.parts.size)
            decision.parts[0].let { part ->
                assertEquals(testChild_1.id, part.child.id)
                assertEquals(28900, part.baseFee)
                assertEquals(PlacementType.DAYCARE, part.placement.type)
                assertEquals(ServiceNeed.MISSING, part.placement.serviceNeed)
                assertEquals(0, part.siblingDiscount)
                assertEquals(28900, part.fee)
                assertEquals(14450, part.finalFee())
            }
        }
    }

    @Test
    fun `active fee decisions have their validity end dates updated on changed future placement end date`() {
        val originalPeriod = Period(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1))
        val sentDecision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            originalPeriod,
            testAdult_1.id,
            listOf(
                createFeeDecisionPartFixture(
                    testChild_1.id,
                    testChild_1.dateOfBirth,
                    testDaycare.id
                )
            )
        )
        jdbi.handle { h -> upsertFeeDecisions(h, objectMapper, listOf(sentDecision)) }

        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), originalPeriod)
        val newPeriod = originalPeriod.copy(end = originalPeriod.end!!.minusDays(7))
        insertPlacement(testChild_1.id, newPeriod, DAYCARE, testDaycare.id)

        generator.handlePlacement(testChild_1.id, newPeriod)

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        assertEquals(sentDecision.copy(validTo = newPeriod.end), decisions.first())
    }

    @Test
    fun `active fee decisions are not updated on changed past placement end date`() {
        val originalPeriod = Period(LocalDate.now().minusYears(1), LocalDate.now().minusDays(1))
        val sentDecision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            originalPeriod,
            testAdult_1.id,
            listOf(
                createFeeDecisionPartFixture(
                    testChild_1.id,
                    testChild_1.dateOfBirth,
                    testDaycare.id
                )
            )
        )
        jdbi.handle { h -> upsertFeeDecisions(h, objectMapper, listOf(sentDecision)) }

        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), originalPeriod)
        val newPeriod = originalPeriod.copy(end = originalPeriod.end!!.minusDays(7))
        insertPlacement(testChild_1.id, newPeriod, DAYCARE, testDaycare.id)

        generator.handlePlacement(testChild_1.id, newPeriod)

        val decisions = getAllFeeDecisions()
        assertEquals(2, decisions.size)
        assertEquals(sentDecision, decisions.first())
        decisions.last().let { decision ->
            assertEquals(FeeDecisionStatus.DRAFT, decision.status)
            assertEquals(newPeriod.end!!.plusDays(1), decision.validFrom)
            assertEquals(originalPeriod.end, decision.validTo)
            assertEquals(0, decision.parts.size)
        }
    }

    @Test
    fun `fee decisions are not generated before the global fee decision min date`() {
        val period = Period(LocalDate.of(2014, 6, 1), LocalDate.of(2015, 6, 1))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        generator.handlePlacement(testChild_1.id, period)

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions.first().let { decision ->
            assertEquals(FeeDecisionStatus.DRAFT, decision.status)
            assertEquals(feeDecisionMinDate, decision.validFrom)
            assertEquals(period.end, decision.validTo)
        }
    }

    @Test
    fun `an empty draft is generated when a placement is moved to start later and a family update is triggered`() {
        val originalPlacementPeriod = Period(LocalDate.now().minusWeeks(2), LocalDate.now().plusYears(1))
        val familyPeriod = Period(LocalDate.now().minusYears(1), LocalDate.now().plusYears(17))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), familyPeriod)
        insertPlacement(
            testChild_1.id,
            originalPlacementPeriod.copy(start = originalPlacementPeriod.start.plusMonths(6)),
            DAYCARE,
            testDaycare.id
        )
        jdbi.handle { h ->
            upsertFeeDecisions(
                h,
                objectMapper,
                listOf(
                    createFeeDecisionFixture(
                        status = FeeDecisionStatus.SENT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_1.id,
                        period = originalPlacementPeriod,
                        parts = listOf(
                            createFeeDecisionPartFixture(
                                childId = testChild_1.id,
                                dateOfBirth = testChild_1.dateOfBirth,
                                daycareId = testDaycare.id,
                                serviceNeed = ServiceNeed.MISSING
                            )
                        )
                    )
                )
            )
        }

        generator.handleFamilyUpdate(testAdult_1.id, familyPeriod)

        val decisions = getAllFeeDecisions()
        assertEquals(3, decisions.size)
        val sent = decisions.find { it.status == FeeDecisionStatus.SENT }!!
        val (firstDraft, secondDraft) = decisions.filter { it.status == FeeDecisionStatus.DRAFT }
            .sortedBy { it.validFrom }

        assertEquals(FeeDecisionStatus.DRAFT, firstDraft.status)
        assertEquals(originalPlacementPeriod.start, firstDraft.validFrom)
        assertEquals(originalPlacementPeriod.start.plusMonths(6).minusDays(1), firstDraft.validTo)
        assertEquals(0, firstDraft.parts.size)

        assertEquals(FeeDecisionStatus.DRAFT, secondDraft.status)
        assertEquals(originalPlacementPeriod.start.plusMonths(6), secondDraft.validFrom)
        assertEquals(originalPlacementPeriod.end, secondDraft.validTo)
        assertEquals(sent.parts, secondDraft.parts)
    }

    @Test
    fun `zero euro fee decisions get an updated draft when pricing changes`() {
        val period = Period(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        jdbi.handle { h ->
            insertIncome(adultId = testAdult_1.id, amount = 0, period = period)
            upsertFeeDecisions(
                h,
                objectMapper,
                listOf(
                    createFeeDecisionFixture(
                        status = FeeDecisionStatus.SENT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_1.id,
                        period = period,
                        pricing = oldTestPricing,
                        headOfFamilyIncome = DecisionIncome(
                            effect = IncomeEffect.INCOME,
                            data = mapOf(IncomeType.MAIN_INCOME to 0),
                            total = 0,
                            validFrom = period.start,
                            validTo = period.end
                        ),
                        parts = listOf(
                            createFeeDecisionPartFixture(
                                childId = testChild_1.id,
                                dateOfBirth = testChild_1.dateOfBirth,
                                daycareId = testDaycare.id,
                                serviceNeed = ServiceNeed.MISSING,
                                baseFee = 0,
                                fee = 0
                            )
                        )
                    )
                )
            )
        }

        generator.handleFamilyUpdate(testAdult_1.id, period)

        val decisions = getAllFeeDecisions()
        assertEquals(2, decisions.size)

        val sent = decisions.find { it.status == FeeDecisionStatus.SENT }!!
        assertEquals(FeeDecisionStatus.SENT, sent.status)
        assertEquals(period.start, sent.validFrom)
        assertEquals(period.end, sent.validTo)
        assertEquals(oldTestPricing, sent.pricing)
        assertEquals(0, sent.totalFee())

        val draft = decisions.find { it.status == FeeDecisionStatus.DRAFT }!!
        assertEquals(FeeDecisionStatus.DRAFT, draft.status)
        assertEquals(period.start, draft.validFrom)
        assertEquals(period.end, draft.validTo)
        assertEquals(testPricing, draft.pricing)
        assertEquals(0, draft.totalFee())
    }

    @Test
    fun `family with children placed into voucher and municipal daycares get sibling discounts in fee and voucher value decisions`() {
        val period = Period(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, testChild_2.id, testChild_3.id), period)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertPlacement(testChild_2.id, period, DAYCARE, testVoucherDaycare.id)
        insertPlacement(testChild_3.id, period, DAYCARE, testDaycare.id)

        generator.handleFamilyUpdate(testAdult_1.id, period)

        val feeDecisions = getAllFeeDecisions()
        assertEquals(1, feeDecisions.size)
        feeDecisions.first().let { decision ->
            assertEquals(FeeDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(43400, decision.totalFee())
            assertEquals(2, decision.parts.size)
            decision.parts.first().let { part ->
                assertEquals(testChild_3.id, part.child.id)
                assertEquals(testDaycare.id, part.placement.unit)
                assertEquals(28900, part.fee)
            }
            decision.parts.last().let { part ->
                assertEquals(testChild_1.id, part.child.id)
                assertEquals(testDaycare.id, part.placement.unit)
                assertEquals(14500, part.fee)
            }
        }

        val voucherValueDecisions = getAllVoucherValueDecisions()
        assertEquals(1, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(5800, decision.totalCoPayment())
            assertEquals(1, decision.parts.size)
            decision.parts.first().let { part ->
                assertEquals(testChild_2.id, part.child.id)
                assertEquals(testVoucherDaycare.id, part.placement.unit)
                assertEquals(5800, part.coPayment)
            }
        }
    }

    private fun assertEqualEnoughDecisions(expected: List<FeeDecision>, actual: List<FeeDecision>) {
        val createdAt = Instant.now()
        UUID.randomUUID().let { uuid ->
            assertEquals(
                expected.map { it.copy(id = uuid, createdAt = createdAt) },
                actual.map { it.copy(id = uuid, createdAt = createdAt) }
            )
        }
    }

    private fun insertPlacement(childId: UUID, period: Period, type: fi.espoo.evaka.placement.PlacementType, daycareId: UUID): UUID {
        return jdbi.handle { h ->
            insertTestPlacement(
                h,
                childId = childId,
                unitId = daycareId,
                startDate = period.start,
                endDate = period.end!!,
                type = type
            )
        }
    }

    private fun insertFamilyRelations(headOfFamilyId: UUID, childIds: List<UUID>, period: Period) {
        jdbi.handle { h ->
            childIds.forEach { childId ->
                insertTestParentship(h, headOfFamilyId, childId, startDate = period.start, endDate = period.end!!)
            }
        }
    }

    private fun insertPartnership(adultId1: UUID, adultId2: UUID, period: Period) {
        jdbi.handle { h ->
            insertTestPartnership(h, adultId1, adultId2, startDate = period.start, endDate = period.end!!)
        }
    }

    private fun insertServiceNeed(
        childId: UUID,
        hours: List<Pair<Period, Double>>
    ) {
        jdbi.handle { h ->
            hours.map {
                insertTestServiceNeed(
                    h,
                    childId,
                    startDate = it.first.start,
                    endDate = it.first.end,
                    hoursPerWeek = it.second,
                    updatedBy = testDecisionMaker_1.id
                )
            }
        }
    }

    private fun insertIncome(adultId: UUID, amount: Int, period: Period) {
        jdbi.handle { h ->
            insertTestIncome(
                h,
                objectMapper,
                personId = adultId,
                validFrom = period.start,
                validTo = period.end,
                effect = IncomeEffect.INCOME,
                data = mapOf(IncomeType.MAIN_INCOME to IncomeValue(amount, IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS))
            )
        }
    }

    private fun deleteIncomes() {
        jdbi.handle { h ->
            h.createUpdate("DELETE FROM income").execute()
        }
    }

    private fun insertFeeAlteration(childId: UUID, amount: Double, period: Period) {
        jdbi.handle { h ->
            insertTestFeeAlteration(
                h,
                childId,
                type = FeeAlteration.Type.DISCOUNT,
                amount = amount,
                isAbsolute = false,
                validFrom = period.start,
                validTo = period.end,
                updatedBy = testDecisionMaker_1.id
            )
        }
    }

    private fun getAllFeeDecisions(): List<FeeDecision> {
        return jdbi.handle { h ->
            h.createQuery(feeDecisionQueryBase)
                .map(toFeeDecision(objectMapper))
                .let { it.merge() }
        }
    }

    private fun getAllVoucherValueDecisions(): List<VoucherValueDecision> {
        return jdbi.handle { h ->
            h.createQuery(voucherValueDecisionQueryBase)
                .map(toVoucherValueDecision(objectMapper))
                .let { it.merge() }
        }
    }
}
