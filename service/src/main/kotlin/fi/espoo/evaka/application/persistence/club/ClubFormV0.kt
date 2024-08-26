// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application.persistence.club

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import fi.espoo.evaka.application.persistence.DatabaseForm
import fi.espoo.evaka.shared.DaycareId
import java.time.LocalDate

const val DEFAULT_CHILD_NATIONALITY = "FI"
const val DEFAULT_CHILD_LANGUAGE = "fi"

@JsonIgnoreProperties(
    "guardian2",
    "hasSecondGuardian",
    "guardiansSeparated",
    "guardianInformed",
    "careFactor",
    "term",
)
data class ClubFormV0(
    val child: Child,
    val guardian: Adult,
    val apply: Apply = Apply(),
    override val preferredStartDate: LocalDate? = null,
    val wasOnDaycare: Boolean = false,
    val wasOnClubCare: Boolean = false,
    val clubCare: ClubCare = ClubCare(),
    val docVersion: Long = 0L,
    val additionalDetails: ClubAdditionalDetails = ClubAdditionalDetails(),
) : DatabaseForm.ClubForm() {
    override fun hideGuardianAddress(): ClubFormV0 {
        return this.copy(
            guardian =
                this.guardian.copy(
                    address = Address(),
                    correctingAddress = Address(),
                    restricted = true,
                )
        )
    }

    override fun hideChildAddress(): ClubFormV0 {
        return this.copy(
            child =
                this.child.copy(
                    address = Address(),
                    correctingAddress = Address(),
                    restricted = true,
                )
        )
    }

    companion object {
        fun fromForm2(
            form: fi.espoo.evaka.application.ApplicationForm,
            childRestricted: Boolean,
            guardianRestricted: Boolean,
        ) =
            ClubFormV0(
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
                                    editable = false,
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
                                    editable = true,
                                )
                            } ?: Address(editable = true),
                        childMovingDate = form.child.futureAddress?.movingDate,
                        restricted = childRestricted,
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
                                    editable = false,
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
                                    editable = true,
                                )
                            } ?: Address(editable = true),
                        guardianMovingDate = form.guardian.futureAddress?.movingDate,
                        restricted = guardianRestricted,
                    ),
                apply =
                    Apply(
                        preferredUnits = form.preferences.preferredUnits.map { it.id },
                        siblingBasis = form.preferences.siblingBasis != null,
                        siblingName = form.preferences.siblingBasis?.siblingName ?: "",
                        siblingSsn = form.preferences.siblingBasis?.siblingSsn ?: "",
                    ),
                preferredStartDate = form.preferences.preferredStartDate,
                clubCare =
                    ClubCare(
                        assistanceNeeded = form.child.assistanceNeeded,
                        assistanceDescription = form.child.assistanceDescription,
                    ),
                additionalDetails = ClubAdditionalDetails(otherInfo = form.otherInfo),
                wasOnClubCare = form.clubDetails?.wasOnClubCare ?: false,
                wasOnDaycare = form.clubDetails?.wasOnDaycare ?: false,
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
    val restricted: Boolean = false,
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
    val restricted: Boolean = false,
)

data class Address(
    val street: String = "",
    val postalCode: String = "",
    val city: String = "",
    val editable: Boolean = false,
)

data class ClubAdditionalDetails(val otherInfo: String = "")

data class Apply(
    val preferredUnits: List<DaycareId> = emptyList(),
    val siblingBasis: Boolean = false,
    val siblingName: String = "",
    val siblingSsn: String = "",
)

data class ClubCare(
    val assistanceNeeded: Boolean = false,
    val assistanceDescription: String = "",
    val assistanceAdditionalDetails: String = "",
)
