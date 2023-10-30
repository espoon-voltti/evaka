// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.attachment.AttachmentType
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.shared.job.ScheduledJobs
import fi.espoo.evaka.test.validDaycareApplication
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class GetApplicationIntegrationTests : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var applicationController: ApplicationControllerV2
    @Autowired lateinit var applicationControllerCitizen: ApplicationControllerCitizen
    @Autowired lateinit var stateService: ApplicationStateService
    @Autowired lateinit var scheduledJobs: ScheduledJobs

    private val serviceWorker =
        AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))
    private val citizen = AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG)
    private val testSpecialEducationTeacherId = EmployeeId(UUID.randomUUID())
    private val testSpecialEducationTeacher =
        AuthenticatedUser.Employee(testSpecialEducationTeacherId, setOf())

    private val validDaycareForm = DaycareFormV0.fromApplication2(validDaycareApplication)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insertGeneralTestFixtures() }
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
                    childId = testChild_1.id,
                    guardianId = testAdult_1.id,
                    type = ApplicationType.DAYCARE
                )
            }

        db.transaction { tx ->
            tx.insertTestApplicationForm(
                applicationId = applicationId,
                document =
                    validDaycareForm.copy(
                        apply = validDaycareForm.apply.copy(preferredUnits = listOf(testDaycare.id))
                    )
            )
        }

        val data = getApplication(applicationId)

        assertEquals(applicationId, data.application.id)
        assertEquals(testChild_1.id, data.application.childId)
        assertEquals(testAdult_1.id, data.application.guardianId)
        assertEquals(null, data.application.otherGuardianId)

        assertEquals(ApplicationType.DAYCARE, data.application.type)
        assertEquals(ApplicationStatus.SENT, data.application.status)
        assertEquals(ApplicationOrigin.ELECTRONIC, data.application.origin)

        assertEquals(validDaycareApplication.form, data.application.form)
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
                    guardianId = testAdult_1.id,
                    type = ApplicationType.DAYCARE
                )
            }

        db.transaction { tx ->
            tx.insertTestApplicationForm(
                applicationId = applicationId,
                document =
                    validDaycareForm.copy(
                        child =
                            validDaycareForm.child.copy(
                                address =
                                    fi.espoo.evaka.application.persistence.daycare.Address(
                                        street = "foo",
                                        postalCode = "00200",
                                        city = "Espoo"
                                    )
                            )
                    )
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
                    childId = testChild_1.id,
                    guardianId = guardianId,
                    type = ApplicationType.DAYCARE
                )
            }

        db.transaction { tx ->
            tx.insertTestApplicationForm(
                applicationId = applicationId,
                document =
                    validDaycareForm.copy(
                        guardian =
                            validDaycareForm.guardian.copy(
                                address =
                                    fi.espoo.evaka.application.persistence.daycare.Address(
                                        street = "foo",
                                        postalCode = "00200",
                                        city = "Espoo"
                                    )
                            )
                    )
            )
        }

        val data = getApplication(applicationId)
        assertEquals(null, data.application.form.guardian.address)
        assertEquals(true, data.application.guardianRestricted)
    }

    @Test
    fun `old drafts are removed`() {
        val (old, id1, id2) =
            db.transaction { tx ->
                listOf(
                    tx.insertTestApplication(
                        childId = testChild_1.id,
                        guardianId = testAdult_1.id,
                        status = ApplicationStatus.CREATED,
                        type = ApplicationType.DAYCARE
                    ),
                    tx.insertTestApplication(
                        childId = testChild_2.id,
                        guardianId = testAdult_1.id,
                        status = ApplicationStatus.CREATED,
                        type = ApplicationType.DAYCARE
                    ),
                    tx.insertTestApplication(
                        childId = testChild_3.id,
                        guardianId = testAdult_1.id,
                        type = ApplicationType.DAYCARE
                    )
                )
            }

        db.transaction { tx ->
            tx.createUpdate(
                    """update application set created = :createdAt where id = :applicationId"""
                )
                .bind("applicationId", old)
                .bind("createdAt", Instant.parse("2020-01-01T00:00:00Z"))
                .execute()
        }

        db.transaction { tx ->
            val data = tx.createQuery("""select id from application""").toList<ApplicationId>()

            assertEquals(3, data.size)
        }

        scheduledJobs.removeOldDraftApplications(db, RealEvakaClock())

        db.transaction { tx ->
            val data = tx.createQuery("""select id from application""").toSet<ApplicationId>()

            assertEquals(setOf(id1, id2), data)
        }
    }

    @Test
    fun `application attachments`() {
        db.transaction { tx ->
            tx.insert(DevEmployee(testSpecialEducationTeacherId))
            tx.insertDaycareAclRow(
                testDaycare.id,
                testSpecialEducationTeacherId,
                UserRole.SPECIAL_EDUCATION_TEACHER
            )
        }

        val applicationId = createPlacementProposalWithAttachments(testDaycare.id)

        // Service workers sees attachments
        val serviceWorkerResult = getApplication(applicationId, serviceWorker)
        assertEquals(2, serviceWorkerResult.attachments.size)

        // Special education teacher sees the application (because it has an assistance need) but
        // doesn't see the attachments
        val specialEducationTeacherResult =
            getApplication(applicationId, testSpecialEducationTeacher)
        assertEquals(0, specialEducationTeacherResult.attachments.size)
    }

    @Test
    fun `application attachments when service workers adds attachments, end user does not see attachments uploaded by service worker`() {
        val applicationId = createPlacementProposalWithAttachments(testDaycare.id)

        assertTrue(uploadAttachment(applicationId, serviceWorker))

        val serviceWorkerResult = getApplication(applicationId, serviceWorker)
        assertEquals(3, serviceWorkerResult.attachments.size)

        val citizenResult = getApplication(applicationId, citizen)
        assertEquals(2, citizenResult.attachments.size)
    }

    private fun getApplication(
        applicationId: ApplicationId,
        user: AuthenticatedUser.Employee = serviceWorker
    ): ApplicationResponse {
        return applicationController.getApplicationDetails(
            dbInstance(),
            user,
            RealEvakaClock(),
            applicationId
        )
    }

    private fun getApplication(
        applicationId: ApplicationId,
        user: AuthenticatedUser.Citizen
    ): ApplicationDetails {
        return applicationControllerCitizen.getApplication(
            dbInstance(),
            user,
            RealEvakaClock(),
            applicationId
        )
    }

    private fun createPlacementProposalWithAttachments(unitId: DaycareId): ApplicationId {
        val applicationId =
            db.transaction { tx ->
                val applicationId =
                    tx.insertTestApplication(
                        childId = testChild_1.id,
                        guardianId = citizen.id,
                        status = ApplicationStatus.CREATED,
                        type = ApplicationType.DAYCARE
                    )
                tx.insertTestApplicationForm(
                    applicationId = applicationId,
                    document = DaycareFormV0.fromApplication2(validDaycareApplication)
                )
                applicationId
            }
        uploadAttachment(applicationId, citizen, AttachmentType.URGENCY)
        uploadAttachment(applicationId, citizen, AttachmentType.EXTENDED_CARE)
        val today = LocalDate.of(2021, 1, 1)
        val clock = MockEvakaClock(HelsinkiDateTime.of(today, LocalTime.of(12, 0)))
        db.transaction { tx ->
            stateService.sendApplication(tx, serviceWorker, clock, applicationId)
            stateService.moveToWaitingPlacement(tx, serviceWorker, clock, applicationId)
            stateService.createPlacementPlan(
                tx,
                serviceWorker,
                applicationId,
                DaycarePlacementPlan(
                    unitId = unitId,
                    period = FiniteDateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 7, 31))
                )
            )
            stateService.sendPlacementProposal(tx, serviceWorker, clock, applicationId)
        }
        return applicationId
    }
}
