// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application.persistence.daycare

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import fi.espoo.evaka.application.ApplicationDetails
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.OtherGuardianAgreementStatus
import fi.espoo.evaka.application.ServiceNeedOption
import fi.espoo.evaka.application.persistence.DatabaseForm
import fi.espoo.evaka.shared.DaycareId
import java.time.LocalDate

const val DEFAULT_CHILD_NATIONALITY = "FI"
const val DEFAULT_CHILD_LANGUAGE = "fi"

@JsonIgnoreProperties(
    "hasSecondGuardian",
    "guardiansSeparated",
    "secondGuardianHasAgreed",
    "guardianIsSingleParent"
)
data class DaycareFormV0(
    override val type: ApplicationType,
    val child: Child,
    val guardian: Adult,
    val otherGuardianAgreementStatus: OtherGuardianAgreementStatus? = null,
    val apply: Apply = Apply(),
    val urgent: Boolean = false,
    val partTime: Boolean = false,
    val serviceNeedOption: ServiceNeedOption? = null,
    val connectedDaycare: Boolean? = false.takeIf { type == ApplicationType.PRESCHOOL },
    val connectedDaycarePreferredStartDate: LocalDate? = null,
    val preferredStartDate: LocalDate? = null,
    val serviceStart: String? = null,
    val serviceEnd: String? = null,
    val extendedCare: Boolean = false,
    val careDetails: CareDetails =
        CareDetails(preparatory = false.takeIf { type == ApplicationType.PRESCHOOL }),
    val guardian2: Adult? = null,
    val hasOtherAdults: Boolean = false,
    val otherAdults: List<OtherPerson> = emptyList(),
    val hasOtherChildren: Boolean = false,
    val otherChildren: List<OtherPerson> = emptyList(),
    val docVersion: Long = 0L,
    val additionalDetails: DaycareAdditionalDetails = DaycareAdditionalDetails(),
    val maxFeeAccepted: Boolean = false
) : DatabaseForm.DaycareForm() {
    init {
        when (type) {
            ApplicationType.CLUB -> error("Invalid application type $type")
            ApplicationType.DAYCARE,
            ApplicationType.PRESCHOOL -> {}
        }
    }

    override fun hideGuardianAddress(): DaycareFormV0 =
        this.copy(
            guardian =
                this.guardian.copy(
                    address = Address(),
                    correctingAddress = Address(),
                    restricted = true
                )
        )

    override fun hideChildAddress(): DaycareFormV0 =
        this.copy(
            child =
                this.child.copy(
                    address = Address(),
                    correctingAddress = Address(),
                    restricted = true
                )
        )

    companion object {
        fun fromApplication2(application: ApplicationDetails) =
            fromForm2(
                application.form,
                application.type,
                application.childRestricted,
                application.guardianRestricted
            )

        fun fromForm2(
            form: fi.espoo.evaka.application.ApplicationForm,
            type: ApplicationType,
            childRestricted: Boolean,
            guardianRestricted: Boolean
        ) = DaycareFormV0(
            type = type,
            child =
                Child(
                    firstName = form.child.person.firstName,
                    lastName = form.child.person.lastName,
                    socialSecurityNumber = form.child.person.socialSecurityNumber ?: "",
                    dateOfBirth = form.child.dateOfBirth,
                    address =
                        form.child.address?.let {
                            Address(
                                street = it.street,
                                postalCode = it.postalCode,
                                city = it.postOffice,
                                editable = false
                            )
                        } ?: Address(),
                    nationality = form.child.nationality,
                    language = form.child.language,
                    hasCorrectingAddress = form.child.futureAddress != null,
                    correctingAddress =
                        form.child.futureAddress?.let {
                            Address(
                                street = it.street,
                                postalCode = it.postalCode,
                                city = it.postOffice,
                                editable = true
                            )
                        } ?: Address(editable = true),
                    childMovingDate = form.child.futureAddress?.movingDate,
                    restricted = childRestricted
                ),
            guardian =
                Adult(
                    firstName = form.guardian.person.firstName,
                    lastName = form.guardian.person.lastName,
                    socialSecurityNumber = form.guardian.person.socialSecurityNumber ?: "",
                    address =
                        form.guardian.address?.let {
                            Address(
                                street = it.street,
                                postalCode = it.postalCode,
                                city = it.postOffice,
                                editable = false
                            )
                        } ?: Address(),
                    phoneNumber = form.guardian.phoneNumber,
                    email = form.guardian.email,
                    hasCorrectingAddress = form.guardian.futureAddress != null,
                    correctingAddress =
                        form.guardian.futureAddress?.let {
                            Address(
                                street = it.street,
                                postalCode = it.postalCode,
                                city = it.postOffice,
                                editable = true
                            )
                        } ?: Address(editable = true),
                    guardianMovingDate = form.guardian.futureAddress?.movingDate,
                    restricted = guardianRestricted
                ),
            otherGuardianAgreementStatus = form.secondGuardian?.agreementStatus,
            apply =
                Apply(
                    preferredUnits = form.preferences.preferredUnits.map { it.id },
                    siblingBasis = form.preferences.siblingBasis != null,
                    siblingName = form.preferences.siblingBasis?.siblingName ?: "",
                    siblingSsn = form.preferences.siblingBasis?.siblingSsn ?: ""
                ),
            urgent = form.preferences.urgent,
            partTime = form.preferences.serviceNeed?.partTime ?: false,
            serviceNeedOption = form.preferences.serviceNeed?.serviceNeedOption,
            connectedDaycare =
                (form.preferences.serviceNeed != null).takeIf {
                    type == ApplicationType.PRESCHOOL
                },
            connectedDaycarePreferredStartDate =
                form.preferences.connectedDaycarePreferredStartDate,
            preferredStartDate = form.preferences.preferredStartDate,
            serviceStart = form.preferences.serviceNeed?.startTime,
            serviceEnd = form.preferences.serviceNeed?.endTime,
            extendedCare = form.preferences.serviceNeed?.shiftCare ?: false,
            careDetails =
                CareDetails(
                    preparatory =
                        form.preferences.preparatory.takeIf {
                            type == ApplicationType.PRESCHOOL
                        },
                    assistanceNeeded = form.child.assistanceNeeded,
                    assistanceDescription = form.child.assistanceDescription
                ),
            guardian2 =
                form.secondGuardian?.let { secondGuardian ->
                    Adult(
                        phoneNumber = secondGuardian.phoneNumber,
                        email = secondGuardian.email
                    )
                },
            hasOtherAdults = form.otherPartner != null,
            otherAdults =
                form.otherPartner?.let {
                    listOf(
                        OtherPerson(
                            firstName = it.firstName,
                            lastName = it.lastName,
                            socialSecurityNumber = it.socialSecurityNumber ?: ""
                        )
                    )
                } ?: listOf(),
            hasOtherChildren = form.otherChildren.isNotEmpty(),
            otherChildren =
                form.otherChildren.map {
                    OtherPerson(
                        firstName = it.firstName,
                        lastName = it.lastName,
                        socialSecurityNumber = it.socialSecurityNumber ?: ""
                    )
                },
            additionalDetails =
                DaycareAdditionalDetails(
                    allergyType = form.child.allergies,
                    dietType = form.child.diet,
                    otherInfo = form.otherInfo
                ),
            maxFeeAccepted = form.maxFeeAccepted
        )
    }
}

data class Child(
    val firstName: String = "",
    val lastName: String = "",
    val socialSecurityNumber: String = "",
    val dateOfBirth: LocalDate?,
    val address: Address = Address(),
    val nationality: String = DEFAULT_CHILD_NATIONALITY,
    val language: String = DEFAULT_CHILD_LANGUAGE,
    val hasCorrectingAddress: Boolean? = null,
    val correctingAddress: Address = Address(editable = true),
    val childMovingDate: LocalDate? = null,
    val restricted: Boolean = false
)

@JsonIgnoreProperties("workStatus", "workAddress")
data class Adult(
    val firstName: String = "",
    val lastName: String = "",
    val socialSecurityNumber: String = "",
    val address: Address = Address(),
    val phoneNumber: String? = null,
    val email: String? = null,
    val hasCorrectingAddress: Boolean? = null,
    val correctingAddress: Address = Address(editable = true),
    val guardianMovingDate: LocalDate? = null,
    val restricted: Boolean = false
)

data class Address(
    val street: String = "",
    val postalCode: String = "",
    val city: String = "",
    val editable: Boolean = false
)

data class DaycareAdditionalDetails(
    val allergyType: String = "",
    val dietType: String = "",
    val otherInfo: String = ""
)

data class Apply(
    val preferredUnits: List<DaycareId> = emptyList(),
    val siblingBasis: Boolean = false,
    val siblingName: String = "",
    val siblingSsn: String = ""
)

data class OtherPerson(
    val firstName: String = "",
    val lastName: String = "",
    val socialSecurityNumber: String = ""
)

enum class CareType(
    val id: Long
) {
    @JsonProperty("centre")
    CENTRE(1L),

    @JsonProperty("family")
    FAMILY(2L),

    @JsonProperty("group_family")
    GROUP_FAMILY(3L)
}

enum class WeeklyHours(
    val id: Long
) {
    @JsonProperty("over_35")
    OVER_35(1L),

    @JsonProperty("between_25_and_35")
    BETWEEN_25_AND_35(2L),

    @JsonProperty("under_25")
    UNDER_25(3L)
}

@JsonIgnoreProperties(
    // no longer in the class since commit 90b5a4f4f949bcce4680b2ee133820d6affc0695, but still
    // present in data
    "assistanceAdditionalDetails",
    "careFactor"
)
data class CareDetails(
    val preparatory: Boolean? = null,
    val assistanceNeeded: Boolean = false,
    val assistanceDescription: String = ""
)
