// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtjclient.dto

import evaka.core.identity.Ssn
import java.time.LocalDate

data class VtjPerson(
    val firstNames: String,
    val lastName: String,
    val ssn: Ssn,
    val residentialAddress: PersonAddress? = null,
    val mailingAddressPeriods: List<MailingAddressPeriod> = emptyList(),
    val nationalities: List<Nationality> = emptyList(),
    val nativeLanguage: NativeLanguage? = null,
    val restrictedDetails: RestrictedDetails?,
    val dateOfDeath: LocalDate? = null,
    val residenceCode: String? = null,
    val municipalityOfResidence: String? = null,
    val municipalityOfResidenceSe: String? = null,
)
