// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.bulletin

import java.time.Instant
import java.util.UUID

data class Bulletin(
    val id: UUID,
    val title: String,
    val content: String,
    val createdByEmployee: UUID,
    val unitId: UUID?,
    val createdByEmployeeName: String,
    val groupId: UUID?,
    val groupName: String?,
    val sentAt: Instant?
)

data class ReceivedBulletin(
    val id: UUID,
    val sentAt: Instant,
    val sender: String,
    val title: String,
    val content: String,
    val isRead: Boolean
)
