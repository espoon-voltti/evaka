// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.pis.CitizenUserDetails
import fi.espoo.evaka.pis.getCitizenUserDetails
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
    ): CitizenUserResponse {
        val notFound = { throw NotFound("Person not found") }
        if (user.id != personId) {
            notFound()
        }

        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Person.READ_VTJ_DETAILS,
                        personId
                    )
                    val details = tx.getCitizenUserDetails(personId) ?: notFound()
                    val accessibleFeatures =
                        accessControlCitizen.getPermittedFeatures(tx, user, clock)
                    CitizenUserResponse(details, user.authLevel, accessibleFeatures)
                }
            }
            .also { Audit.VtjRequest.log(targetId = personId) }
    }

    data class CitizenUserResponse(
        val details: CitizenUserDetails,
        val authLevel: CitizenAuthLevel,
        val accessibleFeatures: CitizenFeatures,
    )
}
