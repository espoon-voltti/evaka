// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PredicateTest {
    @Test
    fun `predicate builder allows only one where call`() {
        assertThrows<IllegalStateException> {
            Predicate {
                    where("TRUE")
                    where("FALSE -- this fails")
                }
                .forTable("some_table")
        }
    }

    @Test
    fun `a predicate generates correct SQL`() {
        val predicate = Predicate { where("$it.some_column = '1'") }.forTable("some_table")
        assertEquals("(some_table.some_column = '1')", predicate.sql.toString())
        assertEquals(emptyList<ValueBinding<Any?>>(), predicate.bindings)
    }

    @Test
    fun `a predicate with parameter binding generates correct SQL`() {
        val value = "value"
        val predicate =
            Predicate { where("$it.some_column = ${bind(value)}") }.forTable("some_table")
        assertEquals("(some_table.some_column = ?)", predicate.sql.toString())
        assertEquals(listOf(ValueBinding.of(value)), predicate.bindings)
    }

    @Test
    fun `or generates correct SQL`() {
        val a = Predicate { where("$it.a = ${bind(1)}") }
        val b = Predicate { where("$it.b = ${bind(2)}") }
        val predicate = a.or(b).forTable("some_table")
        assertEquals("((some_table.a = ?) OR (some_table.b = ?))", predicate.sql.toString())
        assertEquals(
            listOf(
                ValueBinding.of(1),
                ValueBinding.of(2),
            ),
            predicate.bindings
        )
    }

    @Test
    fun `and generates correct SQL`() {
        val a = Predicate { where("$it.a = ${bind(1)}") }
        val b = Predicate { where("$it.b = ${bind(2)}") }
        val predicate = a.and(b).forTable("some_table")
        assertEquals("((some_table.a = ?) AND (some_table.b = ?))", predicate.sql.toString())
        assertEquals(
            listOf(
                ValueBinding.of(1),
                ValueBinding.of(2),
            ),
            predicate.bindings
        )
    }

    @Test
    fun `any generates correct SQL`() {
        val a = Predicate { where("$it.a = ${bind(1)}") }
        val b = Predicate { where("$it.b = ${bind(2)}") }
        val predicate = Predicate.any(a, b).forTable("some_table")
        assertEquals("((some_table.a = ?) OR (some_table.b = ?))", predicate.sql.toString())
        assertEquals(
            listOf(
                ValueBinding.of(1),
                ValueBinding.of(2),
            ),
            predicate.bindings
        )
    }

    @Test
    fun `all generates correct SQL`() {
        val a = Predicate { where("$it.a = ${bind(1)}") }
        val b = Predicate { where("$it.b = ${bind(2)}") }
        val predicate = Predicate.all(a, b).forTable("some_table")
        assertEquals("((some_table.a = ?) AND (some_table.b = ?))", predicate.sql.toString())
        assertEquals(
            listOf(
                ValueBinding.of(1),
                ValueBinding.of(2),
            ),
            predicate.bindings
        )
    }

    @Test
    fun `Predicate alwaysTrue short-circuits or-any operations in the final table predicate`() {
        val other = Predicate { where("$it.some_column = '1'") }

        fun assertAlwaysTrue(predicate: Predicate) =
            assertEquals(PredicateSql.alwaysTrue(), predicate.forTable("some_table"))

        val alwaysTrue = Predicate.alwaysTrue()
        assertAlwaysTrue(other.or(alwaysTrue))
        assertAlwaysTrue(alwaysTrue.or(other))
        assertAlwaysTrue(Predicate.any(alwaysTrue, other))
        assertAlwaysTrue(Predicate.any(other, alwaysTrue))
    }

    @Test
    fun `Predicate alwaysTrue is automatically removed from and-all operations in the final table predicate`() {
        val a = Predicate { where("$it.a = ${bind(1)}") }
        val b = Predicate { where("$it.b = ${bind(2)}") }
        val triple = Predicate.all(a, Predicate.alwaysTrue(), b).forTable("some_table")
        assertEquals("((some_table.a = ?) AND (some_table.b = ?))", triple.sql.toString())
        assertEquals(
            listOf(
                ValueBinding.of(1),
                ValueBinding.of(2),
            ),
            triple.bindings
        )
    }

    @Test
    fun `Predicate alwaysFalse short-circuits and-all operations in the final table predicate`() {
        val other = Predicate { where("$it.some_column = '1'") }

        fun assertAlwaysFalse(predicate: Predicate) =
            assertEquals(PredicateSql.alwaysFalse(), predicate.forTable("some_table"))

        val alwaysFalse = Predicate.alwaysFalse()
        assertAlwaysFalse(other.and(alwaysFalse))
        assertAlwaysFalse(alwaysFalse.and(other))
        assertAlwaysFalse(Predicate.all(alwaysFalse, other))
        assertAlwaysFalse(Predicate.all(other, alwaysFalse))
    }

    @Test
    fun `Predicate alwaysFalse is automatically removed from or-any operations in the final table predicate`() {
        val a = Predicate { where("$it.a = ${bind(1)}") }
        val b = Predicate { where("$it.b = ${bind(2)}") }
        val triple = Predicate.any(a, Predicate.alwaysFalse(), b).forTable("some_table")
        assertEquals("((some_table.a = ?) OR (some_table.b = ?))", triple.sql.toString())
        assertEquals(
            listOf(
                ValueBinding.of(1),
                ValueBinding.of(2),
            ),
            triple.bindings
        )
    }

    @Test
    fun `complex predicate generates correct SQL`() {
        val a = Predicate { where("$it.a = ${bind(1)}") }
        val b = Predicate { where("$it.b = ${bind(2)}") }
        val c = Predicate { where("$it.c = ${bind(3)}") }
        val predicate =
            Predicate.all(a, Predicate.alwaysTrue(), b)
                .and(Predicate.alwaysTrue().or(a)) // this should get eliminated
                .or(Predicate.any(c, Predicate.alwaysFalse(), b))
                .or(Predicate.alwaysFalse().and(b)) // this should get eliminated
                .forTable("some_table")
        assertEquals(
            "(((some_table.a = ?) AND (some_table.b = ?)) OR (some_table.c = ?) OR (some_table.b = ?))",
            predicate.sql.toString()
        )
        assertEquals(
            listOf(
                ValueBinding.of(1),
                ValueBinding.of(2),
                ValueBinding.of(3),
                ValueBinding.of(2),
            ),
            predicate.bindings
        )
    }

    @Test
    fun `PredicateSql alwaysTrue short-circuits or-any operations`() {
        val other = Predicate { where("$it.some_column = '1'") }.forTable("some_table")
        val alwaysTrue = PredicateSql.alwaysTrue()
        assertEquals(alwaysTrue, alwaysTrue.or(other))
        assertEquals(alwaysTrue, other.or(alwaysTrue))
        assertEquals(alwaysTrue, PredicateSql.any(alwaysTrue, other))
        assertEquals(alwaysTrue, PredicateSql.any(other, alwaysTrue))
    }

    @Test
    fun `PredicateSql alwaysTrue is automatically removed from and-all operations`() {
        val a = Predicate { where("$it.a = ${bind(1)}") }.forTable("some_table")
        val b = Predicate { where("$it.b = ${bind(2)}") }.forTable("some_table")
        val alwaysTrue = PredicateSql.alwaysTrue()
        assertEquals(a, PredicateSql.all(alwaysTrue, a))
        assertEquals(b, PredicateSql.all(b, alwaysTrue))

        val triple = PredicateSql.all(a, alwaysTrue, b)
        assertEquals("((some_table.a = ?) AND (some_table.b = ?))", triple.sql.toString())
        assertEquals(
            listOf(
                ValueBinding.of(1),
                ValueBinding.of(2),
            ),
            triple.bindings
        )
    }

    @Test
    fun `PredicateSql alwaysFalse short-circuits and-all operations`() {
        val other = Predicate { where("$it.some_column = '1'") }.forTable("some_table")
        val alwaysFalse = PredicateSql.alwaysFalse()
        assertEquals(alwaysFalse, alwaysFalse.and(other))
        assertEquals(alwaysFalse, other.and(alwaysFalse))
        assertEquals(alwaysFalse, PredicateSql.all(alwaysFalse, other))
        assertEquals(alwaysFalse, PredicateSql.all(other, alwaysFalse))
    }

    @Test
    fun `PredicateSql alwaysFalse is automatically removed from or-any operations`() {
        val a = Predicate { where("$it.a = ${bind(1)}") }.forTable("some_table")
        val b = Predicate { where("$it.b = ${bind(2)}") }.forTable("some_table")
        val alwaysFalse = PredicateSql.alwaysFalse()
        assertEquals(a, PredicateSql.any(alwaysFalse, a))
        assertEquals(b, PredicateSql.any(b, alwaysFalse))

        val triple = PredicateSql.any(a, alwaysFalse, b)
        assertEquals("((some_table.a = ?) OR (some_table.b = ?))", triple.sql.toString())
        assertEquals(
            listOf(
                ValueBinding.of(1),
                ValueBinding.of(2),
            ),
            triple.bindings
        )
    }
}
