// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application.placementdesktop

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.Address
import fi.espoo.evaka.application.ApplicationControllerV2
import fi.espoo.evaka.application.ApplicationForm
import fi.espoo.evaka.application.ApplicationOrigin
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationStatusOption
import fi.espoo.evaka.application.ApplicationSummaryPlacementDraft
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.ApplicationTypeToggle
import fi.espoo.evaka.application.ChildDetails
import fi.espoo.evaka.application.Guardian
import fi.espoo.evaka.application.PersonBasics
import fi.espoo.evaka.application.Preferences
import fi.espoo.evaka.application.PreferredUnit
import fi.espoo.evaka.application.SearchApplicationRequest
import fi.espoo.evaka.application.ServiceNeed
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.placement.PlacementDraftUnit
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevApplicationWithForm
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareCaretaker
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevPlacementPlan
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertApplication
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.springframework.beans.factory.annotation.Autowired

class PlacementDesktopIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var placementDesktopController: PlacementDesktopController
    @Autowired private lateinit var applicationController: ApplicationControllerV2

    private val clock = MockEvakaClock(2020, 1, 1, 12, 0)

    val serviceWorker = DevEmployee(roles = setOf(UserRole.SERVICE_WORKER))

    @Test
    fun `service worker updates placement draft unit`() {
        val area = DevCareArea()
        val daycare1 = DevDaycare(areaId = area.id, name = "Daycare 1")
        val daycare2 =
            DevDaycare(areaId = area.id, name = "Daycare 2", serviceWorkerNote = "Väistössä")
        val guardian = DevPerson()
        val child = DevPerson()
        val application =
            createTestApplication(
                child = child,
                guardian = guardian,
                preferredUnits = listOf(daycare1),
            )
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare1)
            tx.insert(daycare2)
            tx.insert(serviceWorker)
            tx.insert(guardian, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.RAW_ROW)
            tx.insertApplication(application)
        }

        // Verify initial state
        assertNull(getSingleApplicationSummary().placementDraft)
        assertEquals(0, getPlacementDesktopDaycare(daycare1.id).placementDrafts.size)
        assertEquals(0, getPlacementDesktopDaycare(daycare2.id).placementDrafts.size)

        // Set placement draft unit
        upsertApplicationPlacementDraft(application.id, daycare1.id)
        assertEquals(
            ApplicationSummaryPlacementDraft(
                PreferredUnit(id = daycare1.id, name = daycare1.name),
                application.form.preferences.preferredStartDate!!,
            ),
            getSingleApplicationSummary().placementDraft,
        )
        assertEquals(
            listOf(
                PlacementDraft(
                    applicationId = application.id,
                    unitId = daycare1.id,
                    startDate = application.form.preferences.preferredStartDate!!,
                    childId = child.id,
                    childName = "${child.lastName} ${child.firstName}",
                    modifiedAt = clock.now(),
                    modifiedBy = serviceWorker.evakaUser,
                    serviceWorkerNote = "",
                )
            ),
            getPlacementDesktopDaycare(daycare1.id).placementDrafts,
        )
        assertEquals(0, getPlacementDesktopDaycare(daycare2.id).placementDrafts.size)

        // Change placement draft unit and update placement draft start date
        val newStartDate = clock.today().plusMonths(3)
        upsertApplicationPlacementDraft(
            applicationId = application.id,
            daycareId = daycare2.id,
            startDate = newStartDate,
        )
        assertEquals(
            ApplicationSummaryPlacementDraft(
                PreferredUnit(daycare2.id, daycare2.name),
                newStartDate,
            ),
            getSingleApplicationSummary().placementDraft,
        )
        assertEquals(0, getPlacementDesktopDaycare(daycare1.id).placementDrafts.size)
        assertEquals(
            listOf(
                PlacementDraft(
                    applicationId = application.id,
                    unitId = daycare2.id,
                    startDate = newStartDate,
                    childId = child.id,
                    childName = "${child.lastName} ${child.firstName}",
                    modifiedAt = clock.now(),
                    modifiedBy = serviceWorker.evakaUser,
                    serviceWorkerNote = "",
                )
            ),
            getPlacementDesktopDaycare(daycare2.id).placementDrafts,
        )
        assertEquals(
            setOf(
                PlacementDesktopDaycare(
                    id = daycare1.id,
                    name = daycare1.name,
                    serviceWorkerNote = daycare1.serviceWorkerNote,
                    placementDrafts = emptyList(),
                    occupancyConfirmed = null,
                    occupancyPlanned = null,
                    occupancyDraft = null,
                ),
                PlacementDesktopDaycare(
                    id = daycare2.id,
                    name = daycare2.name,
                    serviceWorkerNote = daycare2.serviceWorkerNote,
                    placementDrafts =
                        listOf(
                            PlacementDraft(
                                applicationId = application.id,
                                unitId = daycare2.id,
                                startDate = newStartDate,
                                childId = child.id,
                                childName = "${child.lastName} ${child.firstName}",
                                modifiedAt = clock.now(),
                                modifiedBy = serviceWorker.evakaUser,
                                serviceWorkerNote = "",
                            )
                        ),
                    occupancyConfirmed = null,
                    occupancyPlanned = null,
                    occupancyDraft = null,
                ),
            ),
            getPlacementDesktopDaycares(setOf(daycare1.id, daycare2.id)).toSet(),
        )

        // Placement plan draft shows the draft placement unit
        val placementPlanDraft = getPlacementPlanDraft(application.id)
        assertEquals(
            PlacementDraftUnit(daycare2.id, daycare2.name),
            placementPlanDraft.placementDraftUnit,
        )

        // Remove placement draft unit
        deleteApplicationPlacementDraft(application.id)
        assertNull(getSingleApplicationSummary().placementDraft)
        assertEquals(0, getPlacementDesktopDaycare(daycare1.id).placementDrafts.size)
        assertEquals(0, getPlacementDesktopDaycare(daycare2.id).placementDrafts.size)
    }

    @Test
    fun `occupancies are calculated`() {
        val area = DevCareArea()
        val daycare1 = DevDaycare(areaId = area.id, name = "Daycare 1")
        val group1 = DevDaycareGroup(daycareId = daycare1.id)
        val group1Caretakers =
            DevDaycareCaretaker(groupId = group1.id, amount = BigDecimal.valueOf(3.0))
        val daycare2 = DevDaycare(areaId = area.id, name = "Daycare 2")
        val group2 = DevDaycareGroup(daycareId = daycare2.id)
        val group2Caretakers =
            DevDaycareCaretaker(groupId = group2.id, amount = BigDecimal.valueOf(3.0))
        val guardian = DevPerson()

        val placedChild1 = DevPerson()
        val placement1 =
            DevPlacement(
                childId = placedChild1.id,
                unitId = daycare1.id,
                startDate = clock.today().minusDays(10),
                endDate = clock.today().plusYears(1),
                type = PlacementType.DAYCARE,
            )

        val placedChild2 = DevPerson()
        val placement2 =
            DevPlacement(
                childId = placedChild2.id,
                unitId = daycare2.id,
                startDate = clock.today().minusDays(10),
                endDate = clock.today().plusYears(1),
                type = PlacementType.DAYCARE_PART_TIME,
            )

        val plannedChild = DevPerson()
        val application1 =
            createTestApplication(
                child = plannedChild,
                guardian = guardian,
                preferredUnits = listOf(daycare1),
                status = ApplicationStatus.WAITING_DECISION,
            )
        val placementPlan =
            DevPlacementPlan(
                applicationId = application1.id,
                unitId = daycare1.id,
                startDate = clock.today().plusDays(10),
                endDate = clock.today().plusYears(1),
            )

        val draftPlacedChild = DevPerson()
        val application2 =
            createTestApplication(
                child = draftPlacedChild,
                guardian = guardian,
                preferredUnits = listOf(daycare1),
            )

        db.transaction { tx ->
            tx.insertServiceNeedOptions()
            tx.insert(area)
            tx.insert(daycare1)
            tx.insert(group1)
            tx.insert(group1Caretakers)
            tx.insert(daycare2)
            tx.insert(group2)
            tx.insert(group2Caretakers)
            tx.insert(serviceWorker)
            tx.insert(guardian, DevPersonType.ADULT)
            tx.insert(placedChild1, DevPersonType.CHILD)
            tx.insert(placement1)
            tx.insert(placedChild2, DevPersonType.CHILD)
            tx.insert(placement2)
            tx.insert(plannedChild, DevPersonType.RAW_ROW)
            tx.insertApplication(application1)
            tx.insert(placementPlan)
            tx.insert(draftPlacedChild, DevPersonType.RAW_ROW)
            tx.insertApplication(application2)
        }
        upsertApplicationPlacementDraft(applicationId = application2.id, daycareId = daycare1.id)

        val result1 = getPlacementDesktopDaycare(daycare1.id)
        assertEquals(4.8, result1.occupancyConfirmed?.max?.percentage)
        assertEquals(9.5, result1.occupancyPlanned?.max?.percentage)
        assertEquals(14.3, result1.occupancyDraft?.max?.percentage)

        val result2 = getPlacementDesktopDaycare(daycare2.id)
        assertEquals(2.6, result2.occupancyConfirmed?.max?.percentage)
        assertEquals(2.6, result2.occupancyPlanned?.max?.percentage)
        assertEquals(2.6, result2.occupancyDraft?.max?.percentage)

        getPlacementDesktopDaycares(setOf(daycare1.id, daycare2.id)).also {
            assertTrue(it.contains(result1))
            assertTrue(it.contains(result2))
        }
    }

    private fun getAllApplicationSummaries() =
        applicationController.getApplicationSummaries(
            dbInstance(),
            serviceWorker.user,
            clock,
            SearchApplicationRequest(
                page = null,
                sortBy = null,
                sortDir = null,
                areas = null,
                units = null,
                basis = null,
                type = ApplicationTypeToggle.ALL,
                preschoolType = null,
                statuses = listOf(ApplicationStatusOption.WAITING_PLACEMENT),
                dateType = null,
                distinctions = null,
                periodStart = null,
                periodEnd = null,
                searchTerms = null,
                transferApplications = null,
                voucherApplications = null,
            ),
        )

    private fun getSingleApplicationSummary() =
        getAllApplicationSummaries().also { assertEquals(1, it.total) }.data.first()

    private fun getPlacementDesktopDaycare(daycareId: DaycareId) =
        placementDesktopController.getPlacementDesktopDaycare(
            dbInstance(),
            clock,
            serviceWorker.user,
            daycareId,
        )

    private fun getPlacementDesktopDaycares(daycareIds: Set<DaycareId>) =
        placementDesktopController.getPlacementDesktopDaycares(
            dbInstance(),
            clock,
            serviceWorker.user,
            daycareIds,
        )

    private fun upsertApplicationPlacementDraft(
        applicationId: ApplicationId,
        daycareId: DaycareId,
        startDate: LocalDate? = null,
    ) =
        placementDesktopController.upsertApplicationPlacementDraft(
            dbInstance(),
            clock,
            serviceWorker.user,
            applicationId = applicationId,
            body =
                PlacementDesktopController.PlacementDraftUpdateRequest(
                    unitId = daycareId,
                    startDate = startDate,
                ),
        )

    private fun deleteApplicationPlacementDraft(applicationId: ApplicationId) =
        placementDesktopController.deleteApplicationPlacementDraft(
            dbInstance(),
            clock,
            serviceWorker.user,
            applicationId = applicationId,
        )

    private fun getPlacementPlanDraft(applicationId: ApplicationId) =
        applicationController.getPlacementPlanDraft(
            dbInstance(),
            serviceWorker.user,
            clock,
            applicationId,
        )

    private fun createTestApplication(
        child: DevPerson,
        guardian: DevPerson,
        preferredUnits: List<DevDaycare>,
        status: ApplicationStatus = ApplicationStatus.WAITING_PLACEMENT,
        transferApplication: Boolean = false,
        assistanceNeed: String = "",
        otherInfo: String = "",
        startInDays: Long = 50,
        partTime: Boolean = false,
        startTime: String = "08:00",
        endTime: String = "16:00",
    ) =
        DevApplicationWithForm(
            id = ApplicationId(UUID.randomUUID()),
            type = ApplicationType.DAYCARE,
            createdAt = clock.now().minusDays(55),
            createdBy = guardian.evakaUserId(),
            modifiedAt = clock.now().minusDays(55),
            modifiedBy = guardian.evakaUserId(),
            sentDate = clock.today().minusDays(55),
            dueDate = clock.today().plusDays(18),
            status = status,
            guardianId = guardian.id,
            childId = child.id,
            origin = ApplicationOrigin.ELECTRONIC,
            checkedByAdmin = true,
            confidential = true,
            hideFromGuardian = false,
            transferApplication = transferApplication,
            otherGuardians = emptyList(),
            form =
                ApplicationForm(
                    child =
                        ChildDetails(
                            person =
                                PersonBasics(
                                    child.firstName,
                                    child.lastName,
                                    socialSecurityNumber = null,
                                ),
                            dateOfBirth = child.dateOfBirth,
                            address = Address("Testikatu 1", "00200", "Espoo"),
                            futureAddress = null,
                            nationality = "fi",
                            language = "fi",
                            allergies = "",
                            diet = "",
                            assistanceNeeded = assistanceNeed.isNotBlank(),
                            assistanceDescription = assistanceNeed,
                        ),
                    guardian =
                        Guardian(
                            person =
                                PersonBasics(
                                    guardian.firstName,
                                    guardian.lastName,
                                    socialSecurityNumber = null,
                                ),
                            address = Address("Testikatu 1", "00200", "Espoo"),
                            futureAddress = null,
                            phoneNumber = "+358 50 1234567",
                            email = "testitesti@gmail.com",
                        ),
                    secondGuardian = null,
                    otherPartner = null,
                    otherChildren = emptyList(),
                    preferences =
                        Preferences(
                            preferredUnits = preferredUnits.map { PreferredUnit(it.id, it.name) },
                            preferredStartDate = clock.today().plusDays(startInDays),
                            connectedDaycarePreferredStartDate = null,
                            serviceNeed =
                                ServiceNeed(
                                    startTime = startTime,
                                    endTime = endTime,
                                    shiftCare = false,
                                    partTime = partTime,
                                    serviceNeedOption = null,
                                ),
                            siblingBasis = null,
                            preparatory = false,
                            urgent = false,
                        ),
                    maxFeeAccepted = true,
                    otherInfo = otherInfo,
                    clubDetails = null,
                ),
        )
}
