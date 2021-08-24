// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen

import kotlin.reflect.KClass

val fileHeader = """
// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable prettier/prettier */
""".trimStart()

typealias Generator = () -> String

fun generateFileContents(vararg generators: Generator): String =
    sequenceOf({ fileHeader }, *generators).joinToString(separator = "\n") { it() }

fun generateNamespace(name: String, vararg generators: Generator): Generator = {
    """
export namespace $name {
${generators.joinToString(separator = "\n", prefix = "\n") { it() }}
}
""".trimStart()
}

inline fun <reified T : Enum<T>> generateEnum(name: String = T::class.simpleName!!): Generator =
    generateEnum(T::class, name)

fun <T : Enum<T>> generateEnum(clazz: KClass<T>, name: String = clazz.simpleName!!): Generator =
    generateEnum(name, *clazz.java.enumConstants)

fun <T : Enum<T>> generateEnum(name: String, vararg values: T): Generator = {
    when (values.size) {
        0 -> "export type $name = never\n"
        1 -> "export type $name = ${values[0].tsLiteral()}\n"
        else -> {
            val literals = values.sortedBy { it.name }.joinToString(separator = "\n") { "  | ${it.tsLiteral()}" }
            "export type $name =\n${literals}\n"
        }
    }
}

private fun <T : Enum<T>> T.tsLiteral() = "'$name'"
