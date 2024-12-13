// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare

import fi.espoo.evaka.messaging.deactivateEmployeeMessageAccount
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.hasAnyDaycareAclRow
import fi.espoo.evaka.shared.db.Database

fun deactivatePersonalMessageAccountIfNeeded(tx: Database.Transaction, employeeId: EmployeeId) {
    if (!tx.hasAnyDaycareAclRow(employeeId)) {
        // Deactivate the message account when the employee is not in any unit anymore
        tx.deactivateEmployeeMessageAccount(employeeId)
    }
}
