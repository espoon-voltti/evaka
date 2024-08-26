// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.dto

import java.time.LocalDate
import java.util.UUID

// TODO: The address is going to be lifted to a separate class/model soon.
//  It wasn't changed yet because it would require changes at both application-srv and pis-srv
data class VtjPersonDTO(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val socialSecurityNumber: String,
    val nationalities: List<Nationality> = emptyList(),
    val nativeLanguage: NativeLanguage? = null,
    val restrictedDetailsEnabled: Boolean = false,
    val restrictedDetailsEndDate: LocalDate?,
    val streetAddress: String,
    val postalCode: String,

    // TODO: this should be postOffice
    val city: String,
    val residenceCode: String,
    val children: List<VtjPersonDTO> = listOf(),
    val guardians: List<VtjPersonDTO> = listOf(),
    val dateOfBirth: LocalDate,
    val dateOfDeath: LocalDate? = null,
    val streetAddressSe: String,
    val citySe: String,
)
