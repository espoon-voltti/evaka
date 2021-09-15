// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attachment

import fi.espoo.evaka.shared.AttachmentId

data class Attachment(
    val id: AttachmentId,
    val name: String,
    val contentType: String,
)

enum class AttachmentType {
    URGENCY,
    EXTENDED_CARE
}
