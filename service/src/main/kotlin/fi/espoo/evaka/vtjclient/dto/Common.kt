// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.dto

data class PersonAddress(
    val streetAddress: String?,
    val postalCode: String?,
    val postOffice: String?,
    val streetAddressSe: String?,
    val postOfficeSe: String?
)

data class Nationality(
    val countryName: String = "",
    val countryCode: String
)

data class NativeLanguage(
    val languageName: String = "",
    val code: String
)
