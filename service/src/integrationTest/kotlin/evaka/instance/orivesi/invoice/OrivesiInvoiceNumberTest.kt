// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.orivesi.invoice

import evaka.core.invoicing.data.getInvoicesByIds
import evaka.core.invoicing.domain.InvoiceStatus
import evaka.core.invoicing.service.InvoiceService
import evaka.core.shared.AreaId
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevInvoice
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.instance.orivesi.AbstractOrivesiIntegrationTest
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class OrivesiInvoiceNumberTest : AbstractOrivesiIntegrationTest() {
    @Autowired private lateinit var invoiceService: InvoiceService

    @Test
    fun `first invoice has correct number`() {
        val invoices = db.transaction { tx ->
            val employee = DevEmployee(roles = setOf(UserRole.FINANCE_ADMIN)).also { tx.insert(it) }
            val clock =
                MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2025, 1, 7), LocalTime.of(12, 34)))
            val headOfFamilyId = tx.insert(DevPerson(), DevPersonType.ADULT)
            val draftInvoice1 =
                tx.insert(
                    DevInvoice(
                        status = InvoiceStatus.DRAFT,
                        number = null,
                        invoiceDate = LocalDate.EPOCH,
                        dueDate = LocalDate.EPOCH,
                        periodStart = LocalDate.of(2024, 12, 1),
                        periodEnd = LocalDate.of(2024, 12, 31),
                        headOfFamilyId = headOfFamilyId,
                        areaId = AreaId(UUID.fromString("e463e978-4de3-44d2-ab5f-4ca883453385")),
                    )
                )
            val invoiceIds = listOf(draftInvoice1)
            val invoiceDate = LocalDate.of(2025, 1, 7)
            val dueDate = invoiceDate.plusWeeks(2)

            invoiceService.sendInvoices(
                tx,
                employee.evakaUserId,
                clock.now(),
                invoiceIds,
                invoiceDate,
                dueDate,
            )
            tx.getInvoicesByIds(invoiceIds)
        }

        assertThat(invoices).extracting<Long?> { it.number }.containsExactly(30000001)
    }

    @Test
    fun `second invoice has correct number`() {
        val invoices = db.transaction { tx ->
            val employee = DevEmployee(roles = setOf(UserRole.FINANCE_ADMIN)).also { tx.insert(it) }
            val clock =
                MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2025, 1, 7), LocalTime.of(12, 34)))
            val headOfFamilyId = tx.insert(DevPerson(), DevPersonType.ADULT)
            tx.insert(
                DevInvoice(
                    status = InvoiceStatus.SENT,
                    number = 30000001, // current series start
                    invoiceDate = LocalDate.of(2024, 12, 9),
                    dueDate = LocalDate.of(2024, 12, 23),
                    periodStart = LocalDate.of(2024, 11, 1),
                    periodEnd = LocalDate.of(2024, 11, 30),
                    headOfFamilyId = headOfFamilyId,
                    areaId = AreaId(UUID.fromString("e463e978-4de3-44d2-ab5f-4ca883453385")),
                )
            )
            val draftInvoice1 =
                tx.insert(
                    DevInvoice(
                        status = InvoiceStatus.DRAFT,
                        number = null,
                        invoiceDate = LocalDate.EPOCH,
                        dueDate = LocalDate.EPOCH,
                        periodStart = LocalDate.of(2024, 12, 1),
                        periodEnd = LocalDate.of(2024, 12, 31),
                        headOfFamilyId = headOfFamilyId,
                        areaId = AreaId(UUID.fromString("e463e978-4de3-44d2-ab5f-4ca883453385")),
                    )
                )
            val invoiceIds = listOf(draftInvoice1)
            val invoiceDate = LocalDate.of(2025, 1, 7)
            val dueDate = invoiceDate.plusWeeks(2)

            invoiceService.sendInvoices(
                tx,
                employee.evakaUserId,
                clock.now(),
                invoiceIds,
                invoiceDate,
                dueDate,
            )
            tx.getInvoicesByIds(invoiceIds)
        }

        assertThat(invoices).extracting<Long?> { it.number }.containsExactly(30000002)
    }

    @Test
    fun `first invoice with third series start has correct number`() {
        val invoices = db.transaction { tx ->
            val employee = DevEmployee(roles = setOf(UserRole.FINANCE_ADMIN)).also { tx.insert(it) }
            val clock =
                MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2025, 1, 7), LocalTime.of(12, 34)))
            val headOfFamilyId = tx.insert(DevPerson(), DevPersonType.ADULT)
            tx.insert(
                DevInvoice(
                    status = InvoiceStatus.SENT,
                    number = 152705, // first series start
                    invoiceDate = LocalDate.of(2024, 11, 11),
                    dueDate = LocalDate.of(2024, 11, 25),
                    periodStart = LocalDate.of(2024, 10, 1),
                    periodEnd = LocalDate.of(2024, 10, 31),
                    headOfFamilyId = headOfFamilyId,
                    areaId = AreaId(UUID.fromString("e463e978-4de3-44d2-ab5f-4ca883453385")),
                )
            )
            tx.insert(
                DevInvoice(
                    status = InvoiceStatus.SENT,
                    number = 54000001, // second series start
                    invoiceDate = LocalDate.of(2024, 12, 9),
                    dueDate = LocalDate.of(2024, 12, 23),
                    periodStart = LocalDate.of(2024, 11, 1),
                    periodEnd = LocalDate.of(2024, 11, 30),
                    headOfFamilyId = headOfFamilyId,
                    areaId = AreaId(UUID.fromString("e463e978-4de3-44d2-ab5f-4ca883453385")),
                )
            )
            val draftInvoice1 =
                tx.insert(
                    DevInvoice(
                        status = InvoiceStatus.DRAFT,
                        number = null,
                        invoiceDate = LocalDate.EPOCH,
                        dueDate = LocalDate.EPOCH,
                        periodStart = LocalDate.of(2024, 12, 1),
                        periodEnd = LocalDate.of(2024, 12, 31),
                        headOfFamilyId = headOfFamilyId,
                        areaId = AreaId(UUID.fromString("e463e978-4de3-44d2-ab5f-4ca883453385")),
                    )
                )
            val invoiceIds = listOf(draftInvoice1)
            val invoiceDate = LocalDate.of(2025, 1, 7)
            val dueDate = invoiceDate.plusWeeks(2)

            invoiceService.sendInvoices(
                tx,
                employee.evakaUserId,
                clock.now(),
                invoiceIds,
                invoiceDate,
                dueDate,
            )
            tx.getInvoicesByIds(invoiceIds)
        }

        assertThat(invoices).extracting<Long?> { it.number }.containsExactly(30000001)
    }
}
