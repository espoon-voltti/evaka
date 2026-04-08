// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.application

import evaka.core.FullApplicationTest
import evaka.core.application.persistence.daycare.Apply
import evaka.core.application.persistence.daycare.CareDetails
import evaka.core.application.persistence.daycare.DaycareAdditionalDetails
import evaka.core.application.persistence.daycare.DaycareFormV0
import evaka.core.application.persistence.daycare.OtherPerson
import evaka.core.attachment.AttachmentsController
import evaka.core.attachment.uploadApplicationAttachment
import evaka.core.shared.ApplicationId
import evaka.core.shared.DaycareId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.CitizenAuthLevel
import evaka.core.shared.auth.UserRole
import evaka.core.shared.auth.insertDaycareAclRow
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevGuardian
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertTestApplication
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.domain.NotFound
import evaka.core.shared.domain.RealEvakaClock
import evaka.core.shared.job.ScheduledJobs
import evaka.core.toDaycareFormAdult
import evaka.core.toDaycareFormChild
import evaka.core.vtjclient.service.persondetails.MockPersonDetailsService
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class GetApplicationIntegrationTests : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var applicationController: ApplicationControllerV2
    @Autowired lateinit var applicationControllerCitizen: ApplicationControllerCitizen
    @Autowired lateinit var attachmentsController: AttachmentsController
    @Autowired lateinit var stateService: ApplicationStateService
    @Autowired lateinit var scheduledJobs: ScheduledJobs

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val employee = DevEmployee()
    private val specialEducationTeacher = DevEmployee()
    private val adult1 =
        DevPerson(
            ssn = "010180-1232",
            firstName = "John",
            lastName = "Doe",
            streetAddress = "Kamreerintie 2",
            postalCode = "02770",
            postOffice = "Espoo",
        )
    private val adult2 =
        DevPerson(
            ssn = "010279-123L",
            firstName = "Joan",
            lastName = "Doe",
            streetAddress = "Kamreerintie 2",
            postalCode = "02770",
            postOffice = "Espoo",
        )
    private val child1 = DevPerson(ssn = "010617A123U")
    private val child2 = DevPerson()
    private val child3 = DevPerson()

    private val serviceWorker =
        AuthenticatedUser.Employee(employee.id, setOf(UserRole.SERVICE_WORKER))
    private val citizen = AuthenticatedUser.Citizen(adult1.id, CitizenAuthLevel.STRONG)

    private val daycareForm =
        DaycareFormV0(
            type = ApplicationType.DAYCARE,
            child = child1.toDaycareFormChild(),
            guardian = adult1.toDaycareFormAdult(),
            apply = Apply(preferredUnits = listOf(daycare.id)),
            careDetails =
                CareDetails(
                    assistanceNeeded = true,
                    assistanceDescription = "assistance description",
                ),
            additionalDetails =
                DaycareAdditionalDetails(
                    allergyType = "allergies",
                    dietType = "diet",
                    otherInfo = "other info",
                ),
            preferredStartDate = LocalDate.of(2021, 8, 15),
            serviceStart = "08:00",
            serviceEnd = "17:00",
            maxFeeAccepted = false,
        )

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(employee)
            tx.insert(specialEducationTeacher)
            listOf(adult1, adult2).forEach { tx.insert(it, DevPersonType.ADULT) }
            listOf(child1, child2, child3).forEach { tx.insert(it, DevPersonType.CHILD) }
        }
        MockPersonDetailsService.addPersons(adult1, adult2, child1)
        MockPersonDetailsService.addDependants(adult1, child1)
        MockPersonDetailsService.addDependants(adult2, child1)
    }

    @Test
    fun `application not found returns 404`() {
        assertThrows<NotFound> { getApplication(ApplicationId(UUID.randomUUID())) }
    }

    @Test
    fun `application found returns 200`() {
        val applicationId =
            db.transaction { tx ->
                tx.insertTestApplication(
                    childId = child1.id,
                    guardianId = adult1.id,
                    type = ApplicationType.DAYCARE,
                    document = daycareForm,
                )
            }

        val data = getApplication(applicationId)

        assertEquals(applicationId, data.application.id)
        assertEquals(child1.id, data.application.childId)
        assertEquals(adult1.id, data.application.guardianId)
        assertEquals(emptySet(), db.read { tx -> tx.getApplicationOtherGuardians(applicationId) })

        assertEquals(ApplicationType.DAYCARE, data.application.type)
        assertEquals(ApplicationStatus.SENT, data.application.status)
        assertEquals(ApplicationOrigin.ELECTRONIC, data.application.origin)

        val expectedForm =
            ApplicationForm.fromV0(daycareForm, childRestricted = false, guardianRestricted = false)
                .let {
                    it.copy(
                        preferences =
                            it.preferences.copy(
                                preferredUnits =
                                    listOf(PreferredUnit(id = daycare.id, name = daycare.name))
                            )
                    )
                }
        assertEquals(expectedForm, data.application.form)
        assertEquals(0, data.decisions.size)
    }

    @Test
    fun `restricted child address is hidden`() {
        val childId =
            db.transaction {
                it.insert(DevPerson(restrictedDetailsEnabled = true), DevPersonType.RAW_ROW)
            }

        val applicationId =
            db.transaction { tx ->
                tx.insertTestApplication(
                    childId = childId,
                    guardianId = adult1.id,
                    type = ApplicationType.DAYCARE,
                    document =
                        daycareForm.copy(
                            child =
                                daycareForm.child.copy(
                                    address =
                                        evaka.core.application.persistence.daycare.Address(
                                            street = "foo",
                                            postalCode = "00200",
                                            city = "Espoo",
                                        )
                                )
                        ),
                )
            }

        val data = getApplication(applicationId)
        assertEquals(null, data.application.form.child.address)
        assertEquals(true, data.application.childRestricted)
    }

    @Test
    fun `restricted guardian address is hidden`() {
        val guardianId =
            db.transaction {
                it.insert(DevPerson(restrictedDetailsEnabled = true), DevPersonType.RAW_ROW)
            }

        val applicationId =
            db.transaction { tx ->
                tx.insertTestApplication(
                    childId = child1.id,
                    guardianId = guardianId,
                    type = ApplicationType.DAYCARE,
                    document =
                        daycareForm.copy(
                            guardian =
                                daycareForm.guardian.copy(
                                    address =
                                        evaka.core.application.persistence.daycare.Address(
                                            street = "foo",
                                            postalCode = "00200",
                                            city = "Espoo",
                                        )
                                )
                        ),
                )
            }

        val data = getApplication(applicationId)
        assertEquals(null, data.application.form.guardian.address)
        assertEquals(true, data.application.guardianRestricted)
    }

    @Test
    fun `old drafts are removed`() {
        val clock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2024, 1, 1), LocalTime.of(0, 0)))
        val (old, id1, id2) =
            db.transaction { tx ->
                listOf(
                    tx.insertTestApplication(
                        childId = child1.id,
                        guardianId = adult1.id,
                        status = ApplicationStatus.CREATED,
                        type = ApplicationType.DAYCARE,
                        document =
                            DaycareFormV0(
                                ApplicationType.DAYCARE,
                                child = child1.toDaycareFormChild(),
                                guardian = adult1.toDaycareFormAdult(),
                            ),
                    ),
                    tx.insertTestApplication(
                        childId = child2.id,
                        guardianId = adult1.id,
                        status = ApplicationStatus.CREATED,
                        type = ApplicationType.DAYCARE,
                        document =
                            DaycareFormV0(
                                ApplicationType.DAYCARE,
                                child = child2.toDaycareFormChild(),
                                guardian = adult1.toDaycareFormAdult(),
                            ),
                    ),
                    tx.insertTestApplication(
                        childId = child3.id,
                        guardianId = adult1.id,
                        type = ApplicationType.DAYCARE,
                        document =
                            DaycareFormV0(
                                ApplicationType.DAYCARE,
                                child = child3.toDaycareFormChild(),
                                guardian = adult1.toDaycareFormAdult(),
                            ),
                    ),
                )
            }

        db.transaction { tx ->
            tx.execute {
                sql(
                    "UPDATE application SET created_at = '2020-01-01T00:00:00Z' WHERE id = ${bind(old)}"
                )
            }
        }

        db.transaction { tx ->
            val data = tx.createQuery { sql("SELECT id FROM application") }.toList<ApplicationId>()

            assertEquals(3, data.size)
        }

        scheduledJobs.removeOldDraftApplications(db, clock)

        db.transaction { tx ->
            val data = tx.createQuery { sql("SELECT id FROM application") }.toSet<ApplicationId>()

            assertEquals(setOf(id1, id2), data)
        }
    }

    @Test
    fun `application attachments`() {
        db.transaction { tx ->
            tx.insertDaycareAclRow(
                daycare.id,
                specialEducationTeacher.id,
                UserRole.SPECIAL_EDUCATION_TEACHER,
            )
        }

        val applicationId = createPlacementProposalWithAttachments(daycare.id)

        // Service workers sees attachments
        val serviceWorkerResult = getApplication(applicationId, serviceWorker)
        assertEquals(2, serviceWorkerResult.attachments.size)

        // Special education teacher sees the application (because it has an assistance need) but
        // doesn't see the attachments
        val specialEducationTeacherResult =
            getApplication(
                applicationId,
                AuthenticatedUser.Employee(specialEducationTeacher.id, setOf()),
            )
        assertEquals(0, specialEducationTeacherResult.attachments.size)
    }

    @Test
    fun `application attachments when service workers adds attachments, end user does not see attachments uploaded by service worker`() {
        val applicationId = createPlacementProposalWithAttachments(daycare.id)

        attachmentsController.uploadApplicationAttachment(
            dbInstance(),
            applicationId,
            serviceWorker,
        )

        val serviceWorkerResult = getApplication(applicationId, serviceWorker)
        assertEquals(3, serviceWorkerResult.attachments.size)

        val citizenResult = getApplication(applicationId, citizen)
        assertEquals(2, citizenResult.attachments.size)
    }

    @Test
    fun `other guardian does not see sensitive info`() {
        val applicationId =
            db.transaction { tx ->
                tx.insertTestApplication(
                        childId = child1.id,
                        guardianId = adult1.id,
                        type = ApplicationType.DAYCARE,
                        otherGuardians = setOf(adult2.id),
                        allowOtherGuardianAccess = true,
                        document =
                            daycareForm.copy(
                                apply =
                                    daycareForm.apply.copy(
                                        siblingBasis = true,
                                        siblingSsn = "secret",
                                        siblingName = "secret",
                                        siblingUnit = "secret",
                                    ),
                                hasOtherChildren = true,
                                otherChildren =
                                    listOf(
                                        OtherPerson(
                                            firstName = "secret",
                                            lastName = "secret",
                                            socialSecurityNumber = "secret",
                                        )
                                    ),
                            ),
                    )
                    .also { tx.insert(DevGuardian(guardianId = adult2.id, childId = child1.id)) }
            }

        val guardianResult =
            getApplication(
                applicationId,
                AuthenticatedUser.Citizen(adult1.id, CitizenAuthLevel.STRONG),
            )
        assertEquals(
            listOf(PreferredUnit(id = daycare.id, name = daycare.name)),
            guardianResult.form.preferences.preferredUnits,
        )
        assertEquals(
            SiblingBasis(siblingName = "secret", siblingSsn = "secret", siblingUnit = "secret"),
            guardianResult.form.preferences.siblingBasis,
        )
        assertEquals(
            listOf(
                PersonBasics(
                    firstName = "secret",
                    lastName = "secret",
                    socialSecurityNumber = "secret",
                )
            ),
            guardianResult.form.otherChildren,
        )

        val otherGuardianResult =
            getApplication(
                applicationId,
                AuthenticatedUser.Citizen(adult2.id, CitizenAuthLevel.STRONG),
            )
        assertEquals(
            listOf(PreferredUnit(id = daycare.id, name = daycare.name)),
            otherGuardianResult.form.preferences.preferredUnits,
        )
        assertEquals(
            SiblingBasis(siblingName = "", siblingSsn = "", siblingUnit = ""),
            otherGuardianResult.form.preferences.siblingBasis,
        )
        assertEquals(emptyList(), otherGuardianResult.form.otherChildren)
    }

    private fun getApplication(
        applicationId: ApplicationId,
        user: AuthenticatedUser.Employee = serviceWorker,
    ): ApplicationResponse {
        return applicationController.getApplicationDetails(
            dbInstance(),
            user,
            RealEvakaClock(),
            applicationId,
        )
    }

    private fun getApplication(
        applicationId: ApplicationId,
        user: AuthenticatedUser.Citizen,
    ): ApplicationDetails {
        return applicationControllerCitizen.getApplication(
            dbInstance(),
            user,
            RealEvakaClock(),
            applicationId,
        )
    }

    private fun createPlacementProposalWithAttachments(unitId: DaycareId): ApplicationId {
        val applicationId =
            db.transaction { tx ->
                tx.insertTestApplication(
                    childId = child1.id,
                    guardianId = citizen.id,
                    status = ApplicationStatus.CREATED,
                    type = ApplicationType.DAYCARE,
                    document = daycareForm,
                )
            }
        attachmentsController.uploadApplicationAttachment(
            dbInstance(),
            applicationId,
            citizen,
            ApplicationAttachmentType.URGENCY,
        )
        attachmentsController.uploadApplicationAttachment(
            dbInstance(),
            applicationId,
            citizen,
            ApplicationAttachmentType.EXTENDED_CARE,
        )
        val today = LocalDate.of(2021, 1, 1)
        val clock = MockEvakaClock(HelsinkiDateTime.of(today, LocalTime.of(12, 0)))
        db.transaction { tx ->
            stateService.sendApplication(tx, serviceWorker, clock, applicationId)
            stateService.moveToWaitingPlacement(tx, serviceWorker, clock, applicationId)
            stateService.setVerified(tx, serviceWorker, clock, applicationId, null)
            stateService.createPlacementPlan(
                tx,
                serviceWorker,
                clock,
                applicationId,
                DaycarePlacementPlan(
                    unitId = unitId,
                    period = FiniteDateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 7, 31)),
                ),
            )
            stateService.sendPlacementProposal(tx, serviceWorker, clock, applicationId)
        }
        return applicationId
    }
}
