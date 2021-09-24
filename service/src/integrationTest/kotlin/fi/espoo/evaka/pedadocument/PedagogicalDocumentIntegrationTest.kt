// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pedadocument

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.extensions.jsonBody
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDecisionMaker_1
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PedagogicalDocumentIntegrationTest : FullApplicationTest() {
    private val adminUser = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.ADMIN))

    private fun deserializeGetResult(json: String) = objectMapper.readValue<List<PedagogicalDocument>>(json)
    private fun deserializePutResult(json: String) = objectMapper.readValue<PedagogicalDocument>(json)
    private fun deserializePostResult(json: String) = objectMapper.readValue<PedagogicalDocument>(json)

    @BeforeEach
    private fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            tx.insertGeneralTestFixtures()
        }
    }

    @Test
    fun `creating new document`() {
        val (_, _, result) = http.post("/pedagogical-document")
            .jsonBody("""{"childId": "${testChild_1.id}", "description": "", "attachmentId": null}""")
            .asUser(adminUser)
            .responseString()

        assertNotNull(deserializePostResult(result.get()).id)
        assertEquals(
            ChildId(testChild_1.id),
            deserializePostResult(result.get()).childId
        )
    }

    @Test
    fun `updating document`() {
        val (_, _, res) = http.post("/pedagogical-document")
            .jsonBody("""{"childId": "${testChild_1.id}", "description": "", "attachmentId": null}""")
            .asUser(adminUser)
            .responseString()

        val id = deserializePostResult(res.get()).id

        val (_, _, result) = http.put("/pedagogical-document/$id")
            .jsonBody("""{"id": "$id", "childId": "${testChild_1.id}", "description": "foobar", "attachmentId": null}""")
            .asUser(adminUser)
            .responseString()

        assertEquals(
            "foobar",
            deserializePutResult(result.get()).description
        )
    }

    @Test
    fun `find updated document`() {
        val testDescription = "foobar"

        val (_, _, res) = http.post("/pedagogical-document")
            .jsonBody("""{"childId": "${testChild_1.id}", "description": "", "attachmentId": null}""")
            .asUser(adminUser)
            .responseString()

        val id = deserializePostResult(res.get()).id

        http.put("/pedagogical-document/$id")
            .jsonBody("""{"id": "$id", "childId": "${testChild_1.id}", "description": "$testDescription", "attachmentId": null}""")
            .asUser(adminUser)
            .responseString()

        val (_, _, result) = http.get("/pedagogical-document/child/${testChild_1.id}")
            .asUser(adminUser)
            .responseString()

        val parsed = deserializeGetResult(result.get())

        assertEquals(1, parsed.size)
        assertEquals(
            testDescription,
            parsed.first().description
        )
    }
}
