// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.message

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.WithCount
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.mapToPaged
import fi.espoo.evaka.shared.withCountMapper
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate
import java.util.UUID

fun Database.Read.getUnreadMessagesCounts(accountIds: Set<UUID>): Set<UnreadCountByAccount> {
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

fun Database.Transaction.markThreadRead(accountId: UUID, threadId: UUID): Int {
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
    contentId: UUID,
    threadId: UUID,
    sender: UUID,
    recipientNames: List<String>,
    repliesToMessageId: UUID? = null,
    sentAt: HelsinkiDateTime? = HelsinkiDateTime.now()
): UUID {
    // language=SQL
    val insertMessageSql = """
        INSERT INTO message (content_id, thread_id, sender_id, sender_name, replies_to, sent_at, recipient_names)
        SELECT :contentId, :threadId, :senderId, name_view.account_name, :repliesToId, :sentAt, :recipientNames
        FROM message_account_name_view name_view
        WHERE name_view.id = :senderId
        RETURNING id
    """.trimIndent()
    return createQuery(insertMessageSql)
        .bind("contentId", contentId)
        .bind("threadId", threadId)
        .bindNullable("repliesToId", repliesToMessageId)
        .bind("senderId", sender)
        .bind("sentAt", sentAt)
        .bind("recipientNames", recipientNames.toTypedArray())
        .mapTo<UUID>()
        .one()
}

fun Database.Transaction.insertMessageContent(
    content: String,
    sender: UUID
): UUID {
    // language=SQL
    val messageContentSql = "INSERT INTO message_content (content, author_id) VALUES (:content, :authorId) RETURNING id"
    return createQuery(messageContentSql)
        .bind("content", content)
        .bind("authorId", sender)
        .mapTo<UUID>()
        .one()
}

fun Database.Transaction.insertRecipients(
    recipientAccountIds: Set<UUID>,
    messageId: UUID,
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
): UUID {
    // language=SQL
    val insertThreadSql = "INSERT INTO message_thread (message_type, title) VALUES (:messageType, :title) RETURNING id"
    return createQuery(insertThreadSql)
        .bind("messageType", type)
        .bind("title", title)
        .mapTo<UUID>()
        .one()
}

data class ReceivedMessageResultItem(
    val count: Int,
    val id: UUID,
    val title: String,
    val type: MessageType,
    val messageId: UUID,
    val sentAt: HelsinkiDateTime,
    val content: String,
    val senderId: UUID,
    val senderName: String,
    val senderAccountType: AccountType,
    val readAt: HelsinkiDateTime? = null,
    val recipientId: UUID,
    val recipientName: String,
    val recipientAccountType: AccountType
)

fun Database.Read.getMessagesReceivedByAccount(accountId: UUID, pageSize: Int, page: Int, isCitizen: Boolean = false): Paged<MessageThread> {
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
        CASE
            WHEN sender_acc.employee_id IS NOT NULL THEN 'PERSONAL'
            WHEN sender_acc.daycare_group_id IS NOT NULL THEN 'GROUP'
            ELSE 'CITIZEN'
        END AS sender_account_type,
        c.content,
        rec.read_at,
        rec.recipient_id,
        acc.account_name recipient_name,
        CASE
            WHEN recipient_acc.employee_id IS NOT NULL THEN 'PERSONAL'
            WHEN recipient_acc.daycare_group_id IS NOT NULL THEN 'GROUP'
            ELSE 'CITIZEN'
        END AS recipient_account_type
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
    SELECT id, message_type AS type, title, last_message, COUNT(*) OVER () AS count
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
    msg.message_id,
    msg.sent_at,
    msg.content,
    msg.sender_name,
    msg.sender_id,
    msg.sender_account_type,
    msg.read_at,
    msg.recipient_id,
    msg.recipient_name,
    msg.recipient_account_type
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
                                    }.toSet()
                            )
                        }
                )
            )
        }
        .let(mapToPaged(pageSize))
}

data class MessageResultItem(
    val id: UUID,
    val senderId: UUID,
    val senderName: String,
    val senderAccountType: AccountType,
    val recipientId: UUID,
    val recipientName: String,
    val recipientAccountType: AccountType,
    val sentAt: HelsinkiDateTime,
    val content: String,
)

fun Database.Read.getMessage(id: UUID): Message {
    val sql = """
        SELECT 
            m.id,
            m.sender_id,
            m.sender_name,
            CASE
                WHEN sender_acc.employee_id IS NOT NULL THEN 'PERSONAL'
                WHEN sender_acc.daycare_group_id IS NOT NULL THEN 'GROUP'
                ELSE 'CITIZEN'
            END AS sender_account_type,
            m.sent_at,
            c.content,
            rec.recipient_id,
            recipient_acc_name.account_name recipient_name,
            CASE
                WHEN recipient_acc.employee_id IS NOT NULL THEN 'PERSONAL'
                WHEN recipient_acc.daycare_group_id IS NOT NULL THEN 'GROUP'
                ELSE 'CITIZEN'
            END AS recipient_account_type
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
                recipients = messages.map { MessageAccount(it.recipientId, it.recipientName, it.recipientAccountType) }.toSet()
            )
        }
        .single()
}

fun Database.Read.getCitizenReceivers(accountId: UUID): List<MessageAccount> {
    val params = mapOf(
        "accountId" to accountId,
    )

    // language=SQL
    val sql = """
WITH all_placement_ids AS (
    SELECT pl.id, pl.unit_id
    FROM guardian g
    JOIN placement pl
    ON g.child_id = pl.child_id
    WHERE guardian_id = (
        SELECT person_id AS id
        FROM message_account
        WHERE id = :accountId
    )
    AND Daterange(pl.start_date, pl.end_date, '[]') @> current_date
    AND NOT (
        SELECT EXISTS (
            SELECT 1
            FROM messaging_blocklist
            WHERE child_id = pl.child_id
            AND blocked_recipient = guardian_id
            )
        )

    UNION

    SELECT pl.id, pl.unit_id
    FROM fridge_child fg
    JOIN placement pl
    ON fg.child_id = pl.child_id
    WHERE head_of_child = (
        SELECT person_id AS id
        FROM message_account
        WHERE id = :accountId
    )
    AND Daterange(pl.start_date, pl.end_date, '[]') @> current_date
    AND Daterange(fg.start_date, fg.end_date, '[]') @> current_date
    AND fg.conflict = false
    AND NOT (
        SELECT EXISTS (
            SELECT 1
            FROM messaging_blocklist
            WHERE child_id = pl.child_id
            AND blocked_recipient = head_of_child
            )
        )

),
pilot_placement_ids AS (
    SELECT pl.id
    FROM all_placement_ids pl
    JOIN daycare d
    ON pl.unit_id = d.id
    WHERE 'MESSAGING' = ANY(d.enabled_pilot_features)
),
supervisors AS (
    SELECT DISTINCT e.id AS id
    FROM pilot_placement_ids
    JOIN placement plt
    ON pilot_placement_ids.id = plt.id
    JOIN daycare d
    ON plt.unit_id = d.id
    JOIN daycare_acl_view acl
    ON acl.daycare_id = d.id
    JOIN employee e
    ON acl.employee_id = e.id
    WHERE acl.role = 'UNIT_SUPERVISOR'
),
groups AS (
    SELECT DISTINCT gplt.daycare_group_id AS id
    FROM pilot_placement_ids
    JOIN daycare_group_placement gplt
    ON pilot_placement_ids.id = gplt.daycare_placement_id
)

SELECT
    msg.id AS id,
    msg_name.account_name AS name,
    'PERSONAL' AS type
FROM supervisors
JOIN employee e
ON e.id = supervisors.id
JOIN message_account msg
ON e.id = msg.employee_id
JOIN message_account_name_view msg_name
ON msg.id = msg_name.id

UNION

SELECT
    msg.id AS id,
    g.name AS name,
    'GROUP' AS type
FROM groups
JOIN daycare_group g
ON groups.id = g.id
JOIN message_account msg
ON g.id = msg.daycare_group_id
    """.trimIndent()

    return this.createQuery(sql)
        .bindMap(params)
        .mapTo<MessageAccount>()
        .toList()
}

fun Database.Read.getMessagesSentByAccount(accountId: UUID, pageSize: Int, page: Int): Paged<SentMessage> {
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
        COUNT(*) OVER () AS count
    FROM message m
    JOIN message_thread t ON m.thread_id = t.id
    WHERE sender_id = :accountId
    GROUP BY content_id, sent_at, recipient_names, title, message_type
    ORDER BY sent_at DESC
    LIMIT :pageSize OFFSET :offset
),
recipients AS (
    SELECT
        m.content_id,
        rec.recipient_id,
        name_view.account_name,
        CASE
            WHEN acc.employee_id IS NOT NULL THEN 'PERSONAL'
            WHEN acc.daycare_group_id IS NOT NULL THEN 'GROUP'
            ELSE 'CITIZEN'
        END AS account_type
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
    mc.content,
    (SELECT jsonb_agg(json_build_object(
           'id', rec.recipient_id,
           'name', rec.account_name,
           'type', rec.account_type
       ))) AS recipients
FROM pageable_messages msg
JOIN recipients rec ON msg.content_id = rec.content_id
JOIN message_content mc ON msg.content_id = mc.id
GROUP BY msg.count, msg.content_id, msg.sent_at, msg.recipient_names, mc.content, msg.message_type, msg.title
ORDER BY msg.sent_at DESC
    """.trimIndent()

    return this.createQuery(sql)
        .bindMap(params)
        .map(withCountMapper<SentMessage>())
        .let(mapToPaged(pageSize))
}

data class ThreadWithParticipants(
    val threadId: UUID,
    val type: MessageType,
    val senders: Set<UUID>,
    val recipients: Set<UUID>
)

fun Database.Read.getThreadByMessageId(messageId: UUID): ThreadWithParticipants? {
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
    val childId: UUID,
    val groupId: GroupId,
    val groupName: String,
    val childFirstName: String,
    val childLastName: String,
    val childDateOfBirth: LocalDate,
    val accountId: UUID,
    val receiverFirstName: String,
    val receiverLastName: String
)

fun Database.Read.getReceiversForNewMessage(
    employeeId: UUID,
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
                SELECT 1 FROM child_acl_view a
                WHERE a.employee_id = :employeeId AND a.child_id = pl.child_id
            )
            AND 'MESSAGING' = ANY(d.enabled_pilot_features)
        ), receivers AS (
            SELECT c.child_id, c.group_id, c.group_name, g.guardian_id AS receiver_id
            FROM children c
            JOIN guardian g ON g.child_id = c.child_id
            WHERE NOT EXISTS (
                SELECT 1 FROM messaging_blocklist bl
                WHERE bl.child_id = c.child_id
                AND bl.blocked_recipient = g.guardian_id
            )

            UNION DISTINCT

            SELECT c.child_id, c.group_id, c.group_name, fc.head_of_child AS receiver_id
            FROM children c
            JOIN fridge_child fc ON fc.child_id = c.child_id AND daterange(fc.start_date, fc.end_date, '[]') @> :date
            WHERE NOT EXISTS (
                SELECT 1 FROM messaging_blocklist bl
                WHERE bl.child_id = c.child_id
                AND bl.blocked_recipient = fc.head_of_child
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
        .bind("employeeId", employeeId)
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

fun Database.Read.isEmployeeAuthorizedToSendTo(employeeId: UUID, accountIds: Set<UUID>): Boolean {
    // language=SQL
    val sql = """
        WITH children AS (
            SELECT child_id
            FROM child_acl_view
            WHERE employee_id = :employeeId
        ), receivers AS (
            SELECT g.guardian_id AS receiver_id
            FROM children c
            JOIN guardian g ON g.child_id = c.child_id
            WHERE NOT EXISTS (
                SELECT 1 FROM messaging_blocklist bl
                WHERE bl.child_id = c.child_id
                AND bl.blocked_recipient = g.guardian_id
            )

            UNION DISTINCT

            SELECT fc.head_of_child AS receiver_id
            FROM children c
            JOIN fridge_child fc ON fc.child_id = c.child_id AND daterange(fc.start_date, fc.end_date, '[]') @> :date
            WHERE NOT EXISTS (
                SELECT 1 FROM messaging_blocklist bl
                WHERE bl.child_id = c.child_id
                AND bl.blocked_recipient = fc.head_of_child
            )
        )
        SELECT COUNT(*)
        FROM receivers r
        JOIN message_account acc ON acc.person_id = r.receiver_id AND acc.id = ANY(:accountIds)
    """.trimIndent()

    val numAccounts = createQuery(sql)
        .bind("employeeId", employeeId)
        .bind("date", LocalDate.now())
        .bind("accountIds", accountIds.toTypedArray())
        .mapTo<Int>()
        .one()

    return numAccounts == accountIds.size
}

fun Database.Transaction.markNotificationAsSent(messageRecipientId: UUID) {
    val sql = """
        UPDATE message_recipients
        SET notification_sent_at = now()
        WHERE id = :id
    """.trimIndent()
    this.createUpdate(sql)
        .bind("id", messageRecipientId)
        .execute()
}
