// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen.apitypes

import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.controllers.Wrapper
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.functions
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.javaMethod
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner
import org.springframework.core.MethodParameter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.context.support.StaticWebApplicationContext
import org.springframework.web.method.annotation.RequestParamMethodArgumentResolver
import org.springframework.web.servlet.mvc.method.annotation.PathVariableMethodArgumentResolver
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor

fun getApiClasses(packageName: String): Set<KClass<*>> {
    fun KType.collectTypes(): Sequence<KClass<*>> =
        when (val classifier = this.classifier) {
            // Include inner type, but ignore the wrapper itself since we don't support type
            // parameters
            // in codegen
            Wrapper::class ->
                this.arguments.asSequence().flatMap { it.type?.collectTypes() ?: emptySequence() }
            // The inner type of Id<*> is irrelevant, because all Ids become UUID/string in frontend
            Id::class -> emptySequence()
            is KClass<*> ->
                sequenceOf(classifier) +
                    this.arguments.asSequence().flatMap {
                        it.type?.collectTypes() ?: emptySequence()
                    }
            else -> error("Unsupported case $this")
        }

    val endpoints = scanEndpoints(packageName)
    return (endpoints.flatMap { endpoint -> endpoint.types().flatMap { it.collectTypes() } } +
            forceIncludes)
        .filter { it.qualifiedName?.startsWith("$packageName.") ?: false }
        .toSet()
}

fun scanEndpoints(packageName: String): List<EndpointMetadata> =
    StaticWebApplicationContext().use { ctx ->
        val scanner = ClassPathBeanDefinitionScanner(ctx)
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
    val path: String,
    val method: RequestMethod,
    val pathVariables: List<NamedParameter>,
    val requestParameters: List<NamedParameter>,
    val requestBodyType: KType?,
    val responseBodyType: KType?
) {
    fun types(): Sequence<KType> =
        pathVariables.asSequence().map { it.type } +
            requestParameters.asSequence().map { it.type } +
            listOfNotNull(requestBodyType, responseBodyType)

    fun validate() {
        fun fail(reason: String): Nothing =
            error("Invalid $method endpoint $path in $controllerClass: $reason")
        when (method) {
            RequestMethod.GET,
            RequestMethod.HEAD,
            RequestMethod.DELETE, -> {
                if (method == RequestMethod.GET && responseBodyType == null) {
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
    }
}

data class NamedParameter(val name: String, val type: KType)

private fun RequestMappingHandlerMapping.getEndpointMetadata(): List<EndpointMetadata> {
    fun KFunction<*>.find(param: MethodParameter): KParameter =
        valueParameters[param.parameterIndex]

    val pathSupport = PathVariableMethodArgumentResolver()
    val paramSupport = RequestParamMethodArgumentResolver(true)
    val bodySupport = RequestResponseBodyMethodProcessor(listOf(StringHttpMessageConverter()))
    return handlerMethods
        .asSequence()
        .flatMap { (info, method) ->
            val controllerClass = method.beanType.kotlin
            val kotlinMethod = controllerClass.functions.find { it.javaMethod == method.method }!!
            val pathVariables =
                method.methodParameters
                    .filter { pathSupport.supportsParameter(it) }
                    .map { param ->
                        val kotlinParam = kotlinMethod.find(param)
                        val name =
                            param.getParameterAnnotation(PathVariable::class.java)?.name?.takeIf {
                                it.isNotBlank()
                            } ?: kotlinParam.name!!
                        NamedParameter(name = name, type = kotlinParam.type)
                    }
            val requestParameters =
                method.methodParameters
                    .filter { paramSupport.supportsParameter(it) }
                    .mapNotNull { param ->
                        val kotlinParam = kotlinMethod.find(param)
                        val name =
                            param.getParameterAnnotation(RequestParam::class.java)?.name?.takeIf {
                                it.isNotBlank()
                            } ?: kotlinParam.name!!
                        NamedParameter(name = name, type = kotlinParam.type)
                    }
            val requestBodyType =
                method.methodParameters
                    .firstOrNull { bodySupport.supportsParameter(it) }
                    ?.let { kotlinMethod.find(it).type }
            val responseBodyType =
                if (!method.isVoid && bodySupport.supportsReturnType(method.returnType)) {
                    kotlinMethod.returnType
                } else {
                    null
                }
            val paths = info.patternValues
            val methods = info.methodsCondition.methods
            paths
                .flatMap { path -> methods.map { method -> Pair(path, method) } }
                .map { (path, method) ->
                    EndpointMetadata(
                        controllerClass = controllerClass,
                        path = path,
                        method = method,
                        pathVariables = pathVariables,
                        requestParameters = requestParameters,
                        requestBodyType = requestBodyType,
                        responseBodyType = responseBodyType
                    )
                }
        }
        .toList()
}
