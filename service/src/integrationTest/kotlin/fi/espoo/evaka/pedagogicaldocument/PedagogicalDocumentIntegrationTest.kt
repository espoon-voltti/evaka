// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pedagogicaldocument

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.addUnitFeatures
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertEmployeeToDaycareGroupAcl
import fi.espoo.evaka.shared.dev.updateDaycareAclWithEmployee
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PedagogicalDocumentIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val employeeId = EmployeeId(UUID.randomUUID())
    private val employee = AuthenticatedUser.Employee(employeeId, setOf(UserRole.ADMIN))

    private val supervisorId = EmployeeId(UUID.randomUUID())
    private val supervisor = AuthenticatedUser.Employee(supervisorId, setOf())

    private val staffId = EmployeeId(UUID.randomUUID())
    private val staff = AuthenticatedUser.Employee(staffId, setOf())

    private val groupStaffId = EmployeeId(UUID.randomUUID())
    private val groupStaff = AuthenticatedUser.Employee(groupStaffId, setOf())

    private val guardian = AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG)

    val groupId = GroupId(UUID.randomUUID())

    private fun deserializeGetResult(json: String) =
        jsonMapper.readValue<List<PedagogicalDocument>>(json)

    private fun deserializeGetResultCitizen(json: String) =
        jsonMapper.readValue<List<PedagogicalDocumentCitizen>>(json)

    private fun deserializePutResult(json: String) = jsonMapper.readValue<PedagogicalDocument>(json)

    private fun deserializePostResult(json: String) =
        jsonMapper.readValue<PedagogicalDocument>(json)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            listOf(testChild_1, testChild_2).forEach { tx.insert(it, DevPersonType.CHILD) }
            tx.addUnitFeatures(listOf(testDaycare.id), listOf(PilotFeature.VASU_AND_PEDADOC))
            tx.insert(DevEmployee(id = employeeId, roles = setOf(UserRole.ADMIN)))
            tx.insert(DevEmployee(id = supervisorId, roles = setOf()))
            tx.insert(DevEmployee(id = staffId, roles = setOf()))
            tx.insert(DevEmployee(id = groupStaffId, roles = setOf()))

            tx.updateDaycareAclWithEmployee(testDaycare.id, supervisorId, UserRole.UNIT_SUPERVISOR)
            tx.updateDaycareAclWithEmployee(testDaycare.id, staffId, UserRole.STAFF)
            tx.updateDaycareAclWithEmployee(testDaycare.id, groupStaffId, UserRole.STAFF)

            tx.insert(DevDaycareGroup(groupId, testDaycare.id))
            tx.insertEmployeeToDaycareGroupAcl(groupId, groupStaffId)

            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = testChild_1.id,
                        unitId = testDaycare.id,
                        endDate = LocalDate.MAX
                    )
                )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId,
                    daycareGroupId = groupId,
                    endDate = LocalDate.MAX
                )
            )

            tx.insertGuardian(testAdult_1.id, testChild_1.id)
        }
    }

    private fun createDocumentAsUser(childId: ChildId, user: AuthenticatedUser) =
        http
            .post("/pedagogical-document")
            .jsonBody("""{"childId": "$childId", "description": ""}""")
            .asUser(user)
            .responseString()
            .third

    private fun getAttachmentAsUser(
        attachmentId: AttachmentId,
        user: AuthenticatedUser,
        requestedFilename: String = "evaka-logo.png"
    ) =
        http
            .get("/attachments/$attachmentId/download/$requestedFilename")
            .asUser(user)
            .responseString()
            .second

    private fun getPedagogicalDocumentAsUser(childId: ChildId, user: AuthenticatedUser) =
        http.get("/pedagogical-document/child/$childId").asUser(user).responseString()

    private fun getPedagogicalDocumentsAsCitizen(
        user: AuthenticatedUser.Citizen,
        childId: ChildId
    ) = http.get("/citizen/children/$childId/pedagogical-documents").asUser(user).responseString()

    private fun getUnreadCount(user: AuthenticatedUser.Citizen) =
        http
            .get("/citizen/pedagogical-documents/unread-count")
            .asUser(user)
            .responseObject<Map<ChildId, Int>>(jsonMapper)
            .third
            .get()

    @Test
    fun `creating new document`() {
        val result = createDocumentAsUser(testChild_1.id, employee)

        assertNotNull(deserializePostResult(result.get()).id)
        assertEquals(testChild_1.id, deserializePostResult(result.get()).childId)
    }

    @Test
    fun `updating document`() {
        val id = deserializePostResult(createDocumentAsUser(testChild_1.id, employee).get()).id

        val (_, _, result) =
            http
                .put("/pedagogical-document/$id")
                .jsonBody(
                    """{"id": "$id", "childId": "${testChild_1.id}", "description": "foobar", "attachmentId": null}"""
                )
                .asUser(employee)
                .responseString()

        assertEquals("foobar", deserializePutResult(result.get()).description)
    }

    @Test
    fun `find updated document`() {
        val id = deserializePostResult(createDocumentAsUser(testChild_1.id, employee).get()).id

        val testDescription = "foobar"
        http
            .put("/pedagogical-document/$id")
            .jsonBody(
                """{"id": "$id", "childId": "${testChild_1.id}", "description": "$testDescription", "attachmentId": null}"""
            )
            .asUser(employee)
            .responseString()

        val parsed =
            deserializeGetResult(getPedagogicalDocumentAsUser(testChild_1.id, employee).third.get())

        assertEquals(1, parsed.size)
        assertEquals(testDescription, parsed.first().description)
    }

    @Test
    fun `admin can read document with attachment`() {
        val res = createDocumentAsUser(testChild_1.id, employee)

        val id = deserializePostResult(res.get()).id
        val attachmentId = uploadAttachment(id)

        val parsed =
            deserializeGetResult(getPedagogicalDocumentAsUser(testChild_1.id, employee).third.get())
        assertEquals(1, parsed.size)

        val attachment = parsed.first().attachments.get(0)
        assertNotNull(attachment)
        assertEquals(attachmentId, attachment.id)
    }

    @Test
    fun `supervisor can read pedagogical document and attachment by daycare`() {
        val res = createDocumentAsUser(testChild_1.id, employee)

        val id = deserializePostResult(res.get()).id
        val attachmentId = uploadAttachment(id)

        val result = getPedagogicalDocumentAsUser(testChild_1.id, supervisor).third

        val parsed = deserializeGetResult(result.get())
        assertEquals(1, parsed.size)

        val attachment = parsed.first().attachments.get(0)
        assertNotNull(attachment)
        assertEquals(attachmentId, attachment.id)

        val res2 = getAttachmentAsUser(attachmentId, supervisor, attachment.name)
        assertEquals(200, res2.statusCode)
    }

    @Test
    fun `supervisor can't read pedagogical document or attachment if child is in another unit`() {
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = testChild_2.id,
                    unitId = testDaycare2.id,
                    endDate = LocalDate.MAX
                )
            )
        }

        createDocumentAsUser(testChild_2.id, employee)

        val id = deserializePostResult(createDocumentAsUser(testChild_2.id, employee).get()).id
        val attachmentId = uploadAttachment(id)

        assertEquals(
            403,
            getPedagogicalDocumentAsUser(testChild_2.id, supervisor).second.statusCode
        )
        assertEquals(403, getAttachmentAsUser(attachmentId, supervisor).statusCode)
    }

    @Test
    fun `staff from daycare can read pedagogical document and attachment`() {
        val res = createDocumentAsUser(testChild_1.id, employee)

        val id = deserializePostResult(res.get()).id
        val attachmentId = uploadAttachment(id)

        val parsed =
            deserializeGetResult(getPedagogicalDocumentAsUser(testChild_1.id, staff).third.get())

        assertEquals(1, parsed.size)

        val attachment = parsed.first().attachments.get(0)
        assertNotNull(attachment)
        assertEquals(attachmentId, attachment.id)

        assertEquals(200, getAttachmentAsUser(attachment.id, staff, attachment.name).statusCode)
    }

    @Test
    fun `staff from group can read pedagogical document and attachment`() {
        val res = createDocumentAsUser(testChild_1.id, employee)

        val id = deserializePostResult(res.get()).id
        val attachmentId = uploadAttachment(id)

        val parsed =
            deserializeGetResult(
                getPedagogicalDocumentAsUser(testChild_1.id, groupStaff).third.get()
            )
        assertEquals(1, parsed.size)

        val attachment = parsed.first().attachments.get(0)
        assertNotNull(attachment)
        assertEquals(attachmentId, attachment.id)

        assertEquals(
            200,
            getAttachmentAsUser(attachment.id, groupStaff, attachment.name).statusCode
        )
    }

    @Test
    fun `staff from another daycare can't read pedagogical document or attachment`() {
        val staff2Id = EmployeeId(UUID.randomUUID())
        val staff2 = AuthenticatedUser.Employee(staff2Id, setOf())
        db.transaction {
            it.insert(DevEmployee(id = staff2Id, roles = setOf()))
            it.updateDaycareAclWithEmployee(testDaycare2.id, staff2Id, UserRole.STAFF)

            val group2Id = GroupId(UUID.randomUUID())
            it.insert(DevDaycareGroup(group2Id, testDaycare2.id))
            it.insertEmployeeToDaycareGroupAcl(group2Id, staff2Id)
        }

        val id = deserializePostResult(createDocumentAsUser(testChild_1.id, employee).get()).id
        val attachmentId = uploadAttachment(id)

        assertEquals(403, getPedagogicalDocumentAsUser(testChild_1.id, staff2).second.statusCode)
        assertEquals(403, getAttachmentAsUser(attachmentId, staff2).statusCode)
    }

    @Test
    fun `staff from another group can't read pedagogical document or attachment`() {
        val staff2Id = EmployeeId(UUID.randomUUID())
        val staff2 = AuthenticatedUser.Employee(staff2Id, setOf())
        db.transaction {
            it.insert(DevEmployee(id = staff2Id, roles = setOf()))

            val group2Id = GroupId(UUID.randomUUID())
            it.insert(DevDaycareGroup(group2Id, testDaycare.id))
            it.insertEmployeeToDaycareGroupAcl(group2Id, staff2Id)
        }

        val id = deserializePostResult(createDocumentAsUser(testChild_1.id, employee).get()).id
        val attachmentId = uploadAttachment(id)

        assertEquals(403, getPedagogicalDocumentAsUser(testChild_1.id, staff2).second.statusCode)
        assertEquals(403, getAttachmentAsUser(attachmentId, staff2).statusCode)
    }

    @Test
    fun `guardian can read pedagogical document and attachment`() {
        val res = createDocumentAsUser(testChild_1.id, employee)

        val id = deserializePostResult(res.get()).id
        val attachmentId = uploadAttachment(id)

        val parsed =
            deserializeGetResultCitizen(
                getPedagogicalDocumentsAsCitizen(guardian, testChild_1.id).third.get()
            )
        assertEquals(1, parsed.size)

        val attachment = parsed.first().attachments.get(0)
        assertNotNull(attachment)
        assertEquals(attachmentId, attachment.id)

        assertEquals(200, getAttachmentAsUser(attachment.id, guardian, attachment.name).statusCode)
    }

    @Test
    fun `guardian finds new document only when it has attachment or description`() {
        val id1 = deserializePostResult(createDocumentAsUser(testChild_1.id, employee).get()).id

        assertEquals(
            emptyList(),
            deserializeGetResultCitizen(
                getPedagogicalDocumentsAsCitizen(guardian, testChild_1.id).third.get()
            )
        )
        assertEquals(0, getUnreadCount(guardian).values.sum())

        uploadAttachment(id1)

        assertEquals(
            1,
            deserializeGetResultCitizen(
                    getPedagogicalDocumentsAsCitizen(guardian, testChild_1.id).third.get()
                )
                .size
        )
        assertEquals(1, getUnreadCount(guardian).values.sum())

        val id2 = deserializePostResult(createDocumentAsUser(testChild_1.id, employee).get()).id

        assertEquals(
            1,
            deserializeGetResultCitizen(
                    getPedagogicalDocumentsAsCitizen(guardian, testChild_1.id).third.get()
                )
                .size
        )

        http
            .put("/pedagogical-document/$id2")
            .jsonBody("""{"id": "$id2", "childId": "${testChild_1.id}", "description": "123123"}""")
            .asUser(employee)
            .responseString()

        assertEquals(
            2,
            deserializeGetResultCitizen(
                    getPedagogicalDocumentsAsCitizen(guardian, testChild_1.id).third.get()
                )
                .size
        )
        // only docs with attachments are counted in unread
        assertEquals(1, getUnreadCount(guardian).values.sum())
    }

    @Test
    fun `marking document read works`() {
        val id1 = deserializePostResult(createDocumentAsUser(testChild_1.id, employee).get()).id
        val attachmentId = uploadAttachment(id1)

        http
            .put("/pedagogical-document/$id1")
            .jsonBody(
                """{"id": "$id1", "childId": "${testChild_1.id}", "description": "123123", "attachmentId": "$attachmentId"}"""
            )
            .asUser(employee)
            .responseString()

        assertEquals(1, getUnreadCount(guardian).values.sum())

        http.post("/citizen/pedagogical-documents/$id1/mark-read").asUser(guardian).responseString()

        assertEquals(0, getUnreadCount(guardian).values.sum())
    }

    @Test
    fun `admin can delete read document with attachment`() {
        val id = deserializePostResult(createDocumentAsUser(testChild_1.id, employee).get()).id
        uploadAttachment(id)

        http.post("/pedagogical-document/$id/mark-read").asUser(employee).responseString()

        assertEquals(
            1,
            deserializeGetResult(getPedagogicalDocumentAsUser(testChild_1.id, employee).third.get())
                .size
        )
        assertEquals(1, getUnreadCount(guardian).values.sum())

        val (_, res, _) = http.delete("/pedagogical-document/$id").asUser(employee).responseString()

        assertEquals(200, res.statusCode)
        assertEquals(
            0,
            deserializeGetResult(getPedagogicalDocumentAsUser(testChild_1.id, employee).third.get())
                .size
        )
        assertEquals(
            0,
            deserializeGetResultCitizen(
                    getPedagogicalDocumentsAsCitizen(guardian, testChild_1.id).third.get()
                )
                .size
        )
    }

    private fun uploadAttachment(id: PedagogicalDocumentId): AttachmentId {
        val (_, _, result) =
            http
                .upload("/attachments/pedagogical-documents/$id")
                .add(FileDataPart(File(pngFile.toURI()), name = "file"))
                .asUser(employee)
                .responseObject<AttachmentId>(jsonMapper)

        return result.get()
    }
}
