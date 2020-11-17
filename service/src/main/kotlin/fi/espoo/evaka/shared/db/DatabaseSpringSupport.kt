// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import org.jdbi.v3.core.Jdbi
import org.springframework.context.annotation.Configuration
import org.springframework.core.MethodParameter
import org.springframework.ui.ModelMap
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.context.request.WebRequestInterceptor
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Automatically manages and injects a per-thread Database / Database.Connection into controllers
 * which have one of those as an argument in the method signature.
 */
@Configuration
class DatabaseSpringSupport(private val jdbi: Jdbi) : WebMvcConfigurer {
    private val database: ThreadLocal<Database> = ThreadLocal.withInitial { Database(jdbi) }
    private val connection: ThreadLocal<Database.Connection?> = ThreadLocal()

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addWebRequestInterceptor(object : WebRequestInterceptor {
            override fun preHandle(request: WebRequest) {}
            override fun postHandle(request: WebRequest, model: ModelMap?) {}
            override fun afterCompletion(request: WebRequest, ex: Exception?) = database.remove()
        })
        registry.addWebRequestInterceptor(object : WebRequestInterceptor {
            override fun preHandle(request: WebRequest) {}
            override fun postHandle(request: WebRequest, model: ModelMap?) {}
            override fun afterCompletion(request: WebRequest, ex: Exception?) {
                val c = connection.get()
                connection.remove()
                c?.close()
            }
        })
        super.addInterceptors(registry)
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(object : HandlerMethodArgumentResolver {
            override fun supportsParameter(parameter: MethodParameter): Boolean =
                Database.Connection::class.java.isAssignableFrom(parameter.parameterType)

            override fun resolveArgument(
                parameter: MethodParameter,
                mavContainer: ModelAndViewContainer?,
                webRequest: NativeWebRequest,
                binderFactory: WebDataBinderFactory?
            ) = connection.get() ?: database.get().connect().also(connection::set)
        })
        resolvers.add(object : HandlerMethodArgumentResolver {
            override fun supportsParameter(parameter: MethodParameter): Boolean =
                Database::class.java.isAssignableFrom(parameter.parameterType)

            override fun resolveArgument(
                parameter: MethodParameter,
                mavContainer: ModelAndViewContainer?,
                webRequest: NativeWebRequest,
                binderFactory: WebDataBinderFactory?
            ) = Database(jdbi)
        })
        super.addArgumentResolvers(resolvers)
    }
}
