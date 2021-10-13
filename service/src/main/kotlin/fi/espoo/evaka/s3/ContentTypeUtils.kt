// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.s3

import fi.espoo.evaka.shared.domain.BadRequest
import java.io.InputStream

val tika: org.apache.tika.Tika = org.apache.tika.Tika()

fun getAndCheckFileContentType(file: InputStream, allowedContentTypes: List<String>): String {
    val contentType = tika.detect(file)
    if (!isAllowedContentType(contentType, allowedContentTypes)) throw BadRequest("Invalid content type")
    return contentType
}

// Matches given content type with list of allowed contents types which must in input type=file accept format:
// https://developer.mozilla.org/en-US/docs/Web/HTML/Element/input/file#unique_file_type_specifiers
// eg "image/png" or "video/*
fun isAllowedContentType(contentType: String, allowedContentTypes: List<String>): Boolean {
    val contentTypeParts = contentType.split("/", ";")
    return contentTypeParts.size >= 2 && allowedContentTypes.any { allowedContentType ->
        val allowedContentTypeParts = allowedContentType.split("/")
        (allowedContentTypeParts.size == 1 && contentTypeParts.get(0) == allowedContentTypeParts.get(0)) ||
            (
                allowedContentTypeParts.size == 2 && contentTypeParts.get(0) == allowedContentTypeParts.get(0) &&
                    (contentTypeParts.get(1) == "*" || contentTypeParts.get(1) == allowedContentTypeParts.get(1))
                )
    }
}
