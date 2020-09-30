// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.service.attachments

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3URI
import org.springframework.stereotype.Service
import java.lang.IllegalArgumentException

@Service
class S3DocumentService(private val s3: AmazonS3) : DocumentService {
    override fun getDocument(documentUri: String): ByteArray {
        val uri = AmazonS3URI(documentUri)
        checkKeyIsNotNull(uri)
        checkBucketExists(uri)

        val s3Object = s3.getObject(uri.bucket, uri.key)
        return s3Object.objectContent.readBytes()
    }

    private fun checkBucketExists(uri: AmazonS3URI) {
        if (!s3.doesBucketExistV2(uri.bucket)) {
            throw IllegalArgumentException("Bucket [${uri.bucket}] does not exist")
        }
    }

    private fun checkKeyIsNotNull(uri: AmazonS3URI) {
        if (uri.key == null) {
            throw IllegalArgumentException("Document key is missing from documentUri [${uri.uri}]")
        }
    }
}
