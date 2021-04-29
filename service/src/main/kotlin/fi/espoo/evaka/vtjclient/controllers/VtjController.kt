// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.vtjclient.dto.VtjPersonDTO
import fi.espoo.evaka.vtjclient.service.persondetails.PersonStorageService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Deprecated("Use PersonController instead")
@RestController
@RequestMapping("/persondetails")
class VtjController(
    private val personStorageService: PersonStorageService,
    private val personService: PersonService
) {
    @GetMapping("/uuid/{personId}")
    internal fun getDetails(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable(value = "personId") personId: UUID
    ): CitizenUserDetails {
        Audit.VtjRequest.log(targetId = personId)
        user.requireOneOfRoles(UserRole.END_USER, UserRole.CITIZEN_WEAK)
        if (user.id != personId) {
            throw Forbidden("Query not allowed")
        }

        return db.read { it.getPersonById(personId) }?.let { person ->
            when (person.identity) {
                is ExternalIdentifier.NoID -> CitizenUserDetails.from(person)
                is ExternalIdentifier.SSN -> personService.getPersonWithDependants(user, person.identity)
                    ?.let { db.transaction { tx -> personStorageService.upsertVtjGuardianAndChildren(tx, it) } }
                    ?.let { CitizenUserDetails.from(it) }
            }
        } ?: throw NotFound("Person not found")
    }

    internal data class Child(
        val id: UUID,
        val firstName: String,
        val lastName: String,
        val socialSecurityNumber: String,
    ) {
        companion object {
            fun from(person: VtjPersonDTO): Child = Child(
                id = person.id,
                firstName = person.firstName,
                lastName = person.lastName,
                socialSecurityNumber = person.socialSecurityNumber,
            )
        }
    }

    internal data class CitizenUserDetails(
        val id: UUID,
        val firstName: String,
        val lastName: String,
        val socialSecurityNumber: String,
        val children: List<Child>
    ) {
        companion object {
            fun from(person: PersonDTO): CitizenUserDetails = CitizenUserDetails(
                id = person.id,
                firstName = person.firstName ?: "",
                lastName = person.lastName ?: "",
                socialSecurityNumber = (person.identity as? ExternalIdentifier.SSN)?.ssn ?: "",
                children = emptyList()
            )
            fun from(person: VtjPersonDTO): CitizenUserDetails = CitizenUserDetails(
                id = person.id,
                firstName = person.firstName,
                lastName = person.lastName,
                socialSecurityNumber = person.socialSecurityNumber,
                children = person.children.map { Child.from(it) }
            )
        }
    }
}
