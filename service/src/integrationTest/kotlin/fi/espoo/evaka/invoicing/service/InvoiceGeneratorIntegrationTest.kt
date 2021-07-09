// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.service.Absence
import fi.espoo.evaka.daycare.service.AbsenceService
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.daycare.service.CareType
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.createFeeDecisionAlterationFixture
import fi.espoo.evaka.invoicing.createFeeDecisionChildFixture
import fi.espoo.evaka.invoicing.createFeeDecisionFixture
import fi.espoo.evaka.invoicing.data.flatten
import fi.espoo.evaka.invoicing.data.invoiceQueryBase
import fi.espoo.evaka.invoicing.data.toInvoice
import fi.espoo.evaka.invoicing.data.upsertFeeDecisions
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.Invoice
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.invoicing.domain.Product
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestParentship
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.DateRange
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
import org.springframework.beans.factory.annotation.Autowired
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class InvoiceGeneratorIntegrationTest : FullApplicationTest() {
    @Autowired
    private lateinit var absenceService: AbsenceService

    private val areaCode = 200
    private val costCenter = "31500"
    private val subCostCenter = "00"

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
    fun `invoice generation for child with a day long temporary placement`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 2))
        db.transaction(insertPlacement(testChild_1.id, placementPeriod, PlacementType.TEMPORARY_DAYCARE))

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(areaCode, invoice.agreementType)
            assertEquals(2900, invoice.totalPrice())
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(costCenter, invoiceRow.costCenter)
                assertEquals(subCostCenter, invoiceRow.subCostCenter)
                assertEquals(Product.TEMPORARY_CARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(2900, invoiceRow.unitPrice)
                assertEquals(2900, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation for child with one day long part day temporary placement`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 2))
        db.transaction(insertPlacement(testChild_1.id, placementPeriod, PlacementType.TEMPORARY_DAYCARE_PART_DAY))

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(1500, invoice.totalPrice())
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.TEMPORARY_CARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(1500, invoiceRow.unitPrice)
                assertEquals(1500, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation for child with a three day long temporary placement`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        db.transaction(insertChildParentRelation(testAdult_1.id, testChild_1.id, period))
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 4))
        db.transaction(insertPlacement(testChild_1.id, placementPeriod, PlacementType.TEMPORARY_DAYCARE))

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(8700, invoice.totalPrice())
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.TEMPORARY_CARE, invoiceRow.product)
                assertEquals(3, invoiceRow.amount)
                assertEquals(2900, invoiceRow.unitPrice)
                assertEquals(8700, invoiceRow.price())
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

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(7300, invoice.totalPrice())
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.TEMPORARY_CARE, invoiceRow.product)
                assertEquals(2, invoiceRow.amount)
                assertEquals(2900, invoiceRow.unitPrice)
                assertEquals(5800, invoiceRow.price())
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.TEMPORARY_CARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(1500, invoiceRow.unitPrice)
                assertEquals(1500, invoiceRow.price())
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

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(4400, invoice.totalPrice())
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.TEMPORARY_CARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(2900, invoiceRow.unitPrice)
                assertEquals(2900, invoiceRow.price())
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_2.id, invoiceRow.child.id)
                assertEquals(Product.TEMPORARY_CARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(1500, invoiceRow.unitPrice)
                assertEquals(1500, invoiceRow.price())
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

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(2300, invoice.totalPrice())
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.TEMPORARY_CARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(1500, invoiceRow.unitPrice)
                assertEquals(1500, invoiceRow.price())
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_2.id, invoiceRow.child.id)
                assertEquals(Product.TEMPORARY_CARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(800, invoiceRow.unitPrice)
                assertEquals(800, invoiceRow.price())
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

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(2, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(2900, invoice.totalPrice())
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.TEMPORARY_CARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(2900, invoiceRow.unitPrice)
                assertEquals(2900, invoiceRow.price())
            }
        }
        result.last().let { invoice ->
            assertEquals(testAdult_2.id, invoice.headOfFamily.id)
            assertEquals(2900, invoice.totalPrice())
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.TEMPORARY_CARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(2900, invoiceRow.unitPrice)
                assertEquals(2900, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation for child with a day long temporary placement that has no family configured`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        val placementPeriod = DateRange(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 2))
        db.transaction(insertPlacement(testChild_1.id, placementPeriod, PlacementType.TEMPORARY_DAYCARE))

        db.transaction { it.createAllDraftInvoices(period) }

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

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(5800, invoice.totalPrice())
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.TEMPORARY_CARE, invoiceRow.product)
                assertEquals(2, invoiceRow.amount)
                assertEquals(2900, invoiceRow.unitPrice)
                assertEquals(5800, invoiceRow.price())
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
                decision.copy(id = FeeDecisionId(UUID.randomUUID()), validDuring = decision.validDuring.copy(start = period.start.plusDays(8)))
            )
        )

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(28900, invoice.totalPrice())
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
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
                decision.copy(id = FeeDecisionId(UUID.randomUUID()), validDuring = decision.validDuring.copy(start = period.start.plusDays(8)))
            )
        )

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(23120, invoice.totalPrice())
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE_DISCOUNT, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-5780, invoiceRow.unitPrice)
                assertEquals(-5780, invoiceRow.price())
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

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(40103, invoice.totalPrice())
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_2.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(17, invoiceRow.amount)
                assertEquals(659, invoiceRow.unitPrice)
                assertEquals(11203, invoiceRow.price())
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

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(28900, invoice.totalPrice())
            assertEquals(3, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(4, invoiceRow.amount)
                assertEquals(1314, invoiceRow.unitPrice)
                assertEquals(5256, invoiceRow.price())
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(18, invoiceRow.amount)
                assertEquals(1314, invoiceRow.unitPrice)
                assertEquals(23652, invoiceRow.price())
            }
            invoice.rows[2].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-8, invoiceRow.unitPrice)
                assertEquals(-8, invoiceRow.price())
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

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(28900, invoice.totalPrice())
            assertEquals(3, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(5, invoiceRow.amount)
                assertEquals(1376, invoiceRow.unitPrice)
                assertEquals(6880, invoiceRow.price())
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(16, invoiceRow.amount)
                assertEquals(1376, invoiceRow.unitPrice)
                assertEquals(22016, invoiceRow.price())
            }
            invoice.rows[2].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(4, invoiceRow.unitPrice)
                assertEquals(4, invoiceRow.price())
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

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(28900, invoice.totalPrice())
            assertEquals(2, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(5, invoiceRow.amount)
                assertEquals(1445, invoiceRow.unitPrice)
                assertEquals(7225, invoiceRow.price())
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(15, invoiceRow.amount)
                assertEquals(1445, invoiceRow.unitPrice)
                assertEquals(21675, invoiceRow.price())
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

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(39930, invoice.totalPrice())
            assertEquals(3, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(testChild_2.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(5, invoiceRow.amount)
                assertEquals(659, invoiceRow.unitPrice)
                assertEquals(3295, invoiceRow.price())
            }
            invoice.rows[2].let { invoiceRow ->
                assertEquals(testChild_2.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(17, invoiceRow.amount)
                assertEquals(455, invoiceRow.unitPrice)
                assertEquals(7735, invoiceRow.price())
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

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(23120, invoice.totalPrice())
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE_DISCOUNT, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-5780, invoiceRow.unitPrice)
                assertEquals(-5780, invoiceRow.price())
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

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(32420, invoice.totalPrice())
            assertEquals(3, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE_DISCOUNT, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-5780, invoiceRow.unitPrice)
                assertEquals(-5780, invoiceRow.price())
            }

            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE_INCREASE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(9300, invoiceRow.unitPrice)
                assertEquals(9300, invoiceRow.price())
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

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(1445, invoice.totalPrice())
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE_DISCOUNT, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-27455, invoiceRow.unitPrice)
                assertEquals(-27455, invoiceRow.price())
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
                listOf(decision, decision.copy(id = FeeDecisionId(UUID.randomUUID()), headOfFamily = PersonData.JustId(testAdult_2.id)))
            )
        }

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(28900, invoice.totalPrice())
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
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
                listOf(decision, decision.copy(id = FeeDecisionId(UUID.randomUUID()), headOfFamily = PersonData.JustId(testAdult_2.id)))
            )
        }

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(testAdult_1.id, invoice.headOfFamily.id)
            assertEquals(6570, invoice.totalPrice())
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(testChild_1.id, invoiceRow.child.id)
                assertEquals(placementPeriod.start, invoiceRow.periodStart)
                assertEquals(placementPeriod.end, invoiceRow.periodEnd)
                assertEquals(Product.DAYCARE, invoiceRow.product)
                assertEquals(5, invoiceRow.amount)
                assertEquals(1314, invoiceRow.unitPrice)
                assertEquals(6570, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation with sick leave absences covering period`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val absenceDays = generateSequence(period.start) { it.plusDays(1) }
            .takeWhile { it <= period.end }
            .map { it to AbsenceType.SICKLEAVE }
            .toMap()

        initDataForAbsences(listOf(period), absenceDays)

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(0, invoice.totalPrice())
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(Product.SICK_LEAVE_100, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-28900, invoiceRow.unitPrice)
                assertEquals(-28900, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation with a lot sick leave absences`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val absenceDays = generateSequence(period.start) { it.plusDays(1) }
            .takeWhile { it <= period.end }
            .filter { it.dayOfWeek != DayOfWeek.MONDAY }
            .map { it to AbsenceType.SICKLEAVE }
            .toMap()

        initDataForAbsences(listOf(period), absenceDays)

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(14450, invoice.totalPrice())
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(Product.SICK_LEAVE_50, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-14450, invoiceRow.unitPrice)
                assertEquals(-14450, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation with some unknown absences`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val absenceDays = generateSequence(period.start) { it.plusDays(1) }
            .takeWhile { it <= period.end }
            .filter { it.dayOfWeek != DayOfWeek.MONDAY }
            .map { it to AbsenceType.UNKNOWN_ABSENCE }
            .toMap()

        initDataForAbsences(listOf(period), absenceDays)

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(28900, invoice.totalPrice())
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation with unknown absences covering period`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val absenceDays = generateSequence(period.start) { it.plusDays(1) }
            .takeWhile { it <= period.end }
            .map { it to AbsenceType.UNKNOWN_ABSENCE }
            .toMap()

        initDataForAbsences(listOf(period), absenceDays)

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(14450, invoice.totalPrice())
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(Product.ABSENCE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-14450, invoiceRow.unitPrice)
                assertEquals(-14450, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation with some parentleave absences`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val absenceDays = generateSequence(period.start) { it.plusDays(1) }
            .takeWhile { it <= LocalDate.of(2019, 1, 4) }
            .map { it to AbsenceType.PARENTLEAVE }
            .toMap()

        initDataForAbsences(listOf(period), absenceDays)

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(24958, invoice.totalPrice())
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(Product.FREE_OF_CHARGE, invoiceRow.product)
                assertEquals(3, invoiceRow.amount)
                assertEquals(-1314, invoiceRow.unitPrice)
                assertEquals(-3942, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation with some parentleave and sickleave absences`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val absenceDays = generateSequence(period.start) { it.plusDays(1) }
            .takeWhile { it <= LocalDate.of(2019, 1, 4) }
            .map { it to AbsenceType.PARENTLEAVE }
            .plus(
                generateSequence(LocalDate.of(2019, 1, 10)) { it.plusDays(1) }
                    .takeWhile { it <= LocalDate.of(2019, 1, 31) }
                    .map { it to AbsenceType.SICKLEAVE }
            )
            .toMap()

        initDataForAbsences(listOf(period), absenceDays)

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(12479, invoice.totalPrice())
            assertEquals(3, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(Product.FREE_OF_CHARGE, invoiceRow.product)
                assertEquals(3, invoiceRow.amount)
                assertEquals(-1314, invoiceRow.unitPrice)
                assertEquals(-3942, invoiceRow.price())
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(Product.SICK_LEAVE_50, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-12479, invoiceRow.unitPrice)
                assertEquals(-12479, invoiceRow.price())
            }
        }
    }

    @Test
    fun `planned absences do not generate a discount on invoices`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val absenceDays = generateSequence(period.start) { it.plusDays(1) }
            .takeWhile { it <= period.end }
            .map { it to AbsenceType.PLANNED_ABSENCE }
            .toMap()

        initDataForAbsences(listOf(period), absenceDays)

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(28900, invoice.totalPrice())
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation with some parentleave absences for a too old child`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val absenceDays = generateSequence(period.start) { it.plusDays(1) }
            .takeWhile { it <= LocalDate.of(2019, 1, 4) }
            .map { it to AbsenceType.PARENTLEAVE }
            .toMap()

        initDataForAbsences(listOf(period), absenceDays, child = testChild_2)

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(28900, invoice.totalPrice())
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation with 2 decisions plus sick leave absences`() {
        val periods = listOf(
            DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 11)),
            DateRange(LocalDate.of(2019, 1, 12), LocalDate.of(2019, 1, 31))
        )

        val absenceDays = generateSequence(LocalDate.of(2019, 1, 1)) { it.plusDays(1) }
            .takeWhile { it <= LocalDate.of(2019, 1, 31) }
            .filter { it.dayOfWeek != DayOfWeek.MONDAY }
            .map { it to AbsenceType.SICKLEAVE }
            .toMap()

        initDataForAbsences(periods, absenceDays)

        db.transaction {
            it.createAllDraftInvoices(
                DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
            )
        }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(11262, invoice.totalPrice())
            assertEquals(5, invoice.rows.size)

            invoice.rows[0].let { invoiceRow ->
                assertEquals(8, invoiceRow.amount)
                assertEquals(1314, invoiceRow.unitPrice)
                assertEquals(10512, invoiceRow.price())
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(Product.SICK_LEAVE_50, invoiceRow.product)
                assertEquals(8, invoiceRow.amount)
                assertEquals(-657, invoiceRow.unitPrice)
                assertEquals(-5256, invoiceRow.price())
            }

            invoice.rows[2].let { invoiceRow ->
                assertEquals(14, invoiceRow.amount)
                assertEquals(1314, invoiceRow.unitPrice)
                assertEquals(18396, invoiceRow.price())
            }
            invoice.rows[3].let { invoiceRow ->
                assertEquals(Product.DAYCARE_DISCOUNT, invoiceRow.product)
                assertEquals(14, invoiceRow.amount)
                assertEquals(-455, invoiceRow.unitPrice)
                assertEquals(-6370, invoiceRow.price())
            }
            invoice.rows[4].let { invoiceRow ->
                assertEquals(Product.SICK_LEAVE_50, invoiceRow.product)
                assertEquals(14, invoiceRow.amount)
                assertEquals(-430, invoiceRow.unitPrice)
                assertEquals(-6020, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation with 2 decisions plus parent leave and force majeure absences`() {
        val periods = listOf(
            DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 11)),
            DateRange(LocalDate.of(2019, 1, 12), LocalDate.of(2019, 1, 31))
        )

        val absenceDays = mapOf(
            LocalDate.of(2019, 1, 7) to AbsenceType.PARENTLEAVE,
            LocalDate.of(2019, 1, 14) to AbsenceType.PARENTLEAVE,
            LocalDate.of(2019, 1, 21) to AbsenceType.FORCE_MAJEURE,
            LocalDate.of(2019, 1, 28) to AbsenceType.FORCE_MAJEURE
        )

        initDataForAbsences(periods, absenceDays)

        db.transaction {
            it.createAllDraftInvoices(
                DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
            )
        }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(18647, invoice.totalPrice())
            assertEquals(5, invoice.rows.size)

            invoice.rows[0].let { invoiceRow ->
                assertEquals(8, invoiceRow.amount)
                assertEquals(1314, invoiceRow.unitPrice)
                assertEquals(10512, invoiceRow.price())
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(Product.FREE_OF_CHARGE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-1314, invoiceRow.unitPrice)
                assertEquals(-1314, invoiceRow.price())
            }

            invoice.rows[2].let { invoiceRow ->
                assertEquals(14, invoiceRow.amount)
                assertEquals(1314, invoiceRow.unitPrice)
                assertEquals(18396, invoiceRow.price())
            }
            invoice.rows[3].let { invoiceRow ->
                assertEquals(Product.DAYCARE_DISCOUNT, invoiceRow.product)
                assertEquals(14, invoiceRow.amount)
                assertEquals(-455, invoiceRow.unitPrice)
                assertEquals(-6370, invoiceRow.price())
            }
            invoice.rows[4].let { invoiceRow ->
                assertEquals(Product.FREE_OF_CHARGE, invoiceRow.product)
                assertEquals(3, invoiceRow.amount)
                assertEquals(-859, invoiceRow.unitPrice)
                assertEquals(-2577, invoiceRow.price())
            }
        }
    }

    @Test
    fun `invoice generation with full month of force majeure absences`() {
        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val absenceDays = generateSequence(period.start) { it.plusDays(1) }
            .takeWhile { it <= LocalDate.of(2019, 1, 31) }
            .map { it to AbsenceType.FORCE_MAJEURE }
            .toMap()

        initDataForAbsences(listOf(period), absenceDays)

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(1, result.size)

        result.first().let { invoice ->
            assertEquals(0, invoice.totalPrice())
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(Product.FREE_OF_CHARGE, invoiceRow.product)
                assertEquals(1, invoiceRow.amount)
                assertEquals(-28900, invoiceRow.unitPrice)
                assertEquals(-28900, invoiceRow.price())
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

        db.transaction { it.createAllDraftInvoices(period) }

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
                    placementUnitId = testRoundTheClockDaycare.id!!,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                    baseFee = 28900,
                    fee = 28900,
                    feeAlterations = listOf()
                )
            )
        )
        insertDecisionsAndPlacements(listOf(decision))

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(15210, invoice.totalPrice())
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(10, invoiceRow.amount)
                assertEquals(1521, invoiceRow.unitPrice)
                assertEquals(15210, invoiceRow.price())
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
                    placementUnitId = testRoundTheClockDaycare.id!!,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                    baseFee = 28900,
                    fee = 28900,
                    feeAlterations = listOf()
                )
            )
        )
        insertDecisionsAndPlacements(listOf(decision))

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(3042, invoice.totalPrice())
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(2, invoiceRow.amount)
                assertEquals(1521, invoiceRow.unitPrice)
                assertEquals(3042, invoiceRow.price())
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
                    placementUnitId = testRoundTheClockDaycare.id!!,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                    baseFee = 28900,
                    fee = 28900,
                    feeAlterations = listOf()
                )
            )
        )
        insertDecisionsAndPlacements(listOf(decision))

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(28900, invoice.totalPrice())
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
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
                    placementUnitId = testRoundTheClockDaycare.id!!,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                    baseFee = 28900,
                    fee = 28900,
                    feeAlterations = listOf()
                )
            )
        )
        insertDecisionsAndPlacements(listOf(decision))

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(28900, invoice.totalPrice())
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
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
                    placementUnitId = testRoundTheClockDaycare.id!!,
                    placementType = PlacementType.DAYCARE,
                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                    baseFee = 28900,
                    fee = 28900,
                    feeAlterations = listOf()
                )
            )
        )
        insertDecisionsAndPlacements(listOf(decision))

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(6084, invoice.totalPrice())
            assertEquals(1, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(4, invoiceRow.amount)
                assertEquals(1521, invoiceRow.unitPrice)
                assertEquals(6084, invoiceRow.price())
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
                period.copy(end = period.start.plusDays(13)),
                testAdult_1.id,
                listOf(
                    createFeeDecisionChildFixture(
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        placementUnitId = testRoundTheClockDaycare.id!!,
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
                period.copy(start = period.start.plusDays(14)),
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

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(28900, invoice.totalPrice())
            assertEquals(3, invoice.rows.size)
            invoice.rows[0].let { invoiceRow ->
                assertEquals(8, invoiceRow.amount)
                assertEquals(1521, invoiceRow.unitPrice)
                assertEquals(12168, invoiceRow.price())
            }
            invoice.rows[1].let { invoiceRow ->
                assertEquals(11, invoiceRow.amount)
                assertEquals(1521, invoiceRow.unitPrice)
                assertEquals(16731, invoiceRow.price())
            }
            invoice.rows[2].let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(1, invoiceRow.unitPrice)
                assertEquals(1, invoiceRow.price())
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
                    placementUnitId = testRoundTheClockDaycare.id!!,
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
            absenceService.upsertAbsences(
                tx,
                listOf(
                    Absence(
                        absenceType = AbsenceType.FORCE_MAJEURE,
                        childId = testChild_1.id,
                        date = LocalDate.of(2021, 1, 5),
                        careType = CareType.DAYCARE
                    )
                ),
                testDecisionMaker_1.id
            )
        }

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(27379, invoice.totalPrice())
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(-1521, invoiceRow.unitPrice)
                assertEquals(-1521, invoiceRow.price())
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
                    placementUnitId = testRoundTheClockDaycare.id!!,
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
            absenceService.upsertAbsences(
                tx,
                listOf(
                    Absence(
                        absenceType = AbsenceType.FORCE_MAJEURE,
                        childId = testChild_1.id,
                        date = LocalDate.of(2021, 1, 3),
                        careType = CareType.DAYCARE
                    )
                ),
                testDecisionMaker_1.id
            )
        }

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(27379, invoice.totalPrice())
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(-1521, invoiceRow.unitPrice)
                assertEquals(-1521, invoiceRow.price())
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
                    placementUnitId = testRoundTheClockDaycare.id!!,
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
            absenceService.upsertAbsences(
                tx,
                generateSequence(period.start) { it.plusDays(1) }
                    .takeWhile { it <= period.end }
                    .map {
                        Absence(
                            absenceType = AbsenceType.FORCE_MAJEURE,
                            childId = testChild_1.id,
                            date = it,
                            careType = CareType.DAYCARE
                        )
                    }
                    .toList(),
                testDecisionMaker_1.id
            )
        }

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)
        assertEquals(1, result.size)
        result.first().let { invoice ->
            assertEquals(0, invoice.totalPrice())
            assertEquals(2, invoice.rows.size)
            invoice.rows.first().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(28900, invoiceRow.unitPrice)
                assertEquals(28900, invoiceRow.price())
            }
            invoice.rows.last().let { invoiceRow ->
                assertEquals(1, invoiceRow.amount)
                assertEquals(-28900, invoiceRow.unitPrice)
                assertEquals(-28900, invoiceRow.price())
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

        db.transaction { it.createAllDraftInvoices(period) }

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
        assertFalse(PlacementType.PRESCHOOL.isInvoiceable())

        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        initByPeriodAndPlacementType(period, PlacementType.PRESCHOOL)

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(0, result.size)
    }

    @Test
    fun `plain preparatory is not invoiced`() {
        assertFalse(PlacementType.PREPARATORY.isInvoiceable())

        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        initByPeriodAndPlacementType(period, PlacementType.PREPARATORY)

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(0, result.size)
    }

    @Test
    fun `plain club is not invoiced`() {
        assertFalse(PlacementType.CLUB.isInvoiceable())

        val period = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
        initByPeriodAndPlacementType(period, PlacementType.CLUB)

        db.transaction { it.createAllDraftInvoices(period) }

        val result = db.read(getAllInvoices)

        assertEquals(0, result.size)
    }

    private fun initByPeriodAndPlacementType(period: DateRange, placementType: PlacementType) {
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
                    placementType = placementType,
                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                    baseFee = 28900,
                    fee = 28900
                )
            )
        )
        insertDecisionsAndPlacements(
            listOf(
                decision.copy(validDuring = decision.validDuring.copy(end = period.start.plusDays(7))),
                decision.copy(id = FeeDecisionId(UUID.randomUUID()), validDuring = decision.validDuring.copy(start = period.start.plusDays(8)))
            )
        )
    }

    private fun initFreeJulyTestData(invoicingPeriod: DateRange, placementPeriods: List<DateRange>, placementType: PlacementType = PlacementType.DAYCARE) {
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

        placementPeriods.forEach { period -> db.transaction(insertPlacement(testChild_1.id, period, placementType)) }
        db.transaction { it.createAllDraftInvoices(invoicingPeriod) }
    }

    private fun initDataForAbsences(
        periods: List<DateRange>,
        absenceDays: Map<LocalDate, AbsenceType>,
        child: PersonData.Detailed = testChild_1
    ) =
        periods.forEachIndexed { index, period ->
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
                        serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
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
                absenceService.upsertAbsences(
                    tx,
                    absenceDays.map { (date, type) ->
                        Absence(
                            absenceType = type,
                            childId = child.id,
                            date = date,
                            careType = CareType.DAYCARE
                        )
                    },
                    testDecisionMaker_1.id
                )
            }
        }

    private fun insertDecisionsAndPlacements(feeDecisions: List<FeeDecision>) = db.transaction { tx ->
        tx.upsertFeeDecisions(feeDecisions)
        feeDecisions.forEach { decision ->
            decision.children.forEach { part ->
                tx.insertTestPlacement(
                    childId = part.child.id,
                    unitId = part.placement.unit.id,
                    startDate = decision.validFrom,
                    endDate = decision.validTo!!,
                    type = part.placement.type
                )
            }
        }
    }

    private fun insertPlacement(childId: UUID, period: DateRange, type: PlacementType = PlacementType.DAYCARE) = { tx: Database.Transaction ->
        tx.insertTestPlacement(
            childId = childId,
            unitId = testDaycare.id,
            startDate = period.start,
            endDate = period.end!!,
            type = type
        )
    }

    private fun insertChildParentRelation(headOfFamilyId: UUID, childId: UUID, period: DateRange) = { tx: Database.Transaction ->
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
}
