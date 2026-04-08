// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.document.archival

import evaka.core.FullApplicationTest
import evaka.core.document.ChildDocumentType
import evaka.core.document.DocumentTemplateContent
import evaka.core.document.childdocument.DocumentContent
import evaka.core.document.childdocument.DocumentStatus
import evaka.core.shared.ChildDocumentId
import evaka.core.shared.ChildId
import evaka.core.shared.DocumentTemplateId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.dev.DevChildDocument
import evaka.core.shared.dev.DevChildDocumentPublishedVersion
import evaka.core.shared.dev.DevDocumentTemplate
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.domain.UiLanguage
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ChildDocumentArchivalSchedulingIntegrationTest :
    FullApplicationTest(resetDbBeforeEach = true) {

    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val now = HelsinkiDateTime.of(LocalDateTime.of(2025, 11, 5, 12, 0))
    private val clock = MockEvakaClock(now)
    private val today = clock.today()
    private val userId = AuthenticatedUser.SystemInternalUser.evakaUserId

    private val childId = ChildId(UUID.randomUUID())
    private val templateId = DocumentTemplateId(UUID.randomUUID())
    private val documentId = ChildDocumentId(UUID.randomUUID())
    private val emptyContent = DocumentContent(answers = emptyList())

    @BeforeEach
    fun setUp() {
        db.transaction { tx ->
            tx.insert(
                DevPerson(id = childId, dateOfBirth = LocalDate.of(2020, 1, 1)),
                DevPersonType.CHILD,
            )
        }
    }

    @Test
    fun `schedules archival jobs for documents from expired templates`() {
        // Template validity ended 40 days ago
        val validityEnd = today.minusDays(40)
        insertTemplateAndDocument(validityEnd, archiveExternally = true)

        planChildDocumentArchival(db, clock, asyncJobRunner, delayDays = 30)

        val jobs = getScheduledArchivalJobs()
        assertEquals(1, jobs.size)
        assertEquals(documentId, jobs.first())
    }

    @Test
    fun `does not schedule jobs when template validity ended less than delay days ago`() {
        // Template validity ended 20 days ago, delay is 30 days
        val validityEnd = today.minusDays(20)
        insertTemplateAndDocument(validityEnd, archiveExternally = true)

        planChildDocumentArchival(db, clock, asyncJobRunner, delayDays = 30)

        val jobs = getScheduledArchivalJobs()
        assertEquals(0, jobs.size)
    }

    @Test
    fun `does not schedule jobs for templates not marked for external archival`() {
        val validityEnd = today.minusDays(40)
        insertTemplateAndDocument(validityEnd, archiveExternally = false)

        planChildDocumentArchival(db, clock, asyncJobRunner, delayDays = 30)

        val jobs = getScheduledArchivalJobs()
        assertEquals(0, jobs.size)
    }

    @Test
    fun `does not schedule jobs for already archived documents`() {
        val validityEnd = today.minusDays(40)
        insertTemplateAndDocument(validityEnd, archiveExternally = true)

        // Mark as archived
        db.transaction { tx ->
            tx.execute {
                sql(
                    """
                    UPDATE child_document
                    SET archived_at = ${bind(now)}
                    WHERE id = ${bind(documentId)}
                    """
                )
            }
        }

        planChildDocumentArchival(db, clock, asyncJobRunner, delayDays = 30)

        val jobs = getScheduledArchivalJobs()
        assertEquals(0, jobs.size)
    }

    @Test
    fun `respects archival limit when specified`() {
        val validityEnd = today.minusDays(40)

        // Create 5 documents eligible for archival
        val documentIds = (1..5).map { ChildDocumentId(UUID.randomUUID()) }
        db.transaction { tx ->
            tx.insert(
                DevDocumentTemplate(
                    id = templateId,
                    name = "Test Template",
                    type = ChildDocumentType.PEDAGOGICAL_REPORT,
                    language = UiLanguage.FI,
                    validity = DateRange(LocalDate.of(2020, 1, 1), validityEnd),
                    content = DocumentTemplateContent(sections = emptyList()),
                    archiveExternally = true,
                    processDefinitionNumber = "12.06.01",
                    archiveDurationMonths = 120,
                )
            )

            documentIds.forEach { docId ->
                tx.insert(
                    DevChildDocument(
                        id = docId,
                        childId = childId,
                        templateId = templateId,
                        status = DocumentStatus.COMPLETED,
                        content = emptyContent,
                        modifiedAt = now,
                        modifiedBy = userId,
                        contentLockedAt = now,
                        contentLockedBy = null,
                        publishedVersions =
                            listOf(
                                DevChildDocumentPublishedVersion(
                                    versionNumber = 1,
                                    createdAt = now,
                                    createdBy = userId,
                                    publishedContent = emptyContent,
                                )
                            ),
                    )
                )
            }
        }

        // Plan with limit of 3
        planChildDocumentArchival(db, clock, asyncJobRunner, delayDays = 30, limit = 3)

        val jobs = getScheduledArchivalJobs()
        assertEquals(3, jobs.size)
    }

    private fun insertTemplateAndDocument(validityEnd: LocalDate, archiveExternally: Boolean) {
        db.transaction { tx ->
            tx.insert(
                DevDocumentTemplate(
                    id = templateId,
                    name = "Test Template",
                    type = ChildDocumentType.PEDAGOGICAL_REPORT,
                    language = UiLanguage.FI,
                    validity = DateRange(LocalDate.of(2020, 1, 1), validityEnd),
                    content = DocumentTemplateContent(sections = emptyList()),
                    archiveExternally = archiveExternally,
                    processDefinitionNumber = if (archiveExternally) "12.06.01" else null,
                    archiveDurationMonths = if (archiveExternally) 120 else null,
                )
            )

            tx.insert(
                DevChildDocument(
                    id = documentId,
                    childId = childId,
                    templateId = templateId,
                    status = DocumentStatus.COMPLETED,
                    content = emptyContent,
                    modifiedAt = now,
                    modifiedBy = userId,
                    contentLockedAt = now,
                    contentLockedBy = null,
                    publishedVersions =
                        listOf(
                            DevChildDocumentPublishedVersion(
                                versionNumber = 1,
                                createdAt = now,
                                createdBy = userId,
                                publishedContent = emptyContent,
                            )
                        ),
                )
            )
        }
    }

    private fun getScheduledArchivalJobs(): List<ChildDocumentId> {
        return db.read { tx ->
            tx.createQuery {
                    sql(
                        """
                        SELECT payload->>'documentId' as document_id
                        FROM async_job
                        WHERE type = 'ArchiveChildDocument'
                        ORDER BY id
                        """
                    )
                }
                .toList<String>()
                .map { ChildDocumentId(UUID.fromString(it)) }
        }
    }
}
