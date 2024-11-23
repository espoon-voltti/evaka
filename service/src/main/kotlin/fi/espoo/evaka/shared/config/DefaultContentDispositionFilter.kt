// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingResponseWrapper

@Component
class DefaultContentDispositionFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val responseWrapper = ContentCachingResponseWrapper(response)

        filterChain.doFilter(request, responseWrapper)

        if (!response.containsHeader("Content-Disposition")) {
            response.setHeader("Content-Disposition", "attachment; filename=\"api.json\"")
        }

        responseWrapper.copyBodyToResponse()
    }
}
