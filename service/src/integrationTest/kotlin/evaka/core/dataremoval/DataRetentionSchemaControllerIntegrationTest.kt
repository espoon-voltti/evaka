// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dataremoval

import evaka.core.FullApplicationTest
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.insert
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired

class DataRetentionSchemaControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var controller: DataRetentionSchemaController

    private val employee = DevEmployee(roles = setOf(UserRole.SERVICE_WORKER))

    @BeforeEach
    fun setup() {
        db.transaction { it.insert(employee) }
    }

    @Test
    fun `the endpoint returns the declared tables and their references`() {
        val graph = controller.getDataRetentionSchema(employee.user)

        assertContains(graph.tables.map { it.name }, "person")
        assertContains(graph.tables.map { it.name }, "child")
        assertTrue(graph.tables.size > 1)
        assertTrue(graph.references.isNotEmpty())
    }

    @Test
    fun `the tables deleted with their parents share the root of their deletion bundle`() {
        val graph = controller.getDataRetentionSchema(employee.user)
        val tablesByName = graph.tables.associateBy { it.name }

        assertEquals("child", tablesByName.getValue("placement").deletionBundleRoot)
        assertEquals("child", tablesByName.getValue("service_need").deletionBundleRoot)
        assertEquals("child", tablesByName.getValue("child").deletionBundleRoot)
        assertEquals("person", tablesByName.getValue("message_account").deletionBundleRoot)
        assertNull(tablesByName.getValue("income").deletionBundleRoot)
    }
}
