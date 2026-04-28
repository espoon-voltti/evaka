// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.attachments

import evaka.core.PureJdbiTest
import evaka.core.application.ApplicationAttachmentType
import evaka.core.application.ApplicationType
import evaka.core.application.persistence.daycare.Adult
import evaka.core.application.persistence.daycare.Apply
import evaka.core.application.persistence.daycare.Child
import evaka.core.application.persistence.daycare.DaycareFormV0
import evaka.core.attachment.AttachmentParent
import evaka.core.attachment.associateOrphanAttachments
import evaka.core.attachment.dissociateAttachmentsOfParent
import evaka.core.attachment.getAttachment
import evaka.core.attachment.insertAttachment
import evaka.core.attachment.userAttachmentCount
import evaka.core.shared.ApplicationId
import evaka.core.shared.AttachmentId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.CitizenAuthLevel
import evaka.core.shared.auth.UserRole
import evaka.core.shared.db.Database
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevGuardian
import evaka.core.shared.dev.DevIncome
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertTestApplication
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.MockEvakaClock
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AttachmentQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val guardian = DevPerson()
    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val child = DevPerson()
    private val applicationId = ApplicationId(UUID.randomUUID())
    private val application = AttachmentParent.Application(applicationId)
    private val devIncome = DevIncome(personId = guardian.id, modifiedBy = admin.evakaUserId)
    private val income = AttachmentParent.Income(devIncome.id)

    private val clock = MockEvakaClock(2023, 1, 1, 12, 0)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(guardian, DevPersonType.ADULT)
            tx.insert(admin)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = guardian.id, childId = child.id))
            tx.insertTestApplication(
                id = applicationId,
                type = ApplicationType.DAYCARE,
                childId = child.id,
                guardianId = guardian.id,
                document =
                    DaycareFormV0(
                        type = ApplicationType.DAYCARE,
                        child = Child(dateOfBirth = null),
                        guardian = Adult(),
                        apply = Apply(preferredUnits = listOf(daycare.id)),
                    ),
            )
            tx.insert(devIncome)
        }
    }

    @Test
    fun `userAttachmentCount counts attachments correctly`() {
        db.transaction { tx ->
            tx.insertAttachment(guardian.user(CitizenAuthLevel.STRONG), application)
            tx.insertAttachment(guardian.user(CitizenAuthLevel.STRONG), application)
            tx.insertAttachment(guardian.user(CitizenAuthLevel.STRONG), income)
            tx.insertAttachment(admin.user, income)
        }

        db.read { tx ->
            assertEquals(2, tx.userAttachmentCount(guardian.evakaUserId(), application))
            assertEquals(1, tx.userAttachmentCount(guardian.evakaUserId(), income))
            assertEquals(0, tx.userAttachmentCount(admin.evakaUserId, application))
            assertEquals(1, tx.userAttachmentCount(admin.evakaUserId, income))
        }
    }

    @Test
    fun `associateOrphanAttachments associates only orphan attachments uploaded by the given user`() {
        lateinit var validTarget: AttachmentId
        lateinit var notOrphan: AttachmentId
        lateinit var wrongUser: AttachmentId
        db.transaction { tx ->
            validTarget =
                tx.insertAttachment(guardian.user(CitizenAuthLevel.STRONG), AttachmentParent.None)
            notOrphan = tx.insertAttachment(guardian.user(CitizenAuthLevel.STRONG), income)
            wrongUser = tx.insertAttachment(admin.user, AttachmentParent.None)
        }

        fun associateGuardianAttachmentsToApplication(vararg ids: AttachmentId) = db.transaction {
            it.associateOrphanAttachments(guardian.evakaUserId(), application, ids.toList())
        }

        assertThrows<BadRequest> {
            associateGuardianAttachmentsToApplication(validTarget, notOrphan)
        }
        assertThrows<BadRequest> {
            associateGuardianAttachmentsToApplication(validTarget, wrongUser)
        }

        associateGuardianAttachmentsToApplication(validTarget)
        assertEquals(application, parentOf(validTarget))
    }

    @Test
    fun `dissociateOrphanAttachments dissociates only attachments uploaded by the given user associated with the given parent`() {
        lateinit var validTarget: AttachmentId
        lateinit var differentParent: AttachmentId
        lateinit var differentUser: AttachmentId
        db.transaction { tx ->
            validTarget = tx.insertAttachment(guardian.user(CitizenAuthLevel.STRONG), application)
            differentParent = tx.insertAttachment(guardian.user(CitizenAuthLevel.STRONG), income)
            differentUser = tx.insertAttachment(admin.user, application)
        }

        db.transaction { it.dissociateAttachmentsOfParent(guardian.evakaUserId(), application) }
        assertEquals(AttachmentParent.None, parentOf(validTarget))

        assertEquals(application, parentOf(differentUser))
        assertEquals(income, parentOf(differentParent))
    }

    private fun Database.Transaction.insertAttachment(
        user: AuthenticatedUser,
        parent: AttachmentParent,
    ) =
        insertAttachment(
            user,
            clock.now(),
            "dummy-name",
            "text/plain",
            parent,
            type =
                if (parent is AttachmentParent.Application) ApplicationAttachmentType.URGENCY
                else null,
        )

    private fun parentOf(attachment: AttachmentId): AttachmentParent? =
        db.read { it.getAttachment(attachment) }?.second
}
