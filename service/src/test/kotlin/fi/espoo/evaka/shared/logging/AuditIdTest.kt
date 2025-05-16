// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.logging

import fi.espoo.evaka.AuditId
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class AuditIdTest {
    @Test
    fun plusCombinesTwoOneInstancesIntoMany() {
        val one1 = AuditId.One("value1")
        val one2 = AuditId.One("value2")
        val result = one1 + one2
        assertEquals(AuditId.Many(listOf("value1", "value2")), result)
    }

    @Test
    fun plusAddsOneToMany() {
        val many = AuditId.Many(listOf("value1", "value2"))
        val one = AuditId.One("value3")
        val result = many + one
        assertEquals(AuditId.Many(listOf("value1", "value2", "value3")), result)
    }

    @Test
    fun plusAddsManyToOne() {
        val one = AuditId.One("value1")
        val many = AuditId.Many(listOf("value2", "value3"))
        val result = one + many
        assertEquals(AuditId.Many(listOf("value1", "value2", "value3")), result)
    }

    @Test
    fun plusCombinesTwoManyInstances() {
        val many1 = AuditId.Many(listOf("value1", "value2"))
        val many2 = AuditId.Many(listOf("value3", "value4"))
        val result = many1 + many2
        assertEquals(AuditId.Many(listOf("value1", "value2", "value3", "value4")), result)
    }

    @Test
    fun plusHandlesEmptyOneAndOne() {
        val one1 = AuditId.One("")
        val one2 = AuditId.One("")
        val result = one1 + one2
        assertEquals(AuditId.Many(listOf("", "")), result)
    }

    @Test
    fun plusHandlesEmptyManyAndOne() {
        val many = AuditId.Many(emptyList())
        val one = AuditId.One("value")
        val result = many + one
        assertEquals(AuditId.Many(listOf("value")), result)
    }

    @Test
    fun plusHandlesOneAndEmptyMany() {
        val one = AuditId.One("value")
        val many = AuditId.Many(emptyList())
        val result = one + many
        assertEquals(AuditId.Many(listOf("value")), result)
    }

    @Test
    fun plusHandlesTwoEmptyManyInstances() {
        val many1 = AuditId.Many(emptyList())
        val many2 = AuditId.Many(emptyList())
        val result = many1 + many2
        assertEquals(AuditId.Many(emptyList()), result)
    }
}
