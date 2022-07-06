// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.attachment.MessageAttachment
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageContentId
import fi.espoo.evaka.shared.MessageId
import fi.espoo.evaka.shared.MessageRecipientId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.WithCount
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.mapToPaged
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.json.Json
import java.time.LocalDate

fun Database.Read.getUnreadMessagesCounts(accountIds: Set<MessageAccountId>): Set<UnreadCountByAccount> {
    // language=SQL
    val sql = """
        SELECT 
        acc.id as account_id,
        SUM(CASE WHEN m.id IS NOT NULL AND m.read_at IS NULL THEN 1 ELSE 0 END) as unread_count
        FROM message_account acc
        LEFT JOIN message_recipients m
        ON m.recipient_id = acc.id
        WHERE acc.id = ANY(:accountIds)
        GROUP BY acc.id
    """.trimIndent()

    return this.createQuery(sql)
        .bind("accountIds", accountIds.toTypedArray())
        .mapTo<UnreadCountByAccount>().toSet()
}

fun Database.Read.getUnreadMessagesCountsByDaycare(daycareId: DaycareId): Set<UnreadCountByAccountAndGroup> {
    // language=SQL
    val sql = """
        SELECT
            acc.id as account_id,
            acc.daycare_group_id as group_id,
            SUM(CASE WHEN m.id IS NOT NULL AND m.read_at IS NULL THEN 1 ELSE 0 END) as unread_count
        FROM message_account acc
        LEFT JOIN message_recipients m ON m.recipient_id = acc.id
        JOIN daycare_group dg ON acc.daycare_group_id = dg.id AND dg.daycare_id = :daycareId
        WHERE acc.active = true
        GROUP BY acc.id, acc.daycare_group_id
    """.trimIndent()

    return this.createQuery(sql)
        .bind("daycareId", daycareId)
        .mapTo<UnreadCountByAccountAndGroup>().toSet()
}

fun Database.Transaction.markThreadRead(accountId: MessageAccountId, threadId: MessageThreadId): Int {
    // language=SQL
    val sql = """
UPDATE message_recipients rec
SET read_at = now()
FROM message msg
WHERE rec.message_id = msg.id
  AND msg.thread_id = :threadId
  AND rec.recipient_id = :accountId
  AND read_at IS NULL;
    """.trimIndent()

    return this.createUpdate(sql)
        .bind("accountId", accountId)
        .bind("threadId", threadId)
        .execute()
}

fun Database.Transaction.insertMessage(
    contentId: MessageContentId,
    threadId: MessageThreadId,
    sender: MessageAccountId,
    recipientNames: List<String>,
    repliesToMessageId: MessageId? = null,
): MessageId {
    // language=SQL
    val insertMessageSql = """
        INSERT INTO message (content_id, thread_id, sender_id, sender_name, replies_to, sent_at, recipient_names)
        SELECT :contentId, :threadId, :senderId, name_view.account_name, :repliesToId, now(), :recipientNames
        FROM message_account_name_view name_view
        WHERE name_view.id = :senderId
        RETURNING id
    """.trimIndent()
    return createQuery(insertMessageSql)
        .bind("contentId", contentId)
        .bind("threadId", threadId)
        .bindNullable("repliesToId", repliesToMessageId)
        .bind("senderId", sender)
        .bind("recipientNames", recipientNames.toTypedArray())
        .mapTo<MessageId>()
        .one()
}

fun Database.Transaction.insertMessageContent(
    content: String,
    sender: MessageAccountId
): MessageContentId {
    // language=SQL
    val messageContentSql = "INSERT INTO message_content (content, author_id) VALUES (:content, :authorId) RETURNING id"
    return createQuery(messageContentSql)
        .bind("content", content)
        .bind("authorId", sender)
        .mapTo<MessageContentId>()
        .one()
}

fun Database.Transaction.insertRecipients(
    recipientAccountIds: Set<MessageAccountId>,
    messageId: MessageId,
) {
    // language=SQL
    val insertRecipientsSql =
        "INSERT INTO message_recipients (message_id, recipient_id) VALUES (:messageId, :accountId)"

    val batch = this.prepareBatch(insertRecipientsSql)
    recipientAccountIds.forEach { batch.bind("messageId", messageId).bind("accountId", it).add() }
    batch.execute()
}

fun Database.Transaction.insertThread(
    type: MessageType,
    title: String,
    urgent: Boolean
): MessageThreadId {
    // language=SQL
    val insertThreadSql = "INSERT INTO message_thread (message_type, title, urgent) VALUES (:messageType, :title, :urgent) RETURNING id"
    return createQuery(insertThreadSql)
        .bind("messageType", type)
        .bind("title", title)
        .bind("urgent", urgent)
        .mapTo<MessageThreadId>()
        .one()
}

fun Database.Transaction.reAssociateMessageAttachments(attachmentIds: Set<AttachmentId>, messageContentId: MessageContentId): Int {
    return createUpdate(
        """
UPDATE attachment
SET
    message_content_id = :messageContentId,
    message_draft_id = NULL
WHERE
    id = ANY(:attachmentIds)
        """.trimIndent()
    )
        .bind("attachmentIds", attachmentIds.toTypedArray())
        .bind("messageContentId", messageContentId)
        .execute()
}

data class ReceivedMessageResultItem(
    val count: Int,
    val id: MessageThreadId,
    val title: String,
    val type: MessageType,
    val urgent: Boolean,
    val messageId: MessageId,
    val sentAt: HelsinkiDateTime,
    val content: String,
    val senderId: MessageAccountId,
    val senderName: String,
    val senderAccountType: AccountType,
    val readAt: HelsinkiDateTime? = null,
    val recipientId: MessageAccountId,
    val recipientName: String,
    val recipientAccountType: AccountType,
    @Json
    val attachments: List<MessageAttachment>
)

fun Database.Read.getMessagesReceivedByAccount(accountId: MessageAccountId, pageSize: Int, page: Int, isCitizen: Boolean = false): Paged<MessageThread> {
    val params = mapOf(
        "accountId" to accountId,
        "offset" to (page - 1) * pageSize,
        "pageSize" to pageSize
    )

    // language=SQL
    val sql = """
WITH
participated_messages AS (
    SELECT 
        rec.message_id,
        m.thread_id,
        m.sent_at, 
        m.sender_name,
        m.sender_id,
        sender_acc.type AS sender_account_type,
        m.content_id,
        c.content,
        rec.read_at,
        rec.recipient_id,
        acc.account_name recipient_name,
        recipient_acc.type AS recipient_account_type
    FROM message_recipients rec
    JOIN message m ON rec.message_id = m.id
    JOIN message_content c ON m.content_id = c.id
    JOIN message_account_name_view acc ON rec.recipient_id = acc.id
    JOIN message_account sender_acc ON sender_acc.id = m.sender_id
    JOIN message_account recipient_acc ON recipient_acc.id = rec.recipient_id
    WHERE m.sender_id = :accountId OR EXISTS (
        SELECT 1
            FROM message_recipients rec2
            WHERE rec2.message_id = m.id AND rec2.recipient_id = :accountId
    )
),
threads AS (
    SELECT id, message_type AS type, title, urgent, last_message, COUNT(*) OVER () AS count
    FROM message_thread t
    JOIN LATERAL (
        SELECT MAX(sent_at) last_message FROM message WHERE thread_id = t.id
    ) last_msg ON true
    WHERE EXISTS(
            SELECT 1
            FROM participated_messages rec
            WHERE rec.thread_id = t.id
            AND (rec.recipient_id = :accountId OR ${if (isCitizen) "rec.sender_id = :accountId" else "false"}))
    GROUP BY id, message_type, title, last_message
    ORDER BY last_message DESC
    LIMIT :pageSize OFFSET :offset
)

SELECT
    t.count,
    t.id,
    t.title,
    t.type,
    t.urgent,
    msg.message_id,
    msg.sent_at,
    msg.content,
    msg.sender_name,
    msg.sender_id,
    msg.sender_account_type,
    msg.read_at,
    msg.recipient_id,
    msg.recipient_name,
    msg.recipient_account_type,
    (
        SELECT coalesce(jsonb_agg(json_build_object(
           'id', att.id,
           'name', att.name,
           'contentType', att.content_type
        )), '[]'::jsonb) 
        FROM attachment att WHERE att.message_content_id = msg.content_id
    ) AS attachments
    FROM threads t
    JOIN participated_messages msg ON msg.thread_id = t.id
    ORDER BY t.last_message DESC, msg.sent_at ASC
    """.trimIndent()

    return createQuery(sql)
        .bindMap(params)
        .mapTo<ReceivedMessageResultItem>()
        .groupBy { it.id }
        .map { (threadId, threads) ->
            WithCount(
                threads[0].count,
                MessageThread(
                    id = threadId,
                    type = threads[0].type,
                    title = threads[0].title,
                    urgent = threads[0].urgent,
                    messages = threads
                        .groupBy { it.messageId }
                        .map { (messageId, messages) ->
                            Message(
                                id = messageId,
                                content = messages[0].content,
                                sender = MessageAccount(
                                    id = messages[0].senderId,
                                    name = messages[0].senderName,
                                    type = messages[0].senderAccountType
                                ),
                                sentAt = messages[0].sentAt,
                                readAt = messages.find { it.recipientId == accountId }?.readAt,
                                recipients = messages
                                    .groupBy { it.recipientId }
                                    .map { (recipientId, recipients) ->
                                        MessageAccount(recipientId, recipients[0].recipientName, recipients[0].recipientAccountType)
                                    }.toSet(),
                                attachments = messages[0].attachments
                            )
                        }
                )
            )
        }
        .mapToPaged(pageSize)
}

data class MessageResultItem(
    val id: MessageId,
    val senderId: MessageAccountId,
    val senderName: String,
    val senderAccountType: AccountType,
    val recipientId: MessageAccountId,
    val recipientName: String,
    val recipientAccountType: AccountType,
    val sentAt: HelsinkiDateTime,
    val content: String,
    @Json
    val attachments: List<MessageAttachment>
)

fun Database.Read.getMessage(id: MessageId): Message {
    val sql = """
        SELECT 
            m.id,
            m.sender_id,
            m.sender_name,
            sender_acc.type as sender_account_type,
            m.sent_at,
            c.content,
            rec.recipient_id,
            recipient_acc_name.account_name recipient_name,
            recipient_acc.type AS recipient_account_type,
            (
                SELECT coalesce(jsonb_agg(json_build_object(
                   'id', att.id,
                   'name', att.name,
                   'contentType', att.content_type
                )), '[]'::jsonb) 
                FROM attachment att WHERE att.message_content_id = m.content_id
            ) AS attachments
        FROM message m
        JOIN message_content c ON m.content_id = c.id
        JOIN message_recipients rec ON m.id = rec.message_id
        JOIN message_account recipient_acc ON recipient_acc.id = rec.recipient_id
        JOIN message_account sender_acc ON m.sender_id = sender_acc.id
        JOIN message_account_name_view recipient_acc_name ON rec.recipient_id = recipient_acc_name.id
        WHERE m.id = :id
    """.trimIndent()

    return this.createQuery(sql)
        .bind("id", id)
        .mapTo<MessageResultItem>()
        .groupBy { it.id }
        .map { (id, messages) ->
            Message(
                id = id,
                content = messages[0].content,
                sentAt = messages[0].sentAt,
                sender = MessageAccount(
                    id = messages[0].senderId,
                    name = messages[0].senderName,
                    type = messages[0].senderAccountType
                ),
                recipients = messages.map { MessageAccount(it.recipientId, it.recipientName, it.recipientAccountType) }.toSet(),
                attachments = messages[0].attachments
            )
        }
        .single()
}

fun Database.Read.getCitizenReceivers(today: LocalDate, accountId: MessageAccountId): List<MessageAccount> {
    // language=SQL
    val sql = """
WITH backup_care_placements AS (
    SELECT p.id, p.unit_id, p.child_id, p.group_id
    FROM guardian g
    JOIN backup_care p ON p.child_id = g.child_id AND daterange(p.start_date, p.end_date, '[]') @> :today
    WHERE guardian_id = (SELECT person_id AS id FROM message_account WHERE id = :accountId)
    AND NOT EXISTS (
        SELECT 1 FROM messaging_blocklist b
        WHERE b.child_id = p.child_id
        AND b.blocked_recipient = g.guardian_id
    )
    AND EXISTS (
        SELECT 1 FROM daycare u
        WHERE p.unit_id = u.id AND 'MESSAGING' = ANY(u.enabled_pilot_features)
    )

    UNION

    SELECT p.id, p.unit_id, p.child_id, p.group_id
    FROM fridge_child fg
    JOIN backup_care p ON fg.child_id = p.child_id AND daterange(p.start_date, p.end_date, '[]') @> :today
    WHERE daterange(fg.start_date, fg.end_date, '[]') @> :today
    AND fg.head_of_child = (SELECT person_id AS id FROM message_account WHERE id = :accountId)
    AND fg.conflict = false
    AND NOT EXISTS (
        SELECT 1 FROM messaging_blocklist b
        WHERE b.child_id = p.child_id
        AND b.blocked_recipient = fg.head_of_child
    )
    AND EXISTS (
        SELECT 1 FROM daycare u
        WHERE p.unit_id = u.id AND 'MESSAGING' = ANY(u.enabled_pilot_features)
    )
), placements AS (
    SELECT p.id, p.unit_id, p.child_id
    FROM guardian g
    JOIN placement p ON p.child_id = g.child_id AND daterange(p.start_date, p.end_date, '[]') @> :today
    WHERE guardian_id = (SELECT person_id AS id FROM message_account WHERE id = :accountId)
    AND NOT EXISTS (
        SELECT 1 FROM messaging_blocklist b
        WHERE b.child_id = p.child_id
        AND b.blocked_recipient = g.guardian_id
    )
    AND NOT EXISTS (
        SELECT 1 FROM backup_care_placements bc
        WHERE bc.child_id = p.child_id
    )
    AND EXISTS (
        SELECT 1 FROM daycare u
        WHERE p.unit_id = u.id AND 'MESSAGING' = ANY(u.enabled_pilot_features)
    )

    UNION

    SELECT p.id, p.unit_id, p.child_id
    FROM fridge_child fg
    JOIN placement p ON fg.child_id = p.child_id AND daterange(p.start_date, p.end_date, '[]') @> :today
    WHERE daterange(fg.start_date, fg.end_date, '[]') @> :today
    AND fg.head_of_child = (SELECT person_id AS id FROM message_account WHERE id = :accountId)
    AND fg.conflict = false
    AND NOT EXISTS (
        SELECT 1 FROM messaging_blocklist b
        WHERE b.child_id = p.child_id
        AND b.blocked_recipient = fg.head_of_child
    )
    AND NOT EXISTS (
        SELECT 1 FROM backup_care_placements bc
        WHERE bc.child_id = p.child_id
    )
    AND EXISTS (
        SELECT 1 FROM daycare u
        WHERE p.unit_id = u.id AND 'MESSAGING' = ANY(u.enabled_pilot_features)
    )    
),
relevant_placements AS (
    SELECT p.id, p.unit_id
    FROM placements p
    
    UNION 
    
    SELECT bc.id, bc.unit_id
    FROM backup_care_placements bc
),
personal_accounts AS (
    SELECT acc.id, acc_name.account_name AS name, 'PERSONAL' AS type
    FROM (SELECT DISTINCT unit_id FROM relevant_placements) p
    JOIN daycare_acl acl ON acl.daycare_id = p.unit_id
    JOIN message_account acc ON acc.employee_id = acl.employee_id
    JOIN message_account_name_view acc_name ON acc_name.id = acc.id
    WHERE active IS TRUE
),
group_accounts AS (
    SELECT acc.id, g.name, 'GROUP' AS type
    FROM placements p
    JOIN daycare_group_placement dgp ON dgp.daycare_placement_id = p.id AND :today BETWEEN dgp.start_date AND dgp.end_date
    JOIN daycare_group g ON g.id = dgp.daycare_group_id
    JOIN message_account acc on g.id = acc.daycare_group_id
    
    UNION ALL

         SELECT acc.id, g.name, 'GROUP' AS type
         FROM backup_care_placements p
                  JOIN daycare_group g ON g.id = p.group_id
                  JOIN message_account acc on g.id = acc.daycare_group_id
),
mixed_accounts AS (
    SELECT id, name, type FROM personal_accounts
    UNION ALL
    SELECT id, name, type FROM group_accounts
)
SELECT id, name, type FROM mixed_accounts
ORDER BY type, name  -- groups first
    """.trimIndent()

    return this.createQuery(sql)
        .bind("accountId", accountId)
        .bind("today", today)
        .mapTo<MessageAccount>()
        .toList()
}

fun Database.Read.getMessagesSentByAccount(accountId: MessageAccountId, pageSize: Int, page: Int): Paged<SentMessage> {
    val params = mapOf(
        "accountId" to accountId,
        "offset" to (page - 1) * pageSize,
        "pageSize" to pageSize
    )

    // language=SQL
    val sql = """
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
        name_view.account_name,
        acc.type AS account_type
    FROM message_recipients rec
    JOIN message m ON rec.message_id = m.id
    JOIN message_account_name_view name_view ON rec.recipient_id = name_view.id
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
    (SELECT jsonb_agg(json_build_object(
           'id', rec.recipient_id,
           'name', rec.account_name,
           'type', rec.account_type
       ))) AS recipients,
    (SELECT coalesce(jsonb_agg(json_build_object(
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
    """.trimIndent()

    return this.createQuery(sql)
        .bindMap(params)
        .mapToPaged(pageSize)
}

data class ThreadWithParticipants(
    val threadId: MessageThreadId,
    val type: MessageType,
    val senders: Set<MessageAccountId>,
    val recipients: Set<MessageAccountId>
)

fun Database.Read.getThreadByMessageId(messageId: MessageId): ThreadWithParticipants? {
    val sql = """
        SELECT
            t.id AS threadId,
            t.message_type AS type,
            (SELECT array_agg(m2.sender_id)) as senders,
            (SELECT array_agg(rec.recipient_id)) as recipients
            FROM message m
            JOIN message_thread t ON m.thread_id = t.id
            JOIN message m2 ON m2.thread_id = t.id 
            JOIN message_recipients rec ON rec.message_id = m2.id
            WHERE m.id = :messageId
            GROUP BY t.id, t.message_type
    """.trimIndent()
    return this.createQuery(sql)
        .bind("messageId", messageId)
        .mapTo<ThreadWithParticipants>()
        .firstOrNull()
}

data class MessageReceiversResult(
    val childId: ChildId,
    val groupId: GroupId,
    val groupName: String,
    val childFirstName: String,
    val childLastName: String,
    val childDateOfBirth: LocalDate,
    val accountId: MessageAccountId,
    val receiverFirstName: String,
    val receiverLastName: String
)

fun Database.Read.getReceiversForNewMessage(
    employeeOrMobileId: Id<*>,
    unitId: DaycareId
): List<MessageReceiversResponse> {
    // language=sql
    val sql = """
        WITH children AS (
            SELECT pl.child_id, dg.id group_id, dg.name group_name
            FROM daycare_group dg
            JOIN daycare_group_placement gpl ON dg.id = gpl.daycare_group_id AND daterange(gpl.start_date, gpl.end_date, '[]') @> :date
            JOIN placement pl ON gpl.daycare_placement_id = pl.id
            JOIN daycare d ON pl.unit_id = d.id
            WHERE pl.unit_id = :unitId AND EXISTS (
                SELECT 1
                FROM child_daycare_acl(:date)
                JOIN mobile_device_daycare_acl_view USING (daycare_id)
                WHERE mobile_device_id = :employeeOrMobileId
                AND child_id = pl.child_id
                
                UNION ALL
                
                SELECT 1
                FROM employee_child_daycare_acl(:date)
                WHERE employee_id = :employeeOrMobileId
                AND child_id = pl.child_id
            )
            AND 'MESSAGING' = ANY(d.enabled_pilot_features)
            
            UNION ALL 
            
            SELECT bc.child_id, dg.id group_id, dg.name group_name
            FROM daycare_group dg
            JOIN backup_care bc ON dg.id = bc.group_id AND daterange(bc.start_date, bc.end_date, '[]') @> :date
            JOIN daycare d ON bc.unit_id = d.id
            WHERE d.id = :unitId AND EXISTS (
                SELECT 1
                FROM child_daycare_acl(:date)
                JOIN mobile_device_daycare_acl_view USING (daycare_id)
                WHERE mobile_device_id = :employeeOrMobileId
                AND child_id = bc.child_id
                
                UNION ALL
                
                SELECT 1
                FROM employee_child_daycare_acl(:date)
                WHERE employee_id = :employeeOrMobileId
                AND child_id = bc.child_id
            )
            AND 'MESSAGING' = ANY(d.enabled_pilot_features)
        ), receivers AS (
            SELECT DISTINCT c.child_id, c.group_id, c.group_name, g.guardian_id AS receiver_id
            FROM children c
            JOIN guardian g ON g.child_id = c.child_id
            WHERE NOT EXISTS (
                SELECT 1 FROM messaging_blocklist bl
                WHERE bl.child_id = c.child_id
                AND bl.blocked_recipient = g.guardian_id
            )
        )
        SELECT
            acc.id account_id,
            r.group_id,
            r.group_name,
            p.first_name receiver_first_name,
            p.last_name receiver_last_name,
            r.child_id,
            c.first_name child_first_name,
            c.last_name child_last_name,
            c.date_of_birth child_date_of_birth
        FROM receivers r
        JOIN person p ON r.receiver_id = p.id
        JOIN person c ON r.child_id = c.id
        JOIN message_account acc ON p.id = acc.person_id
    """.trimIndent()

    return this.createQuery(sql)
        .bind("employeeOrMobileId", employeeOrMobileId)
        .bind("date", HelsinkiDateTime.now().toLocalDate())
        .bind("unitId", unitId)
        .mapTo<MessageReceiversResult>()
        .toList()
        .groupBy { it.groupId }
        .map { (groupId, receiverChildren) ->
            MessageReceiversResponse(
                groupId = groupId,
                groupName = receiverChildren.first().groupName,
                receivers = receiverChildren.groupBy { it.childId }
                    .map { (childId, receivers) ->
                        MessageReceiver(
                            childId = childId,
                            childFirstName = receivers.first().childFirstName,
                            childLastName = receivers.first().childLastName,
                            childDateOfBirth = receivers.first().childDateOfBirth,
                            receiverPersons = receivers.map {
                                MessageReceiverPerson(
                                    accountId = it.accountId,
                                    receiverFirstName = it.receiverFirstName,
                                    receiverLastName = it.receiverLastName
                                )
                            }
                        )
                    }
            )
        }
}

fun Database.Read.isEmployeeAuthorizedToSendTo(clock: EvakaClock, employeeOrMobileId: Id<*>, accountIds: Set<MessageAccountId>): Boolean {
    // language=SQL
    val sql = """
        WITH children AS (
            SELECT child_id
            FROM child_daycare_acl(:today)
            JOIN mobile_device_daycare_acl_view USING (daycare_id)
            WHERE mobile_device_id = :employeeOrMobileId
            
            UNION ALL
            
            SELECT child_id
            FROM employee_child_daycare_acl(:today)
            WHERE employee_id = :employeeOrMobileId
        ), receivers AS (
            SELECT DISTINCT g.guardian_id AS receiver_id
            FROM children c
            JOIN guardian g ON g.child_id = c.child_id
            WHERE NOT EXISTS (
                SELECT 1 FROM messaging_blocklist bl
                WHERE bl.child_id = c.child_id
                AND bl.blocked_recipient = g.guardian_id
            )
        )
        SELECT COUNT(*)
        FROM receivers r
        JOIN message_account acc ON acc.person_id = r.receiver_id AND acc.id = ANY(:accountIds)
    """.trimIndent()

    val numAccounts = createQuery(sql)
        .bind("today", clock.today())
        .bind("employeeOrMobileId", employeeOrMobileId)
        .bind("accountIds", accountIds.toTypedArray())
        .mapTo<Int>()
        .one()

    return numAccounts == accountIds.size
}

fun Database.Transaction.markNotificationAsSent(id: MessageRecipientId, timestamp: HelsinkiDateTime) {
    val sql = """
        UPDATE message_recipients
        SET notification_sent_at = :timestamp
        WHERE id = :id
    """.trimIndent()
    this.createUpdate(sql)
        .bind("id", id)
        .bind("timestamp", timestamp)
        .execute()
}
