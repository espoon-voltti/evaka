// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.logging

import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.servlet.HandlerMapping

fun HttpServletRequest.getBestMatchingPattern(): String? {
    val pattern = this.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)
    return if (pattern is String) pattern else null
}

fun HttpServletRequest.getPathVariables(): Map<String, String> {
    val variables = this.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as? Map<*, *>
    val result = mutableMapOf<String, String>()
    variables?.forEach { (key, value) ->
        if (key is String && value is String) {
            result[key] = value
        }
    }
    return result
}
