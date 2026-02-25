// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attachments

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.ApplicationAttachmentType
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.persistence.daycare.Adult
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.Child
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.attachment.AttachmentsController
import fi.espoo.evaka.incomestatement.IncomeStatementBody
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevIncomeStatement
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile

class AttachmentsControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var attachmentsController: AttachmentsController

    private val clock = MockEvakaClock(2020, 1, 1, 12, 0)
    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val adult = DevPerson()
    private val child1 = DevPerson()
    private val child2 = DevPerson()

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(adult, DevPersonType.ADULT)
            listOf(child1, child2).forEach { tx.insert(it, DevPersonType.CHILD) }
        }
    }

    @Test
    fun `Citizen can upload application attachments up to a limit`() {
        val maxAttachments = evakaEnv.maxAttachmentsPerUser
        val applicationId = ApplicationId(UUID.randomUUID())
        db.transaction {
            it.insertTestApplication(
                id = applicationId,
                guardianId = adult.id,
                childId = child2.id,
                status = ApplicationStatus.WAITING_DECISION,
                confidential = true,
                type = ApplicationType.DAYCARE,
                document =
                    DaycareFormV0(
                        type = ApplicationType.DAYCARE,
                        child = Child(dateOfBirth = null),
                        guardian = Adult(),
                        apply = Apply(preferredUnits = listOf(daycare.id)),
                    ),
            )
        }
        for (i in 1..maxAttachments) {
            assertTrue(uploadApplicationAttachment(applicationId))
        }
        assertFalse(uploadApplicationAttachment(applicationId))
    }

    @Test
    fun `Citizen can upload income statement attachments up to a limit`() {
        val maxAttachments = evakaEnv.maxAttachmentsPerUser
        val incomeStatement =
            DevIncomeStatement(
                personId = adult.id,
                data = IncomeStatementBody.HighestFee(startDate = LocalDate.now(), endDate = null),
            )
        db.transaction { it.insert(incomeStatement) }
        for (i in 1..maxAttachments) {
            assertTrue(uploadIncomeStatementAttachment(incomeStatement.id))
        }
        assertFalse(uploadIncomeStatementAttachment(incomeStatement.id))
    }

    @Test
    fun `Citizen can upload unparented attachments up to a limit`() {
        val maxAttachments = evakaEnv.maxAttachmentsPerUser
        for (i in 1..maxAttachments) {
            assertTrue(uploadUnparentedAttachment())
        }
        assertFalse(uploadUnparentedAttachment())
    }

    private fun mockFile() =
        MockMultipartFile("file", "evaka-logo.png", "image/png", pngFile.readBytes())

    private fun uploadApplicationAttachment(
        applicationId: ApplicationId,
        type: ApplicationAttachmentType = ApplicationAttachmentType.URGENCY,
    ): Boolean {
        return try {
            attachmentsController.uploadApplicationAttachmentCitizen(
                dbInstance(),
                adult.user(CitizenAuthLevel.STRONG),
                clock,
                applicationId,
                type,
                mockFile(),
            )
            true
        } catch (_: Forbidden) {
            false
        }
    }

    private fun uploadIncomeStatementAttachment(incomeStatementId: IncomeStatementId): Boolean {
        return try {
            attachmentsController.uploadIncomeStatementAttachmentCitizen(
                dbInstance(),
                adult.user(CitizenAuthLevel.STRONG),
                clock,
                incomeStatementId,
                null,
                mockFile(),
            )
            true
        } catch (_: Forbidden) {
            false
        }
    }

    private fun uploadUnparentedAttachment(): Boolean {
        return try {
            attachmentsController.uploadOrphanIncomeStatementAttachmentCitizen(
                dbInstance(),
                adult.user(CitizenAuthLevel.STRONG),
                clock,
                null,
                mockFile(),
            )
            true
        } catch (_: Forbidden) {
            false
        }
    }
}
