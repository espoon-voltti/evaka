// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.message

import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.util.UUID

data class DraftContent(
    val id: UUID,
    val created: HelsinkiDateTime,
    val type: MessageType? = null,
    val title: String? = null,
    val content: String? = null,
    val recipientAccountIds: Set<UUID>? = null,
    val recipientNames: List<String>? = null,
)

data class UpsertableDraftContent(
    val type: MessageType? = null,
    val title: String? = null,
    val content: String? = null,
    val recipientAccountIds: Set<UUID>? = null,
    val recipientNames: List<String>? = null,
)
