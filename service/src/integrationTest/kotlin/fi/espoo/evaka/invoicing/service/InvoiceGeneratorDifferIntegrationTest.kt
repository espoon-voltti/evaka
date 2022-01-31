// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.TestInvoiceProductProvider
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.createFeeDecisionChildFixture
import fi.espoo.evaka.invoicing.createFeeDecisionFixture
import fi.espoo.evaka.invoicing.data.upsertFeeDecisions
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.config.testFeatureConfig
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.insertTestParentship
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.resetDatabase
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.snDaycareFullDay35
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testRoundTheClockDaycare
import fi.espoo.evaka.toFeeDecisionServiceNeed
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import kotlin.test.assertEquals

class InvoiceGeneratorDifferIntegrationTest : PureJdbiTest() {

    private val productProvider: InvoiceProductProvider = TestInvoiceProductProvider()
    private val featureConfig: FeatureConfig = testFeatureConfig

    @Autowired
    private val invoiceGeneratorDiffer: InvoiceGeneratorDiffer = InvoiceGeneratorDiffer(DefaultDraftInvoiceGenerator(productProvider), NewDraftInvoiceGenerator(productProvider, featureConfig))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.execute(
                "INSERT INTO holiday (date) VALUES (?), (?), (?), (?), (?), (?)",
                LocalDate.of(2019, 1, 1),
                LocalDate.of(2019, 1, 6),
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2020, 1, 6),
                LocalDate.of(2021, 1, 1),
                LocalDate.of(2021, 1, 6),
            )
        }
    }

    @AfterEach
    fun afterEach() {
        db.transaction { tx ->
            tx.resetDatabase()
        }
    }

    @Test
    fun `Invoice generator differ does not report identical invoices`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 2))
        db.transaction(insertPlacement(testChild_1.id, placementPeriod, PlacementType.TEMPORARY_DAYCARE))

        val diff = db.transaction { invoiceGeneratorDiffer.createInvoiceGeneratorDiff(it, period) }

        assertEquals(0, diff.differentInvoices.size)
        assertEquals(0, diff.onlyInCurrentInvoices.size)
        assertEquals(0, diff.onlyInNewInvoices.size)
    }

    @Test
    fun `Shows the diff when calculation differs`() {
        val period = DateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))
        val placementPeriod = DateRange(LocalDate.of(2021, 1, 4), LocalDate.of(2021, 1, 10))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        db.transaction(insertChildParentRelation(testAdult_2.id, testChild_2.id, period))

        val decision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            placementPeriod,
            testAdult_1.id,
            listOf(
                createFeeDecisionChildFixture(
                    childId = testChild_1.id,
                    dateOfBirth = testChild_1.dateOfBirth,
                    placementUnitId = testRoundTheClockDaycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                    baseFee = 28900,
                    fee = 28900,
                    feeAlterations = listOf()
                )
            )
        )

        val decision2 = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            placementPeriod,
            testAdult_2.id,
            listOf(
                createFeeDecisionChildFixture(
                    childId = testChild_2.id,
                    dateOfBirth = testChild_2.dateOfBirth,
                    placementUnitId = testRoundTheClockDaycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                    baseFee = 14450,
                    fee = 14450,
                    feeAlterations = listOf()
                )
            )
        )
        insertDecisionsAndPlacements(listOf(decision, decision2))

        val diff = db.transaction { invoiceGeneratorDiffer.createInvoiceGeneratorDiff(it, period) }

        assertEquals(2, diff.differentInvoices.size)

        assertEquals("${testAdult_1.id}-2021-02-12", diff.differentInvoices.get(0).invoiceId)
        assertEquals(6084, diff.differentInvoices.get(0).currentInvoice.totalPrice)
        assertEquals(7605, diff.differentInvoices.get(0).newInvoice.totalPrice)

        assertEquals("${testAdult_2.id}-2021-02-12", diff.differentInvoices.get(1).invoiceId)
        assertEquals(3044, diff.differentInvoices.get(1).currentInvoice.totalPrice)
        assertEquals(3805, diff.differentInvoices.get(1).newInvoice.totalPrice)

        assertEquals(0, diff.onlyInCurrentInvoices.size)
        assertEquals(0, diff.onlyInNewInvoices.size)
    }

    private fun insertDecisionsAndPlacements(feeDecisions: List<FeeDecision>) = db.transaction { tx ->
        tx.upsertFeeDecisions(feeDecisions)
        feeDecisions.forEach { decision ->
            decision.children.forEach { part ->
                tx.insertTestPlacement(
                    childId = part.child.id,
                    unitId = part.placement.unitId,
                    startDate = decision.validFrom,
                    endDate = decision.validTo!!,
                    type = part.placement.type
                )
            }
        }
    }

    private fun insertPlacement(childId: ChildId, period: DateRange, type: PlacementType = PlacementType.DAYCARE) = { tx: Database.Transaction ->
        tx.insertTestPlacement(
            childId = childId,
            unitId = testDaycare.id,
            startDate = period.start,
            endDate = period.end!!,
            type = type
        )
    }

    private fun insertChildParentRelation(headOfFamilyId: PersonId, childId: ChildId, period: DateRange) = { tx: Database.Transaction ->
        tx.insertTestParentship(
            headOfFamilyId,
            childId,
            startDate = period.start,
            endDate = period.end!!
        )
    }
}
