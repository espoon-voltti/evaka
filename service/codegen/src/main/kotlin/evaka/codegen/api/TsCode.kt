// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen.api

import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.relativeTo

enum class TsProject {
    LibCommon,
    CitizenFrontend;

    operator fun div(path: String): TsFile = TsFile(this, Path.of(path))

    fun absoluteImportPath(path: Path): Path =
        when (this) {
            LibCommon -> Path.of("lib-common") / path
            CitizenFrontend -> Path.of("citizen-frontend") / path
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
        override val name: String
    ) : TsImport
}

/**
 * A fragment of TS code carrying also information about names that need to be imported from other
 * modules.
 */
data class TsCode(val text: String, val imports: Set<TsImport> = emptySet()) {
    companion object {
        operator fun invoke(f: Builder.() -> TsCode): TsCode = Builder().run { f(this) }
    }

    class Builder {
        private var imports: List<TsImport> = listOf()

        fun ts(code: TsCode): String {
            this.imports += code.imports
            return code.text
        }

        fun ref(import: TsImport): String {
            this.imports += import
            return import.name
        }

        fun join(code: Collection<TsCode>, separator: String): String {
            this.imports += code.flatMap { it.imports }
            return code.joinToString(separator) { it.text }
        }

        fun ts(text: String): TsCode = TsCode(text, imports.toSet())
    }
}
