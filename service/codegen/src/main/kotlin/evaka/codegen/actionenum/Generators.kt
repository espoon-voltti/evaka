// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen.actionenum

import evaka.codegen.fileHeader

typealias Generator = () -> String

fun generateFileContents(vararg generators: Generator): String =
    sequenceOf({ fileHeader }, *generators).joinToString(separator = "\n") { it() }

fun generateNamespace(
    name: String,
    vararg generators: Generator
): Generator =
    {
        """
export namespace $name {
${generators.joinToString(separator = "\n", prefix = "\n") { it() }}
}
""".trimStart()
    }

inline fun <reified T : Enum<T>> generateEnum(name: String = T::class.simpleName!!): Generator = generateEnum(name, *enumValues<T>())

fun <T : Enum<T>> generateEnum(
    name: String,
    vararg values: T
): Generator =
    {
        when (values.size) {
            0 -> "export type $name = never\n"
            1 -> "export type $name = ${values[0].tsLiteral()}\n"
            else -> {
                val literals =
                    values
                        .sortedBy { it.name }
                        .joinToString(separator = "\n") { "  | ${it.tsLiteral()}" }
                "export type $name =\n${literals}\n"
            }
        }
    }

private fun <T : Enum<T>> T.tsLiteral() = "'$name'"
