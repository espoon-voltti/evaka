// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen.api

import evaka.codegen.fileHeader
import fi.espoo.evaka.shared.utils.letIf
import fi.espoo.evaka.shared.utils.mapOfNotNullValues
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName
import org.springframework.web.util.UriComponentsBuilder

private val logger = KotlinLogging.logger {}

fun generateApiFiles(): Map<TsFile, String> {
    val allEndpoints = scanEndpoints("fi.espoo.evaka")
    allEndpoints.forEach { it.validate() }

    val endpoints =
        allEndpoints
            .filter { it.type == EndpointType.Json || it.type == EndpointType.PlainGet }
            .filterNot {
                it.isDeprecated ||
                    it.path.startsWith("/integration/") ||
                    it.path.startsWith("/system/") ||
                    endpointExcludes.contains(it.path)
            }

    val metadata =
        discoverMetadata(
            initial = defaultMetadata,
            rootTypes = endpoints.asSequence().flatMap { it.types() } + forceIncludes.asSequence(),
        )

    val devEndpoints =
        scanEndpoints("fi.espoo.evaka", profiles = listOf("enable_dev_api")).filter {
            it.type == EndpointType.Json && it.path.startsWith("/dev-api/")
        }
    val devMetadata =
        discoverMetadata(
            initial = defaultMetadata,
            rootTypes = devEndpoints.asSequence().flatMap { it.types() },
        )

    val generator =
        object : TsCodeGenerator(metadata + devMetadata) {
            override fun locateNamedType(namedType: TsNamedType<*>): TsFile =
                when (namedType.clazz) {
                    in metadata ->
                        TsProject.LibCommon /
                            "generated/api-types/${getBasePackage(namedType.clazz)}.ts"
                    in devMetadata -> TsProject.E2ETest / "generated/api-types.ts"
                    else -> error("Unexpected type $namedType")
                }
        }

    val apiTypes =
        generator
            .namedTypes()
            .groupBy { generator.locateNamedType(it) }
            .mapValues { (file, namedTypes) ->
                generateApiTypes(file, generator, namedTypes.sortedBy { it.name })
            }

    val citizenApiClients =
        endpoints
            .filter { it.path.startsWith("/citizen/") }
            .groupBy {
                TsProject.CitizenFrontend /
                    "generated/api-clients/${getBasePackage(it.controllerClass)}.ts"
            }
            .mapValues { (file, citizenEndpoints) ->
                generateApiClients(
                    ApiClientConfig(mockedTimeSupport = false),
                    generator,
                    file,
                    TsImport.Named(TsProject.CitizenFrontend / "api-client.ts", "client"),
                    citizenEndpoints,
                )
            }

    val employeeApiClients =
        endpoints
            .filter { it.path.startsWith("/employee/") }
            .groupBy {
                TsProject.EmployeeFrontend /
                    "generated/api-clients/${getBasePackage(it.controllerClass)}.ts"
            }
            .mapValues { (file, citizenEndpoints) ->
                generateApiClients(
                    ApiClientConfig(mockedTimeSupport = false),
                    generator,
                    file,
                    TsImport.Named(TsProject.EmployeeFrontend / "api/client.ts", "client"),
                    citizenEndpoints,
                )
            }

    val employeeMobileApiClients =
        endpoints
            .filter { it.path.startsWith("/employee-mobile/") }
            .groupBy {
                TsProject.EmployeeMobileFrontend /
                    "generated/api-clients/${getBasePackage(it.controllerClass)}.ts"
            }
            .mapValues { (file, citizenEndpoints) ->
                generateApiClients(
                    ApiClientConfig(mockedTimeSupport = false),
                    generator,
                    file,
                    TsImport.Named(TsProject.EmployeeMobileFrontend / "client.ts", "client"),
                    citizenEndpoints,
                )
            }

    val devApiClients =
        (TsProject.E2ETest / "generated/api-clients.ts").let { file ->
            mapOf(
                file to
                    generateApiClients(
                        ApiClientConfig(mockedTimeSupport = true),
                        generator,
                        file,
                        TsImport.Named(TsProject.E2ETest / "dev-api", "devClient"),
                        devEndpoints.map { it.copy(path = it.path.removePrefix("/dev-api")) },
                    ) { body: TsCode ->
                        TsCode {
                            """
try {
${inline(body).prependIndent("  ")}
} catch (e) {
  throw new ${ref(Imports.devApiError)}(e)
}"""
                                .removePrefix("\n")
                        }
                    }
            )
        }

    return apiTypes +
        citizenApiClients +
        employeeApiClients +
        employeeMobileApiClients +
        devApiClients
}

fun generateApiTypes(
    file: TsFile,
    generator: TsCodeGenerator,
    namedTypes: Collection<TsNamedType<*>>,
): String {
    val conflicts = namedTypes.groupBy { it.name }.filter { it.value.size > 1 }
    conflicts.forEach { (name, conflictingClasses) ->
        logger.error {
            "Multiple Kotlin classes map to $name: ${conflictingClasses.map { it.name }}"
        }
    }
    if (conflicts.isNotEmpty()) {
        error("${conflicts.size} classes are generated by more than one Kotlin class")
    }
    val tsNamedTypes =
        namedTypes.map { generator.namedType(it) } +
            namedTypes
                .mapNotNull {
                    idTypeAliases[it.name]?.let { aliases ->
                        generator.typeAliases(it.name, aliases)
                    }
                }
                .flatten()
    val deserializers = namedTypes.mapNotNull { generator.jsonDeserializer(it) }
    val imports =
        tsNamedTypes.flatMap { it.imports }.toSet() + deserializers.flatMap { it.imports }.toSet()
    val sections =
        listOf(generateImports(file, imports)) +
            tsNamedTypes.map { it.text } +
            deserializers.map { it.text }
    return """$fileHeader
${sections.filter { it.isNotBlank() }.joinToString("\n\n")}
"""
        .lineSequence()
        .map { it.trimEnd() }
        .joinToString("\n")
}

fun generateImports(currentFile: TsFile, imports: Iterable<TsImport>): String =
    imports
        .filterNot { it.file == currentFile }
        .sortedWith(compareBy({ it is TsImport.Named }, { it.name }))
        .joinToString("\n") { import ->
            val path = import.file.importFrom(currentFile)
            when (import) {
                is TsImport.Default -> "import ${import.name} from '$path'"
                is TsImport.NamedAs ->
                    "import { ${import.originalName} as ${import.name} } from '$path'"
                is TsImport.Named -> "import { ${import.name} } from '$path'"
            }
        }

data class ApiClientConfig(val mockedTimeSupport: Boolean)

fun generateApiClients(
    config: ApiClientConfig,
    generator: TsCodeGenerator,
    file: TsFile,
    axiosClient: TsImport,
    endpoints: Collection<EndpointMetadata>,
    wrapBody: (body: TsCode) -> TsCode = { it },
): String {
    val mockedTimeSupport = config.mockedTimeSupport && endpoints.any { it.hasClockParameter }
    val clients =
        endpoints
            .groupBy { Pair(it.controllerClass, it.controllerMethod) }
            .values
            .map { duplicates ->
                require(duplicates.size == 1) {
                    "Endpoint conflict:\n${duplicates.joinToString("\n")}"
                }
                duplicates.single()
            }
            .sortedWith(compareBy({ it.controllerClass.jvmName }, { it.controllerMethod.name }))
            .mapNotNull {
                try {
                    if (it.type == EndpointType.Json)
                        generateJsonApiClient(config, generator, axiosClient, it, wrapBody)
                    else if (it.type == EndpointType.PlainGet)
                        generatePlainGetApiFunction(
                            generator,
                            TsCode { "${ref(axiosClient)}.defaults.baseURL ?? ''" },
                            it,
                        )
                    else null
                } catch (e: Exception) {
                    throw RuntimeException(
                        "Failed to generate API client for ${it.controllerClass}.${it.controllerMethod.name}",
                        e,
                    )
                }
            }
    val imports =
        clients
            .flatMap { it.imports }
            .toSet()
            .letIf(mockedTimeSupport) { it + Imports.helsinkiDateTime }
    val sections = listOf(generateImports(file, imports)) + clients.map { it.text }
    return """$fileHeader
${sections.filter { it.isNotBlank() }.joinToString("\n\n")}
"""
}

/**
 * Generates TS code that returns a Uri object built from the endpoint path with all path variables
 * inserted.
 */
private fun TsCodeGenerator.tsBuildUri(
    pathPrefix: TsCode?,
    endpoint: EndpointMetadata,
    // Function must return a TS expression that evaluates to the value of the given path variable
    tsPathVariableValue: (pathVariable: String) -> TsCode,
): TsTypedExpression {
    val tsPathVariables =
        endpoint.pathVariables.associate {
            it.name to
                serializePathVariable(
                    type = it.toOptionalType(),
                    valueExpression = tsPathVariableValue(it.name),
                )
        }
    val uriTemplateString =
        TsCode(
            // Use Spring to replace path variables with TS expressions
            // For example, /employees/:id/delete might become /employees/${request.id}/delete
            UriComponentsBuilder.fromPath(endpoint.path)
                .buildAndExpand(tsPathVariables.mapValues { "\${${it.value.text}}" })
                .toUriString(),
            imports = tsPathVariables.flatMap { it.value.imports }.toSet(),
        )
    // Example of returned code: uri`/employees/${request.id}/delete`
    return TsTypedExpression(
        type = TsCode { ref(Imports.uriType) },
        value =
            TsCode {
                "${ref(Imports.uri)}`${if (pathPrefix != null) "\${${inline(pathPrefix)}}" else ""}${inline(uriTemplateString)}`"
            },
    )
}

/** Generates TS code that returns a URLSearchParams object built from all query parameter values */
private fun TsCodeGenerator.tsBuildQueryParams(
    parameters: Collection<NamedParameter>,
    // Function must return a TS expression that evaluates to the value of the given query parameter
    tsQueryParamValue: (queryParam: String) -> TsCode,
): TsTypedExpression {
    val nameValuePairs =
        parameters.map { param ->
            toRequestParamPairs(param.toOptionalType(), param.name, tsQueryParamValue(param.name))
        }
    return TsTypedExpression(
        type = TsCode("URLSearchParams"),
        value =
            TsCode {
                """
${ref(Imports.createUrlSearchParams)}(
${join(nameValuePairs, separator = ",\n").prependIndent("  ")}
)"""
                    .removePrefix("\n")
            },
    )
}

fun generateJsonApiClient(
    config: ApiClientConfig,
    generator: TsCodeGenerator,
    axiosClient: TsImport,
    endpoint: EndpointMetadata,
    wrapBody: ((functionBody: TsCode) -> TsCode),
): TsCode {
    val mockedTimeSupport = config.mockedTimeSupport && endpoint.hasClockParameter
    val argumentType =
        TsObjectLiteral(
                (endpoint.pathVariables + endpoint.requestParameters).associate {
                    it.name to TsProperty(it.toOptionalType())
                } + mapOfNotNullValues("body" to endpoint.requestBodyType?.let(::TsProperty))
            )
            .takeIf { it.properties.isNotEmpty() }
            ?.let { TsType(it, isNullable = false, typeArguments = emptyList()) }

    val tsArgument = TsCode {
        listOfNotNull(
                if (argumentType != null)
                    "request: ${inline(generator.tsType(argumentType, compact = false))}"
                        .prependIndent("  ")
                else null,
                if (mockedTimeSupport) "  options?: { mockedTime?: HelsinkiDateTime }" else null,
            )
            .let { params ->
                if (params.isNotEmpty())
                    params.joinToString(separator = ",\n", prefix = "\n", postfix = "\n")
                else ""
            }
    }

    val getRequestProperty = { name: String -> TsCode { "request.$name" } }
    val url = generator.tsBuildUri(pathPrefix = null, endpoint, getRequestProperty)
    val createQueryParameters =
        if (endpoint.requestParameters.isNotEmpty())
            TsCode {
                "const params = ${inline(generator.tsBuildQueryParams(endpoint.requestParameters, getRequestProperty).value)}"
            }
        else null

    val tsRequestType =
        endpoint.requestBodyType?.let { generator.tsType(it, compact = true) } ?: TsCode("void")

    val axiosArguments =
        listOfNotNull(
            TsCode { "url: ${inline(url.value)}.toString()" },
            TsCode { "method: '${endpoint.httpMethod}'" },
            if (mockedTimeSupport)
                TsCode { "headers: { EvakaMockedTime: options?.mockedTime?.formatIso() }" }
            else null,
            createQueryParameters?.let { TsCode { "params" } },
            endpoint.requestBodyType?.let {
                TsCode {
                    "data: request.body satisfies ${ref(Imports.jsonCompatible)}<${inline(tsRequestType)}>"
                }
            },
        )

    val tsResponseType =
        endpoint.responseBodyType?.let { generator.tsType(it, compact = true) } ?: TsCode("void")

    val responseDeserializer =
        endpoint.responseBodyType?.let { generator.jsonDeserializerExpression(it, TsCode("json")) }

    val functionBody =
        TsCode.join(
            listOfNotNull(
                createQueryParameters,
                TsCode {
                    """
const { data: json } = await ${ref(axiosClient)}.request<${ref(Imports.jsonOf)}<${inline(tsResponseType)}>>({
${join(axiosArguments, ",\n").prependIndent("  ")}
})"""
                        .removePrefix("\n")
                },
                TsCode { "return ${inline(responseDeserializer ?: TsCode("json"))}" },
            ),
            separator = "\n",
        )

    return TsCode {
        """
/**
* Generated from ${endpoint.controllerClass.qualifiedName ?: endpoint.controllerClass.jvmName}.${endpoint.controllerMethod.name}
*/
export async function ${endpoint.controllerMethod.name}(${inline(tsArgument)}): Promise<${inline(tsResponseType)}> {
${inline(wrapBody(functionBody).prependIndent("  "))}
}"""
    }
}

fun generatePlainGetApiFunction(
    generator: TsCodeGenerator,
    pathPrefix: TsCode,
    endpoint: EndpointMetadata,
): TsCode {
    val argumentType =
        TsObjectLiteral(
                (endpoint.pathVariables + endpoint.requestParameters).associate {
                    it.name to TsProperty(it.toOptionalType())
                }
            )
            .takeIf { it.properties.isNotEmpty() }
            ?.let { TsType(it, isNullable = false, typeArguments = emptyList()) }

    val tsArgument = TsCode {
        listOfNotNull(
                if (argumentType != null)
                    "request: ${inline(generator.tsType(argumentType, compact = false))}"
                        .prependIndent("  ")
                else null
            )
            .let { params ->
                if (params.isNotEmpty())
                    params.joinToString(separator = ",\n", prefix = "\n", postfix = "\n")
                else ""
            }
    }

    val getRequestProperty = { name: String -> TsCode { "request.$name" } }
    val url = generator.tsBuildUri(pathPrefix, endpoint, getRequestProperty)
    val createQueryParameters =
        if (endpoint.requestParameters.isNotEmpty())
            TsCode {
                "const params = ${inline(generator.tsBuildQueryParams(endpoint.requestParameters, getRequestProperty).value)}"
            }
        else null

    val functionBody =
        TsCode.join(
            listOfNotNull(
                createQueryParameters,
                TsCode {
                    """
return {
  url: ${inline(url.value)}${if (createQueryParameters != null) ".appendQuery(params)" else ""}
}"""
                        .removePrefix("\n")
                },
            ),
            separator = "\n",
        )
    return TsCode {
        """
/**
* Generated from ${endpoint.controllerClass.qualifiedName ?: endpoint.controllerClass.jvmName}.${endpoint.controllerMethod.name}
*/
export function ${endpoint.controllerMethod.name}(${inline(tsArgument)}): { url: ${inline(url.type)} } {
${inline(functionBody.prependIndent("  "))}
}"""
    }
}

private fun getBasePackage(clazz: KClass<*>): String {
    val pkg = clazz.jvmName.substringBeforeLast('.')
    val relativePackage =
        when {
            pkg == basePackage -> return "base"
            pkg.startsWith("$basePackage.") -> pkg.substring(basePackage.length + 1)
            else -> error("class not under base package")
        }
    return relativePackage.substringBefore('.')
}
