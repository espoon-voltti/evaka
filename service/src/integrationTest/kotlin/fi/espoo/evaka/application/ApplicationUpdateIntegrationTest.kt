// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import com.github.kittinunf.fuel.core.extensions.jsonBody
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.test.getValidDaycareApplication
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDecisionMaker_1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ApplicationUpdateIntegrationTest : FullApplicationTest() {
    private val citizen = AuthenticatedUser.Citizen(testAdult_1.id)
    private val serviceWorker = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))

    @BeforeEach
    private fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            tx.insertGeneralTestFixtures()
        }
    }

    @Test
    fun `when application update sets urgent to false, the new due date is calculated from sent date`() {
        // given
        val sentDate = LocalDate.of(2021, 1, 1)
        val originalDueDate = LocalDate.of(2021, 1, 15)
        val application = insertSentApplication(sentDate, originalDueDate, true)

        // when
        val updatedApplication =
            application.copy(form = application.form.copy(preferences = application.form.preferences.copy(urgent = false)))
        val (_, res, _) = http.put("/v2/applications/${application.id}")
            .jsonBody(objectMapper.writeValueAsString(ApplicationUpdate(form = ApplicationFormUpdate.from(updatedApplication.form))))
            .asUser(serviceWorker)
            .responseString()

        // then
        assertEquals(204, res.statusCode)
        val result = db.transaction { it.fetchApplicationDetails(application.id) }
        assertEquals(sentDate.plusMonths(4), result?.dueDate)
    }

    @Test
    fun `when application update sets urgent to true, new due date is calculated`() {
        // given
        val sentDate = LocalDate.of(2021, 1, 1)
        val originalDueDate = LocalDate.of(2021, 5, 1)
        val application = insertSentApplication(sentDate, originalDueDate, false)

        // when
        val updatedApplication =
            application.copy(form = application.form.copy(preferences = application.form.preferences.copy(urgent = true)))
        val (_, res, _) = http.put("/v2/applications/${application.id}")
            .jsonBody(objectMapper.writeValueAsString(ApplicationUpdate(form = ApplicationFormUpdate.from(updatedApplication.form))))
            .asUser(serviceWorker)
            .responseString()

        // then
        assertEquals(204, res.statusCode)
        val beforeSendingAttachment = db.transaction { it.fetchApplicationDetails(application.id) }
        assertNull(beforeSendingAttachment?.dueDate)

        // when
        uploadAttachment(applicationId = application.id, serviceWorker)

        // then
        val afterSendingAttachment = db.transaction { it.fetchApplicationDetails(application.id) }
        assertEquals(LocalDate.now().plusWeeks(2), afterSendingAttachment?.dueDate)
    }

    @Test
    fun `when application update does not update urgent, the due date is not changed`() {
        // given
        val sentDate = LocalDate.of(2021, 1, 1)
        val originalDueDate = LocalDate.of(2021, 1, 15)
        val application = insertSentApplication(sentDate, originalDueDate, true)

        uploadAttachment(applicationId = application.id, serviceWorker)
        db.transaction { tx ->
            tx.createUpdate("UPDATE attachment SET received_at = :receivedAt WHERE application_id = :applicationId")
                .bind("receivedAt", sentDate).bind("applicationId", application.id).execute()
        }

        // when
        val (_, res, _) = http.put("/v2/applications/${application.id}")
            .jsonBody(objectMapper.writeValueAsString(ApplicationUpdate(form = ApplicationFormUpdate.from(application.form))))
            .asUser(serviceWorker)
            .responseString()

        // then
        assertEquals(204, res.statusCode)
        val result = db.transaction { it.fetchApplicationDetails(application.id) }
        assertEquals(originalDueDate, result?.dueDate)
    }

    @Test
    fun `when due date is set manually by service worker, new attachments do not re-calculate the due date`() {
        // given
        val sentDate = LocalDate.of(2021, 1, 1)
        val originalDueDate = LocalDate.of(2021, 5, 1)
        val application = insertSentApplication(sentDate, originalDueDate, false)
        val manuallySetDueDate = HelsinkiDateTime.now().plusMonths(4).toLocalDate()

        // when
        val (_, res, _) = http.put("/v2/applications/${application.id}")
            .jsonBody(objectMapper.writeValueAsString(ApplicationUpdate(form = ApplicationFormUpdate.from(application.form), dueDate = manuallySetDueDate)))
            .asUser(serviceWorker)
            .responseString()

        // then
        assertEquals(204, res.statusCode)
        val beforeSendingAttachment = db.transaction { it.fetchApplicationDetails(application.id) }
        assertEquals(manuallySetDueDate, beforeSendingAttachment!!.dueDate)
        assertTrue(HelsinkiDateTime.now().durationSince(beforeSendingAttachment.dueDateSetManuallyAt ?: throw Error("dueDateSetManuallyAt should have been set")).seconds <= 5, "dueDateSetManuallyAt should have been about now")

        // when
        uploadAttachment(applicationId = application.id, serviceWorker)

        // then
        val afterSendingAttachment = db.transaction { it.fetchApplicationDetails(application.id) }
        assertEquals(manuallySetDueDate, afterSendingAttachment?.dueDate)
    }

    @Test
    fun `when urgency is cancelled by citizen, relevant attachments are deleted`() {
        // given
        val sentDate = LocalDate.of(2021, 1, 1)
        val originalDueDate = LocalDate.of(2021, 5, 1)
        val application = insertSentApplication(sentDate, originalDueDate, true, shiftCare = true)
        uploadAttachment(applicationId = application.id, user = citizen, type = AttachmentType.URGENCY)
        uploadAttachment(applicationId = application.id, user = citizen, type = AttachmentType.EXTENDED_CARE)

        val beforeClearingUrgency = db.transaction { it.fetchApplicationDetails(application.id) }
        assertEquals(true, beforeClearingUrgency!!.form.preferences.urgent)
        assertEquals(2, beforeClearingUrgency.attachments.size)

        // when
        val updatedApplication =
            application.copy(form = application.form.copy(preferences = application.form.preferences.copy(urgent = false)))
        val (_, res, _) = http.put("/citizen/applications/${application.id}")
            .jsonBody(objectMapper.writeValueAsString(ApplicationFormUpdate.from(updatedApplication.form)))
            .asUser(citizen)
            .responseString()

        assertEquals(204, res.statusCode)

        // then
        val afterClearingUrgency = db.transaction { it.fetchApplicationDetails(application.id) }
        assertEquals(false, afterClearingUrgency!!.form.preferences.urgent)
        assertEquals(0, afterClearingUrgency.attachments.filter { it.type === AttachmentType.URGENCY }.size)
        assertEquals(1, afterClearingUrgency.attachments.filter { it.type === AttachmentType.EXTENDED_CARE }.size)
    }

    @Test
    fun `when urgency is cancelled by service worker, relevant attachments are deleted`() {
        // given
        val sentDate = LocalDate.of(2021, 1, 1)
        val originalDueDate = LocalDate.of(2021, 5, 1)
        val application = insertSentApplication(sentDate, originalDueDate, urgent = true, shiftCare = true)
        uploadAttachment(applicationId = application.id, user = serviceWorker, type = AttachmentType.URGENCY)
        uploadAttachment(applicationId = application.id, user = serviceWorker, type = AttachmentType.EXTENDED_CARE)

        val beforeClearingUrgency = db.transaction { it.fetchApplicationDetails(application.id) }
        assertEquals(true, beforeClearingUrgency!!.form.preferences.urgent)
        assertEquals(2, beforeClearingUrgency.attachments.size)

        // when
        val updatedApplication =
            application.copy(form = application.form.copy(preferences = application.form.preferences.copy(urgent = false)))
        val (_, res, _) = http.put("/v2/applications/${application.id}")
            .jsonBody(objectMapper.writeValueAsString(ApplicationUpdate(form = ApplicationFormUpdate.from(updatedApplication.form))))
            .asUser(serviceWorker)
            .responseString()

        assertEquals(204, res.statusCode)

        // then
        val afterClearingUrgency = db.transaction { it.fetchApplicationDetails(application.id) }
        assertEquals(false, afterClearingUrgency!!.form.preferences.urgent)
        assertEquals(0, afterClearingUrgency.attachments.filter { it.type === AttachmentType.URGENCY }.size)
        assertEquals(1, afterClearingUrgency.attachments.filter { it.type === AttachmentType.EXTENDED_CARE }.size)
    }

    @Test
    fun `when extended care is cancelled by citizen, relevant attachments are deleted`() {
        // given
        val sentDate = LocalDate.of(2021, 1, 1)
        val originalDueDate = LocalDate.of(2021, 5, 1)
        val application = insertSentApplication(sentDate, originalDueDate, urgent = true, shiftCare = true)
        uploadAttachment(applicationId = application.id, user = citizen, type = AttachmentType.URGENCY)
        uploadAttachment(applicationId = application.id, user = citizen, type = AttachmentType.EXTENDED_CARE)

        val beforeClearingShiftCare = db.transaction { it.fetchApplicationDetails(application.id) }
        assertEquals(true, beforeClearingShiftCare!!.form.preferences.urgent)
        assertEquals(2, beforeClearingShiftCare.attachments.size)

        // when
        val updatedApplication =
            application.copy(
                form = application.form.copy(
                    preferences = application.form.preferences.copy(serviceNeed = application.form.preferences.serviceNeed!!.copy(shiftCare = false))
                )
            )
        val (_, res, _) = http.put("/citizen/applications/${application.id}")
            .jsonBody(objectMapper.writeValueAsString(ApplicationFormUpdate.from(updatedApplication.form)))
            .asUser(citizen)
            .responseString()

        assertEquals(204, res.statusCode)

        // then
        val afterClearingShiftCare = db.transaction { it.fetchApplicationDetails(application.id) }
        assertEquals(false, afterClearingShiftCare!!.form.preferences.serviceNeed!!.shiftCare)
        assertEquals(1, afterClearingShiftCare.attachments.filter { it.type === AttachmentType.URGENCY }.size)
        assertEquals(0, afterClearingShiftCare.attachments.filter { it.type === AttachmentType.EXTENDED_CARE }.size)
    }

    @Test
    fun `when extended care is cancelled by service worker, relevant attachments are deleted`() {
        // given
        val sentDate = LocalDate.of(2021, 1, 1)
        val originalDueDate = LocalDate.of(2021, 5, 1)
        val application = insertSentApplication(sentDate, originalDueDate, urgent = true, shiftCare = true)
        uploadAttachment(applicationId = application.id, user = serviceWorker, type = AttachmentType.URGENCY)
        uploadAttachment(applicationId = application.id, user = serviceWorker, type = AttachmentType.EXTENDED_CARE)

        val beforeClearingShiftCare = db.transaction { it.fetchApplicationDetails(application.id) }
        assertEquals(true, beforeClearingShiftCare!!.form.preferences.serviceNeed!!.shiftCare)
        assertEquals(2, beforeClearingShiftCare.attachments.size)

        // when
        val updatedApplication =
            application.copy(
                form = application.form.copy(
                    preferences = application.form.preferences.copy(serviceNeed = application.form.preferences.serviceNeed!!.copy(shiftCare = false))
                )
            )
        val (_, res, _) = http.put("/v2/applications/${application.id}")
            .jsonBody(objectMapper.writeValueAsString(ApplicationUpdate(form = ApplicationFormUpdate.from(updatedApplication.form))))
            .asUser(serviceWorker)
            .responseString()

        assertEquals(204, res.statusCode)

        // then
        val afterClearingShiftCare = db.transaction { it.fetchApplicationDetails(application.id) }
        assertEquals(false, afterClearingShiftCare!!.form.preferences.serviceNeed!!.shiftCare)
        assertEquals(1, afterClearingShiftCare.attachments.filter { it.type === AttachmentType.URGENCY }.size)
        assertEquals(0, afterClearingShiftCare.attachments.filter { it.type === AttachmentType.EXTENDED_CARE }.size)
    }

    private fun insertSentApplication(
        sentDate: LocalDate,
        dueDate: LocalDate,
        urgent: Boolean,
        shiftCare: Boolean = false
    ): ApplicationDetails = db.transaction { tx ->
        val applicationId = tx.insertTestApplication(status = ApplicationStatus.SENT, sentDate = sentDate, dueDate = dueDate, childId = testChild_1.id, guardianId = testAdult_1.id)
        val validDaycareForm = DaycareFormV0.fromApplication2(getValidDaycareApplication(shiftCare = shiftCare))
        tx.insertTestApplicationForm(applicationId, validDaycareForm.copy(urgent = urgent))
        tx.fetchApplicationDetails(applicationId)!!
    }
}
