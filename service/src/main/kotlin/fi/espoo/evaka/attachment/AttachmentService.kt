// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attachment

import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class AttachmentService(
    private val documentClient: DocumentService,
    bucketEnv: BucketEnv,
    asyncJobRunner: AsyncJobRunner<AsyncJob>
) {
    private val filesBucket = bucketEnv.attachments
    private val logger = KotlinLogging.logger {}

    init {
        asyncJobRunner.registerHandler<AsyncJob.DeleteAttachment> { dbc, _, job ->
            deleteAttachment(dbc, job.attachmentId)
        }
    }

    /**
     * Deletes an attachment from both S3 and the database.
     *
     * This operation is idempotent and is safe to call even if the attachment doesn't exist in S3
     * and/or the database.
     */
    fun deleteAttachment(dbc: Database.Connection, id: AttachmentId) {
        logger.info("Deleting attachment $id")
        dbc.close() // avoid hogging the connection while we access S3
        // AWS S3 client seems to be idempotent, so deleting a non-existing file doesn't throw an
        // error
        documentClient.delete(filesBucket, "$id")
        // We must remove the bookkeeping information *after* we are sure the S3 file has been
        // removed, or we could end up losing all bookkeeping info but have a leftover file.
        dbc.transaction {
            it.createUpdate<Any> { sql("""
DELETE FROM attachment
WHERE id = ${bind(id)}
""") }
                .execute()
        }
    }
}
