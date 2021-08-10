// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.Audit
import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pairing.MobileDeviceIdentity
import fi.espoo.evaka.pairing.getDeviceByToken
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class SystemIdentityController(private val personService: PersonService) {
    @PostMapping("/system/person-identity")
    fun personIdentity(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody person: PersonIdentityRequest
    ): ResponseEntity<AuthenticatedUser> {
        Audit.PersonCreate.log()
        user.assertSystemInternalUser()
        return db.transaction { tx ->
            val p = tx.getPersonBySSN(person.socialSecurityNumber)
                ?: personService.getOrCreatePerson(
                    tx,
                    user,
                    ExternalIdentifier.SSN.getInstance(person.socialSecurityNumber)
                )
                ?: error("No person found with ssn")
            tx.markPersonLastLogin(p.id)
            p
        }
            .let { ResponseEntity.ok().body(AuthenticatedUser.Citizen(it.id)) }
    }

    @PostMapping("/system/employee-identity")
    fun employeeIdentity(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody employee: EmployeeIdentityRequest
    ): EmployeeUser {
        Audit.EmployeeGetOrCreate.log(targetId = employee.externalId)
        user.assertSystemInternalUser()
        return db.transaction {
            val e = it.getEmployeeUserByExternalId(employee.externalId)
                ?: EmployeeUser(
                    id = it.createEmployee(employee.toNewEmployee()).id,
                    firstName = employee.firstName,
                    lastName = employee.lastName,
                    globalRoles = emptySet(),
                    allScopedRoles = emptySet()
                )
            it.markEmployeeLastLogin(e.id)
            e
        }
    }

    @GetMapping("/system/employee/{id}")
    fun employeeUser(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable
        id: UUID
    ): EmployeeUser? {
        Audit.EmployeeGetOrCreate.log(targetId = id)
        user.assertSystemInternalUser()
        return db.read { it.getEmployeeUser(id) }
    }

    @GetMapping("/system/mobile-identity/{token}")
    fun mobileIdentity(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable
        token: UUID
    ): ResponseEntity<MobileDeviceIdentity> {
        Audit.MobileDevicesRead.log(targetId = token)
        user.assertSystemInternalUser()
        return ResponseEntity.ok(db.read { it.getDeviceByToken(token) })
    }

    data class EmployeeIdentityRequest(
        val externalId: ExternalId,
        val firstName: String,
        val lastName: String,
        val email: String?
    ) {
        fun toNewEmployee(): NewEmployee =
            NewEmployee(firstName = firstName, lastName = lastName, email = email, externalId = externalId)
    }

    data class PersonIdentityRequest(
        val socialSecurityNumber: String,
        val firstName: String,
        val lastName: String
    )
}
