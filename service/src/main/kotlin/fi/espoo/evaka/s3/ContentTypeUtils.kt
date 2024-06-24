// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.s3

import fi.espoo.evaka.shared.domain.BadRequest
import java.io.InputStream

val tika: org.apache.tika.Tika = org.apache.tika.Tika()

enum class ContentTypePattern(
    private val type: String,
    private val subtypePattern: String,
    private val allowedFileExtensions: Set<String>
) {
    JPEG("image", "jpeg", setOf("jpg", "jpeg")),
    PNG("image", "png", setOf("png")),
    PDF("application", "pdf", setOf("pdf")),
    MSWORD("application", "msword", setOf("doc")),
    MSWORD_DOCX(
        "application",
        "vnd.openxmlformats-officedocument.wordprocessingml.document",
        setOf("docx")
    ),
    OPEN_DOCUMENT_TEXT("application", "vnd.oasis.opendocument.text", setOf("odt")),
    TIKA_MSOFFICE("application", "x-tika-msoffice", setOf("doc", "docx")),
    TIKA_OOXML("application", "x-tika-ooxml", setOf("doc", "docx")),
    VIDEO_ANY("video", "*", setOf("avi", "mp4", "mpeg", "mov", "ogv", "webm", "3gp")),
    AUDIO_ANY("audio", "*", setOf("aac", "mid", "midi", "mp3", "oga", "wav", "weba", "3gp"));

    fun matchesContentType(contentType: String): Boolean {
        val parts = contentType.split("/", ";")
        if (parts.size < 2) return false
        return parts[0] == type && (subtypePattern == "*" || parts[1] == subtypePattern)
    }

    fun matchesExtension(fileExtension: String): Boolean = allowedFileExtensions.contains(fileExtension.lowercase())
}

fun checkFileContentType(
    file: InputStream,
    allowedContentTypes: Set<ContentTypePattern>
): String {
    val detectedContentType = tika.detect(file)
    allowedContentTypes.find { it.matchesContentType(detectedContentType) }
        ?: throw BadRequest("Invalid content type $detectedContentType", "INVALID_CONTENT_TYPE")
    return detectedContentType
}

fun checkFileContentTypeAndExtension(
    file: InputStream,
    fileExtension: String,
    allowedContentTypes: List<ContentTypePattern>
): String {
    val detectedContentType = tika.detect(file)
    val contentTypePattern =
        allowedContentTypes.find { it.matchesContentType(detectedContentType) }
            ?: throw BadRequest("Invalid content type $detectedContentType", "INVALID_CONTENT_TYPE")
    if (!contentTypePattern.matchesExtension(fileExtension)) {
        throw BadRequest("Invalid file extension $fileExtension", "EXTENSION_INVALID")
    }
    return detectedContentType
}
