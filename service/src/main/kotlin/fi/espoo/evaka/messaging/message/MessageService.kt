// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.message

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.NotFound
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class MessageService {
    fun replyToThread(
        db: Database.Connection,
        messageId: UUID,
        senderAccount: MessageAccount,
        recipientAccountIds: Set<UUID>,
        content: String,
    ): UUID {
        val (threadId, type, sender, recipients) = db.read { it.getThreadByMessageId(messageId) }
            ?: throw NotFound("Message not found")

        if (type == MessageType.BULLETIN && sender != senderAccount.id) throw Forbidden("Only the author can reply to bulletin")

        val previousParticipants = recipients + sender
        if (!previousParticipants.contains(senderAccount.id)) throw Forbidden("Not authorized to post to message")
        if (!previousParticipants.containsAll(recipientAccountIds)) throw Forbidden("Not authorized to widen the audience")

        return db.transaction {
            replyToThread(
                tx = it,
                threadId = threadId,
                repliesToMessageId = messageId,
                content = content,
                sender = senderAccount,
                recipients = recipientAccountIds
            )
        }
    }
}
