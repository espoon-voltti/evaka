// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.s3

import fi.espoo.evaka.shared.domain.BadRequest
import java.io.InputStream

val tika: org.apache.tika.Tika = org.apache.tika.Tika()

enum class ContentType(val value: String) {
    JPEG("image/jpeg"),
    PNG("image/png"),
    PDF("application/pdf"),
    MSWORD("application/msword"),
    MSWORD_DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    OPEN_DOCUMENT_TEXT("application/vnd.oasis.opendocument.text"),
    TIKA_MSOFFICE("application/x-tika-msoffice"),
    TIKA_OOXML("application/x-tika-ooxml"),
    VIDEO_ANY("video/*"),
    AUDIO_ANY("audio/*"),
}

fun getAllowedFileExtensionsByContentType(contentType: ContentType): Set<String> = when (contentType) {
    ContentType.JPEG -> setOf("jpg", "jpeg")
    ContentType.PNG -> setOf("png")
    ContentType.PDF -> setOf("pdf")
    ContentType.MSWORD -> setOf("doc")
    ContentType.MSWORD_DOCX -> setOf("docx")
    ContentType.OPEN_DOCUMENT_TEXT -> setOf("odt")
    ContentType.TIKA_MSOFFICE -> setOf("doc", "docx")
    ContentType.TIKA_OOXML -> setOf("doc", "docx")
    ContentType.VIDEO_ANY -> setOf("avi", "mp4", "mpeg", "ogv", "webm", "3gp")
    ContentType.AUDIO_ANY -> setOf("aac", "mid", "midi", "mp3", "oga", "wav", "weba", "3gp")
}

fun getAndCheckFileContentType(file: InputStream, allowedContentTypes: Set<ContentType>): String {
    val contentType = tika.detect(file)
    if (!isAllowedContentType(contentType, allowedContentTypes)) throw BadRequest("Invalid content type $contentType", "INVALID_CONTENT_TYPE")
    return contentType
}

// Matches given content type with list of allowed contents types which must be either be "type/subtype" or
// "type/*" to allow all subtypes of the type, like "image/png" or "video/*
fun isAllowedContentType(contentType: String, allowedContentTypes: Set<ContentType>): Boolean {
    val contentTypeParts = contentType.split("/", ";")
    return contentTypeParts.size >= 2 && allowedContentTypes.any { allowedContentType ->
        val allowedContentTypeParts = allowedContentType.value.split("/")
        (allowedContentTypeParts.size == 1 && contentTypeParts.get(0) == allowedContentTypeParts.get(0)) ||
            (
                allowedContentTypeParts.size == 2 && contentTypeParts.get(0) == allowedContentTypeParts.get(0) &&
                    (allowedContentTypeParts.get(1) == "*" || contentTypeParts.get(1) == allowedContentTypeParts.get(1))
                )
    }
}

fun checkFileExtension(fileExtension: String, contentType: String) {
    ContentType.values().find { it.value == contentType }
        ?.let { getAllowedFileExtensionsByContentType(it).contains(fileExtension.lowercase()) }
        ?: false ||
        throw BadRequest("Invalid file extension $fileExtension", "EXTENSION_INVALID")
}
