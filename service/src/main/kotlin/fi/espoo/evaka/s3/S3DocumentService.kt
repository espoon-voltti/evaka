// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.s3

import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.shared.domain.NotFound
import java.io.InputStream
import java.net.URL
import java.time.Duration
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest

private const val INTERNAL_REDIRECT_PREFIX = "/internal_redirect/"

@Service
class S3DocumentService(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    private val env: BucketEnv,
) : DocumentService {
    override fun locate(key: DocumentKey): DocumentLocation =
        DocumentLocation(
            bucket =
                when (key) {
                    is DocumentKey.AssistanceNeedDecision -> env.data
                    is DocumentKey.AssistanceNeedPreschoolDecision -> env.data
                    is DocumentKey.Attachment -> env.attachments
                    is DocumentKey.ChildDocument -> env.data
                    is DocumentKey.ChildImage -> env.data
                    is DocumentKey.Decision -> env.decisions
                    is DocumentKey.FeeDecision -> env.feeDecisions
                    is DocumentKey.VoucherValueDecision -> env.voucherValueDecisions
                },
            key = key.value,
        )

    override fun get(location: DocumentLocation): Document {
        val request = GetObjectRequest.builder().bucket(location.bucket).key(location.key).build()
        val stream = s3Client.getObject(request) ?: throw NotFound("File not found")
        return stream.use {
            Document(
                name = location.key,
                bytes = it.readAllBytes(),
                contentType = it.response().contentType(),
            )
        }
    }

    private fun presignedGetUrl(location: DocumentLocation): URL {
        val request = GetObjectRequest.builder().bucket(location.bucket).key(location.key).build()

        val getObjectPresignRequest =
            GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(1))
                .getObjectRequest(request)
                .build()

        return s3Presigner.presignGetObject(getObjectPresignRequest).url()
    }

    override fun response(
        location: DocumentLocation,
        contentDisposition: ContentDisposition,
    ): ResponseEntity<Any> {
        val presignedUrl = presignedGetUrl(location)

        return if (env.proxyThroughNginx) {
            val url = "$INTERNAL_REDIRECT_PREFIX$presignedUrl"
            ResponseEntity.ok()
                .header("X-Accel-Redirect", url)
                .header("Content-Disposition", contentDisposition.toString())
                .body(null)
        } else {
            // nginx is not available in development => redirect to the presigned S3 url
            ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", presignedUrl.toString())
                .header("Content-Disposition", contentDisposition.toString())
                .body(null)
        }
    }

    override fun upload(
        location: DocumentLocation,
        inputStream: InputStream,
        size: Long,
        contentType: String,
    ) {
        val request =
            PutObjectRequest.builder()
                .bucket(location.bucket)
                .key(location.key)
                .contentType(contentType)
                .build()

        val body = RequestBody.fromInputStream(inputStream, size)
        s3Client.putObject(request, body)
    }

    override fun delete(location: DocumentLocation) {
        val request =
            DeleteObjectRequest.builder().bucket(location.bucket).key(location.key).build()
        s3Client.deleteObject(request)
    }
}

fun responseEntityToS3URL(response: ResponseEntity<Any>): String {
    return response.headers["X-Accel-Redirect"]!!.first().replace(INTERNAL_REDIRECT_PREFIX, "")
}
