// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.client

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service("invoicingS3DocumentClient")
class S3DocumentClient(
    private val s3Client: AmazonS3,
    @Value("\${fi.espoo.voltti.document.bucket.paymentdecision}")
    private val feeDecisionBucket: String
) {
    fun upload(bucketName: String, document: Document, contentType: String) {
        val metadata = ObjectMetadata()
        metadata.contentType = contentType
        metadata.contentLength = document.bytes.size.toLong()
        val key = document.key
        val request = PutObjectRequest(
            bucketName,
            key,
            document.bytes.inputStream(),
            metadata
        )

        s3Client.putObject(request)
    }

    fun uploadPdfToS3(bucket: String, key: String, documentBytes: ByteArray) {
        upload(
            bucket,
            Document(key, documentBytes),
            "application/pdf"
        )

        logger.debug { "PDF (object name: $key) uploaded to S3 bucket $bucket." }
    }

    private fun getFeeDecisionDocumentKey(decisionId: UUID, lang: String) = "feedecision_${decisionId}_$lang.pdf"

    fun uploadFeeDecisionPdf(decisionId: UUID, documentBytes: ByteArray, lang: String): String {
        val documentKey = getFeeDecisionDocumentKey(decisionId, lang)

        logger.debug { "Uploading fee decision PDF for $decisionId to S3." }

        uploadPdfToS3(feeDecisionBucket, documentKey, documentBytes).toString()

        return documentKey
    }

    fun getPdf(bucket: String, key: String): ByteArray {
        val s3Object = s3Client.getObject(bucket, key)
        return s3Object.objectContent.readBytes()
    }

    fun getFeeDecisionPdf(documentKey: String): ByteArray {
        logger.debug { "Getting fee decision PDF for $documentKey." }
        val s3Object = s3Client.getObject(feeDecisionBucket, documentKey)
        return s3Object.objectContent.readBytes()
    }

    fun getFeeDecisionDocumentUri(documentKey: String): String = "s3://$feeDecisionBucket/$documentKey"

    fun getDocumentUri(bucket: String, key: String): String = "s3://$bucket/$key"
}

data class Document(val key: String, val bytes: ByteArray)
