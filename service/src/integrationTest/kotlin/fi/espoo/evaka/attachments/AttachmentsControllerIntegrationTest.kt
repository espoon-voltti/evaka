// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attachments

import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.core.isSuccessful
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.ApplicationAttachmentType
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.incomestatement.IncomeStatementBody
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevIncomeStatement
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.test.getValidDaycareApplication
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AttachmentsControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
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
                    DaycareFormV0.fromApplication2(
                        getValidDaycareApplication(preferredUnit = daycare)
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

    private fun uploadAttachment(
        path: String,
        parameters: List<Pair<String, Any?>>? = null,
    ): Boolean {
        val (_, res, _) =
            http
                .upload(path, parameters = parameters)
                .add(FileDataPart(File(pngFile.toURI()), name = "file"))
                .asUser(adult.user(CitizenAuthLevel.STRONG))
                .response()

        return res.isSuccessful
    }

    private fun uploadApplicationAttachment(
        applicationId: ApplicationId,
        type: ApplicationAttachmentType = ApplicationAttachmentType.URGENCY,
    ): Boolean {
        return uploadAttachment(
            "/citizen/attachments/applications/$applicationId",
            listOf("type" to type),
        )
    }

    private fun uploadIncomeStatementAttachment(incomeStatementId: IncomeStatementId): Boolean {
        return uploadAttachment("/citizen/attachments/income-statements/$incomeStatementId")
    }

    private fun uploadUnparentedAttachment(): Boolean {
        return uploadAttachment("/citizen/attachments/income-statements")
    }
}
