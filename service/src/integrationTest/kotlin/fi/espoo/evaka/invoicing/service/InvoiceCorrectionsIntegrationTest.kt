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
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.config.defaultJsonMapperBuilder
import fi.espoo.evaka.shared.config.testFeatureConfig
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevInvoiceCorrection
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.RealEvakaClock
import java.time.Month
import java.time.YearMonth
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
            featureConfig,
        )
    private val clock = RealEvakaClock()

    val employee = DevEmployee(roles = setOf(UserRole.FINANCE_ADMIN))
    val area = DevCareArea()
    val daycare = DevDaycare(areaId = area.id)
    val adult = DevPerson()
    val child = DevPerson()

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
        }
    }

    @Test
    fun `refund totaling one month's invoice is refunded completely`() {
        val month = YearMonth.of(2020, Month.JANUARY)
        val invoice = createTestInvoice(100_00, month)
        val correctionId = insertTestCorrection(1, -100_00, month)

        val invoiceWithCorrections = applyCorrections(invoice, month)
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
        val month = YearMonth.of(2020, Month.JANUARY)
        val correctionId = insertTestCorrection(1, 100_00, month)

        val invoiceWithCorrections = applyCorrections(createTestInvoice(100_00, month), month)
        insertAndSendInvoice(invoiceWithCorrections)

        assertEquals(2, invoiceWithCorrections.rows.size)
        assertEquals(200_00, invoiceWithCorrections.totalPrice)
        assertEquals(1, invoiceWithCorrections.rows.first().amount)
        assertEquals(100_00, invoiceWithCorrections.rows.first().unitPrice)
        assertEquals(1, invoiceWithCorrections.rows.last().amount)
        assertEquals(100_00, invoiceWithCorrections.rows.last().unitPrice)
        assertEquals(correctionId, invoiceWithCorrections.rows.last().correctionId)

        val nextMonthsInvoiceWithCorrections =
            applyCorrections(createTestInvoice(100_00, month.plusMonths(1)), month.plusMonths(1))
        insertAndSendInvoice(nextMonthsInvoiceWithCorrections)

        assertEquals(1, nextMonthsInvoiceWithCorrections.rows.size)
        assertEquals(100_00, nextMonthsInvoiceWithCorrections.totalPrice)
        assertEquals(1, nextMonthsInvoiceWithCorrections.rows.first().amount)
        assertEquals(100_00, nextMonthsInvoiceWithCorrections.rows.first().unitPrice)
    }

    @Test
    fun `refunds are invoiced only once`() {
        val month = YearMonth.of(2020, Month.JANUARY)
        val correctionId = insertTestCorrection(1, -100_00, month)

        val invoiceWithCorrections = applyCorrections(createTestInvoice(100_00, month), month)
        insertAndSendInvoice(invoiceWithCorrections)

        assertEquals(2, invoiceWithCorrections.rows.size)
        assertEquals(0, invoiceWithCorrections.totalPrice)
        assertEquals(1, invoiceWithCorrections.rows.first().amount)
        assertEquals(100_00, invoiceWithCorrections.rows.first().unitPrice)
        assertEquals(1, invoiceWithCorrections.rows.last().amount)
        assertEquals(-100_00, invoiceWithCorrections.rows.last().unitPrice)
        assertEquals(correctionId, invoiceWithCorrections.rows.last().correctionId)

        val nextMonthsInvoiceWithCorrections =
            applyCorrections(createTestInvoice(100_00, month.plusMonths(1)), month.plusMonths(1))
        insertAndSendInvoice(nextMonthsInvoiceWithCorrections)

        assertEquals(1, nextMonthsInvoiceWithCorrections.rows.size)
        assertEquals(100_00, nextMonthsInvoiceWithCorrections.totalPrice)
        assertEquals(1, nextMonthsInvoiceWithCorrections.rows.first().amount)
        assertEquals(100_00, nextMonthsInvoiceWithCorrections.rows.first().unitPrice)
    }

    @Test
    fun `refund with smaller unit price totaling one month's invoice is refunded completely`() {
        val month = YearMonth.of(2020, Month.JANUARY)
        val invoice = createTestInvoice(100_00, month)
        val correctionId = insertTestCorrection(5, -20_00, month)

        val invoiceWithCorrections = applyCorrections(invoice, month)
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
        val correctionId = insertTestCorrection(5, -40_00, YearMonth.of(2020, Month.JANUARY))

        val firstMonth = YearMonth.of(2020, Month.JANUARY)
        val firstInvoice = applyCorrections(createTestInvoice(100_00, firstMonth), firstMonth)
        assertEquals(2, firstInvoice.rows.size)
        assertEquals(20_00, firstInvoice.totalPrice)
        assertEquals(1, firstInvoice.rows.first().amount)
        assertEquals(100_00, firstInvoice.rows.first().unitPrice)
        assertEquals(2, firstInvoice.rows.last().amount)
        assertEquals(-40_00, firstInvoice.rows.last().unitPrice)
        assertEquals(correctionId, firstInvoice.rows.last().correctionId)

        insertAndSendInvoice(firstInvoice)

        val secondMonth = firstMonth.plusMonths(1)
        val secondInvoice = applyCorrections(createTestInvoice(100_00, secondMonth), secondMonth)
        assertEquals(2, secondInvoice.rows.size)
        assertEquals(20_00, secondInvoice.totalPrice)
        assertEquals(1, secondInvoice.rows.first().amount)
        assertEquals(100_00, secondInvoice.rows.first().unitPrice)
        assertEquals(2, secondInvoice.rows.last().amount)
        assertEquals(-40_00, secondInvoice.rows.last().unitPrice)
        assertEquals(getUnappliedCorrection().id, secondInvoice.rows.last().correctionId)

        insertAndSendInvoice(secondInvoice)

        val thirdMonth = secondMonth.plusMonths(1)
        val thirdInvoice = applyCorrections(createTestInvoice(100_00, thirdMonth), thirdMonth)
        assertEquals(2, thirdInvoice.rows.size)
        assertEquals(60_00, thirdInvoice.totalPrice)
        assertEquals(1, thirdInvoice.rows.first().amount)
        assertEquals(100_00, thirdInvoice.rows.first().unitPrice)
        assertEquals(1, thirdInvoice.rows.last().amount)
        assertEquals(-40_00, thirdInvoice.rows.last().unitPrice)
        assertEquals(getUnappliedCorrection().id, thirdInvoice.rows.last().correctionId)

        insertAndSendInvoice(thirdInvoice)
    }

    @Test
    fun `refund with unit price over one month's invoice is split over several invoices`() {
        val correctionId = insertTestCorrection(1, -250_00, YearMonth.of(2020, Month.JANUARY))

        val firstMonth = YearMonth.of(2020, Month.JANUARY)
        val firstInvoice = applyCorrections(createTestInvoice(100_00, firstMonth), firstMonth)
        assertEquals(2, firstInvoice.rows.size)
        assertEquals(0, firstInvoice.totalPrice)
        assertEquals(1, firstInvoice.rows.first().amount)
        assertEquals(100_00, firstInvoice.rows.first().unitPrice)
        assertEquals(1, firstInvoice.rows.last().amount)
        assertEquals(-100_00, firstInvoice.rows.last().unitPrice)
        assertEquals(correctionId, firstInvoice.rows.last().correctionId)

        insertAndSendInvoice(firstInvoice)

        val secondMonth = firstMonth.plusMonths(1)
        val secondInvoice = applyCorrections(createTestInvoice(100_00, secondMonth), secondMonth)
        assertEquals(2, secondInvoice.rows.size)
        assertEquals(0, secondInvoice.totalPrice)
        assertEquals(1, secondInvoice.rows.first().amount)
        assertEquals(100_00, secondInvoice.rows.first().unitPrice)
        assertEquals(1, secondInvoice.rows.last().amount)
        assertEquals(-100_00, secondInvoice.rows.last().unitPrice)
        assertEquals(getUnappliedCorrection().id, secondInvoice.rows.last().correctionId)

        insertAndSendInvoice(secondInvoice)

        val thirdMonth = secondMonth.plusMonths(1)
        val thirdInvoice = applyCorrections(createTestInvoice(100_00, thirdMonth), thirdMonth)
        assertEquals(2, thirdInvoice.rows.size)
        assertEquals(50_00, thirdInvoice.totalPrice)
        assertEquals(1, thirdInvoice.rows.first().amount)
        assertEquals(100_00, thirdInvoice.rows.first().unitPrice)
        assertEquals(1, thirdInvoice.rows.last().amount)
        assertEquals(-50_00, thirdInvoice.rows.last().unitPrice)
        assertEquals(getUnappliedCorrection().id, thirdInvoice.rows.last().correctionId)

        insertAndSendInvoice(thirdInvoice)
    }

    @Test
    fun `increase over one month's invoice is applied`() {
        val month = YearMonth.of(2020, Month.JANUARY)
        val invoice = createTestInvoice(100_00, month)
        val correctionId = insertTestCorrection(1, 500_00, month)

        val invoiceWithCorrections = applyCorrections(invoice, month)
        assertEquals(2, invoiceWithCorrections.rows.size)
        assertEquals(600_00, invoiceWithCorrections.totalPrice)
        assertEquals(1, invoiceWithCorrections.rows.first().amount)
        assertEquals(100_00, invoiceWithCorrections.rows.first().unitPrice)
        assertEquals(1, invoiceWithCorrections.rows.last().amount)
        assertEquals(500_00, invoiceWithCorrections.rows.last().unitPrice)
        assertEquals(correctionId, invoiceWithCorrections.rows.last().correctionId)
    }

    @Test
    fun `multiple corrections are applied biggest absolute amount first`() {
        val smallerCorrectionId = insertTestCorrection(1, -70_00, YearMonth.of(2020, Month.JANUARY))
        val biggerCorrectionId = insertTestCorrection(1, -80_00, YearMonth.of(2020, Month.JANUARY))

        val firstMonth = YearMonth.of(2020, Month.JANUARY)
        val firstInvoice = applyCorrections(createTestInvoice(100_00, firstMonth), firstMonth)
        assertEquals(3, firstInvoice.rows.size)
        assertEquals(0, firstInvoice.totalPrice)
        assertEquals(1, firstInvoice.rows[0].amount)
        assertEquals(100_00, firstInvoice.rows[0].unitPrice)
        assertEquals(1, firstInvoice.rows[1].amount)
        assertEquals(-80_00, firstInvoice.rows[1].unitPrice)
        assertEquals(biggerCorrectionId, firstInvoice.rows[1].correctionId)
        assertEquals(1, firstInvoice.rows[2].amount)
        assertEquals(-20_00, firstInvoice.rows[2].unitPrice)
        assertEquals(smallerCorrectionId, firstInvoice.rows[2].correctionId)

        insertAndSendInvoice(firstInvoice)

        val secondMonth = firstMonth.plusMonths(1)
        val secondInvoice = applyCorrections(createTestInvoice(100_00, secondMonth), secondMonth)
        assertEquals(2, secondInvoice.rows.size)
        assertEquals(50_00, secondInvoice.totalPrice)
        assertEquals(1, secondInvoice.rows.first().amount)
        assertEquals(100_00, secondInvoice.rows.first().unitPrice)
        assertEquals(1, secondInvoice.rows.last().amount)
        assertEquals(-50_00, secondInvoice.rows.last().unitPrice)
        assertEquals(getUnappliedCorrection().id, secondInvoice.rows.last().correctionId)

        insertAndSendInvoice(secondInvoice)
    }

    @Test
    fun `large increase makes room for multiple refunds`() {
        val month = YearMonth.of(2020, Month.JANUARY)
        val increaseCorrectionId = insertTestCorrection(1, 200_00, month)
        val smallerRefundCorrectionId = insertTestCorrection(1, -70_00, month)
        val biggerRefundCorrectionId = insertTestCorrection(1, -80_00, month)

        val firstInvoice = applyCorrections(createTestInvoice(100_00, month), month)
        assertEquals(4, firstInvoice.rows.size)
        assertEquals(150_00, firstInvoice.totalPrice)
        assertEquals(1, firstInvoice.rows[0].amount)
        assertEquals(100_00, firstInvoice.rows[0].unitPrice)
        assertEquals(1, firstInvoice.rows[1].amount)
        assertEquals(200_00, firstInvoice.rows[1].unitPrice)
        assertEquals(increaseCorrectionId, firstInvoice.rows[1].correctionId)
        assertEquals(1, firstInvoice.rows[2].amount)
        assertEquals(-80_00, firstInvoice.rows[2].unitPrice)
        assertEquals(biggerRefundCorrectionId, firstInvoice.rows[2].correctionId)
        assertEquals(1, firstInvoice.rows[3].amount)
        assertEquals(-70_00, firstInvoice.rows[3].unitPrice)
        assertEquals(smallerRefundCorrectionId, firstInvoice.rows[3].correctionId)
    }

    @Test
    fun `refund partition with variable invoice totals works as expected`() {
        val correctionId = insertTestCorrection(5, -50_00, YearMonth.of(2020, Month.JANUARY))

        val firstMonth = YearMonth.of(2020, Month.JANUARY)
        val firstInvoice = applyCorrections(createTestInvoice(100_00, firstMonth), firstMonth)
        assertEquals(2, firstInvoice.rows.size)
        assertEquals(0, firstInvoice.totalPrice)
        assertEquals(1, firstInvoice.rows.first().amount)
        assertEquals(100_00, firstInvoice.rows.first().unitPrice)
        assertEquals(2, firstInvoice.rows.last().amount)
        assertEquals(-50_00, firstInvoice.rows.last().unitPrice)
        assertEquals(correctionId, firstInvoice.rows.last().correctionId)

        insertAndSendInvoice(firstInvoice)

        val secondMonth = firstMonth.plusMonths(1)
        val secondInvoice = applyCorrections(createTestInvoice(30_00, secondMonth), secondMonth)
        assertEquals(2, secondInvoice.rows.size)
        assertEquals(0, secondInvoice.totalPrice)
        assertEquals(1, secondInvoice.rows.first().amount)
        assertEquals(30_00, secondInvoice.rows.first().unitPrice)
        assertEquals(3, secondInvoice.rows.last().amount)
        assertEquals(-10_00, secondInvoice.rows.last().unitPrice)
        val secondCorrection = getUnappliedCorrection()
        assertEquals(secondCorrection.id, secondInvoice.rows.last().correctionId)

        insertAndSendInvoice(secondInvoice)

        val thirdMonth = secondMonth.plusMonths(1)
        val thirdInvoice = applyCorrections(createTestInvoice(100_00, thirdMonth), thirdMonth)
        assertEquals(2, thirdInvoice.rows.size)
        assertEquals(20_00, thirdInvoice.totalPrice)
        assertEquals(1, thirdInvoice.rows.first().amount)
        assertEquals(100_00, thirdInvoice.rows.first().unitPrice)
        assertEquals(2, thirdInvoice.rows.last().amount)
        assertEquals(-40_00, thirdInvoice.rows.last().unitPrice)
        assertEquals(getUnappliedCorrection().id, thirdInvoice.rows.last().correctionId)

        insertAndSendInvoice(thirdInvoice)

        val fourthMonth = thirdMonth.plusMonths(1)
        val fourthInvoice = applyCorrections(createTestInvoice(100_00, fourthMonth), fourthMonth)
        assertEquals(2, fourthInvoice.rows.size)
        assertEquals(60_00, fourthInvoice.totalPrice)
        assertEquals(1, fourthInvoice.rows.first().amount)
        assertEquals(100_00, fourthInvoice.rows.first().unitPrice)
        assertEquals(1, fourthInvoice.rows.last().amount)
        assertEquals(-40_00, fourthInvoice.rows.last().unitPrice)
        assertEquals(getUnappliedCorrection().id, fourthInvoice.rows.last().correctionId)

        insertAndSendInvoice(fourthInvoice)
    }

    @Test
    fun `correction is kept when its invoice is sent`() {
        val month = YearMonth.of(2020, Month.JANUARY)
        val correctionId = insertTestCorrection(1, -50_00, month)
        val invoice = applyCorrections(createTestInvoice(100_00, month), month)
        insertAndSendInvoice(invoice)

        val correction =
            db.read { tx -> tx.getInvoiceCorrectionsByIds(setOf(correctionId)).single() }
        assertEquals(correctionId, correction.id)
        assertEquals(1, correction.amount)
        assertEquals(-50_00, correction.unitPrice)
    }

    @Test
    fun `leftover amount from negative correction is moved to next month when its invoice is sent`() {
        val month = YearMonth.of(2020, Month.JANUARY)
        insertTestCorrection(1, -200_00, month)
        val invoice = applyCorrections(createTestInvoice(100_00, month), month)
        insertAndSendInvoice(invoice)

        val unappliedCorrection = getUnappliedCorrection()
        assertEquals(1, unappliedCorrection.amount)
        assertEquals(-100_00, unappliedCorrection.unitPrice)
    }

    @Test
    fun `corrections without a matching invoice are included as new invoices`() {
        val month = YearMonth.of(2020, Month.JANUARY)
        val refundId = insertTestCorrection(1, -50_00, month)
        val increaseId = insertTestCorrection(1, 100_00, month)

        val invoiceWithCorrections = applyCorrections(listOf(), month).single()
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
        val month = YearMonth.of(2020, Month.JANUARY)
        insertTestCorrection(1, -50_00, month)

        val invoicesWithCorrections = applyCorrections(listOf(), month)
        assertEquals(0, invoicesWithCorrections.size)
    }

    private fun applyCorrections(invoice: Invoice, month: YearMonth): Invoice =
        applyCorrections(listOf(invoice), month).single()

    private fun applyCorrections(invoices: List<Invoice>, month: YearMonth): List<Invoice> =
        db.read { tx ->
            generator.applyCorrections(tx, invoices, month, mapOf(daycare.id to area.id)).shuffled()
        }

    private fun createTestInvoice(total: Int, month: YearMonth): Invoice =
        createInvoiceFixture(
            status = InvoiceStatus.DRAFT,
            headOfFamilyId = adult.id,
            areaId = area.id,
            period = FiniteDateRange.ofMonth(month),
            rows =
                listOf(
                    createInvoiceRowFixture(
                        childId = child.id,
                        unitId = daycare.id,
                        amount = 1,
                        unitPrice = total,
                    )
                ),
        )

    private fun insertAndSendInvoice(invoice: Invoice) {
        db.transaction { tx ->
            tx.insertInvoices(listOf(invoice))
            invoiceService.sendInvoices(
                tx,
                employee.evakaUserId,
                clock.now(),
                listOf(invoice.id),
                null,
                null,
            )
        }
    }

    private fun insertTestCorrection(
        amount: Int,
        unitPrice: Int,
        month: YearMonth,
    ): InvoiceCorrectionId =
        db.transaction {
            it.insert(
                DevInvoiceCorrection(
                    headOfFamilyId = adult.id,
                    targetMonth = null,
                    childId = child.id,
                    amount = amount,
                    unitPrice = unitPrice,
                    period = FiniteDateRange.ofMonth(month),
                    unitId = daycare.id,
                    product = productProvider.mapToProduct(PlacementType.DAYCARE),
                    description = "",
                    note = "",
                )
            )
        }

    private fun getUnappliedCorrection() = db.read { it.getUnappliedInvoiceCorrections() }.single()
}
