// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.pis.service.PersonWithChildrenDTO
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.AccessControlCitizen
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.CitizenFeatures
import fi.espoo.evaka.vtjclient.dto.VtjPersonDTO
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Deprecated("Use PersonController instead")
@RestController
@RequestMapping("/persondetails")
class VtjController(
    private val personService: PersonService,
    private val accessControl: AccessControl,
    private val accessControlCitizen: AccessControlCitizen
) {
    @GetMapping("/uuid/{personId}")
    internal fun getDetails(
        db: Database,
        user: AuthenticatedUser.Citizen,
        @PathVariable(value = "personId") personId: PersonId
    ): CitizenUserDetails {
        Audit.VtjRequest.log(targetId = personId)
        accessControl.requirePermissionFor(user, Action.Citizen.Person.READ_VTJ_DETAILS, personId)
        val notFound = { throw NotFound("Person not found") }
        if (user.id != personId) {
            notFound()
        }

        return db.connect { dbc ->
            dbc.transaction { tx ->
                val person = tx.getPersonById(personId) ?: return@transaction null
                when (person.identity) {
                    is ExternalIdentifier.NoID ->
                        CitizenUserDetails.from(person, accessControlCitizen.getPermittedFeatures(tx, user))
                    is ExternalIdentifier.SSN ->
                        personService.getPersonWithChildren(tx, user, personId)
                            ?.let { CitizenUserDetails.from(it, accessControlCitizen.getPermittedFeatures(tx, user)) }
                }
            }
        } ?: notFound()
    }

    internal data class Child(
        val id: ChildId,
        val firstName: String,
        val lastName: String,
        val socialSecurityNumber: String,
    ) {
        companion object {
            fun from(person: VtjPersonDTO): Child = Child(
                id = ChildId(person.id),
                firstName = person.firstName,
                lastName = person.lastName,
                socialSecurityNumber = person.socialSecurityNumber,
            )

            fun from(person: PersonWithChildrenDTO) = Child(
                id = person.id,
                firstName = person.firstName,
                lastName = person.lastName,
                socialSecurityNumber = person.socialSecurityNumber!!
            )
        }
    }

    internal data class CitizenUserDetails(
        val id: PersonId,
        val firstName: String,
        val lastName: String,
        val preferredName: String,
        val socialSecurityNumber: String,
        val streetAddress: String,
        val postalCode: String,
        val postOffice: String,
        val phone: String,
        val backupPhone: String,
        val email: String?,
        val children: List<Child>,
        val accessibleFeatures: CitizenFeatures
    ) {
        companion object {
            fun from(person: PersonDTO, accessibleFeatures: CitizenFeatures): CitizenUserDetails = CitizenUserDetails(
                id = person.id,
                firstName = person.firstName,
                lastName = person.lastName,
                preferredName = person.preferredName,
                socialSecurityNumber = (person.identity as? ExternalIdentifier.SSN)?.ssn ?: "",
                streetAddress = person.streetAddress,
                postalCode = person.postalCode,
                postOffice = person.postOffice,
                phone = person.phone,
                backupPhone = person.backupPhone,
                email = person.email,
                children = emptyList(),
                accessibleFeatures = accessibleFeatures
            )

            fun from(person: PersonWithChildrenDTO, accessibleFeatures: CitizenFeatures) = CitizenUserDetails(
                id = person.id,
                firstName = person.firstName,
                lastName = person.lastName,
                preferredName = person.preferredName,
                socialSecurityNumber = person.socialSecurityNumber!!,
                streetAddress = person.address.streetAddress,
                postalCode = person.address.postalCode,
                postOffice = person.address.city,
                phone = person.phone,
                backupPhone = person.backupPhone,
                email = person.email,
                children = person.children.map { Child.from(it) },
                accessibleFeatures = accessibleFeatures
            )
        }
    }
}
