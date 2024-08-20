// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.TestInvoiceProductProvider
import fi.espoo.evaka.invoicing.createInvoiceFixture
import fi.espoo.evaka.invoicing.createInvoiceRowFixture
import fi.espoo.evaka.invoicing.data.insertInvoices
import fi.espoo.evaka.invoicing.domain.Invoice
import fi.espoo.evaka.invoicing.domain.InvoiceStatus
import fi.espoo.evaka.invoicing.integration.InvoiceIntegrationClient
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.InvoiceCorrectionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.config.defaultJsonMapperBuilder
import fi.espoo.evaka.shared.config.testFeatureConfig
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevInvoiceCorrection
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import java.time.Month
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InvoiceCorrectionsIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val productProvider: InvoiceProductProvider = TestInvoiceProductProvider()
    private val featureConfig: FeatureConfig = testFeatureConfig
    private val draftInvoiceGenerator: DraftInvoiceGenerator =
        DraftInvoiceGenerator(productProvider, featureConfig, DefaultInvoiceGenerationLogic)
    private val generator: InvoiceGenerator = InvoiceGenerator(draftInvoiceGenerator, featureConfig)
    private val invoiceService =
        InvoiceService(
            InvoiceIntegrationClient.MockClient(defaultJsonMapperBuilder().build()),
            TestInvoiceProductProvider(),
            featureConfig
        )
    private val clock = RealEvakaClock()

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testDecisionMaker_1)
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testAdult_1, DevPersonType.ADULT)
            tx.insert(testChild_1, DevPersonType.CHILD)
        }
    }

    @Test
    fun `refund totaling one month's invoice is refunded completely`() {
        val month = Month.JANUARY
        val invoice = createTestInvoice(100_00, month)
        val correctionId = insertTestCorrection(1, -100_00, month)

        val invoiceWithCorrections = db.read { it.applyCorrections(listOf(invoice), month) }.first()
        assertEquals(2, invoiceWithCorrections.rows.size)
        assertEquals(0, invoiceWithCorrections.totalPrice)
        assertEquals(1, invoiceWithCorrections.rows.first().amount)
        assertEquals(100_00, invoiceWithCorrections.rows.first().unitPrice)
        assertEquals(1, invoiceWithCorrections.rows.last().amount)
        assertEquals(-100_00, invoiceWithCorrections.rows.last().unitPrice)
        assertEquals(correctionId, invoiceWithCorrections.rows.last().correctionId)
    }

    @Test
    fun `increases are invoiced only once`() {
        val month = Month.JANUARY
        val correctionId = insertTestCorrection(1, 100_00, month)

        val invoicesWithCorrections =
            db.transaction {
                val invoices = it.applyCorrections(listOf(createTestInvoice(100_00, month)), month)
                it.insertInvoices(invoices)
                invoiceService.sendInvoices(
                    it,
                    AuthenticatedUser.Employee(
                        testDecisionMaker_1.id,
                        setOf(UserRole.FINANCE_ADMIN)
                    ),
                    clock,
                    invoices.map { it.id },
                    null,
                    null
                )
                invoices
            }
        assertEquals(1, invoicesWithCorrections.size)
        val invoiceWithCorrections = invoicesWithCorrections.first()
        assertEquals(2, invoiceWithCorrections.rows.size)
        assertEquals(200_00, invoiceWithCorrections.totalPrice)
        assertEquals(1, invoiceWithCorrections.rows.first().amount)
        assertEquals(100_00, invoiceWithCorrections.rows.first().unitPrice)
        assertEquals(1, invoiceWithCorrections.rows.last().amount)
        assertEquals(100_00, invoiceWithCorrections.rows.last().unitPrice)
        assertEquals(correctionId, invoiceWithCorrections.rows.last().correctionId)

        val nextMonthsInvoicesWithCorrections =
            db.transaction {
                val invoices =
                    it.applyCorrections(
                        listOf(createTestInvoice(100_00, month.plus(1))),
                        month.plus(1)
                    )
                it.insertInvoices(invoices)
                invoiceService.sendInvoices(
                    it,
                    AuthenticatedUser.Employee(
                        testDecisionMaker_1.id,
                        setOf(UserRole.FINANCE_ADMIN)
                    ),
                    clock,
                    invoices.map { it.id },
                    null,
                    null
                )
                invoices
            }
        assertEquals(1, nextMonthsInvoicesWithCorrections.size)
        val nextMonthsInvoiceWithCorrections = nextMonthsInvoicesWithCorrections.first()
        assertEquals(1, nextMonthsInvoiceWithCorrections.rows.size)
        assertEquals(100_00, nextMonthsInvoiceWithCorrections.totalPrice)
        assertEquals(1, nextMonthsInvoiceWithCorrections.rows.first().amount)
        assertEquals(100_00, nextMonthsInvoiceWithCorrections.rows.first().unitPrice)
    }

    @Test
    fun `refunds are invoiced only once`() {
        val month = Month.JANUARY
        val correctionId = insertTestCorrection(1, -100_00, month)

        val invoicesWithCorrections =
            db.transaction {
                val invoices = it.applyCorrections(listOf(createTestInvoice(100_00, month)), month)
                it.insertInvoices(invoices)
                invoiceService.sendInvoices(
                    it,
                    AuthenticatedUser.Employee(
                        testDecisionMaker_1.id,
                        setOf(UserRole.FINANCE_ADMIN)
                    ),
                    clock,
                    invoices.map { it.id },
                    null,
                    null
                )
                invoices
            }
        assertEquals(1, invoicesWithCorrections.size)
        val invoiceWithCorrections = invoicesWithCorrections.first()
        assertEquals(2, invoiceWithCorrections.rows.size)
        assertEquals(0, invoiceWithCorrections.totalPrice)
        assertEquals(1, invoiceWithCorrections.rows.first().amount)
        assertEquals(100_00, invoiceWithCorrections.rows.first().unitPrice)
        assertEquals(1, invoiceWithCorrections.rows.last().amount)
        assertEquals(-100_00, invoiceWithCorrections.rows.last().unitPrice)
        assertEquals(correctionId, invoiceWithCorrections.rows.last().correctionId)

        val nextMonthsInvoicesWithCorrections =
            db.transaction {
                val invoices =
                    it.applyCorrections(
                        listOf(createTestInvoice(100_00, month.plus(1))),
                        month.plus(1)
                    )
                it.insertInvoices(invoices)
                invoiceService.sendInvoices(
                    it,
                    AuthenticatedUser.Employee(
                        testDecisionMaker_1.id,
                        setOf(UserRole.FINANCE_ADMIN)
                    ),
                    clock,
                    invoices.map { it.id },
                    null,
                    null
                )
                invoices
            }
        assertEquals(1, nextMonthsInvoicesWithCorrections.size)
        val nextMonthsInvoiceWithCorrections = nextMonthsInvoicesWithCorrections.first()
        assertEquals(1, nextMonthsInvoiceWithCorrections.rows.size)
        assertEquals(100_00, nextMonthsInvoiceWithCorrections.totalPrice)
        assertEquals(1, nextMonthsInvoiceWithCorrections.rows.first().amount)
        assertEquals(100_00, nextMonthsInvoiceWithCorrections.rows.first().unitPrice)
    }

    @Test
    fun `refund with smaller unit price totaling one month's invoice is refunded completely`() {
        val month = Month.JANUARY
        val invoice = createTestInvoice(100_00, month)
        val correctionId = insertTestCorrection(5, -20_00, month)

        val invoiceWithCorrections = db.read { it.applyCorrections(listOf(invoice), month) }.first()
        assertEquals(2, invoiceWithCorrections.rows.size)
        assertEquals(0, invoiceWithCorrections.totalPrice)
        assertEquals(1, invoiceWithCorrections.rows.first().amount)
        assertEquals(100_00, invoiceWithCorrections.rows.first().unitPrice)
        assertEquals(5, invoiceWithCorrections.rows.last().amount)
        assertEquals(-20_00, invoiceWithCorrections.rows.last().unitPrice)
        assertEquals(correctionId, invoiceWithCorrections.rows.last().correctionId)
    }

    @Test
    fun `refund with multiple amounts that totals over one month's invoice is split according to amounts`() {
        val correctionId = insertTestCorrection(5, -40_00, Month.JANUARY)

        val firstMonth = Month.JANUARY
        val firstInvoice =
            db.read {
                    it.applyCorrections(listOf(createTestInvoice(100_00, firstMonth)), firstMonth)
                }
                .first()
        assertEquals(2, firstInvoice.rows.size)
        assertEquals(20_00, firstInvoice.totalPrice)
        assertEquals(1, firstInvoice.rows.first().amount)
        assertEquals(100_00, firstInvoice.rows.first().unitPrice)
        assertEquals(2, firstInvoice.rows.last().amount)
        assertEquals(-40_00, firstInvoice.rows.last().unitPrice)
        assertEquals(correctionId, firstInvoice.rows.last().correctionId)
        db.transaction { it.insertInvoices(listOf(firstInvoice.copy(status = InvoiceStatus.SENT))) }

        val secondMonth = Month.FEBRUARY
        val secondInvoice =
            db.read {
                    it.applyCorrections(listOf(createTestInvoice(100_00, secondMonth)), secondMonth)
                }
                .first()
        assertEquals(2, secondInvoice.rows.size)
        assertEquals(20_00, secondInvoice.totalPrice)
        assertEquals(1, secondInvoice.rows.first().amount)
        assertEquals(100_00, secondInvoice.rows.first().unitPrice)
        assertEquals(2, secondInvoice.rows.last().amount)
        assertEquals(-40_00, secondInvoice.rows.last().unitPrice)
        assertEquals(correctionId, secondInvoice.rows.last().correctionId)
        db.transaction {
            it.insertInvoices(listOf(secondInvoice.copy(status = InvoiceStatus.SENT)))
        }

        val thirdMonth = Month.MARCH
        val thirdInvoice =
            db.read {
                    it.applyCorrections(listOf(createTestInvoice(100_00, thirdMonth)), thirdMonth)
                }
                .first()
        assertEquals(2, thirdInvoice.rows.size)
        assertEquals(60_00, thirdInvoice.totalPrice)
        assertEquals(1, thirdInvoice.rows.first().amount)
        assertEquals(100_00, thirdInvoice.rows.first().unitPrice)
        assertEquals(1, thirdInvoice.rows.last().amount)
        assertEquals(-40_00, thirdInvoice.rows.last().unitPrice)
        assertEquals(correctionId, thirdInvoice.rows.last().correctionId)
        db.transaction { it.insertInvoices(listOf(thirdInvoice.copy(status = InvoiceStatus.SENT))) }
    }

    @Test
    fun `refund with unit price over one month's invoice is split over several invoices`() {
        val correctionId = insertTestCorrection(1, -250_00, Month.JANUARY)

        val firstMonth = Month.JANUARY
        val firstInvoice =
            db.read {
                    it.applyCorrections(listOf(createTestInvoice(100_00, firstMonth)), firstMonth)
                }
                .first()
        assertEquals(2, firstInvoice.rows.size)
        assertEquals(0, firstInvoice.totalPrice)
        assertEquals(1, firstInvoice.rows.first().amount)
        assertEquals(100_00, firstInvoice.rows.first().unitPrice)
        assertEquals(1, firstInvoice.rows.last().amount)
        assertEquals(-100_00, firstInvoice.rows.last().unitPrice)
        assertEquals(correctionId, firstInvoice.rows.last().correctionId)
        db.transaction { it.insertInvoices(listOf(firstInvoice.copy(status = InvoiceStatus.SENT))) }

        val secondMonth = Month.FEBRUARY
        val secondInvoice =
            db.read {
                    it.applyCorrections(listOf(createTestInvoice(100_00, secondMonth)), secondMonth)
                }
                .first()
        assertEquals(2, secondInvoice.rows.size)
        assertEquals(0, secondInvoice.totalPrice)
        assertEquals(1, secondInvoice.rows.first().amount)
        assertEquals(100_00, secondInvoice.rows.first().unitPrice)
        assertEquals(1, secondInvoice.rows.last().amount)
        assertEquals(-100_00, secondInvoice.rows.last().unitPrice)
        assertEquals(correctionId, secondInvoice.rows.last().correctionId)
        db.transaction {
            it.insertInvoices(listOf(secondInvoice.copy(status = InvoiceStatus.SENT)))
        }

        val thirdMonth = Month.MARCH
        val thirdInvoice =
            db.read {
                    it.applyCorrections(listOf(createTestInvoice(100_00, thirdMonth)), thirdMonth)
                }
                .first()
        assertEquals(2, thirdInvoice.rows.size)
        assertEquals(50_00, thirdInvoice.totalPrice)
        assertEquals(1, thirdInvoice.rows.first().amount)
        assertEquals(100_00, thirdInvoice.rows.first().unitPrice)
        assertEquals(1, thirdInvoice.rows.last().amount)
        assertEquals(-50_00, thirdInvoice.rows.last().unitPrice)
        assertEquals(correctionId, thirdInvoice.rows.last().correctionId)
        db.transaction { it.insertInvoices(listOf(thirdInvoice.copy(status = InvoiceStatus.SENT))) }
    }

    @Test
    fun `increase over one month's invoice is applied`() {
        val month = Month.JANUARY
        val invoice = createTestInvoice(100_00, month)
        val correctionId = insertTestCorrection(1, 500_00, month)

        val invoiceWithCorrections = db.read { it.applyCorrections(listOf(invoice), month) }.first()
        assertEquals(2, invoiceWithCorrections.rows.size)
        assertEquals(600_00, invoiceWithCorrections.totalPrice)
        assertEquals(1, invoiceWithCorrections.rows.first().amount)
        assertEquals(100_00, invoiceWithCorrections.rows.first().unitPrice)
        assertEquals(1, invoiceWithCorrections.rows.last().amount)
        assertEquals(500_00, invoiceWithCorrections.rows.last().unitPrice)
        assertEquals(correctionId, invoiceWithCorrections.rows.last().correctionId)
    }

    @Test
    fun `multiple corrections are applied oldest first`() {
        val firstCorrectionId = insertTestCorrection(1, -70_00, Month.JANUARY, 2019)
        val secondCorrectionId = insertTestCorrection(1, -80_00, Month.FEBRUARY, 2019)

        val firstMonth = Month.JANUARY
        val firstInvoice =
            db.read {
                    it.applyCorrections(listOf(createTestInvoice(100_00, firstMonth)), firstMonth)
                }
                .first()
        assertEquals(3, firstInvoice.rows.size)
        assertEquals(0, firstInvoice.totalPrice)
        assertEquals(1, firstInvoice.rows[0].amount)
        assertEquals(100_00, firstInvoice.rows[0].unitPrice)
        assertEquals(1, firstInvoice.rows[1].amount)
        assertEquals(-70_00, firstInvoice.rows[1].unitPrice)
        assertEquals(firstCorrectionId, firstInvoice.rows[1].correctionId)
        assertEquals(1, firstInvoice.rows[2].amount)
        assertEquals(-30_00, firstInvoice.rows[2].unitPrice)
        assertEquals(secondCorrectionId, firstInvoice.rows[2].correctionId)
        db.transaction { it.insertInvoices(listOf(firstInvoice.copy(status = InvoiceStatus.SENT))) }

        val secondMonth = Month.FEBRUARY
        val secondInvoice =
            db.read {
                    it.applyCorrections(listOf(createTestInvoice(100_00, secondMonth)), secondMonth)
                }
                .first()
        assertEquals(2, secondInvoice.rows.size)
        assertEquals(50_00, secondInvoice.totalPrice)
        assertEquals(1, secondInvoice.rows.first().amount)
        assertEquals(100_00, secondInvoice.rows.first().unitPrice)
        assertEquals(1, secondInvoice.rows.last().amount)
        assertEquals(-50_00, secondInvoice.rows.last().unitPrice)
        assertEquals(secondCorrectionId, secondInvoice.rows.last().correctionId)
        db.transaction {
            it.insertInvoices(listOf(secondInvoice.copy(status = InvoiceStatus.SENT)))
        }
    }

    @Test
    fun `large increase makes room for multiple refunds`() {
        val month = Month.JANUARY
        val increaseCorrectionId = insertTestCorrection(1, 200_00, month)
        val firstRefundCorrectionId = insertTestCorrection(1, -70_00, month)
        val secondRefundCorrectionId = insertTestCorrection(1, -80_00, month)

        val firstInvoice =
            db.read { it.applyCorrections(listOf(createTestInvoice(100_00, month)), month) }.first()
        assertEquals(4, firstInvoice.rows.size)
        assertEquals(150_00, firstInvoice.totalPrice)
        assertEquals(1, firstInvoice.rows[0].amount)
        assertEquals(100_00, firstInvoice.rows[0].unitPrice)
        assertEquals(1, firstInvoice.rows[1].amount)
        assertEquals(200_00, firstInvoice.rows[1].unitPrice)
        assertEquals(increaseCorrectionId, firstInvoice.rows[1].correctionId)
        assertEquals(1, firstInvoice.rows[2].amount)
        assertEquals(-70_00, firstInvoice.rows[2].unitPrice)
        assertEquals(firstRefundCorrectionId, firstInvoice.rows[2].correctionId)
        assertEquals(1, firstInvoice.rows[3].amount)
        assertEquals(-80_00, firstInvoice.rows[3].unitPrice)
        assertEquals(secondRefundCorrectionId, firstInvoice.rows[3].correctionId)
    }

    @Test
    fun `refund partition with variable invoice totals works as expected`() {
        val correctionId = insertTestCorrection(5, -50_00, Month.JANUARY)

        val firstMonth = Month.JANUARY
        val firstInvoice =
            db.read {
                    it.applyCorrections(listOf(createTestInvoice(100_00, firstMonth)), firstMonth)
                }
                .first()
        assertEquals(2, firstInvoice.rows.size)
        assertEquals(0, firstInvoice.totalPrice)
        assertEquals(1, firstInvoice.rows.first().amount)
        assertEquals(100_00, firstInvoice.rows.first().unitPrice)
        assertEquals(2, firstInvoice.rows.last().amount)
        assertEquals(-50_00, firstInvoice.rows.last().unitPrice)
        assertEquals(correctionId, firstInvoice.rows.last().correctionId)
        db.transaction { it.insertInvoices(listOf(firstInvoice.copy(status = InvoiceStatus.SENT))) }

        val secondMonth = Month.FEBRUARY
        val secondInvoice =
            db.read {
                    it.applyCorrections(listOf(createTestInvoice(30_00, secondMonth)), secondMonth)
                }
                .first()
        assertEquals(2, secondInvoice.rows.size)
        assertEquals(0, secondInvoice.totalPrice)
        assertEquals(1, secondInvoice.rows.first().amount)
        assertEquals(30_00, secondInvoice.rows.first().unitPrice)
        assertEquals(3, secondInvoice.rows.last().amount)
        assertEquals(-10_00, secondInvoice.rows.last().unitPrice)
        assertEquals(correctionId, secondInvoice.rows.last().correctionId)
        db.transaction {
            it.insertInvoices(listOf(secondInvoice.copy(status = InvoiceStatus.SENT)))
        }

        val thirdMonth = Month.MARCH
        val thirdInvoice =
            db.read {
                    it.applyCorrections(listOf(createTestInvoice(100_00, thirdMonth)), thirdMonth)
                }
                .first()
        assertEquals(2, thirdInvoice.rows.size)
        assertEquals(20_00, thirdInvoice.totalPrice)
        assertEquals(1, thirdInvoice.rows.first().amount)
        assertEquals(100_00, thirdInvoice.rows.first().unitPrice)
        assertEquals(2, thirdInvoice.rows.last().amount)
        assertEquals(-40_00, thirdInvoice.rows.last().unitPrice)
        assertEquals(correctionId, thirdInvoice.rows.last().correctionId)
        db.transaction { it.insertInvoices(listOf(thirdInvoice.copy(status = InvoiceStatus.SENT))) }

        val fourthMonth = Month.APRIL
        val fourthInvoice =
            db.read {
                    it.applyCorrections(listOf(createTestInvoice(100_00, fourthMonth)), fourthMonth)
                }
                .first()
        assertEquals(2, fourthInvoice.rows.size)
        assertEquals(60_00, fourthInvoice.totalPrice)
        assertEquals(1, fourthInvoice.rows.first().amount)
        assertEquals(100_00, fourthInvoice.rows.first().unitPrice)
        assertEquals(1, fourthInvoice.rows.last().amount)
        assertEquals(-40_00, fourthInvoice.rows.last().unitPrice)
        assertEquals(correctionId, fourthInvoice.rows.last().correctionId)
        db.transaction {
            it.insertInvoices(listOf(fourthInvoice.copy(status = InvoiceStatus.SENT)))
        }
    }

    @Test
    fun `correction is marked as applied when its invoice is sent`() {
        val month = Month.JANUARY
        val correctionId = insertTestCorrection(1, -50_00, month)
        db.transaction {
            val invoices = it.applyCorrections(listOf(createTestInvoice(100_00, month)), month)
            it.insertInvoices(invoices)
            invoiceService.sendInvoices(
                it,
                AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.FINANCE_ADMIN)),
                clock,
                invoices.map { it.id },
                null,
                null
            )
        }

        val result =
            db.read {
                it.createQuery { sql("SELECT id, applied_completely FROM invoice_correction") }
                    .toList {
                        column<InvoiceCorrectionId>("id") to column<Boolean>("applied_completely")
                    }
            }
        assertEquals(1, result.size)
        assertEquals(correctionId, result.first().first)
        assertEquals(true, result.first().second)
    }

    @Test
    fun `correction is not marked as applied when its invoice is sent if the correction hasn't been applied completely`() {
        val month = Month.JANUARY
        val correctionId = insertTestCorrection(1, -200_00, month)
        db.transaction {
            val invoices = it.applyCorrections(listOf(createTestInvoice(100_00, month)), month)
            it.insertInvoices(invoices)
            invoiceService.sendInvoices(
                it,
                AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.FINANCE_ADMIN)),
                clock,
                invoices.map { it.id },
                null,
                null
            )
        }

        val result =
            db.read {
                it.createQuery { sql("SELECT id, applied_completely FROM invoice_correction") }
                    .toList {
                        column<InvoiceCorrectionId>("id") to column<Boolean>("applied_completely")
                    }
            }
        assertEquals(1, result.size)
        assertEquals(correctionId, result.first().first)
        assertEquals(false, result.first().second)
    }

    @Test
    fun `corrections without a matching invoice are included as new invoices`() {
        val month = Month.JANUARY
        val refundId = insertTestCorrection(1, -50_00, month)
        val increaseId = insertTestCorrection(1, 100_00, month)

        val invoiceWithCorrections = db.read { it.applyCorrections(listOf(), month) }.first()
        assertEquals(2, invoiceWithCorrections.rows.size)
        assertEquals(50_00, invoiceWithCorrections.totalPrice)
        assertEquals(1, invoiceWithCorrections.rows.first().amount)
        assertEquals(100_00, invoiceWithCorrections.rows.first().unitPrice)
        assertEquals(increaseId, invoiceWithCorrections.rows.first().correctionId)
        assertEquals(1, invoiceWithCorrections.rows.last().amount)
        assertEquals(-50_00, invoiceWithCorrections.rows.last().unitPrice)
        assertEquals(refundId, invoiceWithCorrections.rows.last().correctionId)
    }

    @Test
    fun `refunds without a matching invoice do not generate new invoices`() {
        val month = Month.JANUARY
        insertTestCorrection(1, -50_00, month)

        val invoicesWithCorrections = db.read { it.applyCorrections(listOf(), month) }
        assertEquals(0, invoicesWithCorrections.size)
    }

    private fun Database.Read.applyCorrections(
        invoices: List<Invoice>,
        month: Month
    ): List<Invoice> {
        val period = FiniteDateRange.ofMonth(2020, month)
        return generator
            .applyCorrections(this, invoices, period, mapOf(testDaycare.id to testArea.id))
            .shuffled() // randomize order to expose assumptions
    }

    private fun createTestInvoice(total: Int, month: Month): Invoice {
        val period = FiniteDateRange.ofMonth(2020, month)
        return createInvoiceFixture(
            status = InvoiceStatus.DRAFT,
            headOfFamilyId = testAdult_1.id,
            areaId = testArea.id,
            period = period,
            rows =
                listOf(
                    createInvoiceRowFixture(
                        childId = testChild_1.id,
                        unitId = testDaycare.id,
                        amount = 1,
                        unitPrice = total
                    )
                )
        )
    }

    private fun insertTestCorrection(
        amount: Int,
        unitPrice: Int,
        month: Month,
        year: Int = 2020
    ): InvoiceCorrectionId {
        val period = FiniteDateRange.ofMonth(year, month)
        return db.transaction {
            it.insert(
                DevInvoiceCorrection(
                    headOfFamilyId = testAdult_1.id,
                    childId = testChild_1.id,
                    amount = amount,
                    unitPrice = unitPrice,
                    period = period,
                    unitId = testDaycare.id,
                    product = productProvider.mapToProduct(PlacementType.DAYCARE),
                    description = "",
                    note = ""
                )
            )
        }
    }
}
