// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.decision

import evaka.core.FullApplicationTest
import evaka.core.application.AcceptDecisionRequest
import evaka.core.application.ApplicationControllerCitizen
import evaka.core.application.ApplicationControllerV2
import evaka.core.application.ApplicationDecisions
import evaka.core.application.ApplicationStateService
import evaka.core.application.ApplicationStatus
import evaka.core.application.ChildInfo
import evaka.core.application.DaycarePlacementPlan
import evaka.core.application.DecisionDraftGroup
import evaka.core.application.DecisionSummary
import evaka.core.application.GuardianInfo
import evaka.core.application.SimpleApplicationAction
import evaka.core.application.persistence.daycare.Apply
import evaka.core.application.persistence.daycare.CareDetails
import evaka.core.application.persistence.daycare.DaycareFormV0
import evaka.core.daycare.Daycare
import evaka.core.daycare.DaycareDecisionCustomization
import evaka.core.daycare.VisitingAddress
import evaka.core.daycare.domain.ProviderType
import evaka.core.daycare.getDaycare
import evaka.core.pis.service.PersonService
import evaka.core.pis.service.blockGuardian
import evaka.core.placement.PlacementType
import evaka.core.shared.ApplicationId
import evaka.core.shared.DaycareId
import evaka.core.shared.DecisionId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.CitizenAuthLevel
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertTestApplication
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.Forbidden
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.security.Action
import evaka.core.test.DecisionTableRow
import evaka.core.test.getApplicationStatus
import evaka.core.test.getDecisionRowsByApplication
import evaka.core.toApplicationType
import evaka.core.toDaycareFormAdult
import evaka.core.toDaycareFormChild
import evaka.core.vtjclient.service.persondetails.MockPersonDetailsService
import java.time.LocalDate
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
    private val employee =
        DevEmployee(
            firstName = "Decision",
            lastName = "Maker",
            roles = setOf(UserRole.SERVICE_WORKER),
        )
    private lateinit var testDaycare: Daycare
    private val testArea2 = DevCareArea(name = "Test Care Area 2", shortName = "test_area_2")
    private val testDaycare2 = DevDaycare(areaId = testArea2.id, name = "Test Daycare 2")

    @Autowired private lateinit var applicationController: ApplicationControllerV2
    @Autowired private lateinit var applicationControllerCitizen: ApplicationControllerCitizen
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    @Autowired private lateinit var applicationStateService: ApplicationStateService
    @Autowired private lateinit var personService: PersonService

    private val decisionId = DecisionId(UUID.randomUUID())

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insert(employee) }
        testDaycare = db.transaction { tx ->
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
        db.transaction { tx ->
            tx.insert(testArea2)
            tx.insert(testDaycare2)
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
                    AuthenticatedUser.Employee(employee.id, roles),
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
    fun `citizen can fetch decision details`() {
        val guardian = DevPerson(ssn = "070644-937X")
        val child = DevPerson(ssn = "070714A9126")
        val citizen = db.transaction { tx ->
            tx.insert(guardian, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            AuthenticatedUser.Citizen(guardian.id, CitizenAuthLevel.STRONG)
        }
        MockPersonDetailsService.addPersons(guardian, child)
        MockPersonDetailsService.addDependants(guardian, child)
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
            otherGuardian = null,
        )
        val createdDecisions = createDecisions(applicationId)
        assertEquals(1, createdDecisions.size)

        val details =
            applicationControllerCitizen.getDecisionDetails(
                dbInstance(),
                citizen,
                clock,
                createdDecisions[0].id,
            )
        assertEquals("Test Daycare / daycare", details.unitName)
        assertEquals(period.start, details.startDate)
        assertEquals(period.end, details.endDate)
        assertEquals(clock.today(), details.sentDate)
        assertNull(details.resolved)
    }

    @Test
    fun `citizen cannot fetch decision details without access`() {
        val guardian = DevPerson(ssn = "070644-937X")
        val otherPerson = DevPerson(ssn = "311299-999E")
        val child = DevPerson(ssn = "070714A9126")
        val otherCitizen = db.transaction { tx ->
            tx.insert(guardian, DevPersonType.ADULT)
            tx.insert(otherPerson, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            AuthenticatedUser.Citizen(otherPerson.id, CitizenAuthLevel.STRONG)
        }
        MockPersonDetailsService.addPersons(guardian, otherPerson, child)
        MockPersonDetailsService.addDependants(guardian, child)
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
            otherGuardian = null,
        )
        val createdDecisions = createDecisions(applicationId)
        assertEquals(1, createdDecisions.size)

        assertThrows<Forbidden> {
            applicationControllerCitizen.getDecisionDetails(
                dbInstance(),
                otherCitizen,
                clock,
                createdDecisions[0].id,
            )
        }
    }

    @Test
    fun `other guardian in different address as guardian sees decision but can't decide`() {
        val guardian = DevPerson(ssn = "070644-937X", residenceCode = "1")
        val otherGuardian = DevPerson(ssn = "311299-999E", residenceCode = "2")
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
        val guardian = DevPerson(ssn = "070644-937X", residenceCode = "1")
        val otherGuardian = DevPerson(ssn = "311299-999E", residenceCode = "1")
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
        val guardian = DevPerson(ssn = "070644-937X", residenceCode = residenceCode)
        val otherGuardian = DevPerson(ssn = "311299-999E", residenceCode = residenceCode)
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
        val citizen = db.transaction { tx ->
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
        val citizen = db.transaction { tx ->
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

    @Test
    fun testPreschoolDaycareAdditionalSameUnit() {
        val guardian = DevPerson(ssn = "070644-937X")
        val child = DevPerson(ssn = "070714A9126")
        db.transaction { tx ->
            tx.insert(guardian, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
        }
        MockPersonDetailsService.addPersons(guardian, child)
        MockPersonDetailsService.addDependants(guardian, child)

        val period = FiniteDateRange(LocalDate.of(2020, 8, 13), LocalDate.of(2021, 5, 31))

        // First: create and accept a PRESCHOOL decision at testDaycare
        val firstAppId =
            insertInitialData(
                type = PlacementType.PRESCHOOL,
                adult = guardian,
                child = child,
                period = period,
            )
        val firstDecisions = createDecisions(firstAppId)
        acceptDecisions(firstAppId, firstDecisions, guardian)

        // Second: create a PRESCHOOL_DAYCARE application at the SAME unit
        val secondAppId =
            insertInitialData(
                type = PlacementType.PRESCHOOL_DAYCARE,
                adult = guardian,
                child = child,
                period = period,
                preschoolDaycarePeriod = period,
            )
        val drafts = getDecisionDrafts(secondAppId)
        val plannedByType = drafts.associate { it.type to it.planned }
        assertEquals(false, plannedByType[DecisionType.PRESCHOOL])
        assertEquals(true, plannedByType[DecisionType.PRESCHOOL_DAYCARE])
    }

    @Test
    fun testPreschoolDaycareAdditionalDifferentUnit() {
        val guardian = DevPerson(ssn = "070644-937X")
        val child = DevPerson(ssn = "070714A9126")
        db.transaction { tx ->
            tx.insert(guardian, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
        }
        MockPersonDetailsService.addPersons(guardian, child)
        MockPersonDetailsService.addDependants(guardian, child)

        val period = FiniteDateRange(LocalDate.of(2020, 8, 13), LocalDate.of(2021, 5, 31))

        // First: create and accept a PRESCHOOL decision at testDaycare
        val firstAppId =
            insertInitialData(
                type = PlacementType.PRESCHOOL,
                adult = guardian,
                child = child,
                period = period,
            )
        val firstDecisions = createDecisions(firstAppId)
        acceptDecisions(firstAppId, firstDecisions, guardian)

        // Second: create a PRESCHOOL_DAYCARE application at a DIFFERENT unit
        val secondAppId =
            insertInitialData(
                type = PlacementType.PRESCHOOL_DAYCARE,
                unitId = testDaycare2.id,
                adult = guardian,
                child = child,
                period = period,
                preschoolDaycarePeriod = period,
            )
        val drafts = getDecisionDrafts(secondAppId)
        val plannedByType = drafts.associate { it.type to it.planned }
        assertEquals(true, plannedByType[DecisionType.PRESCHOOL])
        assertEquals(true, plannedByType[DecisionType.PRESCHOOL_DAYCARE])
    }

    private fun getDecisionDrafts(applicationId: ApplicationId): List<DecisionDraft> =
        applicationController
            .getDecisionDrafts(dbInstance(), employee.user, clock, applicationId)
            .decisions

    private fun acceptDecisions(
        applicationId: ApplicationId,
        decisions: List<DecisionTableRow>,
        guardian: DevPerson,
    ) {
        decisions.forEach { decision ->
            applicationControllerCitizen.acceptDecision(
                dbInstance(),
                guardian.user(CitizenAuthLevel.STRONG),
                clock,
                applicationId,
                AcceptDecisionRequest(decision.id, decision.startDate),
            )
        }
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
                employee.user,
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
            employee.user,
            clock,
            applicationId,
            SimpleApplicationAction.SEND_DECISIONS_WITHOUT_PROPOSAL,
        )
        asyncJobRunner.runPendingJobsSync(clock)

        val rows = db.read { r -> r.getDecisionRowsByApplication(applicationId).toList() }
        rows.forEach { row ->
            assertEquals(employee.evakaUserId, row.createdBy)
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
        unitId: DaycareId = testDaycare.id,
        adult: DevPerson,
        child: DevPerson,
        period: FiniteDateRange,
        preschoolDaycarePeriod: FiniteDateRange? = null,
        preparatoryEducation: Boolean = false,
        hideFromGuardian: Boolean = false,
    ): ApplicationId = db.transaction { tx ->
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
                        apply = Apply(preferredUnits = listOf(unitId)),
                        preferredStartDate = period.start,
                    ),
            )

        applicationStateService.setVerified(tx, employee.user, clock, applicationId, false)
        applicationStateService.createPlacementPlan(
            tx,
            employee.user,
            clock,
            applicationId,
            DaycarePlacementPlan(
                unitId = unitId,
                period = period,
                preschoolDaycarePeriod = preschoolDaycarePeriod,
            ),
        )

        applicationId
    }
}
