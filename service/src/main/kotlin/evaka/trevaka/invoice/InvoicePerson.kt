// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.trevaka.invoice

import evaka.core.invoicing.domain.PersonDetailed

data class InvoicePerson(val name: String, val address: InvoiceAddress) {
    companion object {
        fun of(person: PersonDetailed, restrictedAddress: InvoiceAddress): InvoicePerson {
            val name =
                person.invoiceRecipientName.trim().ifEmpty {
                    "${person.lastName.trim()} ${person.firstName.trim()}".trim()
                }
            val address =
                if (person.restrictedDetailsEnabled) restrictedAddress else address(person)
            return InvoicePerson(name, address)
        }

        private fun address(person: PersonDetailed): InvoiceAddress {
            val invoicingStreetAddress = person.invoicingStreetAddress.trim()
            val invoicingPostalCode = person.invoicingPostalCode.trim()
            val invoicingPostOffice = person.invoicingPostOffice.trim()
            return if (
                invoicingStreetAddress.isNotEmpty() &&
                    invoicingPostalCode.isNotEmpty() &&
                    invoicingPostOffice.isNotEmpty()
            ) {
                InvoiceAddress(invoicingStreetAddress, invoicingPostalCode, invoicingPostOffice)
            } else {
                InvoiceAddress(
                    person.streetAddress.trim(),
                    person.postalCode.trim(),
                    person.postOffice.trim(),
                )
            }
        }
    }
}

data class InvoiceAddress(val streetAddress: String, val postalCode: String, val postOffice: String)
