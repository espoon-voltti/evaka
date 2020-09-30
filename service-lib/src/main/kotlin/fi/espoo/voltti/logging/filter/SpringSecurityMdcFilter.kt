// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.logging.filter

import fi.espoo.voltti.logging.MdcKey
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse

@Component
class SpringSecurityMdcFilter : Filter {
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val authentication = SecurityContextHolder.getContext().authentication
        assert(authentication != null) { "Spring Security must be initialized" }
        MdcKey.USER_ID.set(authentication.principal.toString())
        try {
            chain.doFilter(request, response)
        } finally {
            MdcKey.USER_ID.unset()
        }
    }
}
