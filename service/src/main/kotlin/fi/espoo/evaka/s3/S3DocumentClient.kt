// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.s3

import fi.espoo.evaka.shared.domain.NotFound
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.io.InputStream
import java.net.URL
import java.time.Duration

@Service("appS3DocumentClient")
class S3DocumentClient(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner?
) : DocumentService {
    override fun get(bucketName: String, key: String): Document = stream(bucketName, key).use {
        DocumentWrapper(name = key, bytes = it.readAllBytes())
    }

    override fun presignedGetUrl(bucketName: String, key: String): URL? {
        if (s3Presigner == null) return null

        val request = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .build()

        val getObjectPresignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(1))
            .getObjectRequest(request)
            .build()

        return s3Presigner.presignGetObject(getObjectPresignRequest).url()
    }

    override fun stream(bucketName: String, key: String): InputStream {
        val request = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .build()
        return s3Client.getObject(request) ?: throw NotFound("File not found")
    }

    override fun upload(bucketName: String, document: Document, contentType: String): DocumentLocation {
        val key = document.getName()
        val request = PutObjectRequest.builder().bucket(bucketName).key(key).contentType(contentType).build()

        val body = RequestBody.fromBytes(document.getBytes())

        s3Client.putObject(request, body)
        return DocumentLocation(
            bucket = bucketName,
            key = key
        )
    }

    override fun delete(bucketName: String, key: String) {
        val request = DeleteObjectRequest.builder().bucket(bucketName).key(key).build()
        s3Client.deleteObject(request)
    }
}
