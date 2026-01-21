// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.logging

import fi.espoo.voltti.logging.MdcKey
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.web.servlet.HandlerInterceptor

/**
 * Stores matched path pattern and path parameters in MDC so that they are available in audit
 * logging
 */
class RouteLoggingInterceptor : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        request.getBestMatchingPattern()?.let { MdcKey.HTTP_ROUTE.set(it) }

        request.getPathVariables().forEach { (name, value) ->
            val key = "${MdcKey.HTTP_PATH_PARAM.key}.$name"
            MDC.put(key, value)
        }

        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?,
    ) {
        MdcKey.HTTP_ROUTE.unset()
        val keysToRemove =
            MDC.getCopyOfContextMap()?.keys?.filter {
                it.startsWith("${MdcKey.HTTP_PATH_PARAM.key}.")
            }
        keysToRemove?.forEach { MDC.remove(it) }
    }
}
