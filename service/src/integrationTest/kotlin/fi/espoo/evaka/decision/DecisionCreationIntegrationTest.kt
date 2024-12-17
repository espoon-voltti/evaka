// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.decision

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.ApplicationControllerCitizen
import fi.espoo.evaka.application.ApplicationControllerV2
import fi.espoo.evaka.application.ApplicationDecisions
import fi.espoo.evaka.application.ApplicationStateService
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ChildInfo
import fi.espoo.evaka.application.DaycarePlacementPlan
import fi.espoo.evaka.application.DecisionDraftGroup
import fi.espoo.evaka.application.DecisionSummary
import fi.espoo.evaka.application.GuardianInfo
import fi.espoo.evaka.application.SimpleApplicationAction
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.CareDetails
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.daycare.Daycare
import fi.espoo.evaka.daycare.DaycareDecisionCustomization
import fi.espoo.evaka.daycare.VisitingAddress
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.pis.service.blockGuardian
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.job.ScheduledJobs
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.test.DecisionTableRow
import fi.espoo.evaka.test.getApplicationStatus
import fi.espoo.evaka.test.getDecisionRowsByApplication
import fi.espoo.evaka.toApplicationType
import fi.espoo.evaka.toDaycareFormAdult
import fi.espoo.evaka.toDaycareFormChild
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class DecisionCreationIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val clock = MockEvakaClock(2024, 12, 16, 13, 32, 25)
    private lateinit var serviceWorker: AuthenticatedUser.Employee
    private lateinit var testDaycare: Daycare

    @Autowired private lateinit var applicationController: ApplicationControllerV2
    @Autowired private lateinit var applicationControllerCitizen: ApplicationControllerCitizen
    @Autowired private lateinit var scheduledJobs: ScheduledJobs
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    @Autowired private lateinit var applicationStateService: ApplicationStateService
    @Autowired private lateinit var personService: PersonService

    private val decisionId = DecisionId(UUID.randomUUID())

    @BeforeEach
    fun beforeEach() {
        serviceWorker =
            db.transaction { tx ->
                val employeeId = tx.insert(DevEmployee(firstName = "Decision", lastName = "Maker"))
                AuthenticatedUser.Employee(employeeId, setOf(UserRole.SERVICE_WORKER))
            }
        testDaycare =
            db.transaction { tx ->
                val areaId = tx.insert(DevCareArea())
                val unitId =
                    tx.insert(
                        DevDaycare(
                            areaId = areaId,
                            name = "Test Daycare",
                            decisionCustomization =
                                DaycareDecisionCustomization(
                                    daycareName = "Test Daycare / daycare",
                                    preschoolName = "Test Daycare / preschool",
                                    handler = "Test decision handler",
                                    handlerAddress = "Test decision handler address",
                                ),
                            visitingAddress =
                                VisitingAddress(
                                    streetAddress = "Test address",
                                    postalCode = "Test postal code",
                                    postOffice = "Test post office",
                                ),
                            phone = "Test phone",
                        )
                    )
                tx.getDaycare(unitId)!!
            }
    }

    @Test
    fun testDaycareFullTime() {
        val guardian = DevPerson(ssn = "070644-937X")
        val otherGuardian = DevPerson(ssn = "311299-999E")
        val child = DevPerson(ssn = "070714A9126")
        db.transaction { tx ->
            listOf(guardian, otherGuardian).forEach { tx.insert(it, DevPersonType.ADULT) }
            tx.insert(child, DevPersonType.CHILD)
        }
        MockPersonDetailsService.addPersons(guardian, otherGuardian, child)
        MockPersonDetailsService.addDependants(guardian, child)
        MockPersonDetailsService.addDependants(otherGuardian, child)
        val period = FiniteDateRange(LocalDate.of(2020, 3, 17), LocalDate.of(2023, 7, 31))
        val applicationId =
            insertInitialData(
                type = PlacementType.DAYCARE,
                adult = guardian,
                child = child,
                period = period,
            )
        checkDecisionDrafts(
            applicationId,
            adult = guardian,
            child = child,
            decisions =
                listOf(
                    DecisionDraft(
                        id = decisionId,
                        unitId = testDaycare.id,
                        type = DecisionType.DAYCARE,
                        startDate = period.start,
                        endDate = period.end,
                        planned = true,
                    )
                ),
            otherGuardian = otherGuardian,
        )

        val decisions = createDecisions(applicationId)
        assertEquals(1, decisions.size)

        val decision = decisions[0]
        assertEquals(testDaycare.id, decision.unitId)
        assertEquals(DecisionType.DAYCARE, decision.type)
        assertEquals(period.start, decision.startDate)
        assertEquals(period.end, decision.endDate)
    }

    @Test
    fun testDaycarePartTime() {
        val guardian = DevPerson(ssn = "070644-937X")
        val otherGuardian = DevPerson(ssn = "311299-999E")
        val child = DevPerson(ssn = "070714A9126")
        db.transaction { tx ->
            listOf(guardian, otherGuardian).forEach { tx.insert(it, DevPersonType.ADULT) }
            tx.insert(child, DevPersonType.CHILD)
        }
        MockPersonDetailsService.addPersons(guardian, otherGuardian, child)
        MockPersonDetailsService.addDependants(guardian, child)
        MockPersonDetailsService.addDependants(otherGuardian, child)
        val period = FiniteDateRange(LocalDate.of(2020, 3, 18), LocalDate.of(2023, 7, 29))
        val applicationId =
            insertInitialData(
                type = PlacementType.DAYCARE_PART_TIME,
                adult = guardian,
                child = child,
                period = period,
            )
        checkDecisionDrafts(
            applicationId,
            adult = guardian,
            child = child,
            decisions =
                listOf(
                    DecisionDraft(
                        id = decisionId,
                        unitId = testDaycare.id,
                        type = DecisionType.DAYCARE_PART_TIME,
                        startDate = period.start,
                        endDate = period.end,
                        planned = true,
                    )
                ),
            otherGuardian = otherGuardian,
        )

        val decisions = createDecisions(applicationId)
        assertEquals(1, decisions.size)

        val decision = decisions[0]
        assertEquals(testDaycare.id, decision.unitId)
        assertEquals(DecisionType.DAYCARE_PART_TIME, decision.type)
        assertEquals(period.start, decision.startDate)
        assertEquals(period.end, decision.endDate)
    }

    @Test
    fun testPreschoolOnly() {
        val guardian = DevPerson(ssn = "070644-937X")
        val otherGuardian = DevPerson(ssn = "311299-999E")
        val child = DevPerson(ssn = "070714A9126")
        db.transaction { tx ->
            listOf(guardian, otherGuardian).forEach { tx.insert(it, DevPersonType.ADULT) }
            tx.insert(child, DevPersonType.CHILD)
        }
        MockPersonDetailsService.addPersons(guardian, otherGuardian, child)
        MockPersonDetailsService.addDependants(guardian, child)
        MockPersonDetailsService.addDependants(otherGuardian, child)
        val period = FiniteDateRange(LocalDate.of(2020, 8, 15), LocalDate.of(2021, 5, 31))
        val applicationId =
            insertInitialData(
                type = PlacementType.PRESCHOOL,
                adult = guardian,
                child = child,
                period = period,
            )
        checkDecisionDrafts(
            applicationId,
            adult = guardian,
            child = child,
            decisions =
                listOf(
                    DecisionDraft(
                        id = decisionId,
                        unitId = testDaycare.id,
                        type = DecisionType.PRESCHOOL,
                        startDate = period.start,
                        endDate = period.end,
                        planned = true,
                    ),
                    DecisionDraft(
                        id = decisionId,
                        unitId = testDaycare.id,
                        type = DecisionType.PRESCHOOL_DAYCARE,
                        startDate = period.start,
                        endDate = period.end,
                        planned = false,
                    ),
                ),
            otherGuardian = otherGuardian,
        )

        val decisions = createDecisions(applicationId)
        assertEquals(1, decisions.size)

        val decision = decisions[0]
        assertEquals(testDaycare.id, decision.unitId)
        assertEquals(DecisionType.PRESCHOOL, decision.type)
        assertEquals(period.start, decision.startDate)
        assertEquals(period.end, decision.endDate)
    }

    @Test
    fun testPreschoolAll() {
        val guardian = DevPerson(ssn = "070644-937X")
        val otherGuardian = DevPerson(ssn = "311299-999E")
        val child = DevPerson(ssn = "070714A9126")
        db.transaction { tx ->
            listOf(guardian, otherGuardian).forEach { tx.insert(it, DevPersonType.ADULT) }
            tx.insert(child, DevPersonType.CHILD)
        }
        MockPersonDetailsService.addPersons(guardian, otherGuardian, child)
        MockPersonDetailsService.addDependants(guardian, child)
        MockPersonDetailsService.addDependants(otherGuardian, child)
        val period = FiniteDateRange(LocalDate.of(2020, 8, 15), LocalDate.of(2021, 5, 31))
        val preschoolDaycarePeriod =
            FiniteDateRange(LocalDate.of(2020, 8, 1), LocalDate.of(2021, 7, 31))
        val applicationId =
            insertInitialData(
                type = PlacementType.PRESCHOOL_DAYCARE,
                adult = guardian,
                child = child,
                period = period,
                preschoolDaycarePeriod = preschoolDaycarePeriod,
                preparatoryEducation = false,
            )
        checkDecisionDrafts(
            applicationId,
            adult = guardian,
            child = child,
            decisions =
                listOf(
                    DecisionDraft(
                        id = decisionId,
                        unitId = testDaycare.id,
                        type = DecisionType.PRESCHOOL,
                        startDate = period.start,
                        endDate = period.end,
                        planned = true,
                    ),
                    DecisionDraft(
                        id = decisionId,
                        unitId = testDaycare.id,
                        type = DecisionType.PRESCHOOL_DAYCARE,
                        startDate = preschoolDaycarePeriod.start,
                        endDate = preschoolDaycarePeriod.end,
                        planned = true,
                    ),
                ),
            otherGuardian = otherGuardian,
        )

        val decisions = createDecisions(applicationId)
        assertEquals(2, decisions.size)

        val decision = decisions.find { it.type == DecisionType.PRESCHOOL }!!
        assertEquals(testDaycare.id, decision.unitId)
        assertEquals(period.start, decision.startDate)
        assertEquals(period.end, decision.endDate)

        val decision2 = decisions.find { it.type == DecisionType.PRESCHOOL_DAYCARE }!!
        assertEquals(testDaycare.id, decision2.unitId)
        assertEquals(preschoolDaycarePeriod.start, decision2.startDate)
        assertEquals(preschoolDaycarePeriod.end, decision2.endDate)
    }

    @Test
    fun testPreparatoryAll() {
        val guardian = DevPerson(ssn = "070644-937X")
        val otherGuardian = DevPerson(ssn = "311299-999E")
        val child = DevPerson(ssn = "070714A9126")
        db.transaction { tx ->
            listOf(guardian, otherGuardian).forEach { tx.insert(it, DevPersonType.ADULT) }
            tx.insert(child, DevPersonType.CHILD)
        }
        MockPersonDetailsService.addPersons(guardian, otherGuardian, child)
        MockPersonDetailsService.addDependants(guardian, child)
        MockPersonDetailsService.addDependants(otherGuardian, child)
        val period = FiniteDateRange(LocalDate.of(2020, 8, 15), LocalDate.of(2021, 5, 31))
        val preschoolDaycarePeriod =
            FiniteDateRange(LocalDate.of(2020, 8, 1), LocalDate.of(2021, 7, 31))
        val applicationId =
            insertInitialData(
                type = PlacementType.PRESCHOOL_DAYCARE,
                adult = guardian,
                child = child,
                period = period,
                preschoolDaycarePeriod = preschoolDaycarePeriod,
                preparatoryEducation = true,
            )
        checkDecisionDrafts(
            applicationId,
            adult = guardian,
            child = child,
            decisions =
                listOf(
                    DecisionDraft(
                        id = decisionId,
                        unitId = testDaycare.id,
                        type = DecisionType.PREPARATORY_EDUCATION,
                        startDate = period.start,
                        endDate = period.end,
                        planned = true,
                    ),
                    DecisionDraft(
                        id = decisionId,
                        unitId = testDaycare.id,
                        type = DecisionType.PRESCHOOL_DAYCARE,
                        startDate = preschoolDaycarePeriod.start,
                        endDate = preschoolDaycarePeriod.end,
                        planned = true,
                    ),
                ),
            otherGuardian = otherGuardian,
        )

        val decisions = createDecisions(applicationId)
        assertEquals(2, decisions.size)

        val decision = decisions.find { it.type == DecisionType.PREPARATORY_EDUCATION }!!
        assertEquals(testDaycare.id, decision.unitId)
        assertEquals(period.start, decision.startDate)
        assertEquals(period.end, decision.endDate)

        val decision2 = decisions.find { it.type == DecisionType.PRESCHOOL_DAYCARE }!!
        assertEquals(testDaycare.id, decision2.unitId)
        assertEquals(preschoolDaycarePeriod.start, decision2.startDate)
        assertEquals(preschoolDaycarePeriod.end, decision2.endDate)
    }

    @Test
    fun testEndpointSecurity() {
        val guardian = DevPerson(ssn = "070644-937X")
        val otherGuardian = DevPerson(ssn = "311299-999E")
        val child = DevPerson(ssn = "070714A9126")
        db.transaction { tx ->
            listOf(guardian, otherGuardian).forEach { tx.insert(it, DevPersonType.ADULT) }
            tx.insert(child, DevPersonType.CHILD)
        }
        MockPersonDetailsService.addPersons(guardian, otherGuardian, child)
        MockPersonDetailsService.addDependants(guardian, child)
        MockPersonDetailsService.addDependants(otherGuardian, child)
        val period = FiniteDateRange(LocalDate.of(2020, 3, 17), LocalDate.of(2023, 7, 31))
        val applicationId =
            insertInitialData(
                type = PlacementType.DAYCARE,
                adult = guardian,
                child = child,
                period = period,
            )
        val invalidRoleLists =
            listOf(setOf(UserRole.UNIT_SUPERVISOR), setOf(UserRole.FINANCE_ADMIN), setOf())
        invalidRoleLists.forEach { roles ->
            assertThrows<Forbidden> {
                applicationController.getDecisionDrafts(
                    dbInstance(),
                    AuthenticatedUser.Employee(serviceWorker.id, roles),
                    clock,
                    applicationId,
                )
            }
        }
    }

    @Test
    fun `citizen sees decision for his own application`() {
        val guardian = DevPerson(ssn = "070644-937X")
        val otherGuardian = DevPerson(ssn = "311299-999E")
        val child = DevPerson(ssn = "070714A9126")
        val (citizen, citizenWeak) =
            db.transaction { tx ->
                listOf(guardian, otherGuardian).forEach { tx.insert(it, DevPersonType.ADULT) }
                tx.insert(child, DevPersonType.CHILD)
                AuthenticatedUser.Citizen(guardian.id, CitizenAuthLevel.STRONG) to
                    AuthenticatedUser.Citizen(guardian.id, CitizenAuthLevel.WEAK)
            }
        MockPersonDetailsService.addPersons(guardian, otherGuardian, child)
        MockPersonDetailsService.addDependants(guardian, child)
        MockPersonDetailsService.addDependants(otherGuardian, child)
        val period = FiniteDateRange(LocalDate.of(2020, 3, 17), LocalDate.of(2023, 7, 31))
        val applicationId =
            insertInitialData(
                type = PlacementType.DAYCARE,
                adult = guardian,
                child = child,
                period = period,
            )
        checkDecisionDrafts(
            applicationId,
            adult = guardian,
            child = child,
            decisions =
                listOf(
                    DecisionDraft(
                        id = decisionId,
                        unitId = testDaycare.id,
                        type = DecisionType.DAYCARE,
                        startDate = period.start,
                        endDate = period.end,
                        planned = true,
                    )
                ),
            otherGuardian = otherGuardian,
        )
        val createdDecisions = createDecisions(applicationId)
        assertEquals(1, createdDecisions.size)

        val notificationCount =
            applicationControllerCitizen.getGuardianApplicationNotifications(
                dbInstance(),
                citizen,
                clock,
            )
        assertEquals(1, notificationCount)

        val notificationCountAsWeak =
            applicationControllerCitizen.getGuardianApplicationNotifications(
                dbInstance(),
                citizenWeak,
                clock,
            )
        assertEquals(1, notificationCountAsWeak)

        val citizenDecisions =
            applicationControllerCitizen.getDecisions(dbInstance(), citizen, clock)
        assertEquals(
            citizenDecisions,
            ApplicationDecisions(
                decisions =
                    listOf(
                        DecisionSummary(
                            id = createdDecisions[0].id,
                            applicationId = applicationId,
                            childId = child.id,
                            type = DecisionType.DAYCARE,
                            status = DecisionStatus.PENDING,
                            sentDate = clock.today(),
                            resolved = null,
                        )
                    ),
                permittedActions =
                    mapOf(
                        createdDecisions[0].id to
                            setOf(
                                Action.Citizen.Decision.READ,
                                Action.Citizen.Decision.DOWNLOAD_PDF,
                            )
                    ),
                decidableApplications = setOf(applicationId),
            ),
        )
    }

    @Test
    fun `other guardian in different address as guardian sees decision but can't decide`() {
        val guardian =
            DevPerson(ssn = "070644-937X", residenceCode = "1", email = "guardian@example.com")
        val otherGuardian =
            DevPerson(
                ssn = "311299-999E",
                residenceCode = "2",
                email = "other.guardian@example.com",
            )
        val child = DevPerson(ssn = "070714A9126", residenceCode = "2")
        db.transaction { tx ->
            listOf(guardian, otherGuardian).forEach { tx.insert(it, DevPersonType.ADULT) }
            tx.insert(child, DevPersonType.CHILD)
        }
        MockPersonDetailsService.addPersons(guardian, otherGuardian, child)
        MockPersonDetailsService.addDependants(guardian, child)
        MockPersonDetailsService.addDependants(otherGuardian, child)
        val period = FiniteDateRange(LocalDate.of(2020, 3, 17), LocalDate.of(2023, 7, 31))
        val applicationId =
            insertInitialData(
                type = PlacementType.DAYCARE,
                period = period,
                adult = otherGuardian,
                child = child,
            )
        checkDecisionDrafts(
            applicationId,
            adult = otherGuardian,
            child = child,
            decisions =
                listOf(
                    DecisionDraft(
                        id = decisionId,
                        unitId = testDaycare.id,
                        type = DecisionType.DAYCARE,
                        startDate = period.start,
                        endDate = period.end,
                        planned = true,
                    )
                ),
            otherGuardian = guardian,
        )
        val createdDecisions = createDecisions(applicationId)
        assertEquals(1, createdDecisions.size)

        sendPendingDecisionsEmails(clock.today().plusWeeks(1), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(1).plusDays(1), 1)
        sendPendingDecisionsEmails(clock.today().plusWeeks(2), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(2).plusDays(1), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(2).plusDays(2), 1)
        sendPendingDecisionsEmails(clock.today().plusWeeks(3), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(3).plusDays(1), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(3).plusDays(2), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(3).plusDays(3), 1)
        sendPendingDecisionsEmails(clock.today().plusWeeks(4), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(4).plusDays(1), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(4).plusDays(2), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(4).plusDays(3), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(4).plusDays(4), 1)
        sendPendingDecisionsEmails(clock.today().plusWeeks(5), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(5).plusDays(1), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(5).plusDays(2), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(5).plusDays(3), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(5).plusDays(4), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(5).plusDays(5), 0)

        val otherCitizen = AuthenticatedUser.Citizen(guardian.id, CitizenAuthLevel.STRONG)
        val otherCitizenWeak = AuthenticatedUser.Citizen(guardian.id, CitizenAuthLevel.WEAK)

        val notificationCount =
            applicationControllerCitizen.getGuardianApplicationNotifications(
                dbInstance(),
                otherCitizen,
                clock,
            )
        assertEquals(0, notificationCount)

        val notificationCountAsWeak =
            applicationControllerCitizen.getGuardianApplicationNotifications(
                dbInstance(),
                otherCitizenWeak,
                clock,
            )
        assertEquals(0, notificationCountAsWeak)

        val citizenDecisions =
            applicationControllerCitizen.getDecisions(dbInstance(), otherCitizen, clock)
        assertEquals(
            citizenDecisions,
            ApplicationDecisions(
                decisions =
                    listOf(
                        DecisionSummary(
                            id = createdDecisions[0].id,
                            applicationId = applicationId,
                            childId = child.id,
                            type = DecisionType.DAYCARE,
                            status = DecisionStatus.PENDING,
                            sentDate = clock.today(),
                            resolved = null,
                        )
                    ),
                permittedActions =
                    mapOf(
                        createdDecisions[0].id to
                            setOf(
                                Action.Citizen.Decision.READ,
                                Action.Citizen.Decision.DOWNLOAD_PDF,
                            )
                    ),
                decidableApplications = emptySet(),
            ),
        )
    }

    @Test
    fun `other guardian in different address as child sees decision but can't decide`() {
        val guardian =
            DevPerson(ssn = "070644-937X", residenceCode = "1", email = "guardian@example.com")
        val otherGuardian =
            DevPerson(
                ssn = "311299-999E",
                residenceCode = "1",
                email = "other.guardian@example.com",
            )
        val child = DevPerson(ssn = "070714A9126", residenceCode = "2")
        db.transaction { tx ->
            listOf(guardian, otherGuardian).forEach { tx.insert(it, DevPersonType.ADULT) }
            tx.insert(child, DevPersonType.CHILD)
        }
        MockPersonDetailsService.addPersons(guardian, otherGuardian, child)
        MockPersonDetailsService.addDependants(guardian, child)
        MockPersonDetailsService.addDependants(otherGuardian, child)
        val period = FiniteDateRange(LocalDate.of(2020, 3, 17), LocalDate.of(2023, 7, 31))
        val applicationId =
            insertInitialData(
                type = PlacementType.DAYCARE,
                adult = guardian,
                child = child,
                period = period,
            )
        checkDecisionDrafts(
            applicationId,
            adult = guardian,
            child = child,
            decisions =
                listOf(
                    DecisionDraft(
                        id = decisionId,
                        unitId = testDaycare.id,
                        type = DecisionType.DAYCARE,
                        startDate = period.start,
                        endDate = period.end,
                        planned = true,
                    )
                ),
            otherGuardian = otherGuardian,
        )
        val createdDecisions = createDecisions(applicationId)
        assertEquals(1, createdDecisions.size)

        sendPendingDecisionsEmails(clock.today().plusWeeks(1), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(1).plusDays(1), 1)
        sendPendingDecisionsEmails(clock.today().plusWeeks(2), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(2).plusDays(1), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(2).plusDays(2), 1)
        sendPendingDecisionsEmails(clock.today().plusWeeks(3), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(3).plusDays(1), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(3).plusDays(2), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(3).plusDays(3), 1)
        sendPendingDecisionsEmails(clock.today().plusWeeks(4), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(4).plusDays(1), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(4).plusDays(2), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(4).plusDays(3), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(4).plusDays(4), 1)
        sendPendingDecisionsEmails(clock.today().plusWeeks(5), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(5).plusDays(1), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(5).plusDays(2), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(5).plusDays(3), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(5).plusDays(4), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(5).plusDays(5), 0)

        val otherCitizen = AuthenticatedUser.Citizen(otherGuardian.id, CitizenAuthLevel.STRONG)
        val otherCitizenWeak = AuthenticatedUser.Citizen(otherCitizen.id, CitizenAuthLevel.WEAK)

        val notificationCount =
            applicationControllerCitizen.getGuardianApplicationNotifications(
                dbInstance(),
                otherCitizen,
                clock,
            )
        assertEquals(0, notificationCount)

        val notificationCountAsWeak =
            applicationControllerCitizen.getGuardianApplicationNotifications(
                dbInstance(),
                otherCitizenWeak,
                clock,
            )
        assertEquals(0, notificationCountAsWeak)

        val citizenDecisions =
            applicationControllerCitizen.getDecisions(dbInstance(), otherCitizen, clock)
        assertEquals(
            citizenDecisions,
            ApplicationDecisions(
                decisions =
                    listOf(
                        DecisionSummary(
                            id = createdDecisions[0].id,
                            applicationId = applicationId,
                            childId = child.id,
                            type = DecisionType.DAYCARE,
                            status = DecisionStatus.PENDING,
                            sentDate = clock.today(),
                            resolved = null,
                        )
                    ),
                permittedActions =
                    mapOf(
                        createdDecisions[0].id to
                            setOf(
                                Action.Citizen.Decision.READ,
                                Action.Citizen.Decision.DOWNLOAD_PDF,
                            )
                    ),
                decidableApplications = emptySet(),
            ),
        )
    }

    @Test
    fun `other guardian in the same address sees decision and can decide`() {
        val residenceCode = "same for all"
        val guardian =
            DevPerson(
                ssn = "070644-937X",
                residenceCode = residenceCode,
                email = "guardian@example.com",
            )
        val otherGuardian =
            DevPerson(
                ssn = "311299-999E",
                residenceCode = residenceCode,
                email = "other.guardian@example.com",
            )
        val child = DevPerson(ssn = "070714A9126", residenceCode = residenceCode)
        db.transaction { tx ->
            listOf(guardian, otherGuardian).forEach { tx.insert(it, DevPersonType.ADULT) }
            tx.insert(child, DevPersonType.CHILD)
        }
        MockPersonDetailsService.addPersons(guardian, otherGuardian, child)
        MockPersonDetailsService.addDependants(guardian, child)
        MockPersonDetailsService.addDependants(otherGuardian, child)

        val period = FiniteDateRange(LocalDate.of(2020, 3, 17), LocalDate.of(2023, 7, 31))
        val applicationId =
            insertInitialData(
                type = PlacementType.DAYCARE,
                period = period,
                adult = guardian,
                child = child,
            )
        checkDecisionDrafts(
            applicationId,
            adult = guardian,
            child = child,
            decisions =
                listOf(
                    DecisionDraft(
                        id = decisionId,
                        unitId = testDaycare.id,
                        type = DecisionType.DAYCARE,
                        startDate = period.start,
                        endDate = period.end,
                        planned = true,
                    )
                ),
            otherGuardian = otherGuardian,
        )
        val createdDecisions = createDecisions(applicationId)
        assertEquals(1, createdDecisions.size)

        sendPendingDecisionsEmails(clock.today().plusWeeks(1), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(1).plusDays(1), 2)
        sendPendingDecisionsEmails(clock.today().plusWeeks(2), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(2).plusDays(1), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(2).plusDays(2), 2)
        sendPendingDecisionsEmails(clock.today().plusWeeks(3), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(3).plusDays(1), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(3).plusDays(2), 0)
        sendPendingDecisionsEmails(clock.today().plusWeeks(3).plusDays(3), 0)

        val otherCitizen = AuthenticatedUser.Citizen(otherGuardian.id, CitizenAuthLevel.STRONG)
        val otherCitizenWeak = AuthenticatedUser.Citizen(otherGuardian.id, CitizenAuthLevel.WEAK)

        val notificationCount =
            applicationControllerCitizen.getGuardianApplicationNotifications(
                dbInstance(),
                otherCitizen,
                clock,
            )
        assertEquals(1, notificationCount)

        val notificationCountAsWeak =
            applicationControllerCitizen.getGuardianApplicationNotifications(
                dbInstance(),
                otherCitizenWeak,
                clock,
            )
        assertEquals(1, notificationCountAsWeak)

        val citizenDecisions =
            applicationControllerCitizen.getDecisions(dbInstance(), otherCitizen, clock)
        assertEquals(
            citizenDecisions,
            ApplicationDecisions(
                decisions =
                    listOf(
                        DecisionSummary(
                            id = createdDecisions[0].id,
                            applicationId = applicationId,
                            childId = child.id,
                            type = DecisionType.DAYCARE,
                            status = DecisionStatus.PENDING,
                            sentDate = clock.today(),
                            resolved = null,
                        )
                    ),
                permittedActions =
                    mapOf(
                        createdDecisions[0].id to
                            setOf(
                                Action.Citizen.Decision.READ,
                                Action.Citizen.Decision.DOWNLOAD_PDF,
                            )
                    ),
                decidableApplications = setOf(applicationId),
            ),
        )
    }

    @Test
    fun `citizen sees decision notification for hidden application`() {
        val guardian = DevPerson(ssn = "070644-937X")
        val otherGuardian = DevPerson(ssn = "311299-999E")
        val child = DevPerson(ssn = "070714A9126")
        val citizen =
            db.transaction { tx ->
                listOf(guardian, otherGuardian).forEach { tx.insert(it, DevPersonType.ADULT) }
                tx.insert(child, DevPersonType.CHILD)
                AuthenticatedUser.Citizen(guardian.id, CitizenAuthLevel.STRONG)
            }
        MockPersonDetailsService.addPersons(guardian, otherGuardian, child)
        MockPersonDetailsService.addDependants(guardian, child)
        MockPersonDetailsService.addDependants(otherGuardian, child)
        val period = FiniteDateRange(LocalDate.of(2020, 3, 17), LocalDate.of(2023, 7, 31))
        val applicationId =
            insertInitialData(
                type = PlacementType.DAYCARE,
                adult = guardian,
                child = child,
                period = period,
                hideFromGuardian = true,
            )
        checkDecisionDrafts(
            applicationId,
            adult = guardian,
            child = child,
            decisions =
                listOf(
                    DecisionDraft(
                        id = decisionId,
                        unitId = testDaycare.id,
                        type = DecisionType.DAYCARE,
                        startDate = period.start,
                        endDate = period.end,
                        planned = true,
                    )
                ),
            otherGuardian = otherGuardian,
        )
        val createdDecisions = createDecisions(applicationId)
        assertEquals(1, createdDecisions.size)

        val notificationCount =
            applicationControllerCitizen.getGuardianApplicationNotifications(
                dbInstance(),
                citizen,
                clock,
            )
        assertEquals(1, notificationCount)

        val citizenDecisions =
            applicationControllerCitizen.getDecisions(dbInstance(), citizen, clock)
        assertEquals(
            citizenDecisions,
            ApplicationDecisions(
                decisions =
                    listOf(
                        DecisionSummary(
                            id = createdDecisions[0].id,
                            applicationId = applicationId,
                            childId = child.id,
                            type = DecisionType.DAYCARE,
                            status = DecisionStatus.PENDING,
                            sentDate = clock.today(),
                            resolved = null,
                        )
                    ),
                permittedActions =
                    mapOf(
                        createdDecisions[0].id to
                            setOf(
                                Action.Citizen.Decision.READ,
                                Action.Citizen.Decision.DOWNLOAD_PDF,
                            )
                    ),
                decidableApplications = setOf(applicationId),
            ),
        )
    }

    @Test
    fun `citizen doesn't see decisions if guardianship has been blocked`() {
        val guardian = DevPerson(ssn = "070644-937X")
        val otherGuardian = DevPerson(ssn = "311299-999E")
        val child = DevPerson(ssn = "070714A9126")
        val citizen =
            db.transaction { tx ->
                listOf(guardian, otherGuardian).forEach { tx.insert(it, DevPersonType.ADULT) }
                tx.insert(child, DevPersonType.CHILD)
                AuthenticatedUser.Citizen(guardian.id, CitizenAuthLevel.STRONG)
            }
        MockPersonDetailsService.addPersons(guardian, otherGuardian, child)
        MockPersonDetailsService.addDependants(guardian, child)
        MockPersonDetailsService.addDependants(otherGuardian, child)
        val period = FiniteDateRange(LocalDate.of(2020, 3, 17), LocalDate.of(2023, 7, 31))
        val applicationId =
            insertInitialData(
                type = PlacementType.DAYCARE,
                adult = guardian,
                child = child,
                period = period,
            )
        checkDecisionDrafts(
            applicationId,
            adult = guardian,
            child = child,
            decisions =
                listOf(
                    DecisionDraft(
                        id = decisionId,
                        unitId = testDaycare.id,
                        type = DecisionType.DAYCARE,
                        startDate = period.start,
                        endDate = period.end,
                        planned = true,
                    )
                ),
            otherGuardian = otherGuardian,
        )
        val createdDecisions = createDecisions(applicationId)
        assertEquals(1, createdDecisions.size)

        db.transaction { tx -> tx.blockGuardian(child.id, guardian.id) }

        val notificationCount =
            applicationControllerCitizen.getGuardianApplicationNotifications(
                dbInstance(),
                citizen,
                clock,
            )
        assertEquals(0, notificationCount)

        val citizenDecisions =
            applicationControllerCitizen.getDecisions(dbInstance(), citizen, clock)
        assertEquals(
            ApplicationDecisions(
                decisions = emptyList(),
                permittedActions = emptyMap(),
                decidableApplications = emptySet(),
            ),
            citizenDecisions,
        )
    }

    private fun checkDecisionDrafts(
        applicationId: ApplicationId,
        unit: Daycare = testDaycare,
        adult: DevPerson,
        child: DevPerson,
        otherGuardian: DevPerson? = null,
        decisions: List<DecisionDraft>,
    ) {
        val result =
            applicationController.getDecisionDrafts(
                dbInstance(),
                serviceWorker,
                clock,
                applicationId,
            )
        assertEquals(
            DecisionDraftGroup(
                decisions = decisions.sortedBy { it.type },
                placementUnitName = "Test Daycare",
                unit =
                    DecisionUnit(
                        id = unit.id,
                        name = unit.name,
                        daycareDecisionName = "Test Daycare / daycare",
                        preschoolDecisionName = "Test Daycare / preschool",
                        manager = "Unit Manager",
                        streetAddress = "Test address",
                        postalCode = "Test postal code",
                        postOffice = "Test post office",
                        phone = "Test phone",
                        decisionHandler = "Test decision handler",
                        decisionHandlerAddress = "Test decision handler address",
                        providerType = ProviderType.MUNICIPAL,
                    ),
                guardian =
                    GuardianInfo(
                        null,
                        adult.ssn,
                        adult.firstName,
                        adult.lastName,
                        isVtjGuardian = true,
                    ),
                otherGuardian =
                    if (otherGuardian != null) {
                        GuardianInfo(
                            otherGuardian.id,
                            otherGuardian.ssn,
                            otherGuardian.firstName,
                            otherGuardian.lastName,
                            isVtjGuardian = true,
                        )
                    } else {
                        null
                    },
                child = ChildInfo(child.ssn, child.firstName, child.lastName),
            ),
            result.copy(
                decisions = result.decisions.map { it.copy(id = decisionId) }.sortedBy { it.type }
            ),
        )
    }

    private fun createDecisions(applicationId: ApplicationId): List<DecisionTableRow> {
        applicationController.simpleApplicationAction(
            dbInstance(),
            serviceWorker,
            clock,
            applicationId,
            SimpleApplicationAction.SEND_DECISIONS_WITHOUT_PROPOSAL,
        )
        asyncJobRunner.runPendingJobsSync(clock)

        val rows = db.read { r -> r.getDecisionRowsByApplication(applicationId).toList() }
        rows.forEach { row ->
            assertEquals(serviceWorker.evakaUserId, row.createdBy)
            assertEquals(DecisionStatus.PENDING, row.status)
            assertNull(row.requestedStartDate)
            assertNull(row.resolved)
            assertNull(row.resolvedBy)
            assertNotNull(row.sentDate)
            assertNotNull(row.documentKey)
        }
        db.read { r ->
            assertEquals(
                ApplicationStatus.WAITING_CONFIRMATION,
                r.getApplicationStatus(applicationId),
            )
        }
        return rows
    }

    private fun insertInitialData(
        type: PlacementType,
        unit: Daycare = testDaycare,
        adult: DevPerson,
        child: DevPerson,
        period: FiniteDateRange,
        preschoolDaycarePeriod: FiniteDateRange? = null,
        preparatoryEducation: Boolean = false,
        hideFromGuardian: Boolean = false,
    ): ApplicationId =
        db.transaction { tx ->
            // make sure guardians are up-to-date
            personService.getGuardians(tx, AuthenticatedUser.SystemInternalUser, child.id)

            val preschoolDaycare = type == PlacementType.PRESCHOOL_DAYCARE
            val applicationId =
                tx.insertTestApplication(
                    status = ApplicationStatus.WAITING_PLACEMENT,
                    guardianId = adult.id,
                    childId = child.id,
                    type = type.toApplicationType(),
                    hideFromGuardian = hideFromGuardian,
                    document =
                        DaycareFormV0(
                            type = type.toApplicationType(),
                            partTime = type == PlacementType.DAYCARE_PART_TIME,
                            connectedDaycare = preschoolDaycare,
                            serviceStart = "08:00".takeIf { preschoolDaycare },
                            serviceEnd = "16:00".takeIf { preschoolDaycare },
                            careDetails = CareDetails(preparatory = preparatoryEducation),
                            child = child.toDaycareFormChild(),
                            guardian = adult.toDaycareFormAdult(),
                            apply = Apply(preferredUnits = listOf(unit.id)),
                            preferredStartDate = period.start,
                        ),
                )

            applicationStateService.setVerified(tx, serviceWorker, clock, applicationId, false)
            applicationStateService.createPlacementPlan(
                tx,
                serviceWorker,
                clock,
                applicationId,
                DaycarePlacementPlan(
                    unitId = unit.id,
                    period = period,
                    preschoolDaycarePeriod = preschoolDaycarePeriod,
                ),
            )

            applicationId
        }

    private fun sendPendingDecisionsEmails(date: LocalDate, expectedCount: Int) {
        val clock = MockEvakaClock(HelsinkiDateTime.of(date, LocalTime.MIN))
        scheduledJobs.sendPendingDecisionReminderEmails(db, clock)
        assertEquals(expectedCount, asyncJobRunner.runPendingJobsSync(clock))
    }
}
