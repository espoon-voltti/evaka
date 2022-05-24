// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.s3

import com.github.kittinunf.fuel.core.Response
import fi.espoo.evaka.shared.domain.NotFound
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.net.URL
import java.time.Duration

data class Document(
    val name: String,
    val bytes: ByteArray,
    val contentType: String,
)

data class DocumentLocation(val bucket: String, val key: String)

const val INTERNAL_REDIRECT_PREFIX = "/internal_redirect/"

class DocumentService(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    private val proxyThroughNginx: Boolean,
) {
    fun get(bucketName: String, key: String): Document {
        val request = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .build()
        val stream = s3Client.getObject(request) ?: throw NotFound("File not found")
        return stream.use {
            Document(name = key, bytes = it.readAllBytes(), contentType = it.response().contentType())
        }
    }

    private sealed class ContentDispositionType {
        object Inline : ContentDispositionType()
        data class Attachment(val fileName: String?) : ContentDispositionType()
    }

    private fun getContentDispositionHeader(request: ContentDispositionType): String? {
        return when (request) {
            is ContentDispositionType.Inline -> null
            is ContentDispositionType.Attachment ->
                if (request.fileName != null) {
                    ContentDisposition
                        .builder("attachment")
                        .filename(request.fileName)
                        .build()
                        .toString()
                } else {
                    "attachment"
                }
        }
    }

    private fun presignedGetUrl(bucketName: String, key: String, contentDispositionHeader: String?): URL {
        val request = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .let {
                if (contentDispositionHeader != null) it.responseContentDisposition(contentDispositionHeader)
                else it
            }
            .build()

        val getObjectPresignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(1))
            .getObjectRequest(request)
            .build()

        return s3Presigner.presignGetObject(getObjectPresignRequest).url()
    }

    private fun response(
        bucketName: String,
        key: String,
        contentDisposition: ContentDispositionType
    ): ResponseEntity<Any> {
        val contentDispositionHeader = getContentDispositionHeader(contentDisposition)
        val presignedUrl = presignedGetUrl(bucketName, key, contentDispositionHeader)

        return if (proxyThroughNginx) {
            val url = "$INTERNAL_REDIRECT_PREFIX$presignedUrl"
            ResponseEntity.ok().header("X-Accel-Redirect", url).body("")
        } else {
            // nginx is not available in development => redirect to the presigned S3 url
            ResponseEntity.status(HttpStatus.FOUND).header("Location", presignedUrl.toString()).body("")
        }
    }

    fun responseAttachment(bucketName: String, key: String, fileName: String?) =
        response(bucketName, key, ContentDispositionType.Attachment(fileName))

    fun responseInline(bucketName: String, key: String) =
        response(bucketName, key, ContentDispositionType.Inline)

    fun upload(bucketName: String, document: Document): DocumentLocation {
        val key = document.name
        val request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType(document.contentType)
            .build()

        val body = RequestBody.fromBytes(document.bytes)

        s3Client.putObject(request, body)
        return DocumentLocation(bucket = bucketName, key = key)
    }

    fun delete(bucketName: String, key: String) {
        val request = DeleteObjectRequest.builder().bucket(bucketName).key(key).build()
        s3Client.deleteObject(request)
    }
}

fun fuelResponseToS3URL(response: Response): String {
    return response.headers["X-Accel-Redirect"].first().replace(INTERNAL_REDIRECT_PREFIX, "")
}

fun responseEntityToS3URL(response: ResponseEntity<Any>): String {
    return response.headers["X-Accel-Redirect"]!!.first().replace(INTERNAL_REDIRECT_PREFIX, "")
}
