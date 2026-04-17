// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.daycare

import evaka.core.messaging.deactivateEmployeeMessageAccount
import evaka.core.shared.EmployeeId
import evaka.core.shared.auth.hasAnyDaycareAclRow
import evaka.core.shared.db.Database

fun deactivatePersonalMessageAccountIfNeeded(tx: Database.Transaction, employeeId: EmployeeId) {
    if (!tx.hasAnyDaycareAclRow(employeeId)) {
        // Deactivate the message account when the employee is not in any unit anymore
        tx.deactivateEmployeeMessageAccount(employeeId)
    }
}
