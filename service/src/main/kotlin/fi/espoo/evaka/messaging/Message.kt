// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.attachment.Attachment
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
import fi.espoo.evaka.shared.config.SealedSubclassSimpleName
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
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
    @Json val attachments: List<Attachment>,
    val recipientNames: Set<String>? = null,
)

data class MessageThread(
    val id: MessageThreadId,
    val type: MessageType,
    val title: String,
    val urgent: Boolean,
    val sensitive: Boolean,
    val isCopy: Boolean,
    val applicationStatus: ApplicationStatus?,
    val children: List<MessageChild>,
    @Json val messages: List<Message>,
)

data class MessageThreadStub(
    val id: MessageThreadId,
    val type: MessageType,
    val title: String,
    val urgent: Boolean,
    val sensitive: Boolean,
    val isCopy: Boolean,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@JsonTypeIdResolver(SealedSubclassSimpleName::class)
sealed interface CitizenMessageThread {
    val id: MessageThreadId
    val urgent: Boolean

    data class Redacted(
        override val id: MessageThreadId,
        override val urgent: Boolean,
        val sender: MessageAccount?,
        val lastMessageSentAt: HelsinkiDateTime?,
        val hasUnreadMessages: Boolean,
    ) : CitizenMessageThread {
        companion object {
            fun fromMessageThread(accountId: MessageAccountId, messageThread: MessageThread) =
                Redacted(
                    messageThread.id,
                    messageThread.urgent,
                    messageThread.messages.firstOrNull()?.sender,
                    messageThread.messages.lastOrNull()?.sentAt,
                    messageThread.messages
                        .findLast { message -> message.sender.id != accountId }
                        ?.readAt == null,
                )
        }
    }

    data class Regular(
        override val id: MessageThreadId,
        override val urgent: Boolean,
        val children: List<MessageChild>,
        val messageType: MessageType,
        val title: String,
        val sensitive: Boolean,
        val isCopy: Boolean,
        val applicationStatus: ApplicationStatus?,
        val messages: List<Message>,
    ) : CitizenMessageThread {
        companion object {
            fun fromMessageThread(messageThread: MessageThread) =
                Regular(
                    messageThread.id,
                    messageThread.urgent,
                    messageThread.children,
                    messageThread.type,
                    messageThread.title,
                    messageThread.sensitive,
                    messageThread.isCopy,
                    messageThread.applicationStatus,
                    messageThread.messages,
                )
        }
    }
}

data class SentMessage(
    val contentId: MessageContentId,
    val content: String,
    val sentAt: HelsinkiDateTime,
    val threadTitle: String,
    val type: MessageType,
    val urgent: Boolean,
    val sensitive: Boolean,
    val recipientNames: List<String>,
    @Json val attachments: List<Attachment>,
)

enum class MessageType : DatabaseEnum {
    MESSAGE,
    BULLETIN;

    override val sqlType: String = "message_type"
}

data class MessageReceiversResponse(
    val accountId: MessageAccountId,
    val receivers: List<MessageReceiver>,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed class MessageReceiver(val type: MessageRecipientType) {
    abstract val name: String

    @JsonTypeName("AREA")
    data class Area(val id: AreaId, override val name: String, val receivers: List<UnitInArea>) :
        MessageReceiver(MessageRecipientType.AREA)

    @JsonTypeName("UNIT_IN_AREA")
    data class UnitInArea(val id: DaycareId, override val name: String) :
        MessageReceiver(MessageRecipientType.UNIT_IN_AREA)

    @JsonTypeName("UNIT")
    data class Unit(
        val id: DaycareId,
        override val name: String,
        val receivers: List<Group>,
        val hasStarters: Boolean,
    ) : MessageReceiver(MessageRecipientType.UNIT)

    @JsonTypeName("GROUP")
    data class Group(
        val id: GroupId,
        override val name: String,
        val receivers: List<Child>,
        val hasStarters: Boolean,
    ) : MessageReceiver(MessageRecipientType.GROUP)

    @JsonTypeName("CHILD")
    data class Child(val id: ChildId, override val name: String, val startDate: LocalDate?) :
        MessageReceiver(MessageRecipientType.CHILD)

    @JsonTypeName("CITIZEN")
    data class Citizen(val id: PersonId, override val name: String) :
        MessageReceiver(MessageRecipientType.CITIZEN)
}

enum class AccountType : DatabaseEnum {
    PERSONAL,
    GROUP,
    CITIZEN,
    MUNICIPAL,
    SERVICE_WORKER,
    FINANCE;

    fun isPrimaryRecipientForCitizenMessage(): Boolean =
        when (this) {
            PERSONAL -> true
            GROUP -> true
            CITIZEN -> false
            MUNICIPAL -> false
            SERVICE_WORKER -> false
            FINANCE -> false
        }

    override val sqlType: String = "message_account_type"
}

data class MessageAccount(val id: MessageAccountId, val name: String, val type: AccountType)

data class MessageAccountWithPresence(
    val account: MessageAccount,
    val outOfOffice: FiniteDateRange?,
)

data class Group(
    @PropagateNull val id: GroupId,
    val name: String,
    val unitId: DaycareId,
    val unitName: String,
)

data class AuthorizedMessageAccount(
    @Nested("account_") val account: MessageAccount,
    @Nested("group_") val daycareGroup: Group?,
)

enum class MessageRecipientType {
    AREA,
    UNIT,
    UNIT_IN_AREA,
    GROUP,
    CHILD,
    CITIZEN,
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed class MessageRecipient(val type: MessageRecipientType) {
    abstract fun isStarter(): Boolean

    @JsonTypeName("AREA")
    data class Area(val id: AreaId) : MessageRecipient(MessageRecipientType.AREA) {
        override fun isStarter(): Boolean = false
    }

    @JsonTypeName("UNIT")
    data class Unit(val id: DaycareId, val starter: Boolean = false) :
        MessageRecipient(MessageRecipientType.AREA) {
        override fun isStarter(): Boolean = starter
    }

    @JsonTypeName("GROUP")
    data class Group(val id: GroupId, val starter: Boolean = false) :
        MessageRecipient(MessageRecipientType.GROUP) {
        override fun isStarter(): Boolean = starter
    }

    @JsonTypeName("CHILD")
    data class Child(val id: ChildId, val starter: Boolean = false) :
        MessageRecipient(MessageRecipientType.CHILD) {
        override fun isStarter(): Boolean = starter
    }

    @JsonTypeName("CITIZEN")
    data class Citizen(val id: PersonId) : MessageRecipient(MessageRecipientType.CITIZEN) {
        override fun isStarter(): Boolean = false
    }
}

data class MessageChild(
    val childId: ChildId,
    val firstName: String,
    val lastName: String,
    val preferredName: String,
)

data class NewMessageStub(
    val title: String,
    val content: String,
    val urgent: Boolean,
    val sensitive: Boolean,
)
