// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import fi.espoo.evaka.shared.domain.Unauthorized
import org.springframework.context.annotation.Configuration
import org.springframework.core.MethodParameter
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.util.UUID

@Configuration
class AuthenticatedUserSpringSupport : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(object : HandlerMethodArgumentResolver {
            override fun supportsParameter(parameter: MethodParameter): Boolean =
                AuthenticatedUser::class.java.isAssignableFrom(parameter.parameterType)

            override fun resolveArgument(
                parameter: MethodParameter,
                mavContainer: ModelAndViewContainer?,
                webRequest: NativeWebRequest,
                binderFactory: WebDataBinderFactory?
            ) = springSecurityThreadLocalUser() ?: if (parameter.isOptional) null else throw Unauthorized(
                "Unauthorized request (${webRequest.getDescription(
                    false
                )})"
            )
        })
        super.addArgumentResolvers(resolvers)
    }
}

fun springSecurityThreadLocalUser(): AuthenticatedUser? =
    SecurityContextHolder.getContext()?.authentication?.let { it.toAuthenticatedUser() }

fun Authentication.toAuthenticatedUser(): AuthenticatedUser? = (principal as? UUID)?.let { id ->
    AuthenticatedUser(
        id,
        authorities.map { UserRole.parse(it.authority) }.toSet()
    )
}
