// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen.api

import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.relativeTo

enum class TsProject {
    LibCommon,
    CitizenFrontend,
    EmployeeFrontend,
    EmployeeMobileFrontend,
    E2ETest;

    operator fun div(path: String): TsFile = TsFile(this, Path.of(path))

    fun absoluteImportPath(path: Path): Path =
        when (this) {
            LibCommon -> Path.of("lib-common") / path
            CitizenFrontend -> Path.of("citizen-frontend") / path
            EmployeeFrontend -> Path.of("employee-frontend") / path
            EmployeeMobileFrontend -> Path.of("employee-mobile-frontend") / path
            E2ETest -> Path.of("e2e-test") / path
        }
}

/** Path to a TS file (generated or existing) */
data class TsFile(val project: TsProject, val path: Path) {
    fun importFrom(other: TsFile): String =
        when {
                this.project == other.project ->
                    path.relativeTo(other.path.parent).let {
                        if (it.fileName == it) Path.of("./$it") else it
                    }
                else -> this.project.absoluteImportPath(path)
            }
            .toString()
            .removeSuffix(".d.ts")
            .removeSuffix(".tsx")
            .removeSuffix(".ts")
}

/** Representation of a TS import. */
sealed interface TsImport {
    /** File that exports the name that is to be imported */
    val file: TsFile
    /** The local name for the imported thing */
    val name: String

    data class Default(override val file: TsFile, override val name: String) : TsImport

    data class Named(override val file: TsFile, override val name: String) : TsImport

    data class NamedAs(
        override val file: TsFile,
        val originalName: String,
        override val name: String,
    ) : TsImport
}

/**
 * A fragment of TS code carrying also information about names that need to be imported from other
 * modules.
 */
data class TsCode(val text: String, val imports: Set<TsImport>) {
    constructor(text: String, vararg imports: TsImport) : this(text, imports.toSet())

    constructor(import: TsImport) : this(import.name, setOf(import))

    operator fun plus(other: String): TsCode = TsCode(this.text + other, this.imports)

    operator fun plus(other: TsCode): TsCode =
        TsCode(this.text + other.text, this.imports + other.imports)

    fun prependIndent(indent: String): TsCode =
        if (text.isEmpty()) this else copy(text = text.prependIndent(indent))

    companion object {
        operator fun invoke(f: Builder.() -> String): TsCode =
            Builder().run { this.toTsCode(f(this)) }

        fun join(
            code: Collection<TsCode>,
            separator: String,
            prefix: String = "",
            postfix: String = "",
        ): TsCode = TsCode { join(code, separator = separator, prefix = prefix, postfix = postfix) }
    }

    class Builder {
        private var imports: List<TsImport> = listOf()

        fun inline(code: TsCode): String {
            this.imports += code.imports
            return code.text
        }

        fun ref(import: TsImport): String {
            this.imports += import
            return import.name
        }

        fun join(
            code: Collection<TsCode>,
            separator: String,
            prefix: String = "",
            postfix: String = "",
        ): String {
            this.imports += code.flatMap { it.imports }
            return code.joinToString(separator, prefix = prefix, postfix = postfix) { it.text }
        }

        fun toTsCode(text: String): TsCode = TsCode(text, imports.toSet())
    }
}
