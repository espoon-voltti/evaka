// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.updateDaycareAcl
import fi.espoo.evaka.shared.domain.FiniteDateRange
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class GetApplicationIntegrationTests : FullApplicationTest() {
    @Autowired
    lateinit var stateService: ApplicationStateService

    private val serviceWorker = AuthenticatedUser(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))
    private val enduser = AuthenticatedUser(testAdult_1.id, setOf(UserRole.END_USER))
    private val testDaycareSupervisor = AuthenticatedUser(unitSupervisorOfTestDaycare.id, setOf())
    private val testRoundTheClockDaycareSupervisorExternalId = ExternalId.of("test", UUID.randomUUID().toString())
    private val testRoundTheClockDaycareSupervisor = AuthenticatedUser(UUID.randomUUID(), setOf())

    private val validDaycareForm = DaycareFormV0.fromApplication2(validDaycareApplication)

    @BeforeEach
    private fun beforeEach() {
        jdbi.handle { h ->
            resetDatabase(h)
            insertGeneralTestFixtures(h)
            h.insertTestEmployee(
                DevEmployee(
                    id = testRoundTheClockDaycareSupervisor.id,
                    externalId = testRoundTheClockDaycareSupervisorExternalId
                )
            )
            updateDaycareAcl(
                h,
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
        val applicationId = jdbi.handle {
            h ->
            insertTestApplication(h = h, childId = testChild_1.id, guardianId = testAdult_1.id)
        }

        jdbi.handle { h ->
            insertTestApplicationForm(
                h = h,
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
        val childId = jdbi.handle {
            it.insertTestPerson(
                DevPerson(
                    restrictedDetailsEnabled = true
                )
            )
        }

        val applicationId = jdbi.handle { h ->
            insertTestApplication(h = h, childId = childId, guardianId = testAdult_1.id)
        }

        jdbi.handle { h ->
            insertTestApplicationForm(
                h = h,
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
        val guardianId = jdbi.handle {
            it.insertTestPerson(
                DevPerson(
                    restrictedDetailsEnabled = true
                )
            )
        }

        val applicationId = jdbi.handle { h ->
            insertTestApplication(h = h, childId = testChild_1.id, guardianId = guardianId)
        }

        jdbi.handle { h ->
            insertTestApplicationForm(
                h = h,
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
        val old = jdbi.handle { h ->
            insertTestApplication(h = h, childId = testChild_1.id, status = ApplicationStatus.CREATED)
        }

        val id1 = jdbi.handle { h ->
            insertTestApplication(h = h, childId = testChild_2.id, status = ApplicationStatus.CREATED)
        }

        val id2 = jdbi.handle { h ->
            insertTestApplication(h = h, childId = testChild_3.id)
        }

        jdbi.handle { h ->
            h.createUpdate("""update application set created = :createdAt where id = :applicationId""")
                .bind("applicationId", old)
                .bind("createdAt", Instant.parse("2020-01-01T00:00:00Z"))
                .execute()
        }

        jdbi.handle { h ->
            val data =
                h.createQuery("""select id from application""")
                    .mapTo<UUID>()
                    .toList()

            assertEquals(3, data.size)
        }

        val (_, res, _) = http.post("/scheduled/application/clear-old-drafts")
            .asUser(serviceWorker)
            .responseObject<ApplicationResponse>(objectMapper)

        assertEquals(204, res.statusCode)

        jdbi.handle { h ->
            val data =
                h.createQuery("""select id from application""")
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

    private fun createPlacementProposalWithAttachments(unitId: UUID): UUID {
        val applicationId = db.transaction { tx ->
            val applicationId = insertTestApplication(
                tx.handle,
                childId = testChild_1.id,
                guardianId = enduser.id,
                status = ApplicationStatus.CREATED
            )
            insertTestApplicationForm(
                h = tx.handle,
                applicationId = applicationId,
                document = DaycareFormV0.fromApplication2(validDaycareApplication)
            )
            applicationId
        }
        uploadAttachment(applicationId, enduser, AttachmentType.URGENCY)
        uploadAttachment(applicationId, enduser, AttachmentType.EXTENDED_CARE)
        db.transaction { tx ->
            stateService.sendApplication(tx, serviceWorker, applicationId)
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
