// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.Audit
import fi.espoo.evaka.ExcludeCodeGen
import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pairing.MobileDeviceIdentity
import fi.espoo.evaka.pairing.getDeviceByToken
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.EmployeeFeatures
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
/**
 * Controller for "system" endpoints intended to be only called from apigw as the system internal user
 */
class SystemController(private val personService: PersonService, private val accessControl: AccessControl) {
    @PostMapping("/system/citizen-login")
    fun citizenLogin(
        db: Database.Connection,
        user: AuthenticatedUser.SystemInternalUser,
        @RequestBody request: CitizenLoginRequest
    ): CitizenUser {
        return db.transaction { tx ->
            val citizen = tx.getCitizenUserBySsn(request.socialSecurityNumber)
                ?: personService.getOrCreatePerson(
                    tx,
                    user,
                    ExternalIdentifier.SSN.getInstance(request.socialSecurityNumber)
                )?.let { CitizenUser(PersonId(it.id)) }
                ?: error("No person found with ssn")
            tx.markPersonLastLogin(citizen.id)
            citizen
        }.also {
            Audit.CitizenLogin.log(targetId = listOf(request.socialSecurityNumber, request.lastName, request.firstName), objectId = it.id)
        }
    }

    @PostMapping("/system/employee-login")
    fun employeeLogin(
        db: Database.Connection,
        user: AuthenticatedUser.SystemInternalUser,
        @RequestBody request: EmployeeLoginRequest
    ): EmployeeUser {
        return db.transaction {
            val employee = it.getEmployeeUserByExternalId(request.externalId)
                ?: EmployeeUser(
                    id = it.createEmployee(request.toNewEmployee()).id,
                    firstName = request.firstName,
                    lastName = request.lastName,
                    globalRoles = emptySet(),
                    allScopedRoles = emptySet()
                )
            it.markEmployeeLastLogin(employee.id)
            employee
        }.also {
            Audit.EmployeeLogin.log(targetId = listOf(request.externalId, request.lastName, request.firstName, request.email), objectId = it.id)
        }
    }

    @GetMapping("/system/employee/{id}")
    fun employeeUser(
        db: Database.Connection,
        user: AuthenticatedUser.SystemInternalUser,
        @PathVariable
        id: UUID
    ): EmployeeUserResponse? {
        Audit.EmployeeGetOrCreate.log(targetId = id)
        return db.read { tx ->
            tx.getEmployeeUser(id)?.let { employeeUser ->
                EmployeeUserResponse(
                    id = employeeUser.id,
                    firstName = employeeUser.firstName,
                    lastName = employeeUser.lastName,
                    globalRoles = employeeUser.globalRoles,
                    allScopedRoles = employeeUser.allScopedRoles,
                    accessibleFeatures = accessControl.getPermittedFeatures(AuthenticatedUser.Employee(employeeUser))
                )
            }
        }
    }

    @GetMapping("/system/mobile-identity/{token}")
    fun mobileIdentity(
        db: Database.Connection,
        user: AuthenticatedUser.SystemInternalUser,
        @PathVariable
        token: UUID
    ): ResponseEntity<MobileDeviceIdentity> {
        Audit.MobileDevicesRead.log(targetId = token)
        return ResponseEntity.ok(db.read { it.getDeviceByToken(token) })
    }

    @ExcludeCodeGen
    data class EmployeeLoginRequest(
        val externalId: ExternalId,
        val firstName: String,
        val lastName: String,
        val email: String?
    ) {
        fun toNewEmployee(): NewEmployee =
            NewEmployee(firstName = firstName, lastName = lastName, email = email, externalId = externalId)
    }

    @ExcludeCodeGen
    data class CitizenLoginRequest(
        val socialSecurityNumber: String,
        val firstName: String,
        val lastName: String
    )

    data class EmployeeUserResponse(
        val id: UUID,
        val firstName: String,
        val lastName: String,
        val globalRoles: Set<UserRole> = setOf(),
        val allScopedRoles: Set<UserRole> = setOf(),
        val accessibleFeatures: EmployeeFeatures,
    )
}
