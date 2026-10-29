// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtjclient.dto

import evaka.core.shared.db.DatabaseEnum
import evaka.core.shared.domain.DateRange

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

data class Nationality(val countryName: String = "", val countryCode: String)

data class NativeLanguage(val languageName: String = "", val code: String) {
    companion object {
        val FI = NativeLanguage(languageName = "suomi", code = "fi")
    }
}
