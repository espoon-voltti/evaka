// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attachments

import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.core.isSuccessful
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.attachment.AttachmentType
import fi.espoo.evaka.incomestatement.IncomeStatementBody
import fi.espoo.evaka.incomestatement.createIncomeStatement
import fi.espoo.evaka.insertApplication
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.testAdult_5
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_6
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AttachmentsControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val user = AuthenticatedUser.Citizen(testAdult_5.id, CitizenAuthLevel.STRONG)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testAdult_5, DevPersonType.ADULT)
            listOf(testChild_1, testChild_6).forEach { tx.insert(it, DevPersonType.CHILD) }
        }
    }

    @Test
    fun `Citizen can upload application attachments up to a limit`() {
        val maxAttachments = evakaEnv.maxAttachmentsPerUser
        val applicationId = ApplicationId(UUID.randomUUID())
        db.transaction {
            it.insertApplication(
                applicationId = applicationId,
                guardian = testAdult_5,
                status = ApplicationStatus.CREATED,
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
        val incomeStatementId =
            db.transaction {
                it.createIncomeStatement(
                    testAdult_5.id,
                    IncomeStatementBody.HighestFee(startDate = LocalDate.now(), endDate = null),
                )
            }
        for (i in 1..maxAttachments) {
            assertTrue(uploadIncomeStatementAttachment(incomeStatementId))
        }
        assertFalse(uploadIncomeStatementAttachment(incomeStatementId))
    }

    @Test
    fun `Citizen can upload unparented attachments up to a limit`() {
        val maxAttachments = evakaEnv.maxAttachmentsPerUser
        for (i in 1..maxAttachments) {
            assertTrue(uploadUnparentedAttachment())
        }
        assertFalse(uploadUnparentedAttachment())
    }

    private fun uploadAttachment(
        path: String,
        parameters: List<Pair<String, Any?>>? = null,
    ): Boolean {
        val (_, res, _) =
            http
                .upload(path, parameters = parameters)
                .add(FileDataPart(File(pngFile.toURI()), name = "file"))
                .asUser(user)
                .response()

        return res.isSuccessful
    }

    private fun uploadApplicationAttachment(
        applicationId: ApplicationId,
        type: AttachmentType = AttachmentType.URGENCY,
    ): Boolean {
        return uploadAttachment(
            "/attachments/citizen/applications/$applicationId",
            listOf("type" to type),
        )
    }

    private fun uploadIncomeStatementAttachment(incomeStatementId: IncomeStatementId): Boolean {
        return uploadAttachment("/attachments/citizen/income-statements/$incomeStatementId")
    }

    private fun uploadUnparentedAttachment(): Boolean {
        return uploadAttachment("/attachments/citizen/income-statements")
    }
}
