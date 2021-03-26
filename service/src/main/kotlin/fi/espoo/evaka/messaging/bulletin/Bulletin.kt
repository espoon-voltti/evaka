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
    val createdByEmployeeName: String,
    val receiverUnits: List<ReceiverUnit>,
    val receiverGroups: List<ReceiverGroup>,
    val receiverChildren: List<ReceiverChild>,
    val sentAt: Instant?
)

data class ReceiverUnit(
    val unitId: UUID,
    val unitName: String
)

data class ReceiverGroup(
    val unitId: UUID,
    val groupId: UUID,
    val groupName: String
)

data class ReceiverPerson(
    val personId: UUID,
    val firstName: String,
    val lastName: String
)

data class ReceiverChild(
    val childId: UUID,
    val firstName: String,
    val lastName: String,
    val receiverPersons: List<ReceiverPerson>
)

data class ReceivedBulletin(
    val id: UUID,
    val sentAt: Instant,
    val sender: String,
    val title: String,
    val content: String,
    val isRead: Boolean
)
