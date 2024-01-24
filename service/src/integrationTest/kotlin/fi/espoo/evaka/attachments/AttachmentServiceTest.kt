// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attachments

import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.attachment.AttachmentParent
import fi.espoo.evaka.attachment.AttachmentService
import fi.espoo.evaka.attachment.getAttachment
import fi.espoo.evaka.attachment.insertAttachment
import fi.espoo.evaka.attachment.userAttachmentCount
import fi.espoo.evaka.s3.Document
import fi.espoo.evaka.s3.DocumentLocation
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevIncome
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.net.URI
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.ContentDisposition
import org.springframework.http.ResponseEntity

class AttachmentServiceTest : PureJdbiTest(resetDbBeforeEach = true) {
    private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    class MockDocumentService(
        private val uploadFn: () -> DocumentLocation,
        private val deleteFn: () -> Unit
    ) : DocumentService {
        override fun get(bucketName: String, key: String): Document = error("Not implemented")

        override fun response(
            bucketName: String,
            key: String,
            contentDisposition: ContentDisposition
        ): ResponseEntity<Any> = error("Not implemented")

        override fun upload(bucketName: String, document: Document): DocumentLocation = uploadFn()

        override fun delete(bucketName: String, key: String) = deleteFn()
    }

    private fun createAttachmentService(documentService: DocumentService) =
        AttachmentService(
            documentService,
            asyncJobRunner,
            BucketEnv(
                // none of these values matter, because FailingDocumentService doesn't use them
                s3MockUrl = URI(""),
                proxyThroughNginx = false,
                data = "",
                attachments = "",
                decisions = "",
                feeDecisions = "",
                voucherValueDecisions = ""
            )
        )

    @BeforeEach
    fun beforeEach() {
        asyncJobRunner = AsyncJobRunner(AsyncJob::class, listOf(AsyncJob.main), jdbi, noopTracer)
    }

    @AfterEach
    fun afterEach() {
        asyncJobRunner.close()
    }

    @Test
    fun `if saving an orphan attachment fails the file upload, an orphan row is still created`() {
        val clock = MockEvakaClock(2023, 1, 1, 12, 0)
        val user = AuthenticatedUser.SystemInternalUser
        val errorMessage = "Expected failure"
        val attachmentService =
            createAttachmentService(
                MockDocumentService(
                    uploadFn = { error(errorMessage) },
                    deleteFn = { error("Unexpected delete") }
                )
            )
        assertThrows<Exception>(errorMessage) {
            attachmentService.saveOrphanAttachment(
                db,
                user,
                clock,
                "test.pdf",
                byteArrayOf(0x11),
                "text/plain"
            )
        }
        assertEquals(1, db.read { it.userAttachmentCount(user.evakaUserId, AttachmentParent.None) })
    }

    @Test
    fun `if deleting an attachment file fails, the database row is not deleted`() {
        val clock = MockEvakaClock(2023, 1, 1, 12, 0)
        val errorMessage = "Expected failure"
        val attachmentService =
            createAttachmentService(
                MockDocumentService(
                    uploadFn = { error("Unexpected upload") },
                    deleteFn = { error(errorMessage) }
                )
            )
        val (attachment, parent) =
            db.transaction {
                val parent = it.insertTestIncome()
                Pair(it.insertTestAttachment(clock, parent), parent)
            }
        assertThrows<Exception>(errorMessage) { attachmentService.deleteAttachment(db, attachment) }
        assertEquals(parent, db.read { it.getAttachment(attachment) }?.attachedTo)
    }

    @Test
    fun `scheduled orphan deletion deletes old orphans but not attachments with a parent`() {
        val clock = MockEvakaClock(2023, 1, 1, 12, 0)
        val attachmentService =
            createAttachmentService(
                MockDocumentService(uploadFn = { error("Unexpected upload") }, deleteFn = {})
            )
        val (incomeAttachment, orphanAttachment) =
            db.transaction {
                Pair(
                    it.insertTestAttachment(clock, it.insertTestIncome()),
                    it.insertTestAttachment(clock, AttachmentParent.None)
                )
            }
        clock.tick(Duration.ofDays(1) + Duration.ofSeconds(1))
        db.transaction { attachmentService.scheduleOrphanAttachmentDeletion(it, clock) }
        clock.tick(Duration.ofSeconds(1))
        asyncJobRunner.runPendingJobsSync(clock)

        assertNotNull(db.read { it.getAttachment(incomeAttachment) })
        assertNull(db.read { it.getAttachment(orphanAttachment) })
    }

    private fun Database.Transaction.insertTestIncome() =
        AttachmentParent.Income(
            insert(
                DevIncome(
                    personId = insert(DevPerson(), DevPersonType.ADULT),
                    updatedBy = AuthenticatedUser.SystemInternalUser.evakaUserId
                )
            )
        )

    private fun Database.Transaction.insertTestAttachment(
        clock: EvakaClock,
        parent: AttachmentParent
    ) =
        insertAttachment(
            AuthenticatedUser.SystemInternalUser,
            clock.now(),
            "test.pdf",
            "text/plain",
            parent,
            type = null
        )
}
