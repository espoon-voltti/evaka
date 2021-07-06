// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import fi.espoo.evaka.PureJdbiTest
import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class DbTest : PureJdbiTest() {
    private data class Foo(val value: String)

    private fun Database.Read.fooJsonQuery() =
        createQuery("SELECT jsonb_agg(jsonb_build_object('value', 'foo')) AS json")

    @Test
    fun `mapJsonColumn can map a jsonb array to a kotlin array`() {
        val result = db.read { tx ->
            tx.fooJsonQuery().map { row ->
                row.mapJsonColumn<Array<Foo>>("json")
            }.single()
        }
        assertContentEquals(arrayOf(Foo("foo")), result)
        assertEquals(listOf(Foo("foo")), result.toList())
    }

    @Test
    fun `mapJsonColumn can map a jsonb array to a kotlin list`() {
        val result = db.read { tx ->
            tx.fooJsonQuery().map { row ->
                row.mapJsonColumn<List<Foo>>("json")
            }.single()
        }
        assertEquals(listOf(Foo("foo")), result)
    }

    @Test
    fun `mapJsonColumn can map a jsonb array to a kotlin set`() {
        val result = db.read { tx ->
            tx.fooJsonQuery().map { row ->
                row.mapJsonColumn<Set<Foo>>("json")
            }.single()
        }
        assertEquals(setOf(Foo("foo")), result)
    }
}
