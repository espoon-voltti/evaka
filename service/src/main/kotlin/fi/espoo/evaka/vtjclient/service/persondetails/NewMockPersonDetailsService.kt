// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.service.persondetails

import fi.espoo.evaka.vtjclient.dto.Nationality
import fi.espoo.evaka.vtjclient.dto.NativeLanguage
import fi.espoo.evaka.vtjclient.dto.PersonAddress
import fi.espoo.evaka.vtjclient.dto.RestrictedDetails
import fi.espoo.evaka.vtjclient.dto.VtjPerson
import fi.espoo.evaka.vtjclient.service.cache.VtjCache
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService
import java.time.LocalDate

class NewMockPersonDetailsService(private val vtjCache: VtjCache) : IPersonDetailsService {

    override fun getPersonWithDependants(query: IPersonDetailsService.DetailsQuery): PersonDetails {
        return getMockPerson(query.targetIdentifier.ssn, IVtjClientService.RequestType.HUOLTAJA_HUOLLETTAVA)
    }

    override fun getPersonWithGuardians(query: IPersonDetailsService.DetailsQuery): PersonDetails {
        return getMockPerson(query.targetIdentifier.ssn, IVtjClientService.RequestType.HUOLLETTAVA_HUOLTAJAT)
    }

    override fun getBasicDetailsFor(query: IPersonDetailsService.DetailsQuery): PersonDetails {
        return getMockPerson(query.targetIdentifier.ssn, IVtjClientService.RequestType.PERUSSANOMA3)
    }

    private fun getMockPerson(ssn: String, requestType: IVtjClientService.RequestType): PersonDetails {
        val cacheKey = "$ssn-$requestType"
        val cachePerson: VtjPerson? = vtjCache.getPerson(cacheKey)
        if (cachePerson != null) {
            return PersonDetails.Result(cachePerson)
        }

        val vtjResult = when (val person = vtjPersonMap[ssn]) {
            null -> PersonDetails.PersonNotFound()
            else -> PersonDetails.Result(
                VtjPerson(
                    firstNames = person.firstNames,
                    lastName = person.lastName,
                    socialSecurityNumber = ssn,
                    address = person.address,
                    nationalities = person.nationalities,
                    nativeLanguage = person.nativeLanguage,
                    restrictedDetails = person.restrictedDetails,
                    dateOfDeath = person.dateOfDeath,
                    residenceCode = person.residenceCode,
                    guardians = person.guardians.mapNotNull { guardianSsn ->
                        vtjPersonMap[guardianSsn]?.let { guardian ->
                            VtjPerson(
                                firstNames = guardian.firstNames,
                                lastName = guardian.lastName,
                                socialSecurityNumber = guardianSsn,
                                address = guardian.address,
                                nationalities = guardian.nationalities,
                                nativeLanguage = guardian.nativeLanguage,
                                restrictedDetails = guardian.restrictedDetails,
                                dateOfDeath = guardian.dateOfDeath,
                                residenceCode = guardian.residenceCode,
                                guardians = emptyList(),
                                dependants = emptyList()
                            )
                        }
                    },
                    dependants = person.dependants.mapNotNull { dependantSsn ->
                        vtjPersonMap[dependantSsn]?.let { dependant ->
                            VtjPerson(
                                firstNames = dependant.firstNames,
                                lastName = dependant.lastName,
                                socialSecurityNumber = dependantSsn,
                                address = dependant.address,
                                nationalities = dependant.nationalities,
                                nativeLanguage = dependant.nativeLanguage,
                                restrictedDetails = dependant.restrictedDetails,
                                dateOfDeath = dependant.dateOfDeath,
                                residenceCode = dependant.residenceCode,
                                guardians = emptyList(),
                                dependants = emptyList()
                            )
                        }
                    }

                )
            )
        }

        if (vtjResult is PersonDetails.Result) {
            vtjCache.save(cacheKey, vtjResult.vtjPerson)
        }

        return vtjResult
    }
}

data class VtjPersonNormalized(
    val firstNames: String,
    val lastName: String,
    val address: PersonAddress?,
    val nationalities: List<Nationality>,
    val nativeLanguage: NativeLanguage?,
    val restrictedDetails: RestrictedDetails?,
    val dateOfDeath: LocalDate?,
    val residenceCode: String?,
    val dependants: List<String>,
    val guardians: List<String>
)

val nationalityFin = Nationality(
    countryName = "Suomi",
    countryCode = "FIN"
)

val langFin = NativeLanguage(
    languageName = "suomi",
    code = "fi"
)

val address1 = PersonAddress(
    streetAddress = "Kamreerintie 1",
    postalCode = "00340",
    postOffice = "Espoo",
    streetAddressSe = "Kamrersgatan 1",
    postOfficeSe = "Esbo"
)

val residenceCode1 = "003KAMRT00100"

val address2 = PersonAddress(
    streetAddress = "Kamreerintie 2",
    postalCode = "00340",
    postOffice = "Espoo",
    streetAddressSe = "Kamrersgatan 2",
    postOfficeSe = "Esbo"
)

val residenceCode2 = "003KAMRT00200"

val vtjPersonMap = mapOf<String, VtjPersonNormalized>(
    // children
    "070714A9126" to VtjPersonNormalized(
        firstNames = "Jari-Petteri Mukkelis-Makkelis Vetelä-Viljami Eelis-Juhani",
        lastName = "Karhula",
        nationalities = listOf(nationalityFin),
        nativeLanguage = langFin,
        restrictedDetails = RestrictedDetails(enabled = false),
        address = address1,
        dateOfDeath = null,
        residenceCode = residenceCode1,
        dependants = listOf(),
        guardians = listOf("070644-937X", "081181-9984")
    ),
    "160616A978U" to VtjPersonNormalized(
        firstNames = "Kaarina Veera Nelli",
        lastName = "Karhula",
        nationalities = listOf(nationalityFin),
        nativeLanguage = langFin,
        restrictedDetails = RestrictedDetails(enabled = false),
        address = address1,
        dateOfDeath = null,
        residenceCode = residenceCode1,
        dependants = listOf(),
        guardians = listOf("070644-937X", "290385-9900")
    ),

    // adults
    "070644-937X" to VtjPersonNormalized(
        firstNames = "Johannes Olavi Antero Tapio",
        lastName = "Karhula",
        nationalities = listOf(nationalityFin),
        nativeLanguage = langFin,
        restrictedDetails = RestrictedDetails(enabled = false),
        address = address1,
        dateOfDeath = null,
        residenceCode = residenceCode1,
        dependants = listOf("070714A9126", "160616A978U"),
        guardians = listOf()
    ),
    "290385-9900" to VtjPersonNormalized(
        firstNames = "Anna Maria Sofia",
        lastName = "Haaga",
        nationalities = listOf(nationalityFin),
        nativeLanguage = langFin,
        restrictedDetails = RestrictedDetails(enabled = false),
        address = address1,
        dateOfDeath = null,
        residenceCode = residenceCode1,
        dependants = listOf("160616A978U"),
        guardians = listOf()
    ),
    "081181-9984" to VtjPersonNormalized(
        firstNames = "Sylvi Liisa",
        lastName = "Marttila",
        nationalities = listOf(nationalityFin),
        nativeLanguage = langFin,
        restrictedDetails = RestrictedDetails(enabled = false),
        address = address2,
        dateOfDeath = null,
        residenceCode = residenceCode2,
        dependants = listOf("070714A9126"),
        guardians = listOf()
    )
)
