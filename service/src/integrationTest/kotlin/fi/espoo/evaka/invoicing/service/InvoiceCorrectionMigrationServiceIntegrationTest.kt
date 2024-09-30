// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.TestInvoiceProductProvider
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevInvoice
import fi.espoo.evaka.shared.dev.DevInvoiceCorrection
import fi.espoo.evaka.shared.dev.DevInvoiceRow
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals

class InvoiceCorrectionMigrationServiceIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val productProvider: InvoiceProductProvider = TestInvoiceProductProvider()

    private val adult = DevPerson()
    private val child = DevPerson()
    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val product = productProvider.mapToProduct(PlacementType.DAYCARE)

    private val now = HelsinkiDateTime.of(LocalDate.of(2021, 6, 1), LocalTime.of(12, 0))
    private val clock = MockEvakaClock(now)

    private fun insertBaseData() {
        db.transaction { tx ->
            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(area)
            tx.insert(daycare)
        }
    }

    @Test
    fun `migrateInvoiceCorrection sets target month for simple completely applied correction`() {
        // given
        insertBaseData()
        val correctionReasonPeriod =
            FiniteDateRange(LocalDate.of(2021, 1, 15), LocalDate.of(2021, 1, 20))
        val invoiceMonth = YearMonth.of(2021, 3)
        val correction =
            DevInvoiceCorrection(
                headOfFamilyId = adult.id,
                childId = child.id,
                unitId = daycare.id,
                product = product,
                period = correctionReasonPeriod,
                amount = 2,
                unitPrice = 100,
                description = "description",
                note = "note",
                appliedCompletely = true,
            )
        val invoice =
            DevInvoice(
                periodStart = invoiceMonth.atDay(1),
                periodEnd = invoiceMonth.atEndOfMonth(),
                headOfFamily = adult.id,
                areaId = area.id,
            )
        val invoiceBasicRow =
            DevInvoiceRow(
                periodStart = invoiceMonth.atDay(1),
                periodEnd = invoiceMonth.atEndOfMonth(),
                child = child.id,
                amount = 30,
                unitPrice = 150,
                product = product,
                unitId = daycare.id,
                idx = 0,
            )
        val invoiceCorrectionRow = correction.toDevInvoiceRow(idx = 1)
        db.transaction { tx ->
            tx.insert(correction)
            tx.insert(invoice, listOf(invoiceBasicRow, invoiceCorrectionRow))
        }

        // when
        db.transaction { tx -> migrateInvoiceCorrection(tx, clock, correction.id) }

        // then
        val corrections = db.read { it.getAllInvoiceCorrections() }
        assertEquals(
            listOf(
                correction.copy(
                    id = corrections.first().id, // id may change
                    targetMonth = invoiceMonth.atDay(1),
                    appliedCompletely = false, // no longer used in migrated corrections
                )
            ),
            corrections,
        )
        val invoiceRows = db.read { it.getAllInvoiceRows() }
        assertEquals(
            listOf(
                invoiceBasicRow,
                invoiceCorrectionRow.copy(correctionId = corrections.first().id),
            ),
            invoiceRows,
        )
    }

    @Test
    fun `migrateInvoiceCorrection splits correction and assigns target months`() {
        // given
        insertBaseData()
        val correctionReasonPeriod =
            FiniteDateRange(LocalDate.of(2021, 1, 15), LocalDate.of(2021, 1, 20))
        val invoiceMonth1 = YearMonth.of(2021, 3)
        val invoiceMonth2 = YearMonth.of(2021, 4)
        val nextMonth = YearMonth.of(2021, 7)
        val correction =
            DevInvoiceCorrection(
                headOfFamilyId = adult.id,
                childId = child.id,
                unitId = daycare.id,
                product = product,
                period = correctionReasonPeriod,
                amount = 10,
                unitPrice = -200,
                description = "description",
                note = "note",
                appliedCompletely = false,
            )
        val invoice1 =
            DevInvoice(
                periodStart = invoiceMonth1.atDay(1),
                periodEnd = invoiceMonth1.atEndOfMonth(),
                headOfFamily = adult.id,
                areaId = area.id,
            )
        val invoice1BasicRow =
            DevInvoiceRow(
                periodStart = invoiceMonth1.atDay(1),
                periodEnd = invoiceMonth1.atEndOfMonth(),
                child = child.id,
                amount = 1,
                unitPrice = 700,
                product = product,
                unitId = daycare.id,
                idx = 0,
            )
        val invoice1CorrectionRow =
            correction.toDevInvoiceRow(idx = 1).copy(amount = 3, unitPrice = -200)
        val invoice2 =
            DevInvoice(
                periodStart = invoiceMonth2.atDay(1),
                periodEnd = invoiceMonth2.atEndOfMonth(),
                headOfFamily = adult.id,
                areaId = area.id,
            )
        val invoice2BasicRow =
            DevInvoiceRow(
                periodStart = invoiceMonth2.atDay(1),
                periodEnd = invoiceMonth2.atEndOfMonth(),
                child = child.id,
                amount = 1,
                unitPrice = 150,
                product = product,
                unitId = daycare.id,
                idx = 0,
            )
        val invoice2CorrectionRow =
            correction.toDevInvoiceRow(idx = 1).copy(amount = 1, unitPrice = -150)

        // correction left = 10 * 200 - 3 * 200 - 1 * 150 = 1250

        db.transaction { tx ->
            tx.insert(correction)
            tx.insert(invoice1, listOf(invoice1BasicRow, invoice1CorrectionRow))
            tx.insert(invoice2, listOf(invoice2BasicRow, invoice2CorrectionRow))
        }

        // when
        db.transaction { tx -> migrateInvoiceCorrection(tx, clock, correction.id) }

        // then
        val corrections = db.read { it.getAllInvoiceCorrections() }
        assertEquals(
            listOf(
                correction.copy(
                    id = corrections[0].id,
                    targetMonth = invoiceMonth1.atDay(1),
                    amount = 3,
                    unitPrice = -200,
                    appliedCompletely = false,
                ),
                correction.copy(
                    id = corrections[1].id,
                    targetMonth = invoiceMonth2.atDay(1),
                    amount = 1,
                    unitPrice = -150,
                    appliedCompletely = false,
                ),
                correction.copy(
                    id = corrections[2].id,
                    targetMonth = nextMonth.atDay(1),
                    appliedCompletely = false,
                    amount = 1,
                    unitPrice = -1250,
                ),
            ),
            corrections,
        )
        val invoiceRows = db.read { it.getAllInvoiceRows() }
        assertEquals(
            listOf(
                invoice1BasicRow,
                invoice1CorrectionRow.copy(correctionId = corrections[0].id),
                invoice2BasicRow,
                invoice2CorrectionRow.copy(correctionId = corrections[1].id),
            ),
            invoiceRows,
        )
    }

    @Test
    fun `migrateInvoiceCorrection retains unit price when it divides evenly`() {
        // given
        insertBaseData()
        val correctionReasonPeriod =
            FiniteDateRange(LocalDate.of(2021, 1, 15), LocalDate.of(2021, 1, 20))
        val invoiceMonth = YearMonth.of(2021, 3)
        val nextMonth = YearMonth.of(2021, 7)
        val correction =
            DevInvoiceCorrection(
                headOfFamilyId = adult.id,
                childId = child.id,
                unitId = daycare.id,
                product = product,
                period = correctionReasonPeriod,
                amount = 10,
                unitPrice = -200,
                description = "description",
                note = "note",
                appliedCompletely = false,
            )
        val invoice =
            DevInvoice(
                periodStart = invoiceMonth.atDay(1),
                periodEnd = invoiceMonth.atEndOfMonth(),
                headOfFamily = adult.id,
                areaId = area.id,
            )
        val invoiceBasicRow =
            DevInvoiceRow(
                periodStart = invoiceMonth.atDay(1),
                periodEnd = invoiceMonth.atEndOfMonth(),
                child = child.id,
                amount = 1,
                unitPrice = 600,
                product = product,
                unitId = daycare.id,
                idx = 0,
            )
        val invoiceCorrectionRow =
            correction.toDevInvoiceRow(idx = 1).copy(amount = 3, unitPrice = -200)

        // correction left = 10 * 200 - 3 * 200 = 7 * 200

        db.transaction { tx ->
            tx.insert(correction)
            tx.insert(invoice, listOf(invoiceBasicRow, invoiceCorrectionRow))
        }

        // when
        db.transaction { tx -> migrateInvoiceCorrection(tx, clock, correction.id) }

        // then
        val corrections = db.read { it.getAllInvoiceCorrections() }
        assertEquals(
            listOf(
                correction.copy(
                    id = corrections[0].id,
                    targetMonth = invoiceMonth.atDay(1),
                    amount = 3,
                    unitPrice = -200,
                    appliedCompletely = false,
                ),
                correction.copy(
                    id = corrections[1].id,
                    targetMonth = nextMonth.atDay(1),
                    appliedCompletely = false,
                    amount = 7,
                    unitPrice = -200,
                ),
            ),
            corrections,
        )
    }
}

private fun Database.Read.getAllInvoiceCorrections(): List<DevInvoiceCorrection> {
    return createQuery { sql("SELECT * FROM invoice_correction ORDER BY target_month") }
        .toList<DevInvoiceCorrection>()
}

private fun Database.Read.getAllInvoiceRows(): List<DevInvoiceRow> {
    return createQuery {
            sql(
                """
        SELECT ir.* 
        FROM invoice_row ir
        JOIN invoice i ON ir.invoice_id = i.id
        ORDER BY i.period_start, ir.idx
    """
            )
        }
        .toList<DevInvoiceRow>()
}
