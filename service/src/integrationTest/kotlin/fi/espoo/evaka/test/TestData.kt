// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.test

import fi.espoo.evaka.application.Address
import fi.espoo.evaka.application.ApplicationDetails
import fi.espoo.evaka.application.ApplicationForm
import fi.espoo.evaka.application.ApplicationOrigin
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.ChildDetails
import fi.espoo.evaka.application.ClubDetails
import fi.espoo.evaka.application.Guardian
import fi.espoo.evaka.application.PersonBasics
import fi.espoo.evaka.application.Preferences
import fi.espoo.evaka.application.PreferredUnit
import fi.espoo.evaka.application.ServiceNeed
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testClub
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testVoucherDaycare
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

val validDaycareApplication = applicationDetails(
    PreferredUnit(
        id = testDaycare.id,
        name = testDaycare.name
    )
)

val validVoucherApplication =
    applicationDetails(
        PreferredUnit(
            id = testVoucherDaycare.id,
            name = testVoucherDaycare.name
        )
    )

private fun applicationDetails(vararg preferredUnits: PreferredUnit) = ApplicationDetails(
    id = UUID.randomUUID(),
    type = ApplicationType.DAYCARE,
    status = ApplicationStatus.WAITING_DECISION,
    origin = ApplicationOrigin.ELECTRONIC,

    childId = testChild_1.id,
    guardianId = testAdult_1.id,
    otherGuardianId = null,
    otherGuardianLivesInSameAddress = null,
    childRestricted = false,
    guardianRestricted = false,
    checkedByAdmin = true,
    createdDate = OffsetDateTime.now(),
    modifiedDate = OffsetDateTime.now(),
    sentDate = LocalDate.now(),
    dueDate = LocalDate.now(),
    dueDateSetManuallyAt = null,
    transferApplication = false,
    additionalDaycareApplication = false,
    hideFromGuardian = false,
    attachments = listOf(),

    form = ApplicationForm(
        child = ChildDetails(
            person = PersonBasics(
                firstName = testChild_1.firstName,
                lastName = testChild_1.lastName,
                socialSecurityNumber = testChild_1.ssn
            ),
            dateOfBirth = testChild_1.dateOfBirth,
            address = Address(
                street = testChild_1.streetAddress!!,
                postalCode = testChild_1.postalCode!!,
                postOffice = testChild_1.postOffice!!
            ),
            futureAddress = null,
            nationality = "fi",
            language = "fi",
            allergies = "allergies",
            diet = "diet",
            assistanceNeeded = true,
            assistanceDescription = "This is a description for assistance."
        ),
        guardian = Guardian(
            person = PersonBasics(
                firstName = testAdult_1.firstName,
                lastName = testAdult_1.lastName,
                socialSecurityNumber = testAdult_1.ssn
            ),
            address = Address(
                street = testAdult_1.streetAddress!!,
                postalCode = testAdult_1.postalCode!!,
                postOffice = testAdult_1.postOffice!!
            ),
            futureAddress = null,
            phoneNumber = "0504139432",
            email = "joku@maili.fi"
        ),
        secondGuardian = null,
        otherPartner = null,
        otherChildren = emptyList(),
        preferences = Preferences(
            preferredUnits = preferredUnits.asList(),
            preferredStartDate = LocalDate.now().plusMonths(4),
            serviceNeed = ServiceNeed(
                startTime = "08:00",
                endTime = "17:00",
                shiftCare = false,
                partTime = false
            ),
            siblingBasis = null,
            preparatory = false,
            urgent = false
        ),
        maxFeeAccepted = false,
        otherInfo = "other info",
        clubDetails = null
    )
)

val validPreschoolApplication = ApplicationDetails(
    id = UUID.randomUUID(),
    type = ApplicationType.PRESCHOOL,
    status = ApplicationStatus.WAITING_DECISION,
    origin = ApplicationOrigin.ELECTRONIC,

    childId = testChild_1.id,
    guardianId = testAdult_1.id,
    otherGuardianId = null,
    otherGuardianLivesInSameAddress = null,
    childRestricted = false,
    guardianRestricted = false,
    checkedByAdmin = true,
    createdDate = OffsetDateTime.now(),
    modifiedDate = OffsetDateTime.now(),
    sentDate = LocalDate.now(),
    dueDate = LocalDate.now(),
    dueDateSetManuallyAt = null,
    transferApplication = false,
    additionalDaycareApplication = false,
    hideFromGuardian = false,
    attachments = listOf(),

    form = ApplicationForm(
        child = ChildDetails(
            person = PersonBasics(
                firstName = testChild_1.firstName,
                lastName = testChild_1.lastName,
                socialSecurityNumber = testChild_1.ssn
            ),
            dateOfBirth = testChild_1.dateOfBirth,
            address = Address(
                street = testChild_1.streetAddress!!,
                postalCode = testChild_1.postalCode!!,
                postOffice = testChild_1.postOffice!!
            ),
            futureAddress = null,
            nationality = "fi",
            language = "fi",
            allergies = "allergies",
            diet = "diet",
            assistanceNeeded = true,
            assistanceDescription = "Description for assistance"
        ),
        guardian = Guardian(
            person = PersonBasics(
                firstName = testAdult_1.firstName,
                lastName = testAdult_1.lastName,
                socialSecurityNumber = testAdult_1.ssn
            ),
            address = Address(
                street = testAdult_1.streetAddress!!,
                postalCode = testAdult_1.postalCode!!,
                postOffice = testAdult_1.postOffice!!
            ),
            futureAddress = null,
            phoneNumber = "0504139432",
            email = "joku@maili.fi"
        ),
        secondGuardian = null,
        otherPartner = null,
        otherChildren = emptyList(),
        preferences = Preferences(
            preferredUnits = listOf(
                PreferredUnit(
                    id = testDaycare.id,
                    name = testDaycare.name
                )
            ),
            preferredStartDate = LocalDate.now().plusMonths(4),
            serviceNeed = ServiceNeed(
                startTime = "08:00",
                endTime = "17:00",
                shiftCare = false,
                partTime = false
            ),
            siblingBasis = null,
            preparatory = false,
            urgent = false
        ),
        maxFeeAccepted = false,
        otherInfo = "other info",
        clubDetails = null
    )
)

val validClubApplication = ApplicationDetails(
    id = UUID.randomUUID(),
    type = ApplicationType.CLUB,
    status = ApplicationStatus.WAITING_DECISION,
    origin = ApplicationOrigin.ELECTRONIC,

    childId = testChild_1.id,
    guardianId = testAdult_1.id,
    otherGuardianId = null,
    otherGuardianLivesInSameAddress = null,
    childRestricted = false,
    guardianRestricted = false,
    checkedByAdmin = true,
    createdDate = OffsetDateTime.now(),
    modifiedDate = OffsetDateTime.now(),
    sentDate = LocalDate.now(),
    dueDate = LocalDate.now(),
    dueDateSetManuallyAt = null,
    transferApplication = false,
    additionalDaycareApplication = false,
    hideFromGuardian = false,
    attachments = listOf(),

    form = ApplicationForm(
        child = ChildDetails(
            person = PersonBasics(
                firstName = testChild_1.firstName,
                lastName = testChild_1.lastName,
                socialSecurityNumber = testChild_1.ssn
            ),
            dateOfBirth = testChild_1.dateOfBirth,
            address = Address(
                street = testChild_1.streetAddress!!,
                postalCode = testChild_1.postalCode!!,
                postOffice = testChild_1.postOffice!!
            ),
            futureAddress = null,
            nationality = "fi",
            language = "fi",
            allergies = "",
            diet = "",
            assistanceNeeded = true,
            assistanceDescription = "hjelppiväh!"
        ),
        guardian = Guardian(
            person = PersonBasics(
                firstName = testAdult_1.firstName,
                lastName = testAdult_1.lastName,
                socialSecurityNumber = testAdult_1.ssn
            ),
            address = Address(
                street = testAdult_1.streetAddress!!,
                postalCode = testAdult_1.postalCode!!,
                postOffice = testAdult_1.postOffice!!
            ),
            futureAddress = null,
            phoneNumber = "0504139432",
            email = "joku@maili.fi"
        ),
        secondGuardian = null,
        otherPartner = null,
        otherChildren = emptyList(),
        preferences = Preferences(
            preferredUnits = listOf(
                PreferredUnit(
                    id = testClub.id!!,
                    name = testClub.name
                )
            ),
            preferredStartDate = LocalDate.now().plusMonths(4),
            serviceNeed = null,
            siblingBasis = null,
            preparatory = false,
            urgent = false
        ),
        maxFeeAccepted = false,
        otherInfo = "other info",
        clubDetails = ClubDetails(
            wasOnClubCare = true,
            wasOnDaycare = true
        )
    )
)
