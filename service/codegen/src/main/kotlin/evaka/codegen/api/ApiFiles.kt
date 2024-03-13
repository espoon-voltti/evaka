// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen.api

import evaka.codegen.fileHeader
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.utils.mapOfNotNullValues
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName
import kotlin.reflect.typeOf
import mu.KotlinLogging
import org.springframework.web.util.UriComponentsBuilder

private val logger = KotlinLogging.logger {}

fun generateApiFiles(): Map<TsFile, String> {
    val endpoints =
        scanEndpoints("fi.espoo.evaka")
            .filter { it.isJsonEndpoint }
            .filterNot {
                it.isDeprecated ||
                    it.path.startsWith("/integration") ||
                    it.path.startsWith("/system") ||
                    endpointExcludes.contains(it.path)
            }
    endpoints.forEach { it.validate() }

    val metadata =
        discoverMetadata(
            initial = defaultMetadata,
            rootTypes = endpoints.asSequence().flatMap { it.types() } + forceIncludes.asSequence()
        )

    val devEndpoints =
        scanEndpoints("fi.espoo.evaka", profiles = listOf("enable_dev_api")).filter {
            it.isJsonEndpoint && it.path.startsWith("/dev-api/")
        }
    val devMetadata =
        discoverMetadata(
            initial = defaultMetadata,
            rootTypes = devEndpoints.asSequence().flatMap { it.types() }
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
            .filter { it.path.startsWith("/citizen") || it.path.startsWith("/public") }
            .filter {
                when (it.authenticatedUserType) {
                    typeOf<AuthenticatedUser.Citizen>(),
                    typeOf<AuthenticatedUser>(),
                    null -> true
                    else -> false
                }
            }
            .groupBy {
                TsProject.CitizenFrontend /
                    "generated/api-clients/${getBasePackage(it.controllerClass)}.ts"
            }
            .mapValues { (file, citizenEndpoints) ->
                generateApiClients(
                    generator,
                    file,
                    TsImport.Named(TsProject.CitizenFrontend / "api-client.ts", "client"),
                    citizenEndpoints
                )
            }

    val employeeApiClients =
        endpoints
            .filterNot { it.path.startsWith("/citizen") }
            .filter {
                when (it.authenticatedUserType) {
                    typeOf<AuthenticatedUser.Employee>(),
                    typeOf<AuthenticatedUser>(),
                    null -> true
                    else -> false
                }
            }
            .groupBy {
                TsProject.EmployeeFrontend /
                    "generated/api-clients/${getBasePackage(it.controllerClass)}.ts"
            }
            .mapValues { (file, citizenEndpoints) ->
                generateApiClients(
                    generator,
                    file,
                    TsImport.Named(TsProject.EmployeeFrontend / "api/client.ts", "client"),
                    citizenEndpoints
                )
            }

    val employeeMobileApiClients =
        endpoints
            .filterNot { it.path.startsWith("/citizen") }
            .filter {
                when (it.authenticatedUserType) {
                    typeOf<AuthenticatedUser.MobileDevice>(),
                    typeOf<AuthenticatedUser>(),
                    null -> true
                    else -> false
                }
            }
            .groupBy {
                TsProject.EmployeeMobileFrontend /
                    "generated/api-clients/${getBasePackage(it.controllerClass)}.ts"
            }
            .mapValues { (file, citizenEndpoints) ->
                generateApiClients(
                    generator,
                    file,
                    TsImport.Named(TsProject.EmployeeMobileFrontend / "client.ts", "client"),
                    citizenEndpoints
                )
            }

    val devApiClients =
        (TsProject.E2ETest / "generated/api-clients.ts").let { file ->
            mapOf(
                file to
                    generateApiClients(
                        generator,
                        file,
                        TsImport.Named(TsProject.E2ETest / "dev-api", "devClient"),
                        devEndpoints.map { it.copy(path = it.path.removePrefix("/dev-api")) }
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
    namedTypes: Collection<TsNamedType<*>>
): String {
    val conflicts = namedTypes.groupBy { it.name }.filter { it.value.size > 1 }
    conflicts.forEach { (name, conflictingClasses) ->
        logger.error("Multiple Kotlin classes map to $name: ${conflictingClasses.map { it.name }}")
    }
    if (conflicts.isNotEmpty()) {
        error("${conflicts.size} classes are generated by more than one Kotlin class")
    }
    val tsNamedTypes = namedTypes.map { generator.namedType(it) }
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

fun generateApiClients(
    generator: TsCodeGenerator,
    file: TsFile,
    axiosClient: TsImport,
    endpoints: Collection<EndpointMetadata>,
    wrapBody: (body: TsCode) -> TsCode = { it },
): String {
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
            .sortedWith(
                compareBy(
                    { it.controllerClass.jvmName },
                    { it.controllerMethod.name },
                )
            )
            .map {
                try {
                    generateApiClient(generator, axiosClient, it, wrapBody)
                } catch (e: Exception) {
                    throw RuntimeException(
                        "Failed to generate API client for ${it.controllerClass}.${it.controllerMethod.name}",
                        e
                    )
                }
            }
    val imports = clients.flatMap { it.imports }.toSet()
    val sections = listOf(generateImports(file, imports)) + clients.map { it.text }
    return """$fileHeader
${sections.filter { it.isNotBlank() }.joinToString("\n\n")}
"""
}

fun generateApiClient(
    generator: TsCodeGenerator,
    axiosClient: TsImport,
    endpoint: EndpointMetadata,
    wrapBody: ((functionBody: TsCode) -> TsCode),
): TsCode {
    val argumentType =
        TsObjectLiteral(
                (endpoint.pathVariables + endpoint.requestParameters).associate {
                    it.name to TsProperty(it.type, isOptional = it.type.isMarkedNullable)
                } + mapOfNotNullValues("body" to endpoint.requestBodyType?.let(::TsProperty))
            )
            .takeIf { it.properties.isNotEmpty() }
            ?.let { TsType(it, isNullable = false, typeArguments = emptyList()) }
    val tsArgument =
        if (argumentType != null)
            TsCode {
                "\n" +
                    "request: ${inline(generator.tsType(argumentType, compact = false))}"
                        .prependIndent("  ") +
                    "\n"
            }
        else null

    val pathVariables =
        if (endpoint.pathVariables.isNotEmpty())
            endpoint.pathVariables.associate {
                it.name to
                    generator.serializePathVariable(it.type, TsCode("request.${it.name}")).let {
                        TsCode { "\${${inline(it)}}" }
                    }
            }
        else emptyMap()

    val createQueryParameters =
        if (endpoint.requestParameters.isNotEmpty()) {
            val nameValuePairs =
                endpoint.requestParameters.map { param ->
                    generator.toRequestParamPairs(
                        param.type,
                        param.name,
                        TsCode("request.${param.name}")
                    )
                }
            TsCode {
                """
const params = ${ref(Imports.createUrlSearchParams)}(
${join(nameValuePairs, separator = ",\n").prependIndent("  ")}
)"""
                    .removePrefix("\n")
            }
        } else null

    val url =
        TsCode(
            UriComponentsBuilder.fromPath(endpoint.path)
                .buildAndExpand(pathVariables.mapValues { it.value.text })
                .toUriString(),
            imports = pathVariables.flatMap { it.value.imports }.toSet()
        )

    val tsRequestType =
        endpoint.requestBodyType?.let { generator.tsType(it, compact = true) } ?: TsCode("void")

    val axiosArguments =
        listOfNotNull(
            TsCode { "url: ${ref(Imports.uri)}`${inline(url)}`.toString()" },
            TsCode { "method: '${endpoint.httpMethod}'" },
            createQueryParameters?.let { TsCode { "params" } },
            endpoint.requestBodyType?.let {
                TsCode {
                    "data: request.body satisfies ${ref(Imports.jsonCompatible)}<${inline(tsRequestType)}>"
                }
            }
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
            separator = "\n"
        )

    return TsCode {
        """
/**
* Generated from ${endpoint.controllerClass.qualifiedName ?: endpoint.controllerClass.jvmName}.${endpoint.controllerMethod.name}
*/
export async function ${endpoint.controllerMethod.name}(${inline(tsArgument ?: TsCode(""))}): Promise<${inline(tsResponseType)}> {
${inline(wrapBody(functionBody).prependIndent("  "))}
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
