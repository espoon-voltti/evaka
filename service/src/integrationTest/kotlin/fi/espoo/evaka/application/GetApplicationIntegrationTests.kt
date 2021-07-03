// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.application.utils.currentDateInFinland
import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.updateDaycareAcl
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.job.ScheduledJobs
import fi.espoo.evaka.test.validDaycareApplication
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testRoundTheClockDaycare
import fi.espoo.evaka.unitSupervisorOfTestDaycare
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class GetApplicationIntegrationTests : FullApplicationTest() {
    @Autowired
    lateinit var stateService: ApplicationStateService

    @Autowired
    lateinit var scheduledJobs: ScheduledJobs

    private val serviceWorker = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))
    private val endUser = AuthenticatedUser.Citizen(testAdult_1.id)
    private val testDaycareSupervisor = AuthenticatedUser.Employee(unitSupervisorOfTestDaycare.id, setOf())
    private val testRoundTheClockDaycareSupervisorExternalId = ExternalId.of("test", UUID.randomUUID().toString())
    private val testRoundTheClockDaycareSupervisor = AuthenticatedUser.Employee(UUID.randomUUID(), setOf())

    private val validDaycareForm = DaycareFormV0.fromApplication2(validDaycareApplication)

    @BeforeEach
    private fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            tx.insertGeneralTestFixtures()
            tx.insertTestEmployee(
                DevEmployee(
                    id = testRoundTheClockDaycareSupervisor.id,
                    externalId = testRoundTheClockDaycareSupervisorExternalId
                )
            )
            tx.updateDaycareAcl(
                testRoundTheClockDaycare.id!!,
                testRoundTheClockDaycareSupervisorExternalId,
                UserRole.UNIT_SUPERVISOR
            )
        }
    }

    @Test
    fun `application not found returns 404`() {
        val (_, res, _) = http.get("/v2/applications/${UUID.randomUUID()}").asUser(serviceWorker).response()
        assertEquals(404, res.statusCode)
    }

    @Test
    fun `application found returns 200`() {
        val applicationId = db.transaction { tx ->
            tx.insertTestApplication(childId = testChild_1.id, guardianId = testAdult_1.id)
        }

        db.transaction { tx ->
            tx.insertTestApplicationForm(
                applicationId = applicationId,
                document = validDaycareForm.copy(
                    apply = validDaycareForm.apply.copy(
                        preferredUnits = listOf(testDaycare.id)
                    )
                )
            )
        }

        val (_, res, result) = http.get("/v2/applications/$applicationId")
            .asUser(serviceWorker)
            .responseObject<ApplicationResponse>(objectMapper)

        assertEquals(200, res.statusCode)

        val data = result.get()
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
        val childId = db.transaction {
            it.insertTestPerson(
                DevPerson(
                    restrictedDetailsEnabled = true
                )
            )
        }

        val applicationId = db.transaction { tx ->
            tx.insertTestApplication(childId = childId, guardianId = testAdult_1.id)
        }

        db.transaction { tx ->
            tx.insertTestApplicationForm(
                applicationId = applicationId,
                document = validDaycareForm.copy(
                    child = validDaycareForm.child.copy(
                        address = fi.espoo.evaka.application.persistence.daycare.Address(
                            street = "foo",
                            postalCode = "00200",
                            city = "Espoo"
                        )
                    )
                )
            )
        }

        val (_, res, result) = http.get("/v2/applications/$applicationId")
            .asUser(serviceWorker)
            .responseObject<ApplicationResponse>(objectMapper)

        assertEquals(200, res.statusCode)

        val data = result.get()
        assertEquals(null, data.application.form.child.address)
        assertEquals(true, data.application.childRestricted)
    }

    @Test
    fun `restricted guardian address is hidden`() {
        val guardianId = db.transaction {
            it.insertTestPerson(
                DevPerson(
                    restrictedDetailsEnabled = true
                )
            )
        }

        val applicationId = db.transaction { tx ->
            tx.insertTestApplication(childId = testChild_1.id, guardianId = guardianId)
        }

        db.transaction { tx ->
            tx.insertTestApplicationForm(
                applicationId = applicationId,
                document = validDaycareForm.copy(
                    guardian = validDaycareForm.guardian.copy(
                        address = fi.espoo.evaka.application.persistence.daycare.Address(
                            street = "foo",
                            postalCode = "00200",
                            city = "Espoo"
                        )
                    )
                )
            )
        }

        val (_, res, result) = http.get("/v2/applications/$applicationId")
            .asUser(serviceWorker)
            .responseObject<ApplicationResponse>(objectMapper)

        assertEquals(200, res.statusCode)

        val data = result.get()
        assertEquals(null, data.application.form.guardian.address)
        assertEquals(true, data.application.guardianRestricted)
    }

    @Test
    fun `old drafts are removed`() {
        val (old, id1, id2) = db.transaction { tx ->
            listOf(
                tx.insertTestApplication(childId = testChild_1.id, guardianId = testAdult_1.id, status = ApplicationStatus.CREATED),
                tx.insertTestApplication(childId = testChild_2.id, guardianId = testAdult_1.id, status = ApplicationStatus.CREATED),
                tx.insertTestApplication(childId = testChild_3.id, guardianId = testAdult_1.id)
            )
        }

        db.transaction { tx ->
            tx.createUpdate("""update application set created = :createdAt where id = :applicationId""")
                .bind("applicationId", old)
                .bind("createdAt", Instant.parse("2020-01-01T00:00:00Z"))
                .execute()
        }

        db.transaction { tx ->
            val data =
                tx.createQuery("""select id from application""")
                    .mapTo<UUID>()
                    .toList()

            assertEquals(3, data.size)
        }

        scheduledJobs.removeOldDraftApplications(db)

        db.transaction { tx ->
            val data =
                tx.createQuery("""select id from application""")
                    .mapTo<UUID>()
                    .toSet()

            assertEquals(setOf(id1, id2), data)
        }
    }

    @Test
    fun `application attachments when placed into a regular unit`() {
        val applicationId = createPlacementProposalWithAttachments(testDaycare.id)

        val (_, _, serviceWorkerResult) = http.get("/v2/applications/$applicationId")
            .asUser(serviceWorker)
            .responseObject<ApplicationResponse>(objectMapper)
        assertEquals(2, serviceWorkerResult.get().attachments.size)

        val (_, _, unitSupervisorResult) = http.get("/v2/applications/$applicationId")
            .asUser(testDaycareSupervisor)
            .responseObject<ApplicationResponse>(objectMapper)
        assertEquals(0, unitSupervisorResult.get().attachments.size)
    }

    @Test
    fun `application attachments when placed into a round the clock unit`() {
        val applicationId = createPlacementProposalWithAttachments(testRoundTheClockDaycare.id!!)

        val (_, _, serviceWorkerResult) = http.get("/v2/applications/$applicationId")
            .asUser(serviceWorker)
            .responseObject<ApplicationResponse>(objectMapper)
        assertEquals(2, serviceWorkerResult.get().attachments.size)

        val (_, _, unitSupervisorResult) = http.get("/v2/applications/$applicationId")
            .asUser(testRoundTheClockDaycareSupervisor)
            .responseObject<ApplicationResponse>(objectMapper)
        assertEquals(1, unitSupervisorResult.get().attachments.size)
        assertEquals(AttachmentType.EXTENDED_CARE, unitSupervisorResult.get().attachments.first().type)

        val attachment = unitSupervisorResult.get().attachments.first()
        val (_, res, _) = http.get("/attachments/${attachment.id}/download")
            .asUser(testRoundTheClockDaycareSupervisor)
            .response()
        assertEquals(200, res.statusCode)
    }

    @Test
    fun `application attachments when service workers adds attachments, end user does not see attachments uploaded by service worker`() {
        val applicationId = createPlacementProposalWithAttachments(testDaycare.id)

        assertTrue(uploadAttachment(applicationId, serviceWorker))

        val (_, _, serviceWorkerResult) = http.get("/v2/applications/$applicationId")
            .asUser(serviceWorker)
            .responseObject<ApplicationResponse>(objectMapper)
        assertEquals(3, serviceWorkerResult.get().attachments.size)

        val (_, _, endUserResult) = http.get("/citizen/applications/$applicationId")
            .asUser(endUser)
            .responseObject<ApplicationDetails>(objectMapper)
        assertEquals(2, endUserResult.get().attachments.size)
    }

    private fun createPlacementProposalWithAttachments(unitId: DaycareId): UUID {
        val applicationId = db.transaction { tx ->
            val applicationId = tx.insertTestApplication(
                childId = testChild_1.id,
                guardianId = endUser.id,
                status = ApplicationStatus.CREATED
            )
            tx.insertTestApplicationForm(
                applicationId = applicationId,
                document = DaycareFormV0.fromApplication2(validDaycareApplication)
            )
            applicationId
        }
        uploadAttachment(applicationId, endUser, AttachmentType.URGENCY)
        uploadAttachment(applicationId, endUser, AttachmentType.EXTENDED_CARE)
        db.transaction { tx ->
            stateService.sendApplication(tx, serviceWorker, applicationId, currentDateInFinland())
            stateService.moveToWaitingPlacement(tx, serviceWorker, applicationId)
            stateService.createPlacementPlan(
                tx,
                serviceWorker,
                applicationId,
                DaycarePlacementPlan(
                    unitId = unitId,
                    period = FiniteDateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 7, 31))
                )
            )
            stateService.sendPlacementProposal(tx, serviceWorker, applicationId)
        }
        return applicationId
    }
}
