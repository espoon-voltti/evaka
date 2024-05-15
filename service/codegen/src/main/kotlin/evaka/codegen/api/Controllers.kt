// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen.api

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.typeOf
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner
import org.springframework.core.MethodParameter
import org.springframework.core.env.StandardEnvironment
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.context.support.StaticWebApplicationContext
import org.springframework.web.method.annotation.RequestParamMethodArgumentResolver
import org.springframework.web.servlet.mvc.method.annotation.PathVariableMethodArgumentResolver
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor

/** Scans all REST endpoints using Spring, returning metadata about all of them */
fun scanEndpoints(
    packageName: String,
    profiles: List<String> = emptyList()
): List<EndpointMetadata> =
    StaticWebApplicationContext().use { ctx ->
        val env = StandardEnvironment()
        env.setActiveProfiles(*profiles.toTypedArray())
        val scanner = ClassPathBeanDefinitionScanner(ctx, true, env)
        scanner.scan(packageName)
        ctx.getEndpointMetadata()
    }

fun ApplicationContext.getEndpointMetadata(): List<EndpointMetadata> =
    RequestMappingHandlerMapping()
        .also { mapping ->
            mapping.applicationContext = this
            mapping.afterPropertiesSet()
        }
        .getEndpointMetadata()

data class EndpointMetadata(
    val controllerClass: KClass<*>,
    val controllerMethod: KFunction<*>,
    val isJsonEndpoint: Boolean,
    val isDeprecated: Boolean,
    val path: String,
    val httpMethod: RequestMethod,
    val pathVariables: List<NamedParameter>,
    val requestParameters: List<NamedParameter>,
    val requestBodyType: KType?,
    val responseBodyType: KType?,
    val authenticatedUserType: KType?,
) {
    fun types(): Sequence<KType> =
        pathVariables.asSequence().map { it.type } +
            requestParameters.asSequence().map { it.type } +
            listOfNotNull(requestBodyType, responseBodyType)

    fun validate() {
        fun fail(reason: String): Nothing =
            error("Invalid $httpMethod endpoint $path in $controllerClass: $reason")

        requestParameters.forEach {
            if (!it.isOptional && it.type.isSubtypeOf(typeOf<Collection<*>>())) {
                fail("Collection request params must be optional (add default to empty list)")
            }
        }

        when (httpMethod) {
            RequestMethod.GET,
            RequestMethod.HEAD,
            RequestMethod.DELETE, -> {
                if (isJsonEndpoint && httpMethod == RequestMethod.GET && responseBodyType == null) {
                    fail("It should have a response body")
                }
                if (requestBodyType != null) {
                    fail("It should not use a request body")
                }
            }
            RequestMethod.POST,
            RequestMethod.PUT,
            RequestMethod.PATCH -> {}
            RequestMethod.TRACE,
            RequestMethod.OPTIONS -> fail("Method not supported")
        }

        when {
            path.startsWith("/citizen/public/") ||
                path.startsWith("/employee/public/") ||
                path.startsWith("/employee-mobile/public/") ->
                if (authenticatedUserType != null) {
                    fail("It must not include any AuthenticatedUser as a parameter")
                }
            path.startsWith("/system/") ->
                if (authenticatedUserType != typeOf<AuthenticatedUser.SystemInternalUser>()) {
                    fail("It must include an AuthenticatedUser.SystemInternalUser parameter")
                }
            path.startsWith("/integration/") ->
                if (authenticatedUserType != typeOf<AuthenticatedUser.Integration>()) {
                    fail("It must include an AuthenticatedUser.Integration parameter")
                }
            path.startsWith("/citizen/") ->
                if (authenticatedUserType != typeOf<AuthenticatedUser.Citizen>()) {
                    fail("It must include an AuthenticatedUser.Citizen parameter")
                }
            path.startsWith("/employee/") ->
                if (authenticatedUserType != typeOf<AuthenticatedUser.Employee>()) {
                    fail("It must include an AuthenticatedUser.Employee parameter")
                }
            path.startsWith("/employee-mobile/") ->
                if (authenticatedUserType != typeOf<AuthenticatedUser.MobileDevice>()) {
                    fail("It must include an AuthenticatedUser.MobileDevice parameter")
                }
            else -> {} // TODO: legacy endpoints
        }
    }
}

data class NamedParameter(val name: String, val type: KType, val isOptional: Boolean) {
    /**
     * Returns a type that represents both the nullability and optionality of the parameter type.
     *
     * This can be different from the actual parameter type, because a Kotlin method can have a
     * non-nullable parameter with a default value. In this case `type` would not be marked nullable
     * but the type returned by `toOptionalType()` would be.
     */
    fun toOptionalType() =
        type.classifier!!.createType(
            type.arguments,
            type.isMarkedNullable || isOptional,
            type.annotations
        )
}

private fun RequestMappingHandlerMapping.getEndpointMetadata(): List<EndpointMetadata> {
    fun KFunction<*>.find(param: MethodParameter): KParameter =
        valueParameters[param.parameterIndex]
    fun <A : Annotation> KFunction<*>.findAnnotatedParameter(
        annotation: KClass<A>,
        getName: (A) -> String?,
        isRequired: (A) -> Boolean,
        param: MethodParameter
    ): NamedParameter {
        val kotlinParam = find(param)
        val paramAnnotation = param.getParameterAnnotation(annotation.java)
        return NamedParameter(
            name = paramAnnotation?.let(getName)?.takeIf { it.isNotBlank() } ?: kotlinParam.name!!,
            type = kotlinParam.type,
            isOptional = !(paramAnnotation?.let(isRequired) ?: true) || kotlinParam.isOptional
        )
    }

    val pathSupport = PathVariableMethodArgumentResolver()
    val paramSupport = RequestParamMethodArgumentResolver(false)
    val bodySupport = RequestResponseBodyMethodProcessor(listOf(StringHttpMessageConverter()))
    return handlerMethods
        .asSequence()
        .flatMap { (info, method) ->
            val controllerClass = method.beanType.kotlin
            val kotlinMethod = controllerClass.functions.find { it.javaMethod == method.method }!!
            val authenticatedUserType =
                kotlinMethod.parameters
                    .singleOrNull { it.type.isSubtypeOf(typeOf<AuthenticatedUser>()) }
                    ?.type
            val pathVariables =
                method.methodParameters
                    .filter { pathSupport.supportsParameter(it) }
                    .mapNotNull { param ->
                        kotlinMethod.findAnnotatedParameter(
                            PathVariable::class,
                            { it.name },
                            { it.required },
                            param
                        )
                    }
            val requestParameters =
                method.methodParameters
                    .filter { paramSupport.supportsParameter(it) }
                    .mapNotNull { param ->
                        kotlinMethod.findAnnotatedParameter(
                            RequestParam::class,
                            { it.name },
                            { it.required },
                            param
                        )
                    }
            val requestBodyType =
                method.methodParameters
                    .firstOrNull { bodySupport.supportsParameter(it) }
                    ?.let { kotlinMethod.find(it).type }
            val responseBodyType =
                if (!method.isVoid && bodySupport.supportsReturnType(method.returnType)) {
                    if (kotlinMethod.returnType.classifier == ResponseEntity::class) {
                        kotlinMethod.returnType.arguments.single().type
                    } else kotlinMethod.returnType
                } else null
            val paths = info.directPaths + info.patternValues
            val methods = info.methodsCondition.methods
            val consumesJson =
                info.consumesCondition.isEmpty ||
                    info.consumesCondition.consumableMediaTypes.contains(MediaType.APPLICATION_JSON)
            val producesJson =
                info.producesCondition.isEmpty ||
                    info.producesCondition.producibleMediaTypes.contains(MediaType.APPLICATION_JSON)
            paths
                .flatMap { path -> methods.map { method -> Pair(path, method) } }
                .map { (path, method) ->
                    EndpointMetadata(
                        controllerClass = controllerClass,
                        controllerMethod = kotlinMethod,
                        isJsonEndpoint =
                            consumesJson && producesJson && responseBodyType != typeOf<Any>(),
                        isDeprecated = kotlinMethod.hasAnnotation<Deprecated>(),
                        path = path,
                        httpMethod = method,
                        pathVariables = pathVariables,
                        requestParameters = requestParameters,
                        requestBodyType = requestBodyType,
                        responseBodyType = responseBodyType,
                        authenticatedUserType = authenticatedUserType,
                    )
                }
        }
        .toList()
}
