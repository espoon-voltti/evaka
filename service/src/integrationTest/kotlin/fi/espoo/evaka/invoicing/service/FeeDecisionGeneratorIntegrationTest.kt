// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.createFeeDecision2Fixture
import fi.espoo.evaka.invoicing.createFeeDecisionChildFixture
import fi.espoo.evaka.invoicing.data.feeDecisionQueryBase2
import fi.espoo.evaka.invoicing.data.upsertFeeDecisions2
import fi.espoo.evaka.invoicing.domain.DecisionIncome
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.invoicing.domain.FeeDecision2
import fi.espoo.evaka.invoicing.domain.FeeDecisionServiceNeed
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.IncomeCoefficient
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.IncomeType
import fi.espoo.evaka.invoicing.domain.IncomeValue
import fi.espoo.evaka.invoicing.domain.merge
import fi.espoo.evaka.invoicing.oldTestPricing
import fi.espoo.evaka.invoicing.testPricing
import fi.espoo.evaka.placement.PlacementType.CLUB
import fi.espoo.evaka.placement.PlacementType.DAYCARE
import fi.espoo.evaka.placement.PlacementType.DAYCARE_FIVE_YEAR_OLDS
import fi.espoo.evaka.placement.PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS
import fi.espoo.evaka.placement.PlacementType.PREPARATORY
import fi.espoo.evaka.placement.PlacementType.PREPARATORY_DAYCARE
import fi.espoo.evaka.placement.PlacementType.PRESCHOOL
import fi.espoo.evaka.placement.PlacementType.PRESCHOOL_DAYCARE
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.serviceneednew.ServiceNeedOption
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestFeeAlteration
import fi.espoo.evaka.shared.dev.insertTestIncome
import fi.espoo.evaka.shared.dev.insertTestNewServiceNeed
import fi.espoo.evaka.shared.dev.insertTestParentship
import fi.espoo.evaka.shared.dev.insertTestPartnership
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.snDaycareFullDay25to35
import fi.espoo.evaka.snDaycareFullDayPartWeek25
import fi.espoo.evaka.snDefaultDaycare
import fi.espoo.evaka.snDefaultPreparatoryDaycare
import fi.espoo.evaka.snDefaultPreschoolDaycare
import fi.espoo.evaka.snPreparatoryDaycare50
import fi.espoo.evaka.snPreparatoryDaycarePartDay40
import fi.espoo.evaka.snPreparatoryDaycarePartDay40to50
import fi.espoo.evaka.snPreschoolDaycare45
import fi.espoo.evaka.snPreschoolDaycarePartDay35
import fi.espoo.evaka.snPreschoolDaycarePartDay35to45
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testClub
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycareNotInvoiced
import fi.espoo.evaka.testDecisionMaker_1
import org.jdbi.v3.core.kotlin.mapTo
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

class FeeDecisionGeneratorIntegrationTest : FullApplicationTest() {
    @Autowired
    private lateinit var generator: FinanceDecisionGenerator

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
    fun `new placement does not create fee decisions when child is missing head of family`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, DAYCARE, testDaycare.id)

        db.transaction { generator.handlePlacement(it, testChild_1.id, placementPeriod) }

        val decisions = getAllFeeDecisions()
        assertTrue(decisions.isEmpty())
    }

    @Test
    fun `new placement to a net budgeted unit does not generate a fee decision`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, DAYCARE, testDaycareNotInvoiced.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)

        db.transaction { generator.handlePlacement(it, testChild_1.id, placementPeriod) }

        val decisions = getAllFeeDecisions()
        assertEquals(0, decisions.size)
    }

    @Test
    fun `placing a child to a net budgeted unit and their sibling to a normal unit will result in only a single fee decision child`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, DAYCARE, testDaycareNotInvoiced.id)
        insertPlacement(testChild_2.id, placementPeriod, DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, testChild_2.id), placementPeriod)

        db.transaction { generator.handlePlacement(it, testChild_1.id, placementPeriod) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions[0].children.size)
    }

    @Test
    fun `child in a club does not affect fee decisions for siblings in any way`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, CLUB, testClub.id!!)
        insertPlacement(testChild_2.id, placementPeriod, DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, testChild_2.id), placementPeriod)

        db.transaction { generator.handlePlacement(it, testChild_1.id, placementPeriod) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions[0].children.size)
        decisions[0].children[0].let { child ->
            assertEquals(0, child.siblingDiscount)
        }
    }

    @Test
    fun `new placement creates new fee decision when no existing decision`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)

        db.transaction { generator.handlePlacement(it, testChild_1.id, placementPeriod) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
    }

    @Test
    fun `new placement overrides existing fee decision`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)

        db.transaction { generator.handlePlacement(it, testChild_1.id, placementPeriod) }

        val original = getAllFeeDecisions()
        assertEquals(1, original.size)

        db.transaction { generator.handlePlacement(it, testChild_1.id, placementPeriod) }

        val result = getAllFeeDecisions()
        assertEquals(1, original.size)
        assertEqualEnoughDecisions(original, result)
    }

    @Test
    fun `drafts not overlapping with the period are not touched when generating new decisions`() {
        val periodInPast = DateRange(LocalDate.of(2015, 1, 1), LocalDate.of(2015, 12, 31))
        val oldDraft = createFeeDecision2Fixture(
            FeeDecisionStatus.DRAFT,
            FeeDecisionType.NORMAL,
            periodInPast,
            testAdult_1.id,
            listOf(
                createFeeDecisionChildFixture(
                    testChild_2.id,
                    LocalDate.of(2010, 1, 1),
                    testDaycare.id,
                    DAYCARE,
                    snDefaultDaycare.toFeeDecisionServiceNeed()
                )
            )
        )
        db.transaction { tx -> upsertFeeDecisions2(tx.handle, listOf(oldDraft)) }

        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)

        db.transaction { generator.handlePlacement(it, testChild_1.id, placementPeriod) }

        val result = getAllFeeDecisions()
        assertEquals(2, result.size)
        result.first().let {
            assertEquals(periodInPast.start, it.validFrom)
            assertEquals(periodInPast.end, it.validTo)
            assertEquals(testAdult_1.id, it.headOfFamily.id)
            assertEquals(oldDraft.children, it.children)
        }
        result.last().let {
            assertEquals(placementPeriod.start, it.validFrom)
            assertEquals(placementPeriod.end, it.validTo)
            assertEquals(testAdult_1.id, it.headOfFamily.id)
        }
    }

    @Test
    fun `fee decision is not generated for child with a preschool placement`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, PRESCHOOL, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)

        db.transaction { generator.handlePlacement(it, testChild_1.id, placementPeriod) }

        val result = getAllFeeDecisions()

        assertEquals(0, result.size)
    }

    @Test
    fun `fee decision is not generated for child with a preparatory placement`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, PREPARATORY, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)

        db.transaction { generator.handlePlacement(it, testChild_1.id, placementPeriod) }

        val result = getAllFeeDecisions()

        assertEquals(0, result.size)
    }

    @Test
    fun `fee decision placement infers correct service need from a preschool daycare placement`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, PRESCHOOL_DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)

        db.transaction { generator.handlePlacement(it, testChild_1.id, placementPeriod) }

        val result = getAllFeeDecisions()

        assertEquals(PRESCHOOL_DAYCARE, result[0].children[0].placement.type)
        assertEquals(BigDecimal("0.80"), result[0].children[0].serviceNeed.feeCoefficient)
        assertEquals(23100, result[0].children[0].finalFee)
    }

    @Test
    fun `fee decision placement infers correct service need from a preparatory daycare placement`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, PREPARATORY_DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)

        db.transaction { generator.handlePlacement(it, testChild_1.id, placementPeriod) }

        val result = getAllFeeDecisions()

        assertEquals(PREPARATORY_DAYCARE, result[0].children[0].placement.type)
        assertEquals(BigDecimal("0.80"), result[0].children[0].serviceNeed.feeCoefficient)
        assertEquals(23100, result[0].children[0].finalFee)
    }

    @Test
    fun `fee decision placement infers correct service need from a five year olds daycare placement`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, DAYCARE_FIVE_YEAR_OLDS, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)

        db.transaction { generator.handlePlacement(it, testChild_1.id, placementPeriod) }

        val result = getAllFeeDecisions()

        assertEquals(DAYCARE_FIVE_YEAR_OLDS, result[0].children[0].placement.type)
        assertEquals(BigDecimal("0.80"), result[0].children[0].serviceNeed.feeCoefficient)
        assertEquals(23100, result[0].children[0].finalFee)
    }

    @Test
    fun `fee decision placement infers correct service need from a part day five year olds daycare placement`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, DAYCARE_PART_TIME_FIVE_YEAR_OLDS, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)

        db.transaction { generator.handlePlacement(it, testChild_1.id, placementPeriod) }

        val result = getAllFeeDecisions()

        assertEquals(DAYCARE_PART_TIME_FIVE_YEAR_OLDS, result[0].children[0].placement.type)
        assertEquals(BigDecimal("0.35"), result[0].children[0].serviceNeed.feeCoefficient)
        assertEquals(10100, result[0].children[0].finalFee)
    }

    @Test
    fun `new service need updates existing draft`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val placementId = insertPlacement(testChild_1.id, placementPeriod, DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)

        db.transaction { generator.handlePlacement(it, testChild_1.id, placementPeriod) }

        val original = getAllFeeDecisions()
        assertEquals(1, original.size)
        assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), original[0].children[0].serviceNeed)

        val serviceNeed = snDaycareFullDayPartWeek25
        insertServiceNeed(placementId, placementPeriod.asFiniteDateRange()!!, serviceNeed.id)
        db.transaction { generator.handlePlacement(it, testChild_1.id, placementPeriod) }

        val updated = getAllFeeDecisions()
        assertEquals(1, updated.size)
        assertEquals(serviceNeed.toFeeDecisionServiceNeed(), updated[0].children[0].serviceNeed)
    }

    @Test
    fun `new service need that partially covers existing decision splits it`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val placementId = insertPlacement(testChild_1.id, placementPeriod, DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)

        db.transaction { generator.handlePlacement(it, testChild_1.id, placementPeriod) }

        val original = getAllFeeDecisions()
        assertEquals(1, original.size)
        assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), original[0].children[0].serviceNeed)

        val serviceNeedPeriod = FiniteDateRange(LocalDate.of(2019, 7, 1), LocalDate.of(2019, 12, 31))
        val serviceNeed = snDaycareFullDay25to35
        insertServiceNeed(placementId, serviceNeedPeriod, serviceNeed.id)
        db.transaction { generator.handlePlacement(it, testChild_1.id, placementPeriod) }

        val updated = getAllFeeDecisions()
        assertEquals(2, updated.size)
        updated[0].let { decision ->
            assertEquals(placementPeriod.copy(end = serviceNeedPeriod.start.minusDays(1)), decision.validDuring)
            assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), decision.children[0].serviceNeed)
        }
        updated[1].let { decision ->
            assertEquals(serviceNeedPeriod.asDateRange(), decision.validDuring)
            assertEquals(serviceNeed.toFeeDecisionServiceNeed(), decision.children[0].serviceNeed)
        }
    }

    @Test
    fun `deleted service need that split existing decision merges decisions afterwards`() {
        val serviceNeedPeriod = FiniteDateRange(LocalDate.of(2019, 7, 1), LocalDate.of(2019, 12, 31))
        val serviceNeed = snDaycareFullDay25to35
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val placementId = insertPlacement(testChild_1.id, placementPeriod, DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)
        insertServiceNeed(placementId, serviceNeedPeriod, serviceNeed.id)

        db.transaction { generator.handlePlacement(it, testChild_1.id, placementPeriod) }

        val original = getAllFeeDecisions()
        assertEquals(2, original.size)
        original[0].let { decision ->
            assertEquals(placementPeriod.copy(end = serviceNeedPeriod.start.minusDays(1)), decision.validDuring)
            assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), decision.children[0].serviceNeed)
        }
        original[1].let { decision ->
            assertEquals(serviceNeedPeriod.asDateRange(), decision.validDuring)
            assertEquals(serviceNeed.toFeeDecisionServiceNeed(), decision.children[0].serviceNeed)
        }

        db.transaction { tx ->
            tx.execute("DELETE FROM new_service_need WHERE placement_id = ?", placementId)
            generator.handlePlacement(tx, testChild_1.id, serviceNeedPeriod.asDateRange())
        }

        val updated = getAllFeeDecisions()
        assertEquals(1, updated.size)
        assertEquals(placementPeriod, updated[0].validDuring)
        assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), updated[0].children[0].serviceNeed)
    }

    @Test
    fun `new fee decisions are not generated if children have no placements`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)

        db.transaction { generator.handleFeeAlterationChange(it, testChild_1.id, period) }

        val decisions = getAllFeeDecisions()
        assertEquals(0, decisions.size)
    }

    @Test
    fun `new fee decisions are generated if children have no placements but there exists an sent decision for same head of family`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        db.transaction { tx ->
            upsertFeeDecisions2(
                tx.handle,
                listOf(
                    createFeeDecision2Fixture(
                        status = FeeDecisionStatus.SENT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_1.id,
                        period = period,
                        children = listOf(
                            createFeeDecisionChildFixture(
                                childId = testChild_1.id,
                                dateOfBirth = testChild_1.dateOfBirth,
                                placementUnitId = testDaycare.id,
                                placementType = DAYCARE,
                                serviceNeed = snDefaultDaycare.toFeeDecisionServiceNeed()
                            )
                        )
                    )
                )
            )

            generator.handleFamilyUpdate(tx, testAdult_1.id, period)
        }

        val decisions = getAllFeeDecisions()
        assertEquals(2, decisions.size)
        assertEquals(1, decisions.filter { it.status == FeeDecisionStatus.DRAFT }.size)
        assertTrue(decisions.first { it.status == FeeDecisionStatus.DRAFT }.children.isEmpty())
    }

    @Test
    fun `new empty fee decision is generated if head of family has no children but there exists an sent decision for same head of family`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        db.transaction { tx ->
            upsertFeeDecisions2(
                tx.handle,
                listOf(
                    createFeeDecision2Fixture(
                        status = FeeDecisionStatus.SENT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_1.id,
                        period = period,
                        children = listOf(
                            createFeeDecisionChildFixture(
                                childId = testChild_1.id,
                                dateOfBirth = testChild_1.dateOfBirth,
                                placementUnitId = testDaycare.id,
                                placementType = DAYCARE,
                                serviceNeed = snDefaultDaycare.toFeeDecisionServiceNeed()
                            )
                        )
                    )
                )
            )

            generator.handleFamilyUpdate(tx, testAdult_1.id, period)
        }

        val decisions = getAllFeeDecisions()
        assertEquals(2, decisions.size)
        assertEquals(1, decisions.filter { it.status == FeeDecisionStatus.DRAFT }.size)
        assertTrue(decisions.first { it.status == FeeDecisionStatus.DRAFT }.children.isEmpty())
    }

    @Test
    fun `fee decision is formed correctly for two children in preschool with daycare with one default and one 45h service need`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val serviceNeed = snPreschoolDaycare45
        val placementId = insertPlacement(testChild_1.id, placementPeriod, PRESCHOOL_DAYCARE, testDaycare.id)
        insertServiceNeed(placementId, placementPeriod.asFiniteDateRange()!!, serviceNeed.id)
        insertPlacement(testChild_2.id, placementPeriod, PRESCHOOL_DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, testChild_2.id), placementPeriod)

        db.transaction { generator.handleServiceNeed(it, testChild_1.id, placementPeriod) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions.first().let { decision ->
            assertEquals(23100 + 11600, decision.totalFee())
            assertEquals(2, decision.children.size)
            decision.children.first().let { child ->
                assertEquals(28900, child.baseFee)
                assertEquals(PRESCHOOL_DAYCARE, child.placement.type)
                assertEquals(serviceNeed.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(23100, child.fee)
                assertEquals(23100, child.finalFee)
            }
            decision.children.last().let { child ->
                assertEquals(28900, child.baseFee)
                assertEquals(PRESCHOOL_DAYCARE, child.placement.type)
                assertEquals(snDefaultPreschoolDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(50, child.siblingDiscount)
                assertEquals(11600, child.fee)
                assertEquals(11600, child.finalFee)
            }
        }
    }

    @Test
    fun `fee decision is formed correctly for two children in preschool with daycare with one default and one 36h service need`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val serviceNeed = snPreschoolDaycarePartDay35to45
        val placementId = insertPlacement(testChild_1.id, placementPeriod, PRESCHOOL_DAYCARE, testDaycare.id)
        insertServiceNeed(placementId, placementPeriod.asFiniteDateRange()!!, serviceNeed.id)
        insertPlacement(testChild_2.id, placementPeriod, PRESCHOOL_DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, testChild_2.id), placementPeriod)

        db.transaction { generator.handleServiceNeed(it, testChild_1.id, placementPeriod) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions.first().let { decision ->
            assertEquals(17300 + 11600, decision.totalFee())
            assertEquals(2, decision.children.size)
            decision.children.first().let { child ->
                assertEquals(28900, child.baseFee)
                assertEquals(PRESCHOOL_DAYCARE, child.placement.type)
                assertEquals(serviceNeed.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(17300, child.fee)
                assertEquals(17300, child.finalFee)
            }
            decision.children.last().let { child ->
                assertEquals(28900, child.baseFee)
                assertEquals(PRESCHOOL_DAYCARE, child.placement.type)
                assertEquals(snDefaultPreschoolDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(50, child.siblingDiscount)
                assertEquals(11600, child.fee)
                assertEquals(11600, child.finalFee)
            }
        }
    }

    @Test
    fun `fee decision is formed correctly for two children in preschool with daycare with one default and one 35h service need`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val serviceNeed = snPreschoolDaycarePartDay35
        val placementId = insertPlacement(testChild_1.id, placementPeriod, PRESCHOOL_DAYCARE, testDaycare.id)
        insertServiceNeed(placementId, placementPeriod.asFiniteDateRange()!!, serviceNeed.id)
        insertPlacement(testChild_2.id, placementPeriod, PRESCHOOL_DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, testChild_2.id), placementPeriod)

        db.transaction { generator.handleServiceNeed(it, testChild_1.id, placementPeriod) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions.first().let { decision ->
            assertEquals(10100 + 11600, decision.totalFee())
            assertEquals(2, decision.children.size)
            decision.children.first().let { child ->
                assertEquals(28900, child.baseFee)
                assertEquals(PRESCHOOL_DAYCARE, child.placement.type)
                assertEquals(serviceNeed.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(10100, child.fee)
                assertEquals(10100, child.finalFee)
            }
            decision.children.last().let { child ->
                assertEquals(28900, child.baseFee)
                assertEquals(PRESCHOOL_DAYCARE, child.placement.type)
                assertEquals(snDefaultPreschoolDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(50, child.siblingDiscount)
                assertEquals(11600, child.fee)
                assertEquals(11600, child.finalFee)
            }
        }
    }

    @Test
    fun `fee decision is formed correctly for two children with one having preschool without daycare`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, PRESCHOOL, testDaycare.id)
        insertPlacement(testChild_2.id, placementPeriod, PRESCHOOL_DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, testChild_2.id), placementPeriod)

        db.transaction { generator.handleServiceNeed(it, testChild_1.id, placementPeriod) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions.first().let { decision ->
            assertEquals(11600, decision.totalFee())
            assertEquals(1, decision.children.size)
            decision.children.first().let { child ->
                assertEquals(testChild_2.id, child.child.id)
                assertEquals(28900, child.baseFee)
                assertEquals(PRESCHOOL_DAYCARE, child.placement.type)
                assertEquals(snDefaultPreschoolDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(50, child.siblingDiscount)
                assertEquals(11600, child.fee)
                assertEquals(11600, child.finalFee)
            }
        }
    }

    @Test
    fun `fee decision is formed correctly for two children in preparatory with daycare with one default and one 50h service need`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val serviceNeed = snPreparatoryDaycare50
        val placementId = insertPlacement(testChild_1.id, placementPeriod, PREPARATORY_DAYCARE, testDaycare.id)
        insertServiceNeed(placementId, placementPeriod.asFiniteDateRange()!!, serviceNeed.id)
        insertPlacement(testChild_2.id, placementPeriod, PREPARATORY_DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, testChild_2.id), placementPeriod)

        db.transaction { generator.handleServiceNeed(it, testChild_1.id, placementPeriod) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions.first().let { decision ->
            assertEquals(23100 + 11600, decision.totalFee())
            assertEquals(2, decision.children.size)
            decision.children.first().let { child ->
                assertEquals(28900, child.baseFee)
                assertEquals(PREPARATORY_DAYCARE, child.placement.type)
                assertEquals(serviceNeed.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(23100, child.fee)
                assertEquals(23100, child.finalFee)
            }
            decision.children.last().let { child ->
                assertEquals(28900, child.baseFee)
                assertEquals(PREPARATORY_DAYCARE, child.placement.type)
                assertEquals(snDefaultPreparatoryDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(50, child.siblingDiscount)
                assertEquals(11600, child.fee)
                assertEquals(11600, child.finalFee)
            }
        }
    }

    @Test
    fun `fee decision is formed correctly for two children in preparatory with daycare with one default and one 45h service need`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val serviceNeed = snPreparatoryDaycarePartDay40to50
        val placementId = insertPlacement(testChild_1.id, placementPeriod, PREPARATORY_DAYCARE, testDaycare.id)
        insertServiceNeed(placementId, placementPeriod.asFiniteDateRange()!!, serviceNeed.id)
        insertPlacement(testChild_2.id, placementPeriod, PREPARATORY_DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, testChild_2.id), placementPeriod)

        db.transaction { generator.handleServiceNeed(it, testChild_1.id, placementPeriod) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions.first().let { decision ->
            assertEquals(17300 + 11600, decision.totalFee())
            assertEquals(2, decision.children.size)
            decision.children.first().let { child ->
                assertEquals(28900, child.baseFee)
                assertEquals(PREPARATORY_DAYCARE, child.placement.type)
                assertEquals(serviceNeed.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(17300, child.fee)
                assertEquals(17300, child.finalFee)
            }
            decision.children.last().let { child ->
                assertEquals(28900, child.baseFee)
                assertEquals(PREPARATORY_DAYCARE, child.placement.type)
                assertEquals(snDefaultPreparatoryDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(50, child.siblingDiscount)
                assertEquals(11600, child.fee)
                assertEquals(11600, child.finalFee)
            }
        }
    }

    @Test
    fun `fee decision is formed correctly for two children in preschool with daycare with one default and one 40h service need`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val serviceNeed = snPreparatoryDaycarePartDay40
        val placementId = insertPlacement(testChild_1.id, placementPeriod, PREPARATORY_DAYCARE, testDaycare.id)
        insertServiceNeed(placementId, placementPeriod.asFiniteDateRange()!!, serviceNeed.id)
        insertPlacement(testChild_2.id, placementPeriod, PREPARATORY_DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, testChild_2.id), placementPeriod)

        db.transaction { generator.handleServiceNeed(it, testChild_1.id, placementPeriod) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions.first().let { decision ->
            assertEquals(10100 + 11600, decision.totalFee())
            assertEquals(2, decision.children.size)
            decision.children.first().let { child ->
                assertEquals(28900, child.baseFee)
                assertEquals(PREPARATORY_DAYCARE, child.placement.type)
                assertEquals(serviceNeed.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(10100, child.fee)
                assertEquals(10100, child.finalFee)
            }
            decision.children.last().let { child ->
                assertEquals(28900, child.baseFee)
                assertEquals(PREPARATORY_DAYCARE, child.placement.type)
                assertEquals(snDefaultPreparatoryDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(50, child.siblingDiscount)
                assertEquals(11600, child.fee)
                assertEquals(11600, child.finalFee)
            }
        }
    }

    @Test
    fun `fee decision is formed correctly for two children with one having preparatory without daycare`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, PREPARATORY, testDaycare.id)
        insertPlacement(testChild_2.id, placementPeriod, PREPARATORY_DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, testChild_2.id), placementPeriod)

        db.transaction { generator.handleServiceNeed(it, testChild_1.id, placementPeriod) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions.first().let { decision ->
            assertEquals(11600, decision.totalFee())
            assertEquals(1, decision.children.size)
            decision.children.first().let { child ->
                assertEquals(testChild_2.id, child.child.id)
                assertEquals(28900, child.baseFee)
                assertEquals(PREPARATORY_DAYCARE, child.placement.type)
                assertEquals(snDefaultPreparatoryDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(50, child.siblingDiscount)
                assertEquals(11600, child.fee)
                assertEquals(11600, child.finalFee)
            }
        }
    }

    @Test
    fun `children over 18 years old are not included in families when determining base fee`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val birthday = LocalDate.of(2001, 7, 1)
        val childTurning18Id = db.transaction { it.insertTestPerson(DevPerson(dateOfBirth = birthday)) }

        insertPlacement(testChild_1.id, placementPeriod, DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, childTurning18Id), placementPeriod)
        insertIncome(testAdult_1.id, 330000, placementPeriod)

        db.transaction { generator.handleServiceNeed(it, testChild_1.id, placementPeriod) }

        val decisions = getAllFeeDecisions()
        assertEquals(2, decisions.size)
        decisions.first().let { decision ->
            assertEquals(3, decision.familySize)
            assertEquals(6300, decision.totalFee())
            assertEquals(1, decision.children.size)
            decision.children.first().let { child ->
                assertEquals(6300, child.baseFee)
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(6300, child.fee)
                assertEquals(6300, child.finalFee)
            }
        }
        decisions.last().let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(12800, decision.totalFee())
            assertEquals(1, decision.children.size)
            decision.children.first().let { child ->
                assertEquals(12800, child.baseFee)
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(12800, child.fee)
                assertEquals(12800, child.finalFee)
            }
        }
    }

    @Test
    fun `new fee decisions are not generated if new draft would be equal to already existing sent decision`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        db.transaction { tx ->
            upsertFeeDecisions2(
                tx.handle,
                listOf(
                    createFeeDecision2Fixture(
                        status = FeeDecisionStatus.SENT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_1.id,
                        period = period,
                        children = listOf(
                            createFeeDecisionChildFixture(
                                childId = testChild_1.id,
                                dateOfBirth = testChild_1.dateOfBirth,
                                placementUnitId = testDaycare.id,
                                placementType = DAYCARE,
                                serviceNeed = snDefaultDaycare.toFeeDecisionServiceNeed()
                            )
                        )
                    )
                )
            )

            generator.handleFamilyUpdate(tx, testAdult_1.id, period)
        }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        assertEquals(0, decisions.filter { it.status == FeeDecisionStatus.DRAFT }.size)
    }

    @Test
    fun `new fee decisions are not generated if new draft would be equal to already existing sent decisions that cover the period`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        db.transaction { tx ->
            upsertFeeDecisions2(
                tx.handle,
                listOf(
                    createFeeDecision2Fixture(
                        status = FeeDecisionStatus.SENT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_1.id,
                        period = period.copy(end = period.start.plusDays(7)),
                        children = listOf(
                            createFeeDecisionChildFixture(
                                childId = testChild_1.id,
                                dateOfBirth = testChild_1.dateOfBirth,
                                placementUnitId = testDaycare.id,
                                placementType = DAYCARE,
                                serviceNeed = snDefaultDaycare.toFeeDecisionServiceNeed()
                            )
                        )
                    ),
                    createFeeDecision2Fixture(
                        status = FeeDecisionStatus.SENT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_1.id,
                        period = period.copy(start = period.start.plusDays(8)),
                        children = listOf(
                            createFeeDecisionChildFixture(
                                childId = testChild_1.id,
                                dateOfBirth = testChild_1.dateOfBirth,
                                placementUnitId = testDaycare.id,
                                placementType = DAYCARE,
                                serviceNeed = snDefaultDaycare.toFeeDecisionServiceNeed()
                            )
                        )
                    )
                )
            )

            generator.handleFamilyUpdate(tx, testAdult_1.id, period)
        }

        val decisions = getAllFeeDecisions()
        assertEquals(2, decisions.size)
        assertEquals(0, decisions.filter { it.status == FeeDecisionStatus.DRAFT }.size)
    }

    @Test
    fun `new fee decisions are generated if new draft would be equal to already existing sent decision but a new draft that is valid before it is generated`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        val placementId = insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        val serviceNeed = snDaycareFullDayPartWeek25
        val shorterPeriod = FiniteDateRange(period.start, period.start.plusDays(30))
        insertServiceNeed(placementId, shorterPeriod, serviceNeed.id)
        db.transaction { tx ->
            upsertFeeDecisions2(
                tx.handle,
                listOf(
                    createFeeDecision2Fixture(
                        status = FeeDecisionStatus.SENT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_1.id,
                        period = period,
                        children = listOf(
                            createFeeDecisionChildFixture(
                                childId = testChild_1.id,
                                dateOfBirth = testChild_1.dateOfBirth,
                                placementUnitId = testDaycare.id,
                                placementType = DAYCARE,
                                serviceNeed = snDefaultDaycare.toFeeDecisionServiceNeed()
                            )
                        )
                    )
                )
            )

            generator.handleFamilyUpdate(tx, testAdult_1.id, period)
        }

        val decisions = getAllFeeDecisions()
        assertEquals(3, decisions.size)

        val drafts = decisions.filter { it.status == FeeDecisionStatus.DRAFT }
        assertEquals(2, drafts.size)
        drafts.first().let { draft ->
            assertEquals(shorterPeriod.start, draft.validFrom)
            assertEquals(shorterPeriod.end, draft.validTo)
            assertEquals(17300, draft.totalFee())
            assertEquals(1, draft.children.size)
            draft.children.first().let { child ->
                assertEquals(28900, child.baseFee)
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(serviceNeed.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(17300, child.fee)
                assertEquals(17300, child.finalFee)
            }
        }
        drafts.last().let { draft ->
            assertEquals(shorterPeriod.end.plusDays(1), draft.validFrom)
            assertEquals(period.end, draft.validTo)
            assertEquals(28900, draft.totalFee())
            assertEquals(1, draft.children.size)
            draft.children.first().let { child ->
                assertEquals(28900, child.baseFee)
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(28900, child.fee)
                assertEquals(28900, child.finalFee)
            }
        }
    }

    @Test
    fun `new fee decision is generated even if it is equal to existing sent decision from it's start but there is a existing draft before it`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        val placementId = insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        val olderPeriod = period.copy(end = period.start.plusDays(30))
        val serviceNeed = snDaycareFullDayPartWeek25
        insertServiceNeed(placementId, olderPeriod.asFiniteDateRange()!!, serviceNeed.id)
        val newerPeriod = period.copy(start = olderPeriod.end!!.plusDays(1))
        db.transaction { tx ->
            upsertFeeDecisions2(
                tx.handle,
                listOf(
                    createFeeDecision2Fixture(
                        status = FeeDecisionStatus.SENT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_1.id,
                        period = period,
                        children = listOf(
                            createFeeDecisionChildFixture(
                                childId = testChild_1.id,
                                dateOfBirth = testChild_1.dateOfBirth,
                                placementUnitId = testDaycare.id,
                                placementType = DAYCARE,
                                serviceNeed = snDefaultDaycare.toFeeDecisionServiceNeed()
                            )
                        )
                    ),
                    createFeeDecision2Fixture(
                        status = FeeDecisionStatus.DRAFT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_1.id,
                        period = olderPeriod,
                        children = listOf(
                            createFeeDecisionChildFixture(
                                childId = testChild_1.id,
                                dateOfBirth = testChild_1.dateOfBirth,
                                placementUnitId = testDaycare.id,
                                placementType = DAYCARE,
                                serviceNeed = serviceNeed.toFeeDecisionServiceNeed()
                            )
                        )
                    ),
                    createFeeDecision2Fixture(
                        status = FeeDecisionStatus.DRAFT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_1.id,
                        period = newerPeriod,
                        children = listOf(
                            createFeeDecisionChildFixture(
                                childId = testChild_1.id,
                                dateOfBirth = testChild_1.dateOfBirth,
                                placementUnitId = testDaycare.id,
                                placementType = DAYCARE,
                                serviceNeed = snDefaultDaycare.toFeeDecisionServiceNeed()
                            )
                        )
                    )
                )
            )

            generator.handleFamilyUpdate(tx, testAdult_1.id, newerPeriod)
        }

        val decisions = getAllFeeDecisions()
        assertEquals(3, decisions.size)

        val drafts = decisions.filter { it.status == FeeDecisionStatus.DRAFT }
        assertEquals(2, drafts.size)
        drafts.first().let { draft ->
            assertEquals(olderPeriod.start, draft.validFrom)
            assertEquals(olderPeriod.end, draft.validTo)
            assertEquals(1, draft.children.size)
            draft.children.first().let { child ->
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(serviceNeed.toFeeDecisionServiceNeed(), child.serviceNeed)
            }
        }
        drafts.last().let { draft ->
            assertEquals(newerPeriod.start, draft.validFrom)
            assertEquals(newerPeriod.end, draft.validTo)
            assertEquals(1, draft.children.size)
            draft.children.first().let { child ->
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
            }
        }
    }

    @Test
    fun `fee decision generation works as expected with multiple children with partially overlapping placements`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, testChild_2.id, testChild_3.id), period)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertPlacement(testChild_2.id, period.copy(start = period.start.plusMonths(1)), DAYCARE, testDaycare.id)
        insertPlacement(testChild_3.id, period.copy(start = period.start.plusMonths(2)), DAYCARE, testDaycare.id)

        db.transaction { generator.handleFamilyUpdate(it, testAdult_1.id, period) }

        val decisions = getAllFeeDecisions()
        assertEquals(3, decisions.size)
        decisions[0].let { decision ->
            assertEquals(4, decision.familySize)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.start.plusMonths(1).minusDays(1), decision.validTo)
            assertEquals(28900, decision.totalFee())
            assertEquals(1, decision.children.size)
            decision.children[0].let { child ->
                assertEquals(testChild_1.id, child.child.id)
                assertEquals(28900, child.baseFee)
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(28900, child.fee)
                assertEquals(28900, child.finalFee)
            }
        }
        decisions[1].let { decision ->
            assertEquals(4, decision.familySize)
            assertEquals(period.start.plusMonths(1), decision.validFrom)
            assertEquals(period.start.plusMonths(2).minusDays(1), decision.validTo)
            assertEquals(43400, decision.totalFee())
            assertEquals(2, decision.children.size)
            decision.children[0].let { child ->
                assertEquals(testChild_1.id, child.child.id)
                assertEquals(28900, child.baseFee)
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(28900, child.fee)
                assertEquals(28900, child.finalFee)
            }
            decision.children[1].let { child ->
                assertEquals(testChild_2.id, child.child.id)
                assertEquals(28900, child.baseFee)
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(50, child.siblingDiscount)
                assertEquals(14500, child.fee)
                assertEquals(14500, child.finalFee)
            }
        }
        decisions[2].let { decision ->
            assertEquals(4, decision.familySize)
            assertEquals(period.start.plusMonths(2), decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(49200, decision.totalFee())
            assertEquals(3, decision.children.size)
            decision.children[0].let { child ->
                assertEquals(testChild_3.id, child.child.id)
                assertEquals(28900, child.baseFee)
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(28900, child.fee)
                assertEquals(28900, child.finalFee)
            }
            decision.children[1].let { child ->
                assertEquals(testChild_1.id, child.child.id)
                assertEquals(28900, child.baseFee)
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(50, child.siblingDiscount)
                assertEquals(14500, child.fee)
                assertEquals(14500, child.finalFee)
            }
            decision.children[2].let { child ->
                assertEquals(testChild_2.id, child.child.id)
                assertEquals(28900, child.baseFee)
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(80, child.siblingDiscount)
                assertEquals(5800, child.fee)
                assertEquals(5800, child.finalFee)
            }
        }
    }

    @Test
    fun `fee decision generation works as expected with multiple children with distinct placements`() {
        val period_1 = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val period_2 = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        val period_3 = DateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31))
        insertFamilyRelations(
            testAdult_1.id,
            listOf(testChild_1.id, testChild_2.id, testChild_3.id),
            period_1.copy(end = period_3.end)
        )
        insertPlacement(testChild_1.id, period_1, DAYCARE, testDaycare.id)
        insertPlacement(testChild_2.id, period_2, DAYCARE, testDaycare.id)
        insertPlacement(testChild_3.id, period_3, DAYCARE, testDaycare.id)

        db.transaction { generator.handleFamilyUpdate(it, testAdult_1.id, period_1.copy(end = period_3.end)) }

        val decisions = getAllFeeDecisions()
        assertEquals(3, decisions.size)
        decisions[0].let { decision ->
            assertEquals(4, decision.familySize)
            assertEquals(period_1.start, decision.validFrom)
            assertEquals(period_1.end, decision.validTo)
            assertEquals(28900, decision.totalFee())
            assertEquals(1, decision.children.size)
            decision.children[0].let { child ->
                assertEquals(testChild_1.id, child.child.id)
                assertEquals(28900, child.baseFee)
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(28900, child.fee)
                assertEquals(28900, child.finalFee)
            }
        }
        decisions[1].let { decision ->
            assertEquals(4, decision.familySize)
            assertEquals(period_2.start, decision.validFrom)
            assertEquals(period_2.end, decision.validTo)
            assertEquals(28900, decision.totalFee())
            assertEquals(1, decision.children.size)
            decision.children[0].let { child ->
                assertEquals(testChild_2.id, child.child.id)
                assertEquals(28900, child.baseFee)
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(28900, child.fee)
                assertEquals(28900, child.finalFee)
            }
        }
        decisions[2].let { decision ->
            assertEquals(4, decision.familySize)
            assertEquals(period_3.start, decision.validFrom)
            assertEquals(period_3.end, decision.validTo)
            assertEquals(28900, decision.totalFee())
            assertEquals(1, decision.children.size)
            decision.children[0].let { child ->
                assertEquals(testChild_3.id, child.child.id)
                assertEquals(28900, child.baseFee)
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(28900, child.fee)
                assertEquals(28900, child.finalFee)
            }
        }
    }

    @Test
    fun `fee decision generation works as expected with changing family compositions`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val subPeriod_1 = period.copy(end = period.start.plusMonths(1))
        val subPeriod_2 = period.copy(start = subPeriod_1.end!!.plusDays(1))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), subPeriod_1)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_2.id), subPeriod_2)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertPlacement(testChild_2.id, period, DAYCARE, testDaycare.id)

        db.transaction { generator.handleFamilyUpdate(it, testAdult_1.id, period) }

        val decisions = getAllFeeDecisions()
        assertEquals(2, decisions.size)
        decisions[0].let { decision ->
            assertEquals(subPeriod_1.start, decision.validFrom)
            assertEquals(subPeriod_1.end, decision.validTo)
            assertEquals(28900, decision.totalFee())
            assertEquals(1, decision.children.size)
            decision.children[0].let { child ->
                assertEquals(2, decision.familySize)
                assertEquals(testChild_1.id, child.child.id)
                assertEquals(28900, child.baseFee)
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(28900, child.fee)
                assertEquals(28900, child.finalFee)
            }
        }
        decisions[1].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(subPeriod_2.start, decision.validFrom)
            assertEquals(subPeriod_2.end, decision.validTo)
            assertEquals(28900, decision.totalFee())
            assertEquals(1, decision.children.size)
            decision.children[0].let { child ->
                assertEquals(testChild_2.id, child.child.id)
                assertEquals(28900, child.baseFee)
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(28900, child.fee)
                assertEquals(28900, child.finalFee)
            }
        }
    }

    @Test
    fun `fee decision generation works as expected with changing income`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val subPeriod_1 = period.copy(end = period.start.plusMonths(1))
        val subPeriod_2 = period.copy(start = subPeriod_1.end!!.plusDays(1))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertIncome(testAdult_1.id, 310200, subPeriod_1)

        db.transaction { generator.handleFamilyUpdate(it, testAdult_1.id, period) }

        val decisions = getAllFeeDecisions()
        assertEquals(2, decisions.size)
        decisions[0].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(subPeriod_1.start, decision.validFrom)
            assertEquals(subPeriod_1.end, decision.validTo)
            assertEquals(10700, decision.totalFee())
            assertEquals(1, decision.children.size)
            decision.children[0].let { child ->
                assertEquals(testChild_1.id, child.child.id)
                assertEquals(10700, child.baseFee)
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(10700, child.fee)
                assertEquals(10700, child.finalFee)
            }
        }
        decisions[1].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(subPeriod_2.start, decision.validFrom)
            assertEquals(subPeriod_2.end, decision.validTo)
            assertEquals(28900, decision.totalFee())
            assertEquals(1, decision.children.size)
            decision.children[0].let { child ->
                assertEquals(testChild_1.id, child.child.id)
                assertEquals(28900, child.baseFee)
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(28900, child.fee)
                assertEquals(28900, child.finalFee)
            }
        }
    }

    @Test
    fun `fee decision generation works as expected with removed income`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val incomePeriod = period.copy(end = null)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertIncome(testAdult_1.id, 310200, incomePeriod)

        db.transaction { generator.handleFamilyUpdate(it, testAdult_1.id, period) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions[0].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(10700, decision.totalFee())
            assertEquals(1, decision.children.size)
            decision.children[0].let { child ->
                assertEquals(testChild_1.id, child.child.id)
                assertEquals(10700, child.baseFee)
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(10700, child.fee)
                assertEquals(10700, child.finalFee)
            }
        }

        deleteIncomes()
        db.transaction { generator.handleIncomeChange(it, testAdult_1.id, incomePeriod) }

        val newDecisions = getAllFeeDecisions()
        assertEquals(1, newDecisions.size)
        newDecisions[0].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(28900, decision.totalFee())
            assertEquals(1, decision.children.size)
            decision.children[0].let { child ->
                assertEquals(testChild_1.id, child.child.id)
                assertEquals(28900, child.baseFee)
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(28900, child.fee)
                assertEquals(28900, child.finalFee)
            }
        }
    }

    @Test
    fun `fee decision generation with multiple periods works as expected with removed income`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val subPeriod_1 = period.copy(end = period.start.plusMonths(1))
        val subPeriod_2 = period.copy(start = subPeriod_1.end!!.plusDays(1))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertIncome(testAdult_1.id, 310200, subPeriod_1)

        db.transaction { generator.handleFamilyUpdate(it, testAdult_1.id, period) }

        val decisions = getAllFeeDecisions()
        assertEquals(2, decisions.size)
        decisions[0].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(subPeriod_1.start, decision.validFrom)
            assertEquals(subPeriod_1.end, decision.validTo)
            assertEquals(10700, decision.totalFee())
            assertEquals(1, decision.children.size)
            decision.children[0].let { child ->
                assertEquals(testChild_1.id, child.child.id)
                assertEquals(10700, child.baseFee)
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(10700, child.fee)
                assertEquals(10700, child.finalFee)
            }
        }
        decisions[1].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(subPeriod_2.start, decision.validFrom)
            assertEquals(subPeriod_2.end, decision.validTo)
            assertEquals(28900, decision.totalFee())
            assertEquals(1, decision.children.size)
            decision.children[0].let { child ->
                assertEquals(testChild_1.id, child.child.id)
                assertEquals(28900, child.baseFee)
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(28900, child.fee)
                assertEquals(28900, child.finalFee)
            }
        }

        deleteIncomes()
        db.transaction { generator.handleIncomeChange(it, testAdult_1.id, subPeriod_1) }

        val newDecisions = getAllFeeDecisions()
        assertEquals(1, newDecisions.size)
        newDecisions[0].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(28900, decision.totalFee())
            assertEquals(1, decision.children.size)
            decision.children[0].let { child ->
                assertEquals(testChild_1.id, child.child.id)
                assertEquals(28900, child.baseFee)
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(28900, child.fee)
                assertEquals(28900, child.finalFee)
            }
        }
    }

    @Test
    fun `fee decision generation works as expected with changing children`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val subPeriod_1 = period.copy(end = period.start.plusMonths(1))
        val subPeriod_2 = period.copy(start = subPeriod_1.end!!.plusDays(1))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPartnership(testAdult_1.id, testAdult_2.id, subPeriod_2)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertIncome(testAdult_1.id, 310200, period)
        insertIncome(testAdult_2.id, 310200, period)

        db.transaction { generator.handleFamilyUpdate(it, testAdult_1.id, period) }

        val decisions = getAllFeeDecisions()
        assertEquals(2, decisions.size)
        decisions[0].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(subPeriod_1.start, decision.validFrom)
            assertEquals(subPeriod_1.end, decision.validTo)
            assertEquals(10700, decision.totalFee())
            assertEquals(1, decision.children.size)
            decision.children[0].let { child ->
                assertEquals(testChild_1.id, child.child.id)
                assertEquals(10700, child.baseFee)
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(10700, child.fee)
                assertEquals(10700, child.finalFee)
            }
        }
        decisions[1].let { decision ->
            assertEquals(3, decision.familySize)
            assertEquals(subPeriod_2.start, decision.validFrom)
            assertEquals(subPeriod_2.end, decision.validTo)
            assertEquals(28900, decision.totalFee())
            assertEquals(1, decision.children.size)
            decision.children[0].let { child ->
                assertEquals(testChild_1.id, child.child.id)
                assertEquals(28900, child.baseFee)
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(28900, child.fee)
                assertEquals(28900, child.finalFee)
            }
        }
    }

    @Test
    fun `fee decision generation works as expected with a children that has missing income`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val subPeriod_1 = period.copy(end = period.start.plusMonths(1))
        val subPeriod_2 = period.copy(start = subPeriod_1.end!!.plusDays(1))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPartnership(testAdult_1.id, testAdult_2.id, subPeriod_2)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertIncome(testAdult_1.id, 310200, period)

        db.transaction { generator.handleFamilyUpdate(it, testAdult_1.id, period) }

        val decisions = getAllFeeDecisions()
        assertEquals(2, decisions.size)
        decisions[0].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(subPeriod_1.start, decision.validFrom)
            assertEquals(subPeriod_1.end, decision.validTo)
            assertEquals(10700, decision.totalFee())
            assertEquals(1, decision.children.size)
            decision.children[0].let { child ->
                assertEquals(testChild_1.id, child.child.id)
                assertEquals(10700, child.baseFee)
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(10700, child.fee)
                assertEquals(10700, child.finalFee)
            }
        }
        decisions[1].let { decision ->
            assertEquals(3, decision.familySize)
            assertEquals(subPeriod_2.start, decision.validFrom)
            assertEquals(subPeriod_2.end, decision.validTo)
            assertEquals(28900, decision.totalFee())
            assertEquals(1, decision.children.size)
            decision.children[0].let { child ->
                assertEquals(testChild_1.id, child.child.id)
                assertEquals(28900, child.baseFee)
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(28900, child.fee)
                assertEquals(28900, child.finalFee)
            }
        }
    }

    @Test
    fun `fee decision generation works as expected with fee alterations`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertFeeAlteration(testChild_1.id, 50.0, period)

        db.transaction { generator.handleFamilyUpdate(it, testAdult_1.id, period) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions[0].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(14450, decision.totalFee())
            assertEquals(1, decision.children.size)
            decision.children[0].let { child ->
                assertEquals(testChild_1.id, child.child.id)
                assertEquals(28900, child.baseFee)
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(28900, child.fee)
                assertEquals(14450, child.finalFee)
            }
        }
    }

    @Test
    fun `active fee decisions have their validity end dates updated on changed future placement end date`() {
        val originalPeriod = DateRange(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1))
        val sentDecision = createFeeDecision2Fixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            originalPeriod,
            testAdult_1.id,
            listOf(
                createFeeDecisionChildFixture(
                    testChild_1.id,
                    testChild_1.dateOfBirth,
                    testDaycare.id,
                    DAYCARE,
                    snDefaultDaycare.toFeeDecisionServiceNeed()
                )
            )
        )
        db.transaction { tx -> upsertFeeDecisions2(tx.handle, listOf(sentDecision)) }

        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), originalPeriod)
        val newPeriod = originalPeriod.copy(end = originalPeriod.end!!.minusDays(7))
        insertPlacement(testChild_1.id, newPeriod, DAYCARE, testDaycare.id)

        db.transaction { generator.handlePlacement(it, testChild_1.id, newPeriod) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        assertEqualEnoughDecisions(sentDecision.copy(validDuring = newPeriod), decisions.first())
    }

    @Test
    fun `active fee decisions are not updated on changed past placement end date`() {
        val originalPeriod = DateRange(LocalDate.now().minusYears(1), LocalDate.now().minusDays(1))
        val sentDecision = createFeeDecision2Fixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            originalPeriod,
            testAdult_1.id,
            listOf(
                createFeeDecisionChildFixture(
                    testChild_1.id,
                    testChild_1.dateOfBirth,
                    testDaycare.id,
                    DAYCARE,
                    snDefaultDaycare.toFeeDecisionServiceNeed()
                )
            )
        )
        db.transaction { tx -> upsertFeeDecisions2(tx.handle, listOf(sentDecision)) }

        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), originalPeriod)
        val newPeriod = originalPeriod.copy(end = originalPeriod.end!!.minusDays(7))
        insertPlacement(testChild_1.id, newPeriod, DAYCARE, testDaycare.id)

        db.transaction { generator.handlePlacement(it, testChild_1.id, newPeriod) }

        val decisions = getAllFeeDecisions()
        assertEquals(2, decisions.size)
        assertEqualEnoughDecisions(sentDecision, decisions.first())
        decisions.last().let { decision ->
            assertEquals(FeeDecisionStatus.DRAFT, decision.status)
            assertEquals(newPeriod.end!!.plusDays(1), decision.validFrom)
            assertEquals(originalPeriod.end, decision.validTo)
            assertEquals(0, decision.children.size)
        }
    }

    @Test
    fun `fee decisions are not generated before the global fee decision min date`() {
        val period = DateRange(LocalDate.of(2014, 6, 1), LocalDate.of(2015, 6, 1))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        db.transaction { generator.handlePlacement(it, testChild_1.id, period) }

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
        val originalPlacementPeriod = DateRange(LocalDate.now().minusWeeks(2), LocalDate.now().plusYears(1))
        val familyPeriod = DateRange(LocalDate.now().minusYears(1), LocalDate.now().plusYears(17))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), familyPeriod)
        insertPlacement(
            testChild_1.id,
            originalPlacementPeriod.copy(start = originalPlacementPeriod.start.plusMonths(6)),
            DAYCARE,
            testDaycare.id
        )
        db.transaction { tx ->
            upsertFeeDecisions2(
                tx.handle,
                listOf(
                    createFeeDecision2Fixture(
                        status = FeeDecisionStatus.SENT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_1.id,
                        period = originalPlacementPeriod,
                        children = listOf(
                            createFeeDecisionChildFixture(
                                childId = testChild_1.id,
                                dateOfBirth = testChild_1.dateOfBirth,
                                placementUnitId = testDaycare.id,
                                placementType = DAYCARE,
                                serviceNeed = snDefaultDaycare.toFeeDecisionServiceNeed()
                            )
                        )
                    )
                )
            )

            generator.handleFamilyUpdate(tx, testAdult_1.id, familyPeriod)
        }

        val decisions = getAllFeeDecisions()
        assertEquals(3, decisions.size)
        val sent = decisions.find { it.status == FeeDecisionStatus.SENT }!!
        val (firstDraft, secondDraft) = decisions.filter { it.status == FeeDecisionStatus.DRAFT }
            .sortedBy { it.validFrom }

        assertEquals(FeeDecisionStatus.DRAFT, firstDraft.status)
        assertEquals(originalPlacementPeriod.start, firstDraft.validFrom)
        assertEquals(originalPlacementPeriod.start.plusMonths(6).minusDays(1), firstDraft.validTo)
        assertEquals(0, firstDraft.children.size)

        assertEquals(FeeDecisionStatus.DRAFT, secondDraft.status)
        assertEquals(originalPlacementPeriod.start.plusMonths(6), secondDraft.validFrom)
        assertEquals(originalPlacementPeriod.end, secondDraft.validTo)
        assertEquals(sent.children, secondDraft.children)
    }

    @Test
    fun `zero euro fee decisions get an updated draft when pricing changes`() {
        val period = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertIncome(adultId = testAdult_1.id, amount = 0, period = period)
        createFeeDecision2Fixture(
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
            children = listOf(
                createFeeDecisionChildFixture(
                    childId = testChild_1.id,
                    dateOfBirth = testChild_1.dateOfBirth,
                    placementUnitId = testDaycare.id,
                    placementType = DAYCARE,
                    serviceNeed = snDefaultDaycare.toFeeDecisionServiceNeed(),
                    baseFee = 0,
                    fee = 0
                )
            )
        ).let { fixture ->
            db.transaction { tx ->
                upsertFeeDecisions2(
                    tx.handle,
                    listOf(fixture)
                )
                generator.handleFamilyUpdate(tx, testAdult_1.id, period)
            }
        }

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
    fun `decision is generated correctly from two identical and concurrent placements`() {
        val period = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period.copy(end = period.start.plusMonths(1)), DAYCARE, testDaycare.id)
        insertPlacement(
            testChild_1.id,
            period.copy(start = period.start.plusMonths(1).plusDays(1)),
            DAYCARE,
            testDaycare.id
        )

        db.transaction { generator.handleFamilyUpdate(it, testAdult_1.id, period) }

        val feeDecisions = getAllFeeDecisions()
        assertEquals(1, feeDecisions.size)
        feeDecisions.first().let { decision ->
            assertEquals(FeeDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(28900, decision.totalFee())
            assertEquals(1, decision.children.size)
            decision.children.first().let { child ->
                assertEquals(testChild_1.id, child.child.id)
                assertEquals(testDaycare.id, child.placement.unit.id)
                assertEquals(28900, child.fee)
            }
        }
    }

    private fun assertEqualEnoughDecisions(expected: FeeDecision2, actual: FeeDecision2) {
        val createdAt = Instant.now()
        UUID.randomUUID().let { uuid ->
            assertEquals(expected.copy(id = uuid, created = createdAt), actual.copy(id = uuid, created = createdAt))
        }
    }

    private fun assertEqualEnoughDecisions(expected: List<FeeDecision2>, actual: List<FeeDecision2>) {
        val createdAt = Instant.now()
        UUID.randomUUID().let { uuid ->
            assertEquals(
                expected.map { it.copy(id = uuid, created = createdAt) },
                actual.map { it.copy(id = uuid, created = createdAt) }
            )
        }
    }

    private fun insertPlacement(childId: UUID, period: DateRange, type: fi.espoo.evaka.placement.PlacementType, daycareId: UUID): UUID {
        return db.transaction { tx ->
            tx.insertTestPlacement(
                childId = childId,
                unitId = daycareId,
                startDate = period.start,
                endDate = period.end!!,
                type = type
            )
        }
    }

    private fun insertFamilyRelations(headOfFamilyId: UUID, childIds: List<UUID>, period: DateRange) {
        db.transaction { tx ->
            childIds.forEach { childId ->
                tx.insertTestParentship(headOfFamilyId, childId, startDate = period.start, endDate = period.end!!)
            }
        }
    }

    private fun insertPartnership(adultId1: UUID, adultId2: UUID, period: DateRange) {
        db.transaction { tx ->
            tx.insertTestPartnership(adultId1, adultId2, startDate = period.start, endDate = period.end!!)
        }
    }

    private fun insertServiceNeed(
        placementId: UUID,
        period: FiniteDateRange,
        optionId: UUID
    ) {
        db.transaction { tx ->
            tx.insertTestNewServiceNeed(
                placementId,
                period,
                optionId
            )
        }
    }

    private fun insertIncome(adultId: UUID, amount: Int, period: DateRange) {
        db.transaction { tx ->
            tx.insertTestIncome(
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
        db.transaction { tx ->
            tx.createUpdate("DELETE FROM income").execute()
        }
    }

    private fun insertFeeAlteration(childId: UUID, amount: Double, period: DateRange) {
        db.transaction { tx ->
            tx.insertTestFeeAlteration(
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

    private fun getAllFeeDecisions(): List<FeeDecision2> {
        return db.read { tx ->
            tx.createQuery(feeDecisionQueryBase2)
                .mapTo<FeeDecision2>()
                .merge()
        }
    }

    private fun ServiceNeedOption.toFeeDecisionServiceNeed() = FeeDecisionServiceNeed(
        optionId = this.id,
        feeCoefficient = this.feeCoefficient
    )
}
