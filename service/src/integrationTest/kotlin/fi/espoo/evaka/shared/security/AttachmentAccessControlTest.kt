// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.attachment.AttachmentParent
import fi.espoo.evaka.attachment.insertAttachment
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.security.actionrule.HasGlobalRole
import fi.espoo.evaka.shared.security.actionrule.IsCitizen
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class AttachmentAccessControlTest : AccessControlTest() {
    private val clock = MockEvakaClock(HelsinkiDateTime.of(LocalDateTime.of(2022, 1, 1, 12, 0)))

    @Test
    fun `HasGlobalRole andAttachmentWasUploadedByAnyEmployee`() {
        val permittedEmployee =
            createTestEmployee(setOf(UserRole.SERVICE_WORKER, UserRole.FINANCE_ADMIN))
        val deniedEmployee = createTestEmployee(setOf(UserRole.DIRECTOR))
        val permittedRoles = arrayOf(UserRole.REPORT_VIEWER, UserRole.FINANCE_ADMIN)

        val action = Action.Attachment.READ_APPLICATION_ATTACHMENT
        rules.add(action, HasGlobalRole(*permittedRoles).andAttachmentWasUploadedByAnyEmployee())

        val uploaderEmployee = createTestEmployee(emptySet())
        val employeeAttachmentId = insertApplicationAttachment(uploaderEmployee)
        assertTrue(
            accessControl.hasPermissionFor(permittedEmployee, clock, action, employeeAttachmentId)
        )
        assertFalse(
            accessControl.hasPermissionFor(deniedEmployee, clock, action, employeeAttachmentId)
        )

        val uploaderCitizen = createTestCitizen(CitizenAuthLevel.STRONG)
        val citizenAttachmentId = insertApplicationAttachment(uploaderCitizen)
        assertFalse(
            accessControl.hasPermissionFor(permittedEmployee, clock, action, citizenAttachmentId)
        )
    }

    @Test
    fun `IsCitizen uploaderOfAttachment`() {
        val action = Action.Attachment.READ_APPLICATION_ATTACHMENT
        rules.add(action, IsCitizen(allowWeakLogin = false).uploaderOfAttachment())
        val uploaderCitizen = createTestCitizen(CitizenAuthLevel.STRONG)
        val otherCitizen = createTestCitizen(CitizenAuthLevel.STRONG)

        val attachmentId = insertApplicationAttachment(uploaderCitizen)
        assertTrue(accessControl.hasPermissionFor(uploaderCitizen, clock, action, attachmentId))
        assertFalse(accessControl.hasPermissionFor(otherCitizen, clock, action, attachmentId))
        assertFalse(
            accessControl.hasPermissionFor(
                uploaderCitizen.copy(authLevel = CitizenAuthLevel.WEAK),
                clock,
                action,
                attachmentId
            )
        )
    }

    private fun insertApplicationAttachment(user: AuthenticatedUser) =
        db.transaction { tx ->
            val attachmentId = AttachmentId(UUID.randomUUID())
            val guardianId = tx.insertTestPerson(DevPerson())
            val childId = tx.insertTestPerson(DevPerson())
            val applicationId =
                tx.insertTestApplication(
                    guardianId = guardianId,
                    childId = childId,
                    type = ApplicationType.DAYCARE
                )
            tx.insertAttachment(
                user,
                attachmentId,
                "test.pdf",
                "application/pdf",
                AttachmentParent.Application(applicationId),
                type = null
            )
            attachmentId
        }
}
