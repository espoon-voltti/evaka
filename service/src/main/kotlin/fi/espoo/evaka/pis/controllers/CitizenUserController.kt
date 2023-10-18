// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.AccessControlCitizen
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.CitizenFeatures
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/persondetails")
class CitizenUserController(
    private val accessControl: AccessControl,
    private val accessControlCitizen: AccessControlCitizen
) {
    @GetMapping("/uuid/{personId}")
    fun getDetails(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable(value = "personId") personId: PersonId
    ): UserDetailsResponse {
        val notFound = { throw NotFound("Person not found") }
        if (user.id != personId) {
            notFound()
        }

        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Person.READ_VTJ_DETAILS,
                        personId
                    )
                    val person = tx.getPersonById(personId) ?: notFound()
                    UserDetailsResponse(
                        details =
                            CitizenUserDetails.from(
                                person,
                                accessControlCitizen.getPermittedFeatures(tx, user, clock)
                            ),
                        authLevel = user.authLevel,
                    )
                }
            }
            .also { Audit.VtjRequest.log(targetId = personId) }
    }

    data class CitizenUserDetails(
        val id: PersonId,
        val firstName: String,
        val lastName: String,
        val preferredName: String,
        val streetAddress: String,
        val postalCode: String,
        val postOffice: String,
        val phone: String,
        val backupPhone: String,
        val email: String?,
        val accessibleFeatures: CitizenFeatures
    ) {
        companion object {
            fun from(person: PersonDTO, accessibleFeatures: CitizenFeatures): CitizenUserDetails =
                CitizenUserDetails(
                    id = person.id,
                    firstName = person.firstName,
                    lastName = person.lastName,
                    preferredName = person.preferredName,
                    streetAddress = person.streetAddress,
                    postalCode = person.postalCode,
                    postOffice = person.postOffice,
                    phone = person.phone,
                    backupPhone = person.backupPhone,
                    email = person.email,
                    accessibleFeatures = accessibleFeatures
                )
        }
    }

    data class UserDetailsResponse(
        val details: CitizenUserDetails,
        val authLevel: CitizenAuthLevel,
    )
}
