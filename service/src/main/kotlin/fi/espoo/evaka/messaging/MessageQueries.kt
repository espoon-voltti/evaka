// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.attachment.MessageAttachment
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageContentId
import fi.espoo.evaka.shared.MessageDraftId
import fi.espoo.evaka.shared.MessageId
import fi.espoo.evaka.shared.MessageRecipientId
import fi.espoo.evaka.shared.MessageThreadFolderId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.formatName
import fi.espoo.evaka.shared.mapToPaged
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import java.time.LocalDate
import mu.KotlinLogging
import org.jdbi.v3.json.Json

val logger = KotlinLogging.logger {}

const val MESSAGE_UNDO_WINDOW_IN_SECONDS = 15L

fun Database.Read.getUnreadMessagesCounts(
    now: HelsinkiDateTime,
    accountIds: Set<MessageAccountId>
): Set<UnreadCountByAccount> {
    // language=SQL
    val sql =
        """
        SELECT 
            acc.id as account_id,
            SUM(CASE WHEN mtp.folder_id IS NULL AND mr.id IS NOT NULL AND mr.read_at IS NULL AND NOT mt.is_copy THEN 1 ELSE 0 END) as unread_count,
            SUM(CASE WHEN mtp.folder_id IS NULL AND mr.id IS NOT NULL AND mr.read_at IS NULL AND mt.is_copy THEN 1 ELSE 0 END) as unread_copy_count
        FROM message_account acc
        LEFT JOIN message_recipients mr ON mr.recipient_id = acc.id
        LEFT JOIN message m ON mr.message_id = m.id
        LEFT JOIN message_thread mt ON m.thread_id = mt.id
        LEFT JOIN message_thread_participant mtp ON m.thread_id = mtp.thread_id AND mtp.participant_id = acc.id
        WHERE acc.id = ANY(:accountIds) AND (m.id IS NULL OR m.sent_at < :undoThreshold)
        GROUP BY acc.id
    """
            .trimIndent()

    return this.createQuery(sql)
        .bind("accountIds", accountIds)
        .bind("undoThreshold", now.minusSeconds(MESSAGE_UNDO_WINDOW_IN_SECONDS))
        .mapTo<UnreadCountByAccount>()
        .toSet()
}

fun Database.Read.getUnreadMessagesCountsByDaycare(
    now: HelsinkiDateTime,
    daycareId: DaycareId
): Set<UnreadCountByAccountAndGroup> {
    // language=SQL
    val sql =
        """
        SELECT
            acc.id as account_id,
            acc.daycare_group_id as group_id,
            SUM(CASE WHEN mr.id IS NOT NULL AND mr.read_at IS NULL AND NOT mt.is_copy THEN 1 ELSE 0 END) as unread_count,
            SUM(CASE WHEN mr.id IS NOT NULL AND mr.read_at IS NULL AND mt.is_copy THEN 1 ELSE 0 END) as unread_copy_count
        FROM message_account acc
        LEFT JOIN message_recipients mr ON mr.recipient_id = acc.id
        LEFT JOIN message m ON mr.message_id = m.id
        LEFT JOIN message_thread mt ON m.thread_id = mt.id
        JOIN daycare_group dg ON acc.daycare_group_id = dg.id AND dg.daycare_id = :daycareId
        WHERE acc.active = true AND m.sent_at < :undoThreshold
        GROUP BY acc.id, acc.daycare_group_id
    """
            .trimIndent()

    return this.createQuery(sql)
        .bind("daycareId", daycareId)
        .bind("undoThreshold", now.minusSeconds(MESSAGE_UNDO_WINDOW_IN_SECONDS))
        .mapTo<UnreadCountByAccountAndGroup>()
        .toSet()
}

fun Database.Transaction.markThreadRead(
    clock: EvakaClock,
    accountId: MessageAccountId,
    threadId: MessageThreadId
): Int {
    // language=SQL
    val sql =
        """
UPDATE message_recipients rec
SET read_at = :now
FROM message msg
WHERE rec.message_id = msg.id
  AND msg.thread_id = :threadId
  AND rec.recipient_id = :accountId
  AND read_at IS NULL;
    """
            .trimIndent()

    return this.createUpdate(sql)
        .bind("now", clock.now())
        .bind("accountId", accountId)
        .bind("threadId", threadId)
        .execute()
}

fun Database.Transaction.archiveThread(
    accountId: MessageAccountId,
    threadId: MessageThreadId
): Int {
    var archiveFolderId = getArchiveFolderId(accountId)
    if (archiveFolderId == null) {
        archiveFolderId =
            this.createUpdate(
                    "INSERT INTO message_thread_folder (owner_id, name) VALUES (:accountId, 'ARCHIVE') ON CONFLICT DO NOTHING RETURNING id"
                )
                .bind("accountId", accountId)
                .executeAndReturnGeneratedKeys()
                .mapTo<MessageThreadFolderId>()
                .single()
    }

    return this.createUpdate(
            "UPDATE message_thread_participant SET folder_id = :archiveFolderId WHERE thread_id = :threadId AND participant_id = :accountId"
        )
        .bind("accountId", accountId)
        .bind("threadId", threadId)
        .bind("archiveFolderId", archiveFolderId)
        .execute()
}

fun Database.Transaction.insertMessage(
    now: HelsinkiDateTime,
    contentId: MessageContentId,
    threadId: MessageThreadId,
    sender: MessageAccountId,
    recipientNames: List<String>,
    municipalAccountName: String,
    repliesToMessageId: MessageId? = null
): MessageId {
    // language=SQL
    val insertMessageSql =
        """
        INSERT INTO message (content_id, thread_id, sender_id, sender_name, replies_to, sent_at, recipient_names)
        SELECT
            :contentId,
            :threadId,
            :senderId,
            CASE WHEN name_view.type = 'MUNICIPAL' THEN :municipalAccountName ELSE name_view.name END,
            :repliesToId,
            :now,
            :recipientNames
        FROM message_account_view name_view
        WHERE name_view.id = :senderId
        RETURNING id
    """
            .trimIndent()
    return createQuery(insertMessageSql)
        .bind("now", now)
        .bind("contentId", contentId)
        .bind("threadId", threadId)
        .bind("repliesToId", repliesToMessageId)
        .bind("senderId", sender)
        .bind("recipientNames", recipientNames)
        .bind("municipalAccountName", municipalAccountName)
        .mapTo<MessageId>()
        .one()
}

fun Database.Transaction.insertMessageContent(
    content: String,
    sender: MessageAccountId
): MessageContentId {
    // language=SQL
    val messageContentSql =
        "INSERT INTO message_content (content, author_id) VALUES (:content, :authorId) RETURNING id"
    return createQuery(messageContentSql)
        .bind("content", content)
        .bind("authorId", sender)
        .mapTo<MessageContentId>()
        .one()
}

fun Database.Transaction.insertRecipients(
    recipientAccountIds: Set<MessageAccountId>,
    messageId: MessageId
) {
    // language=SQL
    val insertRecipientsSql =
        "INSERT INTO message_recipients (message_id, recipient_id) VALUES (:messageId, :accountId)"

    val batch = this.prepareBatch(insertRecipientsSql)
    recipientAccountIds.forEach { batch.bind("messageId", messageId).bind("accountId", it).add() }
    batch.execute()
}

fun Database.Transaction.insertMessageThreadChildren(
    childIds: Set<ChildId>,
    threadId: MessageThreadId
) {
    // language=SQL
    val insertChildrenSql =
        "INSERT INTO message_thread_children (thread_id, child_id) VALUES (:threadId, :childId)"

    val batch = this.prepareBatch(insertChildrenSql)
    childIds.forEach { batch.bind("threadId", threadId).bind("childId", it).add() }
    batch.execute()
}

fun Database.Transaction.upsertSenderThreadParticipants(
    threadId: MessageThreadId,
    senderId: MessageAccountId,
    now: HelsinkiDateTime
) {
    createUpdate(
            """
        INSERT INTO message_thread_participant as tp (thread_id, participant_id, last_message_timestamp, last_sent_timestamp)
        VALUES (:threadId, :accountId, :now, :now)
        ON CONFLICT (thread_id, participant_id) DO UPDATE SET last_message_timestamp = :now, last_sent_timestamp = :now
    """
        )
        .bind("threadId", threadId)
        .bind("accountId", senderId)
        .bind("now", now)
        .execute()
}

fun Database.Transaction.upsertReceiverThreadParticipants(
    threadId: MessageThreadId,
    receiverIds: Set<MessageAccountId>,
    now: HelsinkiDateTime
) {
    val batch =
        prepareBatch(
            """
        INSERT INTO message_thread_participant as tp (thread_id, participant_id, last_message_timestamp, last_received_timestamp)
        SELECT id, :accountId, :now, :now FROM message_thread WHERE id = :threadId
        ON CONFLICT (thread_id, participant_id) DO UPDATE SET last_message_timestamp = :now, last_received_timestamp = :now
    """
        )
    receiverIds.forEach {
        batch.bind("threadId", threadId).bind("accountId", it).bind("now", now).add()
    }
    batch.execute()

    // If the receiver has archived the thread, move it back to inbox
    createUpdate(
            """
        UPDATE message_thread_participant mtp
        SET folder_id = NULL
        WHERE thread_id = :threadId
          AND participant_id = ANY(:receiverIds)
          AND folder_id = (SELECT id FROM message_thread_folder mtf WHERE mtf.owner_id = mtp.participant_id AND mtf.name = 'ARCHIVE')
    """
        )
        .bind("threadId", threadId)
        .bind("receiverIds", receiverIds)
        .execute()
}

fun Database.Transaction.insertThread(
    type: MessageType,
    title: String,
    urgent: Boolean,
    isCopy: Boolean
): MessageThreadId {
    // language=SQL
    val insertThreadSql =
        "INSERT INTO message_thread (message_type, title, urgent, is_copy) VALUES (:messageType, :title, :urgent, :isCopy) RETURNING id"
    return createQuery(insertThreadSql)
        .bind("messageType", type)
        .bind("title", title)
        .bind("urgent", urgent)
        .bind("isCopy", isCopy)
        .mapTo<MessageThreadId>()
        .one()
}

fun Database.Transaction.reAssociateMessageAttachments(
    attachmentIds: Set<AttachmentId>,
    messageContentId: MessageContentId
): Int {
    return createUpdate(
            """
UPDATE attachment
SET
    message_content_id = :messageContentId,
    message_draft_id = NULL
WHERE
    id = ANY(:attachmentIds)
        """
                .trimIndent()
        )
        .bind("attachmentIds", attachmentIds)
        .bind("messageContentId", messageContentId)
        .execute()
}

private data class ReceivedThread(
    val id: MessageThreadId,
    val title: String,
    val type: MessageType,
    val urgent: Boolean,
    val isCopy: Boolean,
    @Json val children: List<MessageChild>
)

/** Return all threads that are visible to the account through sent and received messages */
fun Database.Read.getThreads(
    now: HelsinkiDateTime,
    accountId: MessageAccountId,
    pageSize: Int,
    page: Int,
    municipalAccountName: String
): Paged<MessageThread> {
    val threads =
        createQuery(
                """
SELECT
    COUNT(*) OVER () AS count,
    t.id,
    t.title,
    t.message_type AS type,
    t.urgent,
    t.is_copy,
    coalesce((
        SELECT jsonb_agg(jsonb_build_object(
            'childId', mtc.child_id,
            'firstName', p.first_name,
            'lastName', p.last_name,
            'preferredName', p.preferred_name
        ))
        FROM message_thread_children mtc
        JOIN person p ON p.id = mtc.child_id
        WHERE mtc.thread_id = t.id
    ), '[]'::jsonb) AS children
FROM message_thread_participant tp
JOIN message_thread t on t.id = tp.thread_id
WHERE tp.participant_id = :accountId AND tp.folder_id IS NULL
AND EXISTS (SELECT 1 FROM message m WHERE m.thread_id = t.id AND (m.sender_id = :accountId OR m.sent_at < :undoThreshold))
ORDER BY tp.last_message_timestamp DESC
LIMIT :pageSize OFFSET :offset
        """
            )
            .bind("accountId", accountId)
            .bind("pageSize", pageSize)
            .bind("offset", (page - 1) * pageSize)
            .bind("undoThreshold", now.minusSeconds(MESSAGE_UNDO_WINDOW_IN_SECONDS))
            .mapToPaged<ReceivedThread>(pageSize)

    val messagesByThread =
        getThreadMessages(now, accountId, threads.data.map { it.id }, municipalAccountName)
    return combineThreadsAndMessages(accountId, threads, messagesByThread)
}

/** Return all threads in which the account has received messages */
fun Database.Read.getReceivedThreads(
    now: HelsinkiDateTime,
    accountId: MessageAccountId,
    pageSize: Int,
    page: Int,
    municipalAccountName: String,
    folderId: MessageThreadFolderId? = null
): Paged<MessageThread> {
    val threads =
        createQuery(
                """
SELECT
    COUNT(*) OVER () AS count,
    t.id,
    t.title,
    t.message_type AS type,
    t.urgent,
    t.is_copy,
    coalesce((
        SELECT jsonb_agg(jsonb_build_object(
            'childId', mtc.child_id,
            'firstName', p.first_name,
            'lastName', p.last_name,
            'preferredName', p.preferred_name
        ))
        FROM message_thread_children mtc
        JOIN person p ON p.id = mtc.child_id
        WHERE mtc.thread_id = t.id
    ), '[]'::jsonb) AS children
FROM message_thread_participant tp
JOIN message_thread t on t.id = tp.thread_id
WHERE
    tp.participant_id = :accountId AND
    tp.last_received_timestamp IS NOT NULL AND
    NOT t.is_copy AND
    ${
        if (folderId === null) {
            "tp.folder_id IS NULL"
        } else {
            "tp.folder_id = :folderId"
        }
    } AND
    EXISTS (SELECT 1 FROM message m WHERE m.thread_id = t.id AND m.sent_at < :undoThreshold)
ORDER BY tp.last_received_timestamp DESC
LIMIT :pageSize OFFSET :offset
        """
            )
            .bind("accountId", accountId)
            .bind("pageSize", pageSize)
            .bind("offset", (page - 1) * pageSize)
            .bind("folderId", folderId)
            .bind("undoThreshold", now.minusSeconds(MESSAGE_UNDO_WINDOW_IN_SECONDS))
            .mapToPaged<ReceivedThread>(pageSize)

    val messagesByThread =
        getThreadMessages(now, accountId, threads.data.map { it.id }, municipalAccountName)
    return combineThreadsAndMessages(accountId, threads, messagesByThread)
}

private fun Database.Read.getThreadMessages(
    now: HelsinkiDateTime,
    accountId: MessageAccountId,
    threadIds: List<MessageThreadId>,
    municipalAccountName: String
): Map<MessageThreadId, List<Message>> {
    if (threadIds.isEmpty()) return mapOf()
    return createQuery(
            """
SELECT
    m.id,
    m.thread_id,
    m.sent_at,
    mc.content,
    mr_self.read_at,
    (
        SELECT jsonb_build_object(
            'id', mav.id,
            'name', CASE WHEN mav.type = 'MUNICIPAL' THEN :municipalAccountName ELSE mav.name END,
            'type', mav.type
        )
        FROM message_account_view mav
        WHERE mav.id = m.sender_id
    ) AS sender,
    (
        SELECT jsonb_agg(
            jsonb_build_object(
                'id', mav.id,
                'name', CASE WHEN mav.type = 'MUNICIPAL' THEN :municipalAccountName ELSE mav.name END,
                'type', mav.type
            )
        )
        FROM message_recipients mr
        JOIN message_account_view mav ON mav.id = mr.recipient_id
        WHERE mr.message_id = m.id
    ) AS recipients,
    coalesce((
        SELECT jsonb_agg(jsonb_build_object('id', a.id, 'name', a.name, 'contentType', a.content_type))
        FROM attachment a
        WHERE a.message_content_id = mc.id
    ), '[]'::jsonb) AS attachments
FROM message m
JOIN message_content mc ON mc.id = m.content_id
LEFT JOIN message_recipients mr_self ON mr_self.message_id = m.id AND mr_self.recipient_id = :accountId
WHERE
    m.thread_id = ANY(:threadIds) AND
    (m.sender_id = :accountId OR EXISTS (
        SELECT 1
        FROM message_recipients mr
        WHERE mr.message_id = m.id AND mr.recipient_id = :accountId
    )) AND
    (m.sender_id = :accountId OR m.sent_at < :undoThreshold)
ORDER BY m.sent_at
            """
        )
        .bind("accountId", accountId)
        .bind("threadIds", threadIds)
        .bind("municipalAccountName", municipalAccountName)
        .bind("undoThreshold", now.minusSeconds(MESSAGE_UNDO_WINDOW_IN_SECONDS))
        .mapTo<Message>()
        .groupBy { it.threadId }
}

private fun combineThreadsAndMessages(
    accountId: MessageAccountId,
    threads: Paged<ReceivedThread>,
    messagesByThread: Map<MessageThreadId, List<Message>>
): Paged<MessageThread> {
    return threads.flatMap { thread ->
        val messages = messagesByThread[thread.id]
        if (messages == null) {
            logger.warn("Thread ${thread.id} has no messages for account $accountId")
            listOf()
        } else {
            listOf(
                MessageThread(
                    id = thread.id,
                    type = thread.type,
                    title = thread.title,
                    urgent = thread.urgent,
                    isCopy = thread.isCopy,
                    children = thread.children,
                    messages = messages
                )
            )
        }
    }
}

data class MessageCopy(
    val threadId: MessageThreadId,
    val messageId: MessageId,
    val title: String,
    val type: MessageType,
    val urgent: Boolean,
    val sentAt: HelsinkiDateTime,
    val content: String,
    val senderId: MessageAccountId,
    val senderName: String,
    val senderAccountType: AccountType,
    val readAt: HelsinkiDateTime? = null,
    val recipientId: MessageAccountId,
    val recipientName: String,
    val recipientAccountType: AccountType,
    val recipientNames: List<String>,
    @Json val attachments: List<MessageAttachment>
)

fun Database.Read.getMessageCopiesByAccount(
    now: HelsinkiDateTime,
    accountId: MessageAccountId,
    pageSize: Int,
    page: Int
): Paged<MessageCopy> {
    // language=SQL
    val sql =
        """
SELECT
    COUNT(*) OVER () AS count,
    t.id AS thread_id,
    m.id AS message_id,
    t.title,
    t.message_type AS type,
    t.urgent,
    m.sent_at,
    m.sender_name,
    m.sender_id,
    sender_acc.type AS sender_account_type,
    m.content_id,
    c.content,
    rec.read_at,
    rec.recipient_id,
    acc.name recipient_name,
    recipient_acc.type AS recipient_account_type,
    m.recipient_names,
    (
        SELECT coalesce(jsonb_agg(jsonb_build_object(
           'id', att.id,
           'name', att.name,
           'contentType', att.content_type
        )), '[]'::jsonb)
        FROM attachment att WHERE att.message_content_id = m.content_id
    ) AS attachments
FROM message_recipients rec
JOIN message m ON rec.message_id = m.id
JOIN message_content c ON m.content_id = c.id
JOIN message_account_view acc ON rec.recipient_id = acc.id
JOIN message_account sender_acc ON sender_acc.id = m.sender_id
JOIN message_account recipient_acc ON recipient_acc.id = rec.recipient_id
JOIN message_thread t ON m.thread_id = t.id
WHERE rec.recipient_id = :accountId AND t.is_copy AND m.sent_at < :undoThreshold
ORDER BY m.sent_at DESC
LIMIT :pageSize OFFSET :offset
"""

    return createQuery(sql)
        .bind("accountId", accountId)
        .bind("offset", (page - 1) * pageSize)
        .bind("pageSize", pageSize)
        .bind("undoThreshold", now.minusSeconds(MESSAGE_UNDO_WINDOW_IN_SECONDS))
        .mapToPaged(pageSize)
}

fun Database.Read.getSentMessage(senderId: MessageAccountId, messageId: MessageId): Message {
    val sql =
        """
SELECT
    m.id,
    m.thread_id,
    m.sent_at,
    mc.content,
    (
        SELECT jsonb_build_object('id', mav.id, 'name', mav.name, 'type', mav.type)
        FROM message_account_view mav
        WHERE mav.id = m.sender_id
    ) AS sender,
    (
        SELECT jsonb_agg(jsonb_build_object('id', mav.id, 'name', mav.name, 'type', mav.type))
        FROM message_recipients mr
        JOIN message_account_view mav ON mav.id = mr.recipient_id
        WHERE mr.message_id = m.id
    ) AS recipients,
    coalesce((
        SELECT jsonb_agg(jsonb_build_object('id', a.id, 'name', a.name, 'contentType', a.content_type))
        FROM attachment a
        WHERE a.message_content_id = mc.id
    ), '[]'::jsonb) AS attachments
FROM message m
JOIN message_content mc ON mc.id = m.content_id
WHERE m.id = :messageId AND m.sender_id = :senderId
"""
    return this.createQuery(sql)
        .bind("messageId", messageId)
        .bind("senderId", senderId)
        .mapTo<Message>()
        .first()
}

fun Database.Read.getCitizenReceivers(
    today: LocalDate,
    accountId: MessageAccountId
): Map<MessageAccount, List<ChildId>> {
    data class MessageAccountWithChildId(
        val id: MessageAccountId,
        val name: String,
        val type: AccountType,
        val childId: ChildId
    )
    // language=SQL
    val sql =
        """
WITH user_account AS (
    SELECT * FROM message_account WHERE id = :accountId
), children AS (
    SELECT g.child_id, g.guardian_id AS parent_id
    FROM user_account acc
    JOIN guardian g ON acc.person_id = g.guardian_id
    WHERE NOT EXISTS (SELECT 1 FROM messaging_blocklist b WHERE b.child_id = g.child_id AND b.blocked_recipient = g.guardian_id)

    UNION ALL

    SELECT fp.child_id, fp.parent_id
    FROM user_account acc
    JOIN foster_parent fp ON acc.person_id = fp.parent_id AND valid_during @> :today
    WHERE NOT EXISTS (SELECT 1 FROM messaging_blocklist b WHERE b.child_id = fp.child_id AND b.blocked_recipient = fp.parent_id)
), backup_care_placements AS (
    SELECT p.id, p.unit_id, p.child_id, p.group_id
    FROM children c
    JOIN backup_care p ON p.child_id = c.child_id AND daterange(p.start_date, p.end_date, '[]') @> :today
    WHERE EXISTS (
        SELECT 1 FROM daycare u
        WHERE p.unit_id = u.id AND 'MESSAGING' = ANY(u.enabled_pilot_features)
    )
), placements AS (
    SELECT p.id, p.unit_id, p.child_id
    FROM children c
    JOIN placement p ON p.child_id = c.child_id AND daterange(p.start_date, p.end_date, '[]') @> :today
    WHERE NOT EXISTS (
        SELECT 1 FROM backup_care_placements bc
        WHERE bc.child_id = p.child_id
    )
    AND EXISTS (
        SELECT 1 FROM daycare u
        WHERE p.unit_id = u.id AND 'MESSAGING' = ANY(u.enabled_pilot_features)
    )
),
relevant_placements AS (
    SELECT p.id, p.unit_id, p.child_id
    FROM placements p

    UNION

    SELECT bc.id, bc.unit_id, bc.child_id
    FROM backup_care_placements bc
),
personal_accounts AS (
    SELECT acc.id, acc_name.name, 'PERSONAL' AS type, p.child_id
    FROM (SELECT DISTINCT unit_id, child_id FROM relevant_placements) p
    JOIN daycare_acl acl ON acl.daycare_id = p.unit_id AND acl.role = 'UNIT_SUPERVISOR'
    JOIN message_account acc ON acc.employee_id = acl.employee_id
    JOIN message_account_view acc_name ON acc_name.id = acc.id
    WHERE active IS TRUE
),
group_accounts AS (
    SELECT acc.id, g.name, 'GROUP' AS type, p.child_id
    FROM placements p
    JOIN daycare_group_placement dgp ON dgp.daycare_placement_id = p.id AND :today BETWEEN dgp.start_date AND dgp.end_date
    JOIN daycare_group g ON g.id = dgp.daycare_group_id
    JOIN message_account acc on g.id = acc.daycare_group_id

    UNION ALL

    SELECT acc.id, g.name, 'GROUP' AS type, p.child_id
    FROM backup_care_placements p
    JOIN daycare_group g ON g.id = p.group_id
    JOIN message_account acc on g.id = acc.daycare_group_id
),
mixed_accounts AS (
    SELECT id, name, type, child_id FROM personal_accounts
    UNION ALL
    SELECT id, name, type, child_id FROM group_accounts
)
SELECT id, name, type, child_id FROM mixed_accounts
ORDER BY type, name  -- groups first
    """
            .trimIndent()

    return this.createQuery(sql)
        .bind("accountId", accountId)
        .bind("today", today)
        .mapTo<MessageAccountWithChildId>()
        .groupBy({ MessageAccount(it.id, it.name, it.type) }, { it.childId })
}

fun Database.Read.getMessagesSentByAccount(
    accountId: MessageAccountId,
    pageSize: Int,
    page: Int
): Paged<SentMessage> {
    // language=SQL
    val sql =
        """
WITH pageable_messages AS (
    SELECT
        m.content_id,
        m.sent_at,
        m.recipient_names,
        t.title,
        t.message_type,
        t.urgent,
        COUNT(*) OVER () AS count
    FROM message m
    JOIN message_thread t ON m.thread_id = t.id
    WHERE sender_id = :accountId
    GROUP BY content_id, sent_at, recipient_names, title, message_type, urgent
    ORDER BY sent_at DESC
    LIMIT :pageSize OFFSET :offset
),
recipients AS (
    SELECT
        m.content_id,
        rec.recipient_id,
        name_view.name,
        acc.type AS account_type
    FROM message_recipients rec
    JOIN message m ON rec.message_id = m.id
    JOIN message_account_view name_view ON rec.recipient_id = name_view.id
    JOIN message_account acc ON acc.id = rec.recipient_id
)

SELECT
    msg.count,
    msg.content_id,
    msg.sent_at,
    msg.recipient_names,
    msg.title AS thread_title,
    msg.message_type AS type,
    msg.urgent,
    mc.content,
    (SELECT jsonb_agg(jsonb_build_object(
           'id', rec.recipient_id,
           'name', rec.name,
           'type', rec.account_type
       ))) AS recipients,
    (SELECT coalesce(jsonb_agg(jsonb_build_object(
           'id', att.id,
           'name', att.name,
           'contentType', att.content_type
        )), '[]'::jsonb)
        FROM attachment att WHERE att.message_content_id = msg.content_id
        ) AS attachments
FROM pageable_messages msg
JOIN recipients rec ON msg.content_id = rec.content_id
JOIN message_content mc ON msg.content_id = mc.id
GROUP BY msg.count, msg.content_id, msg.sent_at, msg.recipient_names, mc.content, msg.message_type, msg.urgent, msg.title
ORDER BY msg.sent_at DESC
    """
            .trimIndent()

    return this.createQuery(sql)
        .bind("accountId", accountId)
        .bind("offset", (page - 1) * pageSize)
        .bind("pageSize", pageSize)
        .mapToPaged(pageSize)
}

data class ThreadWithParticipants(
    val threadId: MessageThreadId,
    val type: MessageType,
    val isCopy: Boolean,
    val senders: Set<MessageAccountId>,
    val recipients: Set<MessageAccountId>
)

fun Database.Read.getThreadByMessageId(messageId: MessageId): ThreadWithParticipants? {
    val sql =
        """
        SELECT
            t.id AS threadId,
            t.message_type AS type,
            t.is_copy,
            (SELECT array_agg(m2.sender_id)) as senders,
            (SELECT array_agg(rec.recipient_id)) as recipients
            FROM message m
            JOIN message_thread t ON m.thread_id = t.id
            JOIN message m2 ON m2.thread_id = t.id
            JOIN message_recipients rec ON rec.message_id = m2.id
            WHERE m.id = :messageId
            GROUP BY t.id, t.message_type
    """
            .trimIndent()
    return this.createQuery(sql)
        .bind("messageId", messageId)
        .mapTo<ThreadWithParticipants>()
        .firstOrNull()
}

data class UnitMessageReceiversResult(
    val accountId: MessageAccountId,
    val unitId: DaycareId?,
    val unitName: String?,
    val groupId: GroupId,
    val groupName: String,
    val childId: ChildId,
    val firstName: String,
    val lastName: String
)

data class MunicipalMessageReceiversResult(
    val accountId: MessageAccountId,
    val areaId: AreaId,
    val areaName: String,
    val unitId: DaycareId,
    val unitName: String
)

fun Database.Read.getReceiversForNewMessage(
    idFilter: AccessControlFilter<MessageAccountId>,
    today: LocalDate
): List<MessageReceiversResponse> {
    val accountIds = getEmployeeMessageAccountIds(idFilter)

    val unitReceivers =
        createQuery(
                """
        WITH accounts AS (
            SELECT id, type, daycare_group_id, employee_id, person_id FROM message_account
            WHERE id = ANY(:accountIds) AND type = ANY('{PERSONAL,GROUP}'::message_account_type[])
        ), children AS (
            SELECT a.id AS account_id, p.child_id, NULL AS unit_id, NULL AS unit_name, p.group_id, g.name AS group_name
            FROM accounts a
            JOIN realized_placement_all(:date) p ON a.daycare_group_id = p.group_id
            JOIN daycare d ON p.unit_id = d.id
            JOIN daycare_group g ON p.group_id = g.id
            WHERE 'MESSAGING' = ANY(d.enabled_pilot_features)

            UNION ALL

            SELECT a.id AS account_id, p.child_id, p.unit_id, d.name AS unit_name, p.group_id, g.name AS group_name
            FROM accounts a
            JOIN daycare_acl_view acl ON a.employee_id = acl.employee_id
            JOIN daycare d ON acl.daycare_id = d.id
            JOIN daycare_group g ON d.id = g.daycare_id
            JOIN realized_placement_all(:date) p ON g.id = p.group_id
            WHERE 'MESSAGING' = ANY(d.enabled_pilot_features)
        )
        SELECT DISTINCT
            c.account_id,
            c.unit_id,
            c.unit_name,
            c.group_id,
            c.group_name,
            c.child_id,
            p.first_name,
            p.last_name
        FROM children c
        JOIN person p ON c.child_id = p.id
        WHERE EXISTS (
            SELECT 1
            FROM guardian g
            LEFT JOIN messaging_blocklist bl ON g.guardian_id = bl.blocked_recipient AND c.child_id = bl.child_id
            WHERE g.child_id = c.child_id AND bl.id IS NULL

            UNION ALL

            SELECT 1
            FROM foster_parent fp
            LEFT JOIN messaging_blocklist bl ON fp.parent_id = bl.blocked_recipient AND fp.child_id = bl.child_id
            WHERE fp.child_id = c.child_id AND fp.valid_during @> :date AND bl.id IS NULL
        )
        ORDER BY c.unit_name, c.group_name
        """
                    .trimIndent()
            )
            .bind("accountIds", accountIds)
            .bind("date", today)
            .mapTo<UnitMessageReceiversResult>()
            .toList()
            .groupBy { it.accountId }
            .map { (accountId, receivers) ->
                val units = receivers.groupBy { it.unitId to it.unitName }
                val accountReceivers =
                    units.flatMap { (unit, groups) ->
                        val (unitId, unitName) = unit
                        if (unitId == null || unitName == null) getReceiverGroups(groups)
                        else
                            listOf(
                                MessageReceiver.Unit(
                                    id = unitId,
                                    name = unitName,
                                    receivers = getReceiverGroups(groups)
                                )
                            )
                    }
                MessageReceiversResponse(accountId = accountId, receivers = accountReceivers)
            }

    val municipalReceivers =
        createQuery(
                """
        WITH accounts AS (
            SELECT id, type, daycare_group_id, employee_id, person_id FROM message_account
            WHERE id = ANY(:accountIds) AND type = 'MUNICIPAL'::message_account_type
        )
        SELECT acc.id AS account_id, a.id AS area_id, a.name AS area_name, d.id AS unit_id, d.name AS unit_name
        FROM accounts acc, care_area a
        JOIN daycare d ON a.id = d.care_area_id
        WHERE 'MESSAGING' = ANY(d.enabled_pilot_features)
        ORDER BY area_name
        """
                    .trimIndent()
            )
            .bind("accountIds", accountIds)
            .bind("date", today)
            .mapTo<MunicipalMessageReceiversResult>()
            .toList()
            .groupBy { it.accountId }
            .map { (accountId, receivers) ->
                val areas = receivers.groupBy { it.areaId to it.areaName }
                val accountReceivers =
                    areas.map { (area, units) ->
                        val (areaId, areaName) = area
                        MessageReceiver.Area(
                            id = areaId,
                            name = areaName,
                            receivers =
                                units.map { unit ->
                                    MessageReceiver.UnitInArea(
                                        id = unit.unitId,
                                        name = unit.unitName
                                    )
                                }
                        )
                    }
                MessageReceiversResponse(accountId = accountId, receivers = accountReceivers)
            }

    return municipalReceivers + unitReceivers
}

private fun getReceiverGroups(
    receivers: List<UnitMessageReceiversResult>
): List<MessageReceiver.Group> =
    receivers
        .groupBy { it.groupId to it.groupName }
        .map { (group, children) ->
            val (groupId, groupName) = group
            MessageReceiver.Group(
                id = groupId,
                name = groupName,
                receivers =
                    children.map {
                        MessageReceiver.Child(
                            id = it.childId,
                            name = formatName(it.firstName, it.lastName, true)
                        )
                    }
            )
        }

fun Database.Read.getMessageAccountsForRecipients(
    accountId: MessageAccountId,
    recipients: Set<MessageRecipient>,
    date: LocalDate
): List<Pair<MessageAccountId, ChildId>> {
    val groupedRecipients = recipients.groupBy { it.type }
    return this.createQuery(
            """
WITH sender AS (
    SELECT type, daycare_group_id, employee_id FROM message_account WHERE id = :senderId
), children AS (
    SELECT DISTINCT pl.child_id
    FROM realized_placement_all(:date) pl
    JOIN daycare d ON pl.unit_id = d.id
    WHERE (d.care_area_id = ANY(:areaRecipients) OR pl.unit_id = ANY(:unitRecipients) OR pl.group_id = ANY(:groupRecipients) OR pl.child_id = ANY(:childRecipients))
    AND EXISTS (
        SELECT 1
        FROM child_daycare_acl(:date)
        JOIN mobile_device_daycare_acl_view USING (daycare_id)
        WHERE mobile_device_id = (SELECT sender.employee_id FROM sender)
        AND child_id = pl.child_id

        UNION ALL

        SELECT 1
        FROM employee_child_daycare_acl(:date)
        WHERE employee_id = (SELECT sender.employee_id FROM sender)
        AND child_id = pl.child_id

        UNION ALL

        SELECT 1
        FROM sender
        WHERE pl.group_id = sender.daycare_group_id

        UNION ALL

        SELECT 1
        FROM sender
        WHERE type = 'MUNICIPAL'
    )
    AND 'MESSAGING' = ANY(d.enabled_pilot_features)
)
SELECT acc.id AS account_id, c.child_id
FROM children c
JOIN guardian g ON g.child_id = c.child_id
JOIN message_account acc ON g.guardian_id = acc.person_id
WHERE NOT EXISTS (
    SELECT 1 FROM messaging_blocklist bl
    WHERE bl.child_id = c.child_id
    AND bl.blocked_recipient = g.guardian_id
)

UNION

SELECT acc.id AS account_id, c.child_id
FROM children c
JOIN foster_parent fp ON fp.child_id = c.child_id AND fp.valid_during @> :date
JOIN message_account acc ON fp.parent_id = acc.person_id
WHERE NOT EXISTS (
    SELECT 1 FROM messaging_blocklist bl
    WHERE bl.child_id = c.child_id
    AND bl.blocked_recipient = fp.parent_id
)
"""
        )
        .bind("senderId", accountId)
        .bind("date", date)
        .bind(
            "areaRecipients",
            groupedRecipients[MessageRecipientType.AREA]?.map { it.id } ?: listOf()
        )
        .bind(
            "unitRecipients",
            groupedRecipients[MessageRecipientType.UNIT]?.map { it.id } ?: listOf()
        )
        .bind(
            "groupRecipients",
            groupedRecipients[MessageRecipientType.GROUP]?.map { it.id } ?: listOf()
        )
        .bind(
            "childRecipients",
            groupedRecipients[MessageRecipientType.CHILD]?.map { it.id } ?: listOf()
        )
        .map { rv ->
            rv.mapColumn<MessageAccountId>("account_id") to rv.mapColumn<ChildId>("child_id")
        }
        .toList()
}

fun Database.Transaction.markNotificationAsSent(
    id: MessageRecipientId,
    timestamp: HelsinkiDateTime
) {
    val sql =
        """
        UPDATE message_recipients
        SET notification_sent_at = :timestamp
        WHERE id = :id
    """
            .trimIndent()
    this.createUpdate(sql).bind("id", id).bind("timestamp", timestamp).execute()
}

fun Database.Read.getStaffCopyRecipients(
    senderId: MessageAccountId,
    areaIds: List<AreaId>,
    unitIds: List<DaycareId>,
    groupIds: List<GroupId>,
    date: LocalDate
): Set<MessageAccountId> {
    return this.createQuery(
            """
SELECT receiver_acc.id
FROM message_account sender_acc
JOIN daycare_acl_view acl ON sender_acc.employee_id = acl.employee_id OR sender_acc.type = 'MUNICIPAL'
JOIN daycare u ON u.id = acl.daycare_id
JOIN daycare_group g ON u.id = g.daycare_id
JOIN message_account receiver_acc ON g.id = receiver_acc.daycare_group_id
WHERE sender_acc.id = :senderId AND (u.care_area_id = ANY(:areaIds) OR u.id = ANY(:unitIds) OR g.id = ANY(:groupIds))
"""
        )
        .bind("senderId", senderId)
        .bind("areaIds", areaIds)
        .bind("unitIds", unitIds)
        .bind("groupIds", groupIds)
        .bind("date", date)
        .mapTo<MessageAccountId>()
        .toSet()
}

fun Database.Read.getArchiveFolderId(accountId: MessageAccountId): MessageThreadFolderId? =
    this.createQuery(
            "SELECT id FROM message_thread_folder WHERE owner_id = :accountId AND name = 'ARCHIVE'"
        )
        .bind("accountId", accountId)
        .mapTo<MessageThreadFolderId>()
        .firstOrNull()

data class MessageToUndo(
    val sentAt: HelsinkiDateTime,
    val messageId: MessageId,
    val contentId: MessageContentId,
    val threadId: MessageThreadId,
    val senderId: MessageAccountId,
    val onlyMessageInThread: Boolean
)

fun Database.Transaction.undoMessageReply(
    now: HelsinkiDateTime,
    accountId: MessageAccountId,
    messageId: MessageId
) {
    val messageToUndo =
        this.createQuery(
                """
SELECT sent_at, id AS message_id, content_id, thread_id, sender_id, FALSE AS only_message_in_thread
FROM message WHERE sender_id = :accountId AND id = :messageId
"""
            )
            .bind("accountId", accountId)
            .bind("messageId", messageId)
            .mapTo<MessageToUndo>()
            .findOne()
            .orElseThrow { throw BadRequest("No message found with messageId $messageId") }

    if (messageToUndo.sentAt.plusSeconds(MESSAGE_UNDO_WINDOW_IN_SECONDS).isBefore(now)) {
        throw BadRequest(
            "Messages older than $MESSAGE_UNDO_WINDOW_IN_SECONDS seconds cannot be undone"
        )
    }

    this.deleteMessages(listOf(messageToUndo))
    this.resetSenderThreadParticipants(messageToUndo.threadId, messageToUndo.senderId)
}

fun Database.Transaction.undoMessageAndAllThreads(
    now: HelsinkiDateTime,
    accountId: MessageAccountId,
    contentId: MessageContentId
): MessageDraftId {
    val messagesToUndo =
        this.createQuery(
                """
SELECT m.sent_at, m.id AS message_id, m.content_id, m.thread_id, m.sender_id,
    NOT EXISTS (SELECT 1 FROM message m2 WHERE m.thread_id = m2.thread_id AND m2.content_id != :contentId) AS only_message_in_thread
FROM message m
WHERE sender_id = :accountId AND content_id = :contentId
"""
            )
            .bind("accountId", accountId)
            .bind("contentId", contentId)
            .mapTo<MessageToUndo>()
            .toList()

    if (messagesToUndo.isEmpty()) {
        throw BadRequest("No messages found with contentId $contentId")
    }

    if (
        messagesToUndo.any { it.sentAt.plusSeconds(MESSAGE_UNDO_WINDOW_IN_SECONDS).isBefore(now) }
    ) {
        throw BadRequest(
            "Messages older than $MESSAGE_UNDO_WINDOW_IN_SECONDS seconds cannot be undone"
        )
    }

    if (messagesToUndo.any { !it.onlyMessageInThread }) {
        throw BadRequest("Cannot undo message threads that have replies")
    }

    val draftContent =
        this.createQuery(
                """
SELECT DISTINCT t.title, t.message_type AS type, t.urgent, c.content, '{}'::text[] AS recipient_ids, '{}'::text[] AS recipient_names
FROM message_content c
JOIN message m ON c.id = m.content_id
JOIN message_thread t ON m.thread_id = t.id
WHERE c.id = :contentId
"""
            )
            .bind("contentId", contentId)
            .mapTo<UpdatableDraftContent>()
            .findOne()
            .orElseThrow { error("Multiple draft contents found") }

    this.deleteMessages(messagesToUndo)

    val draftId = this.initDraft(accountId)
    this.updateDraft(accountId, draftId, draftContent)
    return draftId
}

fun Database.Transaction.deleteMessages(messages: List<MessageToUndo>) {
    this.createUpdate(
            """
WITH deleted_message AS (
    DELETE FROM message WHERE id = ANY(:messageIds)
), deleted_recipients AS (
    DELETE FROM message_recipients WHERE message_id = ANY(:messageIds)
), deleted_thread AS (
    DELETE FROM message_thread WHERE id = ANY(:threadIds)
), deleted_thread_children AS (
    DELETE FROM message_thread_children WHERE thread_id = ANY(:threadIds)
)
DELETE FROM message_content WHERE id = ANY(:contentIds)
"""
        )
        .bind("messageIds", messages.map { it.messageId }.distinct())
        .bind(
            "threadIds",
            messages.filter { it.onlyMessageInThread }.map { it.threadId }.distinct()
        )
        .bind("contentIds", messages.map { it.contentId }.distinct())
        .execute()
}

fun Database.Transaction.resetSenderThreadParticipants(
    threadId: MessageThreadId,
    senderId: MessageAccountId
) {
    this.createUpdate(
            """
UPDATE message_thread_participant SET
    last_message_timestamp = (
        SELECT MAX(sent_at) FROM message m JOIN message_recipients r ON m.id = r.message_id
        WHERE m.thread_id = :threadId AND (m.sender_id = :senderId OR r.recipient_id = :senderId)
    ),
    last_sent_timestamp = (SELECT MAX(sent_at) FROM message m WHERE m.thread_id = :threadId AND m.sender_id = :senderId)
WHERE thread_id = :threadId AND participant_id = :senderId
"""
        )
        .bind("threadId", threadId)
        .bind("senderId", senderId)
        .execute()
}

fun Database.Read.messageForRecipientExists(
    messageId: MessageId,
    recipientId: MessageAccountId
): Boolean {
    return createQuery<Boolean> {
            sql(
                """
SELECT EXISTS (
    SELECT * FROM message_recipients WHERE message_id = ${bind(messageId)} AND recipient_id = ${bind(recipientId)}
)
"""
            )
        }
        .mapTo<Boolean>()
        .first()
}
