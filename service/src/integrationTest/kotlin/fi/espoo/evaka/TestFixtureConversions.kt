// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import fi.espoo.evaka.application.persistence.FormType
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.placement.PlacementType
import java.time.LocalDate
import fi.espoo.evaka.application.persistence.daycare.Address as DaycareFormAddress
import fi.espoo.evaka.application.persistence.daycare.Adult as DaycareFormAdult
import fi.espoo.evaka.application.persistence.daycare.Child as DaycareFormChild

fun PlacementType.toFormType(): FormType = when (this) {
    PlacementType.CLUB -> FormType.CLUB
    PlacementType.DAYCARE, PlacementType.DAYCARE_PART_TIME -> FormType.DAYCARE
    PlacementType.PRESCHOOL, PlacementType.PRESCHOOL_DAYCARE,
    PlacementType.PREPARATORY, PlacementType.PREPARATORY_DAYCARE -> FormType.PRESCHOOL
}

fun PersonData.Detailed.toDaycareFormChild(
    dateOfBirth: LocalDate? = null,
    nationality: String = "FI",
    language: String = "fi",
    restricted: Boolean = false
) = DaycareFormChild(
    firstName = firstName,
    lastName = lastName,
    socialSecurityNumber = ssn ?: "",
    dateOfBirth = dateOfBirth,
    address = DaycareFormAddress(
        street = streetAddress ?: "",
        postalCode = postalCode ?: "",
        city = postOffice ?: ""
    ),
    nationality = nationality,
    language = language,
    restricted = restricted
)

fun PersonData.Detailed.toDaycareFormAdult(restricted: Boolean = false) = DaycareFormAdult(
    firstName = firstName,
    lastName = lastName,
    socialSecurityNumber = ssn ?: "",
    address = DaycareFormAddress(
        street = streetAddress ?: "",
        postalCode = postalCode ?: "",
        city = postOffice ?: ""
    ),
    phoneNumber = phone,
    email = email,
    restricted = restricted
)
