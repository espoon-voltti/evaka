// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.ApplicationControllerV2
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.DaycarePlacementPlan
import fi.espoo.evaka.application.ServiceNeedOption
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.CareDetails
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.preschoolTerm2023
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.test.getApplicationStatus
import fi.espoo.evaka.test.getPlacementPlanRowByApplication
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_7
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.toDaycareFormAdult
import fi.espoo.evaka.toDaycareFormChild
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import fi.espoo.evaka.vtjclient.service.persondetails.legacyMockVtjDataset
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class PlacementPlanIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val serviceWorker =
        AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))

    @Autowired private lateinit var applicationController: ApplicationControllerV2

    private val area = DevCareArea(name = "Area", shortName = "area")
    private val daycare1 = DevDaycare(areaId = area.id)
    private val daycare2 = DevDaycare(areaId = area.id)

    // isSvebiUnit uses hard-coded area short name "svenska-bildningstjanster"
    private val areaSvebi =
        DevCareArea(name = "Svenska Bildningstjänster", shortName = "svenska-bildningstjanster")
    private val daycareSvebi = DevDaycare(areaId = areaSvebi.id, language = Language.sv)

    private val clock =
        MockEvakaClock(HelsinkiDateTime.Companion.of(LocalDate.of(2020, 1, 1), LocalTime.of(15, 0)))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testDecisionMaker_1)
            tx.insert(area)
            tx.insert(daycare1)
            tx.insert(daycare2)
            tx.insert(areaSvebi)
            tx.insert(daycareSvebi)
            listOf(testAdult_1, testAdult_7).forEach { tx.insert(it, DevPersonType.ADULT) }
            tx.insert(testChild_1, DevPersonType.CHILD)
            tx.insert(preschoolTerm2023)
        }
        MockPersonDetailsService.add(legacyMockVtjDataset())
    }

    @Test
    fun testDaycareFullTime() {
        val preferredStartDate = LocalDate.of(2020, 3, 17)
        val applicationId =
            insertInitialData(
                status = ApplicationStatus.WAITING_PLACEMENT,
                type = ApplicationType.DAYCARE,
                preferredStartDate = preferredStartDate,
            )
        val defaultEndDate = LocalDate.of(2023, 7, 31)
        checkPlacementPlanDraft(
            applicationId,
            type = PlacementType.DAYCARE,
            period = FiniteDateRange(preferredStartDate, defaultEndDate),
        )
        createPlacementPlanAndAssert(
            applicationId,
            PlacementType.DAYCARE,
            DaycarePlacementPlan(
                unitId = daycare1.id,
                period =
                    FiniteDateRange(preferredStartDate.plusDays(1), defaultEndDate.minusDays(1)),
            ),
        )
    }

    @Test
    fun testDaycareFullTimeWithRestrictedDetails() {
        val preferredStartDate = LocalDate.of(2020, 3, 17)
        val applicationId =
            insertInitialData(
                status = ApplicationStatus.WAITING_PLACEMENT,
                type = ApplicationType.DAYCARE,
                preferredStartDate = preferredStartDate,
                adult = testAdult_7,
            )
        val defaultEndDate = LocalDate.of(2023, 7, 31)
        checkPlacementPlanDraft(
            applicationId,
            type = PlacementType.DAYCARE,
            period = FiniteDateRange(preferredStartDate, defaultEndDate),
            guardianHasRestrictedDetails = true,
        )
        createPlacementPlanAndAssert(
            applicationId,
            PlacementType.DAYCARE,
            DaycarePlacementPlan(
                unitId = daycare1.id,
                period =
                    FiniteDateRange(preferredStartDate.plusDays(1), defaultEndDate.minusDays(1)),
            ),
        )
    }

    @Test
    fun testDaycarePartTime() {
        val preferredStartDate = LocalDate.of(2022, 11, 30)
        val applicationId =
            insertInitialData(
                status = ApplicationStatus.WAITING_PLACEMENT,
                type = ApplicationType.DAYCARE,
                partTime = true,
                preferredStartDate = preferredStartDate,
            )
        val defaultEndDate = LocalDate.of(2023, 7, 31)
        checkPlacementPlanDraft(
            applicationId,
            type = PlacementType.DAYCARE_PART_TIME,
            period = FiniteDateRange(preferredStartDate, defaultEndDate),
        )
        createPlacementPlanAndAssert(
            applicationId,
            PlacementType.DAYCARE_PART_TIME,
            DaycarePlacementPlan(
                unitId = daycare1.id,
                period =
                    FiniteDateRange(preferredStartDate.plusDays(1), defaultEndDate.minusDays(1)),
            ),
        )
    }

    @Test
    fun testPreschoolOnly() {
        val preferredStartDate = LocalDate.of(2023, 8, 1)
        val applicationId =
            insertInitialData(
                status = ApplicationStatus.WAITING_PLACEMENT,
                type = ApplicationType.PRESCHOOL,
                preferredStartDate = preferredStartDate,
            )
        val defaultEndDate = LocalDate.of(2024, 6, 3)
        checkPlacementPlanDraft(
            applicationId,
            type = PlacementType.PRESCHOOL,
            period = FiniteDateRange(preferredStartDate, defaultEndDate),
        )
        createPlacementPlanAndAssert(
            applicationId,
            PlacementType.PRESCHOOL,
            DaycarePlacementPlan(
                unitId = daycare1.id,
                period =
                    FiniteDateRange(preferredStartDate.plusDays(1), defaultEndDate.minusDays(1)),
            ),
        )
    }

    @Test
    fun testPreschoolWithDaycare() {
        val preferredStartDate = LocalDate.of(2023, 8, 1)
        val applicationId =
            insertInitialData(
                status = ApplicationStatus.WAITING_PLACEMENT,
                type = ApplicationType.PRESCHOOL,
                preschoolDaycare = true,
                preferredStartDate = preferredStartDate,
            )
        val defaultEndDate = LocalDate.of(2024, 6, 3)
        val defaultDaycareEndDate = LocalDate.of(2024, 7, 31)
        checkPlacementPlanDraft(
            applicationId,
            type = PlacementType.PRESCHOOL_DAYCARE,
            period = FiniteDateRange(preferredStartDate, defaultEndDate),
            preschoolDaycarePeriod = FiniteDateRange(preferredStartDate, defaultDaycareEndDate),
        )
        createPlacementPlanAndAssert(
            applicationId,
            PlacementType.PRESCHOOL_DAYCARE,
            DaycarePlacementPlan(
                unitId = daycare1.id,
                period =
                    FiniteDateRange(preferredStartDate.plusDays(1), defaultEndDate.minusDays(1)),
                preschoolDaycarePeriod =
                    FiniteDateRange(
                        preferredStartDate.minusDays(1),
                        defaultDaycareEndDate.plusDays(1),
                    ),
            ),
        )
    }

    @Test
    fun testPreschoolWithClub() {
        val preferredStartDate = LocalDate.of(2023, 8, 1)
        val applicationId =
            insertInitialData(
                status = ApplicationStatus.WAITING_PLACEMENT,
                type = ApplicationType.PRESCHOOL,
                preschoolDaycare = true,
                preferredStartDate = preferredStartDate,
                serviceNeedOption =
                    ServiceNeedOption(
                        ServiceNeedOptionId(UUID.randomUUID()),
                        "",
                        "",
                        "",
                        PlacementType.PRESCHOOL_CLUB,
                    ),
            )
        val defaultEndDate = LocalDate.of(2024, 6, 3)
        val defaultClubEndDate = LocalDate.of(2024, 6, 3)
        checkPlacementPlanDraft(
            applicationId,
            type = PlacementType.PRESCHOOL_CLUB,
            period = FiniteDateRange(preferredStartDate, defaultEndDate),
            preschoolDaycarePeriod = FiniteDateRange(preferredStartDate, defaultClubEndDate),
        )
        createPlacementPlanAndAssert(
            applicationId,
            PlacementType.PRESCHOOL_CLUB,
            DaycarePlacementPlan(
                unitId = daycare1.id,
                period =
                    FiniteDateRange(preferredStartDate.plusDays(1), defaultEndDate.minusDays(1)),
                preschoolDaycarePeriod =
                    FiniteDateRange(preferredStartDate.minusDays(1), defaultClubEndDate.plusDays(1)),
            ),
        )
    }

    @Test
    fun testPreschoolWithSvebiDaycare() {
        val preferredStartDate = LocalDate.of(2023, 8, 1)
        val applicationId =
            insertInitialData(
                status = ApplicationStatus.WAITING_PLACEMENT,
                type = ApplicationType.PRESCHOOL,
                preschoolDaycare = true,
                preferredStartDate = preferredStartDate,
                preferredUnits = listOf(daycareSvebi),
            )
        val svebiEndDate = LocalDate.of(2024, 6, 6)
        val defaultDaycareEndDate = LocalDate.of(2024, 7, 31)
        checkPlacementPlanDraft(
            applicationId,
            type = PlacementType.PRESCHOOL_DAYCARE,
            period = FiniteDateRange(preferredStartDate, svebiEndDate),
            preschoolDaycarePeriod = FiniteDateRange(preferredStartDate, defaultDaycareEndDate),
            preferredUnits = listOf(daycareSvebi),
        )
        createPlacementPlanAndAssert(
            applicationId,
            PlacementType.PRESCHOOL_DAYCARE,
            DaycarePlacementPlan(
                unitId = daycareSvebi.id,
                period = FiniteDateRange(preferredStartDate.plusDays(1), svebiEndDate.minusDays(1)),
                preschoolDaycarePeriod =
                    FiniteDateRange(
                        preferredStartDate.minusDays(1),
                        defaultDaycareEndDate.plusDays(1),
                    ),
            ),
        )
    }

    @Test
    fun testPreschoolWithPreparatory() {
        val preferredStartDate = LocalDate.of(2023, 8, 1)
        val applicationId =
            insertInitialData(
                status = ApplicationStatus.WAITING_PLACEMENT,
                type = ApplicationType.PRESCHOOL,
                preferredStartDate = preferredStartDate,
                preparatory = true,
            )
        val defaultEndDate = LocalDate.of(2024, 6, 3)
        checkPlacementPlanDraft(
            applicationId,
            type = PlacementType.PREPARATORY,
            period = FiniteDateRange(preferredStartDate, defaultEndDate),
        )
        createPlacementPlanAndAssert(
            applicationId,
            PlacementType.PREPARATORY,
            DaycarePlacementPlan(
                unitId = daycare1.id,
                period =
                    FiniteDateRange(preferredStartDate.plusDays(1), defaultEndDate.minusDays(1)),
            ),
        )
    }

    @Test
    fun testEndpointSecurity() {
        val applicationId =
            insertInitialData(
                status = ApplicationStatus.WAITING_PLACEMENT,
                type = ApplicationType.DAYCARE,
                preferredStartDate = LocalDate.of(2020, 3, 17),
            )
        val invalidRoleLists =
            listOf(
                setOf(UserRole.UNIT_SUPERVISOR),
                setOf(UserRole.FINANCE_ADMIN),
                setOf(UserRole.END_USER),
                setOf(),
            )
        invalidRoleLists.forEach { roles ->
            assertThrows<Forbidden> {
                applicationController.getPlacementPlanDraft(
                    dbInstance(),
                    AuthenticatedUser.Employee(testDecisionMaker_1.id, roles),
                    clock,
                    applicationId,
                )
            }
        }
        val proposal =
            DaycarePlacementPlan(
                unitId = daycare1.id,
                period = FiniteDateRange(LocalDate.of(2020, 3, 17), LocalDate.of(2020, 6, 1)),
            )
        invalidRoleLists.forEach { roles ->
            assertThrows<Forbidden> {
                applicationController.createPlacementPlan(
                    dbInstance(),
                    AuthenticatedUser.Employee(testDecisionMaker_1.id, roles),
                    clock,
                    applicationId,
                    proposal,
                )
            }
        }
    }

    private fun checkPlacementPlanDraft(
        applicationId: ApplicationId,
        type: PlacementType,
        child: DevPerson = testChild_1,
        preferredUnits: List<DevDaycare> = listOf(daycare1, daycare2),
        period: FiniteDateRange,
        preschoolDaycarePeriod: FiniteDateRange? = null,
        placements: List<PlacementSummary> = emptyList(),
        guardianHasRestrictedDetails: Boolean = false,
    ) {
        val result =
            applicationController.getPlacementPlanDraft(
                dbInstance(),
                serviceWorker,
                clock,
                applicationId,
            )
        assertEquals(
            PlacementPlanDraft(
                type = type,
                child =
                    PlacementDraftChild(
                        id = child.id,
                        firstName = child.firstName,
                        lastName = child.lastName,
                        dob = child.dateOfBirth,
                    ),
                preferredUnits =
                    preferredUnits.map { PlacementDraftUnit(id = it.id, name = it.name) },
                period = period,
                preschoolDaycarePeriod = preschoolDaycarePeriod,
                placements = placements,
                guardianHasRestrictedDetails = guardianHasRestrictedDetails,
            ),
            result,
        )
    }

    private fun createPlacementPlanAndAssert(
        applicationId: ApplicationId,
        type: PlacementType,
        proposal: DaycarePlacementPlan,
    ) {
        applicationController.createPlacementPlan(
            dbInstance(),
            serviceWorker,
            clock,
            applicationId,
            proposal,
        )

        db.read { r ->
            r.getPlacementPlanRowByApplication(applicationId).also {
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
        adult: DevPerson = testAdult_1,
        child: DevPerson = testChild_1,
        partTime: Boolean = false,
        serviceNeedOption: ServiceNeedOption? = null,
        preschoolDaycare: Boolean = false,
        preferredUnits: List<DevDaycare> = listOf(daycare1, daycare2),
        preparatory: Boolean = false,
    ): ApplicationId =
        db.transaction { tx ->
            val careDetails = if (preparatory) CareDetails(preparatory = true) else CareDetails()
            tx.insertTestApplication(
                status = status,
                guardianId = adult.id,
                childId = child.id,
                type = type,
                document =
                    DaycareFormV0(
                        type = type,
                        partTime = partTime,
                        serviceNeedOption = serviceNeedOption,
                        connectedDaycare = preschoolDaycare,
                        serviceStart = "08:00".takeIf { preschoolDaycare },
                        serviceEnd = "16:00".takeIf { preschoolDaycare },
                        child = child.toDaycareFormChild(),
                        guardian = adult.toDaycareFormAdult(adult.restrictedDetailsEnabled),
                        apply = Apply(preferredUnits = preferredUnits.map { it.id }),
                        preferredStartDate = preferredStartDate,
                        careDetails = careDetails,
                    ),
            )
        }
}
