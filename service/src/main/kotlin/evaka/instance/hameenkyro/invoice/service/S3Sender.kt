// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.hameenkyro.invoice.service

import evaka.core.shared.domain.HelsinkiDateTime
import evaka.instance.hameenkyro.HameenkyroProperties
import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest

@Service
class S3Sender(private val s3Client: S3Client, private val properties: HameenkyroProperties) {
    fun send(content: String, now: HelsinkiDateTime) {
        val bucket = properties.bucket.export
        val fileName =
            now.toLocalDateTime()
                .format(DateTimeFormatter.ofPattern("'invoices/vaka'yyyyMMdd'.txt'"))

        val request =
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .contentType("text/plain")
                .build()
        val body = RequestBody.fromString(content, StandardCharsets.ISO_8859_1)
        s3Client.putObject(request, body)
    }
}
