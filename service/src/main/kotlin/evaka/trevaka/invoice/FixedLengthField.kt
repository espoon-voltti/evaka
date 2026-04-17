// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.trevaka.invoice

import java.time.LocalDate
import java.time.format.DateTimeFormatter

sealed class FixedLengthField(val length: Int) {
    abstract fun render(): String

    init {
        require(length > 0) { "Length must be positive" }
    }

    class Text(private val value: String?, length: Int) : FixedLengthField(length) {
        override fun render(): String = value?.take(length)?.padEnd(length) ?: empty()
    }

    class Number(private val value: Int?, private val integerLength: Int, decimalLength: Int = 0) :
        FixedLengthField(integerLength + decimalLength) {
        override fun render(): String =
            value?.toString()?.padStart(integerLength, '0')?.padEnd(length, '0') ?: empty()
    }

    class Cents(private val value: Int?, val integerLength: Int, decimalLength: Int = 2) :
        FixedLengthField(integerLength + decimalLength) {
        init {
            require(decimalLength >= 2) { "Decimal length must be 2 or greater" }
        }

        override fun render(): String =
            value?.toString()?.padStart(integerLength + 2, '0')?.padEnd(length, '0') ?: empty()
    }

    class Date(private val value: LocalDate?) : FixedLengthField(PATTERN.length) {
        override fun render(): String = value?.format(formatter) ?: empty()

        companion object {
            private const val PATTERN = "yyyyMMdd"
            private val formatter = DateTimeFormatter.ofPattern(PATTERN)
        }
    }

    class Empty(length: Int) : FixedLengthField(length) {
        override fun render() = empty()
    }

    protected fun empty() = "".padEnd(length)

    companion object {
        private const val LINE_BREAK = '\n'

        fun render(rows: List<List<FixedLengthField>>): String =
            rows
                .fold(StringBuilder("")) { acc, row ->
                    acc.append(
                        row.fold(StringBuilder("")) { acc, field -> acc.append(field.render()) }
                            .append(LINE_BREAK)
                    )
                }
                .toString()
    }
}
