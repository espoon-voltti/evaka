// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.persistence.daycare.Adult
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.Child
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.decision.DecisionStatus
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevFosterParent
import fi.espoo.evaka.shared.dev.DevGuardian
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.TestDecision
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestDecision
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.security.Action.Citizen
import fi.espoo.evaka.test.getApplicationStatus
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class ApplicationControllerCitizenIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var applicationControllerCitizen: ApplicationControllerCitizen

    @Autowired private lateinit var stateService: ApplicationStateService

    private val clock = MockEvakaClock(2020, 1, 1, 12, 0)

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val adult = DevPerson(ssn = "010180-1232")
    private val child = DevPerson(ssn = "010617A123U")
    private val decisionMaker = DevEmployee()

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(decisionMaker)
            tx.insert(DevGuardian(guardianId = adult.id, childId = child.id))
        }
        MockPersonDetailsService.addPersons(adult, child)
        MockPersonDetailsService.addDependants(adult, child)
    }

    @Test
    fun `user can delete a draft application`() {
        val applicationId =
            db.transaction { tx ->
                tx.insertTestApplication(
                    guardianId = adult.id,
                    childId = child.id,
                    status = ApplicationStatus.CREATED,
                    type = ApplicationType.DAYCARE,
                    document = daycareApplicationDocument(),
                )
            }

        applicationControllerCitizen.deleteOrCancelUnprocessedApplication(
            db = dbInstance(),
            user = AuthenticatedUser.Citizen(adult.id, CitizenAuthLevel.STRONG),
            clock = clock,
            applicationId = applicationId,
        )

        db.transaction { tx -> assertNull(tx.fetchApplicationDetails(applicationId)) }
    }

    @Test
    fun `user can cancel a sent unprocessed application`() {
        val applicationId =
            db.transaction { tx ->
                tx.insertTestApplication(
                        guardianId = adult.id,
                        childId = child.id,
                        status = ApplicationStatus.CREATED,
                        type = ApplicationType.DAYCARE,
                        document = daycareApplicationDocument(),
                    )
                    .also {
                        stateService.sendApplication(
                            tx = tx,
                            user = AuthenticatedUser.Citizen(adult.id, CitizenAuthLevel.STRONG),
                            clock = clock,
                            applicationId = it,
                        )
                    }
            }

        applicationControllerCitizen.deleteOrCancelUnprocessedApplication(
            db = dbInstance(),
            user = AuthenticatedUser.Citizen(adult.id, CitizenAuthLevel.STRONG),
            clock = clock,
            applicationId = applicationId,
        )

        db.transaction { tx ->
            assertEquals(ApplicationStatus.CANCELLED, tx.getApplicationStatus(applicationId))
        }
    }

    @Test
    fun `user can not cancel a processed application`() {
        val applicationId =
            db.transaction { tx ->
                tx.insertTestApplication(
                        guardianId = adult.id,
                        childId = child.id,
                        status = ApplicationStatus.CREATED,
                        type = ApplicationType.DAYCARE,
                        document = daycareApplicationDocument(),
                    )
                    .also {
                        stateService.sendApplication(
                            tx = tx,
                            user = AuthenticatedUser.Citizen(adult.id, CitizenAuthLevel.STRONG),
                            clock = clock,
                            applicationId = it,
                        )
                        stateService.moveToWaitingPlacement(
                            tx = tx,
                            user =
                                AuthenticatedUser.Employee(
                                    decisionMaker.id,
                                    setOf(UserRole.SERVICE_WORKER),
                                ),
                            clock = clock,
                            applicationId = it,
                        )
                    }
            }

        assertThrows<BadRequest> {
            applicationControllerCitizen.deleteOrCancelUnprocessedApplication(
                db = dbInstance(),
                user = AuthenticatedUser.Citizen(adult.id, CitizenAuthLevel.STRONG),
                clock = clock,
                applicationId = applicationId,
            )
        }
    }

    @Test
    fun `getGuardianApplications returns applications grouped by child`() {
        val child2 = DevPerson(ssn = "020617A124V")
        db.transaction { tx ->
            tx.insert(child2, DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = adult.id, childId = child2.id))
            tx.insertTestApplication(
                guardianId = adult.id,
                childId = child.id,
                type = ApplicationType.DAYCARE,
                document = daycareApplicationDocument(),
            )
            tx.insertTestApplication(
                guardianId = adult.id,
                childId = child2.id,
                type = ApplicationType.PRESCHOOL,
                document = preschoolApplicationDocument(),
            )
        }
        MockPersonDetailsService.addPersons(child2)
        MockPersonDetailsService.addDependants(adult, child2)

        val result =
            applicationControllerCitizen.getGuardianApplications(
                db = dbInstance(),
                user = AuthenticatedUser.Citizen(adult.id, CitizenAuthLevel.STRONG),
                clock = clock,
            )

        assertEquals(2, result.size)
        val childApplications = result.find { it.childId == child.id }!!
        val child2Applications = result.find { it.childId == child2.id }!!
        assertEquals(1, childApplications.applicationSummaries.size)
        assertEquals(1, child2Applications.applicationSummaries.size)
        assertEquals(ApplicationType.DAYCARE, childApplications.applicationSummaries[0].type)
        assertEquals(ApplicationType.PRESCHOOL, child2Applications.applicationSummaries[0].type)
    }

    @Test
    fun `getGuardianApplications returns applications shared via other guardian`() {
        val otherGuardian = DevPerson(ssn = "020180-1233")
        db.transaction { tx ->
            tx.insert(otherGuardian, DevPersonType.ADULT)
            tx.insert(DevGuardian(guardianId = otherGuardian.id, childId = child.id))
            tx.insertTestApplication(
                guardianId = adult.id,
                childId = child.id,
                type = ApplicationType.DAYCARE,
                otherGuardians = setOf(otherGuardian.id),
                document =
                    DaycareFormV0.fromApplication2(createTestApplicationDetails(adult, child)),
            )
        }
        MockPersonDetailsService.addPersons(otherGuardian)
        MockPersonDetailsService.addDependants(otherGuardian, child)

        val result =
            applicationControllerCitizen.getGuardianApplications(
                db = dbInstance(),
                user = AuthenticatedUser.Citizen(otherGuardian.id, CitizenAuthLevel.STRONG),
                clock = clock,
            )

        assertEquals(1, result.size)
        assertEquals(child.id, result[0].childId)
        assertEquals(1, result[0].applicationSummaries.size)
        assertEquals(ApplicationType.DAYCARE, result[0].applicationSummaries[0].type)
    }

    @Test
    fun `getGuardianApplications does not return hidden applications`() {
        db.transaction { tx ->
            tx.insertTestApplication(
                guardianId = adult.id,
                childId = child.id,
                type = ApplicationType.DAYCARE,
                hideFromGuardian = false,
                document =
                    DaycareFormV0.fromApplication2(createTestApplicationDetails(adult, child)),
            )
            tx.insertTestApplication(
                guardianId = adult.id,
                childId = child.id,
                type = ApplicationType.PRESCHOOL,
                hideFromGuardian = true,
                document =
                    DaycareFormV0.fromApplication2(createTestApplicationDetails(adult, child)),
            )
        }

        val result =
            applicationControllerCitizen.getGuardianApplications(
                db = dbInstance(),
                user = AuthenticatedUser.Citizen(adult.id, CitizenAuthLevel.STRONG),
                clock = clock,
            )

        assertEquals(1, result.size)
        assertEquals(1, result[0].applicationSummaries.size)
        assertEquals(ApplicationType.DAYCARE, result[0].applicationSummaries[0].type)
    }

    @Test
    fun `getGuardianApplications does not return children for non-guardians`() {
        val nonGuardian = DevPerson(ssn = "040180-1232")
        val nonGuardianChild = DevPerson(ssn = "040617A123U")

        db.transaction { tx ->
            tx.insert(nonGuardian, DevPersonType.ADULT)
            tx.insert(nonGuardianChild, DevPersonType.CHILD)

            tx.insertTestApplication(
                guardianId = nonGuardian.id,
                childId = nonGuardianChild.id,
                type = ApplicationType.DAYCARE,
                document =
                    DaycareFormV0.fromApplication2(
                        createTestApplicationDetails(nonGuardian, nonGuardianChild)
                    ),
            )
        }

        val result =
            applicationControllerCitizen.getGuardianApplications(
                db = dbInstance(),
                user = AuthenticatedUser.Citizen(nonGuardian.id, CitizenAuthLevel.STRONG),
                clock = clock,
            )

        assertEquals(0, result.size)
    }

    @Test
    fun `application notification count is non-zero for guardian with pending decision`() {
        val guardian = DevPerson(ssn = "030180-1232")
        val guardianChild = DevPerson(ssn = "030617A123U")
        val applicationId = ApplicationId(UUID.randomUUID())

        db.transaction { tx ->
            tx.insert(guardian, DevPersonType.ADULT)
            tx.insert(guardianChild, DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = guardian.id, childId = guardianChild.id))

            tx.insertTestApplication(
                id = applicationId,
                status = ApplicationStatus.WAITING_CONFIRMATION,
                confidential = true,
                childId = guardianChild.id,
                guardianId = guardian.id,
                type = ApplicationType.DAYCARE,
                document =
                    DaycareFormV0.fromApplication2(
                        createTestApplicationDetails(guardian, guardianChild)
                    ),
            )

            tx.insertTestDecision(
                TestDecision(
                    applicationId = applicationId,
                    status = DecisionStatus.PENDING,
                    createdBy = EvakaUserId(decisionMaker.id.raw),
                    unitId = daycare.id,
                    type = DecisionType.DAYCARE,
                    startDate = clock.today(),
                    endDate = clock.today().plusYears(1),
                    resolvedBy = decisionMaker.id.raw,
                    sentDate = clock.today(),
                )
            )
        }

        val notificationCount =
            applicationControllerCitizen.getGuardianApplicationNotifications(
                db = dbInstance(),
                user = AuthenticatedUser.Citizen(guardian.id, CitizenAuthLevel.STRONG),
                clock = clock,
            )

        assertEquals(1, notificationCount)
    }

    @Test
    fun `application notification count is zero for non-guardian`() {
        val nonGuardian = DevPerson(ssn = "020180-1232")
        val nonGuardianChild = DevPerson(ssn = "020617A123U")
        val applicationId = ApplicationId(UUID.randomUUID())

        db.transaction { tx ->
            tx.insert(nonGuardian, DevPersonType.ADULT)
            tx.insert(nonGuardianChild, DevPersonType.CHILD)

            tx.insertTestApplication(
                id = applicationId,
                status = ApplicationStatus.WAITING_CONFIRMATION,
                confidential = true,
                childId = nonGuardianChild.id,
                guardianId = nonGuardian.id,
                type = ApplicationType.DAYCARE,
                document =
                    DaycareFormV0.fromApplication2(
                        createTestApplicationDetails(nonGuardian, nonGuardianChild)
                    ),
            )

            tx.insertTestDecision(
                TestDecision(
                    applicationId = applicationId,
                    status = DecisionStatus.PENDING,
                    createdBy = EvakaUserId(decisionMaker.id.raw),
                    unitId = daycare.id,
                    type = DecisionType.DAYCARE,
                    startDate = clock.today(),
                    endDate = clock.today().plusYears(1),
                    resolvedBy = decisionMaker.id.raw,
                    sentDate = clock.today(),
                )
            )
        }

        val notificationCount =
            applicationControllerCitizen.getGuardianApplicationNotifications(
                db = dbInstance(),
                user = AuthenticatedUser.Citizen(nonGuardian.id, CitizenAuthLevel.STRONG),
                clock = clock,
            )

        assertEquals(0, notificationCount)
    }

    @Test
    fun `getPendingDecisions returns pending decision with valid start date period and permitted actions`() {
        val applicationId = ApplicationId(UUID.randomUUID())
        val rejectedApplicationId = ApplicationId(UUID.randomUUID())

        db.transaction { tx ->
            tx.insertTestApplication(
                id = applicationId,
                status = ApplicationStatus.WAITING_CONFIRMATION,
                confidential = true,
                childId = child.id,
                guardianId = adult.id,
                type = ApplicationType.DAYCARE,
                document =
                    DaycareFormV0.fromApplication2(createTestApplicationDetails(adult, child)),
            )

            tx.insertTestDecision(
                TestDecision(
                    applicationId = applicationId,
                    status = DecisionStatus.PENDING,
                    createdBy = EvakaUserId(decisionMaker.id.raw),
                    unitId = daycare.id,
                    type = DecisionType.DAYCARE,
                    startDate = clock.today(),
                    endDate = clock.today().plusYears(1),
                    resolvedBy = decisionMaker.id.raw,
                    sentDate = clock.today(),
                )
            )
            // Rejected application and related decision should not be returned
            tx.insertTestApplication(
                id = rejectedApplicationId,
                status = ApplicationStatus.REJECTED,
                confidential = true,
                childId = child.id,
                guardianId = adult.id,
                type = ApplicationType.DAYCARE,
                document =
                    DaycareFormV0.fromApplication2(createTestApplicationDetails(adult, child)),
            )

            tx.insertTestDecision(
                TestDecision(
                    applicationId = rejectedApplicationId,
                    status = DecisionStatus.REJECTED,
                    createdBy = EvakaUserId(decisionMaker.id.raw),
                    unitId = daycare.id,
                    type = DecisionType.DAYCARE,
                    startDate = clock.today(),
                    endDate = clock.today().plusYears(1),
                    resolvedBy = decisionMaker.id.raw,
                    sentDate = clock.today(),
                )
            )
        }

        val pendingDecisions =
            applicationControllerCitizen.getPendingDecisions(
                db = dbInstance(),
                user = AuthenticatedUser.Citizen(adult.id, CitizenAuthLevel.STRONG),
                clock = clock,
            )

        assertEquals(1, pendingDecisions.size)
        val decisionWithPeriod = pendingDecisions[0]

        assertEquals(applicationId, decisionWithPeriod.decision.applicationId)
        assertEquals(child.id, decisionWithPeriod.decision.childId)
        assertEquals(DecisionStatus.PENDING, decisionWithPeriod.decision.status)
        assertEquals(DecisionType.DAYCARE, decisionWithPeriod.decision.type)
        assertEquals(daycare.id, decisionWithPeriod.decision.unit.id)

        val expectedStartDate = clock.today()
        val expectedEndDate = clock.today().plusDays(14)
        assertEquals(expectedStartDate, decisionWithPeriod.validRequestedStartDatePeriod.start)
        assertEquals(expectedEndDate, decisionWithPeriod.validRequestedStartDatePeriod.end)

        assertEquals(
            setOf(Citizen.Decision.READ, Citizen.Decision.DOWNLOAD_PDF),
            decisionWithPeriod.permittedActions,
        )
    }

    @Test
    fun `getPendingDecisions does not return decision when foster parent relationship has ended`() {
        val fosterParent = DevPerson(ssn = "050180-1232")
        val fosterChild = DevPerson(ssn = "050617A123U")
        val applicationId = ApplicationId(UUID.randomUUID())

        db.transaction { tx ->
            tx.insert(fosterParent, DevPersonType.ADULT)
            tx.insert(fosterChild, DevPersonType.CHILD)

            // Create foster parent relationship that ended yesterday
            tx.insert(
                DevFosterParent(
                    childId = fosterChild.id,
                    parentId = fosterParent.id,
                    validDuring =
                        DateRange(clock.today().minusYears(1), clock.today().minusDays(1)),
                    modifiedAt = clock.now(),
                    modifiedBy = EvakaUserId(decisionMaker.id.raw),
                )
            )

            tx.insertTestApplication(
                id = applicationId,
                status = ApplicationStatus.WAITING_CONFIRMATION,
                confidential = true,
                childId = fosterChild.id,
                guardianId = fosterParent.id,
                type = ApplicationType.DAYCARE,
                document =
                    DaycareFormV0.fromApplication2(
                        createTestApplicationDetails(fosterParent, fosterChild)
                    ),
            )

            tx.insertTestDecision(
                TestDecision(
                    applicationId = applicationId,
                    status = DecisionStatus.PENDING,
                    createdBy = EvakaUserId(decisionMaker.id.raw),
                    unitId = daycare.id,
                    type = DecisionType.DAYCARE,
                    startDate = clock.today(),
                    endDate = clock.today().plusYears(1),
                    resolvedBy = decisionMaker.id.raw,
                    sentDate = clock.today(),
                )
            )
        }

        MockPersonDetailsService.addPersons(fosterParent, fosterChild)

        val pendingDecisions =
            applicationControllerCitizen.getPendingDecisions(
                db = dbInstance(),
                user = AuthenticatedUser.Citizen(fosterParent.id, CitizenAuthLevel.STRONG),
                clock = clock,
            )

        assertEquals(0, pendingDecisions.size)
    }

    private fun daycareApplicationDocument() =
        DaycareFormV0(
            type = ApplicationType.DAYCARE,
            child = Child(dateOfBirth = null),
            guardian = Adult(),
            apply = Apply(preferredUnits = listOf(daycare.id)),
            preferredStartDate = LocalDate.of(2020, 6, 1),
            serviceStart = "09:00",
            serviceEnd = "17:00",
        )

    private fun preschoolApplicationDocument() =
        DaycareFormV0(
            type = ApplicationType.PRESCHOOL,
            child = Child(dateOfBirth = null),
            guardian = Adult(),
            apply = Apply(preferredUnits = listOf(daycare.id)),
            preferredStartDate = LocalDate.of(2020, 8, 13),
        )

    private fun createTestApplicationDetails(
        guardian: DevPerson,
        child: DevPerson,
    ): ApplicationDetails =
        ApplicationDetails(
            id = ApplicationId(UUID.randomUUID()),
            type = ApplicationType.DAYCARE,
            form =
                ApplicationForm(
                    child =
                        ChildDetails(
                            person =
                                PersonBasics(
                                    firstName = child.firstName,
                                    lastName = child.lastName,
                                    socialSecurityNumber = child.ssn,
                                ),
                            dateOfBirth = child.dateOfBirth,
                            address = Address("", "", ""),
                            futureAddress = null,
                            nationality = "fi",
                            language = "fi",
                            allergies = "",
                            diet = "",
                            assistanceNeeded = false,
                            assistanceDescription = "",
                        ),
                    guardian =
                        Guardian(
                            person =
                                PersonBasics(
                                    firstName = guardian.firstName,
                                    lastName = guardian.lastName,
                                    socialSecurityNumber = guardian.ssn,
                                ),
                            address = Address("", "", ""),
                            futureAddress = null,
                            phoneNumber = "",
                            email = "",
                        ),
                    preferences =
                        Preferences(
                            preferredUnits = emptyList(),
                            preferredStartDate = null,
                            connectedDaycarePreferredStartDate = null,
                            serviceNeed = null,
                            siblingBasis = null,
                            preparatory = false,
                            urgent = false,
                        ),
                    secondGuardian = null,
                    otherPartner = null,
                    otherChildren = emptyList(),
                    otherInfo = "",
                    maxFeeAccepted = false,
                    clubDetails = null,
                ),
            status = ApplicationStatus.SENT,
            origin = ApplicationOrigin.ELECTRONIC,
            childId = child.id,
            childRestricted = false,
            guardianId = guardian.id,
            guardianRestricted = false,
            guardianDateOfDeath = null,
            createdAt = HelsinkiDateTime.now(),
            createdBy = null,
            modifiedAt = HelsinkiDateTime.now(),
            modifiedBy = null,
            sentDate = LocalDate.now(),
            sentTime = null,
            dueDate = null,
            dueDateSetManuallyAt = null,
            transferApplication = false,
            additionalDaycareApplication = false,
            otherGuardianLivesInSameAddress = null,
            checkedByAdmin = false,
            confidential = null,
            hideFromGuardian = false,
            allowOtherGuardianAccess = true,
            attachments = emptyList(),
            hasOtherGuardian = false,
        )
}
