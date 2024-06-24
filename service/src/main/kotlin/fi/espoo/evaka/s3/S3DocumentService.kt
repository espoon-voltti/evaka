// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.s3

import com.github.kittinunf.fuel.core.Response
import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.shared.domain.NotFound
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
    private val env: BucketEnv
) : DocumentService {
    override fun get(
        bucketName: String,
        key: String
    ): Document {
        val request =
            GetObjectRequest
                .builder()
                .bucket(bucketName)
                .key(key)
                .build()
        val stream = s3Client.getObject(request) ?: throw NotFound("File not found")
        return stream.use {
            Document(
                name = key,
                bytes = it.readAllBytes(),
                contentType = it.response().contentType()
            )
        }
    }

    private fun presignedGetUrl(
        bucketName: String,
        key: String
    ): URL {
        val request =
            GetObjectRequest
                .builder()
                .bucket(bucketName)
                .key(key)
                .build()

        val getObjectPresignRequest =
            GetObjectPresignRequest
                .builder()
                .signatureDuration(Duration.ofMinutes(1))
                .getObjectRequest(request)
                .build()

        return s3Presigner.presignGetObject(getObjectPresignRequest).url()
    }

    override fun response(
        bucketName: String,
        key: String,
        contentDisposition: ContentDisposition
    ): ResponseEntity<Any> {
        val presignedUrl = presignedGetUrl(bucketName, key)

        return if (env.proxyThroughNginx) {
            val url = "$INTERNAL_REDIRECT_PREFIX$presignedUrl"
            ResponseEntity
                .ok()
                .header("X-Accel-Redirect", url)
                .header("Content-Disposition", contentDisposition.toString())
                .body(null)
        } else {
            // nginx is not available in development => redirect to the presigned S3 url
            ResponseEntity
                .status(HttpStatus.FOUND)
                .header("Location", presignedUrl.toString())
                .header("Content-Disposition", contentDisposition.toString())
                .body(null)
        }
    }

    override fun upload(
        bucketName: String,
        document: Document
    ): DocumentLocation {
        val key = document.name
        val request =
            PutObjectRequest
                .builder()
                .bucket(bucketName)
                .key(key)
                .contentType(document.contentType)
                .build()

        val body = RequestBody.fromBytes(document.bytes)

        s3Client.putObject(request, body)
        return DocumentLocation(bucket = bucketName, key = key)
    }

    override fun delete(
        bucketName: String,
        key: String
    ) {
        val request =
            DeleteObjectRequest
                .builder()
                .bucket(bucketName)
                .key(key)
                .build()
        s3Client.deleteObject(request)
    }
}

fun fuelResponseToS3URL(response: Response): String = response.headers["X-Accel-Redirect"].first().replace(INTERNAL_REDIRECT_PREFIX, "")

fun responseEntityToS3URL(response: ResponseEntity<Any>): String =
    response.headers["X-Accel-Redirect"]!!.first().replace(INTERNAL_REDIRECT_PREFIX, "")
