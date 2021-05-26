// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.message

import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.util.UUID

data class DraftContent(
    val id: UUID,
    val created: HelsinkiDateTime,
    val type: MessageType,
    val title: String,
    val content: String,
    val recipientIds: Set<UUID>,
    val recipientNames: List<String>,
)

data class UpsertableDraftContent(
    val type: MessageType,
    val title: String,
    val content: String,
    val recipientIds: Set<UUID>,
    val recipientNames: List<String>,
)
