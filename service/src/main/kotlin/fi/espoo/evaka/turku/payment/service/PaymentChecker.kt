// SPDX-FileCopyrightText: 2023-2025 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.turku.payment.service

import fi.espoo.evaka.invoicing.domain.Payment
import org.springframework.stereotype.Component

@Component
class PaymentChecker {
    fun shouldFail(payment: Payment): Boolean {
        if (payment.unit.iban == null) return true
        if (payment.unit.businessId == null) return true
        if (payment.unit.providerId == null) return true
        if (payment.amount <= 0) return true
        return false
    }
}
