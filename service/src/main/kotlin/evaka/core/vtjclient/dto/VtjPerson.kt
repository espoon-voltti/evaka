// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtjclient.dto

import java.time.LocalDate

data class VtjPerson(
    val firstNames: String,
    val lastName: String,
    val socialSecurityNumber: String,
    val residentialAddress: PersonAddress? = null,
    val mailingAddressPeriods: List<MailingAddressPeriod> = emptyList(),
    var nationalities: List<Nationality> = emptyList(),
    var nativeLanguage: NativeLanguage? = null,
    var restrictedDetails: RestrictedDetails?,
    var dateOfDeath: LocalDate? = null,
    var residenceCode: String? = null,
    val municipalityOfResidence: String? = null,
    val municipalityOfResidenceSe: String? = null,
)
