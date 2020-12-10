// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.s3

import com.amazonaws.services.s3.model.ObjectMetadata
import java.net.URI

interface Document {
    fun getName(): String
    fun getPath(): String
    fun getBytes(): ByteArray
}

data class DocumentWrapper(private val name: String, private val path: String, private val bytes: ByteArray) :
    Document {
    override fun getName() = name
    override fun getPath() = path
    override fun getBytes() = bytes
}

data class DocumentLocation(val uri: URI)

interface DocumentService {
    /**
     * Get attachment by attachment path. Subclasses can set add more restrictions to path value.
     */
    fun get(bucketName: String, key: String): Document

    /**
     * Get attachment by URI. Subclasses can set add more restrictions to path value.
     */
    fun get(uri: URI): Document

    /**
     * Upload a document to S3. [DocumentLocation] is returned in response
     */
    fun upload(bucketName: String, document: Document, contentType: String): DocumentLocation

    /**
     * Delete a document from S3
     */
    fun delete(bucketName: String, key: String)

    /**
     * Check headers
     */
    fun headObject(bucketName: String, key: String): ObjectMetadata
}
