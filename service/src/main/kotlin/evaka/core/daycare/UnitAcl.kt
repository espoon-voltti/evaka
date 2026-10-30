// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.daycare

import evaka.core.messaging.deactivateEmployeeMessageAccount
import evaka.core.shared.EmployeeId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.hasAnyDaycareAclRow
import evaka.core.shared.db.Database
import evaka.core.shared.domain.HelsinkiDateTime

fun deactivatePersonalMessageAccountIfNeeded(tx: Database.Transaction, employeeId: EmployeeId) {
    if (!tx.hasAnyDaycareAclRow(employeeId)) {
        // Deactivate the message account when the employee is not in any unit anymore
        tx.deactivateEmployeeMessageAccount(employeeId)
    }
}

fun planPersonalMobileDeviceCleanup(
    tx: Database.Transaction,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    now: HelsinkiDateTime,
    employeeId: EmployeeId,
) {
    // Delete personal mobile devices after a while, in case the employee gets the permission
    // back, e.g. by being added back to this or some other unit
    asyncJobRunner.plan(
        tx,
        listOf(AsyncJob.DeletePersonalDevicesIfNeeded(employeeId)),
        runAt = now.plusHours(1),
    )
}
