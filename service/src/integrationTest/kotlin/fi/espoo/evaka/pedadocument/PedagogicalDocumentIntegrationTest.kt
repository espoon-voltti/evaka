// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pedadocument

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.updateDaycareAclWithEmployee
import fi.espoo.evaka.shared.dev.updateDaycareGroupAclWithEmployee
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PedagogicalDocumentIntegrationTest : FullApplicationTest() {
    private val employeeId = UUID.randomUUID()
    private val employee = AuthenticatedUser.Employee(employeeId, setOf(UserRole.ADMIN))

    private val supervisorId = UUID.randomUUID()
    private val supervisor = AuthenticatedUser.Employee(supervisorId, setOf())

    private val staffId = UUID.randomUUID()
    private val staff = AuthenticatedUser.Employee(staffId, setOf())

    val groupId = GroupId(UUID.randomUUID())

    private fun deserializeGetResult(json: String) = objectMapper.readValue<List<PedagogicalDocument>>(json)
    private fun deserializePutResult(json: String) = objectMapper.readValue<PedagogicalDocument>(json)
    private fun deserializePostResult(json: String) = objectMapper.readValue<PedagogicalDocument>(json)

    @BeforeEach
    private fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            tx.insertGeneralTestFixtures()
            tx.insertTestEmployee(DevEmployee(id = employeeId, roles = setOf(UserRole.ADMIN)))
            tx.insertTestEmployee(DevEmployee(id = supervisorId, roles = setOf()))
            tx.insertTestEmployee(DevEmployee(id = staffId, roles = setOf()))
            tx.updateDaycareAclWithEmployee(testDaycare.id, supervisorId, UserRole.UNIT_SUPERVISOR)
            tx.insertTestDaycareGroup(DevDaycareGroup(groupId, testDaycare.id))
            tx.updateDaycareGroupAclWithEmployee(groupId, staffId)

            val placementId = tx.insertTestPlacement(childId = testChild_1.id, unitId = testDaycare.id, endDate = LocalDate.MAX)
            tx.insertTestDaycareGroupPlacement(
                daycarePlacementId = placementId,
                groupId = groupId,
                endDate = LocalDate.MAX
            )
        }
    }

    @Test
    fun `creating new document`() {
        val (_, _, result) = http.post("/pedagogical-document")
            .jsonBody("""{"childId": "${testChild_1.id}", "description": "", "attachmentId": null}""")
            .asUser(employee)
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
            .asUser(employee)
            .responseString()

        val id = deserializePostResult(res.get()).id

        val (_, _, result) = http.put("/pedagogical-document/$id")
            .jsonBody("""{"id": "$id", "childId": "${testChild_1.id}", "description": "foobar", "attachmentId": null}""")
            .asUser(employee)
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
            .asUser(employee)
            .responseString()

        val id = deserializePostResult(res.get()).id

        http.put("/pedagogical-document/$id")
            .jsonBody("""{"id": "$id", "childId": "${testChild_1.id}", "description": "$testDescription", "attachmentId": null}""")
            .asUser(employee)
            .responseString()

        val (_, _, result) = http.get("/pedagogical-document/child/${testChild_1.id}")
            .asUser(employee)
            .responseString()

        val parsed = deserializeGetResult(result.get())

        assertEquals(1, parsed.size)
        assertEquals(
            testDescription,
            parsed.first().description
        )
    }

    @Test
    fun `admin can read document with attachment`() {
        val (_, _, res) = http.post("/pedagogical-document")
            .jsonBody("""{"childId": "${testChild_1.id}", "description": ""}""")
            .asUser(employee)
            .responseString()

        val id = deserializePostResult(res.get()).id
        val attachmentId = uploadAttachment(id)

        val (_, _, result) = http.get("/pedagogical-document/child/${testChild_1.id}")
            .asUser(employee)
            .responseString()

        val parsed = deserializeGetResult(result.get())
        assertEquals(1, parsed.size)

        val attachment = parsed.first().attachment
        assertNotNull(attachment)
        assertEquals(
            attachmentId,
            attachment.id
        )
    }

    @Test
    fun `supervisor can read pedagogical document by daycare`() {
        val (_, _, res) = http.post("/pedagogical-document")
            .jsonBody("""{"childId": "${testChild_1.id}", "description": ""}""")
            .asUser(employee)
            .responseString()

        val id = deserializePostResult(res.get()).id
        val attachmentId = uploadAttachment(id)

        val (_, _, result) = http.get("/pedagogical-document/child/${testChild_1.id}")
            .asUser(supervisor)
            .responseString()

        val parsed = deserializeGetResult(result.get())
        assertEquals(1, parsed.size)

        val attachment = parsed.first().attachment
        assertNotNull(attachment)
        assertEquals(
            attachmentId,
            attachment.id
        )
    }

    @Test
    fun `employee can't read pedagogical document if child is in another unit`() {
        db.transaction {
            it.insertTestPlacement(childId = testChild_2.id, unitId = testDaycare2.id, endDate = LocalDate.MAX)
        }
        val (_, _, res1) = http.post("/pedagogical-document")
            .jsonBody("""{"childId": "${testChild_2.id}", "description": ""}""")
            .asUser(employee)
            .responseString()

        val id = deserializePostResult(res1.get()).id
        uploadAttachment(id)

        val (_, res2, _) = http.get("/pedagogical-document/child/${testChild_2.id}")
            .asUser(supervisor)
            .responseString()

        assertEquals(403, res2.statusCode)
    }

    @Test
    fun `staff from group can read pedagogical document`() {
        val (_, _, res) = http.post("/pedagogical-document")
            .jsonBody("""{"childId": "${testChild_1.id}", "description": ""}""")
            .asUser(employee)
            .responseString()

        val id = deserializePostResult(res.get()).id
        val attachmentId = uploadAttachment(id)

        val (_, _, result) = http.get("/pedagogical-document/child/${testChild_1.id}")
            .asUser(supervisor)
            .responseString()

        val parsed = deserializeGetResult(result.get())
        assertEquals(1, parsed.size)

        val attachment = parsed.first().attachment
        assertNotNull(attachment)
        assertEquals(
            attachmentId,
            attachment.id
        )
    }

    @Test
    fun `staff from group can read pedagogical document attachment`() {
        val (_, _, res) = http.post("/pedagogical-document")
            .jsonBody("""{"childId": "${testChild_1.id}", "description": ""}""")
            .asUser(employee)
            .responseString()

        val id = deserializePostResult(res.get()).id
        val attachmentId = uploadAttachment(id)

        val (_, _, result) = http.get("/pedagogical-document/child/${testChild_1.id}")
            .asUser(staff)
            .responseString()

        val parsed = deserializeGetResult(result.get())
        assertEquals(1, parsed.size)

        val attachment = parsed.first().attachment
        assertNotNull(attachment)
        assertEquals(
            attachmentId,
            attachment.id
        )

        val (_, res2, _) = http.get("/attachments/${attachment.id}/download")
            .asUser(staff)
            .responseString()

        assertEquals(200, res2.statusCode)
    }

    @Test
    fun `staff from another daycare can't read pedagogical document`() {
        val staff2Id = UUID.randomUUID()
        val staff2 = AuthenticatedUser.Employee(staff2Id, setOf())
        db.transaction {
            it.insertTestEmployee(DevEmployee(id = staff2Id, roles = setOf()))
            it.updateDaycareAclWithEmployee(testDaycare2.id, staff2Id, UserRole.STAFF)

            val group2Id = GroupId(UUID.randomUUID())
            it.insertTestDaycareGroup(DevDaycareGroup(group2Id, testDaycare2.id))
            it.updateDaycareGroupAclWithEmployee(group2Id, staff2Id)
        }
        val (_, _, res1) = http.post("/pedagogical-document")
            .jsonBody("""{"childId": "${testChild_1.id}", "description": ""}""")
            .asUser(employee)
            .responseString()

        val id = deserializePostResult(res1.get()).id
        uploadAttachment(id)

        val (_, res2, _) = http.get("/pedagogical-document/child/${testChild_1.id}")
            .asUser(staff2)
            .responseString()

        assertEquals(403, res2.statusCode)
    }

    @Test
    fun `staff from another group can't read pedagogical document`() {
        val staff2Id = UUID.randomUUID()
        val staff2 = AuthenticatedUser.Employee(staff2Id, setOf())
        db.transaction {
            it.insertTestEmployee(DevEmployee(id = staff2Id, roles = setOf()))

            val group2Id = GroupId(UUID.randomUUID())
            it.insertTestDaycareGroup(DevDaycareGroup(group2Id, testDaycare.id))
            it.updateDaycareGroupAclWithEmployee(group2Id, staff2Id)
        }

        val (_, _, res1) = http.post("/pedagogical-document")
            .jsonBody("""{"childId": "${testChild_1.id}", "description": ""}""")
            .asUser(employee)
            .responseString()

        val id = deserializePostResult(res1.get()).id
        uploadAttachment(id)

        val (_, res2, _) = http.get("/pedagogical-document/child/${testChild_1.id}")
            .asUser(staff2)
            .responseString()

        assertEquals(403, res2.statusCode)
    }

    @Test
    fun `staff from another group can't read pedagogical document attachment`() {
        val staff2Id = UUID.randomUUID()
        val staff2 = AuthenticatedUser.Employee(staff2Id, setOf())
        db.transaction {
            it.insertTestEmployee(DevEmployee(id = staff2Id, roles = setOf()))

            val group2Id = GroupId(UUID.randomUUID())
            it.insertTestDaycareGroup(DevDaycareGroup(group2Id, testDaycare.id))
            it.updateDaycareGroupAclWithEmployee(group2Id, staff2Id)
        }

        val (_, _, res1) = http.post("/pedagogical-document")
            .jsonBody("""{"childId": "${testChild_1.id}", "description": ""}""")
            .asUser(employee)
            .responseString()

        val id = deserializePostResult(res1.get()).id
        val attachmentId = uploadAttachment(id)

        val (_, res2, _) = http.get("/attachments/$attachmentId/download")
            .asUser(staff2)
            .responseString()

        assertEquals(403, res2.statusCode)
    }

    private fun uploadAttachment(id: PedagogicalDocumentId): AttachmentId {
        val (_, _, result) = http.upload("/attachments/pedagogical-documents/$id")
            .add(FileDataPart(File(pngFile.toURI()), name = "file"))
            .asUser(employee)
            .responseObject<AttachmentId>(objectMapper)

        return result.get()
    }
}
