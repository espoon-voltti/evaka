// SPDX-FileCopyrightText: 2026 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.vesilahti.export

import evaka.core.document.DocumentTemplateContent
import evaka.core.document.Question.TextQuestion
import evaka.core.document.Section
import evaka.core.document.childdocument.AnsweredQuestion
import evaka.core.document.childdocument.DocumentContent
import evaka.core.document.childdocument.DocumentStatus
import evaka.core.shared.dev.DevChildDocument
import evaka.core.shared.dev.DevChildDocumentPublishedVersion
import evaka.core.shared.dev.DevDocumentTemplate
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.sftp.SftpClient
import evaka.instance.vesilahti.AbstractVesilahtiIntegrationTest
import evaka.trevaka.export.ChildDocumentTransferType
import evaka.trevaka.export.exportChildDocumentsViaSftp
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private val timestamp = HelsinkiDateTime.of(LocalDate.of(2025, 8, 1), LocalTime.of(5, 1))
private val clock = MockEvakaClock(timestamp)

class ExportDaycareToPreschoolChildDocumentsTest : AbstractVesilahtiIntegrationTest() {

    @Test
    fun `no template throws`() {
        val primus = properties.primus ?: error("Primus not configured")
        assertThrows<IllegalStateException> {
            exportChildDocumentsViaSftp(
                db,
                clock,
                "922",
                primus,
                ChildDocumentTransferType.DAYCARE_TO_PRESCHOOL,
            )
        }
    }

    @Test
    fun `template without documents exports empty array`() {
        db.transaction { tx ->
            tx.insert(
                DevDocumentTemplate(
                    name = "Tiedonsiirto varhaiskasvatuksesta esiopetukseen 2024-2025",
                    validity = DateRange(LocalDate.of(2024, 8, 1), LocalDate.of(2025, 7, 31)),
                    content = DocumentTemplateContent(sections = emptyList()),
                )
            )
        }

        val primus = properties.primus ?: error("Primus not configured")
        exportChildDocumentsViaSftp(
            db,
            clock,
            "922",
            primus,
            ChildDocumentTransferType.DAYCARE_TO_PRESCHOOL,
        )

        val sftpClient = SftpClient(primus.sftp.toSftpEnv())
        val data =
            sftpClient.getAsString(
                "upload/922_daycare_to_preschool_transfer_2025-08-01.json",
                Charsets.UTF_8,
            )
        assertEquals("[]", data)
    }

    @Test
    fun `latest template is selected`() {
        db.transaction { tx ->
            tx.insert(
                DevDocumentTemplate(
                    name = "Tiedonsiirto varhaiskasvatuksesta esiopetukseen 2023-2024",
                    validity = DateRange(LocalDate.of(2023, 8, 1), LocalDate.of(2024, 7, 31)),
                    content = DocumentTemplateContent(sections = emptyList()),
                )
            )
            tx.insert(
                DevDocumentTemplate(
                    name = "Tiedonsiirto varhaiskasvatuksesta esiopetukseen 2024-2025",
                    validity = DateRange(LocalDate.of(2024, 8, 1), LocalDate.of(2025, 7, 31)),
                    content = DocumentTemplateContent(sections = emptyList()),
                )
            )
            tx.insert(
                DevDocumentTemplate(
                    name = "Tiedonsiirto varhaiskasvatuksesta esiopetukseen 2025-2026",
                    validity = DateRange(LocalDate.of(2025, 8, 1), LocalDate.of(2026, 7, 31)),
                    content = DocumentTemplateContent(sections = emptyList()),
                )
            )
            tx.insert(
                DevDocumentTemplate(
                    name = "Tiedonsiirto esiopetuksesta perusopetukseen 2024-2025",
                    validity = DateRange(LocalDate.of(2024, 8, 1), LocalDate.of(2025, 7, 31)),
                    content = DocumentTemplateContent(sections = emptyList()),
                )
            )
        }

        val primus = properties.primus ?: error("Primus not configured")
        exportChildDocumentsViaSftp(
            db,
            clock,
            "922",
            primus,
            ChildDocumentTransferType.DAYCARE_TO_PRESCHOOL,
        )

        val sftpClient = SftpClient(primus.sftp.toSftpEnv())
        val data =
            sftpClient.getAsString(
                "upload/922_daycare_to_preschool_transfer_2025-08-01.json",
                Charsets.UTF_8,
            )
        assertEquals("[]", data)
    }

    @Test
    fun `document data is exported`() {
        val questionId = "1.1"
        val answer = AnsweredQuestion.TextAnswer(questionId, answer = "vastaus")
        val content = DocumentContent(answers = listOf(answer))

        db.transaction { tx ->
            val employee = DevEmployee().also { tx.insert(it) }
            val childId = tx.insert(DevPerson(), DevPersonType.CHILD)
            val templateId =
                tx.insert(
                    DevDocumentTemplate(
                        name = "Tiedonsiirto varhaiskasvatuksesta esiopetukseen 2024-2025",
                        validity = DateRange(LocalDate.of(2024, 8, 1), LocalDate.of(2025, 7, 31)),
                        content =
                            DocumentTemplateContent(
                                sections =
                                    listOf(
                                        Section(
                                            id = "1",
                                            label = "section",
                                            questions = listOf(TextQuestion(questionId, "kysymys")),
                                        )
                                    )
                            ),
                    )
                )
            tx.insert(
                DevChildDocument(
                    status = DocumentStatus.COMPLETED,
                    childId = childId,
                    templateId = templateId,
                    content = content,
                    modifiedAt = timestamp,
                    modifiedBy = employee.evakaUserId,
                    contentLockedAt = timestamp,
                    contentLockedBy = null,
                    publishedVersions =
                        listOf(
                            DevChildDocumentPublishedVersion(
                                versionNumber = 1,
                                createdAt = timestamp,
                                createdBy = employee.evakaUserId,
                                publishedContent = content,
                            )
                        ),
                )
            )
        }

        val primus = properties.primus ?: error("Primus not configured")
        exportChildDocumentsViaSftp(
            db,
            clock,
            "922",
            primus,
            ChildDocumentTransferType.DAYCARE_TO_PRESCHOOL,
        )

        val sftpClient = SftpClient(primus.sftp.toSftpEnv())
        val data =
            sftpClient.getAsString(
                "upload/922_daycare_to_preschool_transfer_2025-08-01.json",
                Charsets.UTF_8,
            )
        assertEquals(
            "[{\"child\": {\"oid\": null, \"last_name\": \"Person\", \"first_name\": \"Test\", \"date_of_birth\": \"1980-01-01\"}, \"document\": {\"kysymys\": \"vastaus\"}}]",
            data,
        )
    }
}
