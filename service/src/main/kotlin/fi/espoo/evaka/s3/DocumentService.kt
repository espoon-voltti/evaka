// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.s3

import org.springframework.http.ResponseEntity

data class Document(
    val name: String,
    val bytes: ByteArray,
    val contentType: String,
)

data class DocumentLocation(val bucket: String, val key: String)

interface DocumentService {
    /**
     * Get file by path
     */
    fun get(bucketName: String, key: String): Document

    /**
     * Return the file as a response with Content-Disposition: attachment and an optional filename
     */
    fun responseAttachment(bucketName: String, key: String, fileName: String?): ResponseEntity<Any>

    /**
     * Return the file as a response with Content-Disposition: inline
     */
    fun responseInline(bucketName: String, key: String): ResponseEntity<Any>

    /**
     * Upload a document to S3. [DocumentLocation] is returned in response
     */
    fun upload(bucketName: String, document: Document): DocumentLocation

    /**
     * Delete a document from S3
     */
    fun delete(bucketName: String, key: String)
}
