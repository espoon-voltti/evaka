// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.feeThresholds
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.invoicing.calculateMonthlyAmount
import fi.espoo.evaka.invoicing.controller.FeeDecisionController
import fi.espoo.evaka.invoicing.createFeeDecisionChildFixture
import fi.espoo.evaka.invoicing.createFeeDecisionFixture
import fi.espoo.evaka.invoicing.data.feeDecisionQuery
import fi.espoo.evaka.invoicing.data.upsertFeeDecisions
import fi.espoo.evaka.invoicing.domain.DecisionIncome
import fi.espoo.evaka.invoicing.domain.FeeAlterationType
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionDifference
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.FeeThresholds
import fi.espoo.evaka.invoicing.domain.IncomeCoefficient
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.IncomeValue
import fi.espoo.evaka.invoicing.oldTestFeeThresholds
import fi.espoo.evaka.invoicing.testFeeThresholds
import fi.espoo.evaka.pis.controllers.ParentshipController
import fi.espoo.evaka.pis.controllers.PartnershipsController
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.PlacementType.CLUB
import fi.espoo.evaka.placement.PlacementType.DAYCARE
import fi.espoo.evaka.placement.PlacementType.DAYCARE_FIVE_YEAR_OLDS
import fi.espoo.evaka.placement.PlacementType.DAYCARE_PART_TIME
import fi.espoo.evaka.placement.PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS
import fi.espoo.evaka.placement.PlacementType.PREPARATORY
import fi.espoo.evaka.placement.PlacementType.PREPARATORY_DAYCARE
import fi.espoo.evaka.placement.PlacementType.PRESCHOOL
import fi.espoo.evaka.placement.PlacementType.PRESCHOOL_CLUB
import fi.espoo.evaka.placement.PlacementType.PRESCHOOL_DAYCARE
import fi.espoo.evaka.placement.PlacementType.SCHOOL_SHIFT_CARE
import fi.espoo.evaka.serviceneed.ServiceNeedOptionFee
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.FeeAlterationId
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.dev.DevFeeAlteration
import fi.espoo.evaka.shared.dev.DevIncome
import fi.espoo.evaka.shared.dev.DevParentship
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevServiceNeed
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestPartnership
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.snDaycareContractDays15
import fi.espoo.evaka.snDaycareFullDay25to35
import fi.espoo.evaka.snDaycareFullDay35
import fi.espoo.evaka.snDaycareFullDayPartWeek25
import fi.espoo.evaka.snDefaultDaycare
import fi.espoo.evaka.snDefaultPreparatoryDaycare
import fi.espoo.evaka.snDefaultPreschoolDaycare
import fi.espoo.evaka.snPreparatoryDaycare50
import fi.espoo.evaka.snPreparatoryDaycarePartDay40
import fi.espoo.evaka.snPreparatoryDaycarePartDay40to50
import fi.espoo.evaka.snPreschoolClub45
import fi.espoo.evaka.snPreschoolDaycare45
import fi.espoo.evaka.snPreschoolDaycarePartDay35
import fi.espoo.evaka.snPreschoolDaycarePartDay35to45
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testAdult_3
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testChild_4
import fi.espoo.evaka.testChild_8
import fi.espoo.evaka.testClub
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDaycareNotInvoiced
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testDecisionMaker_2
import fi.espoo.evaka.toFeeDecisionServiceNeed
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FeeDecisionGeneratorIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var generator: FinanceDecisionGenerator
    @Autowired private lateinit var feeDecisionController: FeeDecisionController
    @Autowired private lateinit var parentshipController: ParentshipController
    @Autowired private lateinit var partnershipsController: PartnershipsController
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    @Autowired
    private lateinit var coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testDecisionMaker_1)
            tx.insert(testDecisionMaker_2)
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testDaycare2)
            tx.insert(testDaycareNotInvoiced)
            tx.insert(testClub)
            listOf(testAdult_1, testAdult_2, testAdult_3).forEach {
                tx.insert(it, DevPersonType.ADULT)
            }
            listOf(testChild_1, testChild_2, testChild_3, testChild_4, testChild_8).forEach {
                tx.insert(it, DevPersonType.CHILD)
            }
            tx.insert(feeThresholds)
            tx.insertServiceNeedOptions()
        }
    }

    @Test
    fun `new placement does not create fee decisions when child is missing head of family`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, DAYCARE, testDaycare.id)

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val decisions = getAllFeeDecisions()
        assertTrue(decisions.isEmpty())
    }

    @Test
    fun `new placement to a net budgeted unit does not generate a fee decision`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, DAYCARE, testDaycareNotInvoiced.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val decisions = getAllFeeDecisions()
        assertEquals(0, decisions.size)
    }

    @Test
    fun `placing a child to a net budgeted unit and their sibling to a normal unit will result in only a single fee decision child`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, DAYCARE, testDaycareNotInvoiced.id)
        insertPlacement(testChild_2.id, placementPeriod, DAYCARE, testDaycare.id)
        insertFamilyRelations(
            testAdult_1.id,
            listOf(testChild_1.id, testChild_2.id),
            placementPeriod
        )

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions[0].children.size)
    }

    @Test
    fun `child in a club does not affect fee decisions for siblings in any way`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, CLUB, testClub.id)
        insertPlacement(testChild_2.id, placementPeriod, DAYCARE, testDaycare.id)
        insertFamilyRelations(
            testAdult_1.id,
            listOf(testChild_1.id, testChild_2.id),
            placementPeriod
        )

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions[0].children.size)
        decisions[0].children[0].let { child -> assertEquals(0, child.siblingDiscount) }
    }

    @Test
    fun `child in a school shift care does not affect fee decisions for siblings in any way`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, SCHOOL_SHIFT_CARE, testClub.id)
        insertPlacement(testChild_2.id, placementPeriod, DAYCARE, testDaycare.id)
        insertFamilyRelations(
            testAdult_1.id,
            listOf(testChild_1.id, testChild_2.id),
            placementPeriod
        )

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions[0].children.size)
        decisions[0].children[0].let { child -> assertEquals(0, child.siblingDiscount) }
    }

    @Test
    fun `new placement creates new fee decision when no existing decision`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
    }

    @Test
    fun `regenerating overrides existing fee decision and preserves metadata`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val original = getAllFeeDecisions()
        assertEquals(1, original.size)

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val result = getAllFeeDecisions()
        assertEquals(1, original.size)
        assertEqualEnoughDecisions(original, result)
        assertEquals(original.first().id, result.first().id)
        assertEquals(original.first().created, result.first().created)
    }

    @Test
    fun `fee decision is not generated for child with a preschool placement`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, PRESCHOOL, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val result = getAllFeeDecisions()

        assertEquals(0, result.size)
    }

    @Test
    fun `fee decision is not generated for child with a preparatory placement`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, PREPARATORY, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val result = getAllFeeDecisions()

        assertEquals(0, result.size)
    }

    @Test
    fun `fee decision placement infers correct service need from a preschool daycare placement`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, PRESCHOOL_DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val result = getAllFeeDecisions()

        assertEquals(PRESCHOOL_DAYCARE, result[0].children[0].placement.type)
        assertEquals(BigDecimal("0.80"), result[0].children[0].serviceNeed.feeCoefficient)
        assertEquals(23100, result[0].children[0].finalFee)
    }

    @Test
    fun `fee decision is generated for child with a preschool club`() {
        db.transaction { tx ->
            tx.insert(
                ServiceNeedOptionFee(
                    serviceNeedOptionId = snPreschoolClub45.id,
                    validity = DateRange(LocalDate.of(2000, 1, 1), null),
                    baseFee = 14000,
                    siblingDiscount2 = BigDecimal("0.4"),
                    siblingFee2 = 8000,
                    siblingDiscount2Plus = BigDecimal("0.4"),
                    siblingFee2Plus = 8000
                )
            )
        }

        val placementPeriod = FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertServiceNeed(
            insertPlacement(
                testChild_1.id,
                placementPeriod.asDateRange(),
                PRESCHOOL_CLUB,
                testDaycare.id
            ),
            placementPeriod,
            snPreschoolClub45.id
        )
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod.asDateRange())

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val result = getAllFeeDecisions()

        assertEquals(PRESCHOOL_CLUB, result[0].children[0].placement.type)
        assertEquals(BigDecimal("0.80"), result[0].children[0].serviceNeed.feeCoefficient)
        assertEquals(11200, result[0].children[0].finalFee)
    }

    @Test
    fun `fee decision is formed correctly for two children in preschool club`() {
        db.transaction { tx ->
            tx.insert(
                ServiceNeedOptionFee(
                    serviceNeedOptionId = snPreschoolClub45.id,
                    validity = DateRange(LocalDate.of(2000, 1, 1), null),
                    baseFee = 14000,
                    siblingDiscount2 = BigDecimal("0.4"),
                    siblingFee2 = 8000,
                    siblingDiscount2Plus = BigDecimal("0.4"),
                    siblingFee2Plus = 8000
                )
            )
        }

        val placementPeriod = FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val serviceNeed = snPreschoolClub45
        insertServiceNeed(
            insertPlacement(
                testChild_1.id,
                placementPeriod.asDateRange(),
                PRESCHOOL_CLUB,
                testDaycare.id
            ),
            placementPeriod,
            serviceNeed.id
        )
        insertServiceNeed(
            insertPlacement(
                testChild_2.id,
                placementPeriod.asDateRange(),
                PRESCHOOL_CLUB,
                testDaycare.id
            ),
            placementPeriod,
            serviceNeed.id
        )
        insertFamilyRelations(
            testAdult_1.id,
            listOf(testChild_1.id, testChild_2.id),
            placementPeriod.asDateRange()
        )

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions.first().let { decision ->
            assertEquals(11200 + 6400, decision.totalFee)
            assertEquals(2, decision.children.size)
            decision.children.first().let { child ->
                assertEquals(14000, child.baseFee)
                assertEquals(PRESCHOOL_CLUB, child.placement.type)
                assertEquals(serviceNeed.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(11200, child.fee)
                assertEquals(11200, child.finalFee)
            }
            decision.children.last().let { child ->
                assertEquals(14000, child.baseFee)
                assertEquals(PRESCHOOL_CLUB, child.placement.type)
                assertEquals(serviceNeed.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(40, child.siblingDiscount)
                assertEquals(6400, child.fee)
                assertEquals(6400, child.finalFee)
            }
        }
    }

    @Test
    fun `fee decision placement infers correct service need from a preparatory daycare placement`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, PREPARATORY_DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val result = getAllFeeDecisions()

        assertEquals(PREPARATORY_DAYCARE, result[0].children[0].placement.type)
        assertEquals(BigDecimal("0.80"), result[0].children[0].serviceNeed.feeCoefficient)
        assertEquals(23100, result[0].children[0].finalFee)
    }

    @Test
    fun `fee decision placement gets correct contract days`() {
        val start = LocalDate.of(2019, 1, 1)
        val end = LocalDate.of(2019, 12, 31)
        val placementPeriod = DateRange(start, end)
        val placementId = insertPlacement(testChild_1.id, placementPeriod, DAYCARE, testDaycare.id)
        insertServiceNeed(placementId, FiniteDateRange(start, end), snDaycareContractDays15.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val result = getAllFeeDecisions()

        assertEquals(DAYCARE, result[0].children[0].placement.type)
        assertEquals(15, result[0].children[0].serviceNeed.contractDaysPerMonth)
    }

    @Test
    fun `fee decision placement infers correct service need from a five year olds daycare placement`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, DAYCARE_FIVE_YEAR_OLDS, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val result = getAllFeeDecisions()

        assertEquals(DAYCARE_FIVE_YEAR_OLDS, result[0].children[0].placement.type)
        assertEquals(BigDecimal("0.80"), result[0].children[0].serviceNeed.feeCoefficient)
        assertEquals(23100, result[0].children[0].finalFee)
    }

    @Test
    fun `fee decision placement infers correct service need from a part day five year olds daycare placement`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(
            testChild_1.id,
            placementPeriod,
            DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
            testDaycare.id
        )
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val result = getAllFeeDecisions()

        assertEquals(DAYCARE_PART_TIME_FIVE_YEAR_OLDS, result[0].children[0].placement.type)
        assertEquals(BigDecimal("0.80"), result[0].children[0].serviceNeed.feeCoefficient)
        assertEquals(23100, result[0].children[0].finalFee)
    }

    @Test
    fun `new service need updates existing draft`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val placementId = insertPlacement(testChild_1.id, placementPeriod, DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val original = getAllFeeDecisions()
        assertEquals(1, original.size)
        assertEquals(
            snDefaultDaycare.toFeeDecisionServiceNeed(),
            original[0].children[0].serviceNeed
        )

        val serviceNeed = snDaycareFullDayPartWeek25
        insertServiceNeed(placementId, placementPeriod.asFiniteDateRange()!!, serviceNeed.id)
        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val updated = getAllFeeDecisions()
        assertEquals(1, updated.size)
        assertEquals(serviceNeed.toFeeDecisionServiceNeed(), updated[0].children[0].serviceNeed)
    }

    @Test
    fun `new service need that partially covers existing decision splits it`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val placementId = insertPlacement(testChild_1.id, placementPeriod, DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val original = getAllFeeDecisions()
        assertEquals(1, original.size)
        assertEquals(
            snDefaultDaycare.toFeeDecisionServiceNeed(),
            original[0].children[0].serviceNeed
        )

        val serviceNeedPeriod =
            FiniteDateRange(LocalDate.of(2019, 7, 1), LocalDate.of(2019, 12, 31))
        val serviceNeed = snDaycareFullDay25to35
        insertServiceNeed(placementId, serviceNeedPeriod, serviceNeed.id)
        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val updated = getAllFeeDecisions().sortedBy { it.validFrom }
        assertEquals(2, updated.size)
        updated[0].let { decision ->
            assertEquals(
                placementPeriod.copy(end = serviceNeedPeriod.start.minusDays(1)),
                decision.validDuring
            )
            assertEquals(
                snDefaultDaycare.toFeeDecisionServiceNeed(),
                decision.children[0].serviceNeed
            )
        }
        updated[1].let { decision ->
            assertEquals(serviceNeedPeriod.asDateRange(), decision.validDuring)
            assertEquals(serviceNeed.toFeeDecisionServiceNeed(), decision.children[0].serviceNeed)
        }
    }

    @Test
    fun `deleted service need that split existing decision merges decisions afterwards`() {
        val serviceNeedPeriod =
            FiniteDateRange(LocalDate.of(2019, 7, 1), LocalDate.of(2019, 12, 31))
        val serviceNeed = snDaycareFullDay25to35
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val placementId = insertPlacement(testChild_1.id, placementPeriod, DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), placementPeriod)
        insertServiceNeed(placementId, serviceNeedPeriod, serviceNeed.id)

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val original = getAllFeeDecisions().sortedBy { it.validFrom }
        assertEquals(2, original.size)
        original[0].let { decision ->
            assertEquals(
                placementPeriod.copy(end = serviceNeedPeriod.start.minusDays(1)),
                decision.validDuring
            )
            assertEquals(
                snDefaultDaycare.toFeeDecisionServiceNeed(),
                decision.children[0].serviceNeed
            )
        }
        original[1].let { decision ->
            assertEquals(serviceNeedPeriod.asDateRange(), decision.validDuring)
            assertEquals(serviceNeed.toFeeDecisionServiceNeed(), decision.children[0].serviceNeed)
        }

        db.transaction { tx ->
            tx.execute { sql("DELETE FROM service_need WHERE placement_id = ${bind(placementId)}") }
            generator.generateNewDecisionsForChild(tx, testChild_1.id)
        }

        val updated = getAllFeeDecisions()
        assertEquals(1, updated.size)
        assertEquals(placementPeriod, updated[0].validDuring)
        assertEquals(
            snDefaultDaycare.toFeeDecisionServiceNeed(),
            updated[0].children[0].serviceNeed
        )
    }

    @Test
    fun `new fee decisions are not generated if children have no placements`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val decisions = getAllFeeDecisions()
        assertEquals(0, decisions.size)
    }

    @Test
    fun `new fee decisions are generated if children have no placements but there exists an sent decision for same head of family`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        db.transaction { tx ->
            tx.upsertFeeDecisions(
                listOf(
                    createFeeDecisionFixture(
                        status = FeeDecisionStatus.SENT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_1.id,
                        period = period,
                        children =
                            listOf(
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

            generator.generateNewDecisionsForAdult(tx, testAdult_1.id)
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
            tx.upsertFeeDecisions(
                listOf(
                    createFeeDecisionFixture(
                        status = FeeDecisionStatus.SENT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_1.id,
                        period = period,
                        children =
                            listOf(
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

            generator.generateNewDecisionsForAdult(tx, testAdult_1.id)
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
        val placementId =
            insertPlacement(testChild_1.id, placementPeriod, PRESCHOOL_DAYCARE, testDaycare.id)
        insertServiceNeed(placementId, placementPeriod.asFiniteDateRange()!!, serviceNeed.id)
        insertPlacement(testChild_2.id, placementPeriod, PRESCHOOL_DAYCARE, testDaycare.id)
        insertFamilyRelations(
            testAdult_1.id,
            listOf(testChild_1.id, testChild_2.id),
            placementPeriod
        )

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions.first().let { decision ->
            assertEquals(23100 + 11600, decision.totalFee)
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
                assertEquals(
                    snDefaultPreschoolDaycare.toFeeDecisionServiceNeed(),
                    child.serviceNeed
                )
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
        val placementId =
            insertPlacement(testChild_1.id, placementPeriod, PRESCHOOL_DAYCARE, testDaycare.id)
        insertServiceNeed(placementId, placementPeriod.asFiniteDateRange()!!, serviceNeed.id)
        insertPlacement(testChild_2.id, placementPeriod, PRESCHOOL_DAYCARE, testDaycare.id)
        insertFamilyRelations(
            testAdult_1.id,
            listOf(testChild_1.id, testChild_2.id),
            placementPeriod
        )

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions.first().let { decision ->
            assertEquals(17300 + 11600, decision.totalFee)
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
                assertEquals(
                    snDefaultPreschoolDaycare.toFeeDecisionServiceNeed(),
                    child.serviceNeed
                )
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
        val placementId =
            insertPlacement(testChild_1.id, placementPeriod, PRESCHOOL_DAYCARE, testDaycare.id)
        insertServiceNeed(placementId, placementPeriod.asFiniteDateRange()!!, serviceNeed.id)
        insertPlacement(testChild_2.id, placementPeriod, PRESCHOOL_DAYCARE, testDaycare.id)
        insertFamilyRelations(
            testAdult_1.id,
            listOf(testChild_1.id, testChild_2.id),
            placementPeriod
        )

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions.first().let { decision ->
            assertEquals(10100 + 11600, decision.totalFee)
            assertEquals(2, decision.children.size)
            val children = decision.children.sortedBy { it.finalFee }
            children.first().let { child ->
                assertEquals(28900, child.baseFee)
                assertEquals(PRESCHOOL_DAYCARE, child.placement.type)
                assertEquals(serviceNeed.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(10100, child.fee)
                assertEquals(10100, child.finalFee)
            }
            children.last().let { child ->
                assertEquals(28900, child.baseFee)
                assertEquals(PRESCHOOL_DAYCARE, child.placement.type)
                assertEquals(
                    snDefaultPreschoolDaycare.toFeeDecisionServiceNeed(),
                    child.serviceNeed
                )
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
        insertFamilyRelations(
            testAdult_1.id,
            listOf(testChild_1.id, testChild_2.id),
            placementPeriod
        )

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions.first().let { decision ->
            assertEquals(11600, decision.totalFee)
            assertEquals(1, decision.children.size)
            decision.children.first().let { child ->
                assertEquals(testChild_2.id, child.child.id)
                assertEquals(28900, child.baseFee)
                assertEquals(PRESCHOOL_DAYCARE, child.placement.type)
                assertEquals(
                    snDefaultPreschoolDaycare.toFeeDecisionServiceNeed(),
                    child.serviceNeed
                )
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
        val placementId =
            insertPlacement(testChild_1.id, placementPeriod, PREPARATORY_DAYCARE, testDaycare.id)
        insertServiceNeed(placementId, placementPeriod.asFiniteDateRange()!!, serviceNeed.id)
        insertPlacement(testChild_2.id, placementPeriod, PREPARATORY_DAYCARE, testDaycare.id)
        insertFamilyRelations(
            testAdult_1.id,
            listOf(testChild_1.id, testChild_2.id),
            placementPeriod
        )

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions.first().let { decision ->
            assertEquals(23100 + 11600, decision.totalFee)
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
                assertEquals(
                    snDefaultPreparatoryDaycare.toFeeDecisionServiceNeed(),
                    child.serviceNeed
                )
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
        val placementId =
            insertPlacement(testChild_1.id, placementPeriod, PREPARATORY_DAYCARE, testDaycare.id)
        insertServiceNeed(placementId, placementPeriod.asFiniteDateRange()!!, serviceNeed.id)
        insertPlacement(testChild_2.id, placementPeriod, PREPARATORY_DAYCARE, testDaycare.id)
        insertFamilyRelations(
            testAdult_1.id,
            listOf(testChild_1.id, testChild_2.id),
            placementPeriod
        )

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions.first().let { decision ->
            assertEquals(17300 + 11600, decision.totalFee)
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
                assertEquals(
                    snDefaultPreparatoryDaycare.toFeeDecisionServiceNeed(),
                    child.serviceNeed
                )
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
        val placementId =
            insertPlacement(testChild_1.id, placementPeriod, PREPARATORY_DAYCARE, testDaycare.id)
        insertServiceNeed(placementId, placementPeriod.asFiniteDateRange()!!, serviceNeed.id)
        insertPlacement(testChild_2.id, placementPeriod, PREPARATORY_DAYCARE, testDaycare.id)
        insertFamilyRelations(
            testAdult_1.id,
            listOf(testChild_1.id, testChild_2.id),
            placementPeriod
        )

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions.first().let { decision ->
            assertEquals(10100 + 11600, decision.totalFee)
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
                assertEquals(
                    snDefaultPreparatoryDaycare.toFeeDecisionServiceNeed(),
                    child.serviceNeed
                )
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
        insertFamilyRelations(
            testAdult_1.id,
            listOf(testChild_1.id, testChild_2.id),
            placementPeriod
        )

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions.first().let { decision ->
            assertEquals(11600, decision.totalFee)
            assertEquals(1, decision.children.size)
            decision.children.first().let { child ->
                assertEquals(testChild_2.id, child.child.id)
                assertEquals(28900, child.baseFee)
                assertEquals(PREPARATORY_DAYCARE, child.placement.type)
                assertEquals(
                    snDefaultPreparatoryDaycare.toFeeDecisionServiceNeed(),
                    child.serviceNeed
                )
                assertEquals(50, child.siblingDiscount)
                assertEquals(11600, child.fee)
                assertEquals(11600, child.finalFee)
            }
        }
    }

    @Test
    fun `twins are ordered consistently for sibling discount`() {
        // Younger per SSN
        val twin1 =
            testChild_1.copy(
                id = ChildId(UUID.randomUUID()),
                dateOfBirth = LocalDate.of(2019, 1, 1),
                ssn = "010117A902X"
            )

        // Older
        val twin2 =
            testChild_2.copy(
                id = ChildId(UUID.randomUUID()),
                dateOfBirth = LocalDate.of(2019, 1, 1),
                ssn = "010117A901W"
            )

        db.transaction { tx -> listOf(twin1, twin2).forEach { tx.insert(it, DevPersonType.CHILD) } }

        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(twin1.id, placementPeriod, DAYCARE, testDaycare.id)
        insertPlacement(twin2.id, placementPeriod, DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(twin1.id, twin2.id), placementPeriod)

        db.transaction { generator.generateNewDecisionsForChild(it, twin1.id) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions.first().let { decision ->
            assertEquals(2, decision.children.size)
            decision.children.first().let { child ->
                assertEquals(twin1.id, child.child.id)
                assertEquals(0, child.siblingDiscount)
            }
            decision.children.last().let { child ->
                assertEquals(twin2.id, child.child.id)
                assertEquals(50, child.siblingDiscount)
            }
        }
    }

    @Test
    fun `Child income affects only that childs fees`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertPlacement(testChild_1.id, placementPeriod, DAYCARE, testDaycare.id)
        insertPlacement(testChild_2.id, placementPeriod, DAYCARE, testDaycare.id)
        insertFamilyRelations(
            testAdult_1.id,
            listOf(testChild_1.id, testChild_2.id),
            placementPeriod
        )

        // Adult minimal income
        insertIncome(testAdult_1.id, 310200, placementPeriod)

        // Unlike other income, child income should affect only that child's fees
        insertIncome(testChild_1.id, 600000, placementPeriod)

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions.first().let { decision ->
            assertEquals(28900, decision.totalFee)
            assertEquals(2, decision.children.size)
            decision.children.first().let { child ->
                assertEquals(testChild_1.id, child.child.id)
                assertEquals(28900, child.baseFee)
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(28900, child.fee)
                assertEquals(28900, child.finalFee)
            }
            decision.children.last().let { child ->
                assertEquals(testChild_2.id, child.child.id)
                assertEquals(4200, child.baseFee)
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(50, child.siblingDiscount)
                assertEquals(0, child.fee)
                assertEquals(0, child.finalFee)
            }
        }
    }

    @Test
    fun `children over 18 years old are not included in families when determining base fee`() {
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val birthday = LocalDate.of(2001, 7, 1)
        val childTurning18Id =
            db.transaction { it.insert(DevPerson(dateOfBirth = birthday), DevPersonType.RAW_ROW) }

        insertPlacement(testChild_1.id, placementPeriod, DAYCARE, testDaycare.id)
        insertFamilyRelations(
            testAdult_1.id,
            listOf(testChild_1.id, childTurning18Id),
            placementPeriod
        )
        insertIncome(testAdult_1.id, 330000, placementPeriod)

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val decisions = getAllFeeDecisions().sortedBy { it.validFrom }
        assertEquals(2, decisions.size)
        decisions.first().let { decision ->
            assertEquals(3, decision.familySize)
            assertEquals(6300, decision.totalFee)
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
            assertEquals(12800, decision.totalFee)
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
            tx.upsertFeeDecisions(
                listOf(
                    createFeeDecisionFixture(
                        status = FeeDecisionStatus.SENT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_1.id,
                        period = period,
                        children =
                            listOf(
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

            generator.generateNewDecisionsForAdult(tx, testAdult_1.id)
        }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        assertEquals(0, decisions.filter { it.status == FeeDecisionStatus.DRAFT }.size)
    }

    @Test
    fun `head of family difference`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), subPeriod1)
        insertFamilyRelations(testAdult_2.id, listOf(testChild_1.id), subPeriod2)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)

        db.transaction { tx -> generator.generateNewDecisionsForChild(tx, testChild_1.id) }

        assertThat(getAllFeeDecisions())
            .extracting({ it.validDuring }, { it.difference }, { it.headOfFamilyId })
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, emptySet<FeeDecisionDifference>(), testAdult_1.id),
                Tuple(subPeriod2, emptySet<FeeDecisionDifference>(), testAdult_2.id)
            )
    }

    @Test
    fun `partner difference`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        val clock = MockEvakaClock(HelsinkiDateTime.of(period.start, LocalTime.MIN))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertPartnership(testAdult_1.id, testAdult_2.id, subPeriod1, clock.now())
        insertPartnership(testAdult_1.id, testAdult_3.id, subPeriod2, clock.now())

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllFeeDecisions())
            .extracting({ it.validDuring }, { it.difference }, { it.partnerId })
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, emptySet<FeeDecisionDifference>(), testAdult_2.id),
                Tuple(subPeriod2, setOf(FeeDecisionDifference.GUARDIANS), testAdult_3.id)
            )
    }

    @Test
    fun `head of family & partner switch difference`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        val clock = MockEvakaClock(HelsinkiDateTime.of(period.start, LocalTime.MIN))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), subPeriod1)
        insertFamilyRelations(testAdult_2.id, listOf(testChild_1.id), subPeriod2)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertPartnership(testAdult_1.id, testAdult_2.id, period, clock.now())
        insertIncome(testAdult_1.id, 10000, period)
        insertIncome(testAdult_2.id, 20000, period)

        db.transaction { tx -> generator.generateNewDecisionsForChild(tx, testChild_1.id) }

        assertThat(getAllFeeDecisions())
            .extracting({ it.validDuring }, { it.difference }, { it.headOfFamilyId })
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, emptySet<FeeDecisionDifference>(), testAdult_1.id),
                Tuple(subPeriod2, emptySet<FeeDecisionDifference>(), testAdult_2.id)
            )
    }

    @Test
    fun `switch both family head and partnership the other way around`() {
        val firstPeriod = DateRange(LocalDate.of(2020, 11, 13), LocalDate.of(2021, 1, 31))
        val secondPeriod = DateRange(LocalDate.of(2021, 2, 1), null)
        val clock =
            MockEvakaClock(HelsinkiDateTime.Companion.of(firstPeriod.start, LocalTime.of(0, 0)))
        insertPartnership(testAdult_1.id, testAdult_2.id, firstPeriod, clock.now())
        insertPartnership(testAdult_2.id, testAdult_1.id, secondPeriod, clock.now())
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), firstPeriod)
        insertFamilyRelations(
            testAdult_2.id,
            listOf(testChild_1.id),
            secondPeriod.copy(end = LocalDate.of(2036, 2, 7))
        )

        val firstPlacementPeriod = DateRange(LocalDate.of(2021, 1, 11), LocalDate.of(2023, 1, 8))
        insertPlacement(testChild_1.id, firstPlacementPeriod, DAYCARE, testDaycare.id)

        val secondPlacementPeriod = DateRange(LocalDate.of(2023, 1, 9), LocalDate.of(2023, 7, 31))
        insertPlacement(testChild_1.id, secondPlacementPeriod, DAYCARE, testDaycare.id)

        db.transaction { tx ->
            generator.generateNewDecisionsForChild(
                tx,
                testChild_1.id,
            )
        }

        val decisions = getAllFeeDecisions().sortedBy { it.validFrom }
        assertEquals(2, decisions.size)
        decisions.first().let { decision ->
            assertEquals(decision.validFrom, firstPlacementPeriod.start)
            assertEquals(decision.validTo, firstPeriod.end)
            assertEquals(decision.headOfFamilyId, testAdult_1.id)
            assertEquals(decision.partnerId, testAdult_2.id)
        }
        decisions.last().let { decision ->
            assertEquals(decision.validFrom, secondPeriod.start)
            assertEquals(decision.validTo, secondPlacementPeriod.end)
            assertEquals(decision.headOfFamilyId, testAdult_2.id)
            assertEquals(decision.partnerId, testAdult_1.id)
        }
    }

    @Test
    fun `retroactive decisions are created after head of family changes`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        val clock = MockEvakaClock(HelsinkiDateTime.of(period.start, LocalTime.MIN))

        insertPartnership(testAdult_2.id, testAdult_1.id, period, clock.now())
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), subPeriod1)
        insertFamilyRelations(testAdult_2.id, listOf(testChild_1.id), subPeriod2)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)

        db.transaction { tx ->
            generator.createRetroactiveFeeDecisions(tx, testAdult_1.id, period.start)
        }

        assertEquals(
            listOf(Tuple(subPeriod1, testAdult_1.id)),
            getAllFeeDecisions().map { Tuple(it.validDuring, it.headOfFamilyId) }
        )
    }

    @Test
    fun `head of family income difference`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertIncome(testAdult_1.id, 10000, subPeriod2)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllFeeDecisions())
            .extracting({ it.validDuring }, { it.difference })
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, emptySet<FeeDecisionDifference>()),
                Tuple(subPeriod2, setOf(FeeDecisionDifference.INCOME))
            )
    }

    @Test
    fun `partner income difference`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        val clock = MockEvakaClock(HelsinkiDateTime.of(period.start, LocalTime.MIN))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertPartnership(testAdult_1.id, testAdult_2.id, period, clock.now())
        insertIncome(testAdult_2.id, 10000, subPeriod2)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllFeeDecisions())
            .extracting({ it.validDuring }, { it.difference })
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, emptySet<FeeDecisionDifference>()),
                Tuple(subPeriod2, setOf(FeeDecisionDifference.INCOME))
            )
    }

    @Test
    fun `child income difference`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertIncome(testChild_1.id, 10000, subPeriod2)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllFeeDecisions())
            .extracting({ it.validDuring }, { it.difference })
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, emptySet<FeeDecisionDifference>()),
                Tuple(subPeriod2, setOf(FeeDecisionDifference.INCOME))
            )
    }

    @Test
    fun `family size difference`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_2.id), subPeriod2)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllFeeDecisions())
            .extracting({ it.validDuring }, { it.difference }, { it.familySize })
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, emptySet<FeeDecisionDifference>(), 2),
                Tuple(
                    subPeriod2,
                    setOf(FeeDecisionDifference.FAMILY_SIZE, FeeDecisionDifference.FEE_THRESHOLDS),
                    3
                )
            )
    }

    @Test
    fun `placement unit difference`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, subPeriod1, DAYCARE, testDaycare.id)
        insertPlacement(testChild_1.id, subPeriod2, DAYCARE, testDaycare2.id)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllFeeDecisions())
            .extracting(
                { it.validDuring },
                { it.difference },
                { it.children.map { child -> child.placement.unitId } }
            )
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, emptySet<FeeDecisionDifference>(), listOf(testDaycare.id)),
                Tuple(subPeriod2, setOf(FeeDecisionDifference.PLACEMENT), listOf(testDaycare2.id))
            )
    }

    @Test
    fun `placement type difference`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, subPeriod1, DAYCARE, testDaycare.id)
        insertPlacement(testChild_1.id, subPeriod2, DAYCARE_PART_TIME, testDaycare.id)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllFeeDecisions())
            .extracting(
                { it.validDuring },
                { it.difference },
                { it.children.map { child -> child.placement.type } }
            )
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, emptySet<FeeDecisionDifference>(), listOf(DAYCARE)),
                Tuple(subPeriod2, setOf(FeeDecisionDifference.PLACEMENT), listOf(DAYCARE_PART_TIME))
            )
    }

    @Test
    fun `service need difference`() {
        val period = FiniteDateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period.asDateRange())
        val placementId =
            insertPlacement(testChild_1.id, period.asDateRange(), DAYCARE, testDaycare.id)
        insertServiceNeed(placementId, subPeriod1, snDaycareFullDay35.id)
        insertServiceNeed(placementId, subPeriod2, snDaycareFullDayPartWeek25.id)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllFeeDecisions())
            .extracting(
                { it.validDuring },
                { it.difference },
                { it.children.map { child -> child.serviceNeed } }
            )
            .containsExactlyInAnyOrder(
                Tuple(
                    subPeriod1.asDateRange(),
                    emptySet<FeeDecisionDifference>(),
                    listOf(snDaycareFullDay35.toFeeDecisionServiceNeed())
                ),
                Tuple(
                    subPeriod2.asDateRange(),
                    setOf(FeeDecisionDifference.SERVICE_NEED),
                    listOf(snDaycareFullDayPartWeek25.toFeeDecisionServiceNeed())
                )
            )
    }

    @Test
    fun `sibling discount difference`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_2.id), period)
        insertPlacement(testChild_2.id, period, DAYCARE, testDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, subPeriod2, DAYCARE, testDaycare.id)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllFeeDecisions())
            .extracting(
                { it.validDuring },
                { it.difference },
                {
                    it.children
                        .map { child -> Pair(child.child.dateOfBirth, child.siblingDiscount) }
                        .sortedBy { pair -> pair.first }
                }
            )
            .containsExactlyInAnyOrder(
                Tuple(
                    subPeriod1,
                    emptySet<FeeDecisionDifference>(),
                    listOf(Pair(testChild_2.dateOfBirth, 0))
                ),
                Tuple(
                    subPeriod2,
                    setOf(FeeDecisionDifference.CHILDREN, FeeDecisionDifference.SIBLING_DISCOUNT),
                    listOf(Pair(testChild_2.dateOfBirth, 50), Pair(testChild_1.dateOfBirth, 0))
                )
            )
    }

    @Test
    fun `fee alterations difference`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertFeeAlteration(testChild_1.id, 50.0, subPeriod2)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllFeeDecisions())
            .extracting({ it.validDuring }, { it.difference })
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, emptySet<FeeDecisionDifference>()),
                Tuple(subPeriod2, setOf(FeeDecisionDifference.FEE_ALTERATIONS))
            )
    }

    @Test
    fun `fee thresholds difference`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        db.transaction { tx ->
            @Suppress("DEPRECATION")
            tx.createUpdate(
                    "UPDATE fee_thresholds SET valid_during = daterange(lower(valid_during), :endDate, '[]')"
                )
                .bind("endDate", subPeriod1.end)
                .updateExactlyOne()
            tx.insert(
                FeeThresholds(
                    validDuring = subPeriod2.copy(end = null),
                    minIncomeThreshold2 = 213600,
                    minIncomeThreshold3 = 275600,
                    minIncomeThreshold4 = 312900,
                    minIncomeThreshold5 = 350200,
                    minIncomeThreshold6 = 387400,
                    maxIncomeThreshold2 = 482300,
                    maxIncomeThreshold3 = 544300,
                    maxIncomeThreshold4 = 581600,
                    maxIncomeThreshold5 = 618900,
                    maxIncomeThreshold6 = 656100,
                    incomeMultiplier2 = BigDecimal("0.1070"),
                    incomeMultiplier3 = BigDecimal("0.1070"),
                    incomeMultiplier4 = BigDecimal("0.1070"),
                    incomeMultiplier5 = BigDecimal("0.1070"),
                    incomeMultiplier6 = BigDecimal("0.1070"),
                    incomeThresholdIncrease6Plus = 14200,
                    siblingDiscount2 = BigDecimal("0.5"),
                    siblingDiscount2Plus = BigDecimal("0.8"),
                    maxFee = 28800,
                    minFee = 2700,
                    temporaryFee = 2900,
                    temporaryFeePartDay = 1500,
                    temporaryFeeSibling = 1500,
                    temporaryFeeSiblingPartDay = 800
                )
            )
        }

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllFeeDecisions())
            .extracting({ it.validDuring }, { it.difference })
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, emptySet<FeeDecisionDifference>()),
                Tuple(subPeriod2, setOf(FeeDecisionDifference.FEE_THRESHOLDS))
            )
    }

    @Test
    fun `difference with overlapping drafts`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 6, 30))
        val subPeriod2 = DateRange(LocalDate.of(2022, 7, 1), LocalDate.of(2022, 12, 31))
        val subPeriod3 = DateRange(LocalDate.of(2022, 4, 1), LocalDate.of(2022, 9, 1))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, subPeriod1, DAYCARE, testDaycare.id)
        insertPlacement(testChild_1.id, subPeriod2, DAYCARE_PART_TIME, testDaycare.id)
        insertIncome(testChild_1.id, 10000, subPeriod3)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllFeeDecisions())
            .extracting({ it.validFrom }, { it.validTo }, { it.difference })
            .containsExactlyInAnyOrder(
                Tuple(
                    LocalDate.of(2022, 1, 1),
                    LocalDate.of(2022, 3, 31),
                    emptySet<FeeDecisionDifference>()
                ),
                Tuple(
                    LocalDate.of(2022, 4, 1),
                    LocalDate.of(2022, 6, 30),
                    setOf(FeeDecisionDifference.INCOME)
                ),
                Tuple(
                    LocalDate.of(2022, 7, 1),
                    LocalDate.of(2022, 9, 1),
                    setOf(FeeDecisionDifference.PLACEMENT)
                ),
                Tuple(
                    LocalDate.of(2022, 9, 2),
                    LocalDate.of(2022, 12, 31),
                    setOf(FeeDecisionDifference.INCOME)
                )
            )
    }

    @Test
    fun `difference between sent and draft`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, subPeriod1, DAYCARE, testDaycare.id)
        db.transaction { tx ->
            generator.generateNewDecisionsForAdult(tx, testAdult_1.id)
            @Suppress("DEPRECATION")
            tx.createUpdate("UPDATE fee_decision SET status = 'SENT'").execute()
        }
        insertPlacement(testChild_1.id, subPeriod2, DAYCARE_PART_TIME, testDaycare.id)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllFeeDecisions())
            .extracting({ it.validDuring }, { it.status }, { it.difference })
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, FeeDecisionStatus.SENT, emptySet<FeeDecisionDifference>()),
                Tuple(subPeriod2, FeeDecisionStatus.DRAFT, setOf(FeeDecisionDifference.PLACEMENT))
            )
    }

    @Test
    fun `difference when drafts replaces sent`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        db.transaction { tx ->
            generator.generateNewDecisionsForAdult(tx, testAdult_1.id)
            @Suppress("DEPRECATION")
            tx.createUpdate("UPDATE fee_decision SET status = 'SENT'").execute()
            @Suppress("DEPRECATION") tx.createUpdate("DELETE FROM placement").execute()
        }
        insertPlacement(testChild_1.id, subPeriod1, DAYCARE_PART_TIME, testDaycare2.id)
        insertPlacement(testChild_1.id, subPeriod2, DAYCARE, testDaycare2.id)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllFeeDecisions())
            .extracting({ it.validDuring }, { it.status }, { it.difference })
            .containsExactlyInAnyOrder(
                Tuple(period, FeeDecisionStatus.SENT, emptySet<FeeDecisionDifference>()),
                Tuple(subPeriod1, FeeDecisionStatus.DRAFT, setOf(FeeDecisionDifference.PLACEMENT)),
                Tuple(subPeriod2, FeeDecisionStatus.DRAFT, setOf(FeeDecisionDifference.PLACEMENT))
            )
    }

    @Test
    fun `difference when gap between two drafts`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 2))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, subPeriod1, DAYCARE, testDaycare.id)
        insertPlacement(testChild_1.id, subPeriod2, DAYCARE, testDaycare2.id)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllFeeDecisions())
            .extracting({ it.validDuring }, { it.difference })
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, emptySet<FeeDecisionDifference>()),
                Tuple(subPeriod2, emptySet<FeeDecisionDifference>())
            )
    }

    @Test
    fun `difference when gap between sent and draft`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 2))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, subPeriod1, DAYCARE, testDaycare.id)
        db.transaction { tx ->
            generator.generateNewDecisionsForAdult(tx, testAdult_1.id)
            @Suppress("DEPRECATION")
            tx.createUpdate("UPDATE fee_decision SET status = 'SENT'").execute()
        }
        insertPlacement(testChild_1.id, subPeriod2, DAYCARE, testDaycare2.id)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllFeeDecisions())
            .extracting({ it.validDuring }, { it.status }, { it.difference })
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, FeeDecisionStatus.SENT, emptySet<FeeDecisionDifference>()),
                Tuple(subPeriod2, FeeDecisionStatus.DRAFT, emptySet<FeeDecisionDifference>())
            )
    }

    @Test
    fun `new fee decisions are not generated if new draft would be equal to already existing sent decisions that cover the period`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        db.transaction { tx ->
            tx.upsertFeeDecisions(
                listOf(
                    createFeeDecisionFixture(
                        status = FeeDecisionStatus.SENT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_1.id,
                        period = period.copy(end = period.start.plusDays(7)),
                        children =
                            listOf(
                                createFeeDecisionChildFixture(
                                    childId = testChild_1.id,
                                    dateOfBirth = testChild_1.dateOfBirth,
                                    placementUnitId = testDaycare.id,
                                    placementType = DAYCARE,
                                    serviceNeed = snDefaultDaycare.toFeeDecisionServiceNeed()
                                )
                            )
                    ),
                    createFeeDecisionFixture(
                        status = FeeDecisionStatus.SENT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_1.id,
                        period = period.copy(start = period.start.plusDays(8)),
                        children =
                            listOf(
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

            generator.generateNewDecisionsForAdult(tx, testAdult_1.id)
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
            tx.upsertFeeDecisions(
                listOf(
                    createFeeDecisionFixture(
                        status = FeeDecisionStatus.SENT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_1.id,
                        period = period,
                        children =
                            listOf(
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

            generator.generateNewDecisionsForAdult(tx, testAdult_1.id)
        }

        val decisions = getAllFeeDecisions().sortedBy { it.validFrom }
        assertEquals(3, decisions.size)

        val drafts = decisions.filter { it.status == FeeDecisionStatus.DRAFT }
        assertEquals(2, drafts.size)
        drafts.first().let { draft ->
            assertEquals(shorterPeriod.start, draft.validFrom)
            assertEquals(shorterPeriod.end, draft.validTo)
            assertEquals(17300, draft.totalFee)
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
            assertEquals(28900, draft.totalFee)
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
            tx.upsertFeeDecisions(
                listOf(
                    createFeeDecisionFixture(
                        status = FeeDecisionStatus.SENT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_1.id,
                        period = period,
                        children =
                            listOf(
                                createFeeDecisionChildFixture(
                                    childId = testChild_1.id,
                                    dateOfBirth = testChild_1.dateOfBirth,
                                    placementUnitId = testDaycare.id,
                                    placementType = DAYCARE,
                                    serviceNeed = snDefaultDaycare.toFeeDecisionServiceNeed()
                                )
                            )
                    ),
                    createFeeDecisionFixture(
                        status = FeeDecisionStatus.DRAFT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_1.id,
                        period = olderPeriod,
                        children =
                            listOf(
                                createFeeDecisionChildFixture(
                                    childId = testChild_1.id,
                                    dateOfBirth = testChild_1.dateOfBirth,
                                    placementUnitId = testDaycare.id,
                                    placementType = DAYCARE,
                                    serviceNeed = serviceNeed.toFeeDecisionServiceNeed()
                                )
                            )
                    ),
                    createFeeDecisionFixture(
                        status = FeeDecisionStatus.DRAFT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_1.id,
                        period = newerPeriod,
                        children =
                            listOf(
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

            generator.generateNewDecisionsForAdult(tx, testAdult_1.id)
        }

        val decisions = getAllFeeDecisions().sortedBy { it.validFrom }
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
        insertFamilyRelations(
            testAdult_1.id,
            listOf(testChild_1.id, testChild_2.id, testChild_3.id),
            period
        )
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertPlacement(
            testChild_2.id,
            period.copy(start = period.start.plusMonths(1)),
            DAYCARE,
            testDaycare.id
        )
        insertPlacement(
            testChild_3.id,
            period.copy(start = period.start.plusMonths(2)),
            DAYCARE,
            testDaycare.id
        )

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

        val decisions = getAllFeeDecisions().sortedBy { it.validFrom }
        assertEquals(3, decisions.size)
        decisions[0].let { decision ->
            assertEquals(4, decision.familySize)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.start.plusMonths(1).minusDays(1), decision.validTo)
            assertEquals(28900, decision.totalFee)
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
            assertEquals(43400, decision.totalFee)
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
            assertEquals(49200, decision.totalFee)
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

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

        val decisions = getAllFeeDecisions().sortedBy { it.validFrom }
        assertEquals(3, decisions.size)
        decisions[0].let { decision ->
            assertEquals(4, decision.familySize)
            assertEquals(period_1.start, decision.validFrom)
            assertEquals(period_1.end, decision.validTo)
            assertEquals(28900, decision.totalFee)
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
            assertEquals(28900, decision.totalFee)
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
            assertEquals(28900, decision.totalFee)
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

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

        val decisions = getAllFeeDecisions().sortedBy { it.validFrom }
        assertEquals(2, decisions.size)
        decisions[0].let { decision ->
            assertEquals(subPeriod_1.start, decision.validFrom)
            assertEquals(subPeriod_1.end, decision.validTo)
            assertEquals(28900, decision.totalFee)
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
            assertEquals(28900, decision.totalFee)
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

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

        val decisions = getAllFeeDecisions().sortedBy { it.validFrom }
        assertEquals(2, decisions.size)
        decisions[0].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(subPeriod_1.start, decision.validFrom)
            assertEquals(subPeriod_1.end, decision.validTo)
            assertEquals(10700, decision.totalFee)
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
            assertEquals(28900, decision.totalFee)
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

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

        val decisions = getAllFeeDecisions().sortedBy { it.validFrom }
        assertEquals(1, decisions.size)
        decisions[0].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(10700, decision.totalFee)
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
        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

        val newDecisions = getAllFeeDecisions()
        assertEquals(1, newDecisions.size)
        newDecisions[0].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(28900, decision.totalFee)
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

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

        val decisions = getAllFeeDecisions().sortedBy { it.validFrom }
        assertEquals(2, decisions.size)
        decisions[0].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(subPeriod_1.start, decision.validFrom)
            assertEquals(subPeriod_1.end, decision.validTo)
            assertEquals(10700, decision.totalFee)
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
            assertEquals(28900, decision.totalFee)
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
        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

        val newDecisions = getAllFeeDecisions()
        assertEquals(1, newDecisions.size)
        newDecisions[0].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(28900, decision.totalFee)
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
    fun `fee decision family sizes and sibling discounts are formed correctly when fridge partners change`() {
        val firstPeriod = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31))
        val secondPeriod = DateRange(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 12, 31))
        val wholePeriod = firstPeriod.copy(end = secondPeriod.end)
        val clock =
            MockEvakaClock(HelsinkiDateTime.Companion.of(wholePeriod.start, LocalTime.of(0, 0)))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), wholePeriod)
        insertFamilyRelations(testAdult_2.id, listOf(testChild_3.id), wholePeriod)
        insertFamilyRelations(testAdult_3.id, listOf(testChild_4.id), wholePeriod)

        insertPartnership(testAdult_1.id, testAdult_2.id, firstPeriod, clock.now())
        insertPartnership(testAdult_1.id, testAdult_3.id, secondPeriod, clock.now())

        insertPlacement(testChild_1.id, wholePeriod, DAYCARE, testDaycare.id)
        insertPlacement(testChild_3.id, wholePeriod, DAYCARE, testDaycare.id)
        insertPlacement(testChild_4.id, wholePeriod, DAYCARE, testDaycare.id)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

        val decisions =
            getAllFeeDecisions()
                .sortedBy { it.children.minOf { it.child.dateOfBirth } }
                .sortedBy { it.validFrom }
        assertEquals(4, decisions.size)
        decisions[0].let { decision ->
            assertEquals(firstPeriod, decision.validDuring)
            assertEquals(4, decision.familySize)
            assertEquals(testAdult_2.id, decision.headOfFamilyId)
            assertEquals(testAdult_1.id, decision.partnerId)
            assertEquals(2, decision.children.size)
            decision.children.first().let { c ->
                assertEquals(testChild_3.id, c.child.id)
                assertEquals(0, c.siblingDiscount)
            }
            decision.children.last().let { c ->
                assertEquals(testChild_1.id, c.child.id)
                assertEquals(50, c.siblingDiscount)
            }
        }
        decisions[1].let { decision ->
            assertEquals(firstPeriod, decision.validDuring)
            assertEquals(testAdult_3.id, decision.headOfFamilyId)
            assertEquals(null, decision.partnerId)
            assertEquals(2, decision.familySize)
            assertEquals(1, decision.children.size)
            decision.children.first().let { c ->
                assertEquals(testChild_4.id, c.child.id)
                assertEquals(0, c.siblingDiscount)
            }
        }
        decisions[2].let { decision ->
            assertEquals(secondPeriod, decision.validDuring)
            assertEquals(4, decision.familySize)
            assertEquals(testAdult_3.id, decision.headOfFamilyId)
            assertEquals(testAdult_1.id, decision.partnerId)
            assertEquals(2, decision.children.size)
            decision.children.first().let { c ->
                assertEquals(testChild_4.id, c.child.id)
                assertEquals(0, c.siblingDiscount)
            }
            decision.children.last().let { c ->
                assertEquals(testChild_1.id, c.child.id)
                assertEquals(50, c.siblingDiscount)
            }
        }
        decisions[3].let { decision ->
            assertEquals(secondPeriod, decision.validDuring)
            assertEquals(2, decision.familySize)
            assertEquals(testAdult_2.id, decision.headOfFamilyId)
            assertEquals(null, decision.partnerId)
            assertEquals(1, decision.children.size)
            decision.children.first().let { c ->
                assertEquals(testChild_3.id, c.child.id)
                assertEquals(0, c.siblingDiscount)
            }
        }
    }

    @Test
    fun `when fridge partners are guardians of all children, only send one fee decision`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val clock = MockEvakaClock(HelsinkiDateTime.Companion.of(period.start, LocalTime.of(0, 0)))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, testChild_2.id), period)
        insertFamilyRelations(testAdult_2.id, listOf(testChild_8.id), period)
        insertGuardianship(testAdult_1.id, listOf(testChild_1.id, testChild_2.id, testChild_8.id))
        insertGuardianship(testAdult_2.id, listOf(testChild_1.id, testChild_2.id, testChild_8.id))

        insertPartnership(testAdult_1.id, testAdult_2.id, period, clock.now())

        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertPlacement(testChild_2.id, period, DAYCARE, testDaycare.id)
        insertPlacement(testChild_8.id, period, DAYCARE, testDaycare.id)

        insertIncome(testAdult_1.id, 310200, period)
        insertIncome(testAdult_2.id, 310200, period)

        db.transaction {
            generator.generateNewDecisionsForAdult(it, testAdult_1.id)
            generator.generateNewDecisionsForAdult(it, testAdult_2.id)
        }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions[0].let { decision ->
            assertEquals(5, decision.familySize)
            assertEquals(3, decision.children.size)
            assertEquals(testAdult_1.id, decision.headOfFamilyId)
            assertEquals(
                listOf(0, 50, 80),
                decision.children
                    .sortedByDescending { it.child.dateOfBirth }
                    .map { it.siblingDiscount }
            )
        }
    }

    @Test
    fun `when both fridge partners are not guardians of each child, only send one fee decision`() {
        val period = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        val clock = MockEvakaClock(HelsinkiDateTime.Companion.of(period.start, LocalTime.of(0, 0)))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, testChild_2.id), period)
        insertFamilyRelations(testAdult_2.id, listOf(testChild_3.id, testChild_4.id), period)
        insertGuardianship(testAdult_1.id, listOf(testChild_1.id, testChild_2.id, testChild_3.id))
        insertGuardianship(testAdult_2.id, listOf(testChild_2.id, testChild_3.id, testChild_4.id))

        insertPartnership(testAdult_1.id, testAdult_2.id, period, clock.now())

        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertPlacement(testChild_2.id, period, DAYCARE, testDaycare.id)
        insertPlacement(testChild_3.id, period, DAYCARE, testDaycare.id)
        insertPlacement(testChild_4.id, period, DAYCARE, testDaycare.id)

        insertIncome(testAdult_1.id, 310200, period)
        insertIncome(testAdult_2.id, 310200, period)

        db.transaction {
            generator.generateNewDecisionsForAdult(it, testAdult_1.id)
            generator.generateNewDecisionsForAdult(it, testAdult_2.id)
        }

        val decisions = getAllFeeDecisions().sortedBy { it.validFrom }
        assertEquals(1, decisions.size)
        decisions.first().let { decision ->
            assertEquals(6, decision.familySize)
            // testAdult_2 is the fridge parent of the youngest child
            assertEquals(testAdult_2.id, decision.headOfFamilyId)
            assertEquals(testAdult_1.id, decision.partnerId)
            assertEquals(4, decision.children.size)
            decision.children[0].let { child ->
                assertEquals(testChild_4.id, child.child.id)
                assertEquals(0, child.siblingDiscount)
            }
            decision.children[1].let { child ->
                assertEquals(testChild_3.id, child.child.id)
                assertEquals(50, child.siblingDiscount)
            }
            decision.children[2].let { child ->
                assertEquals(testChild_1.id, child.child.id)
                assertEquals(80, child.siblingDiscount)
            }
            decision.children[3].let { child ->
                assertEquals(testChild_2.id, child.child.id)
                assertEquals(80, child.siblingDiscount)
            }
        }
    }

    @Test
    fun `require partnership for fridge family discounts`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, testChild_2.id), period)
        insertFamilyRelations(testAdult_2.id, listOf(testChild_8.id), period)
        insertGuardianship(testAdult_1.id, listOf(testChild_1.id, testChild_2.id))
        insertGuardianship(testAdult_2.id, listOf(testChild_8.id))

        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertPlacement(testChild_2.id, period, DAYCARE, testDaycare.id)
        insertPlacement(testChild_8.id, period, DAYCARE, testDaycare.id)

        insertIncome(testAdult_1.id, 310200, period)
        insertIncome(testAdult_2.id, 310200, period)

        db.transaction {
            generator.generateNewDecisionsForAdult(it, testAdult_1.id)
            generator.generateNewDecisionsForAdult(it, testAdult_2.id)
        }

        val decisions = getAllFeeDecisions().sortedBy { it.validFrom }
        assertEquals(2, decisions.size)
        assertEquals(setOf(3, 2), decisions.map { it.familySize }.toSet())
        assertEquals(
            setOf(testAdult_1.id, testAdult_2.id),
            decisions.map { it.headOfFamilyId }.toSet()
        )

        val decisionForAdult1 = decisions.find { it.headOfFamilyId == testAdult_1.id }
        val decisionForAdult2 = decisions.find { it.headOfFamilyId == testAdult_2.id }
        assertEquals(
            setOf(testChild_1.id, testChild_2.id),
            decisionForAdult1?.children?.map { it.child.id }?.toSet()
        )
        assertEquals(
            setOf(testChild_8.id),
            decisionForAdult2?.children?.map { it.child.id }?.toSet()
        )

        assertEquals(
            listOf(0, 50),
            decisionForAdult1
                ?.children
                ?.sortedByDescending { it.child.dateOfBirth }
                ?.map { it.siblingDiscount }
        )
        assertEquals(
            listOf(0),
            decisionForAdult2
                ?.children
                ?.sortedByDescending { it.child.dateOfBirth }
                ?.map { it.siblingDiscount }
        )
    }

    @Test
    fun `fee decision generation works as expected with changing children`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val clock = MockEvakaClock(HelsinkiDateTime.Companion.of(period.start, LocalTime.of(0, 0)))
        val subPeriod_1 = period.copy(end = period.start.plusMonths(1))
        val subPeriod_2 = period.copy(start = subPeriod_1.end!!.plusDays(1))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPartnership(testAdult_1.id, testAdult_2.id, subPeriod_2, clock.now())
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertIncome(testAdult_1.id, 310200, period)
        insertIncome(testAdult_2.id, 310200, period)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

        val decisions = getAllFeeDecisions().sortedBy { it.validFrom }
        assertEquals(2, decisions.size)
        decisions[0].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(subPeriod_1.start, decision.validFrom)
            assertEquals(subPeriod_1.end, decision.validTo)
            assertEquals(10700, decision.totalFee)
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
            assertEquals(28900, decision.totalFee)
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
        val clock = MockEvakaClock(HelsinkiDateTime.Companion.of(period.start, LocalTime.of(0, 0)))
        val subPeriod_1 = period.copy(end = period.start.plusMonths(1))
        val subPeriod_2 = period.copy(start = subPeriod_1.end!!.plusDays(1))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPartnership(testAdult_1.id, testAdult_2.id, subPeriod_2, clock.now())
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertIncome(testAdult_1.id, 310200, period)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

        val decisions = getAllFeeDecisions().sortedBy { it.validFrom }
        assertEquals(2, decisions.size)
        decisions[0].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(subPeriod_1.start, decision.validFrom)
            assertEquals(subPeriod_1.end, decision.validTo)
            assertEquals(10700, decision.totalFee)
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
            assertEquals(28900, decision.totalFee)
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

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions[0].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(14450, decision.totalFee)
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
    fun `echa increase is applied only once with changing family compositions`() {
        val firstPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        val secondPeriod = DateRange(LocalDate.of(2019, 2, 1), LocalDate.of(2019, 12, 31))
        val combinedPeriod = firstPeriod.copy(end = secondPeriod.end)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), firstPeriod)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, testChild_2.id), secondPeriod)
        insertPlacement(testChild_1.id, combinedPeriod, DAYCARE, testDaycare.id)
        insertEchaIncome(testAdult_1.id, combinedPeriod)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

        val decisions = getAllFeeDecisions().sortedBy { it.validFrom }
        assertEquals(2, decisions.size)
        decisions[0].let { decision ->
            assertEquals(2, decision.familySize)
            assertEquals(firstPeriod.start, decision.validFrom)
            assertEquals(firstPeriod.end, decision.validTo)
            assertEquals(38200, decision.totalFee)
            assertEquals(1, decision.children.size)
            decision.children[0].let { child ->
                assertEquals(testChild_1.id, child.child.id)
                assertEquals(28900, child.baseFee)
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(28900, child.fee)
                assertEquals(38200, child.finalFee)
                assertEquals(listOf(9300), child.feeAlterations.map { it.effect })
            }
        }
        decisions[1].let { decision ->
            assertEquals(3, decision.familySize)
            assertEquals(secondPeriod.start, decision.validFrom)
            assertEquals(secondPeriod.end, decision.validTo)
            assertEquals(38200, decision.totalFee)
            assertEquals(1, decision.children.size)
            decision.children[0].let { child ->
                assertEquals(testChild_1.id, child.child.id)
                assertEquals(28900, child.baseFee)
                assertEquals(DAYCARE, child.placement.type)
                assertEquals(snDefaultDaycare.toFeeDecisionServiceNeed(), child.serviceNeed)
                assertEquals(0, child.siblingDiscount)
                assertEquals(28900, child.fee)
                assertEquals(38200, child.finalFee)
                assertEquals(listOf(9300), child.feeAlterations.map { it.effect })
            }
        }
    }

    @Test
    fun `active fee decisions are not updated on changed past placement end date`() {
        val originalPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        val sentDecision =
            createFeeDecisionFixture(
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
        db.transaction { tx -> tx.upsertFeeDecisions(listOf(sentDecision)) }

        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), originalPeriod)
        val newPeriod = originalPeriod.copy(end = originalPeriod.end!!.minusDays(7))
        insertPlacement(testChild_1.id, newPeriod, DAYCARE, testDaycare.id)

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }

        val decisions = getAllFeeDecisions().sortedBy { it.validFrom }
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
    fun `fee decisions are generated starting earliest on the global fee decision min date`() {
        val period = DateRange(LocalDate.of(2014, 6, 1), LocalDate.of(2015, 6, 1))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_8.id), period)
        insertPlacement(testChild_8.id, period, DAYCARE, testDaycare.id)
        db.transaction { generator.generateNewDecisionsForChild(it, testChild_8.id) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        decisions.first().let { decision ->
            assertEquals(FeeDecisionStatus.DRAFT, decision.status)
            assertEquals(evakaEnv.feeDecisionMinDate, decision.validFrom)
            assertEquals(period.end, decision.validTo)
        }
    }

    @Test
    fun `an empty draft is generated when a placement is moved to start later and a family update is triggered`() {
        val originalPlacementPeriod =
            DateRange(LocalDate.now().minusWeeks(2), LocalDate.now().plusYears(1))
        val familyPeriod = DateRange(LocalDate.now().minusYears(1), LocalDate.now().plusYears(17))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), familyPeriod)
        insertPlacement(
            testChild_1.id,
            originalPlacementPeriod.copy(start = originalPlacementPeriod.start.plusMonths(6)),
            DAYCARE,
            testDaycare.id
        )
        db.transaction { tx ->
            tx.upsertFeeDecisions(
                listOf(
                    createFeeDecisionFixture(
                        status = FeeDecisionStatus.SENT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_1.id,
                        period = originalPlacementPeriod,
                        children =
                            listOf(
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

            generator.generateNewDecisionsForAdult(tx, testAdult_1.id)
        }

        val decisions = getAllFeeDecisions().sortedBy { it.validFrom }
        assertEquals(3, decisions.size)
        val sent = decisions.find { it.status == FeeDecisionStatus.SENT }!!
        val (firstDraft, secondDraft) =
            decisions.filter { it.status == FeeDecisionStatus.DRAFT }.sortedBy { it.validFrom }

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
    fun `zero euro fee decisions get an updated draft when fee thresholds change`() {
        val period = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertIncome(adultId = testAdult_1.id, amount = 0, period = period)
        createFeeDecisionFixture(
                status = FeeDecisionStatus.SENT,
                decisionType = FeeDecisionType.NORMAL,
                headOfFamilyId = testAdult_1.id,
                period = period,
                feeThresholds = oldTestFeeThresholds.getFeeDecisionThresholds(2),
                headOfFamilyIncome =
                    DecisionIncome(
                        effect = IncomeEffect.INCOME,
                        data = mapOf("MAIN_INCOME" to 0),
                        totalIncome = 0,
                        totalExpenses = 0,
                        total = 0,
                        worksAtECHA = false
                    ),
                children =
                    listOf(
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
            )
            .let { fixture ->
                db.transaction { tx ->
                    tx.upsertFeeDecisions(listOf(fixture))
                    generator.generateNewDecisionsForAdult(tx, testAdult_1.id)
                }
            }

        val decisions = getAllFeeDecisions()
        assertEquals(2, decisions.size)

        val sent = decisions.find { it.status == FeeDecisionStatus.SENT }!!
        assertEquals(FeeDecisionStatus.SENT, sent.status)
        assertEquals(period.start, sent.validFrom)
        assertEquals(period.end, sent.validTo)
        assertEquals(1, sent.children.size)
        assertEquals(oldTestFeeThresholds.getFeeDecisionThresholds(2), sent.feeThresholds)
        assertEquals(0, sent.totalFee)

        val draft = decisions.find { it.status == FeeDecisionStatus.DRAFT }!!
        assertEquals(FeeDecisionStatus.DRAFT, draft.status)
        assertEquals(period.start, draft.validFrom)
        assertEquals(period.end, draft.validTo)
        assertEquals(0, draft.children.size)
        assertEquals(testFeeThresholds.getFeeDecisionThresholds(1), draft.feeThresholds)
        assertEquals(0, draft.totalFee)
    }

    @Test
    fun `decision is generated correctly from two identical and concurrent placements`() {
        val period = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(
            testChild_1.id,
            period.copy(end = period.start.plusMonths(1)),
            DAYCARE,
            testDaycare.id
        )
        insertPlacement(
            testChild_1.id,
            period.copy(start = period.start.plusMonths(1).plusDays(1)),
            DAYCARE,
            testDaycare.id
        )

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

        val feeDecisions = getAllFeeDecisions()
        assertEquals(1, feeDecisions.size)
        feeDecisions.first().let { decision ->
            assertEquals(FeeDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(28900, decision.totalFee)
            assertEquals(1, decision.children.size)
            decision.children.first().let { child ->
                assertEquals(testChild_1.id, child.child.id)
                assertEquals(testDaycare.id, child.placement.unitId)
                assertEquals(28900, child.fee)
            }
        }
    }

    @Test
    fun `fee decision is generated for fridge family with two head of families with different children for placed child`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val clock = MockEvakaClock(HelsinkiDateTime.Companion.of(period.start, LocalTime.of(0, 0)))

        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertFamilyRelations(testAdult_2.id, listOf(testChild_2.id), period)
        insertPartnership(testAdult_1.id, testAdult_2.id, period, clock.now())
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }
        assertEquals(1, getAllFeeDecisions().size)
    }

    @Test
    fun `fee decision is generated for fridge family with two head of families with different children for non placed child`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val clock = MockEvakaClock(HelsinkiDateTime.Companion.of(period.start, LocalTime.of(0, 0)))

        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertFamilyRelations(testAdult_2.id, listOf(testChild_2.id), period)
        insertPartnership(testAdult_1.id, testAdult_2.id, period, clock.now())
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_2.id) }
        assertEquals(1, getAllFeeDecisions().size)
    }

    @Test
    fun `empty fee decisions are merged into one even if income changes mid decision`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))

        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertIncome(testAdult_1.id, 500000, period.copy(end = period.start.plusMonths(6)))
        insertIncome(
            testAdult_1.id,
            400000,
            period.copy(start = period.start.plusMonths(6).plusDays(1))
        )
        val sentDecision =
            createFeeDecisionFixture(
                status = FeeDecisionStatus.SENT,
                decisionType = FeeDecisionType.NORMAL,
                headOfFamilyId = testAdult_1.id,
                period = period,
                children =
                    listOf(
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
            )
        db.transaction { it.upsertFeeDecisions(listOf(sentDecision)) }

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }
        val feeDecisions = getAllFeeDecisions()
        assertEquals(2, feeDecisions.size)
        assertEquals(1, feeDecisions.filter { it.status == FeeDecisionStatus.SENT }.size)
        val draft = feeDecisions.find { it.status == FeeDecisionStatus.DRAFT }
        assertEquals(true, draft?.isEmpty())
    }

    @Test
    fun `two separate sent fee decisions are not replaced by a new draft if the combined contents are the same`() {
        val period1 = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 7, 31))
        val period2 = DateRange(LocalDate.of(2020, 8, 1), LocalDate.of(2020, 12, 31))
        val period = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        val clock = MockEvakaClock(HelsinkiDateTime.Companion.of(period.start, LocalTime.of(0, 0)))
        insertPartnership(testAdult_1.id, testAdult_2.id, period, clock.now())
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertFamilyRelations(testAdult_2.id, listOf(testChild_2.id), period)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        val sentDecisions =
            listOf(period1, period2).map { p ->
                createFeeDecisionFixture(
                    status = FeeDecisionStatus.SENT,
                    decisionType = FeeDecisionType.NORMAL,
                    headOfFamilyId = testAdult_1.id,
                    partnerId = testAdult_2.id,
                    period = p,
                    feeThresholds = testFeeThresholds.getFeeDecisionThresholds(4),
                    familySize = 4,
                    children =
                        listOf(
                            createFeeDecisionChildFixture(
                                childId = testChild_1.id,
                                dateOfBirth = testChild_1.dateOfBirth,
                                placementUnitId = testDaycare.id,
                                placementType = DAYCARE,
                                serviceNeed = snDefaultDaycare.toFeeDecisionServiceNeed(),
                                baseFee = 28900,
                                siblingDiscount = 0,
                                fee = 28900
                            )
                        )
                )
            }

        db.transaction { tx ->
            tx.upsertFeeDecisions(sentDecisions)
            listOf(testAdult_1.id, testAdult_2.id).forEach { adultId ->
                generator.generateNewDecisionsForAdult(tx, adultId)
            }
        }

        val decisions = getAllFeeDecisions()
        assertEquals(2, decisions.size)
        val drafts = decisions.filter { it.status == FeeDecisionStatus.DRAFT }
        assertEquals(0, drafts.size)
    }

    @Test
    fun `duplicate sent fee decision is not generated`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val clock = MockEvakaClock(HelsinkiDateTime.Companion.of(period.start, LocalTime.of(0, 0)))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        val placementId = insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }
        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        assertEquals(1, decisions.filter { it.status == FeeDecisionStatus.DRAFT }.size)

        feeDecisionController.confirmFeeDecisionDrafts(
            dbInstance(),
            AuthenticatedUser.Employee(testDecisionMaker_2.id, setOf(UserRole.ADMIN)),
            clock,
            listOf(decisions.get(0).id),
            null
        )

        asyncJobRunner.runPendingJobsSync(clock)

        getAllFeeDecisions().let {
            assertEquals(1, it.size)
            assertEquals(1, it.filter { it.status == FeeDecisionStatus.SENT }.size)
        }

        db.transaction {
            @Suppress("DEPRECATION")
            it.createUpdate("DELETE FROM placement WHERE id=:id").bind("id", placementId).execute()
        }

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

        getAllFeeDecisions().let {
            assertEquals(2, it.size)
            assertEquals(1, it.filter { it.status == FeeDecisionStatus.SENT }.size)
            assertEquals(1, it.filter { it.status == FeeDecisionStatus.DRAFT }.size)
        }

        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

        getAllFeeDecisions().let {
            assertEquals(1, it.size)
            assertEquals(1, it.filter { it.status == FeeDecisionStatus.SENT }.size)
        }
    }

    @Test
    fun `duplicate sent fee decision is not generated if there is a draft in the past`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        val clock =
            MockEvakaClock(HelsinkiDateTime.Companion.of(subPeriod2.start, LocalTime.of(0, 0)))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, subPeriod1, DAYCARE, testDaycare.id)
        insertPlacement(testChild_1.id, subPeriod2, DAYCARE_PART_TIME, testDaycare2.id)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }
        val decisions = getAllFeeDecisions()
        assertEquals(2, decisions.size)
        assertEquals(2, decisions.filter { it.status == FeeDecisionStatus.DRAFT }.size)

        val firstDecision = decisions.filter { it.validDuring.end == subPeriod2.end }.first()

        feeDecisionController.confirmFeeDecisionDrafts(
            dbInstance(),
            AuthenticatedUser.Employee(testDecisionMaker_2.id, setOf(UserRole.ADMIN)),
            clock,
            listOf(firstDecision.id),
            null
        )

        asyncJobRunner.runPendingJobsSync(clock)

        getAllFeeDecisions().let {
            assertEquals(2, it.size)
            assertEquals(1, it.filter { it.status == FeeDecisionStatus.SENT }.size)
            assertEquals(1, it.filter { it.status == FeeDecisionStatus.DRAFT }.size)
        }

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

        getAllFeeDecisions().let {
            assertEquals(2, it.size)
            assertEquals(1, it.filter { it.status == FeeDecisionStatus.SENT }.size)
            assertEquals(1, it.filter { it.status == FeeDecisionStatus.DRAFT }.size)
        }
    }

    @Test
    fun `head of family changes`() {
        val admin = AuthenticatedUser.Employee(testDecisionMaker_2.id, setOf(UserRole.ADMIN))
        val period = FiniteDateRange(LocalDate.of(2022, 3, 1), LocalDate.of(2022, 4, 30))
        val clock = MockEvakaClock(HelsinkiDateTime.Companion.of(period.start, LocalTime.of(0, 0)))
        insertPlacement(testChild_1.id, period.asDateRange(), DAYCARE, testDaycare.id)
        parentshipController.createParentship(
            dbInstance(),
            admin,
            clock,
            ParentshipController.ParentshipRequest(
                headOfChildId = testAdult_1.id,
                childId = testChild_1.id,
                startDate = period.start,
                endDate = period.end
            )
        )
        asyncJobRunner.runPendingJobsSync(clock)

        val parentshipId =
            parentshipController
                .getParentships(dbInstance(), admin, clock, childId = testChild_1.id)
                .single()
                .data
                .id

        getAllFeeDecisions().also { decisions ->
            assertEquals(1, decisions.size)
            feeDecisionController.confirmFeeDecisionDrafts(
                dbInstance(),
                admin,
                clock,
                decisions.map { it.id },
                null
            )
        }
        asyncJobRunner.runPendingJobsSync(clock)

        assertEquals(FeeDecisionStatus.SENT, getAllFeeDecisions().single().status)

        val dateOfChange = LocalDate.of(2022, 4, 1)
        parentshipController.updateParentship(
            dbInstance(),
            admin,
            clock,
            parentshipId,
            ParentshipController.ParentshipUpdateRequest(
                startDate = period.start,
                endDate = dateOfChange.minusDays(1)
            )
        )
        asyncJobRunner.runPendingJobsSync(clock)

        getAllFeeDecisions().also { decisions ->
            assertEquals(2, decisions.size)
            assertEquals(1, decisions.filter { it.status == FeeDecisionStatus.SENT }.size)
            decisions
                .find { it.status == FeeDecisionStatus.DRAFT }!!
                .let {
                    assertEquals(dateOfChange, it.validDuring.start)
                    assertEquals(0, it.totalFee)
                }
        }

        partnershipsController.createPartnership(
            dbInstance(),
            admin,
            clock,
            PartnershipsController.PartnershipRequest(
                person1Id = testAdult_1.id,
                person2Id = testAdult_2.id,
                startDate = dateOfChange,
                endDate = null
            )
        )
        asyncJobRunner.runPendingJobsSync(clock)

        parentshipController.createParentship(
            dbInstance(),
            admin,
            clock,
            ParentshipController.ParentshipRequest(
                headOfChildId = testAdult_2.id,
                childId = testChild_1.id,
                startDate = dateOfChange,
                endDate = period.end
            )
        )
        asyncJobRunner.runPendingJobsSync(clock)

        // Stayed the same
        getFeeDecisions(testAdult_1.id).also { decisions ->
            assertEquals(2, decisions.size)
            assertEquals(1, decisions.filter { it.status == FeeDecisionStatus.SENT }.size)
            decisions
                .find { it.status == FeeDecisionStatus.DRAFT }!!
                .let {
                    assertEquals(dateOfChange, it.validDuring.start)
                    assertEquals(0, it.totalFee)
                }
        }

        getFeeDecisions(testAdult_2.id).also { decisions ->
            assertEquals(1, decisions.size)
            decisions
                .find { it.status == FeeDecisionStatus.DRAFT }!!
                .let {
                    assertEquals(dateOfChange, it.validDuring.start)
                    assertNotEquals(0, it.totalFee)
                }
        }
    }

    @Test
    fun `a new fee decision is not generated if incomes change from NOT_AVAILABLE to INCOMPLETE`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))
        val incomePeriod = period.copy(end = null)
        val clock = MockEvakaClock(HelsinkiDateTime.Companion.of(period.start, LocalTime.of(0, 0)))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertIncome(testAdult_1.id, 310200, incomePeriod, IncomeEffect.NOT_AVAILABLE)
        insertIncome(testChild_1.id, 310200, incomePeriod, IncomeEffect.NOT_AVAILABLE)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)

        feeDecisionController.confirmFeeDecisionDrafts(
            dbInstance(),
            AuthenticatedUser.Employee(testDecisionMaker_2.id, setOf(UserRole.ADMIN)),
            clock,
            listOf(decisions.get(0).id),
            null
        )

        asyncJobRunner.runPendingJobsSync(clock)

        db.transaction {
            @Suppress("DEPRECATION")
            it.createUpdate("UPDATE income SET effect = 'INCOMPLETE'").execute()
        }

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

        // No new DRAFT is generated because the only diff was head of family income type change
        // NOT_AVAILABLE -> INCOMPLETE
        assertEquals(1, getAllFeeDecisions().size)
    }

    private fun assertEqualEnoughDecisions(expected: FeeDecision, actual: FeeDecision) {
        val createdAt = HelsinkiDateTime.now()
        FeeDecisionId(UUID.randomUUID()).let { uuid ->
            assertEquals(
                expected.copy(id = uuid, created = createdAt),
                actual.copy(id = uuid, created = createdAt)
            )
        }
    }

    private fun assertEqualEnoughDecisions(expected: List<FeeDecision>, actual: List<FeeDecision>) {
        val createdAt = HelsinkiDateTime.now()
        FeeDecisionId(UUID.randomUUID()).let { uuid ->
            assertEquals(
                expected.map { it.copy(id = uuid, created = createdAt) },
                actual.map { it.copy(id = uuid, created = createdAt) }
            )
        }
    }

    private fun insertPlacement(
        childId: ChildId,
        period: DateRange,
        type: fi.espoo.evaka.placement.PlacementType,
        daycareId: DaycareId
    ): PlacementId {
        return db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    type = type,
                    childId = childId,
                    unitId = daycareId,
                    startDate = period.start,
                    endDate = period.end!!
                )
            )
        }
    }

    private fun insertFamilyRelations(
        headOfFamilyId: PersonId,
        childIds: List<ChildId>,
        period: DateRange
    ) {
        db.transaction { tx ->
            childIds.forEach { childId ->
                tx.insert(
                    DevParentship(
                        childId = childId,
                        headOfChildId = headOfFamilyId,
                        startDate = period.start,
                        endDate = period.end!!
                    )
                )
            }
        }
    }

    private fun insertGuardianship(guardian: PersonId, childIds: List<ChildId>) {
        db.transaction { tx ->
            childIds.forEach { childId -> tx.insertGuardian(guardian, childId) }
        }
    }

    private fun insertPartnership(
        adultId1: PersonId,
        adultId2: PersonId,
        period: DateRange,
        createdAt: HelsinkiDateTime
    ) {
        db.transaction { tx ->
            tx.insertTestPartnership(
                adultId1,
                adultId2,
                startDate = period.start,
                endDate = period.end,
                createdAt = createdAt
            )
        }
    }

    private fun insertServiceNeed(
        placementId: PlacementId,
        period: FiniteDateRange,
        optionId: ServiceNeedOptionId
    ) {
        db.transaction { tx ->
            tx.insert(
                DevServiceNeed(
                    placementId = placementId,
                    startDate = period.start,
                    endDate = period.end,
                    optionId = optionId,
                    shiftCare = ShiftCareType.NONE,
                    partWeek = false,
                    confirmedBy = EvakaUserId(testDecisionMaker_1.id.raw),
                    confirmedAt = HelsinkiDateTime.now()
                )
            )
        }
    }

    private fun insertIncome(
        adultId: PersonId,
        amount: Int,
        period: DateRange,
        effect: IncomeEffect = IncomeEffect.INCOME
    ) {
        db.transaction { tx ->
            tx.insert(
                DevIncome(
                    personId = adultId,
                    validFrom = period.start,
                    validTo = period.end,
                    effect = effect,
                    data =
                        mapOf(
                            "MAIN_INCOME" to
                                IncomeValue(
                                    amount,
                                    IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS,
                                    1,
                                    calculateMonthlyAmount(
                                        amount,
                                        coefficientMultiplierProvider.multiplier(
                                            IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS
                                        )
                                    )
                                )
                        ),
                    updatedBy = EvakaUserId(testDecisionMaker_1.id.raw)
                )
            )
        }
    }

    private fun insertEchaIncome(adultId: PersonId, period: DateRange) {
        db.transaction { tx ->
            tx.insert(
                DevIncome(
                    personId = adultId,
                    validFrom = period.start,
                    validTo = period.end,
                    effect = IncomeEffect.MAX_FEE_ACCEPTED,
                    data = mapOf(),
                    worksAtEcha = true,
                    updatedBy = EvakaUserId(testDecisionMaker_1.id.raw)
                )
            )
        }
    }

    private fun deleteIncomes() {
        db.transaction { tx ->
            @Suppress("DEPRECATION") tx.createUpdate("DELETE FROM income").execute()
        }
    }

    private fun insertFeeAlteration(personId: PersonId, amount: Double, period: DateRange) {
        db.transaction { tx ->
            tx.insert(
                DevFeeAlteration(
                    id = FeeAlterationId(UUID.randomUUID()),
                    personId,
                    type = FeeAlterationType.DISCOUNT,
                    amount = amount,
                    isAbsolute = false,
                    validFrom = period.start,
                    validTo = period.end,
                    updatedBy = EvakaUserId(testDecisionMaker_1.id.raw)
                )
            )
        }
    }

    private fun getAllFeeDecisions(): List<FeeDecision> {
        return db.read { tx -> tx.createQuery(feeDecisionQuery()).toList<FeeDecision>() }
            .shuffled() // randomize order to expose assumptions
    }

    private fun getFeeDecisions(headOfFamilyId: PersonId): List<FeeDecision> {
        val headPredicate = Predicate { where("$it.head_of_family_id = ${bind(headOfFamilyId)}") }
        return db.read { tx ->
                tx.createQuery(feeDecisionQuery(headPredicate)).toList<FeeDecision>()
            }
            .shuffled() // randomize order to expose assumptions
    }
}
