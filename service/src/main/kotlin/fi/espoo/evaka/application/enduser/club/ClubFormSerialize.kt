// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application.enduser.club

import fi.espoo.evaka.application.persistence.club.Address
import fi.espoo.evaka.application.persistence.club.Adult
import fi.espoo.evaka.application.persistence.club.Apply
import fi.espoo.evaka.application.persistence.club.Child
import fi.espoo.evaka.application.persistence.club.ClubAdditionalDetails
import fi.espoo.evaka.application.persistence.club.ClubCare
import fi.espoo.evaka.application.persistence.club.ClubFormV0

fun ClubFormV0.toEnduserJson() = EnduserClubFormJSON(
    preferredStartDate = preferredStartDate,
    additionalDetails = additionalDetails.toJson(),
    apply = apply.toJson(),
    child = child.toJson(),
    careDetails = clubCare.toEnduserJson(),
    guardian = guardian.toJson(),
    wasOnClubCare = wasOnClubCare,
    wasOnDaycare = wasOnDaycare,
    docVersion = docVersion
)

private fun Address.toJson(): AddressJSON =
    AddressJSON(
        street = street,
        postalCode = postalCode,
        city = city,
        editable = editable
    )

private fun Adult.toJson(): AdultJSON =
    AdultJSON(
        firstName = firstName,
        lastName = lastName,
        socialSecurityNumber = socialSecurityNumber,
        address = address.toJson(),
        phoneNumber = phoneNumber,
        email = email,
        hasCorrectingAddress = hasCorrectingAddress,
        correctingAddress = correctingAddress.toJson(),
        guardianMovingDate = guardianMovingDate,
        restricted = restricted
    )

private fun Child.toJson(): ChildJSON = ChildJSON(
    firstName = firstName,
    lastName = lastName,
    socialSecurityNumber = socialSecurityNumber,
    dateOfBirth = dateOfBirth,
    address = address.toJson(),
    nationality = nationality,
    language = language,
    hasCorrectingAddress = hasCorrectingAddress,
    correctingAddress = correctingAddress.toJson(),
    childMovingDate = childMovingDate,
    restricted = restricted
)

private fun Apply.toJson() = ApplyJSON(
    preferredUnits = preferredUnits,
    siblingBasis = siblingBasis,
    siblingName = siblingName,
    siblingSsn = siblingSsn
)

private fun ClubAdditionalDetails.toJson() = ClubAdditionalDetailsJSON(
    otherInfo = otherInfo
)

private fun ClubCare.toEnduserJson(): EndUserClubCare = EndUserClubCare(
    assistanceNeeded = assistanceNeeded,
    assistanceDescription = assistanceDescription
)
