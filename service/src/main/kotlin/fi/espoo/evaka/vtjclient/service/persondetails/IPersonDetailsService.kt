// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.service.persondetails

import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.vtjclient.dto.VtjPerson

interface IPersonDetailsService {
    fun getPersonWithDependants(query: DetailsQuery): PersonDetails

    fun getPersonWithGuardians(query: DetailsQuery): PersonDetails

    fun getBasicDetailsFor(query: DetailsQuery): PersonDetails

    data class DetailsQuery(val requestingUser: AuthenticatedUser, val targetIdentifier: ExternalIdentifier.SSN)
}

sealed class PersonDetails {
    data class QueryError(val message: String = "Error retrieving details") : PersonDetails()
    data class PersonNotFound(val message: String = "Person not found") : PersonDetails()
    data class Result(val vtjPerson: VtjPerson) : PersonDetails()
}
