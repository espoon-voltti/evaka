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
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.CareDetails
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.insertGeneralTestFixtures
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
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.test.DecisionTableRow
import fi.espoo.evaka.test.getApplicationStatus
import fi.espoo.evaka.test.getDecisionRowsByApplication
import fi.espoo.evaka.testAdult_5
import fi.espoo.evaka.testAdult_6
import fi.espoo.evaka.testChild_6
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.toApplicationType
import fi.espoo.evaka.toDaycareFormAdult
import fi.espoo.evaka.toDaycareFormChild
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import fi.espoo.evaka.vtjclient.service.persondetails.legacyMockVtjDataset
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
    private val serviceWorker =
        AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))
    private val citizen = AuthenticatedUser.Citizen(testAdult_5.id, CitizenAuthLevel.STRONG)

    @Autowired private lateinit var applicationController: ApplicationControllerV2

    @Autowired private lateinit var applicationControllerCitizen: ApplicationControllerCitizen

    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    @Autowired private lateinit var applicationStateService: ApplicationStateService

    @Autowired private lateinit var personService: PersonService

    private val decisionId = DecisionId(UUID.randomUUID())

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            MockPersonDetailsService.add(legacyMockVtjDataset())
            @Suppress("DEPRECATION")
            tx
                .createUpdate(
                    // language=SQL
                    """
UPDATE daycare SET
  name = 'Test Daycare',
  decision_daycare_name = 'Test Daycare / daycare',
  decision_preschool_name = 'Test Daycare / preschool',
  street_address = 'Test address',
  postal_code = 'Test postal code',
  post_office = 'Test post office',
  phone = 'Test phone',
  decision_handler = 'Test decision handler',
  decision_handler_address = 'Test decision handler address'
WHERE id = :unitId
"""
                ).bind("unitId", testDaycare.id)
                .execute()
        }
    }

    @Test
    fun testDaycareFullTime() {
        val period = FiniteDateRange(LocalDate.of(2020, 3, 17), LocalDate.of(2023, 7, 31))
        val applicationId = insertInitialData(type = PlacementType.DAYCARE, period = period)
        checkDecisionDrafts(
            applicationId,
            decisions =
                listOf(
                    DecisionDraft(
                        id = decisionId,
                        unitId = testDaycare.id,
                        type = DecisionType.DAYCARE,
                        startDate = period.start,
                        endDate = period.end,
                        planned = true
                    )
                ),
            otherGuardian = testAdult_6
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
        val period = FiniteDateRange(LocalDate.of(2020, 3, 18), LocalDate.of(2023, 7, 29))
        val applicationId =
            insertInitialData(type = PlacementType.DAYCARE_PART_TIME, period = period)
        checkDecisionDrafts(
            applicationId,
            decisions =
                listOf(
                    DecisionDraft(
                        id = decisionId,
                        unitId = testDaycare.id,
                        type = DecisionType.DAYCARE_PART_TIME,
                        startDate = period.start,
                        endDate = period.end,
                        planned = true
                    )
                ),
            otherGuardian = testAdult_6
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
        val period = FiniteDateRange(LocalDate.of(2020, 8, 15), LocalDate.of(2021, 5, 31))
        val applicationId = insertInitialData(type = PlacementType.PRESCHOOL, period = period)
        checkDecisionDrafts(
            applicationId,
            decisions =
                listOf(
                    DecisionDraft(
                        id = decisionId,
                        unitId = testDaycare.id,
                        type = DecisionType.PRESCHOOL,
                        startDate = period.start,
                        endDate = period.end,
                        planned = true
                    ),
                    DecisionDraft(
                        id = decisionId,
                        unitId = testDaycare.id,
                        type = DecisionType.PRESCHOOL_DAYCARE,
                        startDate = period.start,
                        endDate = period.end,
                        planned = false
                    )
                ),
            otherGuardian = testAdult_6
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
        val period = FiniteDateRange(LocalDate.of(2020, 8, 15), LocalDate.of(2021, 5, 31))
        val preschoolDaycarePeriod =
            FiniteDateRange(LocalDate.of(2020, 8, 1), LocalDate.of(2021, 7, 31))
        val applicationId =
            insertInitialData(
                type = PlacementType.PRESCHOOL_DAYCARE,
                period = period,
                preschoolDaycarePeriod = preschoolDaycarePeriod,
                preparatoryEducation = false
            )
        checkDecisionDrafts(
            applicationId,
            decisions =
                listOf(
                    DecisionDraft(
                        id = decisionId,
                        unitId = testDaycare.id,
                        type = DecisionType.PRESCHOOL,
                        startDate = period.start,
                        endDate = period.end,
                        planned = true
                    ),
                    DecisionDraft(
                        id = decisionId,
                        unitId = testDaycare.id,
                        type = DecisionType.PRESCHOOL_DAYCARE,
                        startDate = preschoolDaycarePeriod.start,
                        endDate = preschoolDaycarePeriod.end,
                        planned = true
                    )
                ),
            otherGuardian = testAdult_6
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
        val period = FiniteDateRange(LocalDate.of(2020, 8, 15), LocalDate.of(2021, 5, 31))
        val preschoolDaycarePeriod =
            FiniteDateRange(LocalDate.of(2020, 8, 1), LocalDate.of(2021, 7, 31))
        val applicationId =
            insertInitialData(
                type = PlacementType.PRESCHOOL_DAYCARE,
                period = period,
                preschoolDaycarePeriod = preschoolDaycarePeriod,
                preparatoryEducation = true
            )
        checkDecisionDrafts(
            applicationId,
            decisions =
                listOf(
                    DecisionDraft(
                        id = decisionId,
                        unitId = testDaycare.id,
                        type = DecisionType.PREPARATORY_EDUCATION,
                        startDate = period.start,
                        endDate = period.end,
                        planned = true
                    ),
                    DecisionDraft(
                        id = decisionId,
                        unitId = testDaycare.id,
                        type = DecisionType.PRESCHOOL_DAYCARE,
                        startDate = preschoolDaycarePeriod.start,
                        endDate = preschoolDaycarePeriod.end,
                        planned = true
                    )
                ),
            otherGuardian = testAdult_6
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
        val period = FiniteDateRange(LocalDate.of(2020, 3, 17), LocalDate.of(2023, 7, 31))
        val applicationId = insertInitialData(type = PlacementType.DAYCARE, period = period)
        val invalidRoleLists =
            listOf(
                setOf(UserRole.UNIT_SUPERVISOR),
                setOf(UserRole.FINANCE_ADMIN),
                setOf(UserRole.END_USER),
                setOf()
            )
        invalidRoleLists.forEach { roles ->
            assertThrows<Forbidden> {
                applicationController.getDecisionDrafts(
                    dbInstance(),
                    AuthenticatedUser.Employee(testDecisionMaker_1.id, roles),
                    RealEvakaClock(),
                    applicationId
                )
            }
        }
    }

    @Test
    fun `citizen sees decision for his own application`() {
        val period = FiniteDateRange(LocalDate.of(2020, 3, 17), LocalDate.of(2023, 7, 31))
        val applicationId = insertInitialData(type = PlacementType.DAYCARE, period = period)
        checkDecisionDrafts(
            applicationId,
            decisions =
                listOf(
                    DecisionDraft(
                        id = decisionId,
                        unitId = testDaycare.id,
                        type = DecisionType.DAYCARE,
                        startDate = period.start,
                        endDate = period.end,
                        planned = true
                    )
                ),
            otherGuardian = testAdult_6
        )
        val createdDecisions = createDecisions(applicationId)
        assertEquals(1, createdDecisions.size)

        val notificationCount =
            applicationControllerCitizen.getGuardianApplicationNotifications(
                dbInstance(),
                citizen,
                RealEvakaClock()
            )
        assertEquals(1, notificationCount)

        val citizenDecisions =
            applicationControllerCitizen.getDecisions(dbInstance(), citizen, RealEvakaClock())
        assertEquals(
            citizenDecisions,
            ApplicationDecisions(
                decisions =
                    listOf(
                        DecisionSummary(
                            id = createdDecisions[0].id,
                            applicationId = applicationId,
                            childId = testChild_6.id,
                            type = DecisionType.DAYCARE,
                            status = DecisionStatus.PENDING,
                            sentDate = LocalDate.now(),
                            resolved = null
                        )
                    ),
                permittedActions =
                    mapOf(
                        createdDecisions[0].id to
                            setOf(
                                Action.Citizen.Decision.READ,
                                Action.Citizen.Decision.DOWNLOAD_PDF
                            )
                    ),
                decidableApplications = setOf(applicationId)
            )
        )
    }

    @Test
    fun `citizen doesn't see decisions if guardianship has been blocked`() {
        val period = FiniteDateRange(LocalDate.of(2020, 3, 17), LocalDate.of(2023, 7, 31))
        val applicationId = insertInitialData(type = PlacementType.DAYCARE, period = period)
        checkDecisionDrafts(
            applicationId,
            decisions =
                listOf(
                    DecisionDraft(
                        id = decisionId,
                        unitId = testDaycare.id,
                        type = DecisionType.DAYCARE,
                        startDate = period.start,
                        endDate = period.end,
                        planned = true
                    )
                ),
            otherGuardian = testAdult_6
        )
        val createdDecisions = createDecisions(applicationId)
        assertEquals(1, createdDecisions.size)

        db.transaction { tx -> tx.blockGuardian(testChild_6.id, testAdult_5.id) }

        val notificationCount =
            applicationControllerCitizen.getGuardianApplicationNotifications(
                dbInstance(),
                citizen,
                RealEvakaClock()
            )
        assertEquals(0, notificationCount)

        val citizenDecisions =
            applicationControllerCitizen.getDecisions(dbInstance(), citizen, RealEvakaClock())
        assertEquals(
            ApplicationDecisions(
                decisions = emptyList(),
                permittedActions = emptyMap(),
                decidableApplications = emptySet()
            ),
            citizenDecisions
        )
    }

    private fun checkDecisionDrafts(
        applicationId: ApplicationId,
        unit: DevDaycare = testDaycare,
        adult: DevPerson = testAdult_5,
        child: DevPerson = testChild_6,
        otherGuardian: DevPerson? = null,
        decisions: List<DecisionDraft>
    ) {
        val result =
            applicationController.getDecisionDrafts(
                dbInstance(),
                serviceWorker,
                RealEvakaClock(),
                applicationId
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
                        providerType = ProviderType.MUNICIPAL
                    ),
                guardian =
                    GuardianInfo(
                        null,
                        adult.ssn,
                        adult.firstName,
                        adult.lastName,
                        isVtjGuardian = true
                    ),
                otherGuardian =
                    if (otherGuardian != null) {
                        GuardianInfo(
                            otherGuardian.id,
                            otherGuardian.ssn,
                            otherGuardian.firstName,
                            otherGuardian.lastName,
                            isVtjGuardian = true
                        )
                    } else {
                        null
                    },
                child = ChildInfo(child.ssn, child.firstName, child.lastName)
            ),
            result.copy(
                decisions = result.decisions.map { it.copy(id = decisionId) }.sortedBy { it.type }
            )
        )
    }

    private fun createDecisions(applicationId: ApplicationId): List<DecisionTableRow> {
        applicationController.simpleApplicationAction(
            dbInstance(),
            serviceWorker,
            RealEvakaClock(),
            applicationId,
            "send-decisions-without-proposal"
        )
        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

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
                r.getApplicationStatus(applicationId)
            )
        }
        return rows
    }

    private fun insertInitialData(
        type: PlacementType,
        unit: DevDaycare = testDaycare,
        adult: DevPerson = testAdult_5,
        child: DevPerson = testChild_6,
        period: FiniteDateRange,
        preschoolDaycarePeriod: FiniteDateRange? = null,
        preparatoryEducation: Boolean = false
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
                            preferredStartDate = period.start
                        )
                )

            applicationStateService.createPlacementPlan(
                tx,
                serviceWorker,
                RealEvakaClock(),
                applicationId,
                DaycarePlacementPlan(
                    unitId = unit.id,
                    period = period,
                    preschoolDaycarePeriod = preschoolDaycarePeriod
                )
            )

            applicationId
        }
}
