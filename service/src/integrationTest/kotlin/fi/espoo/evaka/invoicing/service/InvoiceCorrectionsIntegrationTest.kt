// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.TestInvoiceProductProvider
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.createInvoiceFixture
import fi.espoo.evaka.invoicing.createInvoiceRowFixture
import fi.espoo.evaka.invoicing.data.upsertInvoices
import fi.espoo.evaka.invoicing.domain.Invoice
import fi.espoo.evaka.invoicing.domain.InvoiceStatus
import fi.espoo.evaka.invoicing.integration.InvoiceIntegrationClient
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.InvoiceCorrectionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.config.defaultJsonMapper
import fi.espoo.evaka.shared.config.testFeatureConfig
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.dev.DevInvoiceCorrection
import fi.espoo.evaka.shared.dev.insertTestInvoiceCorrection
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class InvoiceCorrectionsIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val productProvider: InvoiceProductProvider = TestInvoiceProductProvider()
    private val featureConfig: FeatureConfig = testFeatureConfig
    private val draftInvoiceGenerator: DraftInvoiceGenerator =
        DraftInvoiceGenerator(productProvider, featureConfig)
    private val generator: InvoiceGenerator = InvoiceGenerator(draftInvoiceGenerator)
    private val invoiceService =
        InvoiceService(InvoiceIntegrationClient.MockClient(defaultJsonMapper()), TestInvoiceProductProvider())

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
        }
    }

    @Test
    fun `refund totaling one month's invoice is refunded completely`() {
        val invoice = createTestInvoice(100_00)
        val correctionId = insertTestCorrection(1, -100_00)

        val invoiceWithCorrections = db.read { it.applyCorrections(listOf(invoice)) }.first()
        assertEquals(2, invoiceWithCorrections.rows.size)
        assertEquals(0, invoiceWithCorrections.totalPrice)
        assertEquals(1, invoiceWithCorrections.rows.first().amount)
        assertEquals(100_00, invoiceWithCorrections.rows.first().unitPrice)
        assertEquals(1, invoiceWithCorrections.rows.last().amount)
        assertEquals(-100_00, invoiceWithCorrections.rows.last().unitPrice)
        assertEquals(correctionId, invoiceWithCorrections.rows.last().correctionId)
    }

    @Test
    fun `refund with smaller unit price totaling one month's invoice is refunded completely`() {
        val invoice = createTestInvoice(100_00)
        val correctionId = insertTestCorrection(5, -20_00)

        val invoiceWithCorrections = db.read { it.applyCorrections(listOf(invoice)) }.first()
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
        val correctionId = insertTestCorrection(5, -40_00)

        val firstPeriod = FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31))
        val firstInvoice =
            db.read { it.applyCorrections(listOf(createTestInvoice(100_00, firstPeriod)), firstPeriod) }.first()
        assertEquals(2, firstInvoice.rows.size)
        assertEquals(20_00, firstInvoice.totalPrice)
        assertEquals(1, firstInvoice.rows.first().amount)
        assertEquals(100_00, firstInvoice.rows.first().unitPrice)
        assertEquals(2, firstInvoice.rows.last().amount)
        assertEquals(-40_00, firstInvoice.rows.last().unitPrice)
        assertEquals(correctionId, firstInvoice.rows.last().correctionId)
        db.transaction { it.upsertInvoices(listOf(firstInvoice.copy(status = InvoiceStatus.SENT))) }

        val secondPeriod = firstPeriod.getNextMonth()
        val secondInvoice =
            db.read { it.applyCorrections(listOf(createTestInvoice(100_00, secondPeriod)), secondPeriod) }.first()
        assertEquals(2, secondInvoice.rows.size)
        assertEquals(20_00, secondInvoice.totalPrice)
        assertEquals(1, secondInvoice.rows.first().amount)
        assertEquals(100_00, secondInvoice.rows.first().unitPrice)
        assertEquals(2, secondInvoice.rows.last().amount)
        assertEquals(-40_00, secondInvoice.rows.last().unitPrice)
        assertEquals(correctionId, secondInvoice.rows.last().correctionId)
        db.transaction { it.upsertInvoices(listOf(secondInvoice.copy(status = InvoiceStatus.SENT))) }

        val thirdPeriod = secondPeriod.getNextMonth()
        val thirdInvoice =
            db.read { it.applyCorrections(listOf(createTestInvoice(100_00, thirdPeriod)), thirdPeriod) }.first()
        assertEquals(2, thirdInvoice.rows.size)
        assertEquals(60_00, thirdInvoice.totalPrice)
        assertEquals(1, thirdInvoice.rows.first().amount)
        assertEquals(100_00, thirdInvoice.rows.first().unitPrice)
        assertEquals(1, thirdInvoice.rows.last().amount)
        assertEquals(-40_00, thirdInvoice.rows.last().unitPrice)
        assertEquals(correctionId, thirdInvoice.rows.last().correctionId)
        db.transaction { it.upsertInvoices(listOf(thirdInvoice.copy(status = InvoiceStatus.SENT))) }
    }

    @Test
    fun `refund with unit price over one month's invoice is split over several invoices`() {
        val correctionId = insertTestCorrection(1, -250_00)

        val firstPeriod = FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31))
        val firstInvoice =
            db.read { it.applyCorrections(listOf(createTestInvoice(100_00, firstPeriod)), firstPeriod) }.first()
        assertEquals(2, firstInvoice.rows.size)
        assertEquals(0, firstInvoice.totalPrice)
        assertEquals(1, firstInvoice.rows.first().amount)
        assertEquals(100_00, firstInvoice.rows.first().unitPrice)
        assertEquals(1, firstInvoice.rows.last().amount)
        assertEquals(-100_00, firstInvoice.rows.last().unitPrice)
        assertEquals(correctionId, firstInvoice.rows.last().correctionId)
        db.transaction { it.upsertInvoices(listOf(firstInvoice.copy(status = InvoiceStatus.SENT))) }

        val secondPeriod = firstPeriod.getNextMonth()
        val secondInvoice =
            db.read { it.applyCorrections(listOf(createTestInvoice(100_00, secondPeriod)), secondPeriod) }.first()
        assertEquals(2, secondInvoice.rows.size)
        assertEquals(0, secondInvoice.totalPrice)
        assertEquals(1, secondInvoice.rows.first().amount)
        assertEquals(100_00, secondInvoice.rows.first().unitPrice)
        assertEquals(1, secondInvoice.rows.last().amount)
        assertEquals(-100_00, secondInvoice.rows.last().unitPrice)
        assertEquals(correctionId, secondInvoice.rows.last().correctionId)
        db.transaction { it.upsertInvoices(listOf(secondInvoice.copy(status = InvoiceStatus.SENT))) }

        val thirdPeriod = secondPeriod.getNextMonth()
        val thirdInvoice =
            db.read { it.applyCorrections(listOf(createTestInvoice(100_00, thirdPeriod)), thirdPeriod) }.first()
        assertEquals(2, thirdInvoice.rows.size)
        assertEquals(50_00, thirdInvoice.totalPrice)
        assertEquals(1, thirdInvoice.rows.first().amount)
        assertEquals(100_00, thirdInvoice.rows.first().unitPrice)
        assertEquals(1, thirdInvoice.rows.last().amount)
        assertEquals(-50_00, thirdInvoice.rows.last().unitPrice)
        assertEquals(correctionId, thirdInvoice.rows.last().correctionId)
        db.transaction { it.upsertInvoices(listOf(thirdInvoice.copy(status = InvoiceStatus.SENT))) }
    }

    @Test
    fun `increase over one month's invoice is applied`() {
        val invoice = createTestInvoice(100_00)
        val correctionId = insertTestCorrection(1, 500_00)

        val invoiceWithCorrections = db.read { it.applyCorrections(listOf(invoice)) }.first()
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
        val firstCorrectionPeriod = FiniteDateRange(LocalDate.of(2019, 1, 1,), LocalDate.of(2019, 1, 31))
        val firstCorrectionId = insertTestCorrection(1, -70_00, firstCorrectionPeriod)
        val secondCorrectionPeriod = FiniteDateRange(LocalDate.of(2019, 2, 1,), LocalDate.of(2019, 2, 28))
        val secondCorrectionId = insertTestCorrection(1, -80_00, secondCorrectionPeriod)

        val firstPeriod = FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31))
        val firstInvoice =
            db.read { it.applyCorrections(listOf(createTestInvoice(100_00, firstPeriod)), firstPeriod) }.first()
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
        db.transaction { it.upsertInvoices(listOf(firstInvoice.copy(status = InvoiceStatus.SENT))) }

        val secondPeriod = firstPeriod.getNextMonth()
        val secondInvoice =
            db.read { it.applyCorrections(listOf(createTestInvoice(100_00, secondPeriod)), secondPeriod) }.first()
        assertEquals(2, secondInvoice.rows.size)
        assertEquals(50_00, secondInvoice.totalPrice)
        assertEquals(1, secondInvoice.rows.first().amount)
        assertEquals(100_00, secondInvoice.rows.first().unitPrice)
        assertEquals(1, secondInvoice.rows.last().amount)
        assertEquals(-50_00, secondInvoice.rows.last().unitPrice)
        assertEquals(secondCorrectionId, secondInvoice.rows.last().correctionId)
        db.transaction { it.upsertInvoices(listOf(secondInvoice.copy(status = InvoiceStatus.SENT))) }
    }

    @Test
    fun `large increase makes room for multiple refunds`() {
        val increaseCorrectionId = insertTestCorrection(1, 200_00)
        val firstRefundCorrectionId = insertTestCorrection(1, -70_00)
        val secondRefundCorrectionId = insertTestCorrection(1, -80_00)

        val firstInvoice = db.read { it.applyCorrections(listOf(createTestInvoice(100_00))) }.first()
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
        val correctionId = insertTestCorrection(5, -50_00)

        val firstInvoice = db.read { it.applyCorrections(listOf(createTestInvoice(100_00))) }.first()
        assertEquals(2, firstInvoice.rows.size)
        assertEquals(0, firstInvoice.totalPrice)
        assertEquals(1, firstInvoice.rows.first().amount)
        assertEquals(100_00, firstInvoice.rows.first().unitPrice)
        assertEquals(2, firstInvoice.rows.last().amount)
        assertEquals(-50_00, firstInvoice.rows.last().unitPrice)
        assertEquals(correctionId, firstInvoice.rows.last().correctionId)
        db.transaction { it.upsertInvoices(listOf(firstInvoice.copy(status = InvoiceStatus.SENT))) }

        val secondInvoice = db.read { it.applyCorrections(listOf(createTestInvoice(30_00))) }.first()
        assertEquals(2, secondInvoice.rows.size)
        assertEquals(0, secondInvoice.totalPrice)
        assertEquals(1, secondInvoice.rows.first().amount)
        assertEquals(30_00, secondInvoice.rows.first().unitPrice)
        assertEquals(3, secondInvoice.rows.last().amount)
        assertEquals(-10_00, secondInvoice.rows.last().unitPrice)
        assertEquals(correctionId, secondInvoice.rows.last().correctionId)
        db.transaction { it.upsertInvoices(listOf(secondInvoice.copy(status = InvoiceStatus.SENT))) }

        val thirdInvoice = db.read { it.applyCorrections(listOf(createTestInvoice(100_00))) }.first()
        assertEquals(2, thirdInvoice.rows.size)
        assertEquals(20_00, thirdInvoice.totalPrice)
        assertEquals(1, thirdInvoice.rows.first().amount)
        assertEquals(100_00, thirdInvoice.rows.first().unitPrice)
        assertEquals(2, thirdInvoice.rows.last().amount)
        assertEquals(-40_00, thirdInvoice.rows.last().unitPrice)
        assertEquals(correctionId, thirdInvoice.rows.last().correctionId)
        db.transaction { it.upsertInvoices(listOf(thirdInvoice.copy(status = InvoiceStatus.SENT))) }

        val fourthInvoice = db.read { it.applyCorrections(listOf(createTestInvoice(100_00))) }.first()
        assertEquals(2, fourthInvoice.rows.size)
        assertEquals(60_00, fourthInvoice.totalPrice)
        assertEquals(1, fourthInvoice.rows.first().amount)
        assertEquals(100_00, fourthInvoice.rows.first().unitPrice)
        assertEquals(1, fourthInvoice.rows.last().amount)
        assertEquals(-40_00, fourthInvoice.rows.last().unitPrice)
        assertEquals(correctionId, fourthInvoice.rows.last().correctionId)
        db.transaction { it.upsertInvoices(listOf(fourthInvoice.copy(status = InvoiceStatus.SENT))) }
    }

    @Test
    fun `correction is marked as applied when its invoice is sent`() {
        val correctionId = insertTestCorrection(1, -50_00)
        db.transaction {
            val invoices = it.applyCorrections(listOf(createTestInvoice(100_00)))
            it.upsertInvoices(invoices)
            invoiceService.sendInvoices(
                it,
                AuthenticatedUser.Employee(testDecisionMaker_1.id.raw, setOf(UserRole.FINANCE_ADMIN)),
                invoices.map { it.id },
                null,
                null
            )
        }

        val result = db.read {
            it.createQuery("SELECT id, applied_completely FROM invoice_correction")
                .map { rv ->
                    rv.mapColumn<InvoiceCorrectionId>("id") to rv.mapColumn<Boolean>("applied_completely")
                }
                .toList()
        }
        assertEquals(1, result.size)
        assertEquals(correctionId, result.first().first)
        assertEquals(true, result.first().second)
    }

    @Test
    fun `correction is not marked as applied when its invoice is sent if the correction hasn't been applied completely`() {
        val correctionId = insertTestCorrection(1, -200_00)
        db.transaction {
            val invoices = it.applyCorrections(listOf(createTestInvoice(100_00)))
            it.upsertInvoices(invoices)
            invoiceService.sendInvoices(
                it,
                AuthenticatedUser.Employee(testDecisionMaker_1.id.raw, setOf(UserRole.FINANCE_ADMIN)),
                invoices.map { it.id },
                null,
                null
            )
        }

        val result = db.read {
            it.createQuery("SELECT id, applied_completely FROM invoice_correction")
                .map { rv ->
                    rv.mapColumn<InvoiceCorrectionId>("id") to rv.mapColumn<Boolean>("applied_completely")
                }
                .toList()
        }
        assertEquals(1, result.size)
        assertEquals(correctionId, result.first().first)
        assertEquals(false, result.first().second)
    }

    @Test
    fun `corrections without a matching invoice are included as new invoices`() {
        val refundId = insertTestCorrection(1, -50_00)
        val increaseId = insertTestCorrection(1, 100_00)

        val invoiceWithCorrections = db.read { it.applyCorrections(listOf()) }.first()
        assertEquals(2, invoiceWithCorrections.rows.size)
        assertEquals(50_00, invoiceWithCorrections.totalPrice)
        assertEquals(1, invoiceWithCorrections.rows.first().amount)
        assertEquals(100_00, invoiceWithCorrections.rows.first().unitPrice)
        assertEquals(increaseId, invoiceWithCorrections.rows.first().correctionId)
        assertEquals(1, invoiceWithCorrections.rows.last().amount)
        assertEquals(-50_00, invoiceWithCorrections.rows.last().unitPrice)
        assertEquals(refundId, invoiceWithCorrections.rows.last().correctionId)
    }

    private fun Database.Read.applyCorrections(
        invoices: List<Invoice>,
        period: FiniteDateRange = FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31))
    ): List<Invoice> {
        return generator.applyCorrections(this, invoices, period.asDateRange(), mapOf(testDaycare.id to testArea.id))
    }

    private fun createTestInvoice(
        total: Int,
        period: FiniteDateRange = FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31))
    ): Invoice {
        return createInvoiceFixture(
            status = InvoiceStatus.DRAFT,
            headOfFamilyId = testAdult_1.id,
            areaId = testArea.id,
            period = period,
            rows = listOf(
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
        period: FiniteDateRange = FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31))
    ): InvoiceCorrectionId {
        return db.transaction {
            it.insertTestInvoiceCorrection(
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

    private fun FiniteDateRange.getNextMonth() =
        this.copy(start = this.start.plusMonths(1).withDayOfMonth(1), end = this.start.plusMonths(2).withDayOfMonth(1))
}
