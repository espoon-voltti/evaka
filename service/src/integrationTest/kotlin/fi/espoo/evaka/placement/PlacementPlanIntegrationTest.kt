// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.DaycarePlacementPlan
import fi.espoo.evaka.application.persistence.FormType
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.CareDetails
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.invoicing.domain.UnitData
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.config.Roles
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.shared.domain.ClosedPeriod
import fi.espoo.evaka.test.getApplicationStatus
import fi.espoo.evaka.test.getPlacementPlanRowByApplication
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.toDaycareFormAdult
import fi.espoo.evaka.toDaycareFormChild
import org.jdbi.v3.core.Handle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class PlacementPlanIntegrationTest : FullApplicationTest() {
    private val serviceWorker = AuthenticatedUser(testDecisionMaker_1.id, setOf(Roles.SERVICE_WORKER))

    @BeforeEach
    private fun beforeEach() {
        jdbi.handle { h ->
            resetDatabase(h)
            insertGeneralTestFixtures(h)
        }
    }

    @Test
    fun testDaycareFullTime(): Unit = jdbi.handle { h ->
        val preferredStartDate = LocalDate.of(2020, 3, 17)
        val applicationId = insertInitialData(
            h,
            status = ApplicationStatus.WAITING_PLACEMENT,
            type = FormType.DAYCARE,
            preferredStartDate = preferredStartDate
        )
        val defaultEndDate = LocalDate.of(2023, 7, 31)
        checkPlacementPlanDraft(
            applicationId,
            type = PlacementType.DAYCARE,
            period = ClosedPeriod(preferredStartDate, defaultEndDate)
        )
        createPlacementPlanAndAssert(
            h,
            applicationId,
            PlacementType.DAYCARE,
            DaycarePlacementPlan(
                unitId = testDaycare.id,
                period = ClosedPeriod(preferredStartDate.plusDays(1), defaultEndDate.minusDays(1))
            )
        )
    }

    @Test
    fun testDaycarePartTime(): Unit = jdbi.handle { h ->
        val preferredStartDate = LocalDate.of(2022, 11, 30)
        val applicationId = insertInitialData(
            h,
            status = ApplicationStatus.WAITING_PLACEMENT,
            type = FormType.DAYCARE,
            partTime = true,
            preferredStartDate = preferredStartDate
        )
        val defaultEndDate = LocalDate.of(2023, 7, 31)
        checkPlacementPlanDraft(
            applicationId,
            type = PlacementType.DAYCARE_PART_TIME,
            period = ClosedPeriod(preferredStartDate, defaultEndDate)
        )
        createPlacementPlanAndAssert(
            h,
            applicationId,
            PlacementType.DAYCARE_PART_TIME,
            DaycarePlacementPlan(
                unitId = testDaycare.id,
                period = ClosedPeriod(preferredStartDate.plusDays(1), defaultEndDate.minusDays(1))
            )
        )
    }

    @Test
    fun testPreschoolOnly(): Unit = jdbi.handle { h ->
        val preferredStartDate = LocalDate.of(2023, 8, 1)
        val applicationId = insertInitialData(
            h,
            status = ApplicationStatus.WAITING_PLACEMENT,
            type = FormType.PRESCHOOL,
            preferredStartDate = preferredStartDate
        )
        val defaultEndDate = LocalDate.of(2024, 5, 31)
        checkPlacementPlanDraft(
            applicationId,
            type = PlacementType.PRESCHOOL,
            period = ClosedPeriod(preferredStartDate, defaultEndDate)
        )
        createPlacementPlanAndAssert(
            h,
            applicationId,
            PlacementType.PRESCHOOL,
            DaycarePlacementPlan(
                unitId = testDaycare.id,
                period = ClosedPeriod(preferredStartDate.plusDays(1), defaultEndDate.minusDays(1))
            )
        )
    }

    @Test
    fun testPreschoolWithDaycare(): Unit = jdbi.handle { h ->
        val preferredStartDate = LocalDate.of(2023, 8, 1)
        val applicationId = insertInitialData(
            h,
            status = ApplicationStatus.WAITING_PLACEMENT,
            type = FormType.PRESCHOOL,
            preschoolDaycare = true,
            preferredStartDate = preferredStartDate
        )
        val defaultEndDate = LocalDate.of(2024, 5, 31)
        val defaultDaycareEndDate = LocalDate.of(2024, 7, 31)
        checkPlacementPlanDraft(
            applicationId,
            type = PlacementType.PRESCHOOL_DAYCARE,
            period = ClosedPeriod(preferredStartDate, defaultEndDate),
            preschoolDaycarePeriod = ClosedPeriod(preferredStartDate, defaultDaycareEndDate)
        )
        createPlacementPlanAndAssert(
            h,
            applicationId,
            PlacementType.PRESCHOOL_DAYCARE,
            DaycarePlacementPlan(
                unitId = testDaycare.id,
                period = ClosedPeriod(preferredStartDate.plusDays(1), defaultEndDate.minusDays(1)),
                preschoolDaycarePeriod = ClosedPeriod(
                    preferredStartDate.minusDays(1),
                    defaultDaycareEndDate.plusDays(1)
                )
            )
        )
    }

    @Test
    fun testPreschoolWithPreparatory(): Unit = jdbi.handle { h ->
        val preferredStartDate = LocalDate.of(2023, 8, 1)
        val applicationId = insertInitialData(
            h,
            status = ApplicationStatus.WAITING_PLACEMENT,
            type = FormType.PRESCHOOL,
            preferredStartDate = preferredStartDate,
            preparatory = true
        )
        val defaultEndDate = LocalDate.of(2024, 5, 31)
        checkPlacementPlanDraft(
            applicationId,
            type = PlacementType.PREPARATORY,
            period = ClosedPeriod(preferredStartDate, defaultEndDate)
        )
        createPlacementPlanAndAssert(
            h,
            applicationId,
            PlacementType.PREPARATORY,
            DaycarePlacementPlan(
                unitId = testDaycare.id,
                period = ClosedPeriod(preferredStartDate.plusDays(1), defaultEndDate.minusDays(1))
            )
        )
    }

    @Test
    fun testEndpointSecurity() {
        val applicationId = jdbi.handle { h ->
            insertInitialData(
                h,
                status = ApplicationStatus.WAITING_PLACEMENT,
                type = FormType.DAYCARE,
                preferredStartDate = LocalDate.of(2020, 3, 17)
            )
        }
        val invalidRoleLists = listOf(
            setOf(Roles.UNIT_SUPERVISOR),
            setOf(Roles.FINANCE_ADMIN),
            setOf(Roles.END_USER),
            setOf()
        )
        invalidRoleLists.forEach { roles ->
            val (_, res, _) = http.get("/v2/applications/$applicationId/placement-draft")
                .asUser(AuthenticatedUser(testDecisionMaker_1.id, roles))
                .response()
            assertEquals(403, res.statusCode)
        }
        val proposal = DaycarePlacementPlan(
            unitId = testDaycare.id,
            period = ClosedPeriod(LocalDate.of(2020, 3, 17), LocalDate.of(2020, 6, 1))
        )
        invalidRoleLists.forEach { roles ->
            val (_, res, _) = http.post("/v2/applications/$applicationId/actions/create-placement-plan")
                .jsonBody(objectMapper.writeValueAsString(proposal))
                .asUser(AuthenticatedUser(testDecisionMaker_1.id, roles))
                .response()
            assertEquals(403, res.statusCode)
        }
    }

    private fun checkPlacementPlanDraft(
        applicationId: UUID,
        type: PlacementType,
        child: PersonData.Detailed = testChild_1,
        preferredUnits: List<UnitData.Detailed> = listOf(testDaycare, testDaycare2),
        period: ClosedPeriod,
        preschoolDaycarePeriod: ClosedPeriod? = null,
        placements: List<PlacementDraftPlacement> = emptyList()
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
                placements = placements
            ),
            body.get()
        )
    }

    private fun createPlacementPlanAndAssert(
        h: Handle,
        applicationId: UUID,
        type: PlacementType,
        proposal: DaycarePlacementPlan
    ) {
        val (_, res, _) = http.post("/v2/applications/$applicationId/actions/create-placement-plan")
            .jsonBody(objectMapper.writeValueAsString(proposal))
            .asUser(serviceWorker)
            .response()
        assertTrue(res.isSuccessful)

        getPlacementPlanRowByApplication(h, applicationId).one().also {
            assertEquals(type, it.type)
            assertEquals(proposal.unitId, it.unitId)
            assertEquals(proposal.period, it.period())
            assertEquals(proposal.preschoolDaycarePeriod, it.preschoolDaycarePeriod())
            assertEquals(false, it.deleted)
        }
        assertEquals(ApplicationStatus.WAITING_DECISION, getApplicationStatus(h, applicationId))
    }
}

private fun insertInitialData(
    h: Handle,
    status: ApplicationStatus,
    type: FormType,
    preferredStartDate: LocalDate,
    adult: PersonData.Detailed = testAdult_1,
    child: PersonData.Detailed = testChild_1,
    partTime: Boolean = false,
    preschoolDaycare: Boolean = false,
    preferredUnits: List<UnitData.Detailed> = listOf(testDaycare, testDaycare2),
    preparatory: Boolean = false
): UUID {
    val applicationId = insertTestApplication(
        h,
        status = status,
        guardianId = adult.id,
        childId = child.id
    )
    val careDetails = if (preparatory) CareDetails(preparatory = true) else CareDetails()
    insertTestApplicationForm(
        h, applicationId,
        DaycareFormV0(
            type = type,
            partTime = partTime,
            connectedDaycare = preschoolDaycare,
            serviceStart = "08:00".takeIf { preschoolDaycare },
            serviceEnd = "16:00".takeIf { preschoolDaycare },
            child = child.toDaycareFormChild(),
            guardian = adult.toDaycareFormAdult(),
            apply = Apply(preferredUnits = preferredUnits.map { it.id }),
            preferredStartDate = preferredStartDate,
            careDetails = careDetails
        )
    )
    return applicationId
}
