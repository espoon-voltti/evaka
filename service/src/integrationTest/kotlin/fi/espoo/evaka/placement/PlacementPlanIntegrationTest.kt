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
import fi.espoo.evaka.application.SimpleApplicationAction
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.CareDetails
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.preschoolTerm2023
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PlacementPlanId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
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
import fi.espoo.evaka.test.getApplicationStatus
import fi.espoo.evaka.test.getPlacementPlanRowByApplication
import fi.espoo.evaka.toApplicationType
import fi.espoo.evaka.toDaycareFormAdult
import fi.espoo.evaka.toDaycareFormChild
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class PlacementPlanIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var applicationController: ApplicationControllerV2

    private val employee = DevEmployee()
    private val serviceWorker =
        AuthenticatedUser.Employee(employee.id, setOf(UserRole.SERVICE_WORKER))

    private val area = DevCareArea(name = "Area", shortName = "area")
    private val daycare1 = DevDaycare(areaId = area.id, name = "Daycare 1")
    private val daycare2 = DevDaycare(areaId = area.id, name = "Daycare 2")

    // isSvebiUnit uses hard-coded area short name "svenska-bildningstjanster"
    private val areaSvebi =
        DevCareArea(name = "Svenska Bildningstjänster", shortName = "svenska-bildningstjanster")
    private val daycareSvebi =
        DevDaycare(areaId = areaSvebi.id, name = "Svebi Daycare", language = Language.sv)

    private val adult = DevPerson(ssn = "010180-1232")
    private val restrictedAdult = DevPerson(ssn = "010180-969B", restrictedDetailsEnabled = true)
    private val child = DevPerson(dateOfBirth = LocalDate.of(2017, 6, 1), ssn = "010617A123U")

    private val preschoolTerm = preschoolTerm2023

    private val clock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2020, 1, 1), LocalTime.of(15, 0)))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(area)
            tx.insert(daycare1)
            tx.insert(daycare2)
            tx.insert(areaSvebi)
            tx.insert(daycareSvebi)
            listOf(adult, restrictedAdult).forEach { tx.insert(it, DevPersonType.ADULT) }
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(preschoolTerm)
        }
        MockPersonDetailsService.addPersons(adult, restrictedAdult, child)
        MockPersonDetailsService.addDependants(adult, child)
        MockPersonDetailsService.addDependants(restrictedAdult, child)
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
            preferredStartDate = preferredStartDate,
            dueDate = LocalDate.of(2019, 5, 1),
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
                adult = restrictedAdult,
            )
        val defaultEndDate = LocalDate.of(2023, 7, 31)
        checkPlacementPlanDraft(
            applicationId,
            type = PlacementType.DAYCARE,
            preferredStartDate = preferredStartDate,
            dueDate = LocalDate.of(2019, 5, 1),
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
            preferredStartDate = preferredStartDate,
            dueDate = LocalDate.of(2019, 5, 1),
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
            preferredStartDate = preferredStartDate,
            dueDate = LocalDate.of(2019, 5, 1),
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
            preferredStartDate = preferredStartDate,
            dueDate = LocalDate.of(2019, 5, 1),
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
            preferredStartDate = preferredStartDate,
            dueDate = LocalDate.of(2019, 5, 1),
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
            preferredStartDate = preferredStartDate,
            dueDate = LocalDate.of(2019, 5, 1),
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
            preferredStartDate = preferredStartDate,
            dueDate = LocalDate.of(2019, 5, 1),
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
            listOf(setOf(UserRole.UNIT_SUPERVISOR), setOf(UserRole.FINANCE_ADMIN), setOf())
        invalidRoleLists.forEach { roles ->
            assertThrows<Forbidden> {
                applicationController.getPlacementPlanDraft(
                    dbInstance(),
                    AuthenticatedUser.Employee(employee.id, roles),
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
                    AuthenticatedUser.Employee(employee.id, roles),
                    clock,
                    applicationId,
                    proposal,
                )
            }
        }
    }

    @Test
    fun `getUnitApplications with daycare placement plan and today is before preschool application period returns unitAcceptDisabled=false`() {
        val expectedPlacementPlan =
            createApplicationAndPlacementPlanWaitingUnitConfirmation(
                PlacementType.DAYCARE,
                daycare1.id,
                preschoolTerm.finnishPreschool,
            )

        val unitApplications =
            applicationController.getUnitApplications(
                dbInstance(),
                serviceWorker,
                clock =
                    MockEvakaClock(
                        HelsinkiDateTime.of(
                            preschoolTerm.applicationPeriod.start.minusDays(1),
                            LocalTime.of(8, 0),
                        )
                    ),
                daycare1.id,
            )

        assertEquals(
            listOf(expectedPlacementPlan.copy(unitAcceptDisabled = false)),
            unitApplications.placementProposals,
        )
    }

    @Test
    fun `getUnitApplications with daycare placement plan and today is during preschool application period returns unitAcceptDisabled=false`() {
        val expectedPlacementPlan =
            createApplicationAndPlacementPlanWaitingUnitConfirmation(
                PlacementType.DAYCARE,
                daycare1.id,
                preschoolTerm.finnishPreschool,
            )

        val unitApplications =
            applicationController.getUnitApplications(
                dbInstance(),
                serviceWorker,
                clock =
                    MockEvakaClock(
                        HelsinkiDateTime.of(
                            preschoolTerm.applicationPeriod.start,
                            LocalTime.of(8, 0),
                        )
                    ),
                daycare1.id,
            )

        assertEquals(
            listOf(expectedPlacementPlan.copy(unitAcceptDisabled = false)),
            unitApplications.placementProposals,
        )
    }

    @Test
    fun `getUnitApplications with daycare placement plan and today is after preschool application period returns unitAcceptDisabled=false`() {
        val expectedPlacementPlan =
            createApplicationAndPlacementPlanWaitingUnitConfirmation(
                PlacementType.DAYCARE,
                daycare1.id,
                preschoolTerm.finnishPreschool,
            )

        val unitApplications =
            applicationController.getUnitApplications(
                dbInstance(),
                serviceWorker,
                clock =
                    MockEvakaClock(
                        HelsinkiDateTime.of(
                            preschoolTerm.applicationPeriod.start.plusDays(1),
                            LocalTime.of(8, 0),
                        )
                    ),
                daycare1.id,
            )

        assertEquals(
            listOf(expectedPlacementPlan.copy(unitAcceptDisabled = false)),
            unitApplications.placementProposals,
        )
    }

    @Test
    fun `getUnitApplications with preschool placement plan and today is before preschool application period returns unitAcceptDisabled=true`() {
        val expectedPlacementPlan =
            createApplicationAndPlacementPlanWaitingUnitConfirmation(
                PlacementType.PRESCHOOL,
                daycare1.id,
                preschoolTerm.finnishPreschool,
            )

        val unitApplications =
            applicationController.getUnitApplications(
                dbInstance(),
                serviceWorker,
                clock =
                    MockEvakaClock(
                        HelsinkiDateTime.of(
                            preschoolTerm.applicationPeriod.start.minusDays(1),
                            LocalTime.of(8, 0),
                        )
                    ),
                daycare1.id,
            )

        assertEquals(
            listOf(expectedPlacementPlan.copy(unitAcceptDisabled = true)),
            unitApplications.placementProposals,
        )
    }

    @Test
    fun `getUnitApplications with preschool placement plan and today is during preschool application period returns unitAcceptDisabled=false`() {
        val expectedPlacementPlan =
            createApplicationAndPlacementPlanWaitingUnitConfirmation(
                PlacementType.PRESCHOOL,
                daycare1.id,
                preschoolTerm.finnishPreschool,
            )

        val unitApplications =
            applicationController.getUnitApplications(
                dbInstance(),
                serviceWorker,
                clock =
                    MockEvakaClock(
                        HelsinkiDateTime.of(
                            preschoolTerm.applicationPeriod.start,
                            LocalTime.of(8, 0),
                        )
                    ),
                daycare1.id,
            )

        assertEquals(
            listOf(expectedPlacementPlan.copy(unitAcceptDisabled = false)),
            unitApplications.placementProposals,
        )
    }

    @Test
    fun `getUnitApplications with preschool placement plan and today is after preschool application period returns unitAcceptDisabled=false`() {
        val expectedPlacementPlan =
            createApplicationAndPlacementPlanWaitingUnitConfirmation(
                PlacementType.PRESCHOOL,
                daycare1.id,
                preschoolTerm.finnishPreschool,
            )

        val unitApplications =
            applicationController.getUnitApplications(
                dbInstance(),
                serviceWorker,
                clock =
                    MockEvakaClock(
                        HelsinkiDateTime.of(
                            preschoolTerm.applicationPeriod.start.plusDays(1),
                            LocalTime.of(8, 0),
                        )
                    ),
                daycare1.id,
            )

        assertEquals(
            listOf(expectedPlacementPlan.copy(unitAcceptDisabled = false)),
            unitApplications.placementProposals,
        )
    }

    @Test
    fun `getUnitApplications with preschool placement plan without preschool term returns unitAcceptDisabled=false`() {
        val expectedPlacementPlan =
            createApplicationAndPlacementPlanWaitingUnitConfirmation(
                PlacementType.PRESCHOOL,
                daycare1.id,
                FiniteDateRange(LocalDate.of(2020, 8, 11), LocalDate.of(2021, 6, 3)),
            )

        val unitApplications =
            applicationController.getUnitApplications(
                dbInstance(),
                serviceWorker,
                clock =
                    MockEvakaClock(
                        HelsinkiDateTime.of(LocalDate.of(2020, 8, 10), LocalTime.of(8, 0))
                    ),
                daycare1.id,
            )

        assertEquals(
            listOf(expectedPlacementPlan.copy(unitAcceptDisabled = false)),
            unitApplications.placementProposals,
        )
    }

    private fun checkPlacementPlanDraft(
        applicationId: ApplicationId,
        type: PlacementType,
        child: DevPerson = this.child,
        preferredStartDate: LocalDate,
        dueDate: LocalDate? = null,
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
                preferredStartDate = preferredStartDate,
                dueDate = dueDate,
                preferredUnits =
                    preferredUnits.map { PlacementDraftUnit(id = it.id, name = it.name) },
                placementDraft = null,
                period = period,
                preschoolDaycarePeriod = preschoolDaycarePeriod,
                placements = placements,
                guardianHasRestrictedDetails = guardianHasRestrictedDetails,
            ),
            result,
        )
    }

    private fun createApplicationAndPlacementPlanWaitingUnitConfirmation(
        placementType: PlacementType,
        unitId: DaycareId,
        placementPlanPeriod: FiniteDateRange,
    ): PlacementPlanDetails {
        val applicationId =
            insertInitialData(
                status = ApplicationStatus.WAITING_PLACEMENT,
                type = placementType.toApplicationType(),
                preferredStartDate = placementPlanPeriod.start,
            )
        val placementPlanId =
            createPlacementPlanAndAssert(
                applicationId,
                placementType,
                DaycarePlacementPlan(unitId = unitId, period = placementPlanPeriod),
            )
        applicationController.simpleApplicationAction(
            dbInstance(),
            serviceWorker,
            clock,
            applicationId,
            SimpleApplicationAction.SEND_PLACEMENT_PROPOSAL,
        )
        return PlacementPlanDetails(
            id = placementPlanId,
            unitId = unitId,
            applicationId = applicationId,
            type = placementType,
            period = placementPlanPeriod,
            preschoolDaycarePeriod = null,
            child =
                PlacementPlanChild(
                    id = child.id,
                    firstName = child.firstName,
                    lastName = child.lastName,
                    dateOfBirth = child.dateOfBirth,
                ),
            unitConfirmationStatus = PlacementPlanConfirmationStatus.PENDING,
            unitAcceptDisabled = false,
            unitRejectReason = null,
            unitRejectOtherReason = null,
            rejectedByCitizen = false,
        )
    }

    private fun createPlacementPlanAndAssert(
        applicationId: ApplicationId,
        type: PlacementType,
        proposal: DaycarePlacementPlan,
    ): PlacementPlanId {
        applicationController.setApplicationVerified(
            dbInstance(),
            serviceWorker,
            clock,
            applicationId,
            false,
        )
        applicationController.createPlacementPlan(
            dbInstance(),
            serviceWorker,
            clock,
            applicationId,
            proposal,
        )

        return db.read { r ->
            val placementPlanTableRow =
                r.getPlacementPlanRowByApplication(applicationId).also {
                    assertEquals(type, it.type)
                    assertEquals(proposal.unitId, it.unitId)
                    assertEquals(proposal.period, it.period())
                    assertEquals(proposal.preschoolDaycarePeriod, it.preschoolDaycarePeriod())
                    assertEquals(false, it.deleted)
                }
            assertEquals(ApplicationStatus.WAITING_DECISION, r.getApplicationStatus(applicationId))
            placementPlanTableRow.id
        }
    }

    private fun insertInitialData(
        status: ApplicationStatus,
        type: ApplicationType,
        preferredStartDate: LocalDate,
        adult: DevPerson = this.adult,
        child: DevPerson = this.child,
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
