// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.service.attachments

import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest

@Service
class S3DocumentService(private val s3: S3Client) : DocumentService {
    override fun getDocument(bucket: String, key: String): ByteArray {
        val request = GetObjectRequest.builder().bucket(bucket).key(key).build()
        return s3.getObjectAsBytes(request).asByteArray()
    }
}
