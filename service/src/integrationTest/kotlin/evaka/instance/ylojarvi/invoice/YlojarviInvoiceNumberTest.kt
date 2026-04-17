// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.ylojarvi.invoice

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
import evaka.instance.ylojarvi.AbstractYlojarviIntegrationTest
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class YlojarviInvoiceNumberTest : AbstractYlojarviIntegrationTest() {
    @Autowired private lateinit var invoiceService: InvoiceService

    @Test
    fun `first invoice has correct number`() {
        val invoices =
            db.transaction { tx ->
                val employee =
                    DevEmployee(roles = setOf(UserRole.FINANCE_ADMIN)).also { tx.insert(it) }
                val clock =
                    MockEvakaClock(
                        HelsinkiDateTime.of(LocalDate.of(2025, 1, 7), LocalTime.of(12, 34))
                    )
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
                            areaId = AreaId(UUID.fromString("37ddb551-8913-44cd-94f4-17f9ee0fa8b9")),
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

        assertThat(invoices).extracting<Long?> { it.number }.containsExactly(1)
    }

    @Test
    fun `second invoice has correct number`() {
        val invoices =
            db.transaction { tx ->
                val employee =
                    DevEmployee(roles = setOf(UserRole.FINANCE_ADMIN)).also { tx.insert(it) }
                val clock =
                    MockEvakaClock(
                        HelsinkiDateTime.of(LocalDate.of(2025, 1, 7), LocalTime.of(12, 34))
                    )
                val headOfFamilyId = tx.insert(DevPerson(), DevPersonType.ADULT)
                tx.insert(
                    DevInvoice(
                        status = InvoiceStatus.SENT,
                        number = 1, // current series start
                        invoiceDate = LocalDate.of(2024, 12, 9),
                        dueDate = LocalDate.of(2024, 12, 23),
                        periodStart = LocalDate.of(2024, 11, 1),
                        periodEnd = LocalDate.of(2024, 11, 30),
                        headOfFamilyId = headOfFamilyId,
                        areaId = AreaId(UUID.fromString("37ddb551-8913-44cd-94f4-17f9ee0fa8b9")),
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
                            areaId = AreaId(UUID.fromString("37ddb551-8913-44cd-94f4-17f9ee0fa8b9")),
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

        assertThat(invoices).extracting<Long?> { it.number }.containsExactly(2)
    }
}
