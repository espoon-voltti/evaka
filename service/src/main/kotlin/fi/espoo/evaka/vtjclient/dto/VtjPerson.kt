// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.dto

import fi.espoo.evaka.identity.getDobFromSsn
import java.time.LocalDate
import java.util.UUID

data class VtjPerson(
    val firstNames: String,
    val lastName: String,
    val socialSecurityNumber: String,
    var address: PersonAddress? = null,
    var dependants: List<VtjPerson> = emptyList(),
    var guardians: List<VtjPerson> = emptyList(),
    var nationalities: List<Nationality> = emptyList(),
    var nativeLanguage: NativeLanguage? = null,
    var restrictedDetails: RestrictedDetails?,
    var dateOfDeath: LocalDate? = null,
    var residenceCode: String? = null,
) {

    fun mapToDto(): VtjPersonDTO =
        VtjPersonDTO(
            firstName = firstNames,
            lastName = lastName,
            id = UUID.fromString(PLACEHOLDER),
            socialSecurityNumber = socialSecurityNumber,
            children = dependants.map { it.mapToDto() } as MutableList<VtjPersonDTO>,
            guardians = guardians.map { it.mapToDto() } as MutableList<VtjPersonDTO>,
            streetAddress = address?.streetAddress ?: "",
            postalCode = address?.postalCode ?: "",
            city = address?.postOffice ?: "",
            streetAddressSe = address?.streetAddressSe ?: "",
            residenceCode = residenceCode ?: "",
            citySe = address?.postOfficeSe ?: "",
            nationalities = nationalities,
            nativeLanguage = nativeLanguage,
            restrictedDetailsEndDate = restrictedDetails?.endDate,
            restrictedDetailsEnabled = restrictedDetails?.enabled ?: false,
            dateOfBirth = getDobFromSsn(socialSecurityNumber),
            dateOfDeath = dateOfDeath,
        )

    companion object {
        const val PLACEHOLDER = "00000000-0000-0000-0000-00000000000"
    }
}
