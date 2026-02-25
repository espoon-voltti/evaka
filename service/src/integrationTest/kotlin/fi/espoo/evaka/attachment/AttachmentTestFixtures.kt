// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attachment

import fi.espoo.evaka.application.ApplicationAttachmentType
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.MockEvakaClock
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
