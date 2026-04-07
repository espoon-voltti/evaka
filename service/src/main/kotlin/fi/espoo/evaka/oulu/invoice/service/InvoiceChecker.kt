// SPDX-FileCopyrightText: 2023-2025 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.oulu.invoice.service

import fi.espoo.evaka.invoicing.domain.InvoiceDetailed
import org.springframework.stereotype.Component

@Component
class InvoiceChecker {
    fun shouldSendManually(invoice: InvoiceDetailed): Boolean {
        if (invoice.headOfFamily.restrictedDetailsEnabled) return true
        if (invoice.headOfFamily.ssn == null) return true
        return false
    }
}
