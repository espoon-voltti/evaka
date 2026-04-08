// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.attachment

import evaka.core.application.ApplicationAttachmentType
import evaka.core.shared.ApplicationId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.MockEvakaClock
import org.springframework.mock.web.MockMultipartFile

fun AttachmentsController.uploadApplicationAttachment(
    db: Database,
    applicationId: ApplicationId,
    user: AuthenticatedUser,
    type: ApplicationAttachmentType = ApplicationAttachmentType.URGENCY,
    clock: EvakaClock = MockEvakaClock(2024, 1, 1, 12, 0),
) {
    val pngBytes =
        this::class.java.getResource("/attachments-fixtures/evaka-logo.png")!!.readBytes()
    val file = MockMultipartFile("file", "evaka-logo.png", "image/png", pngBytes)
    when (user) {
        is AuthenticatedUser.Citizen ->
            uploadApplicationAttachmentCitizen(db, user, clock, applicationId, type, file)
        is AuthenticatedUser.Employee ->
            uploadApplicationAttachmentEmployee(db, user, clock, applicationId, type, file)
        else -> error("Unsupported user type")
    }
}
