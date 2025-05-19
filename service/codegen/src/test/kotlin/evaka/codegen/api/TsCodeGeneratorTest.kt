// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen.api

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver
import fi.espoo.evaka.shared.config.SealedSubclassSimpleName
import java.time.LocalDate
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

private val dummyFile = TsProject.LibCommon / "file.ts"

class TsCodeGeneratorTest {
    data class PlainObject(val str: String, val list: List<String>, val bool: Boolean) {
        companion object {
            val import = TsImport.Type(dummyFile, "PlainObject")
        }
    }

    data class ObjectLiteral(val str: String, val list: List<String>?, val bool: Boolean)

    @JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
    @JsonTypeIdResolver(SealedSubclassSimpleName::class)
    sealed interface SealedInterface {
        data class Variant1(val data: String) : SealedInterface

        data object Variant2 : SealedInterface

        companion object {
            val import = TsImport.Type(dummyFile, "SealedInterface")
        }
    }

    private fun metadata(vararg rootTypes: KType): TypeMetadata =
        discoverMetadata(defaultMetadata, rootTypes.asSequence())

    private fun singleFileGenerator(metadata: TypeMetadata): TsCodeGenerator =
        object : TsCodeGenerator(metadata) {
            override fun locateNamedType(namedType: TsNamedType<*>): TsFile = dummyFile
        }

    @Test
    fun `code for types is generated correctly`() {
        val generator =
            singleFileGenerator(
                metadata(typeOf<PlainObject>(), typeOf<SealedInterface>()) +
                    TypeMetadata(ObjectLiteral::class to TsObjectLiteral(ObjectLiteral::class))
            )
        fun assertTsCode(
            text: String,
            vararg imports: TsImport,
            type: KType,
            compact: Boolean = false,
        ) {
            assertEquals(TsCode(text, *imports), generator.tsType(type, compact = compact))
        }

        assertTsCode("string", type = typeOf<String>())
        assertTsCode("string | null", type = typeOf<String?>())
        assertTsCode("[string | null, number] | null", type = typeOf<Pair<String?, Int>?>())
        assertTsCode("[string, number, boolean]", type = typeOf<Triple<String, Int, Boolean>>())
        assertTsCode("PlainObject", PlainObject.import, type = typeOf<PlainObject>())
        assertTsCode("SealedInterface", SealedInterface.import, type = typeOf<SealedInterface>())
        assertTsCode(
            "{ bool: boolean, list: string[] | null, str: string }",
            type = typeOf<ObjectLiteral>(),
            compact = true,
        )
        assertTsCode(
            """{
  bool: boolean,
  list: string[] | null,
  str: string
}"""
                .trimIndent(),
            type = typeOf<ObjectLiteral>(),
        )

        assertTsCode("string[]", type = typeOf<List<String>>())
        assertTsCode("(string | null)[]", type = typeOf<List<String?>>())
        assertTsCode("(string | null)[] | null", type = typeOf<List<String?>?>())
        assertTsCode("[string, number][]", type = typeOf<List<Pair<String, Int>>>())
        assertTsCode(
            "[string, number, boolean][]",
            type = typeOf<List<Triple<String, Int, Boolean>>>(),
        )
        assertTsCode("LocalDate[]", Imports.localDate, type = typeOf<List<LocalDate>>())
        assertTsCode("PlainObject[]", PlainObject.import, type = typeOf<List<PlainObject>>())
        assertTsCode(
            """({
  bool: boolean,
  list: string[] | null,
  str: string
} | null)[]"""
                .trimIndent(),
            type = typeOf<List<ObjectLiteral?>>(),
        )

        assertTsCode("Partial<Record<string, number>>", type = typeOf<Map<String, Int>>())
        assertTsCode("Partial<Record<string, number | null>>", type = typeOf<Map<String, Int?>>())
        assertTsCode(
            "Partial<Record<string, [string, number]>>",
            type = typeOf<Map<String, Pair<String, Int>>>(),
        )
        assertTsCode(
            "Partial<Record<string, [string, number, boolean]>>",
            type = typeOf<Map<String, Triple<String, Int, Boolean>>>(),
        )
        assertTsCode(
            "Partial<Record<string, LocalDate>>",
            Imports.localDate,
            type = typeOf<Map<String, LocalDate>>(),
        )
        assertTsCode(
            "Partial<Record<string, PlainObject>>",
            PlainObject.import,
            type = typeOf<Map<String, PlainObject>>(),
        )
        assertTsCode(
            """Partial<Record<string, {
  bool: boolean,
  list: string[] | null,
  str: string
} | null>>"""
                .trimIndent(),
            type = typeOf<Map<String, ObjectLiteral?>>(),
        )
    }

    @Test
    fun `declarations for named types are generated correctly`() {
        val generator =
            singleFileGenerator(metadata(typeOf<PlainObject>(), typeOf<SealedInterface>()))
        fun assertTsCode(text: String, vararg imports: TsImport, type: KType) {
            assertEquals(
                TsCode(text.trimIndent(), *imports),
                generator.namedType(generator.metadata[type] as TsNamedType<*>).let {
                    it.copy(text = it.text.trimIndent())
                },
            )
        }

        assertTsCode(
            """
/**
* Generated from evaka.codegen.api.TsCodeGeneratorTest.PlainObject
*/
export interface PlainObject {
  bool: boolean
  list: string[]
  str: string
}""",
            type = typeOf<PlainObject>(),
        )

        assertTsCode(
            """
export namespace SealedInterface {
  /**
  * Generated from evaka.codegen.api.TsCodeGeneratorTest.SealedInterface.Variant1
  */
  export interface Variant1 {
    type: 'Variant1'
    data: string
  }
  
  /**
  * Generated from evaka.codegen.api.TsCodeGeneratorTest.SealedInterface.Variant2
  */
  export interface Variant2 {
    type: 'Variant2'
  }
}

/**
* Generated from evaka.codegen.api.TsCodeGeneratorTest.SealedInterface
*/
export type SealedInterface = SealedInterface.Variant1 | SealedInterface.Variant2
""",
            type = typeOf<SealedInterface>(),
        )
    }
}
