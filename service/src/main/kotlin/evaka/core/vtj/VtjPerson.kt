// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtj

import evaka.core.identity.Ssn
import evaka.core.shared.db.DatabaseEnum
import evaka.core.shared.domain.DateRange
import java.time.LocalDate

data class VtjPerson(
    val firstNames: String,
    val lastName: String,
    val ssn: Ssn,
    val residentialAddress: PersonAddress? = null,
    val mailingAddressPeriods: List<MailingAddressPeriod> = emptyList(),
    val nativeLanguage: NativeLanguage? = null,
    val restrictedDetails: RestrictedDetails?,
    val dateOfDeath: LocalDate? = null,
    val residenceCode: String? = null,
    val municipalityOfResidence: String? = null,
    val municipalityOfResidenceSe: String? = null,
)

data class VtjPersonAndGuardians(val person: VtjPerson, val guardians: List<Ssn>)

data class VtjPersonAndDependants(val person: VtjPerson, val dependants: List<Ssn>)

data class RestrictedDetails(val enabled: Boolean, val endDate: LocalDate? = null)

data class PersonAddress(
    val streetAddress: String?,
    val postalCode: String?,
    val postOffice: String?,
    val streetAddressSe: String? = null,
    val postOfficeSe: String? = null,
)

enum class AddressPeriodType : DatabaseEnum {
    MAILING,
    TEMPORARY;

    override val sqlType: String = "address_period_type"
}

data class MailingAddressPeriod(
    val type: AddressPeriodType,
    val validDuring: DateRange,
    val address: PersonAddress,
)

data class NativeLanguage(val languageName: String = "", val code: String) {
    companion object {
        val FI = NativeLanguage(languageName = "suomi", code = "fi")
    }
}
