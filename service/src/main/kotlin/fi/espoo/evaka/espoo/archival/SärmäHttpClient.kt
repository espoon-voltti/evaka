// SPDX-FileCopyrightText: 2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoo.archival

import fi.espoo.evaka.ArchiveEnv
import fi.espoo.evaka.document.archival.ArchivalClient
import fi.espoo.evaka.s3.Document
import java.time.Duration
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class SärmäHttpClient(private val archiveEnv: ArchiveEnv?) : ArchivalClient {

    private val httpClient =
        OkHttpClient.Builder()
            .connectTimeout(Duration.ofMinutes(1))
            .readTimeout(Duration.ofMinutes(1))
            .writeTimeout(Duration.ofMinutes(1))
            .build()

    override fun putDocument(
        documentContent: Document,
        metadataXml: String,
        masterId: String,
        classId: String,
        virtualArchiveId: String,
    ): Pair<Int, String?> {
        if (archiveEnv == null) {
            throw IllegalStateException("Archive environment not configured")
        }
        val pdfBody = documentContent.bytes.toRequestBody("application/pdf".toMediaType())
        val requestBody =
            MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("protocol_version", "1.0")
                .addFormDataPart("operation_id", "PUT")
                .addFormDataPart("response_format", "URL-ENCODED")
                .addFormDataPart("user_id", archiveEnv.userId)
                .addFormDataPart("user_role", archiveEnv.userRole)
                .addFormDataPart("nr_of_instances", "1")
                .addFormDataPart("instance_1_md_model_id", "standard")
                .addFormDataPart("instance_1_md_model_version", "1.0")
                .addFormDataPart("instance_1_md_master_id", masterId)
                .addFormDataPart("instance_1_md_master_version", "1.0")
                .addFormDataPart("instance_1_class_id", classId)
                .addFormDataPart("instance_1_virtual_archive_id", virtualArchiveId)
                .addFormDataPart("instance_1_record_payload_location", "SELF_CONTAINED")
                .addFormDataPart(
                    "instance_1_md_instance",
                    null,
                    metadataXml.toRequestBody("application/xml".toMediaType()),
                )
                .addFormDataPart(
                    "instance_1_record_payload_content_size",
                    documentContent.bytes.size.toString(),
                )
                .addFormDataPart("instance_1_record_payload_data", documentContent.name, pdfBody)
                .build()

        val endpointUrl = archiveEnv.url.resolve("PUT").toString()
        val response =
            httpClient
                .newCall(Request.Builder().url(endpointUrl).post(requestBody).build())
                .execute()
        return Pair(response.code, response.body?.string())
    }
}
