// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtjclient.service.persondetails

import evaka.core.shared.buildHttpClient
import evaka.core.shared.utils.get
import evaka.core.shared.utils.headerInterceptor
import evaka.core.shared.utils.post
import evaka.core.vtjclient.dto.Nationality
import evaka.core.vtjclient.dto.NativeLanguage
import evaka.core.vtjclient.dto.PersonAddress
import evaka.core.vtjclient.dto.RestrictedDetails
import evaka.core.vtjclient.dto.VtjPerson
import java.net.URI
import java.time.LocalDate
import tools.jackson.databind.json.JsonMapper

data class DummyIdpVtjPerson(
    val firstNames: String,
    val lastName: String,
    val socialSecurityNumber: String,
    val address: PersonAddress? = null,
    val nativeLanguage: NativeLanguage? = null,
    val nationalities: List<Nationality> = emptyList(),
    val restrictedDetails: RestrictedDetails? = null,
    val dateOfDeath: LocalDate? = null,
    val residenceCode: String? = null,
    val municipalityOfResidence: String? = null,
    val guardians: List<DummyIdpVtjPerson> = emptyList(),
    val dependants: List<DummyIdpVtjPerson> = emptyList(),
) {
    fun toVtjPerson(includeGuardians: Boolean, includeDependants: Boolean): VtjPerson =
        VtjPerson(
            firstNames = firstNames,
            lastName = lastName,
            socialSecurityNumber = socialSecurityNumber,
            address = address,
            nativeLanguage = nativeLanguage,
            nationalities = nationalities,
            restrictedDetails = restrictedDetails,
            dateOfDeath = dateOfDeath,
            residenceCode = residenceCode,
            municipalityOfResidence = municipalityOfResidence,
            guardians =
                if (includeGuardians) guardians.map { it.toVtjPerson(false, false) }
                else emptyList(),
            dependants =
                if (includeDependants) dependants.map { it.toVtjPerson(false, false) }
                else emptyList(),
        )
}

class DummyIdpPersonDetailsService(baseUrl: String, jsonMapper: JsonMapper) :
    IPersonDetailsService {
    private val httpClient =
        buildHttpClient(
            rootUrl = URI(baseUrl),
            jsonMapper = jsonMapper,
            interceptors = listOf(headerInterceptor("Accept", "application/json")),
        )

    override fun getBasicDetailsFor(query: IPersonDetailsService.DetailsQuery): VtjPerson =
        fetch(query).toVtjPerson(includeGuardians = false, includeDependants = false)

    override fun getPersonWithDependants(query: IPersonDetailsService.DetailsQuery): VtjPerson =
        fetch(query).toVtjPerson(includeGuardians = false, includeDependants = true)

    override fun getPersonWithGuardians(query: IPersonDetailsService.DetailsQuery): VtjPerson =
        fetch(query).toVtjPerson(includeGuardians = true, includeDependants = false)

    fun clearAll() {
        httpClient.post<Unit>("idp/users/clear", jsonBody = emptyMap<String, String>())
    }

    private fun fetch(query: IPersonDetailsService.DetailsQuery): DummyIdpVtjPerson =
        httpClient.get<DummyIdpVtjPerson>("idp/users/${query.targetIdentifier.ssn}")
}
