// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtj

import evaka.core.identity.Ssn
import evaka.core.shared.EvakaUserId
import evaka.core.shared.buildHttpClient
import evaka.core.shared.utils.get
import evaka.core.shared.utils.headerInterceptor
import evaka.core.shared.utils.post
import java.net.URI
import java.time.LocalDate
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue

data class DummyIdpVtjPerson(
    val firstNames: String,
    val lastName: String,
    val socialSecurityNumber: String,
    val address: PersonAddress? = null,
    val nativeLanguage: NativeLanguage? = null,
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
            restrictedDetails = restrictedDetails,
            dateOfDeath = dateOfDeath,
            residenceCode = residenceCode,
            municipalityOfResidence = municipalityOfResidence,
        )
}

class DummyIdpVtjClient(baseUrl: String, private val jsonMapper: JsonMapper) : VtjClient {
    private val httpClient =
        buildHttpClient(
            rootUrl = URI(baseUrl),
            jsonMapper = jsonMapper,
            interceptors = listOf(headerInterceptor("Accept", "application/json")),
        )

    override fun fetchPersons(requestingUser: EvakaUserId, ssns: Collection<Ssn>) =
        fetch(ssns) { it.toVtjPerson() }

    override fun fetchWithGuardians(requestingUser: EvakaUserId, ssns: Collection<Ssn>) =
        fetch(ssns) { person ->
            VtjPersonAndGuardians(
                person.toVtjPerson(),
                person.guardians.map { Ssn.from(it.socialSecurityNumber) },
            )
        }

    override fun fetchWithDependants(requestingUser: EvakaUserId, ssns: Collection<Ssn>) =
        fetch(ssns) { person ->
            VtjPersonAndDependants(
                person.toVtjPerson(),
                person.dependants.map { Ssn.from(it.socialSecurityNumber) },
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

    private fun <T> fetch(ssns: Collection<Ssn>, map: (DummyIdpVtjPerson) -> T): Map<Ssn, T> =
        ssns
            .distinct()
            .mapNotNull { ssn ->
                httpClient
                    .get<DummyIdpVtjPerson?>("idp/users/${ssn.value}") { response ->
                        when {
                            response.code == 404 -> null
                            !response.isSuccessful ->
                                error("Dummy IdP returned status ${response.code}")
                            else -> jsonMapper.readValue<DummyIdpVtjPerson>(response.body.string())
                        }
                    }
                    ?.let { ssn to map(it) }
            }
            .toMap()
}
