// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.shared.dev.resetDatabase
import fi.espoo.evaka.test.validDaycareApplication
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testDecisionMaker_1
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GetApplicationSummaryIntegrationTests : FullApplicationTest() {
    private val serviceWorker = AuthenticatedUser.Employee(testDecisionMaker_1.id.raw, setOf(UserRole.SERVICE_WORKER))

    @BeforeEach
    private fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            tx.insertGeneralTestFixtures()
        }
        createApplication(child = testChild_1, guardian = testAdult_1)
        createApplication(child = testChild_2, guardian = testAdult_1, extendedCare = true, attachment = true)
        createApplication(child = testChild_3, guardian = testAdult_1, urgent = true)
    }

    @Test
    fun `application summary with minimal parameters returns data`() {
        val summary = getSummary("""{"type": "ALL", "status": "SENT"}""")
        assertEquals(3, summary.total)
    }

    @Test
    fun `application summary can be be filtered by attachments`() {
        val summary = getSummary("""{"type": "ALL", "status": "SENT", "basis": "HAS_ATTACHMENTS"}""")
        assertEquals(1, summary.total)
        assertEquals(1, summary.data[0].attachmentCount)
    }

    @Test
    fun `application summary can be be filtered by urgency`() {
        val summary = getSummary("""{"type": "ALL", "status": "SENT", "basis": "URGENT"}""")
        assertEquals(1, summary.total)
        assertEquals(true, summary.data[0].urgent)
    }

    private fun getSummary(payload: String): Paged<ApplicationSummary> {
        val (_, res, result) = http.post("/v2/applications/search")
            .jsonBody(payload)
            .asUser(serviceWorker)
            .responseObject<Paged<ApplicationSummary>>(jsonMapper)
        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun createApplication(child: DevPerson, guardian: DevPerson, attachment: Boolean = false, urgent: Boolean = false, extendedCare: Boolean = false) {
        val applicationId = db.transaction { tx ->
            tx.insertTestApplication(childId = child.id, guardianId = guardian.id).also { id ->
                val form = DaycareFormV0.fromApplication2(validDaycareApplication.copy(childId = child.id, guardianId = guardian.id)).copy(urgent = urgent).copy(extendedCare = extendedCare)
                tx.insertTestApplicationForm(id, form)
            }
        }

        if (attachment) {
            uploadAttachment(applicationId, AuthenticatedUser.Citizen(guardian.id.raw, CitizenAuthLevel.STRONG))
        }
    }
}
