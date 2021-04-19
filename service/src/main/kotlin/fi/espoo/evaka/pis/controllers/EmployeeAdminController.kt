// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.pis.EmployeeWithDaycareRoles
import fi.espoo.evaka.pis.getEmployeesWithDaycareRoles
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/employee/admin")
class EmployeeAdminController {

    @GetMapping
    fun getEmployees(db: Database.Connection, user: AuthenticatedUser): ResponseEntity<List<EmployeeWithDaycareRoles>> {
        Audit.EmployeesRead.log()
        user.requireOneOfRoles(UserRole.ADMIN)
        return ResponseEntity.ok(db.read { it.handle.getEmployeesWithDaycareRoles() })
    }
}
