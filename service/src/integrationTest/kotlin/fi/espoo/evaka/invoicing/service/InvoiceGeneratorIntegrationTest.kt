// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.TestInvoiceProductProvider
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.absence.AbsenceUpsert
import fi.espoo.evaka.absence.insertAbsences
import fi.espoo.evaka.feeThresholds
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.invoicing.createFeeDecisionAlterationFixture
import fi.espoo.evaka.invoicing.createFeeDecisionChildFixture
import fi.espoo.evaka.invoicing.createFeeDecisionFixture
import fi.espoo.evaka.invoicing.data.flatten
import fi.espoo.evaka.invoicing.data.invoiceQueryBase
import fi.espoo.evaka.invoicing.data.toInvoice
import fi.espoo.evaka.invoicing.data.upsertFeeDecisions
import fi.espoo.evaka.invoicing.domain.FeeAlterationType
import fi.espoo.evaka.invoicing.domain.FeeAlterationWithEffect
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.Invoice
import fi.espoo.evaka.invoicing.domain.feeAlterationEffect
import fi.espoo.evaka.invoicing.domain.roundToEuros
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceneed.ServiceNeedOption
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.config.testFeatureConfig
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Row
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevHoliday
import fi.espoo.evaka.shared.dev.DevInvoiceCorrection
import fi.espoo.evaka.shared.dev.DevParentship
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevServiceNeed
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertServiceNeedOption
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.snDaycareContractDays10
import fi.espoo.evaka.snDaycareContractDays15
import fi.espoo.evaka.snDaycareFullDay35
import fi.espoo.evaka.snDefaultPreschoolDaycare
import fi.espoo.evaka.snPreschoolDaycareContractDays13
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testRoundTheClockDaycare
import fi.espoo.evaka.toFeeDecisionServiceNeed
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InvoiceGeneratorIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val productProvider: InvoiceProductProvider = TestInvoiceProductProvider()
    private val featureConfig: FeatureConfig = testFeatureConfig
    private val draftInvoiceGenerator: DraftInvoiceGenerator =
        DraftInvoiceGenerator(productProvider, featureConfig, DefaultInvoiceGenerationLogic)
    private val generator: InvoiceGenerator = InvoiceGenerator(draftInvoiceGenerator, featureConfig)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testDecisionMaker_1)
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testDaycare2)
            tx.insert(testRoundTheClockDaycare)
            listOf(testAdult_1, testAdult_2).forEach { tx.insert(it, DevPersonType.ADULT) }
            listOf(testChild_1, testChild_2).forEach { tx.insert(it, DevPersonType.CHILD) }
            tx.insert(feeThresholds)
            tx.insertServiceNeedOptions()
            tx.execute {
                sql(
                    """
                    INSERT INTO holiday (date)
                    VALUES
                        (${bind(LocalDate.of(2019, 1, 1))}),
                        (${bind(LocalDate.of(2019, 1, 6))}),
                        (${bind(LocalDate.of(2020, 1, 1))}),
                        (${bind(LocalDate.of(2020, 1, 6))}),
                        (${bind(LocalDate.of(2021, 1, 1))}),
                        (${bind(LocalDate.of(2021, 1, 6))}),
                        (${bind(LocalDate.of(2021, 12, 6))}),
                        (${bind(LocalDate.of(2021, 12, 24))})
                    """
                )
            }
        }
    }

    @Test
    fun `invoice generation for child with a day long temporary placement`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val placementPeriod = FiniteDateRange(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 2))
        db.transaction(
            insertPlacement(testChild_1.id, placementPeriod, PlacementType.TEMPORARY_DAYCARE)
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(testDaycare.areaId, invoice.areaId)
            assertEquals(2900, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(testDaycare.id, invoiceRow.unitId)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.TEMPORARY_DAYCARE),
                    invoiceRow.product,
                )
                assertEquals(1, invoiceRow.amount)
                assertEquals(2900, invoiceRow.unitPrice)
                assertEquals(2900, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation for child with one day long part day temporary placement`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val placementPeriod = FiniteDateRange(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 2))
        db.transaction(
            insertPlacement(
                testChild_1.id,
                placementPeriod,
                PlacementType.TEMPORARY_DAYCARE_PART_DAY,
            )
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(1500, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.TEMPORARY_DAYCARE_PART_DAY),
                    invoiceRow.product,
                )
                assertEquals(1, invoiceRow.amount)
                assertEquals(1500, invoiceRow.unitPrice)
                assertEquals(1500, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation for child with a three day long temporary placement`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val placementPeriod = FiniteDateRange(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 4))
        db.transaction(
            insertPlacement(testChild_1.id, placementPeriod, PlacementType.TEMPORARY_DAYCARE)
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(8700, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.TEMPORARY_DAYCARE),
                    invoiceRow.product,
                )
                assertEquals(3, invoiceRow.amount)
                assertEquals(2900, invoiceRow.unitPrice)
                assertEquals(8700, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation for child with a two day long temporary placement and a day long part day temporary placement`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val placementPeriod = FiniteDateRange(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 4))
        db.transaction(
            insertPlacement(
                testChild_1.id,
                placementPeriod.copy(end = placementPeriod.end.minusDays(1)),
                PlacementType.TEMPORARY_DAYCARE,
            )
        )
        db.transaction(
            insertPlacement(
                testChild_1.id,
                placementPeriod.copy(start = placementPeriod.end),
                PlacementType.TEMPORARY_DAYCARE_PART_DAY,
            )
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(7300, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.TEMPORARY_DAYCARE),
                    invoiceRow.product,
                )
                assertEquals(2, invoiceRow.amount)
                assertEquals(2900, invoiceRow.unitPrice)
                assertEquals(5800, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.TEMPORARY_DAYCARE_PART_DAY),
                    invoiceRow.product,
                )
                assertEquals(1, invoiceRow.amount)
                assertEquals(1500, invoiceRow.unitPrice)
                assertEquals(1500, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation for two children with temporary placements at the same time`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val placementPeriod = FiniteDateRange(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 2))
        db.transaction(
            insertPlacement(testChild_1.id, placementPeriod, PlacementType.TEMPORARY_DAYCARE)
        )

        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_2.id, period))
        db.transaction(
            insertPlacement(testChild_2.id, placementPeriod, PlacementType.TEMPORARY_DAYCARE)
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(4400, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.TEMPORARY_DAYCARE),
                    invoiceRow.product,
                )
                assertEquals(1, invoiceRow.amount)
                assertEquals(2900, invoiceRow.unitPrice)
                assertEquals(2900, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_2.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.TEMPORARY_DAYCARE),
                    invoiceRow.product,
                )
                assertEquals(1, invoiceRow.amount)
                assertEquals(1500, invoiceRow.unitPrice)
                assertEquals(1500, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation for two children with part day temporary placements at the same time`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val placementPeriod = FiniteDateRange(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 2))
        db.transaction(
            insertPlacement(
                testChild_1.id,
                placementPeriod,
                PlacementType.TEMPORARY_DAYCARE_PART_DAY,
            )
        )

        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_2.id, period))
        db.transaction(
            insertPlacement(
                testChild_2.id,
                placementPeriod,
                PlacementType.TEMPORARY_DAYCARE_PART_DAY,
            )
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(2300, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.TEMPORARY_DAYCARE_PART_DAY),
                    invoiceRow.product,
                )
                assertEquals(1, invoiceRow.amount)
                assertEquals(1500, invoiceRow.unitPrice)
                assertEquals(1500, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_2.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.TEMPORARY_DAYCARE_PART_DAY),
                    invoiceRow.product,
                )
                assertEquals(1, invoiceRow.amount)
                assertEquals(800, invoiceRow.unitPrice)
                assertEquals(800, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation for child with a two day long temporary placement that changes head of family during placement`() {
        val month = YearMonth.of(2019, 1)
        val placementPeriod = FiniteDateRange(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 3))
        db.transaction(
            insertPlacement(testChild_1.id, placementPeriod, PlacementType.TEMPORARY_DAYCARE)
        )

        db.transaction(
            insertChildParentRelation(
                testAdult_1.id,
                testChild_1.id,
                placementPeriod.copy(end = placementPeriod.start),
            )
        )
        db.transaction(
            insertChildParentRelation(
                testAdult_2.id,
                testChild_1.id,
                placementPeriod.copy(start = placementPeriod.end),
            )
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices).sortedBy { it.headOfFamily == testAdult_2.id }

        assertEquals(2, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(2900, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.TEMPORARY_DAYCARE),
                    invoiceRow.product,
                )
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
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.TEMPORARY_DAYCARE),
                    invoiceRow.product,
                )
                assertEquals(1, invoiceRow.amount)
                assertEquals(2900, invoiceRow.unitPrice)
                assertEquals(2900, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation for child with a day long temporary placement that has no family configured`() {
        val month = YearMonth.of(2019, 1)
        val placementPeriod = FiniteDateRange(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 2))
        db.transaction(
            insertPlacement(testChild_1.id, placementPeriod, PlacementType.TEMPORARY_DAYCARE)
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)

        assertEquals(0, result.size)
    }

    @Test
    fun `invoice generation for temporary placements does not pick non-temporary placements`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val temporaryPeriod = FiniteDateRange(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 3))
        db.transaction(
            insertPlacement(testChild_1.id, temporaryPeriod, PlacementType.TEMPORARY_DAYCARE)
        )
        val nonTemporaryPeriod = FiniteDateRange(LocalDate.of(2019, 1, 4), LocalDate.of(2019, 1, 5))
        db.transaction(insertPlacement(testChild_1.id, nonTemporaryPeriod))

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(5800, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.TEMPORARY_DAYCARE),
                    invoiceRow.product,
                )
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
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision =
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.asDateRange(),
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
                    )
                ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(
            listOf(
                decision.copy(
                    validDuring = decision.validDuring.copy(end = period.start.plusDays(7))
                ),
                decision.copy(
                    id = FeeDecisionId(UUID.randomUUID()),
                    validDuring = decision.validDuring.copy(start = period.start.plusDays(8)),
                ),
            )
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(28900, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.DAYCARE),
                    invoiceRow.product,
                )
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation from two fee decision with same price and same fee alterations results in one invoice row`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision =
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.asDateRange(),
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
                        feeAlterations =
                            listOf(
                                createFeeDecisionAlterationFixture(
                                    type = FeeAlterationType.DISCOUNT,
                                    amount = 20,
                                    isAbsolute = false,
                                    effect = -5780,
                                )
                            ),
                    )
                ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(
            listOf(
                decision.copy(
                    validDuring = decision.validDuring.copy(end = period.start.plusDays(7))
                ),
                decision.copy(
                    id = FeeDecisionId(UUID.randomUUID()),
                    validDuring = decision.validDuring.copy(start = period.start.plusDays(8)),
                ),
            )
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(23120, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.DAYCARE),
                    invoiceRow.product,
                )
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToFeeAlterationProduct(
                        productProvider.mapToProduct(PlacementType.DAYCARE),
                        FeeAlterationType.DISCOUNT,
                    ),
                    invoiceRow.product,
                )
                assertEquals(1, invoiceRow.amount)
                assertEquals(-5780, invoiceRow.unitPrice)
                assertEquals(-5780, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation from two fee decision with the second one having another child results in one invoice row for the first child`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_2.id, period))
        val decision =
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.asDateRange(),
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
                    )
                ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(
            listOf(
                decision.copy(
                    validDuring = decision.validDuring.copy(end = period.start.plusDays(7))
                ),
                decision.copy(
                    id = FeeDecisionId(UUID.randomUUID()),
                    validDuring = decision.validDuring.copy(start = period.start.plusDays(8)),
                    familySize = decision.familySize + 1,
                    children =
                        decision.children +
                            createFeeDecisionChildFixture(
                                childId = testChild_2.id,
                                dateOfBirth = testChild_2.dateOfBirth,
                                placementUnitId = testDaycare.id,
                                placementType = PlacementType.DAYCARE,
                                serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                                baseFee = 28900,
                                siblingDiscount = 50,
                                fee = 14500,
                            ),
                ),
            )
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(40103, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.DAYCARE),
                    invoiceRow.product,
                )
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_2.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.DAYCARE),
                    invoiceRow.product,
                )
                assertEquals(17, invoiceRow.amount)
                assertEquals(659, invoiceRow.unitPrice)
                assertEquals(11203, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation from two fee decisions makes sure the sum is at most the monthly fee`() {
        // January 2019 has 22 operational days which results in daily price being rounded up
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decisions =
            listOf(
                createFeeDecisionFixture(
                    FeeDecisionStatus.SENT,
                    FeeDecisionType.NORMAL,
                    period.copy(end = period.start.plusDays(6)).asDateRange(),
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
                        )
                    ),
                ),
                createFeeDecisionFixture(
                    FeeDecisionStatus.SENT,
                    FeeDecisionType.NORMAL,
                    period.copy(start = period.start.plusDays(7)).asDateRange(),
                    testAdult_1.id,
                    listOf(
                        createFeeDecisionChildFixture(
                            childId = testChild_1.id,
                            dateOfBirth = testChild_1.dateOfBirth,
                            placementUnitId = testDaycare2.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                            baseFee = 28900,
                            fee = 28900,
                        )
                    ),
                ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(decisions)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(28900, invoice.totalPrice)
            assertEquals(3, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.DAYCARE),
                    invoiceRow.product,
                )
                assertEquals(4, invoiceRow.amount)
                assertEquals(1314, invoiceRow.unitPrice)
                assertEquals(5256, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.DAYCARE),
                    invoiceRow.product,
                )
                assertEquals(18, invoiceRow.amount)
                assertEquals(1314, invoiceRow.unitPrice)
                assertEquals(23652, invoiceRow.price)
            }
            invoice.rows[2].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.DAYCARE),
                    invoiceRow.product,
                )
                assertEquals(1, invoiceRow.amount)
                assertEquals(-8, invoiceRow.unitPrice)
                assertEquals(-8, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation from two fee decisions makes sure the sum is at least the monthly fee`() {
        // March 2019 has 21 operational days which results in daily price being rounded down
        val month = YearMonth.of(2019, 3)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decisions =
            listOf(
                createFeeDecisionFixture(
                    FeeDecisionStatus.SENT,
                    FeeDecisionType.NORMAL,
                    period.copy(end = period.start.plusDays(6)).asDateRange(),
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
                        )
                    ),
                ),
                createFeeDecisionFixture(
                    FeeDecisionStatus.SENT,
                    FeeDecisionType.NORMAL,
                    period.copy(start = period.start.plusDays(7)).asDateRange(),
                    testAdult_1.id,
                    listOf(
                        createFeeDecisionChildFixture(
                            childId = testChild_1.id,
                            dateOfBirth = testChild_1.dateOfBirth,
                            placementUnitId = testDaycare2.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                            baseFee = 28900,
                            fee = 28900,
                        )
                    ),
                ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(decisions)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(28900, invoice.totalPrice)
            assertEquals(3, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.DAYCARE),
                    invoiceRow.product,
                )
                assertEquals(5, invoiceRow.amount)
                assertEquals(1376, invoiceRow.unitPrice)
                assertEquals(6880, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.DAYCARE),
                    invoiceRow.product,
                )
                assertEquals(16, invoiceRow.amount)
                assertEquals(1376, invoiceRow.unitPrice)
                assertEquals(22016, invoiceRow.price)
            }
            invoice.rows[2].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.DAYCARE),
                    invoiceRow.product,
                )
                assertEquals(1, invoiceRow.amount)
                assertEquals(4, invoiceRow.unitPrice)
                assertEquals(4, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation from two fee decisions with cleanly split daily prices does not result in a rounding row`() {
        // February 2019 has 20 operational days which results in daily price being split evenly
        val month = YearMonth.of(2019, 2)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decisions =
            listOf(
                createFeeDecisionFixture(
                    FeeDecisionStatus.SENT,
                    FeeDecisionType.NORMAL,
                    period.copy(end = period.start.plusDays(6)).asDateRange(),
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
                        )
                    ),
                ),
                createFeeDecisionFixture(
                    FeeDecisionStatus.SENT,
                    FeeDecisionType.NORMAL,
                    period.copy(start = period.start.plusDays(7)).asDateRange(),
                    testAdult_1.id,
                    listOf(
                        createFeeDecisionChildFixture(
                            childId = testChild_1.id,
                            dateOfBirth = testChild_1.dateOfBirth,
                            placementUnitId = testDaycare2.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                            baseFee = 28900,
                            fee = 28900,
                        )
                    ),
                ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(decisions)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(28900, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.DAYCARE),
                    invoiceRow.product,
                )
                assertEquals(5, invoiceRow.amount)
                assertEquals(1445, invoiceRow.unitPrice)
                assertEquals(7225, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.DAYCARE),
                    invoiceRow.product,
                )
                assertEquals(15, invoiceRow.amount)
                assertEquals(1445, invoiceRow.unitPrice)
                assertEquals(21675, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation from two fee decision with the second one having changed fee for second child results in one invoice row for the first child`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_2.id, period))
        val decision =
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.asDateRange(),
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
                    ),
                    createFeeDecisionChildFixture(
                        childId = testChild_2.id,
                        dateOfBirth = testChild_2.dateOfBirth,
                        placementUnitId = testDaycare.id,
                        placementType = PlacementType.DAYCARE,
                        serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                        baseFee = 28900,
                        siblingDiscount = 50,
                        fee = 14500,
                    ),
                ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(
            listOf(
                decision.copy(
                    validDuring = decision.validDuring.copy(end = period.start.plusDays(7))
                ),
                decision.copy(
                    id = FeeDecisionId(UUID.randomUUID()),
                    validDuring = decision.validDuring.copy(start = period.start.plusDays(8)),
                    children =
                        listOf(
                            decision.children[0],
                            decision.children[1].copy(fee = 10000, finalFee = 10000),
                        ),
                ),
            )
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(39930, invoice.totalPrice)
            assertEquals(3, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.DAYCARE),
                    invoiceRow.product,
                )
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(testChild_2.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.DAYCARE),
                    invoiceRow.product,
                )
                assertEquals(5, invoiceRow.amount)
                assertEquals(659, invoiceRow.unitPrice)
                assertEquals(3295, invoiceRow.price)
            }
            invoice.rows[2].let { invoiceRow ->
                assertEquals(testChild_2.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.DAYCARE),
                    invoiceRow.product,
                )
                assertEquals(17, invoiceRow.amount)
                assertEquals(455, invoiceRow.unitPrice)
                assertEquals(7735, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation from fee decision with one fee alteration creates additional invoice row`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision =
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.asDateRange(),
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
                        feeAlterations =
                            listOf(
                                createFeeDecisionAlterationFixture(
                                    type = FeeAlterationType.DISCOUNT,
                                    amount = 20,
                                    isAbsolute = false,
                                    effect = -5780,
                                )
                            ),
                    )
                ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(listOf(decision))

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(23120, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.DAYCARE),
                    invoiceRow.product,
                )
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToFeeAlterationProduct(
                        productProvider.mapToProduct(PlacementType.DAYCARE),
                        FeeAlterationType.DISCOUNT,
                    ),
                    invoiceRow.product,
                )
                assertEquals(1, invoiceRow.amount)
                assertEquals(-5780, invoiceRow.unitPrice)
                assertEquals(-5780, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation from fee decision with multiple fee alterations creates additional invoice rows`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision =
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.asDateRange(),
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
                        feeAlterations =
                            listOf(
                                createFeeDecisionAlterationFixture(
                                    type = FeeAlterationType.DISCOUNT,
                                    amount = 20,
                                    isAbsolute = false,
                                    effect = -5780,
                                ),
                                createFeeDecisionAlterationFixture(
                                    type = FeeAlterationType.INCREASE,
                                    amount = 93,
                                    isAbsolute = false,
                                    effect = 9300,
                                ),
                            ),
                    )
                ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(listOf(decision))

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(32420, invoice.totalPrice)
            assertEquals(3, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.DAYCARE),
                    invoiceRow.product,
                )
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToFeeAlterationProduct(
                        productProvider.mapToProduct(PlacementType.DAYCARE),
                        FeeAlterationType.DISCOUNT,
                    ),
                    invoiceRow.product,
                )
                assertEquals(1, invoiceRow.amount)
                assertEquals(-5780, invoiceRow.unitPrice)
                assertEquals(-5780, invoiceRow.price)
            }

            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToFeeAlterationProduct(
                        productProvider.mapToProduct(PlacementType.DAYCARE),
                        FeeAlterationType.INCREASE,
                    ),
                    invoiceRow.product,
                )
                assertEquals(1, invoiceRow.amount)
                assertEquals(9300, invoiceRow.unitPrice)
                assertEquals(9300, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation from fee decision with a 95 percent discount fee alteration`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision =
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.asDateRange(),
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
                        feeAlterations =
                            listOf(
                                createFeeDecisionAlterationFixture(
                                    type = FeeAlterationType.DISCOUNT,
                                    amount = 95,
                                    isAbsolute = false,
                                    effect = -27455,
                                )
                            ),
                    )
                ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(listOf(decision))

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(1445, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.DAYCARE),
                    invoiceRow.product,
                )
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToFeeAlterationProduct(
                        productProvider.mapToProduct(PlacementType.DAYCARE),
                        FeeAlterationType.DISCOUNT,
                    ),
                    invoiceRow.product,
                )
                assertEquals(1, invoiceRow.amount)
                assertEquals(-27455, invoiceRow.unitPrice)
                assertEquals(-27455, invoiceRow.price)
            }
        }
    }

    @Test
    fun `when two people have active fee decisions for the same child both are invoiced`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision =
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.asDateRange(),
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
                        feeAlterations = listOf(),
                    )
                ),
            )
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = period.start,
                    endDate = period.end,
                )
            )
            tx.upsertFeeDecisions(
                listOf(
                    decision,
                    decision.copy(
                        id = FeeDecisionId(UUID.randomUUID()),
                        headOfFamilyId = testAdult_2.id,
                    ),
                )
            )
        }

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)

        assertEquals(2, result.size)
        result
            .find { it.headOfFamily == testAdult_1.id }
            .let { invoice ->
                assertEquals(28900, invoice!!.totalPrice)
                assertEquals(1, invoice.rows.size)
                invoice.rows.first().let { invoiceRow ->
                    assertEquals(testChild_1.id, invoiceRow.child)
                    assertEquals(
                        productProvider.mapToProduct(PlacementType.DAYCARE),
                        invoiceRow.product,
                    )
                    assertEquals(1, invoiceRow.amount)
                    assertEquals(28900, invoiceRow.unitPrice)
                    assertEquals(28900, invoiceRow.price)
                }
            }
        result
            .find { it.headOfFamily == testAdult_2.id }
            .let { invoice ->
                assertEquals(28900, invoice!!.totalPrice)
                assertEquals(1, invoice.rows.size)
                invoice.rows.first().let { invoiceRow ->
                    assertEquals(testChild_1.id, invoiceRow.child)
                    assertEquals(
                        productProvider.mapToProduct(PlacementType.DAYCARE),
                        invoiceRow.product,
                    )
                    assertEquals(1, invoiceRow.amount)
                    assertEquals(28900, invoiceRow.unitPrice)
                    assertEquals(28900, invoiceRow.price)
                }
            }
    }

    @Test
    fun `when a placement ends before the fee decision only the placement period is invoiced`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        val placementPeriod = period.copy(end = period.start.plusDays(7))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision =
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.asDateRange(),
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
                        feeAlterations = listOf(),
                    )
                ),
            )
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = placementPeriod.start,
                    endDate = placementPeriod.end,
                )
            )
            tx.upsertFeeDecisions(
                listOf(
                    decision.copy(
                        validDuring = period.copy(end = period.start.plusDays(14)).asDateRange()
                    ),
                    decision.copy(
                        id = FeeDecisionId(UUID.randomUUID()),
                        headOfFamilyId = testAdult_2.id,
                        validDuring = period.copy(start = period.start.plusDays(15)).asDateRange(),
                    ),
                )
            )
        }

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(6570, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(placementPeriod.start, invoiceRow.periodStart)
                assertEquals(placementPeriod.end, invoiceRow.periodEnd)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.DAYCARE),
                    invoiceRow.product,
                )
                assertEquals(5, invoiceRow.amount)
                assertEquals(1314, invoiceRow.unitPrice)
                assertEquals(6570, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with sick leave absences covering period`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        val absenceDays = datesBetween(period.start, period.end).map { it to AbsenceType.SICKLEAVE }

        initDataForAbsences(listOf(period), absenceDays)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

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
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        val absenceDays =
            datesBetween(period.start, period.end)
                .filter {
                    listOf(
                            DayOfWeek.TUESDAY,
                            DayOfWeek.WEDNESDAY,
                            DayOfWeek.THURSDAY,
                            DayOfWeek.FRIDAY,
                        )
                        .contains(it.dayOfWeek)
                }
                .map { it to AbsenceType.SICKLEAVE }

        initDataForAbsences(listOf(period), absenceDays)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

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
    fun `50 percent discount for sick leaves is applied after reducing force majeure absences`() {
        // 22 operational days
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        // 1 force majeure absence
        val forceMajeure = listOf(LocalDate.of(2019, 1, 16) to AbsenceType.FORCE_MAJEURE)

        // 11 sick leaves
        val sickLeave =
            datesBetween(LocalDate.of(2019, 1, 17), LocalDate.of(2019, 1, 31)).map {
                it to AbsenceType.SICKLEAVE
            }

        initDataForAbsences(listOf(period), forceMajeure + sickLeave)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(13793, invoice.totalPrice)
            assertEquals(3, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(productProvider.dailyRefund, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-1314, invoiceRow.unitPrice) // 28900 / 22
                assertEquals(-1314, invoiceRow.price)
            }
            // Total price minus one day refund: 28900 - 1314 = 27585
            invoice.rows[2].let { invoiceRow ->
                assertEquals(productProvider.partMonthSickLeave, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-13793, invoiceRow.unitPrice) // 27585 / 2
                assertEquals(-13793, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with some unknown absences`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        val absenceDays =
            datesBetween(period.start, period.end)
                .filter {
                    listOf(
                            DayOfWeek.TUESDAY,
                            DayOfWeek.WEDNESDAY,
                            DayOfWeek.THURSDAY,
                            DayOfWeek.FRIDAY,
                        )
                        .contains(it.dayOfWeek)
                }
                .map { it to AbsenceType.UNKNOWN_ABSENCE }

        initDataForAbsences(listOf(period), absenceDays)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

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
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        val absenceDays =
            datesBetween(period.start, period.end).map { it to AbsenceType.UNKNOWN_ABSENCE }

        initDataForAbsences(listOf(period), absenceDays)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

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
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        val absenceDays =
            datesBetween(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 4)).map {
                it to AbsenceType.PARENTLEAVE
            }

        initDataForAbsences(listOf(period), absenceDays)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

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
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        val absenceDays =
            datesBetween(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 4))
                .map { it to AbsenceType.PARENTLEAVE }
                .plus(
                    datesBetween(LocalDate.of(2019, 1, 17), LocalDate.of(2019, 1, 31)).map {
                        it to AbsenceType.SICKLEAVE
                    }
                )

        initDataForAbsences(listOf(period), absenceDays)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

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
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        val absenceDays =
            datesBetween(period.start, period.end.minusDays(1)).map {
                it to AbsenceType.PLANNED_ABSENCE
            }

        initDataForAbsences(listOf(period), absenceDays)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

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
    fun `planned absences for the whole month generate a discount`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        val absenceDays =
            datesBetween(period.start, period.end).map { it to AbsenceType.PLANNED_ABSENCE }

        initDataForAbsences(listOf(period), absenceDays)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

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
                assertEquals(invoiceRow.product, productProvider.fullMonthAbsence)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-14450, invoiceRow.unitPrice)
                assertEquals(-14450, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with some parentleave absences for a too old child`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        val absenceDays =
            datesBetween(period.start, LocalDate.of(2019, 1, 4)).map {
                it to AbsenceType.PARENTLEAVE
            }

        initDataForAbsences(listOf(period), absenceDays, child = testChild_2)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

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
        val periods =
            listOf(
                FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 11)),
                FiniteDateRange(LocalDate.of(2019, 1, 12), LocalDate.of(2019, 1, 31)),
            )

        val absenceDays =
            datesBetween(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
                .filter {
                    listOf(
                            DayOfWeek.TUESDAY,
                            DayOfWeek.WEDNESDAY,
                            DayOfWeek.THURSDAY,
                            DayOfWeek.FRIDAY,
                        )
                        .contains(it.dayOfWeek)
                }
                .map { it to AbsenceType.SICKLEAVE }

        initDataForAbsences(periods, absenceDays)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, YearMonth.of(2019, 1)) }

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
                        FeeAlterationType.DISCOUNT,
                    ),
                    invoiceRow.product,
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
    fun `invoice generation with 2 decisions plus parent leave and force majeure absences`() {
        val periods =
            listOf(
                FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 11)),
                FiniteDateRange(LocalDate.of(2019, 1, 12), LocalDate.of(2019, 1, 31)),
            )

        val absenceDays =
            listOf(
                LocalDate.of(2019, 1, 7) to AbsenceType.PARENTLEAVE,
                LocalDate.of(2019, 1, 14) to AbsenceType.PARENTLEAVE,
                LocalDate.of(2019, 1, 21) to AbsenceType.FORCE_MAJEURE,
                LocalDate.of(2019, 1, 28) to AbsenceType.FORCE_MAJEURE,
            )
        initDataForAbsences(periods, absenceDays)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, YearMonth.of(2019, 1)) }

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
                        FeeAlterationType.DISCOUNT,
                    ),
                    invoiceRow.product,
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
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        // 15 operational days first
        // then planned absences
        val plannedAbsenceDays =
            datesBetween(LocalDate.of(2019, 1, 23), LocalDate.of(2019, 1, 29)).map {
                it to AbsenceType.PLANNED_ABSENCE
            }
        // then 2 more operational days

        initDataForAbsences(
            listOf(period),
            plannedAbsenceDays,
            serviceNeed = snDaycareContractDays15,
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(24594, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(21700, invoiceRow.unitPrice)
                assertEquals(21700, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(productProvider.contractSurplusDay, invoiceRow.product)
                assertEquals(2, invoiceRow.amount)
                assertEquals(1447, invoiceRow.unitPrice) // 21700 / 15
                assertEquals(2894, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days, 2 surplus days and one refunded day`() {
        // 22 operational days
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        // 15 operational days first
        // then planned absences
        val plannedAbsenceDays =
            datesBetween(LocalDate.of(2019, 1, 23), LocalDate.of(2019, 1, 29)).map {
                it to AbsenceType.PLANNED_ABSENCE
            }
        // then 2 more operational days
        // and one refunded day
        val refundedDay =
            datesBetween(LocalDate.of(2019, 1, 31), LocalDate.of(2019, 1, 31)).map {
                it to AbsenceType.FORCE_MAJEURE
            }

        initDataForAbsences(
            listOf(period),
            plannedAbsenceDays + refundedDay,
            serviceNeed = snDaycareContractDays15,
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(23147, invoice.totalPrice)
            assertEquals(3, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(21700, invoiceRow.unitPrice)
                assertEquals(21700, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(productProvider.contractSurplusDay, invoiceRow.product)
                assertEquals(2, invoiceRow.amount)
                assertEquals(1447, invoiceRow.unitPrice) // 21700 / 15
                assertEquals(2894, invoiceRow.price)
            }
            invoice.rows[2].let { invoiceRow ->
                assertEquals(productProvider.dailyRefund, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-1447, invoiceRow.unitPrice) // 21700 / 15
                assertEquals(-1447, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days, 2 surplus days and a fee alteration`() {
        // 22 operational days
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))

        val decision =
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.asDateRange(),
                testAdult_1.id,
                listOf(
                    createFeeDecisionChildFixture(
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        placementUnitId = testDaycare.id,
                        placementType = PlacementType.DAYCARE,
                        serviceNeed = snDaycareContractDays15.toFeeDecisionServiceNeed(),
                        baseFee = 28900,
                        fee = 21700,
                        feeAlterations =
                            listOf(
                                createFeeDecisionAlterationFixture(
                                    amount = -50,
                                    isAbsolute = false,
                                    effect = -10850,
                                )
                            ),
                    )
                ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(listOf(decision))

        // 15 operational days first
        // then planned absences
        val plannedAbsenceDays =
            datesBetween(LocalDate.of(2019, 1, 23), LocalDate.of(2019, 1, 29)).map {
                it to AbsenceType.PLANNED_ABSENCE
            }
        // then 2 more operational days
        insertAbsences(testChild_1.id, plannedAbsenceDays)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(12296, invoice.totalPrice)
            assertEquals(3, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(21700, invoiceRow.unitPrice)
                assertEquals(21700, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(
                    productProvider.mapToFeeAlterationProduct(
                        productProvider.mapToProduct(PlacementType.DAYCARE),
                        FeeAlterationType.DISCOUNT,
                    ),
                    invoiceRow.product,
                )
                assertEquals(1, invoiceRow.amount)
                assertEquals(-10850, invoiceRow.unitPrice)
                assertEquals(-10850, invoiceRow.price)
            }
            invoice.rows[2].let { invoiceRow ->
                assertEquals(productProvider.contractSurplusDay, invoiceRow.product)
                assertEquals(2, invoiceRow.amount)
                assertEquals(723, invoiceRow.unitPrice) // 10850 / 15
                assertEquals(1446, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days, 1 absence and 2 surplus days`() {
        // 22 operational days
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        // 14 operational days first
        // then 1 other absence
        val otherAbsenceDays = listOf(LocalDate.of(2019, 1, 22) to AbsenceType.OTHER_ABSENCE)
        // then planned absences
        val plannedAbsenceDays =
            datesBetween(LocalDate.of(2019, 1, 23), LocalDate.of(2019, 1, 29)).map {
                it to AbsenceType.PLANNED_ABSENCE
            }
        // then 2 more operational days

        initDataForAbsences(
            listOf(period),
            otherAbsenceDays + plannedAbsenceDays,
            serviceNeed = snDaycareContractDays15,
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(24594, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(21700, invoiceRow.unitPrice)
                assertEquals(21700, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(productProvider.contractSurplusDay, invoiceRow.product)
                assertEquals(2, invoiceRow.amount)
                assertEquals(1447, invoiceRow.unitPrice) // 28900 / 15
                assertEquals(2894, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days and 1 absence with unplanned absences not counting as surplus days`() {
        // 22 operational days
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        // 14 operational days first
        // then 1 other absence
        val otherAbsenceDays = listOf(LocalDate.of(2019, 1, 22) to AbsenceType.OTHER_ABSENCE)
        // then planned absences
        val plannedAbsenceDays =
            datesBetween(LocalDate.of(2019, 1, 23), LocalDate.of(2019, 1, 31)).map {
                it to AbsenceType.PLANNED_ABSENCE
            }

        initDataForAbsences(
            listOf(period),
            otherAbsenceDays + plannedAbsenceDays,
            serviceNeed = snDaycareContractDays15,
        )

        // Override to not count unplanned absences as contract surplus days
        val featureConfig = featureConfig.copy(unplannedAbsencesAreContractSurplusDays = false)
        val generator =
            InvoiceGenerator(
                DraftInvoiceGenerator(
                    productProvider,
                    featureConfig,
                    DefaultInvoiceGenerationLogic,
                ),
                featureConfig,
            )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(21700, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(21700, invoiceRow.unitPrice)
                assertEquals(21700, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days, 2 fee decisions and 2 surplus days`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))

        val decisions =
            listOf(
                    FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 15)) to 15000,
                    FiniteDateRange(LocalDate.of(2019, 1, 16), LocalDate.of(2019, 1, 31)) to 9000,
                )
                .map { (range, fee) ->
                    createFeeDecisionFixture(
                        FeeDecisionStatus.SENT,
                        FeeDecisionType.NORMAL,
                        range.asDateRange(),
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
                                feeAlterations = listOf(),
                            )
                        ),
                    )
                }
        insertDecisionsAndPlacementsAndServiceNeeds(decisions)

        // 17 operational days in total, the rest are planned absences
        insertAbsences(
            testChild_1.id,
            (datesBetween(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 4)) +
                    datesBetween(LocalDate.of(2019, 1, 30), LocalDate.of(2019, 1, 31)))
                .map { it to AbsenceType.PLANNED_ABSENCE },
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

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
                assertEquals(
                    productProvider.mapToProduct(PlacementType.DAYCARE),
                    invoiceRow.product,
                )
                assertEquals(8, invoiceRow.amount)
                assertEquals(600, invoiceRow.unitPrice) // 9000 / 15
                assertEquals(4800, invoiceRow.price)
            }
            invoice.rows[2].let { invoiceRow ->
                assertEquals(productProvider.contractSurplusDay, invoiceRow.product)
                assertEquals(2, invoiceRow.amount)
                assertEquals(600, invoiceRow.unitPrice) // 9000 / 15
                assertEquals(1200, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days and 15 days of absences - half price`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        val otherAbsenceDays =
            datesBetween(period.start, LocalDate.of(2019, 1, 22)) // 15 operational days
                .map { it to AbsenceType.OTHER_ABSENCE }
        val plannedAbsenceDays =
            datesBetween(LocalDate.of(2019, 1, 23), LocalDate.of(2019, 1, 31)).map {
                it to AbsenceType.PLANNED_ABSENCE
            }

        initDataForAbsences(
            listOf(period),
            otherAbsenceDays + plannedAbsenceDays,
            serviceNeed = snDaycareContractDays15,
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(10850, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(21700, invoiceRow.unitPrice)
                assertEquals(21700, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(productProvider.fullMonthAbsence, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-10850, invoiceRow.unitPrice)
                assertEquals(-10850, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days and 14 days of absences - full price`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        val otherAbsenceDays =
            datesBetween(period.start, LocalDate.of(2019, 1, 21)) // 14 operational days
                .map { it to AbsenceType.OTHER_ABSENCE }
        // No absence for 2019-01-22
        val plannedAbsenceDays =
            datesBetween(LocalDate.of(2019, 1, 23), LocalDate.of(2019, 1, 31)).map {
                it to AbsenceType.PLANNED_ABSENCE
            }

        initDataForAbsences(
            listOf(period),
            otherAbsenceDays + plannedAbsenceDays,
            serviceNeed = snDaycareContractDays15,
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(21700, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(21700, invoiceRow.unitPrice)
                assertEquals(21700, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days and 1 force majeure absence`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        // 1 force majeure absence
        val forceMajeureAbsenceDays = listOf(LocalDate.of(2019, 1, 2) to AbsenceType.FORCE_MAJEURE)
        // No absence for 14 days
        // Planned absences for the rest of the month
        val plannedAbsenceDays =
            datesBetween(LocalDate.of(2019, 1, 23), LocalDate.of(2019, 1, 31)).map {
                it to AbsenceType.PLANNED_ABSENCE
            }

        initDataForAbsences(
            listOf(period),
            forceMajeureAbsenceDays + plannedAbsenceDays,
            serviceNeed = snDaycareContractDays15,
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(20253, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(21700, invoiceRow.unitPrice)
                assertEquals(21700, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(productProvider.dailyRefund, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-1447, invoiceRow.unitPrice) // 21700 / 15
                assertEquals(-1447, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with free and paid fee decisions with absences for all paid days`() {
        // 21 operational days
        val month = YearMonth.of(2021, 12)
        val period = FiniteDateRange.ofMonth(month)

        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        db.transaction(insertPlacement(testChild_1.id, period))

        val decisions =
            listOf(
                    FiniteDateRange(LocalDate.of(2021, 12, 1), LocalDate.of(2021, 12, 22)) to 0,
                    FiniteDateRange(LocalDate.of(2021, 12, 23), LocalDate.of(2021, 12, 31)) to 28900,
                )
                .map { (valid, fee) ->
                    createFeeDecisionFixture(
                        FeeDecisionStatus.SENT,
                        FeeDecisionType.NORMAL,
                        valid.asDateRange(),
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
                                feeAlterations = listOf(),
                            )
                        ),
                    )
                }
        db.transaction { tx -> tx.upsertFeeDecisions(decisions) }

        insertAbsences(
            testChild_1.id,
            listOf(
                LocalDate.of(2021, 12, 23) to AbsenceType.OTHER_ABSENCE,
                LocalDate.of(2021, 12, 27) to AbsenceType.OTHER_ABSENCE,
                LocalDate.of(2021, 12, 28) to AbsenceType.OTHER_ABSENCE,
                LocalDate.of(2021, 12, 29) to AbsenceType.OTHER_ABSENCE,
                LocalDate.of(2021, 12, 30) to AbsenceType.OTHER_ABSENCE,
                LocalDate.of(2021, 12, 31) to AbsenceType.OTHER_ABSENCE,
            ),
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

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
        val month = YearMonth.of(2019, 12)
        val period = FiniteDateRange.ofMonth(month)

        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        db.transaction(insertPlacement(testChild_1.id, period))

        val decision =
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.asDateRange(),
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
                        feeAlterations =
                            listOf(
                                FeeAlterationWithEffect(
                                    type = FeeAlterationType.RELIEF,
                                    amount = 100,
                                    isAbsolute = false,
                                    effect = -28900,
                                )
                            ),
                    )
                ),
            )
        db.transaction { tx -> tx.upsertFeeDecisions(listOf(decision)) }

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(0, result.size)
    }

    @Test
    fun `invoice generation with full month of force majeure absences`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        val absenceDays =
            datesBetween(period.start, LocalDate.of(2019, 1, 31)).map {
                it to AbsenceType.FORCE_MAJEURE
            }

        initDataForAbsences(listOf(period), absenceDays)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

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
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        val forceMajeureAbsenceDays =
            datesBetween(period.start, LocalDate.of(2019, 1, 22)) // 15 operational days
                .map { it to AbsenceType.FORCE_MAJEURE }
        val plannedAbsenceDays =
            datesBetween(LocalDate.of(2019, 1, 23), LocalDate.of(2019, 1, 31)).map {
                it to AbsenceType.PLANNED_ABSENCE
            }

        initDataForAbsences(
            listOf(period),
            forceMajeureAbsenceDays + plannedAbsenceDays,
            serviceNeed = snDaycareContractDays15,
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(0, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(21700, invoiceRow.unitPrice)
                assertEquals(21700, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(productProvider.dailyRefund, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-21700, invoiceRow.unitPrice)
                assertEquals(-21700, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days and full month of planned absences`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        val absenceDays =
            datesBetween(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31)).map {
                it to AbsenceType.PLANNED_ABSENCE
            }

        initDataForAbsences(listOf(period), absenceDays, serviceNeed = snDaycareContractDays15)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(10850, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(21700, invoiceRow.unitPrice)
                assertEquals(21700, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(productProvider.fullMonthAbsence, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-10850, invoiceRow.unitPrice) // 21700 / 2
                assertEquals(-10850, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days and only 14 days of attendances (rest are planned absences)`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        // 14 operational days, other are planned absences
        val absenceDays =
            datesBetween(LocalDate.of(2019, 1, 22), LocalDate.of(2019, 1, 31)).map {
                it to AbsenceType.PLANNED_ABSENCE
            }

        initDataForAbsences(listOf(period), absenceDays, serviceNeed = snDaycareContractDays15)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            // Full month invoice expected
            assertEquals(21700, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(21700, invoiceRow.unitPrice)
                assertEquals(21700, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days, 2 decisions and only 14 days of attendances (rest are planned absences)`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))

        val decisions =
            listOf(
                createFeeDecisionFixture(
                    FeeDecisionStatus.SENT,
                    FeeDecisionType.NORMAL,
                    // 11 attendance days
                    DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 16)),
                    testAdult_1.id,
                    listOf(
                        createFeeDecisionChildFixture(
                            childId = testChild_1.id,
                            dateOfBirth = testChild_1.dateOfBirth,
                            placementUnitId = testDaycare.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDaycareContractDays15.toFeeDecisionServiceNeed(),
                            baseFee = 15000,
                            fee = 15000,
                            feeAlterations = listOf(),
                        )
                    ),
                ),
                createFeeDecisionFixture(
                    FeeDecisionStatus.SENT,
                    FeeDecisionType.NORMAL,
                    // 3 attendance days
                    DateRange(LocalDate.of(2019, 1, 17), LocalDate.of(2019, 1, 31)),
                    testAdult_1.id,
                    listOf(
                        createFeeDecisionChildFixture(
                            childId = testChild_1.id,
                            dateOfBirth = testChild_1.dateOfBirth,
                            placementUnitId = testDaycare.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDaycareContractDays15.toFeeDecisionServiceNeed(),
                            baseFee = 9000,
                            fee = 9000,
                            feeAlterations = listOf(),
                        )
                    ),
                ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(decisions)

        // 14 operational days, the rest are planned absences
        insertAbsences(
            testChild_1.id,
            datesBetween(LocalDate.of(2019, 1, 22), LocalDate.of(2019, 1, 31)).map {
                it to AbsenceType.PLANNED_ABSENCE
            },
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(13400, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(11, invoiceRow.amount)
                assertEquals(1000, invoiceRow.unitPrice) // 15000 / 15
                assertEquals(11000, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                // Invoice 4 days even though there's one day missing per planned absences
                assertEquals(4, invoiceRow.amount)
                assertEquals(600, invoiceRow.unitPrice) // 9000 / 15
                assertEquals(2400, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation when fee decision is valid only during weekend`() {
        val month = YearMonth.of(2020, 5)
        val period = FiniteDateRange.ofMonth(month)
        val weekEnd = FiniteDateRange(LocalDate.of(2020, 5, 2), LocalDate.of(2020, 5, 3))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision =
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                weekEnd.asDateRange(),
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
                        feeAlterations = listOf(),
                    )
                ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(listOf(decision))

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(0, result.size)
    }

    @Test
    fun `invoice generation when fee decision is valid only for two weeks in a round the clock unit`() {
        val month = YearMonth.of(2021, 1)
        val period = FiniteDateRange.ofMonth(month)
        val placementPeriod = FiniteDateRange(LocalDate.of(2021, 1, 18), LocalDate.of(2021, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision =
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                placementPeriod.asDateRange(),
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
                        feeAlterations = listOf(),
                    )
                ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(listOf(decision))

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

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
        val month = YearMonth.of(2021, 1)
        val period = FiniteDateRange.ofMonth(month)
        val placementPeriod = FiniteDateRange(LocalDate.of(2021, 1, 30), LocalDate.of(2021, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision =
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                placementPeriod.asDateRange(),
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
                        feeAlterations = listOf(),
                    )
                ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(
            listOf(decision),
            shiftCare = ShiftCareType.FULL,
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

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
        val month = YearMonth.of(2021, 1)
        val period = FiniteDateRange.ofMonth(month)
        val placementPeriod = FiniteDateRange(LocalDate.of(2021, 1, 2), LocalDate.of(2021, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision =
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                placementPeriod.asDateRange(),
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
                        feeAlterations = listOf(),
                    )
                ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(listOf(decision))

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

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
        val month = YearMonth.of(2021, 1)
        val period = FiniteDateRange.ofMonth(month)
        val placementPeriod = FiniteDateRange(LocalDate.of(2021, 1, 4), LocalDate.of(2021, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision =
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                placementPeriod.asDateRange(),
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
                        feeAlterations = listOf(),
                    )
                ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(listOf(decision))

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

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
        val month = YearMonth.of(2021, 1)
        val period = FiniteDateRange.ofMonth(month)
        val placementPeriod = FiniteDateRange(LocalDate.of(2021, 1, 4), LocalDate.of(2021, 1, 10))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision =
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                placementPeriod.asDateRange(),
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
                        feeAlterations = listOf(),
                    )
                ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(
            listOf(decision),
            shiftCare = ShiftCareType.FULL,
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

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
        val month = YearMonth.of(2021, 3)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decisions =
            listOf(
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
                            feeAlterations = listOf(),
                        )
                    ),
                )
            )
        insertDecisionsAndPlacementsAndServiceNeeds(decisions)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

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
        val month = YearMonth.of(2021, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decisions =
            listOf(
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
                            feeAlterations = listOf(),
                        )
                    ),
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
                            feeAlterations = listOf(),
                        )
                    ),
                ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(decisions, shiftCare = ShiftCareType.FULL)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

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
        val month = YearMonth.of(2021, 1)
        val period = FiniteDateRange.ofMonth(month)
        val firstPeriod = FiniteDateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 14))
        val secondPeriod = FiniteDateRange(LocalDate.of(2021, 1, 15), LocalDate.of(2021, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decisions =
            listOf(
                createFeeDecisionFixture(
                    FeeDecisionStatus.SENT,
                    FeeDecisionType.NORMAL,
                    firstPeriod.asDateRange(),
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
                            feeAlterations = listOf(),
                        )
                    ),
                ),
                createFeeDecisionFixture(
                    FeeDecisionStatus.SENT,
                    FeeDecisionType.NORMAL,
                    secondPeriod.asDateRange(),
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
                            feeAlterations = listOf(),
                        )
                    ),
                ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(decisions, shiftCare = ShiftCareType.FULL)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

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
        val month = YearMonth.of(2021, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision =
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.asDateRange(),
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
                        feeAlterations = listOf(),
                    )
                ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(listOf(decision))

        insertAbsences(
            testChild_1.id,
            listOf(LocalDate.of(2021, 1, 5) to AbsenceType.FORCE_MAJEURE),
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

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
        val month = YearMonth.of(2021, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision =
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.asDateRange(),
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
                        feeAlterations = listOf(),
                    )
                ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(
            listOf(decision),
            shiftCare = ShiftCareType.FULL,
        )

        insertAbsences(
            testChild_1.id,
            listOf(LocalDate.of(2021, 1, 31) to AbsenceType.FORCE_MAJEURE),
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

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
        val month = YearMonth.of(2021, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decision =
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.asDateRange(),
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
                        feeAlterations = listOf(),
                    )
                ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(listOf(decision))

        insertAbsences(
            testChild_1.id,
            datesBetween(period.start, period.end).map { it to AbsenceType.FORCE_MAJEURE },
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

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
        val month = YearMonth.of(2020, 5)
        val weekEnd = FiniteDateRange(LocalDate.of(2020, 5, 2), LocalDate.of(2020, 5, 3))
        val absenceDays =
            generateSequence(LocalDate.of(2020, 5, 1)) { date -> date.plusDays(1) }
                .takeWhile { date -> date < LocalDate.of(2020, 6, 1) }
                .map { date -> date to AbsenceType.SICKLEAVE }
                .toList()

        initDataForAbsences(listOf(weekEnd), absenceDays)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(0, result.size)
    }

    @Test
    fun `free july 2020 if child has been placed every month since Aug 2019 but not april or may`() {
        initFreeJulyTestData(
            YearMonth.of(2020, 7),
            listOf(
                FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2019, 8, 31)),
                FiniteDateRange(LocalDate.of(2019, 9, 1), LocalDate.of(2019, 9, 30)),
                FiniteDateRange(LocalDate.of(2019, 10, 1), LocalDate.of(2019, 10, 31)),
                FiniteDateRange(LocalDate.of(2019, 11, 1), LocalDate.of(2019, 11, 30)),
                FiniteDateRange(LocalDate.of(2019, 12, 1), LocalDate.of(2019, 12, 31)),
                FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31)),
                FiniteDateRange(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 29)),
                FiniteDateRange(LocalDate.of(2020, 3, 1), LocalDate.of(2020, 3, 31)),
                FiniteDateRange(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 6, 30)),
                FiniteDateRange(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 31)),
            ),
            PlacementType.DAYCARE_PART_TIME,
        )
        val result = db.read(getAllInvoices)
        assertEquals(0, result.size)
    }

    @Test
    fun `no free july 2021 if child has been placed every month since Aug 2019 but not april or may`() {
        initFreeJulyTestData(
            YearMonth.of(2021, 7),
            listOf(
                FiniteDateRange(LocalDate.of(2020, 8, 1), LocalDate.of(2020, 8, 31)),
                FiniteDateRange(LocalDate.of(2020, 9, 1), LocalDate.of(2020, 9, 30)),
                FiniteDateRange(LocalDate.of(2020, 10, 1), LocalDate.of(2020, 10, 31)),
                FiniteDateRange(LocalDate.of(2020, 11, 1), LocalDate.of(2020, 11, 30)),
                FiniteDateRange(LocalDate.of(2020, 12, 1), LocalDate.of(2020, 12, 31)),
                FiniteDateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31)),
                FiniteDateRange(LocalDate.of(2021, 2, 1), LocalDate.of(2021, 2, 28)),
                FiniteDateRange(LocalDate.of(2021, 3, 1), LocalDate.of(2021, 3, 31)),
                FiniteDateRange(LocalDate.of(2021, 6, 1), LocalDate.of(2021, 6, 30)),
                FiniteDateRange(LocalDate.of(2021, 7, 1), LocalDate.of(2021, 7, 31)),
            ),
        )
        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
    }

    @Test
    fun `free july 2020 if child has been placed every month since Aug 2019, also in april or may`() {
        initFreeJulyTestData(
            YearMonth.of(2020, 7),
            listOf(
                FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2019, 8, 31)),
                FiniteDateRange(LocalDate.of(2019, 9, 1), LocalDate.of(2019, 9, 30)),
                FiniteDateRange(LocalDate.of(2019, 10, 1), LocalDate.of(2019, 10, 31)),
                FiniteDateRange(LocalDate.of(2019, 11, 1), LocalDate.of(2019, 11, 30)),
                FiniteDateRange(LocalDate.of(2019, 12, 1), LocalDate.of(2019, 12, 31)),
                FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31)),
                FiniteDateRange(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 29)),
                FiniteDateRange(LocalDate.of(2020, 3, 1), LocalDate.of(2020, 3, 31)),
                FiniteDateRange(LocalDate.of(2020, 4, 1), LocalDate.of(2020, 4, 30)),
                FiniteDateRange(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31)),
                FiniteDateRange(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 6, 30)),
                FiniteDateRange(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 31)),
            ),
        )
        val result = db.read(getAllInvoices)
        assertEquals(0, result.size)
    }

    @Test
    fun `free july 2020 if child has been placed every month since Sep 2019, also in april or may (freeJulyStartOnSeptember = true)`() {
        initFreeJulyTestData(
            YearMonth.of(2020, 7),
            listOf(
                FiniteDateRange(LocalDate.of(2019, 9, 1), LocalDate.of(2019, 9, 30)),
                FiniteDateRange(LocalDate.of(2019, 10, 1), LocalDate.of(2019, 10, 31)),
                FiniteDateRange(LocalDate.of(2019, 11, 1), LocalDate.of(2019, 11, 30)),
                FiniteDateRange(LocalDate.of(2019, 12, 1), LocalDate.of(2019, 12, 31)),
                FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31)),
                FiniteDateRange(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 29)),
                FiniteDateRange(LocalDate.of(2020, 3, 1), LocalDate.of(2020, 3, 31)),
                FiniteDateRange(LocalDate.of(2020, 4, 1), LocalDate.of(2020, 4, 30)),
                FiniteDateRange(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31)),
                FiniteDateRange(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 6, 30)),
                FiniteDateRange(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 31)),
            ),
            freeJulyStartOnSeptember = true,
        )
        val result = db.read(getAllInvoices)
        assertEquals(0, result.size)
    }

    @Test
    fun `free july 2020 if child has been placed even for one day every month since Aug 2019`() {
        initFreeJulyTestData(
            YearMonth.of(2020, 7),
            listOf(
                FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2019, 8, 1)),
                FiniteDateRange(LocalDate.of(2019, 9, 1), LocalDate.of(2019, 9, 1)),
                FiniteDateRange(LocalDate.of(2019, 10, 1), LocalDate.of(2019, 10, 1)),
                FiniteDateRange(LocalDate.of(2019, 11, 1), LocalDate.of(2019, 11, 1)),
                FiniteDateRange(LocalDate.of(2019, 12, 1), LocalDate.of(2019, 12, 1)),
                FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 1)),
                FiniteDateRange(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 1)),
                FiniteDateRange(LocalDate.of(2020, 3, 1), LocalDate.of(2020, 3, 1)),
                FiniteDateRange(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 6, 1)),
                FiniteDateRange(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 1)),
            ),
        )
        val result = db.read(getAllInvoices)
        assertEquals(0, result.size)
    }

    @Test
    fun `free july 2020 if child has been placed even for one day every month since Sep 2019 (freeJulyStartOnSeptember = true)`() {
        initFreeJulyTestData(
            YearMonth.of(2020, 7),
            listOf(
                FiniteDateRange(LocalDate.of(2019, 9, 1), LocalDate.of(2019, 9, 1)),
                FiniteDateRange(LocalDate.of(2019, 10, 1), LocalDate.of(2019, 10, 1)),
                FiniteDateRange(LocalDate.of(2019, 11, 1), LocalDate.of(2019, 11, 1)),
                FiniteDateRange(LocalDate.of(2019, 12, 1), LocalDate.of(2019, 12, 1)),
                FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 1)),
                FiniteDateRange(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 1)),
                FiniteDateRange(LocalDate.of(2020, 3, 1), LocalDate.of(2020, 3, 1)),
                FiniteDateRange(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 6, 1)),
                FiniteDateRange(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 1)),
            ),
            freeJulyStartOnSeptember = true,
        )
        val result = db.read(getAllInvoices)
        assertEquals(0, result.size)
    }

    @Test
    fun `free july 2020 if child has been placed all the time`() {
        initFreeJulyTestData(
            YearMonth.of(2020, 7),
            listOf(FiniteDateRange(LocalDate.of(2018, 7, 1), LocalDate.of(2021, 7, 31))),
        )
        val result = db.read(getAllInvoices)
        assertEquals(0, result.size)
    }

    @Test
    fun `free july 2021 if child has been placed all the time`() {
        initFreeJulyTestData(
            YearMonth.of(2021, 7),
            listOf(FiniteDateRange(LocalDate.of(2018, 7, 1), LocalDate.of(2021, 7, 31))),
        )
        val result = db.read(getAllInvoices)
        assertEquals(0, result.size)
    }

    @Test
    fun `free july 2021 if child has been placed in preparatory with daycare all the time`() {
        initFreeJulyTestData(
            YearMonth.of(2021, 7),
            listOf(
                PlacementType.PREPARATORY_DAYCARE to
                    FiniteDateRange(LocalDate.of(2018, 7, 1), LocalDate.of(2021, 7, 31))
            ),
        )
        val result = db.read(getAllInvoices)
        assertEquals(0, result.size)
    }

    @Test
    fun `no free july 2020 if even one mandatory month has no placement`() {
        initFreeJulyTestData(
            YearMonth.of(2020, 7),
            listOf(
                FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2019, 8, 31)),
                FiniteDateRange(LocalDate.of(2019, 9, 1), LocalDate.of(2019, 9, 30)),
                FiniteDateRange(LocalDate.of(2019, 10, 1), LocalDate.of(2019, 10, 31)),
                FiniteDateRange(LocalDate.of(2019, 11, 1), LocalDate.of(2019, 11, 30)),
                FiniteDateRange(LocalDate.of(2019, 12, 1), LocalDate.of(2019, 12, 31)),
                FiniteDateRange(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 28)),
                FiniteDateRange(LocalDate.of(2020, 3, 1), LocalDate.of(2020, 3, 31)),
                FiniteDateRange(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 6, 30)),
                FiniteDateRange(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 31)),
            ),
        )

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
    }

    @Test
    fun `no free july 2020 if child has a mix of club and daycare placements`() {
        initFreeJulyTestData(
            YearMonth.of(2020, 7),
            listOf(
                PlacementType.CLUB to
                    FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 5, 31)),
                PlacementType.DAYCARE to
                    FiniteDateRange(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 7, 31)),
            ),
        )

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
    }

    @Test
    fun `no free july 2020 if child has a mix of preschool and preschool daycare placements`() {
        initFreeJulyTestData(
            YearMonth.of(2020, 7),
            listOf(
                PlacementType.PRESCHOOL to
                    FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 5, 31)),
                PlacementType.PRESCHOOL_DAYCARE to
                    FiniteDateRange(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 7, 31)),
            ),
        )

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
    }

    @Test
    fun `free july 2020 applies only on july`() {
        initFreeJulyTestData(
            YearMonth.of(2020, 6),
            listOf(
                FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2019, 8, 31)),
                FiniteDateRange(LocalDate.of(2019, 9, 1), LocalDate.of(2019, 9, 30)),
                FiniteDateRange(LocalDate.of(2019, 10, 1), LocalDate.of(2019, 10, 31)),
                FiniteDateRange(LocalDate.of(2019, 11, 1), LocalDate.of(2019, 11, 30)),
                FiniteDateRange(LocalDate.of(2019, 12, 1), LocalDate.of(2019, 12, 31)),
                FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31)),
                FiniteDateRange(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 28)),
                FiniteDateRange(LocalDate.of(2020, 3, 1), LocalDate.of(2020, 3, 31)),
                FiniteDateRange(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 6, 30)),
                FiniteDateRange(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 31)),
            ),
        )
        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
    }

    @Test
    fun `plain preschool is not invoiced`() {
        assertFalse(PlacementType.PRESCHOOL.isInvoiced())

        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        initByPeriodAndPlacementType(period, PlacementType.PRESCHOOL)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)

        assertEquals(0, result.size)
    }

    @Test
    fun `plain preparatory is not invoiced`() {
        assertFalse(PlacementType.PREPARATORY.isInvoiced())

        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        initByPeriodAndPlacementType(period, PlacementType.PREPARATORY)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)

        assertEquals(0, result.size)
    }

    @Test
    fun `plain club is not invoiced`() {
        assertFalse(PlacementType.CLUB.isInvoiced())

        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        initByPeriodAndPlacementType(period, PlacementType.CLUB)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)

        assertEquals(0, result.size)
    }

    @Test
    fun `invoice codebtor is set when partner on decision is child's guardian`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        initByPeriodAndPlacementType(period, PlacementType.DAYCARE, partner = testAdult_2.id)
        db.transaction {
            it.insertGuardian(testAdult_1.id, testChild_1.id)
            it.insertGuardian(testAdult_2.id, testChild_1.id)
        }

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }
        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(testAdult_2.id, invoice.codebtor)
        }
    }

    @Test
    fun `invoice codebtor is not set when partner on decision is not child's guardian`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        initByPeriodAndPlacementType(period, PlacementType.DAYCARE, partner = testAdult_2.id)
        db.transaction { it.insertGuardian(testAdult_1.id, testChild_1.id) }

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }
        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertNull(invoice.codebtor)
        }
    }

    @Test
    fun `invoice codebtor is not set when partner on decision is not any child's guardian`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        initByPeriodAndPlacementType(
            period,
            PlacementType.DAYCARE,
            children = listOf(testChild_1, testChild_2),
            partner = testAdult_2.id,
        )
        db.transaction {
            it.insertGuardian(testAdult_1.id, testChild_1.id)
            it.insertGuardian(testAdult_1.id, testChild_2.id)
        }

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }
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
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        // Override to use 20 days instead when calculating a daily refund
        val featureConfig = featureConfig.copy(dailyFeeDivisorOperationalDaysOverride = 20)
        val generator =
            InvoiceGenerator(
                DraftInvoiceGenerator(
                    productProvider,
                    featureConfig,
                    DefaultInvoiceGenerationLogic,
                ),
                featureConfig,
            )

        val absenceDays =
            listOf(
                LocalDate.of(2019, 1, 2) to AbsenceType.FORCE_MAJEURE,
                LocalDate.of(2019, 1, 3) to AbsenceType.FORCE_MAJEURE,
            )

        initDataForAbsences(listOf(period), absenceDays)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

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
    fun `invoice generation with 15 contract days for half a month with some planned absences`() {
        // 23 operational days
        val month = YearMonth.of(2021, 3)
        val period = FiniteDateRange.ofMonth(month)

        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decisions =
            listOf(
                createFeeDecisionFixture(
                    FeeDecisionStatus.SENT,
                    FeeDecisionType.NORMAL,
                    // 12 operational days
                    DateRange(LocalDate.of(2021, 3, 16), LocalDate.of(2021, 3, 31)),
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
                            fee = 21700,
                            feeAlterations = listOf(),
                        )
                    ),
                )
            )
        insertDecisionsAndPlacementsAndServiceNeeds(decisions)

        // 2 days of planned absences -> total 12 - 2 = 10 attendance days
        insertAbsences(
            testChild_1.id,
            listOf(
                LocalDate.of(2021, 3, 18) to AbsenceType.PLANNED_ABSENCE,
                LocalDate.of(2021, 3, 19) to AbsenceType.PLANNED_ABSENCE,
            ),
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(14470, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(10, invoiceRow.amount)
                assertEquals(1447, invoiceRow.unitPrice) // 21700 / 15
                assertEquals(14470, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days for a partial month with all days as planned or other absences`() {
        val month = YearMonth.of(2021, 3)
        val period = FiniteDateRange.ofMonth(month)

        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decisions =
            listOf(
                createFeeDecisionFixture(
                    FeeDecisionStatus.SENT,
                    FeeDecisionType.NORMAL,
                    // 3 operational days
                    DateRange(LocalDate.of(2021, 3, 16), LocalDate.of(2021, 3, 31)),
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
                            feeAlterations = listOf(),
                        )
                    ),
                )
            )
        insertDecisionsAndPlacementsAndServiceNeeds(decisions)

        // 3 other absences, rest are planned absences
        insertAbsences(
            testChild_1.id,
            listOf(
                LocalDate.of(2021, 3, 16) to AbsenceType.OTHER_ABSENCE,
                LocalDate.of(2021, 3, 17) to AbsenceType.OTHER_ABSENCE,
                LocalDate.of(2021, 3, 18) to AbsenceType.OTHER_ABSENCE,
            ) +
                datesBetween(LocalDate.of(2021, 3, 19), LocalDate.of(2021, 3, 31)).map { date ->
                    date to AbsenceType.PLANNED_ABSENCE
                },
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(2889, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(3, invoiceRow.amount)
                assertEquals(1927, invoiceRow.unitPrice) // 28900 / 15
                assertEquals(5781, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                // All days
                assertEquals(3, invoiceRow.amount)
                assertEquals(-964, invoiceRow.unitPrice) // all days
                assertEquals(-2892, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days for a partial month with all days as planned absences or sick leaves (freeSickLeaveOnContractDays = false)`() {
        val month = YearMonth.of(2021, 3)
        val period = FiniteDateRange.ofMonth(month)

        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decisions =
            listOf(
                createFeeDecisionFixture(
                    FeeDecisionStatus.SENT,
                    FeeDecisionType.NORMAL,
                    // 3 operational days
                    DateRange(LocalDate.of(2021, 3, 16), LocalDate.of(2021, 3, 31)),
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
                            feeAlterations = listOf(),
                        )
                    ),
                )
            )
        insertDecisionsAndPlacementsAndServiceNeeds(decisions)

        // 3 sick leaves, rest are planned absences
        insertAbsences(
            testChild_1.id,
            listOf(
                LocalDate.of(2021, 3, 16) to AbsenceType.SICKLEAVE,
                LocalDate.of(2021, 3, 17) to AbsenceType.SICKLEAVE,
                LocalDate.of(2021, 3, 18) to AbsenceType.SICKLEAVE,
            ) +
                datesBetween(LocalDate.of(2021, 3, 19), LocalDate.of(2021, 3, 31)).map { date ->
                    date to AbsenceType.PLANNED_ABSENCE
                },
        )

        // freeSickLeaveOnContractDays = false
        // ==> 50 % discount because this case is considered a normal full month of absences
        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(2889, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(3, invoiceRow.amount)
                assertEquals(1927, invoiceRow.unitPrice) // 28900 / 15
                assertEquals(5781, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(productProvider.fullMonthAbsence, invoiceRow.product)
                assertEquals(3, invoiceRow.amount)
                assertEquals(-964, invoiceRow.unitPrice) // -50 %
                assertEquals(-2892, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days for a partial month with all days as planned absences or sick leaves (freeSickLeaveOnContractDays = true)`() {
        val month = YearMonth.of(2021, 3)
        val period = FiniteDateRange.ofMonth(month)

        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decisions =
            listOf(
                createFeeDecisionFixture(
                    FeeDecisionStatus.SENT,
                    FeeDecisionType.NORMAL,
                    // 3 operational days
                    DateRange(LocalDate.of(2021, 3, 16), LocalDate.of(2021, 3, 31)),
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
                            feeAlterations = listOf(),
                        )
                    ),
                )
            )
        insertDecisionsAndPlacementsAndServiceNeeds(decisions)

        // 3 sick leaves, rest are planned absences
        insertAbsences(
            testChild_1.id,
            listOf(
                LocalDate.of(2021, 3, 16) to AbsenceType.SICKLEAVE,
                LocalDate.of(2021, 3, 17) to AbsenceType.SICKLEAVE,
                LocalDate.of(2021, 3, 18) to AbsenceType.SICKLEAVE,
            ) +
                datesBetween(LocalDate.of(2021, 3, 19), LocalDate.of(2021, 3, 31)).map { date ->
                    date to AbsenceType.PLANNED_ABSENCE
                },
        )

        // freeSickLeaveOnContractDays = true
        // ==> 100 % discount because this case is considered a full month of sick leaves
        val featureConfig = featureConfig.copy(freeSickLeaveOnContractDays = true)
        val generator =
            InvoiceGenerator(
                DraftInvoiceGenerator(
                    productProvider,
                    featureConfig,
                    DefaultInvoiceGenerationLogic,
                ),
                featureConfig,
            )
        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(0, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(3, invoiceRow.amount)
                assertEquals(1927, invoiceRow.unitPrice) // 28900 / 15
                assertEquals(5781, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(productProvider.fullMonthSickLeave, invoiceRow.product)
                assertEquals(3, invoiceRow.amount)
                assertEquals(-1927, invoiceRow.unitPrice)
                assertEquals(-5781, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days for half a month with (useContractDaysAsDailyFeeDivisor = false)`() {
        // 23 operational days
        val month = YearMonth.of(2021, 3)
        val period = FiniteDateRange.ofMonth(month)

        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decisions =
            listOf(
                createFeeDecisionFixture(
                    FeeDecisionStatus.SENT,
                    FeeDecisionType.NORMAL,
                    // 12 operational days
                    DateRange(LocalDate.of(2021, 3, 16), LocalDate.of(2021, 3, 31)),
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
                            fee = 21700,
                            feeAlterations = listOf(),
                        )
                    ),
                )
            )
        insertDecisionsAndPlacementsAndServiceNeeds(decisions)

        // Override useContractDaysAsDailyFeeDivisor
        val featureConfig = featureConfig.copy(useContractDaysAsDailyFeeDivisor = false)
        val generator =
            InvoiceGenerator(
                DraftInvoiceGenerator(
                    productProvider,
                    featureConfig,
                    DefaultInvoiceGenerationLogic,
                ),
                featureConfig,
            )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(11316, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(12, invoiceRow.amount)
                assertEquals(943, invoiceRow.unitPrice) // 21700 / 23
                assertEquals(11316, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days for half a month with some planned absences with (useContractDaysAsDailyFeeDivisor = false)`() {
        // 23 operational days
        val month = YearMonth.of(2021, 3)
        val period = FiniteDateRange.ofMonth(month)

        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decisions =
            listOf(
                createFeeDecisionFixture(
                    FeeDecisionStatus.SENT,
                    FeeDecisionType.NORMAL,
                    // 12 operational days
                    DateRange(LocalDate.of(2021, 3, 16), LocalDate.of(2021, 3, 31)),
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
                            fee = 21700,
                            feeAlterations = listOf(),
                        )
                    ),
                )
            )
        insertDecisionsAndPlacementsAndServiceNeeds(decisions)

        // 2 days of planned absences -> total 12 - 2 = 10 attendance days
        insertAbsences(
            testChild_1.id,
            listOf(
                LocalDate.of(2021, 3, 18) to AbsenceType.PLANNED_ABSENCE,
                LocalDate.of(2021, 3, 19) to AbsenceType.PLANNED_ABSENCE,
            ),
        )

        // Override useContractDaysAsDailyFeeDivisor
        val featureConfig = featureConfig.copy(useContractDaysAsDailyFeeDivisor = false)
        val generator =
            InvoiceGenerator(
                DraftInvoiceGenerator(
                    productProvider,
                    featureConfig,
                    DefaultInvoiceGenerationLogic,
                ),
                featureConfig,
            )
        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(11316, invoice.totalPrice)
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(12, invoiceRow.amount) // Planned absences do not affect the amount
                assertEquals(943, invoiceRow.unitPrice) // 21700 / 23
                assertEquals(11316, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days for a partial month with all days as planned or other absences with (useContractDaysAsDailyFeeDivisor = false)`() {
        val month = YearMonth.of(2021, 3)
        val period = FiniteDateRange.ofMonth(month)

        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decisions =
            listOf(
                createFeeDecisionFixture(
                    FeeDecisionStatus.SENT,
                    FeeDecisionType.NORMAL,
                    // 3 operational days
                    DateRange(LocalDate.of(2021, 3, 16), LocalDate.of(2021, 3, 31)),
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
                            fee = 21700,
                            feeAlterations = listOf(),
                        )
                    ),
                )
            )
        insertDecisionsAndPlacementsAndServiceNeeds(decisions)

        // 3 other absences, rest are planned absences
        insertAbsences(
            testChild_1.id,
            listOf(
                LocalDate.of(2021, 3, 16) to AbsenceType.OTHER_ABSENCE,
                LocalDate.of(2021, 3, 17) to AbsenceType.OTHER_ABSENCE,
                LocalDate.of(2021, 3, 18) to AbsenceType.OTHER_ABSENCE,
            ) +
                datesBetween(LocalDate.of(2021, 3, 19), LocalDate.of(2021, 3, 31)).map { date ->
                    date to AbsenceType.PLANNED_ABSENCE
                },
        )

        // Override useContractDaysAsDailyFeeDivisor
        val featureConfig = featureConfig.copy(useContractDaysAsDailyFeeDivisor = false)
        val generator =
            InvoiceGenerator(
                DraftInvoiceGenerator(
                    productProvider,
                    featureConfig,
                    DefaultInvoiceGenerationLogic,
                ),
                featureConfig,
            )
        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(5652, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(12, invoiceRow.amount)
                assertEquals(943, invoiceRow.unitPrice) // 21700 / 23
                assertEquals(11316, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(12, invoiceRow.amount) // All days
                assertEquals(-472, invoiceRow.unitPrice) // -50 %
                assertEquals(-5664, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days for a partial month with all days as planned absences or sick leaves with (useContractDaysAsDailyFeeDivisor = false)`() {
        val month = YearMonth.of(2021, 3)
        val period = FiniteDateRange.ofMonth(month)

        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decisions =
            listOf(
                createFeeDecisionFixture(
                    FeeDecisionStatus.SENT,
                    FeeDecisionType.NORMAL,
                    // 3 operational days
                    DateRange(LocalDate.of(2021, 3, 16), LocalDate.of(2021, 3, 31)),
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
                            fee = 21700,
                            feeAlterations = listOf(),
                        )
                    ),
                )
            )
        insertDecisionsAndPlacementsAndServiceNeeds(decisions)

        // 3 sick leaves, rest are planned absences
        insertAbsences(
            testChild_1.id,
            listOf(
                LocalDate.of(2021, 3, 16) to AbsenceType.SICKLEAVE,
                LocalDate.of(2021, 3, 17) to AbsenceType.SICKLEAVE,
                LocalDate.of(2021, 3, 18) to AbsenceType.SICKLEAVE,
            ) +
                datesBetween(LocalDate.of(2021, 3, 19), LocalDate.of(2021, 3, 31)).map { date ->
                    date to AbsenceType.PLANNED_ABSENCE
                },
        )

        // Override useContractDaysAsDailyFeeDivisor
        val featureConfig = featureConfig.copy(useContractDaysAsDailyFeeDivisor = false)
        val generator =
            InvoiceGenerator(
                DraftInvoiceGenerator(
                    productProvider,
                    featureConfig,
                    DefaultInvoiceGenerationLogic,
                ),
                featureConfig,
            )
        // freeSickLeaveOnContractDays = false
        // ==> 50 % discount because this case is considered a normal full month of absences
        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(5652, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(12, invoiceRow.amount)
                assertEquals(943, invoiceRow.unitPrice) // 21700 / 23
                assertEquals(11316, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(12, invoiceRow.amount)
                assertEquals(-472, invoiceRow.unitPrice) // -50 %
                assertEquals(-5664, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days, 2 fee decisions and 2 surplus days with (useContractDaysAsDailyFeeDivisor = false)`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))

        val decisions =
            listOf(
                    DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 15)) to 22000,
                    DateRange(LocalDate.of(2019, 1, 16), LocalDate.of(2019, 1, 31)) to 11000,
                )
                .map { (range, fee) ->
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
                                fee =
                                    roundToEuros(
                                            BigDecimal(fee) * snDaycareContractDays15.feeCoefficient
                                        )
                                        .toInt(),
                            )
                        ),
                    )
                }
        insertDecisionsAndPlacementsAndServiceNeeds(decisions)

        // 17 operational days in total, the rest are planned absences
        insertAbsences(
            testChild_1.id,
            (datesBetween(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 4)) +
                    datesBetween(LocalDate.of(2019, 1, 30), LocalDate.of(2019, 1, 31)))
                .map { it to AbsenceType.PLANNED_ABSENCE },
        )

        // Override useContractDaysAsDailyFeeDivisor
        val featureConfig = featureConfig.copy(useContractDaysAsDailyFeeDivisor = false)
        val generator =
            InvoiceGenerator(
                DraftInvoiceGenerator(
                    productProvider,
                    featureConfig,
                    DefaultInvoiceGenerationLogic,
                ),
                featureConfig,
            )
        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(13130, invoice.totalPrice)
            assertEquals(3, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(
                    productProvider.mapToProduct(PlacementType.DAYCARE),
                    invoiceRow.product,
                )
                assertEquals(10, invoiceRow.amount)
                assertEquals(750, invoiceRow.unitPrice) // 16500 / 22
                assertEquals(7500, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(
                    productProvider.mapToProduct(PlacementType.DAYCARE),
                    invoiceRow.product,
                )
                assertEquals(12, invoiceRow.amount)
                assertEquals(377, invoiceRow.unitPrice) // 8300 / 22
                assertEquals(4524, invoiceRow.price)
            }
            invoice.rows[2].let { invoiceRow ->
                assertEquals(productProvider.contractSurplusDay, invoiceRow.product)
                assertEquals(2, invoiceRow.amount)
                assertEquals(553, invoiceRow.unitPrice) // 8300 / 15
                assertEquals(1106, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with a fixed daily fee divisor for half a month`() {
        // 23 operational days
        val month = YearMonth.of(2021, 3)
        val period = FiniteDateRange.ofMonth(month)

        // Override to use 20 days instead when calculating a daily fee
        val featureConfig = featureConfig.copy(dailyFeeDivisorOperationalDaysOverride = 20)
        val generator =
            InvoiceGenerator(
                DraftInvoiceGenerator(
                    productProvider,
                    featureConfig,
                    DefaultInvoiceGenerationLogic,
                ),
                featureConfig,
            )

        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decisions =
            listOf(
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
                            feeAlterations = listOf(),
                        )
                    ),
                )
            )
        insertDecisionsAndPlacementsAndServiceNeeds(decisions)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

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
        val month = YearMonth.of(2021, 3)
        val period = FiniteDateRange.ofMonth(month)

        // Override to use 20 days instead when calculating a daily fee
        val featureConfig = featureConfig.copy(dailyFeeDivisorOperationalDaysOverride = 20)
        val generator =
            InvoiceGenerator(
                DraftInvoiceGenerator(
                    productProvider,
                    featureConfig,
                    DefaultInvoiceGenerationLogic,
                ),
                featureConfig,
            )

        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decisions =
            listOf(
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
                            feeAlterations = listOf(),
                        )
                    ),
                )
            )
        insertDecisionsAndPlacementsAndServiceNeeds(decisions)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

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

    @Test
    fun `invoice generation with daily fee divisor 20 and only 19 operational days`() {
        // Easter
        db.transaction { tx ->
            tx.execute {
                sql(
                    """
                    INSERT INTO holiday (date)
                    VALUES
                        (${bind(LocalDate.of(2022, 4, 15))}),
                        (${bind(LocalDate.of(2022, 4, 18))})
                    """
                )
            }
        }

        // 19 operational days
        val month = YearMonth.of(2022, 4)
        val period = FiniteDateRange.ofMonth(month)

        initDataForAbsences(listOf(period), listOf())

        // Override to use 20 as the daily fee divisor
        val featureConfig = featureConfig.copy(dailyFeeDivisorOperationalDaysOverride = 20)
        val generator =
            InvoiceGenerator(
                DraftInvoiceGenerator(
                    productProvider,
                    featureConfig,
                    DefaultInvoiceGenerationLogic,
                ),
                featureConfig,
            )
        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

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
    fun `invoice generation with daily fee divisor 20 and only 19 operational days with a force majeure absence`() {
        // Easter
        db.transaction { tx ->
            tx.execute {
                sql(
                    """
                    INSERT INTO holiday (date)
                    VALUES
                        (${bind(LocalDate.of(2022, 4, 15))}),
                        (${bind(LocalDate.of(2022, 4, 18))})
                    """
                )
            }
        }

        // 19 operational days
        val month = YearMonth.of(2022, 4)
        val period = FiniteDateRange.ofMonth(month)

        val absenceDays = listOf(LocalDate.of(2022, 4, 1) to AbsenceType.FORCE_MAJEURE)

        initDataForAbsences(listOf(period), absenceDays)

        // Override to use 20 as the daily fee divisor
        val featureConfig = featureConfig.copy(dailyFeeDivisorOperationalDaysOverride = 20)
        val generator =
            InvoiceGenerator(
                DraftInvoiceGenerator(
                    productProvider,
                    featureConfig,
                    DefaultInvoiceGenerationLogic,
                ),
                featureConfig,
            )
        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(27455, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(productProvider.dailyRefund, invoiceRow.product)
                assertEquals(-1445, invoiceRow.unitPrice) // 28900 / 20
                assertEquals(-1445, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with daily fee divisor 20 and only 19 operational days with force majeure absence on all days`() {
        val pitkaperjantai = LocalDate.of(2022, 4, 15)
        val toinenPaasiaispaiva = LocalDate.of(2022, 4, 18)
        db.transaction { tx ->
            tx.insert(DevHoliday(pitkaperjantai))
            tx.insert(DevHoliday(toinenPaasiaispaiva))
        }

        // 19 operational days
        val month = YearMonth.of(2022, 4)
        val period = FiniteDateRange.ofMonth(month)

        // All operation days are force majeure absences
        val absenceDays =
            datesBetween(LocalDate.of(2022, 4, 1), LocalDate.of(2022, 4, 30))
                .filterNot {
                    it.dayOfWeek == DayOfWeek.SATURDAY ||
                        it.dayOfWeek == DayOfWeek.SUNDAY ||
                        it == pitkaperjantai ||
                        it == toinenPaasiaispaiva
                }
                .map { it to AbsenceType.FORCE_MAJEURE }

        initDataForAbsences(listOf(period), absenceDays)

        // Override to use 20 as the daily fee divisor
        val featureConfig = featureConfig.copy(dailyFeeDivisorOperationalDaysOverride = 20)
        val generator =
            InvoiceGenerator(
                DraftInvoiceGenerator(
                    productProvider,
                    featureConfig,
                    DefaultInvoiceGenerationLogic,
                ),
                featureConfig,
            )
        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

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
                assertEquals(productProvider.dailyRefund, invoiceRow.product)
                assertEquals(-28900, invoiceRow.unitPrice)
                assertEquals(-28900, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice corrections are applied to invoices when generation is done multiple times`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val decisions =
            listOf(
                createFeeDecisionFixture(
                    FeeDecisionStatus.SENT,
                    FeeDecisionType.NORMAL,
                    period.asDateRange(),
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
                        )
                    ),
                )
            )
        insertDecisionsAndPlacementsAndServiceNeeds(decisions)
        db.transaction {
            it.insert(
                DevInvoiceCorrection(
                    headOfFamilyId = testAdult_1.id,
                    childId = testChild_1.id,
                    amount = 1,
                    unitPrice = -28900,
                    period = FiniteDateRange(LocalDate.of(2018, 12, 1), LocalDate.of(2018, 12, 31)),
                    unitId = testDaycare.id,
                    product = productProvider.mapToProduct(PlacementType.DAYCARE),
                    description = "",
                    note = "",
                )
            )
        }

        fun assertResult(result: List<Invoice>) {
            assertEquals(1, result.size)
            result.first().let { invoice ->
                assertEquals(testAdult_1.id, invoice.headOfFamily)
                assertEquals(0, invoice.totalPrice)
                assertEquals(2, invoice.rows.size)
                invoice.rows[0].let { invoiceRow ->
                    assertEquals(testChild_1.id, invoiceRow.child)
                    assertEquals(
                        productProvider.mapToProduct(PlacementType.DAYCARE),
                        invoiceRow.product,
                    )
                    assertEquals(1, invoiceRow.amount)
                    assertEquals(28900, invoiceRow.unitPrice)
                    assertEquals(28900, invoiceRow.price)
                }
                invoice.rows[1].let { invoiceRow ->
                    assertEquals(testChild_1.id, invoiceRow.child)
                    assertEquals(
                        productProvider.mapToProduct(PlacementType.DAYCARE),
                        invoiceRow.product,
                    )
                    assertEquals(1, invoiceRow.amount)
                    assertEquals(-28900, invoiceRow.unitPrice)
                    assertEquals(-28900, invoiceRow.price)
                }
            }
        }

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }
        assertResult(db.read(getAllInvoices))
        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }
        assertResult(db.read(getAllInvoices))
    }

    @Test
    fun `invoice generation with 15 contract days, 1 sick leave and 1 surplus day`() {
        // 22 operational days
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        // 14 operational days first
        // then 1 sickleave
        val sickleaveDays = listOf(LocalDate.of(2019, 1, 22) to AbsenceType.SICKLEAVE)
        // then planned absences
        val plannedAbsenceDays =
            listOf(
                    LocalDate.of(2019, 1, 23),
                    LocalDate.of(2019, 1, 24),
                    LocalDate.of(2019, 1, 25),
                    LocalDate.of(2019, 1, 28),
                    LocalDate.of(2019, 1, 29),
                    LocalDate.of(2019, 1, 30),
                )
                .map { it to AbsenceType.PLANNED_ABSENCE }
        // then 1 more operational days

        initDataForAbsences(
            listOf(period),
            sickleaveDays + plannedAbsenceDays,
            serviceNeed = snDaycareContractDays15,
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(23147, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(21700, invoiceRow.unitPrice)
                assertEquals(21700, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(productProvider.contractSurplusDay, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(1447, invoiceRow.unitPrice) // 21700 / 15
                assertEquals(1447, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 10 contract days, 10 sick leaves`() {
        // 22 operational days
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        // 10 sickleaves
        val sickleaveDays =
            listOf(
                LocalDate.of(2019, 1, 2) to AbsenceType.SICKLEAVE,
                LocalDate.of(2019, 1, 3) to AbsenceType.SICKLEAVE,
                LocalDate.of(2019, 1, 4) to AbsenceType.SICKLEAVE,
                LocalDate.of(2019, 1, 7) to AbsenceType.SICKLEAVE,
                LocalDate.of(2019, 1, 8) to AbsenceType.SICKLEAVE,
                LocalDate.of(2019, 1, 9) to AbsenceType.SICKLEAVE,
                LocalDate.of(2019, 1, 10) to AbsenceType.SICKLEAVE,
                LocalDate.of(2019, 1, 11) to AbsenceType.SICKLEAVE,
                LocalDate.of(2019, 1, 14) to AbsenceType.SICKLEAVE,
                LocalDate.of(2019, 1, 15) to AbsenceType.SICKLEAVE,
            )
        // then 12 planned absences
        val plannedAbsenceDays =
            listOf(
                    LocalDate.of(2019, 1, 16),
                    LocalDate.of(2019, 1, 17),
                    LocalDate.of(2019, 1, 18),
                    LocalDate.of(2019, 1, 21),
                    LocalDate.of(2019, 1, 22),
                    LocalDate.of(2019, 1, 23),
                    LocalDate.of(2019, 1, 24),
                    LocalDate.of(2019, 1, 25),
                    LocalDate.of(2019, 1, 28),
                    LocalDate.of(2019, 1, 29),
                    LocalDate.of(2019, 1, 30),
                    LocalDate.of(2019, 1, 31),
                )
                .map { it to AbsenceType.PLANNED_ABSENCE }

        initDataForAbsences(
            listOf(period),
            sickleaveDays + plannedAbsenceDays,
            serviceNeed = snDaycareContractDays10,
        )

        // freeSickLeaveOnContractDays = true
        // ==> 100 % discount because this case is considered a full month of sick leaves
        val featureConfig = featureConfig.copy(freeSickLeaveOnContractDays = true)
        val generator =
            InvoiceGenerator(
                DraftInvoiceGenerator(
                    productProvider,
                    featureConfig,
                    DefaultInvoiceGenerationLogic,
                ),
                featureConfig,
            )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(0, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(14500, invoiceRow.unitPrice)
                assertEquals(14500, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(productProvider.fullMonthSickLeave, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-14500, invoiceRow.unitPrice)
                assertEquals(-14500, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 10 contract days, 11 sick leave and 2 surplus day`() {
        // 22 operational days
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        // 1 operational days first
        // then 11 sickleaves
        val sickleaveDays =
            listOf(
                LocalDate.of(2019, 1, 3) to AbsenceType.SICKLEAVE,
                LocalDate.of(2019, 1, 4) to AbsenceType.SICKLEAVE,
                LocalDate.of(2019, 1, 7) to AbsenceType.SICKLEAVE,
                LocalDate.of(2019, 1, 8) to AbsenceType.SICKLEAVE,
                LocalDate.of(2019, 1, 9) to AbsenceType.SICKLEAVE,
                LocalDate.of(2019, 1, 10) to AbsenceType.SICKLEAVE,
                LocalDate.of(2019, 1, 11) to AbsenceType.SICKLEAVE,
                LocalDate.of(2019, 1, 14) to AbsenceType.SICKLEAVE,
                LocalDate.of(2019, 1, 15) to AbsenceType.SICKLEAVE,
                LocalDate.of(2019, 1, 16) to AbsenceType.SICKLEAVE,
                LocalDate.of(2019, 1, 17) to AbsenceType.SICKLEAVE,
            )
        // then 10 planned absences
        val plannedAbsenceDays =
            listOf(
                    LocalDate.of(2019, 1, 18),
                    LocalDate.of(2019, 1, 21),
                    LocalDate.of(2019, 1, 22),
                    LocalDate.of(2019, 1, 23),
                    LocalDate.of(2019, 1, 24),
                    LocalDate.of(2019, 1, 25),
                    LocalDate.of(2019, 1, 28),
                    LocalDate.of(2019, 1, 29),
                    LocalDate.of(2019, 1, 30),
                    LocalDate.of(2019, 1, 31),
                )
                .map { it to AbsenceType.PLANNED_ABSENCE }

        initDataForAbsences(
            listOf(period),
            sickleaveDays + plannedAbsenceDays,
            serviceNeed = snDaycareContractDays10,
        )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(8700, invoice.totalPrice)
            assertEquals(3, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(14500, invoiceRow.unitPrice)
                assertEquals(14500, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(productProvider.contractSurplusDay, invoiceRow.product)
                assertEquals(2, invoiceRow.amount)
                assertEquals(1450, invoiceRow.unitPrice) // 14500 / 10
                assertEquals(2900, invoiceRow.price)
            }
            invoice.rows[2].let { invoiceRow ->
                assertEquals(productProvider.partMonthSickLeave, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-8700, invoiceRow.unitPrice)
                assertEquals(-8700, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days and 7 surplus days results in a monthly maximum invoice`() {
        // 22 operational days
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        // no planned absences
        initDataForAbsences(listOf(period), listOf(), serviceNeed = snDaycareContractDays15)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(28900, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(21700, invoiceRow.unitPrice)
                assertEquals(21700, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(productProvider.contractSurplusDay, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(7200, invoiceRow.unitPrice) // 28900 - 21700
                assertEquals(7200, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 13 contract days and 1 surplus day in preschool daycare results in a monthly maximum invoice no greater than the preschool daycare maximum (maxContractDaySurplusThreshold = 13)`() {
        db.transaction { it.insertServiceNeedOption(snPreschoolDaycareContractDays13) }
        // 22 operational days
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        // 14 attendance days
        // then 8 planned absences
        val plannedAbsenceDays =
            listOf(
                    LocalDate.of(2019, 1, 22),
                    LocalDate.of(2019, 1, 23),
                    LocalDate.of(2019, 1, 24),
                    LocalDate.of(2019, 1, 25),
                    LocalDate.of(2019, 1, 28),
                    LocalDate.of(2019, 1, 29),
                    LocalDate.of(2019, 1, 30),
                    LocalDate.of(2019, 1, 31),
                )
                .map { it to AbsenceType.PLANNED_ABSENCE }

        initDataForAbsences(
            listOf(period),
            absenceDays = plannedAbsenceDays,
            placementType = PlacementType.PRESCHOOL_DAYCARE,
            serviceNeed = snPreschoolDaycareContractDays13,
        )

        val featureConfig =
            featureConfig.copy(
                maxContractDaySurplusThreshold = 13,
                useContractDaysAsDailyFeeDivisor = false,
            )
        val generator =
            InvoiceGenerator(
                DraftInvoiceGenerator(
                    productProvider,
                    featureConfig,
                    DefaultInvoiceGenerationLogic,
                ),
                featureConfig,
            )
        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            // The *preschool daycare maximum* (23120) is invoiced, not the *daycare maximum*.
            assertEquals(
                (snDefaultPreschoolDaycare.feeCoefficient * BigDecimal(28900)).toInt(),
                invoice.totalPrice,
            )
            assertEquals(2, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(17300, invoiceRow.unitPrice)
                assertEquals(17300, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(productProvider.contractSurplusDay, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(5820, invoiceRow.unitPrice) // 23120 - 17300
                assertEquals(5820, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice capping to monthly preschool maximum takes sibling discount into account (maxContractDaySurplusThreshold = 13)`() {
        db.transaction { it.insertServiceNeedOption(snPreschoolDaycareContractDays13) }

        // 22 operational days
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        // 14 attendance days
        // then 8 planned absences
        val plannedAbsenceDays =
            listOf(
                    LocalDate.of(2019, 1, 22),
                    LocalDate.of(2019, 1, 23),
                    LocalDate.of(2019, 1, 24),
                    LocalDate.of(2019, 1, 25),
                    LocalDate.of(2019, 1, 28),
                    LocalDate.of(2019, 1, 29),
                    LocalDate.of(2019, 1, 30),
                    LocalDate.of(2019, 1, 31),
                )
                .map { it to AbsenceType.PLANNED_ABSENCE }

        val decision =
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.asDateRange(),
                testAdult_1.id,
                listOf(
                    createFeeDecisionChildFixture(
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        placementUnitId = testDaycare.id,
                        placementType = PlacementType.PRESCHOOL_DAYCARE,
                        serviceNeed = snPreschoolDaycareContractDays13.toFeeDecisionServiceNeed(),
                        baseFee = 28900,
                        siblingDiscount = 50,
                        fee = 8700, // 28900 * 0.6 * 0.5
                    )
                ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(listOf(decision))
        insertAbsences(testChild_1.id, plannedAbsenceDays)

        val featureConfig =
            featureConfig.copy(
                maxContractDaySurplusThreshold = 13,
                useContractDaysAsDailyFeeDivisor = false,
            )
        val generator =
            InvoiceGenerator(
                DraftInvoiceGenerator(
                    productProvider,
                    featureConfig,
                    DefaultInvoiceGenerationLogic,
                ),
                featureConfig,
            )
        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(11600, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(8700, invoiceRow.unitPrice)
                assertEquals(8700, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(productProvider.contractSurplusDay, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(2900, invoiceRow.unitPrice) // 11600 - 8700
                assertEquals(2900, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days and 7 surplus days with a sibling discount results in a monthly maximum invoice`() {
        // 22 operational days
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))

        val decision =
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.asDateRange(),
                testAdult_1.id,
                listOf(
                    createFeeDecisionChildFixture(
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        placementUnitId = testDaycare.id,
                        placementType = PlacementType.DAYCARE,
                        serviceNeed = snDaycareContractDays15.toFeeDecisionServiceNeed(),
                        baseFee = 28900,
                        siblingDiscount = 50,
                        fee = 10900,
                        feeAlterations = listOf(),
                    )
                ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(listOf(decision))

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(14500, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(10900, invoiceRow.unitPrice)
                assertEquals(10900, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(productProvider.contractSurplusDay, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(3600, invoiceRow.unitPrice) // 14500 - 10900
                assertEquals(3600, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days and 7 surplus days with a fee alteration results in a monthly maximum invoice`() {
        // 22 operational days
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))

        val decision =
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.asDateRange(),
                testAdult_1.id,
                listOf(
                    createFeeDecisionChildFixture(
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        placementUnitId = testDaycare.id,
                        placementType = PlacementType.DAYCARE,
                        serviceNeed = snDaycareContractDays15.toFeeDecisionServiceNeed(),
                        baseFee = 28900,
                        fee = 21700,
                        feeAlterations =
                            listOf(
                                createFeeDecisionAlterationFixture(
                                    amount = 50,
                                    isAbsolute = false,
                                    effect =
                                        feeAlterationEffect(
                                            21700,
                                            FeeAlterationType.DISCOUNT,
                                            50,
                                            false,
                                        ),
                                )
                            ),
                    )
                ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(listOf(decision))

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(14450, invoice.totalPrice)
            assertEquals(3, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(21700, invoiceRow.unitPrice)
                assertEquals(21700, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(
                    productProvider.mapToFeeAlterationProduct(
                        productProvider.mapToProduct(PlacementType.DAYCARE),
                        FeeAlterationType.DISCOUNT,
                    ),
                    invoiceRow.product,
                )
                assertEquals(1, invoiceRow.amount)
                assertEquals(-10850, invoiceRow.unitPrice)
                assertEquals(-10850, invoiceRow.price)
            }
            invoice.rows[2].let { invoiceRow ->
                assertEquals(productProvider.contractSurplusDay, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(3600, invoiceRow.unitPrice) // 14450 - 10850
                assertEquals(3600, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days and 1 surplus day with maximum contract surplus days of 16 results in a normal increase`() {
        // 22 operational days
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        // 15 operational days first
        // then planned absences
        val plannedAbsenceDays =
            listOf(
                    LocalDate.of(2019, 1, 23),
                    LocalDate.of(2019, 1, 24),
                    LocalDate.of(2019, 1, 25),
                    LocalDate.of(2019, 1, 28),
                    LocalDate.of(2019, 1, 29),
                    LocalDate.of(2019, 1, 30),
                )
                .map { it to AbsenceType.PLANNED_ABSENCE }
        // then 1 more operational day
        initDataForAbsences(
            listOf(period),
            plannedAbsenceDays,
            serviceNeed = snDaycareContractDays15,
        )

        // Override maxContractDaySurplusThreshold feature config
        val featureConfig = featureConfig.copy(maxContractDaySurplusThreshold = 16)
        val generator =
            InvoiceGenerator(
                DraftInvoiceGenerator(
                    productProvider,
                    featureConfig,
                    DefaultInvoiceGenerationLogic,
                ),
                featureConfig,
            )
        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(23147, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(21700, invoiceRow.unitPrice)
                assertEquals(21700, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(productProvider.contractSurplusDay, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(1447, invoiceRow.unitPrice) // 21700 / 15
                assertEquals(1447, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with 15 contract days and 2 surplus days with maximum contract surplus days of 16 results in a monthly maximum invoice`() {
        // 22 operational days
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        // 15 operational days first
        // then planned absences
        val plannedAbsenceDays =
            listOf(
                    LocalDate.of(2019, 1, 23),
                    LocalDate.of(2019, 1, 24),
                    LocalDate.of(2019, 1, 25),
                    LocalDate.of(2019, 1, 28),
                    LocalDate.of(2019, 1, 29),
                )
                .map { it to AbsenceType.PLANNED_ABSENCE }
        // then 2 more operational days
        initDataForAbsences(
            listOf(period),
            plannedAbsenceDays,
            serviceNeed = snDaycareContractDays15,
        )

        // Override maxContractDaySurplusThreshold feature config
        val featureConfig = featureConfig.copy(maxContractDaySurplusThreshold = 16)
        val generator =
            InvoiceGenerator(
                DraftInvoiceGenerator(
                    productProvider,
                    featureConfig,
                    DefaultInvoiceGenerationLogic,
                ),
                featureConfig,
            )
        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(28900, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(21700, invoiceRow.unitPrice)
                assertEquals(21700, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(productProvider.contractSurplusDay, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(7200, invoiceRow.unitPrice) // 28900 - 21700
                assertEquals(7200, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with part month fee decisions and surplus days results in a part month maximum invoice`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))

        // 20 operational days and no planned absences
        val decision =
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 29)),
                testAdult_1.id,
                listOf(
                    createFeeDecisionChildFixture(
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        placementUnitId = testDaycare.id,
                        placementType = PlacementType.DAYCARE,
                        serviceNeed = snDaycareContractDays15.toFeeDecisionServiceNeed(),
                        baseFee = 28900,
                        fee = 21700,
                        feeAlterations = listOf(),
                    )
                ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(listOf(decision))

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(28900, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(21700, invoiceRow.unitPrice)
                assertEquals(21700, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(productProvider.contractSurplusDay, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(7200, invoiceRow.unitPrice) // 28900 - 21700
                assertEquals(7200, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with part month fee decision ending early and surplus days results in a part month maximum invoice with (useContractDaysAsDailyFeeDivisor = false, maxContractDaySurplusThreshold = 16)`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))

        // 18 operational days and no planned absences
        val decision =
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 25)),
                testAdult_1.id,
                listOf(
                    createFeeDecisionChildFixture(
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        placementUnitId = testDaycare.id,
                        placementType = PlacementType.DAYCARE,
                        serviceNeed = snDaycareContractDays15.toFeeDecisionServiceNeed(),
                        baseFee = 28900,
                        fee = 21700,
                        feeAlterations = listOf(),
                    )
                ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(listOf(decision))

        // Override useContractDaysAsDailyFeeDivisor and maxContractDaySurplusThreshold feature
        // configs
        val featureConfig =
            featureConfig.copy(
                useContractDaysAsDailyFeeDivisor = false,
                maxContractDaySurplusThreshold = 16,
            )
        val generator =
            InvoiceGenerator(
                DraftInvoiceGenerator(
                    productProvider,
                    featureConfig,
                    DefaultInvoiceGenerationLogic,
                ),
                featureConfig,
            )
        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(23645, invoice.totalPrice) // 28900 * (18 / 22)
            assertEquals(2, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(18, invoiceRow.amount)
                assertEquals(986, invoiceRow.unitPrice) // 21700 / 22
                assertEquals(17748, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(productProvider.contractSurplusDay, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(5897, invoiceRow.unitPrice) // 23465 - 17748
                assertEquals(5897, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with part month fee decision starting late and surplus days results in a part month maximum invoice with (useContractDaysAsDailyFeeDivisor = false, maxContractDaySurplusThreshold = 16)`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))

        // 18 operational days and no planned absences
        val decision =
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                DateRange(LocalDate.of(2019, 1, 8), LocalDate.of(2019, 1, 31)),
                testAdult_1.id,
                listOf(
                    createFeeDecisionChildFixture(
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        placementUnitId = testDaycare.id,
                        placementType = PlacementType.DAYCARE,
                        serviceNeed = snDaycareContractDays15.toFeeDecisionServiceNeed(),
                        baseFee = 28900,
                        fee = 21700,
                        feeAlterations = listOf(),
                    )
                ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(listOf(decision))

        // Override useContractDaysAsDailyFeeDivisor and maxContractDaySurplusThreshold feature
        // configs
        val featureConfig =
            featureConfig.copy(
                useContractDaysAsDailyFeeDivisor = false,
                maxContractDaySurplusThreshold = 16,
            )
        val generator =
            InvoiceGenerator(
                DraftInvoiceGenerator(
                    productProvider,
                    featureConfig,
                    DefaultInvoiceGenerationLogic,
                ),
                featureConfig,
            )
        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(23645, invoice.totalPrice) // 28900 * (18 / 22)
            assertEquals(2, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(18, invoiceRow.amount)
                assertEquals(986, invoiceRow.unitPrice) // 21700 / 22
                assertEquals(17748, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(productProvider.contractSurplusDay, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(5897, invoiceRow.unitPrice) // 23465 - 17748
                assertEquals(5897, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation from two fee decisions with contract surplus days  (useContractDaysAsDailyFeeDivisor = false, maxContractDaySurplusThreshold = 16)`() {
        // 22 operational days
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))

        val decision =
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.asDateRange(),
                testAdult_1.id,
                listOf(
                    createFeeDecisionChildFixture(
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        placementUnitId = testDaycare.id,
                        placementType = PlacementType.DAYCARE,
                        serviceNeed = snDaycareContractDays15.toFeeDecisionServiceNeed(),
                        baseFee = 28900,
                        fee = 21700,
                    )
                ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(
            listOf(
                decision.copy(
                    validDuring = period.copy(end = LocalDate.of(2019, 1, 15)).asDateRange()
                ),
                decision.copy(
                    id = FeeDecisionId(UUID.randomUUID()),
                    validDuring = period.copy(start = LocalDate.of(2019, 1, 16)).asDateRange(),
                ),
            )
        )

        // Override useContractDaysAsDailyFeeDivisor and maxContractDaySurplusThreshold feature
        // configs
        val featureConfig =
            featureConfig.copy(
                useContractDaysAsDailyFeeDivisor = false,
                maxContractDaySurplusThreshold = 16,
            )
        val generator =
            InvoiceGenerator(
                DraftInvoiceGenerator(
                    productProvider,
                    featureConfig,
                    DefaultInvoiceGenerationLogic,
                ),
                featureConfig,
            )
        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(28900, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(21700, invoiceRow.unitPrice)
                assertEquals(21700, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(productProvider.contractSurplusDay, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(7200, invoiceRow.unitPrice) // 28900 - 21700
                assertEquals(7200, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation from two fee decisions with changing fees with contract surplus days (useContractDaysAsDailyFeeDivisor = false, maxContractDaySurplusThreshold = 16)`() {
        // 22 operational days
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))

        val decisions =
            listOf(
                createFeeDecisionFixture(
                    FeeDecisionStatus.SENT,
                    FeeDecisionType.NORMAL,
                    period.copy(end = LocalDate.of(2019, 1, 15)).asDateRange(),
                    testAdult_1.id,
                    listOf(
                        createFeeDecisionChildFixture(
                            childId = testChild_1.id,
                            dateOfBirth = testChild_1.dateOfBirth,
                            placementUnitId = testDaycare.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDaycareContractDays15.toFeeDecisionServiceNeed(),
                            baseFee = 28900,
                            fee = 21700,
                        )
                    ),
                ),
                createFeeDecisionFixture(
                    FeeDecisionStatus.SENT,
                    FeeDecisionType.NORMAL,
                    period.copy(start = LocalDate.of(2019, 1, 16)).asDateRange(),
                    testAdult_1.id,
                    listOf(
                        createFeeDecisionChildFixture(
                            childId = testChild_1.id,
                            dateOfBirth = testChild_1.dateOfBirth,
                            placementUnitId = testDaycare.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDaycareContractDays15.toFeeDecisionServiceNeed(),
                            baseFee = 28900,
                            siblingDiscount = 50,
                            fee = 10900,
                        )
                    ),
                ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(decisions)

        // Override useContractDaysAsDailyFeeDivisor and maxContractDaySurplusThreshold feature
        // configs
        val featureConfig =
            featureConfig.copy(
                useContractDaysAsDailyFeeDivisor = false,
                maxContractDaySurplusThreshold = 16,
            )
        val generator =
            InvoiceGenerator(
                DraftInvoiceGenerator(
                    productProvider,
                    featureConfig,
                    DefaultInvoiceGenerationLogic,
                ),
                featureConfig,
            )
        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(21045, invoice.totalPrice)
            assertEquals(3, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(10, invoiceRow.amount)
                assertEquals(986, invoiceRow.unitPrice) // 21700 / 22
                assertEquals(9860, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(12, invoiceRow.amount)
                assertEquals(495, invoiceRow.unitPrice) // 10900 / 22
                assertEquals(5940, invoiceRow.price)
            }
            invoice.rows[2].let { invoiceRow ->
                assertEquals(productProvider.contractSurplusDay, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(
                    5245,
                    invoiceRow.unitPrice,
                ) // (10 * 28900 + 12 * 14500) (= 21045) / 22 - 9860 - 5940
                assertEquals(5245, invoiceRow.price)
            }
        }
    }

    @Test
    fun `Force majeure and free absence types are free`() {
        // 22 operational days
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        // 1 force majeure absence
        val forceMajeure = listOf(LocalDate.of(2019, 1, 16) to AbsenceType.FORCE_MAJEURE)

        // 1 free absence
        val free = listOf(LocalDate.of(2019, 1, 17) to AbsenceType.FREE_ABSENCE)

        initDataForAbsences(listOf(period), forceMajeure + free)

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(26272, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(productProvider.dailyRefund, invoiceRow.product)
                assertEquals(2, invoiceRow.amount)
                assertEquals(-1314, invoiceRow.unitPrice) // 28900 / 22
                assertEquals(-2628, invoiceRow.price)
            }
        }
    }

    @Test
    fun `Free absence type is treated as other absence if feature is disabled`() {
        // 22 operational days
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        // free absences for the whole month
        val free = datesBetween(period.start, period.end).map { it to AbsenceType.FREE_ABSENCE }
        initDataForAbsences(listOf(period), free)

        // Override freeAbsenceGivesADailyRefund feature config
        val featureConfig = featureConfig.copy(freeAbsenceGivesADailyRefund = false)
        val generator =
            InvoiceGenerator(
                DraftInvoiceGenerator(
                    productProvider,
                    featureConfig,
                    DefaultInvoiceGenerationLogic,
                ),
                featureConfig,
            )
        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(14450, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(-14450, invoiceRow.unitPrice)
                assertEquals(-14450, invoiceRow.price)
            }
        }
    }

    @Test
    fun `Free absence type is does not generate surplus days`() {
        // 22 operational days
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        // free absences for the whole month
        val free = datesBetween(period.start, period.end).map { it to AbsenceType.FREE_ABSENCE }
        initDataForAbsences(listOf(period), free, serviceNeed = snDaycareContractDays15)

        // Override freeAbsenceGivesADailyRefund feature config
        val featureConfig = featureConfig.copy(freeAbsenceGivesADailyRefund = false)
        val generator =
            InvoiceGenerator(
                DraftInvoiceGenerator(
                    productProvider,
                    featureConfig,
                    DefaultInvoiceGenerationLogic,
                ),
                featureConfig,
            )
        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(10850, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(21700, invoiceRow.price)
                assertEquals(21700, invoiceRow.unitPrice)
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(-10850, invoiceRow.unitPrice)
                assertEquals(-10850, invoiceRow.price)
            }
        }
    }

    @Test
    fun `invoice generation with Free logic`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)

        initDataForAbsences(listOf(period), listOf())

        val generator =
            InvoiceGenerator(
                DraftInvoiceGenerator(
                    productProvider,
                    featureConfig,
                    object : InvoiceGenerationLogicChooser {
                        override fun logicForMonth(
                            tx: Database.Read,
                            year: Int,
                            month: Month,
                            childId: ChildId,
                        ) = InvoiceGenerationLogic.Free
                    },
                ),
                featureConfig,
            )

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)
        assertEquals(0, result.size)
    }

    @Test
    fun `family with children split between two fridge parents is invoiced as one`() {
        val month = YearMonth.of(2019, 1)
        val period = FiniteDateRange.ofMonth(month)
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        db.transaction(insertChildParentRelation(testAdult_2.id, testChild_2.id, period))
        val decision =
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.asDateRange(),
                headOfFamilyId = testAdult_1.id,
                partnerId = testAdult_2.id,
                children =
                    listOf(
                        createFeeDecisionChildFixture(
                            childId = testChild_1.id,
                            dateOfBirth = testChild_1.dateOfBirth,
                            placementUnitId = testDaycare.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                            baseFee = 28900,
                            fee = 28900,
                        ),
                        createFeeDecisionChildFixture(
                            childId = testChild_2.id,
                            dateOfBirth = testChild_2.dateOfBirth,
                            placementUnitId = testDaycare.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                            baseFee = 28900,
                            siblingDiscount = 50,
                            fee = 14500,
                        ),
                    ),
            )
        insertDecisionsAndPlacementsAndServiceNeeds(listOf(decision))

        db.transaction { generator.createAndStoreAllDraftInvoices(it, month) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily)
            assertEquals(43400, invoice.totalPrice)
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.DAYCARE),
                    invoiceRow.product,
                )
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price)
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_2.id, invoiceRow.child)
                assertEquals(
                    productProvider.mapToProduct(PlacementType.DAYCARE),
                    invoiceRow.product,
                )
                assertEquals(1, invoiceRow.amount)
                assertEquals(14500, invoiceRow.unitPrice)
                assertEquals(14500, invoiceRow.price)
            }
        }
    }

    private fun initByPeriodAndPlacementType(
        period: FiniteDateRange,
        placementType: PlacementType,
        serviceNeedOption: ServiceNeedOption? = null,
        feeAlterations: List<FeeAlterationWithEffect> = listOf(),
        children: List<DevPerson> = listOf(testChild_1),
        partner: PersonId? = null,
    ) {
        children.forEach { child ->
            db.transaction(insertChildParentRelation(testAdult_1.id, child.id, period))
        }
        val serviceNeed = (serviceNeedOption ?: snDaycareFullDay35).toFeeDecisionServiceNeed()
        val decision =
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                period.asDateRange(),
                testAdult_1.id,
                children.map { child ->
                    createFeeDecisionChildFixture(
                        childId = child.id,
                        dateOfBirth = child.dateOfBirth,
                        placementUnitId = testDaycare.id,
                        placementType = placementType,
                        serviceNeed = serviceNeed,
                        baseFee = 28900,
                        fee = roundToEuros(serviceNeed.feeCoefficient * BigDecimal(28900)).toInt(),
                        feeAlterations = feeAlterations,
                    )
                },
                partnerId = partner,
            )
        insertDecisionsAndPlacementsAndServiceNeeds(
            listOf(
                decision.copy(
                    validDuring = decision.validDuring.copy(end = period.start.plusDays(7))
                ),
                decision.copy(
                    id = FeeDecisionId(UUID.randomUUID()),
                    validDuring = decision.validDuring.copy(start = period.start.plusDays(8)),
                ),
            )
        )
    }

    private fun initFreeJulyTestData(
        invoicingMonth: YearMonth,
        placementPeriods: List<FiniteDateRange>,
        placementType: PlacementType = PlacementType.DAYCARE,
        freeJulyStartOnSeptember: Boolean = false,
    ) =
        initFreeJulyTestData(
            invoicingMonth,
            placementPeriods.map { placementType to it },
            freeJulyStartOnSeptember,
        )

    private fun initFreeJulyTestData(
        invoicingMonth: YearMonth,
        placementPeriods: List<Pair<PlacementType, FiniteDateRange>>,
        freeJulyStartOnSeptember: Boolean = false,
    ) {
        val invoicingPeriod = FiniteDateRange.ofMonth(invoicingMonth)
        val decision =
            createFeeDecisionFixture(
                FeeDecisionStatus.SENT,
                FeeDecisionType.NORMAL,
                invoicingPeriod.asDateRange(),
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
                    )
                ),
            )
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, invoicingPeriod))
        db.transaction { tx ->
            tx.upsertFeeDecisions(
                listOf(
                    decision.copy(
                        validDuring =
                            decision.validDuring.copy(end = invoicingPeriod.start.plusDays(7))
                    )
                )
            )
        }

        placementPeriods.forEach { (type, period) ->
            db.transaction(insertPlacement(testChild_1.id, period, type))
        }

        val generator =
            InvoiceGenerator(
                draftInvoiceGenerator,
                featureConfig.copy(freeJulyStartOnSeptember = freeJulyStartOnSeptember),
            )
        db.transaction { generator.createAndStoreAllDraftInvoices(it, invoicingMonth) }
    }

    private fun initDataForAbsences(
        periods: List<FiniteDateRange>,
        absenceDays: List<Pair<LocalDate, AbsenceType>>,
        child: DevPerson = testChild_1,
        placementType: PlacementType = PlacementType.DAYCARE,
        serviceNeed: ServiceNeedOption = snDaycareFullDay35,
        shiftCare: ShiftCareType = ShiftCareType.NONE,
    ) {
        periods.forEachIndexed { index, period ->
            val decision =
                createFeeDecisionFixture(
                    FeeDecisionStatus.SENT,
                    FeeDecisionType.NORMAL,
                    period.asDateRange(),
                    testAdult_1.id,
                    listOf(
                        createFeeDecisionChildFixture(
                            childId = child.id,
                            dateOfBirth = child.dateOfBirth,
                            placementUnitId = testDaycare.id,
                            placementType = placementType,
                            serviceNeed = serviceNeed.toFeeDecisionServiceNeed(),
                            baseFee = 28900,
                            fee =
                                roundToEuros(serviceNeed.feeCoefficient * BigDecimal(28900))
                                    .toInt(),
                            feeAlterations =
                                if (index > 0) {
                                    listOf(
                                        createFeeDecisionAlterationFixture(
                                            amount = -100,
                                            isAbsolute = true,
                                            effect = -10000,
                                        )
                                    )
                                } else {
                                    listOf()
                                },
                        )
                    ),
                )

            db.transaction { tx ->
                insertChildParentRelation(testAdult_1.id, child.id, period)(tx)
                tx.upsertFeeDecisions(listOf(decision))
                val placementId = insertPlacement(child.id, period)(tx)
                tx.insert(
                    DevServiceNeed(
                        placementId = placementId,
                        startDate = period.start,
                        endDate = period.end,
                        optionId = serviceNeed.id,
                        shiftCare = shiftCare,
                        confirmedBy = EvakaUserId(testDecisionMaker_1.id.raw),
                    )
                )
                val groupId = tx.insert(DevDaycareGroup(daycareId = testDaycare.id))
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycarePlacementId = placementId,
                        daycareGroupId = groupId,
                        startDate = period.start,
                        endDate = period.end,
                    )
                )
            }
        }
        if (absenceDays.isNotEmpty()) {
            insertAbsences(child.id, absenceDays)
        }
    }

    private fun insertAbsences(childId: ChildId, absenceDays: List<Pair<LocalDate, AbsenceType>>) {
        db.transaction { tx ->
            tx.insertAbsences(
                HelsinkiDateTime.now(),
                EvakaUserId(testDecisionMaker_1.id.raw),
                absenceDays.map { (date, type) ->
                    AbsenceUpsert(
                        absenceType = type,
                        childId = childId,
                        date = date,
                        category = AbsenceCategory.BILLABLE,
                    )
                },
            )
        }
    }

    private fun insertDecisionsAndPlacementsAndServiceNeeds(
        feeDecisions: List<FeeDecision>,
        shiftCare: ShiftCareType = ShiftCareType.NONE,
    ) =
        db.transaction { tx ->
            tx.upsertFeeDecisions(feeDecisions)
            feeDecisions.forEach { decision ->
                decision.children.forEach { part ->
                    tx.insert(
                            DevPlacement(
                                type = part.placement.type,
                                childId = part.child.id,
                                unitId = part.placement.unitId,
                                startDate = decision.validFrom,
                                endDate = decision.validTo!!,
                            )
                        )
                        .also { placementId ->
                            if (part.serviceNeed.optionId != null && !part.serviceNeed.missing) {
                                tx.insert(
                                    DevServiceNeed(
                                        placementId = placementId,
                                        startDate = decision.validFrom,
                                        endDate = decision.validTo!!,
                                        optionId = part.serviceNeed.optionId!!,
                                        shiftCare = shiftCare,
                                        confirmedBy = EvakaUserId(testDecisionMaker_1.id.raw),
                                    )
                                )
                            }
                        }
                }
            }
        }

    private fun insertPlacement(
        childId: ChildId,
        period: FiniteDateRange,
        type: PlacementType = PlacementType.DAYCARE,
    ) = { tx: Database.Transaction ->
        tx.insert(
            DevPlacement(
                type = type,
                childId = childId,
                unitId = testDaycare.id,
                startDate = period.start,
                endDate = period.end,
            )
        )
    }

    private fun insertChildParentRelation(
        headOfFamilyId: PersonId,
        childId: ChildId,
        period: FiniteDateRange,
    ) = { tx: Database.Transaction ->
        tx.insert(
            DevParentship(
                childId = childId,
                headOfChildId = headOfFamilyId,
                startDate = period.start,
                endDate = period.end,
            )
        )
    }

    private val getAllInvoices: (Database.Read) -> List<Invoice> = { r ->
        r.createQuery { sql("$invoiceQueryBase ORDER BY invoice.id, row.idx") }
            .toList(Row::toInvoice)
            .let(::flatten)
            .shuffled() // randomize order to expose assumptions
    }

    private fun datesBetween(start: LocalDate, endInclusive: LocalDate?): List<LocalDate> {
        return generateSequence(start) { it.plusDays(1) }.takeWhile { it <= endInclusive }.toList()
    }
}
