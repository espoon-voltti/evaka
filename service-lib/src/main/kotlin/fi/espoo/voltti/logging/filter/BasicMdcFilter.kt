// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.logging.filter

import fi.espoo.voltti.logging.MdcKey
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.util.UUID
import javax.servlet.FilterChain
import javax.servlet.http.HttpFilter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class BasicMdcFilter : HttpFilter() {
    override fun doFilter(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val (traceId, spanId) = request.getHeader("x-request-id")?.let {
            Pair(it, randomTracingId())
        } ?: randomTracingId().let { Pair(it, it) }
        MdcKey.TRACE_ID.set(traceId)
        MdcKey.SPAN_ID.set(spanId)
        MdcKey.REQ_IP.set(request.getHeader("x-real-ip") ?: request.remoteAddr)
        try {
            chain.doFilter(request, response)
        } finally {
            MdcKey.REQ_IP.unset()
            MdcKey.SPAN_ID.unset()
            MdcKey.TRACE_ID.unset()
        }
    }
}

// Generates a random 64-bit tracing ID in hex format
private fun randomTracingId(): String = "%016x".format(UUID.randomUUID().leastSignificantBits)
