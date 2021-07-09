// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.client

import fi.espoo.evaka.shared.FeeDecisionId
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest

private val logger = KotlinLogging.logger {}

@Service("invoicingS3DocumentClient")
class S3DocumentClient(
    private val s3Client: S3Client,
    @Value("\${fi.espoo.voltti.document.bucket.paymentdecision}")
    private val feeDecisionBucket: String
) {
    fun upload(bucketName: String, document: Document, contentType: String) {
        val key = document.key
        val request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType(contentType)
            .contentLength(document.bytes.size.toLong())
            .build()

        s3Client.putObject(request, RequestBody.fromBytes(document.bytes))
    }

    fun uploadPdfToS3(bucket: String, key: String, documentBytes: ByteArray) {
        upload(
            bucket,
            Document(key, documentBytes),
            "application/pdf"
        )

        logger.debug { "PDF (object name: $key) uploaded to S3 bucket $bucket." }
    }

    private fun getFeeDecisionDocumentKey(decisionId: FeeDecisionId, lang: String) = "feedecision_${decisionId}_$lang.pdf"

    fun uploadFeeDecisionPdf(decisionId: FeeDecisionId, documentBytes: ByteArray, lang: String): String {
        val documentKey = getFeeDecisionDocumentKey(decisionId, lang)

        logger.debug { "Uploading fee decision PDF for $decisionId to S3." }

        uploadPdfToS3(feeDecisionBucket, documentKey, documentBytes).toString()

        return documentKey
    }

    fun getPdf(bucket: String, key: String): ByteArray {
        val request = GetObjectRequest.builder().bucket(bucket).key(key).build()
        return s3Client.getObjectAsBytes(request).asByteArray()
    }

    fun getFeeDecisionPdf(documentKey: String): ByteArray {
        logger.debug { "Getting fee decision PDF for $documentKey." }
        val request = GetObjectRequest.builder().bucket(feeDecisionBucket).key(documentKey).build()
        return s3Client.getObjectAsBytes(request).asByteArray()
    }

    fun getFeeDecisionBucket(): String = feeDecisionBucket
}

data class Document(val key: String, val bytes: ByteArray)
