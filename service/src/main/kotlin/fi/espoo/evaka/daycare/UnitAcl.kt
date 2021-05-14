// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare

import fi.espoo.evaka.messaging.message.deactivateEmployeeMessageAccount
import fi.espoo.evaka.messaging.message.upsertMessageAccountForEmployee
import fi.espoo.evaka.shared.auth.DaycareAclRow
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.deleteDaycareAclRow
import fi.espoo.evaka.shared.auth.getDaycareAclRows
import fi.espoo.evaka.shared.auth.hasDaycareAclRowForAnyUnit
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.db.Database
import java.util.UUID

fun getDaycareAclRows(db: Database.Connection, daycareId: UUID): List<DaycareAclRow> {
    return db.read { it.getDaycareAclRows(daycareId) }
}

fun addUnitSupervisor(db: Database.Connection, daycareId: UUID, employeeId: UUID) {
    db.transaction {
        it.insertDaycareAclRow(daycareId, employeeId, UserRole.UNIT_SUPERVISOR)
        it.upsertMessageAccountForEmployee(employeeId)
    }
}

fun removeUnitSupervisor(db: Database.Connection, daycareId: UUID, employeeId: UUID) {
    db.transaction {
        it.deleteDaycareAclRow(daycareId, employeeId, UserRole.UNIT_SUPERVISOR)
        if (!it.hasDaycareAclRowForAnyUnit(employeeId, UserRole.UNIT_SUPERVISOR)) {
            // Deactivate the message account when the employee is not a supervisor in any unit anymore
            it.deactivateEmployeeMessageAccount(employeeId)
        }
    }
}

fun addSpecialEducationTeacher(db: Database.Connection, daycareId: UUID, employeeId: UUID) {
    db.transaction { it.insertDaycareAclRow(daycareId, employeeId, UserRole.SPECIAL_EDUCATION_TEACHER) }
}

fun removeSpecialEducationTeacher(db: Database.Connection, daycareId: UUID, employeeId: UUID) {
    db.transaction { it.deleteDaycareAclRow(daycareId, employeeId, UserRole.SPECIAL_EDUCATION_TEACHER) }
}

fun addStaffMember(db: Database.Connection, daycareId: UUID, employeeId: UUID) {
    db.transaction { it.insertDaycareAclRow(daycareId, employeeId, UserRole.STAFF) }
}

fun removeStaffMember(db: Database.Connection, daycareId: UUID, employeeId: UUID) {
    db.transaction { it.deleteDaycareAclRow(daycareId, employeeId, UserRole.STAFF) }
}
