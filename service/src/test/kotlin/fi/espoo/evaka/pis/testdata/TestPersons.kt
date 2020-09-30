// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.testdata

import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.vtjclient.dto.Nationality
import fi.espoo.evaka.vtjclient.dto.NativeLanguage
import fi.espoo.evaka.vtjclient.dto.PersonDataSource
import fi.espoo.evaka.vtjclient.dto.VtjPersonDTO
import java.time.LocalDate
import java.util.UUID

const val personId = "00000000-4000-0000-0000-000000000001"
const val customerId: Long = 100123
val personUUID: UUID = UUID.fromString(personId)

const val validSSN = "080512A918W"
val ssn = ExternalIdentifier.SSN.getInstance(validSSN)
val nilExternalIdentifier = ExternalIdentifier.NoID()

val validPerson = PersonDTO(
    id = personUUID,
    customerId = customerId,
    identity = ssn,
    firstName = "Matti",
    lastName = "Meikäläinen",
    email = "matti.meikalainen@example.org",
    phone = "+358501234567",
    language = "fi",
    dateOfBirth = LocalDate.of(2012, 5, 8),
    streetAddress = "Street 1",
    postalCode = "02300",
    postOffice = "Espoo",
    restrictedDetailsEnabled = false,
    restrictedDetailsEndDate = null
)

val validPersonIdentity = PersonDTO(
    id = personUUID,
    customerId = customerId,
    identity = ssn,
    firstName = validPerson.firstName,
    lastName = validPerson.lastName,
    email = validPerson.email,
    phone = validPerson.phone,
    language = validPerson.language,
    dateOfBirth = validPerson.dateOfBirth,
    streetAddress = "Street 1",
    postalCode = "02300",
    postOffice = "Espoo",
    restrictedDetailsEnabled = validPerson.restrictedDetailsEnabled,
    restrictedDetailsEndDate = validPerson.restrictedDetailsEndDate
)

val vtjPersonDTO = VtjPersonDTO(
    id = personUUID,
    socialSecurityNumber = validSSN,
    firstName = validPerson.firstName!!,
    lastName = validPerson.lastName!!,
    dateOfBirth = validPerson.dateOfBirth,
    nationalities = validPerson.nationalities.map { Nationality("", it) },
    nativeLanguage = NativeLanguage("", validPerson.language!!),
    restrictedDetailsEnabled = false,
    restrictedDetailsEndDate = null,
    streetAddress = validPerson.streetAddress!!,
    postalCode = validPerson.postalCode!!,
    city = validPerson.postOffice!!,
    children = mutableListOf(),
    source = PersonDataSource.VTJ
)
