// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.TestInvoiceProductProvider
import fi.espoo.evaka.daycare.service.AbsenceCategory
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.daycare.service.AbsenceUpsert
import fi.espoo.evaka.daycare.service.upsertAbsences
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.createFeeDecisionAlterationFixture
import fi.espoo.evaka.invoicing.createFeeDecisionChildFixture
import fi.espoo.evaka.invoicing.createFeeDecisionFixture
import fi.espoo.evaka.invoicing.data.flatten
import fi.espoo.evaka.invoicing.data.invoiceQueryBase
import fi.espoo.evaka.invoicing.data.toInvoice
import fi.espoo.evaka.invoicing.data.upsertFeeDecisions
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.invoicing.domain.FeeAlterationWithEffect
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.Invoice
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceneed.ServiceNeedOption
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.config.testFeatureConfig
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestParentship
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.resetDatabase
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.snDaycareContractDays15
import fi.espoo.evaka.snDaycareFullDay35
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testRoundTheClockDaycare
import fi.espoo.evaka.toFeeDecisionServiceNeed
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class NewInvoiceGeneratorIntegrationTest : PureJdbiTest() {
    private val productProvider: InvoiceProductProvider = TestInvoiceProductProvider()
    private val featureConfig: FeatureConfig = testFeatureConfig
    private val newDraftInvoiceGenerator: DraftInvoiceGenerator =
        NewDraftInvoiceGenerator(productProvider, featureConfig)
    private val generator: InvoiceGenerator = InvoiceGenerator(newDraftInvoiceGenerator)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.execute(
                "INSERT INTO holiday (date) VALUES (?), (?), (?), (?), (?), (?), (?), (?)",
                LocalDate.of(2019, 1, 1),
                LocalDate.of(2019, 1, 6),
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2020, 1, 6),
                LocalDate.of(2021, 1, 1),
                LocalDate.of(2021, 1, 6),
                LocalDate.of(2021, 12, 6),
                LocalDate.of(2021, 12, 24),
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
    fun `invoice generation for child with a day long temporary placement`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 2))
        db.transaction(insertPlacement(testChild_1.id, placementPeriod, PlacementType.TEMPORARY_DAYCARE))

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(testDaycare.areaId, invoice.areaId)
            assertEquals(2900, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(testDaycare.id, invoiceRow.unitId)
                assertEquals(productProvider.mapToProduct(PlacementType.TEMPORARY_DAYCARE), invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(2900, invoiceRow.unitPrice)
                assertEquals(2900, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation for child with one day long part day temporary placement`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 2))
        db.transaction(insertPlacement(testChild_1.id, placementPeriod, PlacementType.TEMPORARY_DAYCARE_PART_DAY))

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(1500, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(productProvider.mapToProduct(PlacementType.TEMPORARY_DAYCARE_PART_DAY), invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(1500, invoiceRow.unitPrice)
                assertEquals(1500, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation for child with a three day long temporary placement`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 4))
        db.transaction(insertPlacement(testChild_1.id, placementPeriod, PlacementType.TEMPORARY_DAYCARE))

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(8700, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(productProvider.mapToProduct(PlacementType.TEMPORARY_DAYCARE), invoiceRow.product)
                assertEquals(3, invoiceRow.amount)
                assertEquals(2900, invoiceRow.unitPrice)
                assertEquals(8700, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation for child with a two day long temporary placement and a day long part day temporary placement`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 4))
        db.transaction(
            insertPlacement(
                testChild_1.id,
                placementPeriod.copy(end = placementPeriod.end!!.minusDays(1)),
                PlacementType.TEMPORARY_DAYCARE
            )
        )
        db.transaction(
            insertPlacement(
                testChild_1.id,
                placementPeriod.copy(start = placementPeriod.end!!),
                PlacementType.TEMPORARY_DAYCARE_PART_DAY
            )
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(7300, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(productProvider.mapToProduct(PlacementType.TEMPORARY_DAYCARE), invoiceRow.product)
                assertEquals(2, invoiceRow.amount)
                assertEquals(2900, invoiceRow.unitPrice)
                assertEquals(5800, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(productProvider.mapToProduct(PlacementType.TEMPORARY_DAYCARE_PART_DAY), invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(1500, invoiceRow.unitPrice)
                assertEquals(1500, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation for two children with temporary placements at the same time`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 2))
        db.transaction(insertPlacement(testChild_1.id, placementPeriod, PlacementType.TEMPORARY_DAYCARE))

        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_2.id, period))
        db.transaction(insertPlacement(testChild_2.id, placementPeriod, PlacementType.TEMPORARY_DAYCARE))

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(4400, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(productProvider.mapToProduct(PlacementType.TEMPORARY_DAYCARE), invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(2900, invoiceRow.unitPrice)
                assertEquals(2900, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_2.id, invoiceRow.child.id)
                assertEquals(productProvider.mapToProduct(PlacementType.TEMPORARY_DAYCARE), invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(1500, invoiceRow.unitPrice)
                assertEquals(1500, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation for two children with part day temporary placements at the same time`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 2))
        db.transaction(insertPlacement(testChild_1.id, placementPeriod, PlacementType.TEMPORARY_DAYCARE_PART_DAY))

        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_2.id, period))
        db.transaction(insertPlacement(testChild_2.id, placementPeriod, PlacementType.TEMPORARY_DAYCARE_PART_DAY))

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(2300, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(productProvider.mapToProduct(PlacementType.TEMPORARY_DAYCARE_PART_DAY), invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(1500, invoiceRow.unitPrice)
                assertEquals(1500, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_2.id, invoiceRow.child.id)
                assertEquals(productProvider.mapToProduct(PlacementType.TEMPORARY_DAYCARE_PART_DAY), invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(800, invoiceRow.unitPrice)
                assertEquals(800, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation for child with a two day long temporary placement that changes head of family during placement`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 3))
        db.transaction(insertPlacement(testChild_1.id, placementPeriod, PlacementType.TEMPORARY_DAYCARE))

        db.transaction(
            insertChildParentRelation(
                testAdult_1.id,
                testChild_1.id,
                placementPeriod.copy(end = placementPeriod.start)
            )
        )
        db.transaction(
            insertChildParentRelation(
                testAdult_2.id,
                testChild_1.id,
                placementPeriod.copy(start = placementPeriod.end!!)
            )
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(2, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(2900, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(productProvider.mapToProduct(PlacementType.TEMPORARY_DAYCARE), invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(2900, invoiceRow.unitPrice)
                assertEquals(2900, invoiceRow.price)
            }
        }
        result.last().let { invoice ->
            assertEquals(testAdult_2.id, invoice.headOfFamily)
            assertEquals(2900, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(productProvider.mapToProduct(PlacementType.TEMPORARY_DAYCARE), invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(2900, invoiceRow.unitPrice)
                assertEquals(2900, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation for child with a day long temporary placement that has no family configured`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 2))
        db.transaction(insertPlacement(testChild_1.id, placementPeriod, PlacementType.TEMPORARY_DAYCARE))

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(0, result.size)
    }

    @Test
    fun `invoice generation for temporary placements does not pick non-temporary placements`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val temporaryPeriod = DateRange(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 3))
        db.transaction(insertPlacement(testChild_1.id, temporaryPeriod, PlacementType.TEMPORARY_DAYCARE))
        val nonTemporaryPeriod = DateRange(LocalDate.of(2019, 1, 4), LocalDate.of(2019, 1, 5))
        db.transaction(insertPlacement(testChild_1.id, nonTemporaryPeriod))

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(5800, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(productProvider.mapToProduct(PlacementType.TEMPORARY_DAYCARE), invoiceRow.product)
                assertEquals(2, invoiceRow.amount)
                assertEquals(2900, invoiceRow.unitPrice)
                assertEquals(5800, invoiceRow.price)
                assertEquals(temporaryPeriod.start, invoiceRow.periodStart)
                assertEquals(temporaryPeriod.end, invoiceRow.periodEnd)
            }
        }
    }

    @Test
    fun `invoice generation from two fee decision with same price results in one invoice row`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            period,
            testAdult_1.id,
            listOf(
                createFeeDecisionChildFixture(
                    childId = testChild_1.id,
                    dateOfBirth = testChild_1.dateOfBirth,
                    placementUnitId = testDaycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                    baseFee = 28900,
                    fee = 28900
                )
            )
        )
        insertDecisionsAndPlacements(
            listOf(
                decision.copy(validDuring = decision.validDuring.copy(end = period.start.plusDays(7))),
                decision.copy(
                    id = FeeDecisionId(UUID.randomUUID()),
                    validDuring = decision.validDuring.copy(start = period.start.plusDays(8))
                )
            )
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(28900, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(productProvider.mapToProduct(PlacementType.DAYCARE), invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation from two fee decision with same price and same fee alterations results in one invoice row`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            period,
            testAdult_1.id,
            listOf(
                createFeeDecisionChildFixture(
                    childId = testChild_1.id,
                    dateOfBirth = testChild_1.dateOfBirth,
                    placementUnitId = testDaycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                    baseFee = 28900,
                    fee = 28900,
                    feeAlterations = listOf(
                        createFeeDecisionAlterationFixture(
                            type = FeeAlteration.Type.DISCOUNT,
                            amount = 20,
                            isAbsolute = false,
                            effect = -5780
                        )
                    )
                )
            )
        )
        insertDecisionsAndPlacements(
            listOf(
                decision.copy(validDuring = decision.validDuring.copy(end = period.start.plusDays(7))),
                decision.copy(
                    id = FeeDecisionId(UUID.randomUUID()),
                    validDuring = decision.validDuring.copy(start = period.start.plusDays(8))
                )
            )
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(23120, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(productProvider.mapToProduct(PlacementType.DAYCARE), invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(
                    productProvider.mapToFeeAlterationProduct(
                        productProvider.mapToProduct(PlacementType.DAYCARE),
                        FeeAlteration.Type.DISCOUNT
                    ),
                    invoiceRow.product
                )
                assertEquals(1, invoiceRow.amount)
                assertEquals(-5780, invoiceRow.unitPrice)
                assertEquals(-5780, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation from two fee decision with the second one having another child results in one invoice row for the first child`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_2.id, period))
        val decision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            period,
            testAdult_1.id,
            listOf(
                createFeeDecisionChildFixture(
                    childId = testChild_1.id,
                    dateOfBirth = testChild_1.dateOfBirth,
                    placementUnitId = testDaycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                    baseFee = 28900,
                    fee = 28900
                )
            )
        )
        insertDecisionsAndPlacements(
            listOf(
                decision.copy(validDuring = decision.validDuring.copy(end = period.start.plusDays(7))),
                decision.copy(
                    id = FeeDecisionId(UUID.randomUUID()),
                    validDuring = decision.validDuring.copy(start = period.start.plusDays(8)),
                    familySize = decision.familySize + 1,
                    children = decision.children + createFeeDecisionChildFixture(
                        childId = testChild_2.id,
                        dateOfBirth = testChild_2.dateOfBirth,
                        placementUnitId = testDaycare.id,
                        placementType = PlacementType.DAYCARE,
                        serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                        baseFee = 28900,
                        siblingDiscount = 50,
                        fee = 14500
                    )
                )
            )
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(40103, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(productProvider.mapToProduct(PlacementType.DAYCARE), invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_2.id, invoiceRow.child.id)
                assertEquals(productProvider.mapToProduct(PlacementType.DAYCARE), invoiceRow.product)
                assertEquals(17, invoiceRow.amount)
                assertEquals(659, invoiceRow.unitPrice)
                assertEquals(11203, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation from two fee decisions makes sure the sum is at most the monthly fee`() {
        // January 2019 has 22 operational days which results in daily price being rounded up
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decisions = listOf(
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.copy(end = period.start.plusDays(6)),
                testAdult_1.id,
                listOf(
                    createFeeDecisionChildFixture(
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        placementUnitId = testDaycare.id,
                        placementType = PlacementType.DAYCARE,
                        serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                        baseFee = 28900,
                        fee = 28900
                    )
                )
            ),
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.copy(start = period.start.plusDays(7)),
                testAdult_1.id,
                listOf(
                    createFeeDecisionChildFixture(
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        placementUnitId = testDaycare2.id,
                        placementType = PlacementType.DAYCARE,
                        serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                        baseFee = 28900,
                        fee = 28900
                    )
                )
            )
        )
        insertDecisionsAndPlacements(decisions)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(28900, invoice.totalPrice)
            assertEquals(3, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(productProvider.mapToProduct(PlacementType.DAYCARE), invoiceRow.product)
                assertEquals(4, invoiceRow.amount)
                assertEquals(1314, invoiceRow.unitPrice)
                assertEquals(5256, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(productProvider.mapToProduct(PlacementType.DAYCARE), invoiceRow.product)
                assertEquals(18, invoiceRow.amount)
                assertEquals(1314, invoiceRow.unitPrice)
                assertEquals(23652, invoiceRow.price)
            }
            invoice.rows[2].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(productProvider.mapToProduct(PlacementType.DAYCARE), invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-8, invoiceRow.unitPrice)
                assertEquals(-8, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation from two fee decisions makes sure the sum is at least the monthly fee`() {
        // March 2019 has 21 operational days which results in daily price being rounded down
        val period = DateRange(LocalDate.of(2019, 3, 1), LocalDate.of(2019, 3, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decisions = listOf(
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.copy(end = period.start.plusDays(6)),
                testAdult_1.id,
                listOf(
                    createFeeDecisionChildFixture(
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        placementUnitId = testDaycare.id,
                        placementType = PlacementType.DAYCARE,
                        serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                        baseFee = 28900,
                        fee = 28900
                    )
                )
            ),
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.copy(start = period.start.plusDays(7)),
                testAdult_1.id,
                listOf(
                    createFeeDecisionChildFixture(
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        placementUnitId = testDaycare2.id,
                        placementType = PlacementType.DAYCARE,
                        serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                        baseFee = 28900,
                        fee = 28900
                    )
                )
            )
        )
        insertDecisionsAndPlacements(decisions)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(28900, invoice.totalPrice)
            assertEquals(3, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(productProvider.mapToProduct(PlacementType.DAYCARE), invoiceRow.product)
                assertEquals(5, invoiceRow.amount)
                assertEquals(1376, invoiceRow.unitPrice)
                assertEquals(6880, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(productProvider.mapToProduct(PlacementType.DAYCARE), invoiceRow.product)
                assertEquals(16, invoiceRow.amount)
                assertEquals(1376, invoiceRow.unitPrice)
                assertEquals(22016, invoiceRow.price)
            }
            invoice.rows[2].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(productProvider.mapToProduct(PlacementType.DAYCARE), invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(4, invoiceRow.unitPrice)
                assertEquals(4, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation from two fee decisions with cleanly split daily prices does not result in a rounding row`() {
        // February 2019 has 20 operational days which results in daily price being split evenly
        val period = DateRange(LocalDate.of(2019, 2, 1), LocalDate.of(2019, 2, 28))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decisions = listOf(
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.copy(end = period.start.plusDays(6)),
                testAdult_1.id,
                listOf(
                    createFeeDecisionChildFixture(
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        placementUnitId = testDaycare.id,
                        placementType = PlacementType.DAYCARE,
                        serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                        baseFee = 28900,
                        fee = 28900
                    )
                )
            ),
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.copy(start = period.start.plusDays(7)),
                testAdult_1.id,
                listOf(
                    createFeeDecisionChildFixture(
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        placementUnitId = testDaycare2.id,
                        placementType = PlacementType.DAYCARE,
                        serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                        baseFee = 28900,
                        fee = 28900
                    )
                )
            )
        )
        insertDecisionsAndPlacements(decisions)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(28900, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(productProvider.mapToProduct(PlacementType.DAYCARE), invoiceRow.product)
                assertEquals(5, invoiceRow.amount)
                assertEquals(1445, invoiceRow.unitPrice)
                assertEquals(7225, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(productProvider.mapToProduct(PlacementType.DAYCARE), invoiceRow.product)
                assertEquals(15, invoiceRow.amount)
                assertEquals(1445, invoiceRow.unitPrice)
                assertEquals(21675, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation from two fee decision with the second one having changed fee for second child results in one invoice row for the first child`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_2.id, period))
        val decision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            period,
            testAdult_1.id,
            listOf(
                createFeeDecisionChildFixture(
                    childId = testChild_1.id,
                    dateOfBirth = testChild_1.dateOfBirth,
                    placementUnitId = testDaycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                    baseFee = 28900,
                    fee = 28900
                ),
                createFeeDecisionChildFixture(
                    childId = testChild_2.id,
                    dateOfBirth = testChild_2.dateOfBirth,
                    placementUnitId = testDaycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                    baseFee = 28900,
                    siblingDiscount = 50,
                    fee = 14500
                )
            )
        )
        insertDecisionsAndPlacements(
            listOf(
                decision.copy(validDuring = decision.validDuring.copy(end = period.start.plusDays(7))),
                decision.copy(
                    id = FeeDecisionId(UUID.randomUUID()),
                    validDuring = decision.validDuring.copy(start = period.start.plusDays(8)),
                    children = listOf(decision.children[0], decision.children[1].copy(fee = 10000, finalFee = 10000))
                )
            )
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(39930, invoice.totalPrice)
            assertEquals(3, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(productProvider.mapToProduct(PlacementType.DAYCARE), invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(testChild_2.id, invoiceRow.child.id)
                assertEquals(productProvider.mapToProduct(PlacementType.DAYCARE), invoiceRow.product)
                assertEquals(5, invoiceRow.amount)
                assertEquals(659, invoiceRow.unitPrice)
                assertEquals(3295, invoiceRow.price)
            }
            invoice.rows[2].let { invoiceRow ->
                assertEquals(testChild_2.id, invoiceRow.child.id)
                assertEquals(productProvider.mapToProduct(PlacementType.DAYCARE), invoiceRow.product)
                assertEquals(17, invoiceRow.amount)
                assertEquals(455, invoiceRow.unitPrice)
                assertEquals(7735, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation from fee decision with one fee alteration creates additional invoice row`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            period,
            testAdult_1.id,
            listOf(
                createFeeDecisionChildFixture(
                    childId = testChild_1.id,
                    dateOfBirth = testChild_1.dateOfBirth,
                    placementUnitId = testDaycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                    baseFee = 28900,
                    fee = 28900,
                    feeAlterations = listOf(
                        createFeeDecisionAlterationFixture(
                            type = FeeAlteration.Type.DISCOUNT,
                            amount = 20,
                            isAbsolute = false,
                            effect = -5780
                        )
                    )
                )
            )
        )
        insertDecisionsAndPlacements(listOf(decision))

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(23120, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(productProvider.mapToProduct(PlacementType.DAYCARE), invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(
                    productProvider.mapToFeeAlterationProduct(
                        productProvider.mapToProduct(PlacementType.DAYCARE),
                        FeeAlteration.Type.DISCOUNT
                    ),
                    invoiceRow.product
                )
                assertEquals(1, invoiceRow.amount)
                assertEquals(-5780, invoiceRow.unitPrice)
                assertEquals(-5780, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation from fee decision with multiple fee alterations creates additional invoice rows`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            period,
            testAdult_1.id,
            listOf(
                createFeeDecisionChildFixture(
                    childId = testChild_1.id,
                    dateOfBirth = testChild_1.dateOfBirth,
                    placementUnitId = testDaycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                    baseFee = 28900,
                    fee = 28900,
                    feeAlterations = listOf(
                        createFeeDecisionAlterationFixture(
                            type = FeeAlteration.Type.DISCOUNT,
                            amount = 20,
                            isAbsolute = false,
                            effect = -5780
                        ),
                        createFeeDecisionAlterationFixture(
                            type = FeeAlteration.Type.INCREASE,
                            amount = 93,
                            isAbsolute = false,
                            effect = 9300
                        )
                    )
                )
            )
        )
        insertDecisionsAndPlacements(listOf(decision))

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(32420, invoice.totalPrice)
            assertEquals(3, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(productProvider.mapToProduct(PlacementType.DAYCARE), invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(
                    productProvider.mapToFeeAlterationProduct(
                        productProvider.mapToProduct(PlacementType.DAYCARE),
                        FeeAlteration.Type.DISCOUNT
                    ),
                    invoiceRow.product
                )
                assertEquals(1, invoiceRow.amount)
                assertEquals(-5780, invoiceRow.unitPrice)
                assertEquals(-5780, invoiceRow.price)
            }

            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(
                    productProvider.mapToFeeAlterationProduct(
                        productProvider.mapToProduct(PlacementType.DAYCARE),
                        FeeAlteration.Type.INCREASE
                    ),
                    invoiceRow.product
                )
                assertEquals(1, invoiceRow.amount)
                assertEquals(9300, invoiceRow.unitPrice)
                assertEquals(9300, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation from fee decision with a 95 percent discount fee alteration`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            period,
            testAdult_1.id,
            listOf(
                createFeeDecisionChildFixture(
                    childId = testChild_1.id,
                    dateOfBirth = testChild_1.dateOfBirth,
                    placementUnitId = testDaycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                    baseFee = 28900,
                    fee = 28900,
                    feeAlterations = listOf(
                        createFeeDecisionAlterationFixture(
                            type = FeeAlteration.Type.DISCOUNT,
                            amount = 95,
                            isAbsolute = false,
                            effect = -27455
                        )
                    )
                )
            )
        )
        insertDecisionsAndPlacements(listOf(decision))

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(1445, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(productProvider.mapToProduct(PlacementType.DAYCARE), invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(
                    productProvider.mapToFeeAlterationProduct(
                        productProvider.mapToProduct(PlacementType.DAYCARE),
                        FeeAlteration.Type.DISCOUNT
                    ),
                    invoiceRow.product
                )
                assertEquals(1, invoiceRow.amount)
                assertEquals(-27455, invoiceRow.unitPrice)
                assertEquals(-27455, invoiceRow.price)
            }
        }
    }

    @Test
    fun `when two people have active fee decisions for the same child only one of them is invoiced`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            period,
            testAdult_1.id,
            listOf(
                createFeeDecisionChildFixture(
                    childId = testChild_1.id,
                    dateOfBirth = testChild_1.dateOfBirth,
                    placementUnitId = testDaycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                    baseFee = 28900,
                    fee = 28900,
                    feeAlterations = listOf()
                )
            )
        )
        db.transaction { tx ->
            tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = period.start,
                endDate = period.end!!
            )
            tx.upsertFeeDecisions(
                listOf(decision, decision.copy(id = FeeDecisionId(UUID.randomUUID()), headOfFamilyId = testAdult_2.id))
            )
        }

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(28900, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(productProvider.mapToProduct(PlacementType.DAYCARE), invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
        }
    }

    @Test
    fun `when a placement ends before the fee decision only the placement period is invoiced`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        val placementPeriod = period.copy(end = period.start.plusDays(7))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            period,
            testAdult_1.id,
            listOf(
                createFeeDecisionChildFixture(
                    childId = testChild_1.id,
                    dateOfBirth = testChild_1.dateOfBirth,
                    placementUnitId = testDaycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                    baseFee = 28900,
                    fee = 28900,
                    feeAlterations = listOf()
                )
            )
        )
        db.transaction { tx ->
            tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = placementPeriod.start,
                endDate = placementPeriod.end!!
            )
            tx.upsertFeeDecisions(
                listOf(decision, decision.copy(id = FeeDecisionId(UUID.randomUUID()), headOfFamilyId = testAdult_2.id))
            )
        }

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(6570, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(placementPeriod.start, invoiceRow.periodStart)
                assertEquals(placementPeriod.end, invoiceRow.periodEnd)
                assertEquals(productProvider.mapToProduct(PlacementType.DAYCARE), invoiceRow.product)
                assertEquals(5, invoiceRow.amount)
                assertEquals(1314, invoiceRow.unitPrice)
                assertEquals(6570, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with sick leave absences covering period`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val absenceDays = datesBetween(period.start, period.end)
            .map { it to AbsenceType.SICKLEAVE }
            .toMap()

        initDataForAbsences(listOf(period), absenceDays)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(0, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(productProvider.fullMonthSickLeave, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-28900, invoiceRow.unitPrice)
                assertEquals(-28900, invoiceRow.price)
            }
        }
    }

    @Test
    fun `50 percent discount is generated with more than 11 sickleave absences`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val absenceDays = datesBetween(period.start, period.end)
            .filter {
                listOf(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
                    .contains(it.dayOfWeek)
            }
            .map { it to AbsenceType.SICKLEAVE }
            .toMap()

        initDataForAbsences(listOf(period), absenceDays)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(14450, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(productProvider.partMonthSickLeave, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-14450, invoiceRow.unitPrice)
                assertEquals(-14450, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with some unknown absences`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val absenceDays = datesBetween(period.start, period.end)
            .filter {
                listOf(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
                    .contains(it.dayOfWeek)
            }
            .map { it to AbsenceType.UNKNOWN_ABSENCE }
            .toMap()

        initDataForAbsences(listOf(period), absenceDays)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(28900, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with unknown absences covering period`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val absenceDays = datesBetween(period.start, period.end)
            .map { it to AbsenceType.UNKNOWN_ABSENCE }
            .toMap()

        initDataForAbsences(listOf(period), absenceDays)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(14450, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(productProvider.fullMonthAbsence, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-14450, invoiceRow.unitPrice)
                assertEquals(-14450, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with some parentleave absences`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val absenceDays = datesBetween(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 4))
            .map { it to AbsenceType.PARENTLEAVE }
            .toMap()

        initDataForAbsences(listOf(period), absenceDays)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(24958, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(productProvider.dailyRefund, invoiceRow.product)
                assertEquals(3, invoiceRow.amount)
                assertEquals(-1314, invoiceRow.unitPrice)
                assertEquals(-3942, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with some parentleave and sickleave absences`() {
        // 22 operational days
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val absenceDays = datesBetween(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 4))
            .map { it to AbsenceType.PARENTLEAVE }
            .plus(
                datesBetween(
                    LocalDate.of(2019, 1, 17),
                    LocalDate.of(2019, 1, 31)
                )
                    .map { it to AbsenceType.SICKLEAVE }
            )
            .toMap()

        initDataForAbsences(listOf(period), absenceDays)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(12479, invoice.totalPrice)
            assertEquals(3, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                // 3 free days because of parental leave
                assertEquals(productProvider.dailyRefund, invoiceRow.product)
                assertEquals(3, invoiceRow.amount)
                assertEquals(-1314, invoiceRow.unitPrice) // 28900 / 22
                assertEquals(-3942, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                // half price because of 11 sick leaves
                assertEquals(productProvider.partMonthSickLeave, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-12479, invoiceRow.unitPrice) // (28900 - 3942) / 2
                assertEquals(-12479, invoiceRow.price)
            }
        }
    }

    @Test
    fun `planned absences do not generate a discount on invoices`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val absenceDays = datesBetween(period.start, period.end)
            .map { it to AbsenceType.PLANNED_ABSENCE }
            .toMap()

        initDataForAbsences(listOf(period), absenceDays)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(28900, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with some parentleave absences for a too old child`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val absenceDays = datesBetween(period.start, LocalDate.of(2019, 1, 4))
            .map { it to AbsenceType.PARENTLEAVE }
            .toMap()

        initDataForAbsences(listOf(period), absenceDays, child = testChild_2)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(28900, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 2 decisions plus sick leave absences`() {
        val periods = listOf(
            DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 11)),
            DateRange(LocalDate.of(2019, 1, 12), LocalDate.of(2019, 1, 31))
        )

        val absenceDays = datesBetween(
            LocalDate.of(2019, 1, 1),
            LocalDate.of(2019, 1, 31)
        )
            .filter {
                listOf(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
                    .contains(it.dayOfWeek)
            }
            .map { it to AbsenceType.SICKLEAVE }
            .toMap()

        initDataForAbsences(periods, absenceDays)

        db.transaction {
            generator.createAndStoreAllDraftInvoices(
                it,
                DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
            )
        }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(11262, invoice.totalPrice)
            assertEquals(5, invoice.rows.size)

            invoice.rows[0].let { invoiceRow ->
                assertEquals(8, invoiceRow.amount)
                assertEquals(1314, invoiceRow.unitPrice)
                assertEquals(10512, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(productProvider.partMonthSickLeave, invoiceRow.product)
                assertEquals(8, invoiceRow.amount)
                assertEquals(-657, invoiceRow.unitPrice)
                assertEquals(-5256, invoiceRow.price)
            }

            invoice.rows[2].let { invoiceRow ->
                assertEquals(14, invoiceRow.amount)
                assertEquals(1314, invoiceRow.unitPrice)
                assertEquals(18396, invoiceRow.price)
            }
            invoice.rows[3].let { invoiceRow ->
                assertEquals(
                    productProvider.mapToFeeAlterationProduct(
                        productProvider.mapToProduct(PlacementType.DAYCARE),
                        FeeAlteration.Type.DISCOUNT
                    ),
                    invoiceRow.product
                )
                assertEquals(14, invoiceRow.amount)
                assertEquals(-455, invoiceRow.unitPrice)
                assertEquals(-6370, invoiceRow.price)
            }
            invoice.rows[4].let { invoiceRow ->
                assertEquals(productProvider.partMonthSickLeave, invoiceRow.product)
                assertEquals(14, invoiceRow.amount)
                assertEquals(-430, invoiceRow.unitPrice)
                assertEquals(-6020, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 2 decisions plus parent leave, force majeure and free absences`() {
        val periods = listOf(
            DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 11)),
            DateRange(LocalDate.of(2019, 1, 12), LocalDate.of(2019, 1, 31))
        )

        val absenceDays = mapOf(
            LocalDate.of(2019, 1, 7) to AbsenceType.PARENTLEAVE,
            LocalDate.of(2019, 1, 14) to AbsenceType.PARENTLEAVE,
            LocalDate.of(2019, 1, 21) to AbsenceType.FORCE_MAJEURE,
            LocalDate.of(2019, 1, 28) to AbsenceType.FREE_ABSENCE
        )

        initDataForAbsences(periods, absenceDays)

        db.transaction {
            generator.createAndStoreAllDraftInvoices(
                it,
                DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
            )
        }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(18647, invoice.totalPrice)
            assertEquals(5, invoice.rows.size)

            invoice.rows[0].let { invoiceRow ->
                assertEquals(8, invoiceRow.amount)
                assertEquals(1314, invoiceRow.unitPrice)
                assertEquals(10512, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(productProvider.dailyRefund, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-1314, invoiceRow.unitPrice)
                assertEquals(-1314, invoiceRow.price)
            }

            invoice.rows[2].let { invoiceRow ->
                assertEquals(14, invoiceRow.amount)
                assertEquals(1314, invoiceRow.unitPrice)
                assertEquals(18396, invoiceRow.price)
            }
            invoice.rows[3].let { invoiceRow ->
                assertEquals(
                    productProvider.mapToFeeAlterationProduct(
                        productProvider.mapToProduct(PlacementType.DAYCARE),
                        FeeAlteration.Type.DISCOUNT
                    ),
                    invoiceRow.product
                )
                assertEquals(14, invoiceRow.amount)
                assertEquals(-455, invoiceRow.unitPrice)
                assertEquals(-6370, invoiceRow.price)
            }
            invoice.rows[4].let { invoiceRow ->
                assertEquals(productProvider.dailyRefund, invoiceRow.product)
                assertEquals(3, invoiceRow.amount)
                assertEquals(-859, invoiceRow.unitPrice)
                assertEquals(-2577, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days and 2 surplus days`() {
        // 22 operational days
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        // 15 operational days first
        // then planned absences
        val plannedAbsenceDays = datesBetween(
            LocalDate.of(2019, 1, 23),
            LocalDate.of(2019, 1, 29)
        )
            .map { it to AbsenceType.PLANNED_ABSENCE }
            .toMap()
        // then 2 more operational days

        initDataForAbsences(listOf(period), plannedAbsenceDays, serviceNeed = snDaycareContractDays15)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(32754, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(productProvider.mapToProduct(PlacementType.DAYCARE), invoiceRow.product)
                assertEquals(2, invoiceRow.amount)
                assertEquals(1927, invoiceRow.unitPrice) // 28900 / 15
                assertEquals(3854, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days, 2 surplus days and a fee alteration`() {
        // 22 operational days
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))

        val decision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            period,
            testAdult_1.id,
            listOf(
                createFeeDecisionChildFixture(
                    childId = testChild_1.id,
                    dateOfBirth = testChild_1.dateOfBirth,
                    placementUnitId = testDaycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareContractDays15.toFeeDecisionServiceNeed(),
                    baseFee = 28900,
                    feeAlterations = listOf(
                        createFeeDecisionAlterationFixture(
                            amount = -50,
                            isAbsolute = false,
                            effect = -14450,
                        )
                    )
                )
            )
        )
        insertDecisionsAndPlacements(listOf(decision))

        // 15 operational days first
        // then planned absences
        val plannedAbsenceDays = datesBetween(
            LocalDate.of(2019, 1, 23),
            LocalDate.of(2019, 1, 29)
        )
            .map { it to AbsenceType.PLANNED_ABSENCE }
            .toMap()
        // then 2 more operational days

        db.transaction { tx ->
            tx.upsertAbsences(
                plannedAbsenceDays.map { (date, type) ->
                    AbsenceUpsert(
                        absenceType = type,
                        childId = testChild_1.id,
                        date = date,
                        category = AbsenceCategory.BILLABLE,
                    )
                },
                EvakaUserId(testDecisionMaker_1.id.raw)
            )
        }

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            val feeAlterationProduct = productProvider.mapToFeeAlterationProduct(
                productProvider.mapToProduct(PlacementType.DAYCARE),
                FeeAlteration.Type.DISCOUNT
            )
            assertEquals(16378, invoice.totalPrice)
            assertEquals(4, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(feeAlterationProduct, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-14450, invoiceRow.unitPrice)
                assertEquals(-14450, invoiceRow.price)
            }
            invoice.rows[2].let { invoiceRow ->
                assertEquals(productProvider.mapToProduct(PlacementType.DAYCARE), invoiceRow.product)
                assertEquals(2, invoiceRow.amount)
                assertEquals(1927, invoiceRow.unitPrice) // 28900 / 15
                assertEquals(3854, invoiceRow.price)
            }
            invoice.rows[3].let { invoiceRow ->
                assertEquals(feeAlterationProduct, invoiceRow.product)
                assertEquals(2, invoiceRow.amount)
                assertEquals(-963, invoiceRow.unitPrice) // -14450 / 15
                assertEquals(-1926, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days, 1 absence and 2 surplus days`() {
        // 22 operational days
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        // 14 operational days first
        // then 1 other absence
        val otherAbsenceDays = listOf(LocalDate.of(2019, 1, 22) to AbsenceType.OTHER_ABSENCE)
        // then planned absences
        val plannedAbsenceDays = datesBetween(
            LocalDate.of(2019, 1, 23),
            LocalDate.of(2019, 1, 29)
        )
            .map { it to AbsenceType.PLANNED_ABSENCE }
        // then 2 more operational days

        val absenceDays = (otherAbsenceDays + plannedAbsenceDays).toMap()
        initDataForAbsences(listOf(period), absenceDays, serviceNeed = snDaycareContractDays15)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(30827, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(productProvider.mapToProduct(PlacementType.DAYCARE), invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(1927, invoiceRow.unitPrice) // 28900 / 15
                assertEquals(1927, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days, 2 fee decisions and 2 surplus days`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))

        val decisions = listOf(
            DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 15)) to 15000,
            DateRange(LocalDate.of(2019, 1, 16), LocalDate.of(2019, 1, 31)) to 9000,
        ).map { (range, fee) ->
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                range,
                testAdult_1.id,
                listOf(
                    createFeeDecisionChildFixture(
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        placementUnitId = testDaycare.id,
                        placementType = PlacementType.DAYCARE,
                        serviceNeed = snDaycareContractDays15.toFeeDecisionServiceNeed(),
                        baseFee = fee,
                        fee = fee,
                        feeAlterations = listOf()
                    )
                )
            )
        }
        insertDecisionsAndPlacements(decisions)

        // 17 operational days in total, the rest are planned absences
        val plannedAbsenceDays = (
            datesBetween(
                LocalDate.of(2019, 1, 1),
                LocalDate.of(2019, 1, 4)
            ) + datesBetween(
                LocalDate.of(2019, 1, 30),
                LocalDate.of(2019, 1, 31)
            )
            )
            .map { it to AbsenceType.PLANNED_ABSENCE }

        db.transaction { tx ->
            tx.upsertAbsences(
                plannedAbsenceDays.map { (date, type) ->
                    AbsenceUpsert(
                        absenceType = type,
                        childId = testChild_1.id,
                        date = date,
                        category = AbsenceCategory.BILLABLE,
                    )
                },
                EvakaUserId(testDecisionMaker_1.id.raw)
            )
        }

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(13000, invoice.totalPrice)
            assertEquals(3, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(7, invoiceRow.amount)
                assertEquals(1000, invoiceRow.unitPrice) // 15000 / 15
                assertEquals(7000, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(productProvider.mapToProduct(PlacementType.DAYCARE), invoiceRow.product)
                assertEquals(8, invoiceRow.amount)
                assertEquals(600, invoiceRow.unitPrice) // 9000 / 15
                assertEquals(4800, invoiceRow.price)
            }
            invoice.rows[2].let { invoiceRow ->
                assertEquals(productProvider.mapToProduct(PlacementType.DAYCARE), invoiceRow.product)
                assertEquals(2, invoiceRow.amount)
                assertEquals(600, invoiceRow.unitPrice) // 9000 / 15
                assertEquals(1200, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days and 15 days of absences - half price`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val otherAbsenceDays = datesBetween(period.start, LocalDate.of(2019, 1, 22)) // 15 operational days
            .map { it to AbsenceType.OTHER_ABSENCE }
        val plannedAbsenceDays = datesBetween(
            LocalDate.of(2019, 1, 23),
            LocalDate.of(2019, 1, 31)
        )
            .map { it to AbsenceType.PLANNED_ABSENCE }
        val absenceDays = (otherAbsenceDays + plannedAbsenceDays).toMap()

        initDataForAbsences(listOf(period), absenceDays, serviceNeed = snDaycareContractDays15)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(14450, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(productProvider.fullMonthAbsence, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-14450, invoiceRow.unitPrice)
                assertEquals(-14450, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days and 14 days of absences - full price`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val otherAbsenceDays = datesBetween(period.start, LocalDate.of(2019, 1, 21)) // 14 operational days
            .map { it to AbsenceType.OTHER_ABSENCE }
        // No absence for 2019-01-22
        val plannedAbsenceDays = datesBetween(
            LocalDate.of(2019, 1, 23),
            LocalDate.of(2019, 1, 31)
        )
            .map { it to AbsenceType.PLANNED_ABSENCE }
        val absenceDays = (otherAbsenceDays + plannedAbsenceDays).toMap()

        initDataForAbsences(listOf(period), absenceDays, serviceNeed = snDaycareContractDays15)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(28900, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days and 1 force majeure absence`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        // 1 force majeure absence
        val forceMajeureAbsenceDays = listOf(LocalDate.of(2019, 1, 2) to AbsenceType.FORCE_MAJEURE)
        // No absence for 14 days
        // Planned absences for the rest of the month
        val plannedAbsenceDays =
            datesBetween(LocalDate.of(2019, 1, 23), LocalDate.of(2019, 1, 31)).map { it to AbsenceType.PLANNED_ABSENCE }
        val absenceDays = (forceMajeureAbsenceDays + plannedAbsenceDays).toMap()

        initDataForAbsences(listOf(period), absenceDays, serviceNeed = snDaycareContractDays15)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(26973, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(productProvider.dailyRefund, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-1927, invoiceRow.unitPrice) // 28900 / 15
                assertEquals(-1927, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with free and paid fee decisions with absences for all paid days`() {
        // 21 operational days
        val period = DateRange(LocalDate.of(2021, 12, 1), LocalDate.of(2021, 12, 31))

        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        db.transaction(insertPlacement(testChild_1.id, period))

        val decisions = listOf(
            DateRange(LocalDate.of(2021, 12, 1), LocalDate.of(2021, 12, 22)) to
                0,
            DateRange(LocalDate.of(2021, 12, 23), LocalDate.of(2021, 12, 31)) to
                28900,
        ).map { (valid, fee) ->
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                valid,
                testAdult_1.id,
                listOf(
                    createFeeDecisionChildFixture(
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        placementUnitId = testDaycare.id,
                        placementType = PlacementType.DAYCARE,
                        serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                        baseFee = fee,
                        fee = fee,
                        feeAlterations = listOf()
                    )
                )
            )
        }
        db.transaction { tx -> tx.upsertFeeDecisions(decisions) }

        val absenceDays = listOf(
            LocalDate.of(2021, 12, 23) to AbsenceType.OTHER_ABSENCE,
            LocalDate.of(2021, 12, 27) to AbsenceType.OTHER_ABSENCE,
            LocalDate.of(2021, 12, 28) to AbsenceType.OTHER_ABSENCE,
            LocalDate.of(2021, 12, 29) to AbsenceType.OTHER_ABSENCE,
            LocalDate.of(2021, 12, 30) to AbsenceType.OTHER_ABSENCE,
            LocalDate.of(2021, 12, 31) to AbsenceType.OTHER_ABSENCE,
        )
        db.transaction { tx ->
            tx.upsertAbsences(
                absenceDays.map { (date, type) ->
                    AbsenceUpsert(
                        absenceType = type,
                        childId = testChild_1.id,
                        date = date,
                        category = AbsenceCategory.BILLABLE
                    )
                },
                EvakaUserId(testDecisionMaker_1.id.raw)
            )
        }

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(8256, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(6, invoiceRow.amount)
                assertEquals(1376, invoiceRow.unitPrice) // 28900 / 21
                assertEquals(8256, invoiceRow.price)
            }
        }
    }

    @Test
    fun `no invoice is generated for 100 percent relief fee decision`() {
        val period = DateRange(LocalDate.of(2021, 12, 1), LocalDate.of(2021, 12, 31))

        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        db.transaction(insertPlacement(testChild_1.id, period))

        val decision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            period,
            testAdult_1.id,
            listOf(
                createFeeDecisionChildFixture(
                    childId = testChild_1.id,
                    dateOfBirth = testChild_1.dateOfBirth,
                    placementUnitId = testDaycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                    baseFee = 28900,
                    fee = 28900,
                    feeAlterations = listOf(
                        FeeAlterationWithEffect(
                            type = FeeAlteration.Type.RELIEF,
                            amount = 100,
                            isAbsolute = false,
                            effect = -28900,
                        )
                    )
                )
            )
        )
        db.transaction { tx -> tx.upsertFeeDecisions(listOf(decision)) }

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)
        assertEquals(0, result.size)
    }

    @Test
    fun `invoice generation with full month of force majeure absences`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val absenceDays = datesBetween(period.start, LocalDate.of(2019, 1, 31))
            .map { it to AbsenceType.FORCE_MAJEURE }
            .toMap()

        initDataForAbsences(listOf(period), absenceDays)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(0, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(productProvider.dailyRefund, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-28900, invoiceRow.unitPrice)
                assertEquals(-28900, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days and 15 days of force majeure absences`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val forceMajeureAbsenceDays = datesBetween(period.start, LocalDate.of(2019, 1, 22)) // 15 operational days
            .map { it to AbsenceType.FORCE_MAJEURE }
        val plannedAbsenceDays =
            datesBetween(
                LocalDate.of(2019, 1, 23),
                LocalDate.of(2019, 1, 31)
            )
                .map { it to AbsenceType.PLANNED_ABSENCE }
        val absenceDays = (forceMajeureAbsenceDays + plannedAbsenceDays).toMap()

        initDataForAbsences(listOf(period), absenceDays, serviceNeed = snDaycareContractDays15)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(0, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(productProvider.dailyRefund, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-28900, invoiceRow.unitPrice)
                assertEquals(-28900, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days and 7 days of force majeure and 8 days of free absences`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val forceMajeureAbsenceDays = datesBetween(period.start, LocalDate.of(2019, 1, 22)) // 15 operational days
            .mapIndexed { idx, it -> it to if (idx % 2 == 0) AbsenceType.FREE_ABSENCE else AbsenceType.FORCE_MAJEURE }
        val plannedAbsenceDays =
            datesBetween(
                LocalDate.of(2019, 1, 23),
                LocalDate.of(2019, 1, 31)
            )
                .map { it to AbsenceType.PLANNED_ABSENCE }
        val absenceDays = (forceMajeureAbsenceDays + plannedAbsenceDays).toMap()

        initDataForAbsences(listOf(period), absenceDays, serviceNeed = snDaycareContractDays15)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(0, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(productProvider.dailyRefund, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-28900, invoiceRow.unitPrice)
                assertEquals(-28900, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation when fee decision is valid only during weekend`() {
        val period = DateRange(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31))
        val weekEnd = DateRange(LocalDate.of(2020, 5, 2), LocalDate.of(2020, 5, 3))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            weekEnd,
            testAdult_1.id,
            listOf(
                createFeeDecisionChildFixture(
                    childId = testChild_1.id,
                    dateOfBirth = testChild_1.dateOfBirth,
                    placementUnitId = testDaycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                    baseFee = 28900,
                    fee = 28900,
                    feeAlterations = listOf()
                )
            )
        )
        insertDecisionsAndPlacements(listOf(decision))

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)
        assertEquals(0, result.size)
    }

    @Test
    fun `invoice generation when fee decision is valid only for two weeks in a round the clock unit`() {
        val period = DateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))
        val placementPeriod = DateRange(LocalDate.of(2021, 1, 18), LocalDate.of(2021, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
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
        insertDecisionsAndPlacements(listOf(decision))

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(15210, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(10, invoiceRow.amount)
                assertEquals(1521, invoiceRow.unitPrice)
                assertEquals(15210, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation when fee decision is valid only during last weekend of month in a round the clock unit`() {
        val period = DateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))
        val placementPeriod = DateRange(LocalDate.of(2021, 1, 30), LocalDate.of(2021, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
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
        insertDecisionsAndPlacements(listOf(decision))

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(3042, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(2, invoiceRow.amount)
                assertEquals(1521, invoiceRow.unitPrice)
                assertEquals(3042, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation when fee decision is valid for all but new year in a round the clock unit`() {
        val period = DateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))
        val placementPeriod = DateRange(LocalDate.of(2021, 1, 2), LocalDate.of(2021, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
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
        insertDecisionsAndPlacements(listOf(decision))

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(28900, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation when fee decision is valid for all but new year and the first weekend in a round the clock unit`() {
        val period = DateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))
        val placementPeriod = DateRange(LocalDate.of(2021, 1, 4), LocalDate.of(2021, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
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
        insertDecisionsAndPlacements(listOf(decision))

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(28900, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation when fee decision is valid for only for the week with epiphany in a round the clock unit`() {
        val period = DateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))
        val placementPeriod = DateRange(LocalDate.of(2021, 1, 4), LocalDate.of(2021, 1, 10))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
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
        insertDecisionsAndPlacements(listOf(decision))

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(7605, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(5, invoiceRow.amount)
                assertEquals(1521, invoiceRow.unitPrice)
                assertEquals(7605, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation for half a month`() {
        // 23 operational days
        val period = DateRange(LocalDate.of(2021, 3, 1), LocalDate.of(2021, 3, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decisions = listOf(
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                // 10 days of daycare
                DateRange(LocalDate.of(2021, 3, 1), LocalDate.of(2021, 3, 12)),
                testAdult_1.id,
                listOf(
                    createFeeDecisionChildFixture(
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        placementUnitId = testDaycare.id,
                        placementType = PlacementType.DAYCARE,
                        serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                        baseFee = 28900,
                        fee = 28900,
                        feeAlterations = listOf()
                    )
                )
            ),
        )
        insertDecisionsAndPlacements(decisions)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(12570, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(10, invoiceRow.amount)
                assertEquals(1257, invoiceRow.unitPrice) // 28900 / 23
                assertEquals(12570, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation when is half a month in a round the clock unit and the rest in a regular unit`() {
        val period = DateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decisions = listOf(
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                DateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 14)),
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
            ),
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                DateRange(LocalDate.of(2021, 1, 15), LocalDate.of(2021, 1, 31)),
                testAdult_1.id,
                listOf(
                    createFeeDecisionChildFixture(
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        placementUnitId = testDaycare.id,
                        placementType = PlacementType.DAYCARE,
                        serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                        baseFee = 28900,
                        fee = 28900,
                        feeAlterations = listOf()
                    )
                )
            )
        )
        insertDecisionsAndPlacements(decisions)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(28900, invoice.totalPrice)
            assertEquals(3, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(12, invoiceRow.amount)
                assertEquals(1521, invoiceRow.unitPrice)
                assertEquals(18252, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(7, invoiceRow.amount)
                assertEquals(1521, invoiceRow.unitPrice)
                assertEquals(10647, invoiceRow.price)
            }
            invoice.rows[2].let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(1, invoiceRow.unitPrice)
                assertEquals(1, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation placement is to a round the clock unit and it changes in the middle of the month`() {
        val firstPeriod = DateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 14))
        val secondPeriod = DateRange(LocalDate.of(2021, 1, 15), LocalDate.of(2021, 1, 31))
        val period = firstPeriod.copy(end = secondPeriod.end)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decisions = listOf(
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                firstPeriod,
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
            ),
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                secondPeriod,
                testAdult_1.id,
                listOf(
                    createFeeDecisionChildFixture(
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        placementUnitId = testRoundTheClockDaycare.id,
                        placementType = PlacementType.DAYCARE_PART_TIME,
                        serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                        baseFee = 28900,
                        fee = 28900,
                        feeAlterations = listOf()
                    )
                )
            )
        )
        insertDecisionsAndPlacements(decisions)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(28900, invoice.totalPrice)
            assertEquals(3, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(12, invoiceRow.amount)
                assertEquals(1521, invoiceRow.unitPrice)
                assertEquals(18252, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(7, invoiceRow.amount)
                assertEquals(1521, invoiceRow.unitPrice)
                assertEquals(10647, invoiceRow.price)
            }
            invoice.rows[2].let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(1, invoiceRow.unitPrice)
                assertEquals(1, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation for round the clock unit with a force majeure absence during the week`() {
        val period = DateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            period,
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
        insertDecisionsAndPlacements(listOf(decision))
        db.transaction { tx ->
            tx.upsertAbsences(
                listOf(
                    AbsenceUpsert(
                        absenceType = AbsenceType.FORCE_MAJEURE,
                        childId = testChild_1.id,
                        date = LocalDate.of(2021, 1, 5),
                        category = AbsenceCategory.BILLABLE
                    )
                ),
                EvakaUserId(testDecisionMaker_1.id.raw)
            )
        }

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(27379, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(-1521, invoiceRow.unitPrice)
                assertEquals(-1521, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation for round the clock unit with a force majeure absence during the weekend`() {
        val period = DateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            period,
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
        insertDecisionsAndPlacements(listOf(decision))
        db.transaction { tx ->
            tx.upsertAbsences(
                listOf(
                    AbsenceUpsert(
                        absenceType = AbsenceType.FORCE_MAJEURE,
                        childId = testChild_1.id,
                        date = LocalDate.of(2021, 1, 31),
                        category = AbsenceCategory.BILLABLE
                    )
                ),
                EvakaUserId(testDecisionMaker_1.id.raw)
            )
        }

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(27379, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(-1521, invoiceRow.unitPrice)
                assertEquals(-1521, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation for round the clock unit with force majeure absences for the whole month`() {
        val period = DateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            period,
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
        insertDecisionsAndPlacements(listOf(decision))
        db.transaction { tx ->
            tx.upsertAbsences(
                datesBetween(period.start, period.end)
                    .map {
                        AbsenceUpsert(
                            absenceType = AbsenceType.FORCE_MAJEURE,
                            childId = testChild_1.id,
                            date = it,
                            category = AbsenceCategory.BILLABLE
                        )
                    }
                    .toList(),
                EvakaUserId(testDecisionMaker_1.id.raw)
            )
        }

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(0, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(-28900, invoiceRow.unitPrice)
                assertEquals(-28900, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation when fee decision is valid only during weekend and there are absences for the whole month`() {
        val period = DateRange(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31))
        val weekEnd = DateRange(LocalDate.of(2020, 5, 2), LocalDate.of(2020, 5, 3))
        val absenceDays = generateSequence(LocalDate.of(2020, 5, 1)) { date -> date.plusDays(1) }
            .takeWhile { date -> date < LocalDate.of(2020, 6, 1) }
            .map { date -> date to AbsenceType.SICKLEAVE }
            .toMap()
        initDataForAbsences(listOf(weekEnd), absenceDays)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)
        assertEquals(0, result.size)
    }

    @Test
    fun `free july 2020 if child has been placed every month since Sep 2019 but not april or may`() {
        initFreeJulyTestData(
            DateRange(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 31)),
            listOf(
                DateRange(LocalDate.of(2019, 9, 1), LocalDate.of(2019, 9, 30)),
                DateRange(LocalDate.of(2019, 10, 1), LocalDate.of(2019, 10, 31)),
                DateRange(LocalDate.of(2019, 11, 1), LocalDate.of(2019, 11, 30)),
                DateRange(LocalDate.of(2019, 12, 1), LocalDate.of(2019, 12, 31)),
                DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31)),
                DateRange(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 29)),
                DateRange(LocalDate.of(2020, 3, 1), LocalDate.of(2020, 3, 31)),
                DateRange(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 6, 30)),
                DateRange(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 31))
            ),
            PlacementType.DAYCARE_PART_TIME
        )
        val result = db.read(getAllInvoices)
        assertEquals(0, result.size)
    }

    @Test
    fun `no free july 2021 if child has been placed every month since Sep 2019 but not april or may`() {
        initFreeJulyTestData(
            DateRange(LocalDate.of(2021, 7, 1), LocalDate.of(2021, 7, 31)),
            listOf(
                DateRange(LocalDate.of(2020, 9, 1), LocalDate.of(2020, 9, 30)),
                DateRange(LocalDate.of(2020, 10, 1), LocalDate.of(2020, 10, 31)),
                DateRange(LocalDate.of(2020, 11, 1), LocalDate.of(2020, 11, 30)),
                DateRange(LocalDate.of(2020, 12, 1), LocalDate.of(2020, 12, 31)),
                DateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31)),
                DateRange(LocalDate.of(2021, 2, 1), LocalDate.of(2021, 2, 28)),
                DateRange(LocalDate.of(2021, 3, 1), LocalDate.of(2021, 3, 31)),
                DateRange(LocalDate.of(2021, 6, 1), LocalDate.of(2021, 6, 30)),
                DateRange(LocalDate.of(2021, 7, 1), LocalDate.of(2021, 7, 31))
            )
        )
        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
    }

    @Test
    fun `free july 2020 if child has been placed every month since Sep 2019, also in april or may`() {
        initFreeJulyTestData(
            DateRange(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 31)),
            listOf(
                DateRange(LocalDate.of(2019, 9, 1), LocalDate.of(2019, 9, 30)),
                DateRange(LocalDate.of(2019, 10, 1), LocalDate.of(2019, 10, 31)),
                DateRange(LocalDate.of(2019, 11, 1), LocalDate.of(2019, 11, 30)),
                DateRange(LocalDate.of(2019, 12, 1), LocalDate.of(2019, 12, 31)),
                DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31)),
                DateRange(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 29)),
                DateRange(LocalDate.of(2020, 3, 1), LocalDate.of(2020, 3, 31)),
                DateRange(LocalDate.of(2020, 4, 1), LocalDate.of(2020, 4, 30)),
                DateRange(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31)),
                DateRange(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 6, 30)),
                DateRange(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 31))
            )
        )
        val result = db.read(getAllInvoices)
        assertEquals(0, result.size)
    }

    @Test
    fun `free july 2020 if child has been placed even for one day every month since Sep 2019`() {
        initFreeJulyTestData(
            DateRange(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 31)),
            listOf(
                DateRange(LocalDate.of(2019, 9, 1), LocalDate.of(2019, 9, 1)),
                DateRange(LocalDate.of(2019, 10, 1), LocalDate.of(2019, 10, 1)),
                DateRange(LocalDate.of(2019, 11, 1), LocalDate.of(2019, 11, 1)),
                DateRange(LocalDate.of(2019, 12, 1), LocalDate.of(2019, 12, 1)),
                DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 1)),
                DateRange(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 1)),
                DateRange(LocalDate.of(2020, 3, 1), LocalDate.of(2020, 3, 1)),
                DateRange(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 6, 1)),
                DateRange(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 1))
            )
        )
        val result = db.read(getAllInvoices)
        assertEquals(0, result.size)
    }

    @Test
    fun `free july 2020 if child has been placed all the time`() {
        initFreeJulyTestData(
            DateRange(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 31)),
            listOf(DateRange(LocalDate.of(2018, 7, 1), LocalDate.of(2021, 7, 31)))
        )
        val result = db.read(getAllInvoices)
        assertEquals(0, result.size)
    }

    @Test
    fun `free july 2021 if child has been placed all the time`() {
        initFreeJulyTestData(
            DateRange(LocalDate.of(2021, 7, 1), LocalDate.of(2021, 7, 31)),
            listOf(DateRange(LocalDate.of(2018, 7, 1), LocalDate.of(2021, 7, 31)))
        )
        val result = db.read(getAllInvoices)
        assertEquals(0, result.size)
    }

    @Test
    fun `free july 2021 if child has been placed in preparatory with daycare all the time`() {
        initFreeJulyTestData(
            DateRange(LocalDate.of(2021, 7, 1), LocalDate.of(2021, 7, 31)),
            listOf(PlacementType.PREPARATORY_DAYCARE to DateRange(LocalDate.of(2018, 7, 1), LocalDate.of(2021, 7, 31)))
        )
        val result = db.read(getAllInvoices)
        assertEquals(0, result.size)
    }

    @Test
    fun `no free july 2020 if even one mandatory month has no placement`() {
        initFreeJulyTestData(
            DateRange(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 31)),
            listOf(
                DateRange(LocalDate.of(2019, 9, 1), LocalDate.of(2019, 9, 30)),
                DateRange(LocalDate.of(2019, 10, 1), LocalDate.of(2019, 10, 31)),
                DateRange(LocalDate.of(2019, 11, 1), LocalDate.of(2019, 11, 30)),
                DateRange(LocalDate.of(2019, 12, 1), LocalDate.of(2019, 12, 31)),
                DateRange(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 28)),
                DateRange(LocalDate.of(2020, 3, 1), LocalDate.of(2020, 3, 31)),
                DateRange(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 6, 30)),
                DateRange(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 31))
            )
        )

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
    }

    @Test
    fun `no free july 2020 if child has a mix of club and daycare placements`() {
        initFreeJulyTestData(
            DateRange(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 31)),
            listOf(
                PlacementType.CLUB to DateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 5, 31)),
                PlacementType.DAYCARE to DateRange(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 7, 31))
            )
        )

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
    }

    @Test
    fun `no free july 2020 if child has a mix of preschool and preschool daycare placements`() {
        initFreeJulyTestData(
            DateRange(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 31)),
            listOf(
                PlacementType.PRESCHOOL to DateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 5, 31)),
                PlacementType.PRESCHOOL_DAYCARE to DateRange(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 7, 31))
            )
        )

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
    }

    @Test
    fun `free july 2020 applies only on july`() {
        initFreeJulyTestData(
            DateRange(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 6, 30)),
            listOf(
                DateRange(LocalDate.of(2019, 9, 1), LocalDate.of(2019, 9, 30)),
                DateRange(LocalDate.of(2019, 10, 1), LocalDate.of(2019, 10, 31)),
                DateRange(LocalDate.of(2019, 11, 1), LocalDate.of(2019, 11, 30)),
                DateRange(LocalDate.of(2019, 12, 1), LocalDate.of(2019, 12, 31)),
                DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31)),
                DateRange(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 28)),
                DateRange(LocalDate.of(2020, 3, 1), LocalDate.of(2020, 3, 31)),
                DateRange(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 6, 30)),
                DateRange(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 31))
            )
        )
        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
    }

    @Test
    fun `plain preschool is not invoiced`() {
        assertFalse(PlacementType.PRESCHOOL.isInvoiced())

        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        initByPeriodAndPlacementType(period, PlacementType.PRESCHOOL)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(0, result.size)
    }

    @Test
    fun `plain preparatory is not invoiced`() {
        assertFalse(PlacementType.PREPARATORY.isInvoiced())

        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        initByPeriodAndPlacementType(period, PlacementType.PREPARATORY)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(0, result.size)
    }

    @Test
    fun `plain club is not invoiced`() {
        assertFalse(PlacementType.CLUB.isInvoiced())

        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        initByPeriodAndPlacementType(period, PlacementType.CLUB)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)

        assertEquals(0, result.size)
    }

    @Test
    fun `invoice codebtor is set when partner on decision is child's guardian`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        initByPeriodAndPlacementType(period, PlacementType.DAYCARE, partner = testAdult_2.id)
        db.transaction {
            it.insertGuardian(testAdult_1.id, testChild_1.id)
            it.insertGuardian(testAdult_2.id, testChild_1.id)
        }

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }
        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(testAdult_2.id, invoice.codebtor)
        }
    }

    @Test
    fun `invoice codebtor is not set when partner on decision is not child's guardian`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        initByPeriodAndPlacementType(period, PlacementType.DAYCARE, partner = testAdult_2.id)
        db.transaction { it.insertGuardian(testAdult_1.id, testChild_1.id) }

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }
        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertNull(invoice.codebtor)
        }
    }

    @Test
    fun `invoice codebtor is not set when partner on decision is not every child's guardian`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        initByPeriodAndPlacementType(
            period,
            PlacementType.DAYCARE,
            children = listOf(testChild_1, testChild_2),
            partner = testAdult_2.id
        )
        db.transaction {
            it.insertGuardian(testAdult_1.id, testChild_1.id)
            it.insertGuardian(testAdult_2.id, testChild_1.id)
            it.insertGuardian(testAdult_1.id, testChild_2.id)
        }

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }
        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertNull(invoice.codebtor)
        }
    }

    @Test
    fun `invoice generation with a fixed daily fee divisor`() {
        // Contains 22 operational days
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        // Override to use 20 days instead when calculating a daily refund
        val generator = InvoiceGenerator(
            NewDraftInvoiceGenerator(
                productProvider,
                featureConfig.copy(dailyFeeDivisorOperationalDaysOverride = 20)
            )
        )

        val absenceDays = listOf(
            LocalDate.of(2019, 1, 2) to AbsenceType.FORCE_MAJEURE,
            LocalDate.of(2019, 1, 3) to AbsenceType.FORCE_MAJEURE
        ).toMap()

        initDataForAbsences(listOf(period), absenceDays)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(26010, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(productProvider.dailyRefund, invoiceRow.product)
                assertEquals(2, invoiceRow.amount)
                assertEquals(-1445, invoiceRow.unitPrice) // 28900 / 20
                assertEquals(-2890, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days for half a month`() {
        // 23 operational days
        val period = DateRange(LocalDate.of(2021, 3, 1), LocalDate.of(2021, 3, 31))

        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decisions = listOf(
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                // 10 days of daycare
                DateRange(LocalDate.of(2021, 3, 1), LocalDate.of(2021, 3, 12)),
                testAdult_1.id,
                listOf(
                    createFeeDecisionChildFixture(
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        placementUnitId = testDaycare.id,
                        placementType = PlacementType.DAYCARE,
                        // 15 contract days
                        serviceNeed = snDaycareContractDays15.toFeeDecisionServiceNeed(),
                        baseFee = 28900,
                        fee = 28900,
                        feeAlterations = listOf()
                    )
                )
            ),
        )
        insertDecisionsAndPlacements(decisions)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(19270, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(10, invoiceRow.amount)
                assertEquals(1927, invoiceRow.unitPrice) // 28900 / 15
                assertEquals(19270, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with a fixed daily fee divisor for half a month`() {
        // 23 operational days
        val period = DateRange(LocalDate.of(2021, 3, 1), LocalDate.of(2021, 3, 31))

        // Override to use 20 days instead when calculating a daily fee
        val generator = InvoiceGenerator(
            NewDraftInvoiceGenerator(
                productProvider,
                featureConfig.copy(dailyFeeDivisorOperationalDaysOverride = 20)
            )
        )

        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decisions = listOf(
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                // 10 days of daycare
                DateRange(LocalDate.of(2021, 3, 1), LocalDate.of(2021, 3, 12)),
                testAdult_1.id,
                listOf(
                    createFeeDecisionChildFixture(
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        placementUnitId = testDaycare.id,
                        placementType = PlacementType.DAYCARE,
                        serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                        baseFee = 28900,
                        fee = 28900,
                        feeAlterations = listOf()
                    )
                )
            ),
        )
        insertDecisionsAndPlacements(decisions)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(14450, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(10, invoiceRow.amount)
                assertEquals(1445, invoiceRow.unitPrice) // 28900 / 20
                assertEquals(14450, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with daily fee divisor 20 for 21 days`() {
        // 23 operational days
        val period = DateRange(LocalDate.of(2021, 3, 1), LocalDate.of(2021, 3, 31))

        // Override to use 20 days instead when calculating a daily fee
        val generator = InvoiceGenerator(
            NewDraftInvoiceGenerator(
                productProvider,
                featureConfig.copy(dailyFeeDivisorOperationalDaysOverride = 20)
            )
        )

        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decisions = listOf(
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                // 21 days of daycare
                DateRange(LocalDate.of(2021, 3, 1), LocalDate.of(2021, 3, 29)),
                testAdult_1.id,
                listOf(
                    createFeeDecisionChildFixture(
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        placementUnitId = testDaycare.id,
                        placementType = PlacementType.DAYCARE,
                        serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                        baseFee = 28900,
                        fee = 28900,
                        feeAlterations = listOf()
                    )
                )
            ),
        )
        insertDecisionsAndPlacements(decisions)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            // 28900 / 20 * 21 is greater than 28900, so it's clamped to a full month
            assertEquals(28900, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
        }
    }

    private fun initByPeriodAndPlacementType(
        period: DateRange,
        placementType: PlacementType,
        children: List<DevPerson> = listOf(testChild_1),
        partner: PersonId? = null
    ) {
        children.forEach { child -> db.transaction(insertChildParentRelation(testAdult_1.id, child.id, period)) }
        val decision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            period,
            testAdult_1.id,
            children.map { child ->
                createFeeDecisionChildFixture(
                    childId = child.id,
                    dateOfBirth = child.dateOfBirth,
                    placementUnitId = testDaycare.id,
                    placementType = placementType,
                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                    baseFee = 28900,
                    fee = 28900
                )
            },
            partnerId = partner
        )
        insertDecisionsAndPlacements(
            listOf(
                decision.copy(validDuring = decision.validDuring.copy(end = period.start.plusDays(7))),
                decision.copy(
                    id = FeeDecisionId(UUID.randomUUID()),
                    validDuring = decision.validDuring.copy(start = period.start.plusDays(8))
                )
            )
        )
    }

    private fun initFreeJulyTestData(
        invoicingPeriod: DateRange,
        placementPeriods: List<DateRange>,
        placementType: PlacementType = PlacementType.DAYCARE
    ) = initFreeJulyTestData(
        invoicingPeriod,
        placementPeriods.map { placementType to it }
    )

    private fun initFreeJulyTestData(
        invoicingPeriod: DateRange,
        placementPeriods: List<Pair<PlacementType, DateRange>>
    ) {
        val decision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            invoicingPeriod,
            testAdult_1.id,
            listOf(
                createFeeDecisionChildFixture(
                    childId = testChild_1.id,
                    dateOfBirth = testChild_1.dateOfBirth,
                    placementUnitId = testDaycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                    baseFee = 28900,
                    fee = 28900
                )
            )
        )
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, invoicingPeriod))
        db.transaction { tx ->
            tx.upsertFeeDecisions(
                listOf(
                    decision.copy(validDuring = decision.validDuring.copy(end = invoicingPeriod.start.plusDays(7)))
                )
            )
        }

        placementPeriods.forEach { (type, period) -> db.transaction(insertPlacement(testChild_1.id, period, type)) }
        db.transaction { generator.createAndStoreAllDraftInvoices(it, invoicingPeriod) }
    }

    private fun initDataForAbsences(
        periods: List<DateRange>,
        absenceDays: Map<LocalDate, AbsenceType>,
        child: DevPerson = testChild_1,
        serviceNeed: ServiceNeedOption = snDaycareFullDay35,
    ) = periods.forEachIndexed { index, period ->
        val decision = createFeeDecisionFixture(
            FeeDecisionStatus.SENT,
            FeeDecisionType.NORMAL,
            period,
            testAdult_1.id,
            listOf(
                createFeeDecisionChildFixture(
                    childId = child.id,
                    dateOfBirth = child.dateOfBirth,
                    placementUnitId = testDaycare.id,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = serviceNeed.toFeeDecisionServiceNeed(),
                    baseFee = 28900,
                    feeAlterations = if (index > 0) {
                        listOf(
                            createFeeDecisionAlterationFixture(
                                amount = -100,
                                isAbsolute = true,
                                effect = -10000
                            )
                        )
                    } else listOf()
                )
            )
        )

        db.transaction(insertChildParentRelation(testAdult_1.id, child.id, period))
        db.transaction { tx -> tx.upsertFeeDecisions(listOf(decision)) }

        val placementId = db.transaction(insertPlacement(child.id, period))
        val groupId = db.transaction { it.insertTestDaycareGroup(DevDaycareGroup(daycareId = testDaycare.id)) }
        db.transaction { tx ->
            tx.insertTestDaycareGroupPlacement(
                daycarePlacementId = placementId,
                groupId = groupId,
                startDate = period.start,
                endDate = period.end!!
            )
        }
        db.transaction { tx ->
            tx.upsertAbsences(
                absenceDays.map { (date, type) ->
                    AbsenceUpsert(
                        absenceType = type,
                        childId = child.id,
                        date = date,
                        category = AbsenceCategory.BILLABLE
                    )
                },
                EvakaUserId(testDecisionMaker_1.id.raw)
            )
        }
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

    private fun insertPlacement(childId: ChildId, period: DateRange, type: PlacementType = PlacementType.DAYCARE) =
        { tx: Database.Transaction ->
            tx.insertTestPlacement(
                childId = childId,
                unitId = testDaycare.id,
                startDate = period.start,
                endDate = period.end!!,
                type = type
            )
        }

    private fun insertChildParentRelation(headOfFamilyId: PersonId, childId: ChildId, period: DateRange) =
        { tx: Database.Transaction ->
            tx.insertTestParentship(
                headOfFamilyId,
                childId,
                startDate = period.start,
                endDate = period.end!!
            )
        }

    private val getAllInvoices: (Database.Read) -> List<Invoice> = { r ->
        r.createQuery(invoiceQueryBase)
            .map(toInvoice)
            .list()
            .let(::flatten)
            .map { it.copy(rows = it.rows.sortedByDescending { row -> row.child.dateOfBirth }) }
    }

    private fun datesBetween(start: LocalDate, endInclusive: LocalDate?): List<LocalDate> {
        return generateSequence(start) { it.plusDays(1) }.takeWhile { it <= endInclusive }.toList()
    }
}
