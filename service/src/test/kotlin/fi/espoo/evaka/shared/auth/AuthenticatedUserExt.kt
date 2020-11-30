// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.request.RequestPostProcessor
import java.util.UUID

private val employeeId = UUID.fromString("00000000-0000-0000-0000-000000000000")

fun AuthenticatedUser.Companion.serviceWorker(id: UUID = employeeId) =
    AuthenticatedUser(id, setOf(UserRole.SERVICE_WORKER))

fun AuthenticatedUser.Companion.unitSupervisor(id: UUID = employeeId) =
    AuthenticatedUser(id, setOf(UserRole.UNIT_SUPERVISOR))

fun AuthenticatedUser.Companion.financeAdmin(id: UUID = employeeId) =
    AuthenticatedUser(id, setOf(UserRole.FINANCE_ADMIN))

fun AuthenticatedUser.Companion.endUser(id: UUID = UUID.randomUUID()) =
    AuthenticatedUser(id, setOf(UserRole.END_USER))

fun AuthenticatedUser.mockMvcAuthentication(): RequestPostProcessor =
    SecurityMockMvcRequestPostProcessors.authentication(object : Authentication {
        override fun getAuthorities(): MutableCollection<out GrantedAuthority> =
            roles.map { SimpleGrantedAuthority("ROLE_$it") }.toMutableList()

        override fun setAuthenticated(isAuthenticated: Boolean) {
            throw UnsupportedOperationException()
        }

        override fun getName(): String = id.toString()
        override fun getCredentials(): Any? = null
        override fun getPrincipal(): Any = id
        override fun isAuthenticated(): Boolean = true
        override fun getDetails(): Any? = null
    })
