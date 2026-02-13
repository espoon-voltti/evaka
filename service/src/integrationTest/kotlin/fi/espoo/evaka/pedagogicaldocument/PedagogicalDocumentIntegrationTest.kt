// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pedagogicaldocument

import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.attachment.AttachmentsController
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevGuardian
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertEmployeeToDaycareGroupAcl
import fi.espoo.evaka.shared.dev.updateDaycareAclWithEmployee
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.security.PilotFeature
import java.io.File
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import tools.jackson.module.kotlin.readValue

class PedagogicalDocumentIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var pedagogicalDocumentController: PedagogicalDocumentController
    @Autowired
    private lateinit var pedagogicalDocumentControllerCitizen: PedagogicalDocumentControllerCitizen
    @Autowired private lateinit var attachmentsController: AttachmentsController

    private val clock = MockEvakaClock(2024, 1, 3, 3, 7)

    private val area = DevCareArea()
    private val daycare =
        DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.VASU_AND_PEDADOC))
    private val daycare2 = DevDaycare(areaId = area.id, name = "Test Daycare 2")
    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val supervisorEmployee = DevEmployee()
    private val staffEmployee = DevEmployee()
    private val groupStaffEmployee = DevEmployee()
    private val adult = DevPerson()
    private val child1 = DevPerson()
    private val child2 = DevPerson()
    private val group = DevDaycareGroup(daycareId = daycare.id)

    private fun deserializeGetResult(json: String) =
        jsonMapper.readValue<List<PedagogicalDocument>>(json)

    private fun deserializeGetResultCitizen(json: String) =
        jsonMapper.readValue<List<PedagogicalDocumentCitizen>>(json)

    private fun deserializePutResult(json: String) = jsonMapper.readValue<PedagogicalDocument>(json)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(daycare2)
            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child1, DevPersonType.CHILD)
            tx.insert(child2, DevPersonType.CHILD)
            tx.insert(admin)
            tx.insert(supervisorEmployee)
            tx.insert(staffEmployee)
            tx.insert(groupStaffEmployee)

            tx.updateDaycareAclWithEmployee(
                daycare.id,
                supervisorEmployee.id,
                UserRole.UNIT_SUPERVISOR,
            )
            tx.updateDaycareAclWithEmployee(daycare.id, staffEmployee.id, UserRole.STAFF)
            tx.updateDaycareAclWithEmployee(daycare.id, groupStaffEmployee.id, UserRole.STAFF)

            tx.insert(group)
            tx.insertEmployeeToDaycareGroupAcl(group.id, groupStaffEmployee.id)

            val placementId =
                tx.insert(
                    DevPlacement(childId = child1.id, unitId = daycare.id, endDate = LocalDate.MAX)
                )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId,
                    daycareGroupId = group.id,
                    endDate = LocalDate.MAX,
                )
            )

            tx.insertGuardian(adult.id, child1.id)
        }
    }

    private fun createDocumentAsUser(
        childId: ChildId,
        user: AuthenticatedUser.Employee = admin.user,
    ) =
        pedagogicalDocumentController.createPedagogicalDocument(
            dbInstance(),
            user,
            clock,
            PedagogicalDocumentPostBody(childId, ""),
        )

    private val guardianUser = adult.user(CitizenAuthLevel.STRONG)

    private fun getAttachmentAsUser(
        attachmentId: AttachmentId,
        user: AuthenticatedUser.Citizen,
        requestedFilename: String = "evaka-logo.png",
    ) =
        http
            .get("/citizen/attachments/$attachmentId/download/$requestedFilename")
            .asUser(user)
            .responseString()
            .second

    private fun getAttachmentAsUser(
        attachmentId: AttachmentId,
        user: AuthenticatedUser.Employee,
        requestedFilename: String = "evaka-logo.png",
    ) =
        http
            .get("/employee/attachments/$attachmentId/download/$requestedFilename")
            .asUser(user)
            .responseString()
            .second

    private fun getPedagogicalDocumentAsUser(childId: ChildId, user: AuthenticatedUser) =
        http.get("/employee/pedagogical-document/child/$childId").asUser(user).responseString()

    private fun getPedagogicalDocumentsAsCitizen(
        user: AuthenticatedUser.Citizen,
        childId: ChildId,
    ) = http.get("/citizen/children/$childId/pedagogical-documents").asUser(user).responseString()

    private fun getUnreadCount(user: AuthenticatedUser.Citizen) =
        http
            .get("/citizen/pedagogical-documents/unread-count")
            .asUser(user)
            .responseObject<Map<ChildId, Int>>(jackson2JsonMapper)
            .third
            .get()

    @Test
    fun `creating new document`() {
        val result = createDocumentAsUser(child1.id)

        assertNotNull(result.id)
        assertEquals(child1.id, result.childId)
    }

    @Test
    fun `updating document`() {
        val id = createDocumentAsUser(child1.id).id

        val (_, _, result) =
            http
                .put("/employee/pedagogical-document/$id")
                .jsonBody(
                    """{"id": "$id", "childId": "${child1.id}", "description": "foobar", "attachmentId": null}"""
                )
                .asUser(admin.user)
                .responseString()

        assertEquals("foobar", deserializePutResult(result.get()).description)
    }

    @Test
    fun `find updated document`() {
        val id = createDocumentAsUser(child1.id).id

        val testDescription = "foobar"
        http
            .put("/employee/pedagogical-document/$id")
            .jsonBody(
                """{"id": "$id", "childId": "${child1.id}", "description": "$testDescription", "attachmentId": null}"""
            )
            .asUser(admin.user)
            .responseString()

        val parsed =
            deserializeGetResult(getPedagogicalDocumentAsUser(child1.id, admin.user).third.get())

        assertEquals(1, parsed.size)
        assertEquals(testDescription, parsed.first().description)
    }

    @Test
    fun `admin can read document with attachment`() {
        val res = createDocumentAsUser(child1.id)

        val attachmentId = uploadAttachment(res.id)

        val parsed =
            deserializeGetResult(getPedagogicalDocumentAsUser(child1.id, admin.user).third.get())
        assertEquals(1, parsed.size)

        val attachment = parsed.first().attachments[0]
        assertNotNull(attachment)
        assertEquals(attachmentId, attachment.id)
    }

    @Test
    fun `supervisor can read pedagogical document and attachment by daycare`() {
        val res = createDocumentAsUser(child1.id)

        val attachmentId = uploadAttachment(res.id)

        val result = getPedagogicalDocumentAsUser(child1.id, supervisorEmployee.user).third

        val parsed = deserializeGetResult(result.get())
        assertEquals(1, parsed.size)

        val attachment = parsed.first().attachments[0]
        assertNotNull(attachment)
        assertEquals(attachmentId, attachment.id)

        val res2 = getAttachmentAsUser(attachmentId, supervisorEmployee.user, attachment.name)
        assertEquals(200, res2.statusCode)
    }

    @Test
    fun `supervisor can't read pedagogical document or attachment if child is in another unit`() {
        db.transaction {
            it.insert(
                DevPlacement(childId = child2.id, unitId = daycare2.id, endDate = LocalDate.MAX)
            )
        }

        createDocumentAsUser(child2.id)

        val id = createDocumentAsUser(child2.id).id
        val attachmentId = uploadAttachment(id)

        assertEquals(
            403,
            getPedagogicalDocumentAsUser(child2.id, supervisorEmployee.user).second.statusCode,
        )
        assertEquals(403, getAttachmentAsUser(attachmentId, supervisorEmployee.user).statusCode)
    }

    @Test
    fun `staff from daycare can read pedagogical document and attachment`() {
        val res = createDocumentAsUser(child1.id)

        val attachmentId = uploadAttachment(res.id)

        val parsed =
            deserializeGetResult(
                getPedagogicalDocumentAsUser(child1.id, staffEmployee.user).third.get()
            )

        assertEquals(1, parsed.size)

        val attachment = parsed.first().attachments[0]
        assertNotNull(attachment)
        assertEquals(attachmentId, attachment.id)

        assertEquals(
            200,
            getAttachmentAsUser(attachment.id, staffEmployee.user, attachment.name).statusCode,
        )
    }

    @Test
    fun `staff from group can read pedagogical document and attachment`() {
        val res = createDocumentAsUser(child1.id)

        val attachmentId = uploadAttachment(res.id)

        val parsed =
            deserializeGetResult(
                getPedagogicalDocumentAsUser(child1.id, groupStaffEmployee.user).third.get()
            )
        assertEquals(1, parsed.size)

        val attachment = parsed.first().attachments[0]
        assertNotNull(attachment)
        assertEquals(attachmentId, attachment.id)

        assertEquals(
            200,
            getAttachmentAsUser(attachment.id, groupStaffEmployee.user, attachment.name).statusCode,
        )
    }

    @Test
    fun `staff from another daycare can't read pedagogical document or attachment`() {
        val otherStaff = DevEmployee()
        db.transaction {
            it.insert(otherStaff)
            it.updateDaycareAclWithEmployee(daycare2.id, otherStaff.id, UserRole.STAFF)

            val otherGroup = DevDaycareGroup(daycareId = daycare2.id)
            it.insert(otherGroup)
            it.insertEmployeeToDaycareGroupAcl(otherGroup.id, otherStaff.id)
        }

        val id = createDocumentAsUser(child1.id).id
        val attachmentId = uploadAttachment(id)

        assertEquals(
            403,
            getPedagogicalDocumentAsUser(child1.id, otherStaff.user).second.statusCode,
        )
        assertEquals(403, getAttachmentAsUser(attachmentId, otherStaff.user).statusCode)
    }

    @Test
    fun `staff from another group can't read pedagogical document or attachment`() {
        val otherStaff = DevEmployee()
        db.transaction {
            it.insert(otherStaff)

            val otherGroup = DevDaycareGroup(daycareId = daycare.id, name = "Other Group")
            it.insert(otherGroup)
            it.insertEmployeeToDaycareGroupAcl(otherGroup.id, otherStaff.id)
        }

        val id = createDocumentAsUser(child1.id).id
        val attachmentId = uploadAttachment(id)

        assertEquals(
            403,
            getPedagogicalDocumentAsUser(child1.id, otherStaff.user).second.statusCode,
        )
        assertEquals(403, getAttachmentAsUser(attachmentId, otherStaff.user).statusCode)
    }

    @Test
    fun `guardian can read pedagogical document and attachment`() {
        val res = createDocumentAsUser(child1.id)

        val attachmentId = uploadAttachment(res.id)

        val parsed =
            deserializeGetResultCitizen(
                getPedagogicalDocumentsAsCitizen(guardianUser, child1.id).third.get()
            )
        assertEquals(1, parsed.size)

        val attachment = parsed.first().attachments[0]
        assertNotNull(attachment)
        assertEquals(attachmentId, attachment.id)

        assertEquals(
            200,
            getAttachmentAsUser(attachment.id, guardianUser, attachment.name).statusCode,
        )
    }

    @Test
    fun `guardian finds new document only when it has attachment or description`() {
        val id1 = createDocumentAsUser(child1.id).id

        assertEquals(
            emptyList(),
            deserializeGetResultCitizen(
                getPedagogicalDocumentsAsCitizen(guardianUser, child1.id).third.get()
            ),
        )
        assertEquals(0, getUnreadCount(guardianUser).values.sum())

        uploadAttachment(id1)

        assertEquals(
            1,
            deserializeGetResultCitizen(
                    getPedagogicalDocumentsAsCitizen(guardianUser, child1.id).third.get()
                )
                .size,
        )
        assertEquals(1, getUnreadCount(guardianUser).values.sum())

        val id2 = createDocumentAsUser(child1.id).id

        assertEquals(
            1,
            deserializeGetResultCitizen(
                    getPedagogicalDocumentsAsCitizen(guardianUser, child1.id).third.get()
                )
                .size,
        )

        http
            .put("/employee/pedagogical-document/$id2")
            .jsonBody("""{"id": "$id2", "childId": "${child1.id}", "description": "123123"}""")
            .asUser(admin.user)
            .responseString()

        assertEquals(
            2,
            deserializeGetResultCitizen(
                    getPedagogicalDocumentsAsCitizen(guardianUser, child1.id).third.get()
                )
                .size,
        )
        // only docs with attachments are counted in unread
        assertEquals(1, getUnreadCount(guardianUser).values.sum())
    }

    @Test
    fun `guardian can't read documents or attachments if child's placement has ended`() {
        val otherAdult = DevPerson()
        val otherChild = DevPerson()
        val otherGuardian = otherAdult.user(CitizenAuthLevel.STRONG)
        val otherArea = DevCareArea(name = "Other Area", shortName = "other_area")
        val otherDaycare = DevDaycare(areaId = otherArea.id, name = "Other Daycare")
        db.transaction {
            it.insert(otherAdult, DevPersonType.ADULT)
            it.insert(otherChild, DevPersonType.CHILD)
            it.insert(otherArea)
            it.insert(otherDaycare)
            it.insert(DevGuardian(otherAdult.id, otherChild.id))
            it.insert(
                DevPlacement(
                    childId = otherChild.id,
                    unitId = otherDaycare.id,
                    startDate = clock.now().minusDays(1).toLocalDate(),
                    endDate = clock.now().minusDays(1).toLocalDate(),
                )
            )
        }

        val pedDoc = createDocumentAsUser(otherChild.id)
        val attachmentId = uploadAttachment(pedDoc.id)

        assertThrows<Forbidden> {
            pedagogicalDocumentControllerCitizen.getPedagogicalDocumentsForChild(
                dbInstance(),
                otherGuardian,
                clock,
                otherChild.id,
            )
        }

        assertThrows<Forbidden> {
            attachmentsController.getAttachment(
                dbInstance(),
                otherGuardian,
                clock,
                attachmentId,
                "file",
            )
        }
    }

    @Test
    fun `marking document read works`() {
        val id1 = createDocumentAsUser(child1.id).id
        val attachmentId = uploadAttachment(id1)

        http
            .put("/employee/pedagogical-document/$id1")
            .jsonBody(
                """{"id": "$id1", "childId": "${child1.id}", "description": "123123", "attachmentId": "$attachmentId"}"""
            )
            .asUser(admin.user)
            .responseString()

        assertEquals(1, getUnreadCount(guardianUser).values.sum())

        http
            .post("/citizen/pedagogical-documents/$id1/mark-read")
            .asUser(guardianUser)
            .responseString()

        assertEquals(0, getUnreadCount(guardianUser).values.sum())
    }

    @Test
    fun `admin can delete read document with attachment`() {
        val id = createDocumentAsUser(child1.id).id
        uploadAttachment(id)

        http
            .post("/employee/pedagogical-document/$id/mark-read")
            .asUser(admin.user)
            .responseString()

        assertEquals(
            1,
            deserializeGetResult(getPedagogicalDocumentAsUser(child1.id, admin.user).third.get())
                .size,
        )
        assertEquals(1, getUnreadCount(guardianUser).values.sum())

        val (_, res, _) =
            http.delete("/employee/pedagogical-document/$id").asUser(admin.user).responseString()

        assertEquals(200, res.statusCode)
        assertEquals(
            0,
            deserializeGetResult(getPedagogicalDocumentAsUser(child1.id, admin.user).third.get())
                .size,
        )
        assertEquals(
            0,
            deserializeGetResultCitizen(
                    getPedagogicalDocumentsAsCitizen(guardianUser, child1.id).third.get()
                )
                .size,
        )
    }

    private fun uploadAttachment(id: PedagogicalDocumentId): AttachmentId {
        val (_, _, result) =
            http
                .upload("/employee/attachments/pedagogical-documents/$id")
                .add(FileDataPart(File(pngFile.toURI()), name = "file"))
                .asUser(admin.user)
                .responseObject<AttachmentId>(jackson2JsonMapper)

        return result.get()
    }
}
