// SPDX-FileCopyrightText: 2023-2025 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.oulu.payment.service

import fi.espoo.evaka.invoicing.domain.Payment
import org.springframework.stereotype.Component

@Component
class PaymentChecker {
    fun shouldFail(payment: Payment): Boolean {
        if (payment.unit.iban == null) return true
        if (payment.unit.businessId == null) return true
        if (payment.unit.providerId == null) return true
        return false
    }
}
