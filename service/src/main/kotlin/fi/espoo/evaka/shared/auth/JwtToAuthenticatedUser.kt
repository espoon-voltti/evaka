// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import fi.espoo.voltti.auth.getDecodedJwt
import fi.espoo.voltti.logging.MdcKey
import javax.servlet.FilterChain
import javax.servlet.http.HttpFilter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

private const val ATTR_USER = "evaka.user"

fun HttpServletRequest.getAuthenticatedUser(): AuthenticatedUser? = getAttribute(ATTR_USER) as AuthenticatedUser?
fun HttpServletRequest.setAuthenticatedUser(user: AuthenticatedUser) = setAttribute(ATTR_USER, user)

class JwtToAuthenticatedUser : HttpFilter() {
    override fun doFilter(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val user = request.getDecodedJwt()?.let { it.toAuthenticatedUser() }

        if (user != null) {
            request.setAuthenticatedUser(user)
            MdcKey.USER_ID.set(user.id.toString())
        }
        try {
            chain.doFilter(request, response)
        } finally {
            MdcKey.USER_ID.unset()
        }
    }
}
