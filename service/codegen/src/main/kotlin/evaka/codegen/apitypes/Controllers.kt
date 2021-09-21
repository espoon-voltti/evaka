// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen.apitypes

import fi.espoo.evaka.ExcludeCodeGen
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.div
import kotlin.jvm.internal.Reflection
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

fun getApiClasses(packageName: String): Set<KClass<*>> {
    return scanClassPath(packageName)
        .flatMap { scanController(packageName, it) }
        .filter { it.annotations.none { it.annotationClass == ExcludeCodeGen::class } }
        .toSet()
}

private fun scanClassPath(packageName: String): Set<KClass<Any>> {
    val workingDir = Path("")
    val path = (workingDir / "build/classes/kotlin/main/${packageName.replace('.', '/')}").absolute().normalize()
    val directory = File(path.toUri())

    if (directory.exists()) {
        // Get the list of the files contained in the package
        return directory.walk()
            .filter { f -> f.isFile() && f.name.contains('$') == false && f.name.endsWith(".class") && !f.name.endsWith("Kt.class") }
            .map {
                val fullyQualifiedClassName = packageName +
                    it.canonicalPath.removePrefix(directory.canonicalPath)
                        .dropLast(6) // remove .class
                        .replace('/', '.')
                        .replace('\\', '.')

                Reflection.createKotlinClass(Class.forName(fullyQualifiedClassName))
            }
            .toSet()
    } else error("classes not found")
}

private fun <T : Any> scanController(packageName: String, clazz: KClass<T>): Set<KClass<*>> {
    if (clazz.annotations.none { it.annotationClass == Controller::class || it.annotationClass == RestController::class })
        return emptySet()

    if (clazz.annotations.any { it.annotationClass == ExcludeCodeGen::class })
        return emptySet()

    return clazz.declaredMemberFunctions
        .flatMap { scanEndpoint(it) }
        .filter { it.qualifiedName!!.startsWith(packageName) }
        .toSet()
}

private fun scanEndpoint(func: KFunction<*>): Set<KClass<*>> {
    return when {
        func.annotations.any { it.annotationClass == GetMapping::class } ->
            setOfNotNull(getResponseBodyClass(func))
        func.annotations.any { it.annotationClass in listOf(PutMapping::class, PostMapping::class) } ->
            setOfNotNull(getRequestBodyClass(func), getResponseBodyClass(func))
        else -> emptySet()
    }
}

private fun getRequestBodyClass(func: KFunction<*>): KClass<*>? {
    return func.parameters
        .find { param -> param.annotations.any { it.annotationClass == RequestBody::class } }
        ?.type
        ?.let { getRealType(it) }
}

private fun getResponseBodyClass(func: KFunction<*>): KClass<*>? {
    return getRealType(func.returnType)
}

private fun getRealType(type: KType): KClass<*>? {
    var t = type
    while (unwrap(t) != t) {
        t = unwrap(t)
    }
    return t.jvmErasure
}

private fun unwrap(type: KType): KType {
    return (
        if (classesToUnwrap.any { type.jvmErasure.isSubclassOf(it) }) {
            type.arguments.first().type!!
        } else type
        )
}
