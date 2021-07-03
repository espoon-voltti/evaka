// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.decision

import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.ApplicationStateService
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ChildInfo
import fi.espoo.evaka.application.DaycarePlacementPlan
import fi.espoo.evaka.application.DecisionDraftJSON
import fi.espoo.evaka.application.GuardianInfo
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.CareDetails
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.invoicing.domain.UnitData
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.shared.domain.FiniteDateRange
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class DecisionCreationIntegrationTest : FullApplicationTest() {
    private val serviceWorker = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))

    @Autowired
    private lateinit var asyncJobRunner: AsyncJobRunner

    @Autowired
    private lateinit var applicationStateService: ApplicationStateService

    private val decisionId = UUID.randomUUID()

    @BeforeEach
    private fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            tx.insertGeneralTestFixtures()
            tx.createUpdate(
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
            ).bind("unitId", testDaycare.id).execute()
        }
    }

    @Test
    fun testDaycareFullTime() {
        val period = FiniteDateRange(
            LocalDate.of(2020, 3, 17),
            LocalDate.of(2023, 7, 31)
        )
        val applicationId = insertInitialData(
            type = PlacementType.DAYCARE,
            period = period
        )
        checkDecisionDrafts(
            applicationId,
            decisions = listOf(
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
        val period = FiniteDateRange(
            LocalDate.of(2020, 3, 18),
            LocalDate.of(2023, 7, 29)
        )
        val applicationId = insertInitialData(
            type = PlacementType.DAYCARE_PART_TIME,
            period = period
        )
        checkDecisionDrafts(
            applicationId,
            decisions = listOf(
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
        val period = FiniteDateRange(
            LocalDate.of(2020, 8, 15),
            LocalDate.of(2021, 5, 31)
        )
        val applicationId = insertInitialData(
            type = PlacementType.PRESCHOOL,
            period = period
        )
        checkDecisionDrafts(
            applicationId,
            decisions = listOf(
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
        val period = FiniteDateRange(
            LocalDate.of(2020, 8, 15),
            LocalDate.of(2021, 5, 31)
        )
        val preschoolDaycarePeriod = FiniteDateRange(
            LocalDate.of(2020, 8, 1),
            LocalDate.of(2021, 7, 31)
        )
        val applicationId = insertInitialData(
            type = PlacementType.PRESCHOOL_DAYCARE,
            period = period,
            preschoolDaycarePeriod = preschoolDaycarePeriod,
            preparatoryEducation = false
        )
        checkDecisionDrafts(
            applicationId,
            decisions = listOf(
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
        val period = FiniteDateRange(
            LocalDate.of(2020, 8, 15),
            LocalDate.of(2021, 5, 31)
        )
        val preschoolDaycarePeriod = FiniteDateRange(
            LocalDate.of(2020, 8, 1),
            LocalDate.of(2021, 7, 31)
        )
        val applicationId = insertInitialData(
            type = PlacementType.PRESCHOOL_DAYCARE,
            period = period,
            preschoolDaycarePeriod = preschoolDaycarePeriod,
            preparatoryEducation = true
        )
        checkDecisionDrafts(
            applicationId,
            decisions = listOf(
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

    private fun checkDecisionDrafts(
        applicationId: ApplicationId,
        unit: UnitData.Detailed = testDaycare,
        adult: PersonData.Detailed = testAdult_5,
        child: PersonData.Detailed = testChild_6,
        otherGuardian: PersonData.Detailed? = null,
        decisions: List<DecisionDraft>
    ) {
        val (_, _, body) = http.get("/v2/applications/$applicationId/decision-drafts")
            .asUser(serviceWorker)
            .responseObject<DecisionDraftJSON>(objectMapper)
        assertEquals(
            DecisionDraftJSON(
                decisions = decisions.sortedBy { it.type },
                placementUnitName = "Test Daycare",
                unit = DecisionUnit(
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
                guardian = GuardianInfo(null, adult.ssn, adult.firstName, adult.lastName, isVtjGuardian = true),
                otherGuardian = if (otherGuardian != null) GuardianInfo(
                    otherGuardian.id,
                    otherGuardian.ssn,
                    otherGuardian.firstName,
                    otherGuardian.lastName,
                    isVtjGuardian = true
                ) else null,
                child = ChildInfo(child.ssn, child.firstName, child.lastName)
            ),
            body.get().copy(
                decisions = body.get().decisions
                    .map { it.copy(id = decisionId) }
                    .sortedBy { it.type }
            )
        )
    }

    private fun createDecisions(
        applicationId: ApplicationId
    ): List<DecisionTableRow> {
        val (_, res, _) = http.post("/v2/applications/$applicationId/actions/send-decisions-without-proposal")
            .asUser(serviceWorker)
            .response()
        assertTrue(res.isSuccessful)

        asyncJobRunner.runPendingJobsSync()

        val rows = db.read { r -> r.getDecisionRowsByApplication(applicationId).list() }
        rows.forEach { row ->
            assertEquals(serviceWorker.id, row.createdBy)
            assertEquals(DecisionStatus.PENDING, row.status)
            assertNull(row.requestedStartDate)
            assertNull(row.resolved)
            assertNull(row.resolvedBy)
            assertNotNull(row.sentDate)
            assertNotNull(row.documentKey)
        }
        db.read { r ->
            assertEquals(ApplicationStatus.WAITING_CONFIRMATION, r.getApplicationStatus(applicationId))
        }
        return rows
    }

    @Test
    fun testEndpointSecurity() {
        val period = FiniteDateRange(
            LocalDate.of(2020, 3, 17),
            LocalDate.of(2023, 7, 31)
        )
        val applicationId = insertInitialData(
            type = PlacementType.DAYCARE,
            period = period
        )
        val invalidRoleLists = listOf(
            setOf(UserRole.UNIT_SUPERVISOR),
            setOf(UserRole.FINANCE_ADMIN),
            setOf(UserRole.END_USER),
            setOf()
        )
        invalidRoleLists.forEach { roles ->
            val (_, res, _) = http.get("/v2/applications/$applicationId/decision-drafts")
                .asUser(AuthenticatedUser.Employee(testDecisionMaker_1.id, roles))
                .response()
            assertEquals(403, res.statusCode)
        }
    }

    private fun insertInitialData(
        type: PlacementType,
        unit: UnitData.Detailed = testDaycare,
        adult: PersonData.Detailed = testAdult_5,
        child: PersonData.Detailed = testChild_6,
        period: FiniteDateRange,
        preschoolDaycarePeriod: FiniteDateRange? = null,
        preparatoryEducation: Boolean = false
    ): ApplicationId = db.transaction { tx ->
        val applicationId = tx.insertTestApplication(
            status = ApplicationStatus.WAITING_PLACEMENT,
            guardianId = adult.id,
            childId = child.id
        )
        val preschoolDaycare = type == PlacementType.PRESCHOOL_DAYCARE
        tx.insertTestApplicationForm(
            applicationId,
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
