// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pedagogicaldocument

import evaka.core.FullApplicationTest
import evaka.core.attachment.AttachmentsController
import evaka.core.pis.service.insertGuardian
import evaka.core.shared.AttachmentId
import evaka.core.shared.ChildId
import evaka.core.shared.PedagogicalDocumentId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.CitizenAuthLevel
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDaycareGroup
import evaka.core.shared.dev.DevDaycareGroupPlacement
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevGuardian
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertEmployeeToDaycareGroupAcl
import evaka.core.shared.dev.updateDaycareAclWithEmployee
import evaka.core.shared.domain.Forbidden
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.security.PilotFeature
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile

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

    @Test
    fun `creating new document`() {
        val result = createDocument(child1.id)

        assertNotNull(result.id)
        assertEquals(child1.id, result.childId)
    }

    @Test
    fun `updating document`() {
        val id = createDocument(child1.id).id

        val result = updateDocument(id, child1.id, "foobar")

        assertEquals("foobar", result.description)
    }

    @Test
    fun `find updated document`() {
        val id = createDocument(child1.id).id

        val testDescription = "foobar"
        updateDocument(id, child1.id, testDescription)

        val parsed = getDocuments(child1.id)

        assertEquals(1, parsed.size)
        assertEquals(testDescription, parsed.first().description)
    }

    @Test
    fun `admin can read document with attachment`() {
        val res = createDocument(child1.id)

        val attachmentId = uploadAttachment(res.id)

        val parsed = getDocuments(child1.id)
        assertEquals(1, parsed.size)

        val attachment = parsed.first().attachments[0]
        assertNotNull(attachment)
        assertEquals(attachmentId, attachment.id)
    }

    @Test
    fun `supervisor can read pedagogical document and attachment by daycare`() {
        val res = createDocument(child1.id)

        val attachmentId = uploadAttachment(res.id)

        val parsed = getDocuments(child1.id, supervisorEmployee.user)
        assertEquals(1, parsed.size)

        val attachment = parsed.first().attachments[0]
        assertNotNull(attachment)
        assertEquals(attachmentId, attachment.id)

        getAttachment(attachmentId, supervisorEmployee.user, attachment.name)
    }

    @Test
    fun `supervisor can't read pedagogical document or attachment if child is in another unit`() {
        db.transaction {
            it.insert(
                DevPlacement(childId = child2.id, unitId = daycare2.id, endDate = LocalDate.MAX)
            )
        }

        createDocument(child2.id)

        val id = createDocument(child2.id).id
        val attachmentId = uploadAttachment(id)

        assertThrows<Forbidden> { getDocuments(child2.id, supervisorEmployee.user) }
        assertThrows<Forbidden> { getAttachment(attachmentId, supervisorEmployee.user) }
    }

    @Test
    fun `staff from daycare can read pedagogical document and attachment`() {
        val res = createDocument(child1.id)

        val attachmentId = uploadAttachment(res.id)

        val parsed = getDocuments(child1.id, staffEmployee.user)

        assertEquals(1, parsed.size)

        val attachment = parsed.first().attachments[0]
        assertNotNull(attachment)
        assertEquals(attachmentId, attachment.id)

        getAttachment(attachment.id, staffEmployee.user, attachment.name)
    }

    @Test
    fun `staff from group can read pedagogical document and attachment`() {
        val res = createDocument(child1.id)

        val attachmentId = uploadAttachment(res.id)

        val parsed = getDocuments(child1.id, groupStaffEmployee.user)
        assertEquals(1, parsed.size)

        val attachment = parsed.first().attachments[0]
        assertNotNull(attachment)
        assertEquals(attachmentId, attachment.id)

        getAttachment(attachment.id, groupStaffEmployee.user, attachment.name)
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

        val id = createDocument(child1.id).id
        val attachmentId = uploadAttachment(id)

        assertThrows<Forbidden> { getDocuments(child1.id, otherStaff.user) }
        assertThrows<Forbidden> { getAttachment(attachmentId, otherStaff.user) }
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

        val id = createDocument(child1.id).id
        val attachmentId = uploadAttachment(id)

        assertThrows<Forbidden> { getDocuments(child1.id, otherStaff.user) }
        assertThrows<Forbidden> { getAttachment(attachmentId, otherStaff.user) }
    }

    @Test
    fun `guardian can read pedagogical document and attachment`() {
        val res = createDocument(child1.id)

        val attachmentId = uploadAttachment(res.id)

        val parsed = getDocumentsAsCitizen(child1.id)
        assertEquals(1, parsed.size)

        val attachment = parsed.first().attachments[0]
        assertNotNull(attachment)
        assertEquals(attachmentId, attachment.id)

        getAttachment(attachment.id, guardianUser, attachment.name)
    }

    @Test
    fun `guardian finds new document only when it has attachment or description`() {
        val id1 = createDocument(child1.id).id

        assertEquals(emptyList(), getDocumentsAsCitizen(child1.id))
        assertEquals(0, getUnreadCount().values.sum())

        uploadAttachment(id1)

        assertEquals(1, getDocumentsAsCitizen(child1.id).size)
        assertEquals(1, getUnreadCount().values.sum())

        val id2 = createDocument(child1.id).id

        assertEquals(1, getDocumentsAsCitizen(child1.id).size)

        updateDocument(id2, child1.id, "123123")

        assertEquals(2, getDocumentsAsCitizen(child1.id).size)
        // only docs with attachments are counted in unread
        assertEquals(1, getUnreadCount().values.sum())
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

        val pedDoc = createDocument(otherChild.id)
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
        val id1 = createDocument(child1.id).id
        val attachmentId = uploadAttachment(id1)

        updateDocument(id1, child1.id, "123123")

        assertEquals(1, getUnreadCount().values.sum())

        markDocumentRead(id1)

        assertEquals(0, getUnreadCount().values.sum())
    }

    @Test
    fun `admin can delete read document with attachment`() {
        val id = createDocument(child1.id).id
        uploadAttachment(id)

        assertEquals(1, getDocuments(child1.id).size)
        assertEquals(1, getUnreadCount().values.sum())

        deleteDocument(id)

        assertEquals(0, getDocuments(child1.id).size)
        assertEquals(0, getDocumentsAsCitizen(child1.id).size)
    }

    private val guardianUser = adult.user(CitizenAuthLevel.STRONG)

    private fun createDocument(childId: ChildId, user: AuthenticatedUser.Employee = admin.user) =
        pedagogicalDocumentController.createPedagogicalDocument(
            dbInstance(),
            user,
            clock,
            PedagogicalDocumentPostBody(childId, ""),
        )

    private fun updateDocument(
        documentId: PedagogicalDocumentId,
        childId: ChildId,
        description: String,
        user: AuthenticatedUser.Employee = admin.user,
    ) =
        pedagogicalDocumentController.updatePedagogicalDocument(
            dbInstance(),
            user,
            clock,
            documentId,
            PedagogicalDocumentPostBody(childId, description),
        )

    private fun getDocuments(childId: ChildId, user: AuthenticatedUser.Employee = admin.user) =
        pedagogicalDocumentController.getChildPedagogicalDocuments(
            dbInstance(),
            user,
            clock,
            childId,
        )

    private fun getDocumentsAsCitizen(
        childId: ChildId,
        user: AuthenticatedUser.Citizen = guardianUser,
    ) =
        pedagogicalDocumentControllerCitizen.getPedagogicalDocumentsForChild(
            dbInstance(),
            user,
            clock,
            childId,
        )

    private fun getUnreadCount(user: AuthenticatedUser.Citizen = guardianUser) =
        pedagogicalDocumentControllerCitizen.getUnreadPedagogicalDocumentCount(
            dbInstance(),
            user,
            clock,
        )

    private fun markDocumentRead(
        documentId: PedagogicalDocumentId,
        user: AuthenticatedUser.Citizen = guardianUser,
    ) =
        pedagogicalDocumentControllerCitizen.markPedagogicalDocumentRead(
            dbInstance(),
            user,
            clock,
            documentId,
        )

    private fun deleteDocument(
        documentId: PedagogicalDocumentId,
        user: AuthenticatedUser.Employee = admin.user,
    ) =
        pedagogicalDocumentController.deletePedagogicalDocument(
            dbInstance(),
            user,
            clock,
            documentId,
        )

    private fun uploadAttachment(
        documentId: PedagogicalDocumentId,
        user: AuthenticatedUser.Employee = admin.user,
    ): AttachmentId =
        attachmentsController.uploadPedagogicalDocumentAttachment(
            dbInstance(),
            user,
            clock,
            documentId,
            MockMultipartFile("file", "evaka-logo.png", "image/png", pngFile.readBytes()),
        )

    private fun getAttachment(
        attachmentId: AttachmentId,
        user: AuthenticatedUser.Employee,
        requestedFilename: String = "evaka-logo.png",
    ) =
        attachmentsController.getAttachment(
            dbInstance(),
            user,
            clock,
            attachmentId,
            requestedFilename,
        )

    private fun getAttachment(
        attachmentId: AttachmentId,
        user: AuthenticatedUser.Citizen,
        requestedFilename: String = "evaka-logo.png",
    ) =
        attachmentsController.getAttachment(
            dbInstance(),
            user,
            clock,
            attachmentId,
            requestedFilename,
        )
}
