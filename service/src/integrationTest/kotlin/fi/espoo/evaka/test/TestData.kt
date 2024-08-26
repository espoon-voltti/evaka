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
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

val defaultPreferredUnit = PreferredUnit(id = testDaycare.id, name = testDaycare.name)

fun getValidDaycareApplication(
    preferredUnit: DevDaycare = testDaycare,
    shiftCare: Boolean = false,
) = applicationDetails(PreferredUnit(preferredUnit.id, preferredUnit.name), shiftCare = shiftCare)

val validDaycareApplication = getValidDaycareApplication()

private fun applicationDetails(vararg preferredUnits: PreferredUnit, shiftCare: Boolean = false) =
    ApplicationDetails(
        id = ApplicationId(UUID.randomUUID()),
        type = ApplicationType.DAYCARE,
        status = ApplicationStatus.WAITING_DECISION,
        origin = ApplicationOrigin.ELECTRONIC,
        childId = testChild_1.id,
        guardianId = testAdult_1.id,
        otherGuardianLivesInSameAddress = null,
        childRestricted = false,
        guardianRestricted = false,
        guardianDateOfDeath = null,
        checkedByAdmin = true,
        createdDate = HelsinkiDateTime.of(LocalDate.of(2021, 8, 15), LocalTime.of(12, 0)),
        modifiedDate = HelsinkiDateTime.of(LocalDate.of(2021, 8, 15), LocalTime.of(12, 0)),
        sentDate = LocalDate.of(2021, 1, 15),
        dueDate = null,
        dueDateSetManuallyAt = null,
        transferApplication = false,
        additionalDaycareApplication = false,
        hideFromGuardian = false,
        allowOtherGuardianAccess = true,
        attachments = listOf(),
        hasOtherGuardian = false,
        form =
            ApplicationForm(
                child =
                    ChildDetails(
                        person =
                            PersonBasics(
                                firstName = testChild_1.firstName,
                                lastName = testChild_1.lastName,
                                socialSecurityNumber = testChild_1.ssn,
                            ),
                        dateOfBirth = testChild_1.dateOfBirth,
                        address =
                            Address(
                                street = testChild_1.streetAddress,
                                postalCode = testChild_1.postalCode,
                                postOffice = testChild_1.postOffice,
                            ),
                        futureAddress = null,
                        nationality = "fi",
                        language = "fi",
                        allergies = "allergies",
                        diet = "diet",
                        assistanceNeeded = true,
                        assistanceDescription = "This is a description for assistance.",
                    ),
                guardian =
                    Guardian(
                        person =
                            PersonBasics(
                                firstName = testAdult_1.firstName,
                                lastName = testAdult_1.lastName,
                                socialSecurityNumber = testAdult_1.ssn,
                            ),
                        address =
                            Address(
                                street = testAdult_1.streetAddress,
                                postalCode = testAdult_1.postalCode,
                                postOffice = testAdult_1.postOffice,
                            ),
                        futureAddress = null,
                        phoneNumber = "0504139432",
                        email = "joku@maili.fi",
                    ),
                secondGuardian = null,
                otherPartner = null,
                otherChildren = emptyList(),
                preferences =
                    Preferences(
                        preferredUnits = preferredUnits.asList(),
                        preferredStartDate = LocalDate.of(2021, 8, 15),
                        connectedDaycarePreferredStartDate = null,
                        serviceNeed =
                            ServiceNeed(
                                startTime = "08:00",
                                endTime = "17:00",
                                shiftCare = shiftCare,
                                partTime = false,
                                serviceNeedOption = null,
                            ),
                        siblingBasis = null,
                        preparatory = false,
                        urgent = false,
                    ),
                maxFeeAccepted = false,
                otherInfo = "other info",
                clubDetails = null,
            ),
    )

val validPreschoolApplication =
    ApplicationDetails(
        id = ApplicationId(UUID.randomUUID()),
        type = ApplicationType.PRESCHOOL,
        status = ApplicationStatus.WAITING_DECISION,
        origin = ApplicationOrigin.ELECTRONIC,
        childId = testChild_1.id,
        guardianId = testAdult_1.id,
        otherGuardianLivesInSameAddress = null,
        childRestricted = false,
        guardianRestricted = false,
        guardianDateOfDeath = null,
        checkedByAdmin = true,
        createdDate = HelsinkiDateTime.of(LocalDate.of(2021, 8, 15), LocalTime.of(12, 0)),
        modifiedDate = HelsinkiDateTime.of(LocalDate.of(2021, 8, 15), LocalTime.of(12, 0)),
        sentDate = LocalDate.of(2021, 1, 15),
        dueDate = null,
        dueDateSetManuallyAt = null,
        transferApplication = false,
        additionalDaycareApplication = false,
        hideFromGuardian = false,
        allowOtherGuardianAccess = true,
        attachments = listOf(),
        hasOtherGuardian = false,
        form =
            ApplicationForm(
                child =
                    ChildDetails(
                        person =
                            PersonBasics(
                                firstName = testChild_1.firstName,
                                lastName = testChild_1.lastName,
                                socialSecurityNumber = testChild_1.ssn,
                            ),
                        dateOfBirth = testChild_1.dateOfBirth,
                        address =
                            Address(
                                street = testChild_1.streetAddress,
                                postalCode = testChild_1.postalCode,
                                postOffice = testChild_1.postOffice,
                            ),
                        futureAddress = null,
                        nationality = "fi",
                        language = "fi",
                        allergies = "allergies",
                        diet = "diet",
                        assistanceNeeded = true,
                        assistanceDescription = "Description for assistance",
                    ),
                guardian =
                    Guardian(
                        person =
                            PersonBasics(
                                firstName = testAdult_1.firstName,
                                lastName = testAdult_1.lastName,
                                socialSecurityNumber = testAdult_1.ssn,
                            ),
                        address =
                            Address(
                                street = testAdult_1.streetAddress,
                                postalCode = testAdult_1.postalCode,
                                postOffice = testAdult_1.postOffice,
                            ),
                        futureAddress = null,
                        phoneNumber = "0504139432",
                        email = "joku@maili.fi",
                    ),
                secondGuardian = null,
                otherPartner = null,
                otherChildren = emptyList(),
                preferences =
                    Preferences(
                        preferredUnits =
                            listOf(PreferredUnit(id = testDaycare.id, name = testDaycare.name)),
                        preferredStartDate = LocalDate.of(2021, 8, 15),
                        connectedDaycarePreferredStartDate = null,
                        serviceNeed =
                            ServiceNeed(
                                startTime = "08:00",
                                endTime = "17:00",
                                shiftCare = false,
                                partTime = false,
                                serviceNeedOption = null,
                            ),
                        siblingBasis = null,
                        preparatory = false,
                        urgent = false,
                    ),
                maxFeeAccepted = false,
                otherInfo = "other info",
                clubDetails = null,
            ),
    )

fun validClubApplication(preferredUnit: DevDaycare, preferredStartDate: LocalDate) =
    ApplicationDetails(
        id = ApplicationId(UUID.randomUUID()),
        type = ApplicationType.CLUB,
        status = ApplicationStatus.WAITING_DECISION,
        origin = ApplicationOrigin.ELECTRONIC,
        childId = testChild_1.id,
        guardianId = testAdult_1.id,
        otherGuardianLivesInSameAddress = null,
        childRestricted = false,
        guardianRestricted = false,
        guardianDateOfDeath = null,
        checkedByAdmin = true,
        createdDate = HelsinkiDateTime.of(LocalDate.of(2021, 8, 15), LocalTime.of(12, 0)),
        modifiedDate = HelsinkiDateTime.of(LocalDate.of(2021, 8, 15), LocalTime.of(12, 0)),
        sentDate = LocalDate.of(2021, 1, 15),
        dueDate = null,
        dueDateSetManuallyAt = null,
        transferApplication = false,
        additionalDaycareApplication = false,
        hideFromGuardian = false,
        allowOtherGuardianAccess = true,
        attachments = listOf(),
        hasOtherGuardian = false,
        form =
            ApplicationForm(
                child =
                    ChildDetails(
                        person =
                            PersonBasics(
                                firstName = testChild_1.firstName,
                                lastName = testChild_1.lastName,
                                socialSecurityNumber = testChild_1.ssn,
                            ),
                        dateOfBirth = testChild_1.dateOfBirth,
                        address =
                            Address(
                                street = testChild_1.streetAddress,
                                postalCode = testChild_1.postalCode,
                                postOffice = testChild_1.postOffice,
                            ),
                        futureAddress = null,
                        nationality = "fi",
                        language = "fi",
                        allergies = "",
                        diet = "",
                        assistanceNeeded = true,
                        assistanceDescription = "hjelppiväh!",
                    ),
                guardian =
                    Guardian(
                        person =
                            PersonBasics(
                                firstName = testAdult_1.firstName,
                                lastName = testAdult_1.lastName,
                                socialSecurityNumber = testAdult_1.ssn,
                            ),
                        address =
                            Address(
                                street = testAdult_1.streetAddress,
                                postalCode = testAdult_1.postalCode,
                                postOffice = testAdult_1.postOffice,
                            ),
                        futureAddress = null,
                        phoneNumber = "0504139432",
                        email = "joku@maili.fi",
                    ),
                secondGuardian = null,
                otherPartner = null,
                otherChildren = emptyList(),
                preferences =
                    Preferences(
                        preferredUnits =
                            listOf(PreferredUnit(id = preferredUnit.id, name = preferredUnit.name)),
                        preferredStartDate = preferredStartDate,
                        connectedDaycarePreferredStartDate = null,
                        serviceNeed = null,
                        siblingBasis = null,
                        preparatory = false,
                        urgent = false,
                    ),
                maxFeeAccepted = false,
                otherInfo = "other info",
                clubDetails = ClubDetails(wasOnClubCare = true, wasOnDaycare = true),
            ),
    )
