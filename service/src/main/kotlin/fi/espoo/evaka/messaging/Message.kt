// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import com.fasterxml.jackson.annotation.JsonTypeInfo
import fi.espoo.evaka.attachment.MessageAttachment
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageContentId
import fi.espoo.evaka.shared.MessageId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.mapper.PropagateNull
import org.jdbi.v3.json.Json

data class Message(
    val id: MessageId,
    val threadId: MessageThreadId,
    @Json val sender: MessageAccount,
    @Json val recipients: Set<MessageAccount>,
    val sentAt: HelsinkiDateTime,
    val content: String,
    val readAt: HelsinkiDateTime? = null,
    @Json val attachments: List<MessageAttachment>,
    val recipientNames: Set<String>? = null
)

data class MessageThread(
    val id: MessageThreadId,
    val type: MessageType,
    val title: String,
    val urgent: Boolean,
    val isCopy: Boolean,
    val children: List<MessageChild>,
    @Json val messages: List<Message>
)

data class SentMessage(
    val contentId: MessageContentId,
    val content: String,
    val sentAt: HelsinkiDateTime,
    val threadTitle: String,
    val type: MessageType,
    val urgent: Boolean,
    @Json val recipients: Set<MessageAccount>,
    val recipientNames: List<String>,
    @Json val attachments: List<MessageAttachment>
)

enum class MessageType {
    MESSAGE,
    BULLETIN
}

data class MessageReceiversResponse(
    val accountId: MessageAccountId,
    val receivers: List<MessageReceiver>
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed class MessageReceiver(val type: MessageRecipientType) {
    abstract val id: Id<*>
    abstract val name: String

    data class Area(
        override val id: AreaId,
        override val name: String,
        val receivers: List<UnitInArea>
    ) : MessageReceiver(MessageRecipientType.AREA)

    data class UnitInArea(override val id: DaycareId, override val name: String) :
        MessageReceiver(MessageRecipientType.UNIT)

    data class Unit(
        override val id: DaycareId,
        override val name: String,
        val receivers: List<Group>
    ) : MessageReceiver(MessageRecipientType.UNIT)

    data class Group(
        override val id: GroupId,
        override val name: String,
        val receivers: List<Child>,
    ) : MessageReceiver(MessageRecipientType.GROUP)

    data class Child(override val id: ChildId, override val name: String) :
        MessageReceiver(MessageRecipientType.CHILD)

    data class Citizen(override val id: PersonId, override val name: String) :
        MessageReceiver(MessageRecipientType.CITIZEN)
}

enum class AccountType {
    PERSONAL,
    GROUP,
    CITIZEN,
    MUNICIPAL,
    SERVICE_WORKER;

    fun isPrimaryRecipientForCitizenMessage(): Boolean =
        when (this) {
            PERSONAL -> true
            GROUP -> true
            CITIZEN -> false
            MUNICIPAL -> false
            SERVICE_WORKER -> false
        }
}

data class MessageAccount(val id: MessageAccountId, val name: String, val type: AccountType)

data class Group(
    @PropagateNull val id: GroupId,
    val name: String,
    val unitId: DaycareId,
    val unitName: String
)

data class AuthorizedMessageAccount(
    @Nested("account_") val account: MessageAccount,
    @Nested("group_") val daycareGroup: Group?
)

enum class MessageRecipientType {
    AREA,
    UNIT,
    GROUP,
    CHILD,
    CITIZEN
}

data class MessageRecipient(val type: MessageRecipientType, val id: Id<*>)

data class MessageChild(
    val childId: ChildId,
    val firstName: String,
    val lastName: String,
    val preferredName: String
)
