// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.serviceneed.application

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.getPlacementsForChild
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.serviceneed.getServiceNeedsByChild
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ServiceApplicationId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevFosterParent
import fi.espoo.evaka.shared.dev.DevGuardian
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevServiceApplication
import fi.espoo.evaka.shared.dev.DevServiceNeed
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.snDaycareFullDay25to35
import fi.espoo.evaka.snDaycareFullDay35
import fi.espoo.evaka.snDaycarePartDay25
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class ServiceApplicationIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var citizenController: ServiceApplicationControllerCitizen
    @Autowired lateinit var employeeController: ServiceApplicationController
    @Autowired lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val today: LocalDate = LocalDate.of(2024, 1, 16)
    private val now = HelsinkiDateTime.of(today, LocalTime.of(17, 0))
    private val clock = MockEvakaClock(now)
    private val startDate: LocalDate = today.plusMonths(2).withDayOfMonth(1)
    private val child = DevPerson(firstName = "Tupu", lastName = "Ankka")
    private val adult = DevPerson(firstName = "Minni", lastName = "Hiiri", email = "test@test.fi")
    private val area = DevCareArea()
    private val daycare =
        DevDaycare(
            areaId = area.id,
            enabledPilotFeatures = setOf(PilotFeature.SERVICE_APPLICATIONS),
        )
    private val supervisor = DevEmployee(firstName = "Hanna", lastName = "Hanhi")
    private val placement =
        DevPlacement(
            unitId = daycare.id,
            childId = child.id,
            startDate = today.minusMonths(1),
            endDate = today.plusMonths(6),
            type = PlacementType.DAYCARE,
        )
    private val serviceNeed =
        DevServiceNeed(
            placementId = placement.id,
            startDate = placement.startDate,
            endDate = placement.endDate,
            optionId = snDaycareFullDay25to35.id,
            confirmedBy = supervisor.evakaUserId,
        )

    @BeforeEach
    fun setUp() {
        db.transaction { tx ->
            tx.insertServiceNeedOptions()
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(supervisor, mapOf(daycare.id to UserRole.UNIT_SUPERVISOR))
            tx.insert(placement)
            tx.insert(serviceNeed)
        }
    }

    @Test
    fun `guardian creates, reads and deletes application`() {
        db.transaction { tx -> tx.insert(DevGuardian(guardianId = adult.id, childId = child.id)) }
        createServiceApplication(
            serviceNeedOptionId = snDaycareFullDay35.id,
            additionalInfo = "Sain uuden duunin",
        )

        val applications = getChildServiceApplicationsAsCitizen()
        assertEquals(
            listOf(
                ServiceApplicationControllerCitizen.CitizenServiceApplication(
                    data =
                        ServiceApplication(
                            id = applications.first().data.id,
                            sentAt = now,
                            personId = adult.id,
                            personName = "Minni Hiiri",
                            childId = child.id,
                            childName = "Tupu Ankka",
                            startDate = startDate,
                            serviceNeedOption =
                                ServiceNeedOptionBasics(
                                    id = snDaycareFullDay35.id,
                                    nameFi = snDaycareFullDay35.nameFi,
                                    nameSv = snDaycareFullDay35.nameSv,
                                    nameEn = snDaycareFullDay35.nameEn,
                                    validPlacementType = PlacementType.DAYCARE,
                                    partWeek = snDaycareFullDay35.partWeek,
                                    validity =
                                        DateRange(
                                            snDaycareFullDay35.validFrom,
                                            snDaycareFullDay35.validTo,
                                        ),
                                ),
                            additionalInfo = "Sain uuden duunin",
                            decision = null,
                            currentPlacement =
                                ServiceApplicationPlacement(
                                    id = placement.id,
                                    type = placement.type,
                                    endDate = placement.endDate,
                                    unitId = daycare.id,
                                ),
                        ),
                    permittedActions = setOf(Action.Citizen.ServiceApplication.DELETE),
                )
            ),
            applications,
        )

        deleteServiceApplication(applications.first().data.id)

        assertEquals(emptyList(), getChildServiceApplicationsAsCitizen())
    }

    @Test
    fun `foster parent creates, reads and deletes application`() {
        db.transaction { tx ->
            tx.insert(
                DevFosterParent(
                    childId = child.id,
                    parentId = adult.id,
                    validDuring = DateRange(today.minusMonths(1), null),
                    modifiedAt = clock.now(),
                    modifiedBy = supervisor.evakaUserId,
                )
            )
        }
        createServiceApplication(
            serviceNeedOptionId = snDaycareFullDay35.id,
            additionalInfo = "Sain uuden duunin",
        )

        val applications = getChildServiceApplicationsAsCitizen()
        assertEquals(1, applications.size)

        deleteServiceApplication(applications.first().data.id)
        assertEquals(0, getChildServiceApplicationsAsCitizen().size)
    }

    @Test
    fun `unrelated adult cannot create or read applications`() {
        assertThrows<Forbidden> {
            createServiceApplication(serviceNeedOptionId = snDaycareFullDay35.id)
        }
        assertThrows<Forbidden> { getChildServiceApplicationsAsCitizen() }
    }

    @Test
    fun `weakly authenticated user cannot create an application`() {
        assertThrows<Forbidden> {
            createServiceApplication(
                serviceNeedOptionId = snDaycareFullDay35.id,
                authLevel = CitizenAuthLevel.WEAK,
            )
        }
    }

    @Test
    fun `guardian cannot create two applications for same child at once`() {
        val child2 = DevPerson()
        db.transaction { tx ->
            tx.insert(child2, DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = adult.id, childId = child.id))
            tx.insert(DevGuardian(guardianId = adult.id, childId = child2.id))
            tx.insert(
                DevPlacement(
                    childId = child2.id,
                    unitId = daycare.id,
                    startDate = today,
                    endDate = today.plusYears(1),
                )
            )
        }
        createServiceApplication(
            serviceNeedOptionId = snDaycareFullDay35.id,
            additionalInfo = "Sain uuden duunin",
        )
        assertThrows<Conflict> {
            createServiceApplication(
                serviceNeedOptionId = snDaycareFullDay35.id,
                requestedDate = startDate.plusDays(1),
            )
        }

        // can create for another child
        createServiceApplication(serviceNeedOptionId = snDaycareFullDay35.id, childId = child2.id)
    }

    @Test
    fun `employee reads and accepts an application`() {
        MockEmailClient.clear()

        db.transaction { tx ->
            tx.insert(DevGuardian(guardianId = adult.id, childId = child.id))
            tx.insert(
                DevServiceApplication(
                    childId = child.id,
                    personId = adult.id,
                    startDate = startDate,
                    serviceNeedOptionId = snDaycareFullDay35.id,
                    additionalInfo = "Sain uuden duunin",
                    sentAt = now.minusDays(1),
                )
            )
        }

        val undecidedApplications = getUndecidedServiceApplications()
        assertEquals(
            listOf(
                UndecidedServiceApplicationSummary(
                    id = undecidedApplications.first().id,
                    childId = child.id,
                    childName = "Ankka Tupu",
                    startDate = startDate,
                    placementEndDate = placement.endDate,
                    sentAt = now.minusDays(1),
                    currentNeed = snDaycareFullDay25to35.nameFi,
                    newNeed = snDaycareFullDay35.nameFi,
                )
            ),
            undecidedApplications,
        )

        val applications = getChildServiceApplicationsAsEmployee()
        assertEquals(
            listOf(
                ServiceApplicationController.EmployeeServiceApplication(
                    data =
                        ServiceApplication(
                            id = applications.first().data.id,
                            sentAt = now.minusDays(1),
                            personId = adult.id,
                            personName = "Minni Hiiri",
                            childId = child.id,
                            childName = "Tupu Ankka",
                            startDate = startDate,
                            serviceNeedOption =
                                ServiceNeedOptionBasics(
                                    id = snDaycareFullDay35.id,
                                    nameFi = snDaycareFullDay35.nameFi,
                                    nameSv = snDaycareFullDay35.nameSv,
                                    nameEn = snDaycareFullDay35.nameEn,
                                    validPlacementType = PlacementType.DAYCARE,
                                    partWeek = snDaycareFullDay35.partWeek,
                                    validity =
                                        DateRange(
                                            snDaycareFullDay35.validFrom,
                                            snDaycareFullDay35.validTo,
                                        ),
                                ),
                            additionalInfo = "Sain uuden duunin",
                            decision = null,
                            currentPlacement =
                                ServiceApplicationPlacement(
                                    id = placement.id,
                                    type = placement.type,
                                    endDate = placement.endDate,
                                    unitId = daycare.id,
                                ),
                        ),
                    permittedActions =
                        setOf(Action.ServiceApplication.ACCEPT, Action.ServiceApplication.REJECT),
                )
            ),
            applications,
        )

        acceptServiceApplication(
            id = applications.first().data.id,
            body =
                ServiceApplicationController.AcceptServiceApplicationBody(
                    shiftCareType = ShiftCareType.NONE,
                    partWeek = false,
                ),
        )

        val decision = getChildServiceApplicationsAsEmployee().first().data.decision
        assertEquals(
            ServiceApplicationDecision(
                status = ServiceApplicationDecisionStatus.ACCEPTED,
                decidedBy = supervisor.id,
                decidedByName = "Hanna Hanhi",
                decidedAt = now,
                rejectedReason = null,
            ),
            decision,
        )

        // citizen sees the same data
        assertEquals(decision, getChildServiceApplicationsAsCitizen().first().data.decision)

        assertEquals(emptyList(), getUndecidedServiceApplications())

        // service need was updated
        val serviceNeeds = db.read { it.getServiceNeedsByChild(child.id) }.sortedBy { it.startDate }
        assertEquals(2, serviceNeeds.size)
        serviceNeeds[0].let {
            assertEquals(it.startDate, serviceNeed.startDate)
            assertEquals(it.endDate, startDate.minusDays(1))
            assertEquals(it.option.id, serviceNeed.optionId)
        }
        serviceNeeds[1].let {
            assertEquals(it.startDate, startDate)
            assertEquals(it.endDate, placement.endDate)
            assertEquals(it.option.id, snDaycareFullDay35.id)
            assertEquals(it.shiftCare, ShiftCareType.NONE)
            assertFalse(it.partWeek)
        }

        asyncJobRunner.runPendingJobsSync(clock)
        assertEquals(1, MockEmailClient.emails.size)
        val emailContent = MockEmailClient.emails.first().content
        assertEquals("Palveluntarpeen muutoshakemuksesi on käsitelty", emailContent.subject)
        assertTrue(emailContent.text.contains("palveluntarve on hyväksytty 01.03.2024 alkaen"))
    }

    @Test
    fun `employee rejects an application`() {
        MockEmailClient.clear()

        val applicationId =
            db.transaction { tx ->
                tx.insert(DevGuardian(guardianId = adult.id, childId = child.id))
                tx.insert(
                    DevServiceApplication(
                        childId = child.id,
                        personId = adult.id,
                        startDate = startDate,
                        serviceNeedOptionId = snDaycareFullDay35.id,
                        additionalInfo = "Sain uuden duunin",
                        sentAt = now.minusDays(1),
                    )
                )
            }

        rejectServiceApplication(applicationId, "Onnistuu vasta ensi kuussa")

        val decision = getChildServiceApplicationsAsEmployee().first().data.decision
        assertEquals(
            ServiceApplicationDecision(
                status = ServiceApplicationDecisionStatus.REJECTED,
                decidedBy = supervisor.id,
                decidedByName = "Hanna Hanhi",
                decidedAt = now,
                rejectedReason = "Onnistuu vasta ensi kuussa",
            ),
            decision,
        )

        // citizen sees the same data
        assertEquals(decision, getChildServiceApplicationsAsCitizen().first().data.decision)

        assertEquals(emptyList(), getUndecidedServiceApplications())

        // service need was not updated
        val serviceNeeds = db.read { it.getServiceNeedsByChild(child.id) }.sortedBy { it.startDate }
        assertEquals(1, serviceNeeds.size)
        serviceNeeds[0].let {
            assertEquals(it.startDate, serviceNeed.startDate)
            assertEquals(it.endDate, serviceNeed.endDate)
            assertEquals(it.option.id, serviceNeed.optionId)
        }

        asyncJobRunner.runPendingJobsSync(clock)
        assertEquals(1, MockEmailClient.emails.size)
        val emailContent = MockEmailClient.emails.first().content
        assertEquals("Palveluntarpeen muutoshakemuksesi on käsitelty", emailContent.subject)
        assertTrue(emailContent.text.contains("palveluntarve on hylätty"))
    }

    @Test
    fun `citizen can change from part day daycare to full day or vice versa`() {
        db.transaction { tx ->
            tx.insert(DevGuardian(guardianId = adult.id, childId = child.id))
            tx.insert(
                DevServiceApplication(
                    childId = child.id,
                    personId = adult.id,
                    startDate = startDate,
                    serviceNeedOptionId = snDaycarePartDay25.id,
                    additionalInfo = "Sain potkut",
                    sentAt = now.minusDays(1),
                )
            )
        }

        val application = getChildServiceApplicationsAsEmployee().first().data

        acceptServiceApplication(
            id = application.id,
            body =
                ServiceApplicationController.AcceptServiceApplicationBody(
                    shiftCareType = ShiftCareType.NONE,
                    partWeek = false,
                ),
        )

        // placement was updated
        val placements = db.read { it.getPlacementsForChild(child.id) }.sortedBy { it.startDate }
        assertEquals(2, placements.size)
        placements[0].let {
            assertEquals(it.startDate, placement.startDate)
            assertEquals(it.endDate, startDate.minusDays(1))
            assertEquals(it.type, placement.type)
            assertEquals(it.unitId, placement.unitId)
        }
        placements[1].let {
            assertEquals(it.startDate, startDate)
            assertEquals(it.endDate, placement.endDate)
            assertEquals(it.type, snDaycarePartDay25.validPlacementType)
            assertEquals(it.unitId, placement.unitId)
        }

        // service need was updated
        val serviceNeeds = db.read { it.getServiceNeedsByChild(child.id) }.sortedBy { it.startDate }
        assertEquals(2, serviceNeeds.size)
        serviceNeeds[0].let {
            assertEquals(it.startDate, serviceNeed.startDate)
            assertEquals(it.endDate, startDate.minusDays(1))
            assertEquals(it.option.id, serviceNeed.optionId)
        }
        serviceNeeds[1].let {
            assertEquals(it.startDate, startDate)
            assertEquals(it.endDate, placement.endDate)
            assertEquals(it.option.id, snDaycarePartDay25.id)
            assertEquals(it.shiftCare, ShiftCareType.NONE)
            assertFalse(it.partWeek)
        }
    }

    private fun createServiceApplication(
        serviceNeedOptionId: ServiceNeedOptionId,
        childId: ChildId = child.id,
        additionalInfo: String = "",
        requestedDate: LocalDate = startDate,
        authLevel: CitizenAuthLevel = CitizenAuthLevel.STRONG,
    ) =
        citizenController.createServiceApplication(
            dbInstance(),
            adult.user(authLevel),
            clock,
            ServiceApplicationControllerCitizen.ServiceApplicationCreateRequest(
                childId = childId,
                startDate = requestedDate,
                serviceNeedOptionId = serviceNeedOptionId,
                additionalInfo = additionalInfo,
            ),
        )

    private fun getChildServiceApplicationsAsCitizen() =
        citizenController.getChildServiceApplications(
            dbInstance(),
            adult.user(CitizenAuthLevel.STRONG),
            clock,
            child.id,
        )

    private fun deleteServiceApplication(id: ServiceApplicationId) =
        citizenController.deleteServiceApplication(
            dbInstance(),
            adult.user(CitizenAuthLevel.STRONG),
            clock,
            id,
        )

    private fun getChildServiceApplicationsAsEmployee() =
        employeeController.getChildServiceApplications(
            dbInstance(),
            supervisor.user,
            clock,
            child.id,
        )

    private fun getUndecidedServiceApplications() =
        employeeController.getUndecidedServiceApplications(
            dbInstance(),
            supervisor.user,
            clock,
            daycare.id,
        )

    private fun acceptServiceApplication(
        id: ServiceApplicationId,
        body: ServiceApplicationController.AcceptServiceApplicationBody,
    ) = employeeController.acceptServiceApplication(dbInstance(), supervisor.user, clock, id, body)

    private fun rejectServiceApplication(id: ServiceApplicationId, reason: String) =
        employeeController.rejectServiceApplication(
            dbInstance(),
            supervisor.user,
            clock,
            id,
            ServiceApplicationController.ServiceApplicationRejection(reason),
        )
}
