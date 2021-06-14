// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.service.attachments

import com.amazonaws.services.s3.AmazonS3
import org.springframework.stereotype.Service

@Service
class S3DocumentService(private val s3: AmazonS3) : DocumentService {
    override fun getDocument(bucket: String, key: String): ByteArray {
        if (!s3.doesBucketExistV2(bucket)) {
            throw IllegalArgumentException("Bucket [$bucket] does not exist")
        }

        val s3Object = s3.getObject(bucket, key)
        return s3Object.objectContent.readBytes()
    }
}
