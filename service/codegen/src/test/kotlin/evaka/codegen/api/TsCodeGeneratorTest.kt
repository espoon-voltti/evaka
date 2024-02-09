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
            val import = TsImport.Named(dummyFile, "PlainObject")
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
    @JsonTypeIdResolver(SealedSubclassSimpleName::class)
    sealed interface SealedInterface {
        data class Variant1(val data: String) : SealedInterface

        data object Variant2 : SealedInterface

        companion object {
            val import = TsImport.Named(dummyFile, "SealedInterface")
        }
    }

    private fun singleFileGenerator(vararg rootTypes: KType): TsCodeGenerator {
        return object : TsCodeGenerator(discoverMetadata(defaultMetadata, rootTypes.asSequence())) {
            override fun locateNamedType(namedType: TsNamedType<*>): TsFile = dummyFile
        }
    }

    @Test
    fun `code for types is generated correctly`() {
        val generator = singleFileGenerator(typeOf<PlainObject>(), typeOf<SealedInterface>())
        fun assertTsCode(text: String, vararg imports: TsImport, type: KType) {
            assertEquals(TsCode(text, *imports), generator.tsType(type))
        }

        assertTsCode("string", type = typeOf<String>())
        assertTsCode("string | null", type = typeOf<String?>())
        assertTsCode("PlainObject", PlainObject.import, type = typeOf<PlainObject>())
        assertTsCode("SealedInterface", SealedInterface.import, type = typeOf<SealedInterface>())

        assertTsCode("string[]", type = typeOf<List<String>>())
        assertTsCode("(string | null)[]", type = typeOf<List<String?>>())
        assertTsCode("(string | null)[] | null", type = typeOf<List<String?>?>())
        assertTsCode("LocalDate[]", Imports.localDate, type = typeOf<List<LocalDate>>())
        assertTsCode("PlainObject[]", PlainObject.import, type = typeOf<List<PlainObject>>())

        assertTsCode("Record<string, number>", type = typeOf<Map<String, Int>>())
        assertTsCode("Record<string, number | null>", type = typeOf<Map<String, Int?>>())
        assertTsCode(
            "Record<string, LocalDate>",
            Imports.localDate,
            type = typeOf<Map<String, LocalDate>>()
        )
        assertTsCode(
            "Record<string, PlainObject>",
            PlainObject.import,
            type = typeOf<Map<String, PlainObject>>()
        )
    }

    @Test
    fun `declarations for named types are generated correctly`() {
        val generator = singleFileGenerator(typeOf<PlainObject>(), typeOf<SealedInterface>())
        fun assertTsCode(text: String, vararg imports: TsImport, type: KType) {
            assertEquals(
                TsCode(text.trimIndent(), *imports),
                generator.namedType(generator.metadata[type] as TsNamedType<*>).let {
                    it.copy(text = it.text.trimIndent())
                }
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
            type = typeOf<PlainObject>()
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
            type = typeOf<SealedInterface>()
        )
    }
}
