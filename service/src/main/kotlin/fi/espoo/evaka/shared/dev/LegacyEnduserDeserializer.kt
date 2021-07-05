// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.dev

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.OtherGuardianAgreementStatus
import fi.espoo.evaka.application.persistence.DatabaseForm
import fi.espoo.evaka.application.persistence.daycare.Address
import fi.espoo.evaka.application.persistence.daycare.Adult
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.CareDetails
import fi.espoo.evaka.application.persistence.daycare.Child
import fi.espoo.evaka.application.persistence.daycare.DEFAULT_CHILD_LANGUAGE
import fi.espoo.evaka.application.persistence.daycare.DEFAULT_CHILD_NATIONALITY
import fi.espoo.evaka.application.persistence.daycare.DaycareAdditionalDetails
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.application.persistence.daycare.OtherPerson
import fi.espoo.evaka.shared.DaycareId
import java.time.LocalDate

sealed class FormJson {
    abstract val type: ApplicationType
    abstract fun deserialize(): DatabaseForm

    abstract class DaycareFormJSON : FormJson()
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class EnduserDaycareFormJSON(
    override val type: ApplicationType,
    val urgent: Boolean = false,
    val partTime: Boolean = false,
    val connectedDaycare: Boolean? = null,
    val preferredStartDate: LocalDate? = null,
    val serviceStart: String? = null,
    val serviceEnd: String? = null,
    val extendedCare: Boolean = false,
    val careDetails: EndUserCareDetailsJSON,
    val apply: ApplyJSON,
    val child: ChildJSON,
    val guardian: AdultJSON,
    val otherGuardianAgreementStatus: OtherGuardianAgreementStatus? = null,
    val guardian2: AdultJSON,
    val hasOtherAdults: Boolean = false,
    val otherAdults: List<OtherPersonJSON> = emptyList(),
    val hasOtherChildren: Boolean = false,
    val otherChildren: List<OtherPersonJSON> = emptyList(),
    val additionalDetails: DaycareAdditionalDetailsJSON = DaycareAdditionalDetailsJSON(),
    val maxFeeAccepted: Boolean = false,
    val docVersion: Long
) : FormJson.DaycareFormJSON() {
    override fun deserialize() = DaycareFormV0(
        type = type,
        connectedDaycare = connectedDaycare,
        urgent = urgent,
        partTime = partTime,
        preferredStartDate = preferredStartDate,
        serviceStart = serviceStart,
        serviceEnd = serviceEnd,
        extendedCare = extendedCare,
        careDetails = careDetails.toCareDetails(),
        apply = apply.toApply(),
        child = child.toChild(),
        guardian = guardian.toAdult(),
        otherGuardianAgreementStatus = otherGuardianAgreementStatus,
        guardian2 = guardian2.toAdult(),
        hasOtherAdults = hasOtherAdults,
        otherAdults = otherAdults.map(OtherPersonJSON::toOtherPerson),
        hasOtherChildren = hasOtherChildren,
        otherChildren = otherChildren.map(OtherPersonJSON::toOtherPerson),
        maxFeeAccepted = maxFeeAccepted,
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

data class OtherPersonJSON(
    val firstName: String = "",
    val lastName: String = "",
    val socialSecurityNumber: String = ""
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
    val preferredUnits: List<DaycareId> = emptyList(),
    val siblingBasis: Boolean = false,
    val siblingName: String = "",
    val siblingSsn: String = ""
)

data class DaycareAdditionalDetailsJSON(
    val allergyType: String = "",
    val dietType: String = "",
    val otherInfo: String = ""
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
    guardianMovingDate = guardianMovingDate,
    phoneNumber = phoneNumber,
    email = email,
    hasCorrectingAddress = hasCorrectingAddress,
    correctingAddress = correctingAddress.toAddress(),
    restricted = restricted
)

private fun OtherPersonJSON.toOtherPerson() = OtherPerson(
    firstName = firstName,
    lastName = lastName,
    socialSecurityNumber = socialSecurityNumber
)

private fun ChildJSON.toChild() = Child(
    firstName = firstName,
    lastName = lastName,
    socialSecurityNumber = socialSecurityNumber,
    dateOfBirth = dateOfBirth,
    nationality = nationality,
    language = language,
    address = address.toAddress(),
    childMovingDate = childMovingDate,
    hasCorrectingAddress = hasCorrectingAddress,
    correctingAddress = correctingAddress.toAddress(),
    restricted = restricted
)

private fun ApplyJSON.toApply() = Apply(
    preferredUnits = preferredUnits,
    siblingBasis = siblingBasis,
    siblingName = siblingName,
    siblingSsn = siblingSsn
)

private fun DaycareAdditionalDetailsJSON.toAdditionalDetails() =
    DaycareAdditionalDetails(
        allergyType = allergyType,
        dietType = dietType,
        otherInfo = otherInfo
    )

@JsonIgnoreProperties(ignoreUnknown = true)
data class EndUserCareDetailsJSON(
    val preparatory: Boolean? = null,
    val assistanceNeeded: Boolean = false,
    val assistanceDescription: String = ""
)

fun EndUserCareDetailsJSON.toCareDetails() = CareDetails(
    preparatory = preparatory,
    assistanceNeeded = assistanceNeeded,
    assistanceDescription = assistanceDescription
)
