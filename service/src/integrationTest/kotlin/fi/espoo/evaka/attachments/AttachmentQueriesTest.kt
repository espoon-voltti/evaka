// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attachments

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.attachment.AttachmentParent
import fi.espoo.evaka.attachment.AttachmentType
import fi.espoo.evaka.attachment.associateOrphanAttachments
import fi.espoo.evaka.attachment.dissociateAttachmentsOfParent
import fi.espoo.evaka.attachment.getAttachment
import fi.espoo.evaka.attachment.insertAttachment
import fi.espoo.evaka.attachment.userAttachmentCount
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevGuardian
import fi.espoo.evaka.shared.dev.DevIncome
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.MockEvakaClock
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AttachmentQueriesTest : FullApplicationTest(resetDbBeforeEach = true) {
    private lateinit var guardian: AuthenticatedUser.Citizen
    private lateinit var admin: AuthenticatedUser.Employee

    private lateinit var application: AttachmentParent.Application
    private lateinit var income: AttachmentParent.Income

    private val clock = MockEvakaClock(2023, 1, 1, 12, 0)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            guardian =
                AuthenticatedUser.Citizen(
                    tx.insert(DevPerson(), DevPersonType.ADULT),
                    CitizenAuthLevel.STRONG
                )
            admin =
                AuthenticatedUser.Employee(tx.insert(DevEmployee()), roles = setOf(UserRole.ADMIN))
            val child = tx.insert(DevPerson(), DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = guardian.id, childId = child))
            application =
                AttachmentParent.Application(
                    tx.insertTestApplication(
                        type = ApplicationType.DAYCARE,
                        childId = child,
                        guardianId = guardian.id
                    )
                )
            income =
                AttachmentParent.Income(
                    tx.insert(DevIncome(personId = guardian.id, updatedBy = admin.evakaUserId))
                )
        }
    }

    @Test
    fun `userAttachmentCount counts attachments correctly`() {
        db.transaction { tx ->
            tx.insertAttachment(guardian, application)
            tx.insertAttachment(guardian, application)
            tx.insertAttachment(guardian, income)
            tx.insertAttachment(admin, income)
        }

        db.read { tx ->
            assertEquals(2, tx.userAttachmentCount(guardian.evakaUserId, application))
            assertEquals(1, tx.userAttachmentCount(guardian.evakaUserId, income))
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
            validTarget = tx.insertAttachment(guardian, AttachmentParent.None)
            notOrphan = tx.insertAttachment(guardian, income)
            wrongUser = tx.insertAttachment(admin, AttachmentParent.None)
        }

        fun associateGuardianAttachmentsToApplication(vararg ids: AttachmentId) =
            db.transaction {
                it.associateOrphanAttachments(guardian.evakaUserId, application, ids.toList())
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
            validTarget = tx.insertAttachment(guardian, application)
            differentParent = tx.insertAttachment(guardian, income)
            differentUser = tx.insertAttachment(admin, application)
        }

        db.transaction { it.dissociateAttachmentsOfParent(guardian.evakaUserId, application) }
        assertEquals(AttachmentParent.None, parentOf(validTarget))

        assertEquals(application, parentOf(differentUser))
        assertEquals(income, parentOf(differentParent))
    }

    private fun Database.Transaction.insertAttachment(
        user: AuthenticatedUser,
        parent: AttachmentParent
    ) =
        insertAttachment(
            user,
            clock.now(),
            "dummy-name",
            "text/plain",
            parent,
            type = if (parent is AttachmentParent.Application) AttachmentType.URGENCY else null
        )

    private fun parentOf(attachment: AttachmentId): AttachmentParent? =
        db.read { it.getAttachment(attachment) }?.attachedTo
}
