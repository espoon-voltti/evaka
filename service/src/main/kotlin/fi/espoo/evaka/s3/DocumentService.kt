// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.s3

import org.springframework.http.ContentDisposition
import org.springframework.http.ResponseEntity

data class Document(
    val name: String,
    val bytes: ByteArray,
    val contentType: String
)

data class DocumentLocation(
    val bucket: String,
    val key: String
)

interface DocumentService {
    fun get(
        bucketName: String,
        key: String
    ): Document

    fun response(
        bucketName: String,
        key: String,
        contentDisposition: ContentDisposition
    ): ResponseEntity<Any>

    fun responseAttachment(
        bucketName: String,
        key: String,
        fileName: String?
    ) = response(
        bucketName,
        key,
        ContentDisposition.attachment().filename(fileName, Charsets.UTF_8).build()
    )

    fun responseInline(
        bucketName: String,
        key: String,
        fileName: String?
    ) = response(
        bucketName,
        key,
        ContentDisposition.inline().filename(fileName, Charsets.UTF_8).build()
    )

    fun upload(
        bucketName: String,
        document: Document
    ): DocumentLocation

    fun delete(
        bucketName: String,
        key: String
    )
}
