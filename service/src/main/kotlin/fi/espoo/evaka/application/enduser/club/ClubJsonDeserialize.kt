// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application.enduser.club

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import fi.espoo.evaka.application.enduser.FormJson
import fi.espoo.evaka.application.persistence.club.Address
import fi.espoo.evaka.application.persistence.club.Adult
import fi.espoo.evaka.application.persistence.club.Apply
import fi.espoo.evaka.application.persistence.club.Child
import fi.espoo.evaka.application.persistence.club.ClubAdditionalDetails
import fi.espoo.evaka.application.persistence.club.ClubCare
import fi.espoo.evaka.application.persistence.club.ClubFormV0
import fi.espoo.evaka.application.persistence.club.DEFAULT_CHILD_LANGUAGE
import fi.espoo.evaka.application.persistence.club.DEFAULT_CHILD_NATIONALITY
import java.time.LocalDate
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
data class EnduserClubFormJSON(
    val preferredStartDate: LocalDate?,
    val wasOnDaycare: Boolean,
    val wasOnClubCare: Boolean,
    val careDetails: EndUserClubCare,
    val apply: ApplyJSON,
    val child: ChildJSON,
    val guardian: AdultJSON,
    val additionalDetails: ClubAdditionalDetailsJSON,
    val docVersion: Long
) : FormJson.ClubFormJSON() {
    override fun deserialize() = ClubFormV0(
        preferredStartDate = preferredStartDate,
        wasOnDaycare = wasOnDaycare,
        wasOnClubCare = wasOnClubCare,
        clubCare = careDetails.toClubCare(),
        apply = apply.toApply(),
        child = child.toChild(),
        guardian = guardian.toAdult(),
        additionalDetails = additionalDetails.toAdditionalDetails()
    )
}

data class AddressJSON(
    val street: String = "",
    val postalCode: String = "",
    val city: String = "",
    val editable: Boolean = false
)

@JsonIgnoreProperties("workStatus", "workAddress")
data class AdultJSON(
    val firstName: String = "",
    val lastName: String = "",
    val socialSecurityNumber: String = "",
    val address: AddressJSON = AddressJSON(),
    val phoneNumber: String? = null,
    val email: String? = null,
    val hasCorrectingAddress: Boolean? = null,
    val correctingAddress: AddressJSON = AddressJSON(
        editable = true
    ),
    val guardianMovingDate: LocalDate? = null,
    val restricted: Boolean = false
)

data class ChildJSON(
    val firstName: String = "",
    val lastName: String = "",
    val socialSecurityNumber: String = "",
    val dateOfBirth: LocalDate?,
    val address: AddressJSON = AddressJSON(),
    val nationality: String = DEFAULT_CHILD_NATIONALITY,
    val language: String = DEFAULT_CHILD_LANGUAGE,
    val hasCorrectingAddress: Boolean? = null,
    val correctingAddress: AddressJSON = AddressJSON(
        editable = true
    ),
    val childMovingDate: LocalDate? = null,
    val restricted: Boolean = false
)

data class ApplyJSON(
    val preferredUnits: List<UUID> = emptyList(),
    val siblingBasis: Boolean = false,
    val siblingName: String = "",
    val siblingSsn: String = ""
)

data class ClubAdditionalDetailsJSON(
    val otherInfo: String = ""
)

data class ClubCareJSON(
    val assistanceNeeded: Boolean = false,
    val assistanceDescription: String = "",
    val assistanceAdditionalDetails: String = ""
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EndUserClubCare(
    val assistanceNeeded: Boolean = false,
    val assistanceDescription: String = ""
)

private fun AddressJSON.toAddress() = Address(
    street = street,
    postalCode = postalCode,
    city = city,
    editable = editable
)

private fun AdultJSON.toAdult() = Adult(
    firstName = firstName,
    lastName = lastName,
    socialSecurityNumber = socialSecurityNumber,
    address = address.toAddress(),
    phoneNumber = phoneNumber,
    email = email,
    hasCorrectingAddress = hasCorrectingAddress,
    correctingAddress = correctingAddress.toAddress(),
    guardianMovingDate = guardianMovingDate,
    restricted = restricted
)

private fun ChildJSON.toChild() = Child(
    firstName = firstName,
    lastName = lastName,
    socialSecurityNumber = socialSecurityNumber,
    dateOfBirth = dateOfBirth,
    nationality = nationality,
    language = language,
    address = address.toAddress(),
    hasCorrectingAddress = hasCorrectingAddress,
    correctingAddress = correctingAddress.toAddress(),
    childMovingDate = childMovingDate,
    restricted = restricted
)

private fun ApplyJSON.toApply() = Apply(
    preferredUnits = preferredUnits,
    siblingBasis = siblingBasis,
    siblingName = siblingName,
    siblingSsn = siblingSsn
)

private fun ClubAdditionalDetailsJSON.toAdditionalDetails() = ClubAdditionalDetails(
    otherInfo = otherInfo
)

private fun EndUserClubCare.toClubCare() = ClubCare(
    assistanceNeeded = assistanceNeeded,
    assistanceDescription = assistanceDescription
)
