// SPDX-FileCopyrightText: 2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.bi

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class BiCsvUtilsTest {

    data class Sample(
        val id: Int,
        @Pii val name: String,
        @LegacyColumn val legacy: String?,
        val keep: String,
    )

    data class SampleBothAnnotations(
        val id: Int,
        @Pii @LegacyColumn val both: String,
        val keep: String,
    )

    data class SampleNullablePii(val id: Int, @Pii val name: String?, val keep: String)

    private val rows = sequenceOf(Sample(1, "Alice", null, "K"))

    private fun render(config: BiExportConfig): List<String> =
        toCsvRecords(::convertToCsv, Sample::class, rows, config).toList()

    // Kotlin reflection returns declaredMemberProperties in alphabetical order

    @Test
    fun `includes all columns when includePII=true and includeLegacyColumns=true`() {
        val out =
            render(
                BiExportConfig(includePII = true, includeLegacyColumns = true, deltaWindowDays = 60)
            )
        assertEquals("id,keep,legacy,name\r\n", out[0])
        assertEquals("1,K,,Alice\r\n", out[1])
    }

    @Test
    fun `omits Pii columns when includePII=false`() {
        val out =
            render(
                BiExportConfig(
                    includePII = false,
                    includeLegacyColumns = true,
                    deltaWindowDays = 60,
                )
            )
        assertEquals("id,keep,legacy\r\n", out[0])
        assertEquals("1,K,\r\n", out[1])
    }

    @Test
    fun `omits LegacyColumn columns when includeLegacyColumns=false`() {
        val out =
            render(
                BiExportConfig(
                    includePII = true,
                    includeLegacyColumns = false,
                    deltaWindowDays = 60,
                )
            )
        assertEquals("id,keep,name\r\n", out[0])
        assertEquals("1,K,Alice\r\n", out[1])
    }

    @Test
    fun `omits both categories when both flags are false`() {
        val out =
            render(
                BiExportConfig(
                    includePII = false,
                    includeLegacyColumns = false,
                    deltaWindowDays = 60,
                )
            )
        assertEquals("id,keep\r\n", out[0])
        assertEquals("1,K\r\n", out[1])
    }

    @Test
    fun `omits column annotated with both Pii and LegacyColumn when includePII=false`() {
        val out =
            toCsvRecords(
                    ::convertToCsv,
                    SampleBothAnnotations::class,
                    sequenceOf(SampleBothAnnotations(1, "X", "K")),
                    BiExportConfig(
                        includePII = false,
                        includeLegacyColumns = true,
                        deltaWindowDays = 60,
                    ),
                )
                .toList()
        assertEquals("id,keep\r\n", out[0])
        assertEquals("1,K\r\n", out[1])
    }

    @Test
    fun `omits column annotated with both Pii and LegacyColumn when includeLegacyColumns=false`() {
        val out =
            toCsvRecords(
                    ::convertToCsv,
                    SampleBothAnnotations::class,
                    sequenceOf(SampleBothAnnotations(1, "X", "K")),
                    BiExportConfig(
                        includePII = true,
                        includeLegacyColumns = false,
                        deltaWindowDays = 60,
                    ),
                )
                .toList()
        assertEquals("id,keep\r\n", out[0])
        assertEquals("1,K\r\n", out[1])
    }

    @Test
    fun `omits nullable Pii column with null value when includePII=false`() {
        val out =
            toCsvRecords(
                    ::convertToCsv,
                    SampleNullablePii::class,
                    sequenceOf(SampleNullablePii(1, null, "K")),
                    BiExportConfig(
                        includePII = false,
                        includeLegacyColumns = true,
                        deltaWindowDays = 60,
                    ),
                )
                .toList()
        assertEquals("id,keep\r\n", out[0])
        assertEquals("1,K\r\n", out[1])
    }
}
