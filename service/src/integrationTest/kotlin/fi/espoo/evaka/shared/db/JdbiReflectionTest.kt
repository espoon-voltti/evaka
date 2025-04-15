// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import fi.espoo.evaka.PureJdbiTest
import kotlin.reflect.KClass
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class JdbiReflectionTest : PureJdbiTest(resetDbBeforeEach = false) {
    private inline fun <reified T> mapOneValue(
        query: QuerySql,
        vararg annotations: KClass<out Annotation>,
    ) = db.read { tx -> tx.createQuery(query).exactlyOne { column<T>("result", *annotations) } }

    @Test
    fun `mapColumn works with primitive arrays`() {
        mapOneValue<CharArray>(QuerySql { sql("SELECT array['a', 'b', 'c'] AS result") }).let {
            assertEquals('A', it[0].uppercaseChar())
            assertContentEquals(charArrayOf('a', 'b', 'c'), it)
        }
        mapOneValue<ShortArray>(QuerySql { sql("SELECT array[0, 1, 2] AS result") }).let {
            assertEquals(1, it[0].inc())
            assertContentEquals(shortArrayOf(0, 1, 2), it)
        }
        mapOneValue<IntArray>(QuerySql { sql("SELECT array[0, 1, 2] AS result") }).let {
            assertEquals(1, it[0].inc())
            assertContentEquals(intArrayOf(0, 1, 2), it)
        }
        mapOneValue<LongArray>(QuerySql { sql("SELECT array[0, 1, 2] AS result") }).let {
            assertEquals(1L, it[0].inc())
            assertContentEquals(longArrayOf(0L, 1L, 2L), it)
        }
        mapOneValue<FloatArray>(QuerySql { sql("SELECT array[0.0, 1.0, 2.0] AS result") }).let {
            assertFalse(it[0].isNaN())
            assertContentEquals(floatArrayOf(0.0f, 1.0f, 2.0f), it)
        }
        mapOneValue<DoubleArray>(QuerySql { sql("SELECT array[0.0, 1.0, 2.0] AS result") }).let {
            assertFalse(it[0].isNaN())
            assertContentEquals(doubleArrayOf(0.0, 1.0, 2.0), it)
        }
        mapOneValue<BooleanArray>(QuerySql { sql("SELECT array[false, true] AS result") }).let {
            assertTrue(it[0].not())
            assertContentEquals(booleanArrayOf(false, true), it)
        }
        // primitive byte arrays are not supported
    }

    @Test
    fun `mapColumn works with boxed arrays`() {
        mapOneValue<Array<Char>>(QuerySql { sql("SELECT array['a', 'b', 'c'] AS result") }).let {
            assertEquals('A', it[0].uppercaseChar())
            assertContentEquals(arrayOf('a', 'b', 'c'), it)
        }
        mapOneValue<Array<Short>>(QuerySql { sql("SELECT array[0, 1, 2] AS result") }).let {
            assertEquals(1, it[0].inc())
            assertContentEquals(arrayOf(0, 1, 2), it)
        }
        mapOneValue<Array<Int>>(QuerySql { sql("SELECT array[0, 1, 2] AS result") }).let {
            assertEquals(1, it[0].inc())
            assertContentEquals(arrayOf(0, 1, 2), it)
        }
        mapOneValue<Array<Long>>(QuerySql { sql("SELECT array[0, 1, 2] AS result") }).let {
            assertEquals(1L, it[0].inc())
            assertContentEquals(arrayOf(0L, 1L, 2L), it)
        }
        mapOneValue<Array<Float>>(QuerySql { sql("SELECT array[0.0, 1.0, 2.0] AS result") }).let {
            assertFalse(it[0].isNaN())
            assertContentEquals(arrayOf(0.0f, 1.0f, 2.0f), it)
        }
        mapOneValue<Array<Double>>(QuerySql { sql("SELECT array[0.0, 1.0, 2.0] AS result") }).let {
            assertFalse(it[0].isNaN())
            assertContentEquals(arrayOf(0.0, 1.0, 2.0), it)
        }
        mapOneValue<Array<Boolean>>(QuerySql { sql("SELECT array[false, true] AS result") }).let {
            assertTrue(it[0].not())
            assertContentEquals(arrayOf(false, true), it)
        }
        mapOneValue<Array<Byte>>(QuerySql { sql("SELECT array[0, 1, 2] AS result") }).let {
            assertEquals(1, it[0].inc())
            assertContentEquals(arrayOf(0, 1, 2), it)
        }
    }

    @Test
    fun `mapColumn works with nullable boxed arrays`() {
        mapOneValue<Array<Char?>>(QuerySql { sql("SELECT array['a', null, 'c'] AS result") }).let {
            assertEquals('A', it[0]?.uppercaseChar())
            assertNull(it[1])
            assertContentEquals(arrayOf('a', null, 'c'), it)
        }
        mapOneValue<Array<Short?>>(QuerySql { sql("SELECT array[0, null, 2] AS result") }).let {
            assertEquals(1, it[0]?.inc())
            assertNull(it[1])
            assertContentEquals(arrayOf(0, null, 2), it)
        }
        mapOneValue<Array<Int?>>(QuerySql { sql("SELECT array[0, null, 2] AS result") }).let {
            assertEquals(1, it[0]?.inc())
            assertNull(it[1])
            assertContentEquals(arrayOf(0, null, 2), it)
        }
        mapOneValue<Array<Long?>>(QuerySql { sql("SELECT array[0, null, 2] AS result") }).let {
            assertEquals(1L, it[0]?.inc())
            assertNull(it[1])
            assertContentEquals(arrayOf(0L, null, 2L), it)
        }
        mapOneValue<Array<Float?>>(QuerySql { sql("SELECT array[0.0, null, 2.0] AS result") }).let {
            assertFalse(it[0]!!.isNaN())
            assertNull(it[1])
            assertContentEquals(arrayOf(0.0f, null, 2.0f), it)
        }
        mapOneValue<Array<Double?>>(QuerySql { sql("SELECT array[0.0, null, 2.0] AS result") })
            .let {
                assertFalse(it[0]!!.isNaN())
                assertNull(it[1])
                assertContentEquals(arrayOf(0.0, null, 2.0), it)
            }
        mapOneValue<Array<Boolean?>>(QuerySql { sql("SELECT array[false, null, true] AS result") })
            .let {
                assertTrue(it[0]!!.not())
                assertNull(it[1])
                assertContentEquals(arrayOf(false, null, true), it)
            }
        mapOneValue<Array<Byte?>>(QuerySql { sql("SELECT array[0, null, 2] AS result") }).let {
            assertEquals(1, it[0]?.inc())
            assertNull(it[1])
            assertContentEquals(arrayOf(0, null, 2), it)
        }
    }

    @Test
    fun `mapColumn works with primitive lists`() {
        mapOneValue<List<Char>>(QuerySql { sql("SELECT array['a', 'b', 'c'] AS result") }).let {
            assertEquals('A', it[0].uppercaseChar())
            assertContentEquals(listOf('a', 'b', 'c'), it)
        }
        mapOneValue<List<Short>>(QuerySql { sql("SELECT array[0, 1, 2] AS result") }).let {
            assertEquals(1, it[0].inc())
            assertContentEquals(listOf(0, 1, 2), it)
        }
        mapOneValue<List<Int>>(QuerySql { sql("SELECT array[0, 1, 2] AS result") }).let {
            assertEquals(1, it[0].inc())
            assertContentEquals(listOf(0, 1, 2), it)
        }
        mapOneValue<List<Long>>(QuerySql { sql("SELECT array[0, 1, 2] AS result") }).let {
            assertEquals(1L, it[0].inc())
            assertContentEquals(listOf(0L, 1L, 2L), it)
        }
        mapOneValue<List<Float>>(QuerySql { sql("SELECT array[0.0, 1.0, 2.0] AS result") }).let {
            assertFalse(it[0].isNaN())
            assertContentEquals(listOf(0.0f, 1.0f, 2.0f), it)
        }
        mapOneValue<List<Double>>(QuerySql { sql("SELECT array[0.0, 1.0, 2.0] AS result") }).let {
            assertFalse(it[0].isNaN())
            assertContentEquals(listOf(0.0, 1.0, 2.0), it)
        }
        mapOneValue<List<Boolean>>(QuerySql { sql("SELECT array[false, true] AS result") }).let {
            assertTrue(it[0].not())
            assertContentEquals(listOf(false, true), it)
        }
        mapOneValue<List<Byte>>(QuerySql { sql("SELECT array[0, 1, 2] AS result") }).let {
            assertEquals(1, it[0].inc())
            assertContentEquals(listOf(0, 1, 2), it)
        }
    }
}
