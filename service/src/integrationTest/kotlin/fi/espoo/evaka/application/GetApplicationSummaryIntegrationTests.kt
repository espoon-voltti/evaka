// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.test.validDaycareApplication
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testDecisionMaker_1
import org.jdbi.v3.core.Handle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetApplicationSummaryIntegrationTests : FullApplicationTest() {
    private val serviceWorker = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))

    @BeforeEach
    private fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            insertGeneralTestFixtures(tx.handle)

            createApplication(h = tx.handle, child = testChild_1, guardian = testAdult_1)
            createApplication(h = tx.handle, child = testChild_2, guardian = testAdult_1, attachment = true)
            createApplication(h = tx.handle, child = testChild_3, guardian = testAdult_1, urgent = true)
        }
    }

    @Test
    fun `application summary with minimal parameters returns data`() {
        val summary = getSummary(listOf("type" to "ALL", "status" to "SENT"))
        assertEquals(3, summary.total)
    }

    @Test
    fun `application summary can be be filtered by attachments`() {
        val summary = getSummary(listOf("type" to "ALL", "status" to "SENT", "basis" to "HAS_ATTACHMENTS"))
        assertEquals(1, summary.total)
        assertEquals(1, summary.data[0].attachmentCount)
    }

    @Test
    fun `application summary can be be filtered by urgency`() {
        val summary = getSummary(listOf("type" to "ALL", "status" to "SENT", "basis" to "URGENT"))
        assertEquals(1, summary.total)
        assertEquals(true, summary.data[0].urgent)
    }

    private fun getSummary(params: List<Pair<String, String>>): Paged<ApplicationSummary> {
        val (_, res, result) = http.get("/v2/applications", params)
            .asUser(serviceWorker)
            .responseObject<Paged<ApplicationSummary>>(objectMapper)
        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun createApplication(h: Handle, child: PersonData.Detailed, guardian: PersonData.Detailed, attachment: Boolean = false, urgent: Boolean = false) {
        val applicationId = insertTestApplication(h, childId = child.id, guardianId = guardian.id)

        val form = DaycareFormV0.fromApplication2(validDaycareApplication.copy(childId = child.id, guardianId = guardian.id)).copy(urgent = urgent)
        insertTestApplicationForm(h, applicationId, form)

        if (attachment) {
            uploadAttachment(applicationId, AuthenticatedUser.Citizen(guardian.id))
        }
    }
}
