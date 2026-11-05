// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtj

import evaka.core.identity.Ssn
import evaka.core.identity.getDobFromSsn
import evaka.core.shared.buildHttpClient
import evaka.core.shared.utils.get
import evaka.core.shared.utils.headerInterceptor
import evaka.core.shared.utils.post
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
    fun toVtjPerson(): VtjPerson =
        VtjPerson(
            firstNames = firstNames,
            lastName = lastName,
            ssn = Ssn.from(socialSecurityNumber),
            residentialAddress = address,
            nativeLanguage = nativeLanguage,
            nationalities = nationalities,
            restrictedDetails = restrictedDetails,
            dateOfDeath = dateOfDeath,
            residenceCode = residenceCode,
            municipalityOfResidence = municipalityOfResidence,
        )

    fun toVtjRelative(): VtjRelative {
        val ssn = Ssn.from(socialSecurityNumber)
        return VtjRelative(
            ssn = ssn,
            dateOfBirth = getDobFromSsn(ssn.value),
            firstNames = firstNames,
            lastName = lastName,
        )
    }
}

class DummyIdpVtjClient(baseUrl: String, jsonMapper: JsonMapper) : VtjClient {
    private val httpClient =
        buildHttpClient(
            rootUrl = URI(baseUrl),
            jsonMapper = jsonMapper,
            interceptors = listOf(headerInterceptor("Accept", "application/json")),
        )

    override fun fetchPerson(query: VtjClient.DetailsQuery): VtjPerson = fetch(query).toVtjPerson()

    override fun fetchWithDependants(query: VtjClient.DetailsQuery): VtjPersonAndDependants =
        fetch(query).let { person ->
            VtjPersonAndDependants(
                person.toVtjPerson(),
                person.dependants.map { it.toVtjRelative() },
            )
        }

    override fun fetchWithGuardians(query: VtjClient.DetailsQuery): VtjPersonAndGuardians =
        fetch(query).let { person ->
            VtjPersonAndGuardians(
                person.toVtjPerson(),
                person.guardians.map { it.toVtjRelative() },
            )
        }

    fun clearAll() {
        httpClient.post<Unit>("idp/users/clear", jsonBody = emptyMap<String, String>())
    }

    fun enterTestMode() {
        httpClient.post<Unit>("idp/test-mode/enter", jsonBody = emptyMap<String, String>())
    }

    fun exitTestMode() {
        httpClient.post<Unit>("idp/test-mode/exit", jsonBody = emptyMap<String, String>())
    }

    private fun fetch(query: VtjClient.DetailsQuery): DummyIdpVtjPerson =
        httpClient.get<DummyIdpVtjPerson>("idp/users/${query.ssn.value}")
}
