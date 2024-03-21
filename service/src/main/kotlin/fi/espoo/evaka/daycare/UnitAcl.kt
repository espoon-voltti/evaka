// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare

import fi.espoo.evaka.messaging.deactivateEmployeeMessageAccount
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.clearDaycareGroupAcl
import fi.espoo.evaka.shared.auth.deleteDaycareAclRow
import fi.espoo.evaka.shared.auth.hasAnyDaycareAclRow
import fi.espoo.evaka.shared.db.Database

fun removeDaycareAclForRole(
    tx: Database.Transaction,
    daycareId: DaycareId,
    employeeId: EmployeeId,
    role: UserRole
) {
    tx.clearDaycareGroupAcl(daycareId, employeeId)
    tx.deleteDaycareAclRow(daycareId, employeeId, role)
    deactivatePersonalMessageAccountIfNeeded(tx, employeeId)
}

fun deactivatePersonalMessageAccountIfNeeded(tx: Database.Transaction, employeeId: EmployeeId) {
    if (!tx.hasAnyDaycareAclRow(employeeId)) {
        // Deactivate the message account when the employee is not in any unit anymore
        tx.deactivateEmployeeMessageAccount(employeeId)
    }
}
