// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import com.github.kittinunf.fuel.core.extensions.jsonBody
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.config.Roles
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.test.validDaycareApplication
import fi.espoo.evaka.testDecisionMaker_1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ApplicationUpdateIntegrationTest : FullApplicationTest() {
    private val serviceWorker = AuthenticatedUser(testDecisionMaker_1.id, setOf(Roles.SERVICE_WORKER))

    @Test
    fun `when application update sets urgent to false, the new due date is calculated from sent date`() {
        // given
        val sentDate = LocalDate.of(2020, 1, 1)
        val originalDueDate = LocalDate.of(2020, 1, 15)
        val application = insertSentApplication(sentDate, originalDueDate, true)

        // when
        val updatedApplication =
            application.copy(form = application.form.copy(preferences = application.form.preferences.copy(urgent = false)))
        val (_, res, _) = http.put("/v2/applications/${application.id}")
            .jsonBody(objectMapper.writeValueAsString(updatedApplication.form))
            .asUser(serviceWorker)
            .responseString()

        // then
        assertEquals(204, res.statusCode)
        val result = jdbi.handle { fetchApplicationDetails(it, application.id) }
        assertEquals(sentDate.plusMonths(4), result?.dueDate)
    }

    @Test
    fun `when application update sets urgent to true, the new due date is calculated from current date`() {
        // given
        val sentDate = LocalDate.of(2020, 1, 1)
        val originalDueDate = LocalDate.of(2020, 5, 1)
        val application = insertSentApplication(sentDate, originalDueDate, false)

        // when
        val updatedApplication =
            application.copy(form = application.form.copy(preferences = application.form.preferences.copy(urgent = true)))
        val (_, res, _) = http.put("/v2/applications/${application.id}")
            .jsonBody(objectMapper.writeValueAsString(updatedApplication.form))
            .asUser(serviceWorker)
            .responseString()

        // then
        assertEquals(204, res.statusCode)
        val result = jdbi.handle { fetchApplicationDetails(it, application.id) }
        assertEquals(LocalDate.now().plusWeeks(2), result?.dueDate)
    }

    @Test
    fun `when application update does not update urgent, the due date is not changed`() {
        // given
        val sentDate = LocalDate.of(2020, 1, 1)
        val originalDueDate = LocalDate.of(2020, 1, 15)
        val application = insertSentApplication(sentDate, originalDueDate, true)

        // when
        val (_, res, _) = http.put("/v2/applications/${application.id}")
            .jsonBody(objectMapper.writeValueAsString(application.form))
            .asUser(serviceWorker)
            .responseString()

        // then
        assertEquals(204, res.statusCode)
        val result = jdbi.handle { fetchApplicationDetails(it, application.id) }
        assertEquals(originalDueDate, result?.dueDate)
    }

    private fun insertSentApplication(
        sentDate: LocalDate,
        dueDate: LocalDate,
        urgent: Boolean
    ): ApplicationDetails = jdbi.handle { h ->
        val applicationId = insertTestApplication(h, status = ApplicationStatus.SENT, sentDate = sentDate, dueDate = dueDate)
        val validDaycareForm = DaycareFormV0.fromApplication2(validDaycareApplication)
        insertTestApplicationForm(h, applicationId, validDaycareForm.copy(urgent = urgent))
        fetchApplicationDetails(h, applicationId)!!
    }
}
