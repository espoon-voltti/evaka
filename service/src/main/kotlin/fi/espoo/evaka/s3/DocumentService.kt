// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.s3

import fi.espoo.evaka.shared.domain.NotFound
import org.springframework.http.ContentDisposition
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
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

@Service
class DocumentService(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner?
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

    private fun presignedGetUrl(bucketName: String, key: String, contentDispositionHeader: String?): URL? {
        if (s3Presigner == null) return null

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

        return if (presignedUrl != null) {
            val url = "/internal_redirect/$presignedUrl"
            ResponseEntity.ok().header("X-Accel-Redirect", url).body("")
        } else {
            // nginx is not available in development => pass the file data through our app
            val document = this.get(bucketName, key)
            ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.contentType))
                .header("Content-Disposition", contentDispositionHeader)
                .body(document.bytes)
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
