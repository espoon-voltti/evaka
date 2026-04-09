// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.trevaka.invoice

import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FixedLengthFieldTest {
    @Test
    fun `zero rows`() {
        assertEquals("", FixedLengthField.render(emptyList()))
    }

    @Test
    fun `empty rows`() {
        assertEquals("\n\n", FixedLengthField.render(listOf(emptyList(), emptyList())))
    }

    @Test
    fun text() {
        assertEquals(
            "hello world         \n",
            FixedLengthField.render(listOf(listOf(FixedLengthField.Text("hello world", 20)))),
        )
    }

    @Test
    fun `text null`() {
        assertEquals(
            "                    \n",
            FixedLengthField.render(listOf(listOf(FixedLengthField.Text(null, 20)))),
        )
    }

    @Test
    fun `text empty`() {
        assertEquals(
            "                    \n",
            FixedLengthField.render(listOf(listOf(FixedLengthField.Text("", 20)))),
        )
    }

    @Test
    fun number() {
        assertEquals(
            "00000000012345678900\n",
            FixedLengthField.render(listOf(listOf(FixedLengthField.Number(123456789, 18, 2)))),
        )
    }

    @Test
    fun `number null`() {
        assertEquals(
            "                    \n",
            FixedLengthField.render(listOf(listOf(FixedLengthField.Number(null, 18, 2)))),
        )
    }

    @Test
    fun `number zero`() {
        assertEquals(
            "00000000000000000000\n",
            FixedLengthField.render(listOf(listOf(FixedLengthField.Number(0, 18, 2)))),
        )
    }

    @Test
    fun cents() {
        assertEquals(
            "000000012800\n",
            FixedLengthField.render(listOf(listOf(FixedLengthField.Cents(12800, 10, 2)))),
        )
    }

    @Test
    fun `cents null`() {
        assertEquals(
            "            \n",
            FixedLengthField.render(listOf(listOf(FixedLengthField.Cents(null, 10, 2)))),
        )
    }

    @Test
    fun `cents zero`() {
        assertEquals(
            "000000000000\n",
            FixedLengthField.render(listOf(listOf(FixedLengthField.Cents(0, 10, 2)))),
        )
    }

    @Test
    fun date() {
        assertEquals(
            "20241204\n",
            FixedLengthField.render(
                listOf(listOf(FixedLengthField.Date(LocalDate.of(2024, 12, 4))))
            ),
        )
    }

    @Test
    fun `date null`() {
        assertEquals(
            "        \n",
            FixedLengthField.render(listOf(listOf(FixedLengthField.Date(null)))),
        )
    }

    @Test
    fun `multiple rows with all fields`() {
        val row1 =
            listOf(
                FixedLengthField.Text("123456T7890", 11),
                FixedLengthField.Number(84871, 10, 2),
                FixedLengthField.Cents(12800, 12, 2),
            )
        val row2 =
            listOf(
                FixedLengthField.Cents(12800, 12, 2),
                FixedLengthField.Number(84871, 10, 2),
                FixedLengthField.Text("123456T7890", 11),
            )

        assertEquals(
            "123456T789000000848710000000000012800\n00000000012800000008487100123456T7890\n",
            FixedLengthField.render(listOf(row1, row2)),
        )
    }
}
