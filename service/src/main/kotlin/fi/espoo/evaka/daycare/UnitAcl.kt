// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare

import fi.espoo.evaka.messaging.deactivateEmployeeMessageAccount
import fi.espoo.evaka.messaging.upsertEmployeeMessageAccount
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.DaycareAclRow
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.clearDaycareGroupAcl
import fi.espoo.evaka.shared.auth.deleteDaycareAclRow
import fi.espoo.evaka.shared.auth.getDaycareAclRows
import fi.espoo.evaka.shared.auth.hasAnyDaycareAclRow
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.db.Database

fun getDaycareAclRows(db: Database.Connection, daycareId: DaycareId): List<DaycareAclRow> {
    return db.read { it.getDaycareAclRows(daycareId) }
}

fun addUnitSupervisor(db: Database.Connection, daycareId: DaycareId, employeeId: EmployeeId) {
    db.transaction {
        it.clearDaycareGroupAcl(daycareId, employeeId)
        it.insertDaycareAclRow(daycareId, employeeId, UserRole.UNIT_SUPERVISOR)
        it.upsertEmployeeMessageAccount(employeeId)
    }
}

fun removeUnitSupervisor(db: Database.Connection, daycareId: DaycareId, employeeId: EmployeeId) {
    db.transaction {
        it.deleteDaycareAclRow(daycareId, employeeId, UserRole.UNIT_SUPERVISOR)
        deactivatePersonalMessageAccountIfNeeded(it, employeeId)
    }
}

fun addSpecialEducationTeacher(
    db: Database.Connection,
    daycareId: DaycareId,
    employeeId: EmployeeId
) {
    db.transaction {
        it.clearDaycareGroupAcl(daycareId, employeeId)
        it.insertDaycareAclRow(daycareId, employeeId, UserRole.SPECIAL_EDUCATION_TEACHER)
        it.upsertEmployeeMessageAccount(employeeId)
    }
}

fun removeSpecialEducationTeacher(
    db: Database.Connection,
    daycareId: DaycareId,
    employeeId: EmployeeId
) {
    db.transaction {
        it.deleteDaycareAclRow(daycareId, employeeId, UserRole.SPECIAL_EDUCATION_TEACHER)
        deactivatePersonalMessageAccountIfNeeded(it, employeeId)
    }
}

fun addEarlyChildhoodEducationSecretary(
    db: Database.Connection,
    daycareId: DaycareId,
    employeeId: EmployeeId
) {
    db.transaction {
        it.clearDaycareGroupAcl(daycareId, employeeId)
        it.insertDaycareAclRow(daycareId, employeeId, UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY)
        it.upsertEmployeeMessageAccount(employeeId)
    }
}

fun removeEarlyChildhoodEducationSecretary(
    db: Database.Connection,
    daycareId: DaycareId,
    employeeId: EmployeeId
) {
    db.transaction {
        it.clearDaycareGroupAcl(daycareId, employeeId)
        it.deleteDaycareAclRow(daycareId, employeeId, UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY)
        deactivatePersonalMessageAccountIfNeeded(it, employeeId)
    }
}

fun addStaffMember(db: Database.Connection, daycareId: DaycareId, employeeId: EmployeeId) {
    db.transaction {
        it.insertDaycareAclRow(daycareId, employeeId, UserRole.STAFF)
        it.upsertEmployeeMessageAccount(employeeId)
    }
}

fun removeStaffMember(db: Database.Connection, daycareId: DaycareId, employeeId: EmployeeId) {
    db.transaction {
        it.clearDaycareGroupAcl(daycareId, employeeId)
        it.deleteDaycareAclRow(daycareId, employeeId, UserRole.STAFF)
        deactivatePersonalMessageAccountIfNeeded(it, employeeId)
    }
}

private fun deactivatePersonalMessageAccountIfNeeded(
    tx: Database.Transaction,
    employeeId: EmployeeId
) {
    if (!tx.hasAnyDaycareAclRow(employeeId)) {
        // Deactivate the message account when the employee is not in any unit anymore
        tx.deactivateEmployeeMessageAccount(employeeId)
    }
}
