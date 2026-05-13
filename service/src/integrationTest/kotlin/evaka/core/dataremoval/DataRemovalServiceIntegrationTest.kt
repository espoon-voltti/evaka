// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dataremoval

import evaka.core.FullApplicationTest
import evaka.core.document.ChildDocumentType
import evaka.core.document.DocumentDeletionBasis
import evaka.core.document.DocumentTemplateContent
import evaka.core.document.childdocument.DocumentContent
import evaka.core.document.childdocument.DocumentStatus
import evaka.core.placement.PlacementType
import evaka.core.shared.ChildDocumentId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevChildDocument
import evaka.core.shared.dev.DevChildDocumentPublishedVersion
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDocumentTemplate
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class DataRemovalServiceIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var dataRemovalService: DataRemovalService
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val today = LocalDate.of(2026, 5, 7)
    private val clock = MockEvakaClock(HelsinkiDateTime.of(today, LocalTime.of(2, 0)))

    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val careArea = DevCareArea()
    private val daycare = DevDaycare(areaId = careArea.id)
    private val child = DevPerson()

    @BeforeEach
    fun setup() {
        db.transaction { tx ->
            tx.insert(admin)
            tx.insert(careArea)
            tx.insert(daycare)
            tx.insert(child, DevPersonType.CHILD)
        }
    }

    @Test
    fun `handler enqueues one DeleteChildDocumentPdf job per published key across all deleted documents`() {
        val template = insertTemplate()
        insertExpiredDocument(template, keys = listOf("s3-key-a-v1", "s3-key-a-v2"))
        insertExpiredDocument(template, keys = listOf("s3-key-b-v1"))
        // A pending PDF (null key) must not leak as a null payload.
        insertExpiredDocument(template, keys = listOf("s3-key-c-v1", null))

        dataRemovalService.deleteExpiredChildDocuments(
            db,
            clock,
            AsyncJob.DeleteExpiredChildDocuments(),
        )

        assertEquals(
            setOf("s3-key-a-v1", "s3-key-a-v2", "s3-key-b-v1", "s3-key-c-v1"),
            scheduledPdfDeletionKeys(),
        )
    }

    @Test
    fun `handler enqueues nothing when no documents are eligible for deletion`() {
        val template = insertTemplate()
        // Recently modified document is not yet expired.
        insertDocument(template, statusModifiedAt = clock.now(), keys = listOf("fresh-key"))

        dataRemovalService.deleteExpiredChildDocuments(
            db,
            clock,
            AsyncJob.DeleteExpiredChildDocuments(),
        )

        assertTrue(scheduledPdfDeletionKeys().isEmpty())
    }

    @Test
    fun `handler enqueues nothing when expired documents have no published PDFs`() {
        val template = insertTemplate()
        insertExpiredDocument(template, keys = emptyList())

        dataRemovalService.deleteExpiredChildDocuments(
            db,
            clock,
            AsyncJob.DeleteExpiredChildDocuments(),
        )

        assertTrue(scheduledPdfDeletionKeys().isEmpty())
    }

    private fun scheduledPdfDeletionKeys(): Set<String> =
        db.read { tx ->
                tx.createQuery {
                        sql(
                            "SELECT payload::json->>'key' FROM async_job WHERE type = 'DeleteChildDocumentPdf'"
                        )
                    }
                    .toList<String>()
            }
            .toSet()

    private fun insertTemplate(retentionDays: Int = 10): DevDocumentTemplate {
        val template =
            DevDocumentTemplate(
                type = ChildDocumentType.PEDAGOGICAL_REPORT,
                placementTypes = setOf(PlacementType.DAYCARE),
                validity = DateRange(today.minusYears(5), null),
                content = DocumentTemplateContent(emptyList()),
                deletionRetentionDays = retentionDays,
                deletionRetentionBasis = DocumentDeletionBasis.STATUS_TRANSITION,
            )
        db.transaction { tx -> tx.insert(template) }
        return template
    }

    private fun insertExpiredDocument(
        template: DevDocumentTemplate,
        keys: List<String?>,
    ): ChildDocumentId =
        insertDocument(template, statusModifiedAt = clock.now().minusYears(1), keys = keys)

    private fun insertDocument(
        template: DevDocumentTemplate,
        statusModifiedAt: HelsinkiDateTime,
        keys: List<String?>,
    ): ChildDocumentId {
        val publishedVersions = keys.mapIndexed { index, key ->
            DevChildDocumentPublishedVersion(
                versionNumber = index + 1,
                createdAt = statusModifiedAt.plusMinutes(index.toLong()),
                createdBy = admin.evakaUserId,
                publishedContent = DocumentContent(emptyList()),
                documentKey = key,
            )
        }
        val doc =
            DevChildDocument(
                status = DocumentStatus.COMPLETED,
                childId = child.id,
                templateId = template.id,
                content = DocumentContent(emptyList()),
                modifiedAt = statusModifiedAt,
                modifiedBy = admin.evakaUserId,
                statusModifiedAt = statusModifiedAt,
                contentLockedAt = statusModifiedAt,
                contentLockedBy = admin.id,
                publishedVersions = publishedVersions,
            )
        return db.transaction { tx -> tx.insert(doc) }
    }
}
