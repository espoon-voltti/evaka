// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare

import fi.espoo.evaka.messaging.message.deactivateEmployeeMessageAccount
import fi.espoo.evaka.messaging.message.upsertEmployeeMessageAccount
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.DaycareAclRow
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.clearDaycareGroupAcl
import fi.espoo.evaka.shared.auth.deleteDaycareAclRow
import fi.espoo.evaka.shared.auth.getDaycareAclRows
import fi.espoo.evaka.shared.auth.hasDaycareAclRowForAnyUnit
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.db.Database

fun getDaycareAclRows(db: Database.Connection, daycareId: DaycareId): List<DaycareAclRow> {
    return db.read { it.getDaycareAclRows(daycareId) }
}

fun addUnitSupervisor(db: Database.Connection, daycareId: DaycareId, employeeId: EmployeeId) {
    db.transaction {
        it.clearDaycareGroupAcl(daycareId, employeeId)
        it.insertDaycareAclRow(daycareId.raw, employeeId.raw, UserRole.UNIT_SUPERVISOR)
        it.upsertEmployeeMessageAccount(employeeId.raw)
    }
}

fun removeUnitSupervisor(db: Database.Connection, daycareId: DaycareId, employeeId: EmployeeId) {
    db.transaction {
        it.deleteDaycareAclRow(daycareId, employeeId, UserRole.UNIT_SUPERVISOR)
        if (!it.hasDaycareAclRowForAnyUnit(employeeId, UserRole.UNIT_SUPERVISOR)) {
            // Deactivate the message account when the employee is not a supervisor in any unit anymore
            it.deactivateEmployeeMessageAccount(employeeId.raw)
        }
    }
}

fun addSpecialEducationTeacher(db: Database.Connection, daycareId: DaycareId, employeeId: EmployeeId) {
    db.transaction {
        it.clearDaycareGroupAcl(daycareId, employeeId)
        it.insertDaycareAclRow(daycareId.raw, employeeId.raw, UserRole.SPECIAL_EDUCATION_TEACHER)
    }
}

fun removeSpecialEducationTeacher(db: Database.Connection, daycareId: DaycareId, employeeId: EmployeeId) {
    db.transaction { it.deleteDaycareAclRow(daycareId, employeeId, UserRole.SPECIAL_EDUCATION_TEACHER) }
}

fun addStaffMember(db: Database.Connection, daycareId: DaycareId, employeeId: EmployeeId) {
    db.transaction { it.insertDaycareAclRow(daycareId.raw, employeeId.raw, UserRole.STAFF) }
}

fun removeStaffMember(db: Database.Connection, daycareId: DaycareId, employeeId: EmployeeId) {
    db.transaction {
        it.clearDaycareGroupAcl(daycareId, employeeId)
        it.deleteDaycareAclRow(daycareId, employeeId, UserRole.STAFF)
    }
}
