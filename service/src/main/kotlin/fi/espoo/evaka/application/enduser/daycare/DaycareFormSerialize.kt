// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application.enduser.daycare

import fi.espoo.evaka.application.persistence.daycare.Address
import fi.espoo.evaka.application.persistence.daycare.Adult
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.CareDetails
import fi.espoo.evaka.application.persistence.daycare.Child
import fi.espoo.evaka.application.persistence.daycare.DaycareAdditionalDetails
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.application.persistence.daycare.OtherPerson

fun DaycareFormV0.toEnduserDaycareJson() = EnduserDaycareFormJSON(
    type = type,
    connectedDaycare = connectedDaycare,
    preferredStartDate = preferredStartDate,
    additionalDetails = additionalDetails.toJson(),
    apply = apply.toJson(),
    child = child.toJson(),
    guardian = guardian.toJson(),
    otherGuardianAgreementStatus = otherGuardianAgreementStatus,
    guardian2 = guardian2?.toJson() ?: AdultJSON(),
    serviceStart = serviceStart,
    serviceEnd = serviceEnd,
    urgent = urgent,
    partTime = partTime,
    hasOtherChildren = hasOtherChildren,
    otherChildren = otherChildren.map(OtherPerson::toJson),
    hasOtherAdults = hasOtherAdults,
    otherAdults = otherAdults.map(OtherPerson::toJson),
    extendedCare = extendedCare,
    careDetails = careDetails.toEndUserJson(),
    maxFeeAccepted = maxFeeAccepted,
    docVersion = docVersion
)

private fun Apply.toJson() = ApplyJSON(
    preferredUnits = preferredUnits,
    siblingBasis = siblingBasis,
    siblingName = siblingName,
    siblingSsn = siblingSsn
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

private fun Address.toJson(): AddressJSON =
    AddressJSON(
        street = street,
        postalCode = postalCode,
        city = city,
        editable = editable
    )

private fun OtherPerson.toJson(): OtherPersonJSON = OtherPersonJSON(
    firstName = firstName,
    lastName = lastName,
    socialSecurityNumber = socialSecurityNumber
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

private fun DaycareAdditionalDetails.toJson() = DaycareAdditionalDetailsJSON(
    allergyType = allergyType,
    dietType = dietType,
    otherInfo = otherInfo
)

private fun CareDetails.toEndUserJson() = EndUserCareDetailsJSON(
    preparatory = preparatory,
    assistanceNeeded = assistanceNeeded,
    assistanceDescription = assistanceDescription
)
