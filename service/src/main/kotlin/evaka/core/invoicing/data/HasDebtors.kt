// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.data

import evaka.core.shared.PersonId

interface HasDebtors {
    val headOfFamilyId: PersonId
    val partnerId: PersonId?

    // null means unknown, i.e. the decision is old and the status was not recorded
    val partnerIsCodebtor: Boolean?

    fun debtors() = listOfNotNull(headOfFamilyId, partnerId?.takeIf { partnerIsCodebtor != false })
}
