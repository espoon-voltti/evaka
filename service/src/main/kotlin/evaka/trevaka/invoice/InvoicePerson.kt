// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.trevaka.invoice

import evaka.core.invoicing.domain.InvoiceAddress
import evaka.core.invoicing.domain.PersonDetailed

data class InvoicePerson(val name: String, val address: InvoiceAddress) {
    companion object {
        fun of(person: PersonDetailed, restrictedAddress: InvoiceAddress): InvoicePerson {
            val name =
                person.invoiceRecipientName.trim().ifEmpty {
                    "${person.lastName.trim()} ${person.firstName.trim()}".trim()
                }
            val address =
                if (person.restrictedDetailsEnabled) restrictedAddress else person.invoiceAddress()
            return InvoicePerson(name, address)
        }
    }
}
