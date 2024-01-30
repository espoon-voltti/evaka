// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.persistence.daycare.Address as DaycareFormAddress
import fi.espoo.evaka.application.persistence.daycare.Adult as DaycareFormAdult
import fi.espoo.evaka.application.persistence.daycare.Child as DaycareFormChild
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.dev.DevPerson
import java.time.LocalDate

fun PlacementType.toApplicationType(): ApplicationType =
    when (this) {
        PlacementType.CLUB -> ApplicationType.CLUB
        PlacementType.DAYCARE,
        PlacementType.DAYCARE_PART_TIME,
        PlacementType.DAYCARE_FIVE_YEAR_OLDS,
        PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
        PlacementType.PRESCHOOL_DAYCARE_ONLY,
        PlacementType.PREPARATORY_DAYCARE_ONLY -> ApplicationType.DAYCARE
        PlacementType.PRESCHOOL,
        PlacementType.PRESCHOOL_DAYCARE,
        PlacementType.PRESCHOOL_CLUB,
        PlacementType.PREPARATORY,
        PlacementType.PREPARATORY_DAYCARE -> ApplicationType.PRESCHOOL
        PlacementType.TEMPORARY_DAYCARE,
        PlacementType.TEMPORARY_DAYCARE_PART_DAY,
        PlacementType.SCHOOL_SHIFT_CARE -> error("Unsupported placement type ($this)")
    }

fun DevPerson.toDaycareFormChild(
    dateOfBirth: LocalDate? = null,
    nationality: String = "FI",
    language: String = "fi",
    restricted: Boolean = false
) =
    DaycareFormChild(
        firstName = firstName,
        lastName = lastName,
        socialSecurityNumber = ssn ?: "",
        dateOfBirth = dateOfBirth,
        address =
            DaycareFormAddress(street = streetAddress, postalCode = postalCode, city = postOffice),
        nationality = nationality,
        language = language,
        restricted = restricted
    )

fun DevPerson.toDaycareFormAdult(restricted: Boolean = false) =
    DaycareFormAdult(
        firstName = firstName,
        lastName = lastName,
        socialSecurityNumber = ssn ?: "",
        address =
            DaycareFormAddress(street = streetAddress, postalCode = postalCode, city = postOffice),
        phoneNumber = phone,
        email = email,
        restricted = restricted
    )
