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
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.ApplicationTypeToggle
import fi.espoo.evaka.application.ChildDetails
import fi.espoo.evaka.application.Guardian
import fi.espoo.evaka.application.PersonBasics
import fi.espoo.evaka.application.Preferences
import fi.espoo.evaka.application.PreferredUnit
import fi.espoo.evaka.application.SearchApplicationRequest
import fi.espoo.evaka.application.ServiceNeed
import fi.espoo.evaka.placement.PlacementDraftUnit
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevApplicationWithForm
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertApplication
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
        val daycare2 = DevDaycare(areaId = area.id, name = "Daycare 2")
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
        assertNull(getSingleApplicationSummary().placementDraftUnit)
        assertEquals(0, getPlacementDesktopDaycare(daycare1.id).placementDrafts.size)
        assertEquals(0, getPlacementDesktopDaycare(daycare2.id).placementDrafts.size)

        // Set placement draft unit
        updateApplicationPlacementDraft(application.id, daycare1.id)
        assertEquals(
            PreferredUnit(daycare1.id, daycare1.name),
            getSingleApplicationSummary().placementDraftUnit,
        )
        assertEquals(
            listOf(
                PlacementDraft(
                    applicationId = application.id,
                    unitId = daycare1.id,
                    childId = child.id,
                    childName = "${child.lastName} ${child.firstName}",
                )
            ),
            getPlacementDesktopDaycare(daycare1.id).placementDrafts,
        )
        assertEquals(0, getPlacementDesktopDaycare(daycare2.id).placementDrafts.size)

        // Change placement draft unit
        updateApplicationPlacementDraft(application.id, daycare2.id)
        assertEquals(
            PreferredUnit(daycare2.id, daycare2.name),
            getSingleApplicationSummary().placementDraftUnit,
        )
        assertEquals(0, getPlacementDesktopDaycare(daycare1.id).placementDrafts.size)
        assertEquals(
            listOf(
                PlacementDraft(
                    applicationId = application.id,
                    unitId = daycare2.id,
                    childId = child.id,
                    childName = "${child.lastName} ${child.firstName}",
                )
            ),
            getPlacementDesktopDaycare(daycare2.id).placementDrafts,
        )
        assertEquals(
            setOf(
                PlacementDesktopDaycare(
                    id = daycare1.id,
                    name = daycare1.name,
                    placementDrafts = emptyList(),
                ),
                PlacementDesktopDaycare(
                    id = daycare2.id,
                    name = daycare2.name,
                    placementDrafts =
                        listOf(
                            PlacementDraft(
                                applicationId = application.id,
                                unitId = daycare2.id,
                                childId = child.id,
                                childName = "${child.lastName} ${child.firstName}",
                            )
                        ),
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
        updateApplicationPlacementDraft(application.id, null)
        assertNull(getSingleApplicationSummary().placementDraftUnit)
        assertEquals(0, getPlacementDesktopDaycare(daycare1.id).placementDrafts.size)
        assertEquals(0, getPlacementDesktopDaycare(daycare2.id).placementDrafts.size)
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

    private fun updateApplicationPlacementDraft(
        applicationId: ApplicationId,
        daycareId: DaycareId?,
    ) =
        placementDesktopController.updateApplicationPlacementDraft(
            dbInstance(),
            clock,
            serviceWorker.user,
            applicationId = applicationId,
            body = PlacementDesktopController.PlacementDraftUpdateRequest(unitId = daycareId),
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
            status = ApplicationStatus.WAITING_PLACEMENT,
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
