// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.config.Roles
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.test.validDaycareApplication
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class GetApplicationIntegrationTests : FullApplicationTest() {
    private val serviceWorker = AuthenticatedUser(testDecisionMaker_1.id, setOf(Roles.SERVICE_WORKER))

    private val validDaycareForm = DaycareFormV0.fromApplication2(validDaycareApplication)

    @BeforeEach
    private fun beforeEach() {
        jdbi.handle { h ->
            resetDatabase(h)
            insertGeneralTestFixtures(h)
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

        val (_, res, _) = http.post("/v2/applications/clear-old-drafts")
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
}
