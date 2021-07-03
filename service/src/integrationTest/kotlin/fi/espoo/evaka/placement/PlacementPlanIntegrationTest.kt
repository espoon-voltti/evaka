// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.DaycarePlacementPlan
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.CareDetails
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.invoicing.domain.UnitData
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.test.getApplicationStatus
import fi.espoo.evaka.test.getPlacementPlanRowByApplication
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_7
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testSvebiDaycare
import fi.espoo.evaka.toDaycareFormAdult
import fi.espoo.evaka.toDaycareFormChild
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PlacementPlanIntegrationTest : FullApplicationTest() {
    private val serviceWorker = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))

    @BeforeEach
    private fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            tx.insertGeneralTestFixtures()
        }
    }

    @Test
    fun testDaycareFullTime() {
        val preferredStartDate = LocalDate.of(2020, 3, 17)
        val applicationId = insertInitialData(
            status = ApplicationStatus.WAITING_PLACEMENT,
            type = ApplicationType.DAYCARE,
            preferredStartDate = preferredStartDate
        )
        val defaultEndDate = LocalDate.of(2023, 7, 31)
        checkPlacementPlanDraft(
            applicationId,
            type = PlacementType.DAYCARE,
            period = FiniteDateRange(preferredStartDate, defaultEndDate)
        )
        createPlacementPlanAndAssert(
            applicationId,
            PlacementType.DAYCARE,
            DaycarePlacementPlan(
                unitId = testDaycare.id,
                period = FiniteDateRange(preferredStartDate.plusDays(1), defaultEndDate.minusDays(1))
            )
        )
    }

    @Test
    fun testDaycareFullTimeWithRestrictedDetails() {
        val preferredStartDate = LocalDate.of(2020, 3, 17)
        val applicationId = insertInitialData(
            status = ApplicationStatus.WAITING_PLACEMENT,
            type = ApplicationType.DAYCARE,
            preferredStartDate = preferredStartDate,
            adult = testAdult_7
        )
        val defaultEndDate = LocalDate.of(2023, 7, 31)
        checkPlacementPlanDraft(
            applicationId,
            type = PlacementType.DAYCARE,
            period = FiniteDateRange(preferredStartDate, defaultEndDate),
            guardianHasRestrictedDetails = true
        )
        createPlacementPlanAndAssert(
            applicationId,
            PlacementType.DAYCARE,
            DaycarePlacementPlan(
                unitId = testDaycare.id,
                period = FiniteDateRange(preferredStartDate.plusDays(1), defaultEndDate.minusDays(1))
            )
        )
    }

    @Test
    fun testDaycarePartTime() {
        val preferredStartDate = LocalDate.of(2022, 11, 30)
        val applicationId = insertInitialData(
            status = ApplicationStatus.WAITING_PLACEMENT,
            type = ApplicationType.DAYCARE,
            partTime = true,
            preferredStartDate = preferredStartDate
        )
        val defaultEndDate = LocalDate.of(2023, 7, 31)
        checkPlacementPlanDraft(
            applicationId,
            type = PlacementType.DAYCARE_PART_TIME,
            period = FiniteDateRange(preferredStartDate, defaultEndDate)
        )
        createPlacementPlanAndAssert(
            applicationId,
            PlacementType.DAYCARE_PART_TIME,
            DaycarePlacementPlan(
                unitId = testDaycare.id,
                period = FiniteDateRange(preferredStartDate.plusDays(1), defaultEndDate.minusDays(1))
            )
        )
    }

    @Test
    fun testPreschoolOnly() {
        val preferredStartDate = LocalDate.of(2023, 8, 1)
        val applicationId = insertInitialData(
            status = ApplicationStatus.WAITING_PLACEMENT,
            type = ApplicationType.PRESCHOOL,
            preferredStartDate = preferredStartDate
        )
        val defaultEndDate = LocalDate.of(2024, 6, 3)
        checkPlacementPlanDraft(
            applicationId,
            type = PlacementType.PRESCHOOL,
            period = FiniteDateRange(preferredStartDate, defaultEndDate)
        )
        createPlacementPlanAndAssert(
            applicationId,
            PlacementType.PRESCHOOL,
            DaycarePlacementPlan(
                unitId = testDaycare.id,
                period = FiniteDateRange(preferredStartDate.plusDays(1), defaultEndDate.minusDays(1))
            )
        )
    }

    @Test
    fun testPreschoolWithDaycare() {
        val preferredStartDate = LocalDate.of(2023, 8, 1)
        val applicationId = insertInitialData(
            status = ApplicationStatus.WAITING_PLACEMENT,
            type = ApplicationType.PRESCHOOL,
            preschoolDaycare = true,
            preferredStartDate = preferredStartDate
        )
        val defaultEndDate = LocalDate.of(2024, 6, 3)
        val defaultDaycareEndDate = LocalDate.of(2024, 7, 31)
        checkPlacementPlanDraft(
            applicationId,
            type = PlacementType.PRESCHOOL_DAYCARE,
            period = FiniteDateRange(preferredStartDate, defaultEndDate),
            preschoolDaycarePeriod = FiniteDateRange(preferredStartDate, defaultDaycareEndDate)
        )
        createPlacementPlanAndAssert(
            applicationId,
            PlacementType.PRESCHOOL_DAYCARE,
            DaycarePlacementPlan(
                unitId = testDaycare.id,
                period = FiniteDateRange(preferredStartDate.plusDays(1), defaultEndDate.minusDays(1)),
                preschoolDaycarePeriod = FiniteDateRange(
                    preferredStartDate.minusDays(1),
                    defaultDaycareEndDate.plusDays(1)
                )
            )
        )
    }

    @Test
    fun testPreschoolWithSvebiDaycare() {
        val preferredStartDate = LocalDate.of(2023, 8, 1)
        val applicationId = insertInitialData(
            status = ApplicationStatus.WAITING_PLACEMENT,
            type = ApplicationType.PRESCHOOL,
            preschoolDaycare = true,
            preferredStartDate = preferredStartDate,
            preferredUnits = listOf(testSvebiDaycare)
        )
        val svebiEndDate = LocalDate.of(2024, 6, 6)
        val defaultDaycareEndDate = LocalDate.of(2024, 7, 31)
        checkPlacementPlanDraft(
            applicationId,
            type = PlacementType.PRESCHOOL_DAYCARE,
            period = FiniteDateRange(preferredStartDate, svebiEndDate),
            preschoolDaycarePeriod = FiniteDateRange(preferredStartDate, defaultDaycareEndDate),
            preferredUnits = listOf(testSvebiDaycare)
        )
        createPlacementPlanAndAssert(
            applicationId,
            PlacementType.PRESCHOOL_DAYCARE,
            DaycarePlacementPlan(
                unitId = testSvebiDaycare.id,
                period = FiniteDateRange(preferredStartDate.plusDays(1), svebiEndDate.minusDays(1)),
                preschoolDaycarePeriod = FiniteDateRange(
                    preferredStartDate.minusDays(1),
                    defaultDaycareEndDate.plusDays(1)
                )
            )
        )
    }

    @Test
    fun testPreschoolWithPreparatory() {
        val preferredStartDate = LocalDate.of(2023, 8, 1)
        val applicationId = insertInitialData(
            status = ApplicationStatus.WAITING_PLACEMENT,
            type = ApplicationType.PRESCHOOL,
            preferredStartDate = preferredStartDate,
            preparatory = true
        )
        val defaultEndDate = LocalDate.of(2024, 6, 3)
        checkPlacementPlanDraft(
            applicationId,
            type = PlacementType.PREPARATORY,
            period = FiniteDateRange(preferredStartDate, defaultEndDate)
        )
        createPlacementPlanAndAssert(
            applicationId,
            PlacementType.PREPARATORY,
            DaycarePlacementPlan(
                unitId = testDaycare.id,
                period = FiniteDateRange(preferredStartDate.plusDays(1), defaultEndDate.minusDays(1))
            )
        )
    }

    @Test
    fun testEndpointSecurity() {
        val applicationId = insertInitialData(
            status = ApplicationStatus.WAITING_PLACEMENT,
            type = ApplicationType.DAYCARE,
            preferredStartDate = LocalDate.of(2020, 3, 17)
        )
        val invalidRoleLists = listOf(
            setOf(UserRole.UNIT_SUPERVISOR),
            setOf(UserRole.FINANCE_ADMIN),
            setOf(UserRole.END_USER),
            setOf()
        )
        invalidRoleLists.forEach { roles ->
            val (_, res, _) = http.get("/v2/applications/$applicationId/placement-draft")
                .asUser(AuthenticatedUser.Employee(testDecisionMaker_1.id, roles))
                .response()
            assertEquals(403, res.statusCode)
        }
        val proposal = DaycarePlacementPlan(
            unitId = testDaycare.id,
            period = FiniteDateRange(LocalDate.of(2020, 3, 17), LocalDate.of(2020, 6, 1))
        )
        invalidRoleLists.forEach { roles ->
            val (_, res, _) = http.post("/v2/applications/$applicationId/actions/create-placement-plan")
                .jsonBody(objectMapper.writeValueAsString(proposal))
                .asUser(AuthenticatedUser.Employee(testDecisionMaker_1.id, roles))
                .response()
            assertEquals(403, res.statusCode)
        }
    }

    private fun checkPlacementPlanDraft(
        applicationId: ApplicationId,
        type: PlacementType,
        child: PersonData.Detailed = testChild_1,
        preferredUnits: List<UnitData.Detailed> = listOf(testDaycare, testDaycare2),
        period: FiniteDateRange,
        preschoolDaycarePeriod: FiniteDateRange? = null,
        placements: List<PlacementDraftPlacement> = emptyList(),
        guardianHasRestrictedDetails: Boolean = false
    ) {
        val (_, _, body) = http.get("/v2/applications/$applicationId/placement-draft")
            .asUser(serviceWorker)
            .responseObject<PlacementPlanDraft>(objectMapper)
        assertEquals(
            PlacementPlanDraft(
                type = type,
                child = PlacementDraftChild(
                    id = child.id,
                    firstName = child.firstName,
                    lastName = child.lastName,
                    dob = child.dateOfBirth
                ),
                preferredUnits = preferredUnits.map {
                    PlacementDraftUnit(
                        id = it.id,
                        name = it.name
                    )
                },
                period = period,
                preschoolDaycarePeriod = preschoolDaycarePeriod,
                placements = placements,
                guardianHasRestrictedDetails = guardianHasRestrictedDetails
            ),
            body.get()
        )
    }

    private fun createPlacementPlanAndAssert(
        applicationId: ApplicationId,
        type: PlacementType,
        proposal: DaycarePlacementPlan
    ) {
        val (_, res, _) = http.post("/v2/applications/$applicationId/actions/create-placement-plan")
            .jsonBody(objectMapper.writeValueAsString(proposal))
            .asUser(serviceWorker)
            .response()
        assertTrue(res.isSuccessful)

        db.read { r ->
            r.getPlacementPlanRowByApplication(applicationId).one().also {
                assertEquals(type, it.type)
                assertEquals(proposal.unitId, it.unitId)
                assertEquals(proposal.period, it.period())
                assertEquals(proposal.preschoolDaycarePeriod, it.preschoolDaycarePeriod())
                assertEquals(false, it.deleted)
            }
            assertEquals(ApplicationStatus.WAITING_DECISION, r.getApplicationStatus(applicationId))
        }
    }
    private fun insertInitialData(
        status: ApplicationStatus,
        type: ApplicationType,
        preferredStartDate: LocalDate,
        adult: PersonData.Detailed = testAdult_1,
        child: PersonData.Detailed = testChild_1,
        partTime: Boolean = false,
        preschoolDaycare: Boolean = false,
        preferredUnits: List<UnitData.Detailed> = listOf(testDaycare, testDaycare2),
        preparatory: Boolean = false
    ): ApplicationId = db.transaction { tx ->
        val applicationId = tx.insertTestApplication(
            status = status,
            guardianId = adult.id,
            childId = child.id
        )
        val careDetails = if (preparatory) CareDetails(preparatory = true) else CareDetails()
        tx.insertTestApplicationForm(
            applicationId,
            DaycareFormV0(
                type = type,
                partTime = partTime,
                connectedDaycare = preschoolDaycare,
                serviceStart = "08:00".takeIf { preschoolDaycare },
                serviceEnd = "16:00".takeIf { preschoolDaycare },
                child = child.toDaycareFormChild(),
                guardian = adult.toDaycareFormAdult(adult.restrictedDetailsEnabled),
                apply = Apply(preferredUnits = preferredUnits.map { it.id }),
                preferredStartDate = preferredStartDate,
                careDetails = careDetails
            )
        )
        applicationId
    }
}
