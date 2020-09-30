// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import com.auth0.jwt.interfaces.Verification
import fi.espoo.voltti.auth.VolttiJwtAuthenticationAdapter
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class VolttiJwtAuthenticationUUIDAdapter : VolttiJwtAuthenticationAdapter {

    override fun configureVerification(verification: Verification): Verification = verification

    override fun configureAuthentication(authentication: Authentication): Authentication {

        val uuid = UUID.fromString(authentication.principal as String)

        return AuthWrapper(authentication, uuid, "")
    }

    class AuthWrapper(
        private val wrapped: Authentication,
        private val principal: UUID,
        private val name: String
    ) : Authentication {

        override fun getName(): String = name

        override fun getAuthorities(): MutableCollection<out GrantedAuthority> = wrapped.authorities

        override fun setAuthenticated(isAuthenticated: Boolean) {
            wrapped.isAuthenticated = isAuthenticated
        }

        override fun getCredentials(): Any = wrapped.credentials

        override fun getPrincipal(): Any = principal

        override fun isAuthenticated(): Boolean = wrapped.isAuthenticated

        override fun getDetails(): Any = wrapped.details
    }
}
