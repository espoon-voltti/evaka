// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.application.persistence.DatabaseForm
import fi.espoo.evaka.application.persistence.club.ClubFormV0
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ServiceNeedOptionId
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ApplicationFormUpdate(
    val child: ChildDetailsUpdate,
    val guardian: GuardianUpdate,
    val secondGuardian: SecondGuardian?,
    val otherPartner: PersonBasics?,
    val otherChildren: List<PersonBasics>,
    val preferences: Preferences,
    val maxFeeAccepted: Boolean,
    val otherInfo: String,
    val clubDetails: ClubDetails?
) {
    companion object {
        fun from(form: ApplicationForm): ApplicationFormUpdate {
            return ApplicationFormUpdate(
                child =
                    ChildDetailsUpdate(
                        futureAddress = form.child.futureAddress,
                        allergies = form.child.allergies,
                        diet = form.child.diet,
                        assistanceNeeded = form.child.assistanceNeeded,
                        assistanceDescription = form.child.assistanceDescription
                    ),
                guardian =
                    GuardianUpdate(
                        futureAddress = form.guardian.futureAddress,
                        phoneNumber = form.guardian.phoneNumber,
                        email = form.guardian.email
                    ),
                secondGuardian = form.secondGuardian,
                otherPartner = form.otherPartner,
                otherChildren = form.otherChildren,
                preferences = form.preferences,
                maxFeeAccepted = form.maxFeeAccepted,
                otherInfo = form.otherInfo,
                clubDetails = form.clubDetails
            )
        }
    }
}

data class ApplicationForm(
    val child: ChildDetails,
    val guardian: Guardian,
    val secondGuardian: SecondGuardian?,
    val otherPartner: PersonBasics?,
    val otherChildren: List<PersonBasics>,
    val preferences: Preferences,
    val maxFeeAccepted: Boolean,
    val otherInfo: String,
    val clubDetails: ClubDetails?
) {
    companion object {
        fun fromV0(v0: DatabaseForm, childRestricted: Boolean, guardianRestricted: Boolean) =
            when (v0) {
                is DaycareFormV0 ->
                    ApplicationForm(
                        child =
                            ChildDetails(
                                person =
                                    PersonBasics(
                                        firstName = v0.child.firstName,
                                        lastName = v0.child.lastName,
                                        socialSecurityNumber = v0.child.socialSecurityNumber
                                    ),
                                dateOfBirth = v0.child.dateOfBirth,
                                address =
                                    Address(
                                            street = v0.child.address.street,
                                            postalCode = v0.child.address.postalCode,
                                            postOffice = v0.child.address.city
                                        )
                                        .takeIf { !childRestricted },
                                futureAddress =
                                    FutureAddress(
                                            street = v0.child.correctingAddress.street,
                                            postalCode = v0.child.correctingAddress.postalCode,
                                            postOffice = v0.child.correctingAddress.city,
                                            movingDate = v0.child.childMovingDate
                                        )
                                        .takeIf {
                                            v0.child.hasCorrectingAddress == true &&
                                                !childRestricted
                                        },
                                nationality = v0.child.nationality,
                                language = v0.child.language,
                                allergies = v0.additionalDetails.allergyType,
                                diet = v0.additionalDetails.dietType,
                                assistanceNeeded = v0.careDetails.assistanceNeeded,
                                assistanceDescription = v0.careDetails.assistanceDescription
                            ),
                        guardian =
                            Guardian(
                                person =
                                    PersonBasics(
                                        firstName = v0.guardian.firstName,
                                        lastName = v0.guardian.lastName,
                                        socialSecurityNumber = v0.guardian.socialSecurityNumber
                                    ),
                                address =
                                    Address(
                                            street = v0.guardian.address.street,
                                            postalCode = v0.guardian.address.postalCode,
                                            postOffice = v0.guardian.address.city
                                        )
                                        .takeIf { !guardianRestricted },
                                futureAddress =
                                    FutureAddress(
                                            street = v0.guardian.correctingAddress.street,
                                            postalCode = v0.guardian.correctingAddress.postalCode,
                                            postOffice = v0.guardian.correctingAddress.city,
                                            movingDate = v0.guardian.guardianMovingDate
                                        )
                                        .takeIf {
                                            v0.guardian.hasCorrectingAddress == true &&
                                                !guardianRestricted
                                        },
                                phoneNumber = v0.guardian.phoneNumber ?: "",
                                email = v0.guardian.email ?: ""
                            ),
                        secondGuardian =
                            if (v0.guardian2 != null) {
                                SecondGuardian(
                                    phoneNumber = v0.guardian2.phoneNumber ?: "",
                                    email = v0.guardian2.email ?: "",
                                    agreementStatus = v0.otherGuardianAgreementStatus
                                )
                            } else {
                                null
                            },
                        otherPartner =
                            v0.otherAdults
                                .firstOrNull()
                                ?.takeIf { v0.hasOtherAdults }
                                ?.let {
                                    PersonBasics(
                                        firstName = it.firstName,
                                        lastName = it.lastName,
                                        socialSecurityNumber = it.socialSecurityNumber
                                    )
                                },
                        otherChildren =
                            v0.otherChildren
                                .takeIf { v0.hasOtherChildren }
                                ?.map {
                                    PersonBasics(
                                        firstName = it.firstName,
                                        lastName = it.lastName,
                                        socialSecurityNumber = it.socialSecurityNumber
                                    )
                                } ?: emptyList(),
                        preferences =
                            Preferences(
                                preferredUnits =
                                    v0.apply.preferredUnits.map {
                                        PreferredUnit(
                                            id = it,
                                            name = "" // filled afterwards
                                        )
                                    },
                                preferredStartDate = v0.preferredStartDate,
                                connectedDaycarePreferredStartDate =
                                    v0.connectedDaycarePreferredStartDate,
                                serviceNeed =
                                    ServiceNeed(
                                            startTime =
                                                v0.serviceStart?.format(
                                                    DateTimeFormatter.ofPattern("HH:mm")
                                                ) ?: "",
                                            endTime =
                                                v0.serviceEnd?.format(
                                                    DateTimeFormatter.ofPattern("HH:mm")
                                                ) ?: "",
                                            shiftCare = v0.extendedCare,
                                            partTime = v0.partTime,
                                            serviceNeedOption =
                                                if (v0.serviceNeedOption != null) {
                                                    ServiceNeedOption(
                                                        id = v0.serviceNeedOption.id,
                                                        nameFi = v0.serviceNeedOption.nameFi,
                                                        nameSv = v0.serviceNeedOption.nameSv,
                                                        nameEn = v0.serviceNeedOption.nameEn,
                                                        validPlacementType =
                                                            v0.serviceNeedOption.validPlacementType
                                                    )
                                                } else {
                                                    null
                                                }
                                        )
                                        .takeIf {
                                            v0.type == ApplicationType.DAYCARE ||
                                                v0.connectedDaycare == true
                                        },
                                siblingBasis =
                                    SiblingBasis(
                                            siblingName = v0.apply.siblingName,
                                            siblingSsn = v0.apply.siblingSsn
                                        )
                                        .takeIf { v0.apply.siblingBasis },
                                preparatory = v0.careDetails.preparatory ?: false,
                                urgent = v0.urgent
                            ),
                        otherInfo = v0.additionalDetails.otherInfo,
                        maxFeeAccepted = v0.maxFeeAccepted,
                        clubDetails = null
                    )
                is ClubFormV0 ->
                    ApplicationForm(
                        child =
                            ChildDetails(
                                person =
                                    PersonBasics(
                                        firstName = v0.child.firstName,
                                        lastName = v0.child.lastName,
                                        socialSecurityNumber = v0.child.socialSecurityNumber
                                    ),
                                dateOfBirth = v0.child.dateOfBirth,
                                address =
                                    Address(
                                            street = v0.child.address.street,
                                            postalCode = v0.child.address.postalCode,
                                            postOffice = v0.child.address.city
                                        )
                                        .takeIf { !childRestricted },
                                futureAddress =
                                    FutureAddress(
                                            street = v0.child.correctingAddress.street,
                                            postalCode = v0.child.correctingAddress.postalCode,
                                            postOffice = v0.child.correctingAddress.city,
                                            movingDate = v0.child.childMovingDate
                                        )
                                        .takeIf {
                                            v0.child.hasCorrectingAddress == true &&
                                                !childRestricted
                                        },
                                nationality = v0.child.nationality,
                                language = v0.child.language,
                                allergies = "",
                                diet = "",
                                assistanceNeeded = v0.clubCare.assistanceNeeded,
                                assistanceDescription = v0.clubCare.assistanceDescription
                            ),
                        guardian =
                            Guardian(
                                person =
                                    PersonBasics(
                                        firstName = v0.guardian.firstName,
                                        lastName = v0.guardian.lastName,
                                        socialSecurityNumber = v0.guardian.socialSecurityNumber
                                    ),
                                address =
                                    Address(
                                            street = v0.guardian.address.street,
                                            postalCode = v0.guardian.address.postalCode,
                                            postOffice = v0.guardian.address.city
                                        )
                                        .takeIf { !guardianRestricted },
                                futureAddress =
                                    FutureAddress(
                                            street = v0.guardian.correctingAddress.street,
                                            postalCode = v0.guardian.correctingAddress.postalCode,
                                            postOffice = v0.guardian.correctingAddress.city,
                                            movingDate = v0.guardian.guardianMovingDate
                                        )
                                        .takeIf {
                                            v0.guardian.hasCorrectingAddress == true &&
                                                !guardianRestricted
                                        },
                                phoneNumber = v0.guardian.phoneNumber ?: "",
                                email = v0.guardian.email ?: ""
                            ),
                        secondGuardian = null,
                        otherPartner = null,
                        otherChildren = listOf(),
                        preferences =
                            Preferences(
                                preferredUnits =
                                    v0.apply.preferredUnits.map {
                                        PreferredUnit(
                                            id = it,
                                            name = "" // filled afterwards
                                        )
                                    },
                                preferredStartDate = v0.preferredStartDate,
                                connectedDaycarePreferredStartDate = null,
                                serviceNeed = null,
                                siblingBasis =
                                    SiblingBasis(
                                            siblingName = v0.apply.siblingName,
                                            siblingSsn = v0.apply.siblingSsn
                                        )
                                        .takeIf { v0.apply.siblingBasis },
                                preparatory = false,
                                urgent = false
                            ),
                        otherInfo = v0.additionalDetails.otherInfo,
                        maxFeeAccepted = false,
                        clubDetails =
                            ClubDetails(
                                wasOnClubCare = v0.wasOnClubCare,
                                wasOnDaycare = v0.wasOnDaycare
                            )
                    )
                else -> throw Exception("lol")
            }

        fun initForm(
            type: ApplicationType,
            guardian: PersonDTO,
            child: PersonDTO
        ): ApplicationForm {
            return ApplicationForm(
                child =
                    ChildDetails(
                        person =
                            PersonBasics(
                                firstName = child.firstName,
                                lastName = child.lastName,
                                socialSecurityNumber =
                                    (child.identity as? ExternalIdentifier.SSN)?.ssn
                            ),
                        dateOfBirth = child.dateOfBirth,
                        address =
                            Address(
                                street = child.streetAddress,
                                postalCode = child.postalCode,
                                postOffice = child.postOffice
                            ),
                        futureAddress = null,
                        nationality = child.nationalities.firstOrNull() ?: "",
                        language = child.language ?: "",
                        allergies = "",
                        diet = "",
                        assistanceNeeded = false,
                        assistanceDescription = ""
                    ),
                guardian =
                    Guardian(
                        person =
                            PersonBasics(
                                firstName = guardian.firstName,
                                lastName = guardian.lastName,
                                socialSecurityNumber =
                                    (guardian.identity as? ExternalIdentifier.SSN)?.ssn
                            ),
                        address =
                            Address(
                                street = guardian.streetAddress,
                                postalCode = guardian.postalCode,
                                postOffice = guardian.postOffice
                            ),
                        futureAddress = null,
                        phoneNumber = guardian.phone,
                        email = guardian.email ?: ""
                    ),
                secondGuardian = null,
                otherPartner = null,
                otherChildren = emptyList(),
                preferences =
                    Preferences(
                        preferredUnits = emptyList(),
                        preferredStartDate = null,
                        connectedDaycarePreferredStartDate = null,
                        serviceNeed =
                            if (type == ApplicationType.DAYCARE) {
                                ServiceNeed(
                                    startTime = "",
                                    endTime = "",
                                    partTime = false,
                                    shiftCare = false,
                                    serviceNeedOption = null
                                )
                            } else {
                                null
                            },
                        siblingBasis = null,
                        preparatory = false,
                        urgent = false
                    ),
                maxFeeAccepted = false,
                otherInfo = "",
                clubDetails =
                    if (type == ApplicationType.CLUB) {
                        ClubDetails(wasOnClubCare = false, wasOnDaycare = false)
                    } else {
                        null
                    }
            )
        }
    }

    fun update(updated: ApplicationFormUpdate): ApplicationForm =
        this.copy(
            child =
                this.child.copy(
                    futureAddress = updated.child.futureAddress,
                    allergies = updated.child.allergies,
                    diet = updated.child.diet,
                    assistanceNeeded = updated.child.assistanceNeeded,
                    assistanceDescription = updated.child.assistanceDescription
                ),
            guardian =
                this.guardian.copy(
                    futureAddress = updated.guardian.futureAddress,
                    phoneNumber = updated.guardian.phoneNumber,
                    email = updated.guardian.email
                ),
            secondGuardian = updated.secondGuardian,
            otherPartner = updated.otherPartner,
            otherChildren = updated.otherChildren,
            preferences = updated.preferences,
            maxFeeAccepted = updated.maxFeeAccepted,
            otherInfo = updated.otherInfo,
            clubDetails = updated.clubDetails
        )
}

data class ChildDetails(
    val person: PersonBasics,
    val dateOfBirth: LocalDate?,
    val address: Address?,
    val futureAddress: FutureAddress?,
    val nationality: String,
    val language: String,
    val allergies: String,
    val diet: String,
    val assistanceNeeded: Boolean,
    val assistanceDescription: String
)

data class ChildDetailsUpdate(
    val futureAddress: FutureAddress?,
    val allergies: String,
    val diet: String,
    val assistanceNeeded: Boolean,
    val assistanceDescription: String
)

data class Guardian(
    val person: PersonBasics,
    val address: Address?,
    val futureAddress: FutureAddress?,
    val phoneNumber: String,
    val email: String
)

data class GuardianUpdate(
    val futureAddress: FutureAddress?,
    val phoneNumber: String,
    val email: String
)

data class SecondGuardian(
    val phoneNumber: String,
    val email: String,
    val agreementStatus: OtherGuardianAgreementStatus?
)

enum class OtherGuardianAgreementStatus {
    AGREED,
    NOT_AGREED,
    RIGHT_TO_GET_NOTIFIED
}

data class PersonBasics(
    val firstName: String,
    val lastName: String,
    val socialSecurityNumber: String?
)

data class Address(val street: String, val postalCode: String, val postOffice: String)

data class FutureAddress(
    val street: String,
    val postalCode: String,
    val postOffice: String,
    val movingDate: LocalDate?
)

data class Preferences(
    val preferredUnits: List<PreferredUnit>,
    val preferredStartDate: LocalDate?,
    val connectedDaycarePreferredStartDate: LocalDate?,
    val serviceNeed: ServiceNeed?,
    val siblingBasis: SiblingBasis?,
    val preparatory: Boolean,
    val urgent: Boolean
)

data class ServiceNeed(
    val startTime: String,
    val endTime: String,
    val shiftCare: Boolean,
    val partTime: Boolean,
    val serviceNeedOption: ServiceNeedOption?
)

data class SiblingBasis(val siblingName: String, val siblingSsn: String)

data class ClubDetails(val wasOnDaycare: Boolean, val wasOnClubCare: Boolean)

data class ServiceNeedOption(
    val id: ServiceNeedOptionId,
    val nameFi: String,
    val nameSv: String,
    val nameEn: String,
    val validPlacementType: PlacementType?
)
