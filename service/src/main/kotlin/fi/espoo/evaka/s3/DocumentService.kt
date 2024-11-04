// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.s3

import java.io.InputStream
import org.springframework.http.ContentDisposition
import org.springframework.http.ResponseEntity

data class Document(val name: String, val bytes: ByteArray, val contentType: String)

data class DocumentLocation(val bucket: String, val key: String)

interface DocumentService {
    fun locate(key: DocumentKey): DocumentLocation

    fun get(location: DocumentLocation): Document

    fun response(
        location: DocumentLocation,
        contentDisposition: ContentDisposition,
    ): ResponseEntity<Any>

    fun responseAttachment(location: DocumentLocation, fileName: String?) =
        response(
            location,
            ContentDisposition.attachment().filename(fileName, Charsets.UTF_8).build(),
        )

    fun responseInline(location: DocumentLocation, fileName: String?) =
        response(location, ContentDisposition.inline().filename(fileName, Charsets.UTF_8).build())

    fun upload(key: DocumentKey, bytes: ByteArray, contentType: String): DocumentLocation =
        bytes.inputStream().use { stream ->
            val location = locate(key)
            upload(location, stream, bytes.size.toLong(), contentType)
            location
        }

    fun upload(
        document: DocumentKey,
        inputStream: InputStream,
        size: Long,
        contentType: String,
    ): DocumentLocation {
        val location = locate(document)
        upload(location, inputStream, size, contentType)
        return location
    }

    fun upload(
        location: DocumentLocation,
        inputStream: InputStream,
        size: Long,
        contentType: String,
    )

    fun delete(key: DocumentKey) = delete(locate(key))

    fun delete(location: DocumentLocation)
}
