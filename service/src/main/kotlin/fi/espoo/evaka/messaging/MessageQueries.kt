// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.application.getCitizenChildren
import fi.espoo.evaka.attachment.MessageAttachment
import fi.espoo.evaka.shared.*
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.db.PredicateSql
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.formatName
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.forTable
import java.time.LocalDate
import mu.KotlinLogging
import org.jdbi.v3.json.Json

private val logger = KotlinLogging.logger {}

sealed class AccountAccessLimit {
    data object NoFurtherLimit : AccountAccessLimit()

    data class AvailableFrom(val date: LocalDate) : AccountAccessLimit()
}

fun Database.Read.getUnreadMessagesCounts(
    idFilter: AccessControlFilter<MessageAccountId>
): Set<UnreadCountByAccount> =
    createQuery {
            sql(
                """
        SELECT
            acc.id as account_id,
            count(*) FILTER (WHERE mtp.folder_id IS NULL AND NOT mt.is_copy) AS unread_count,
            count(*) FILTER (WHERE mtp.folder_id IS NULL AND mt.is_copy) AS unread_copy_count
        FROM message_account acc
        LEFT JOIN message_recipients mr ON mr.recipient_id = acc.id AND mr.read_at IS NULL
        LEFT JOIN message m ON mr.message_id = m.id AND m.sent_at IS NOT NULL
        LEFT JOIN message_thread mt ON m.thread_id = mt.id
        LEFT JOIN message_thread_participant mtp ON m.thread_id = mtp.thread_id AND mtp.participant_id = acc.id
        WHERE ${predicate(idFilter.forTable("acc"))}
        GROUP BY acc.id
        """
            )
        }
        .toSet()

fun Database.Read.getUnreadMessagesCountsByDaycare(
    daycareId: DaycareId
): Set<UnreadCountByAccountAndGroup> {

    return createQuery {
            sql(
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
JOIN daycare_group dg ON acc.daycare_group_id = dg.id AND dg.daycare_id = ${bind(daycareId)}
WHERE acc.active = true AND m.sent_at IS NOT NULL
GROUP BY acc.id, acc.daycare_group_id
"""
            )
        }
        .toSet<UnreadCountByAccountAndGroup>()
}

fun Database.Transaction.markThreadRead(
    clock: EvakaClock,
    accountId: MessageAccountId,
    threadId: MessageThreadId,
): Int {
    val now = clock.now()
    return createUpdate {
            sql(
                """
UPDATE message_recipients rec
SET read_at = ${bind(now)}
FROM message msg
WHERE rec.message_id = msg.id
  AND msg.thread_id = ${bind(threadId)}
  AND rec.recipient_id = ${bind(accountId)}
  AND read_at IS NULL
"""
            )
        }
        .execute()
}

fun Database.Transaction.archiveThread(
    accountId: MessageAccountId,
    threadId: MessageThreadId,
): Int {
    var archiveFolderId = getArchiveFolderId(accountId)
    if (archiveFolderId == null) {
        archiveFolderId =
            createUpdate {
                    sql(
                        "INSERT INTO message_thread_folder (owner_id, name) VALUES (${bind(accountId)}, 'ARCHIVE') ON CONFLICT DO NOTHING RETURNING id"
                    )
                }
                .executeAndReturnGeneratedKeys()
                .exactlyOne<MessageThreadFolderId>()
    }

    return createUpdate {
            sql(
                "UPDATE message_thread_participant SET folder_id = ${bind(archiveFolderId)} WHERE thread_id = ${bind(threadId)} AND participant_id = ${bind(accountId)}"
            )
        }
        .execute()
}

fun Database.Transaction.insertMessage(
    now: HelsinkiDateTime,
    contentId: MessageContentId,
    threadId: MessageThreadId,
    sender: MessageAccountId,
    recipientNames: List<String>,
    municipalAccountName: String,
    serviceWorkerAccountName: String,
    sentAt: HelsinkiDateTime? =
        null, // Only needed because some tests bypass the message service and controllers
    repliesToMessageId: MessageId? = null,
): MessageId {
    return createQuery {
            sql(
                """
INSERT INTO message (created, content_id, thread_id, sender_id, sender_name, replies_to, sent_at, recipient_names)
SELECT
    ${bind(now)},
    ${bind(contentId)},
    ${bind(threadId)},
    ${bind(sender)},
    CASE 
        WHEN name_view.type = 'MUNICIPAL' THEN ${bind(municipalAccountName)} 
        WHEN name_view.type = 'SERVICE_WORKER' THEN ${bind(serviceWorkerAccountName)} 
        ELSE name_view.name 
    END,
    ${bind(repliesToMessageId)},
    ${bind(sentAt)},
    ${bind(recipientNames)}
FROM message_account_view name_view
WHERE name_view.id = ${bind(sender)}
RETURNING id
"""
            )
        }
        .exactlyOne<MessageId>()
}

fun Database.Transaction.insertMessageContent(
    content: String,
    sender: MessageAccountId,
): MessageContentId {
    return createQuery {
            sql(
                "INSERT INTO message_content (content, author_id) VALUES (${bind(content)}, ${bind(sender)}) RETURNING id"
            )
        }
        .exactlyOne<MessageContentId>()
}

fun Database.Transaction.insertRecipients(
    messageRecipientsPairs: List<Pair<MessageId, Set<MessageAccountId>>>
) {
    val rows: Sequence<Pair<MessageId, MessageAccountId>> =
        messageRecipientsPairs.asSequence().flatMap { (messageId, recipients) ->
            recipients.map { recipient -> Pair(messageId, recipient) }
        }
    executeBatch(rows) {
        sql(
            """
INSERT INTO message_recipients (message_id, recipient_id)
VALUES (${bind { (messageId, _) -> messageId }}, ${bind { (_, accountId) -> accountId }})
"""
        )
    }
}

fun Database.Transaction.insertMessageThreadChildren(
    childrenThreadPairs: List<Pair<Set<ChildId>, MessageThreadId>>
) {
    val rows: Sequence<Pair<MessageThreadId, ChildId>> =
        childrenThreadPairs.asSequence().flatMap { (children, threadId) ->
            children.map { childId -> Pair(threadId, childId) }
        }
    executeBatch(rows) {
        sql(
            """
INSERT INTO message_thread_children (thread_id, child_id)
VALUES (${bind { (threadId, _) -> threadId }}, ${bind { (_, childId) -> childId }})
"""
        )
    }
}

fun Database.Transaction.upsertSenderThreadParticipants(
    senderId: MessageAccountId,
    threadIds: List<MessageThreadId>,
    now: HelsinkiDateTime,
) {
    executeBatch(threadIds) {
        sql(
            """
INSERT INTO message_thread_participant as tp (thread_id, participant_id, last_message_timestamp, last_sent_timestamp)
VALUES (${bind { threadId -> threadId }}, ${bind(senderId)}, ${bind(now)}, ${bind(now)})
ON CONFLICT (thread_id, participant_id) DO UPDATE SET last_message_timestamp = ${bind(now)}, last_sent_timestamp = ${bind(now)}
"""
        )
    }
}

fun Database.Transaction.upsertReceiverThreadParticipants(
    contentId: MessageContentId,
    now: HelsinkiDateTime,
) {
    createUpdate {
            sql(
                """
INSERT INTO message_thread_participant as tp (thread_id, participant_id, last_message_timestamp, last_received_timestamp)
SELECT m.thread_id, mr.recipient_id, ${bind(now)}, ${bind(now)}
FROM message m
JOIN message_recipients mr ON mr.message_id = m.id
WHERE m.content_id = ${bind(contentId)}
ON CONFLICT (thread_id, participant_id) DO UPDATE SET last_message_timestamp = ${bind(now)}, last_received_timestamp = ${bind(now)}
"""
            )
        }
        .execute()

    // If the receiver has archived the thread, move it back to inbox
    createUpdate {
            sql(
                """
UPDATE message_thread_participant mtp
SET folder_id = NULL
WHERE
    (thread_id, participant_id) = ANY(
        SELECT m.thread_id, mr.recipient_id
        FROM message m
        JOIN message_recipients mr ON mr.message_id = m.id
        WHERE m.content_id = ${bind(contentId)}
    )
    AND folder_id = (
        SELECT id FROM message_thread_folder mtf
        WHERE mtf.owner_id = mtp.participant_id
        AND mtf.name = 'ARCHIVE'
    )
"""
            )
        }
        .execute()
}

fun Database.Transaction.markMessagesAsSent(
    contentId: MessageContentId,
    sentAt: HelsinkiDateTime,
): List<MessageId> =
    createUpdate {
            sql(
                """
UPDATE message SET sent_at = ${bind(sentAt)}
WHERE content_id = ${bind(contentId)}
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .toList<MessageId>()

fun Database.Transaction.insertThreadsWithMessages(
    count: Int,
    now: HelsinkiDateTime,
    type: MessageType,
    title: String,
    urgent: Boolean,
    sensitive: Boolean,
    isCopy: Boolean,
    contentId: MessageContentId,
    senderId: MessageAccountId,
    recipientNames: List<String>,
    applicationId: ApplicationId?,
    municipalAccountName: String,
    serviceWorkerAccountName: String,
): List<Pair<MessageThreadId, MessageId>> =
    prepareBatch(1..count) { // range is *inclusive*
            sql(
                """
WITH new_thread AS (
    INSERT INTO message_thread (message_type, title, urgent, sensitive, is_copy, application_id) VALUES (${bind(type)}, ${bind(title)}, ${bind(urgent)}, ${bind(sensitive)}, ${bind(isCopy)}, ${bind(applicationId)}) RETURNING id
)
INSERT INTO message (created, content_id, thread_id, sender_id, sender_name, replies_to, recipient_names)
SELECT
    ${bind(now)},
    ${bind(contentId)},
    new_thread.id,
    ${bind(senderId)},
    CASE 
        WHEN name_view.type = 'MUNICIPAL' THEN ${bind(municipalAccountName)}
        WHEN name_view.type = 'SERVICE_WORKER' THEN ${bind(serviceWorkerAccountName)}
        ELSE name_view.name
    END,
    NULL,
    ${bind(recipientNames)}
FROM message_account_view name_view, new_thread
WHERE name_view.id = ${bind(senderId)}
RETURNING id, thread_id
"""
            )
        }
        .executeAndReturn()
        .toList { columnPair("thread_id", "id") }

fun Database.Transaction.insertThread(
    type: MessageType,
    title: String,
    urgent: Boolean,
    sensitive: Boolean,
    isCopy: Boolean,
): MessageThreadId {
    return createQuery {
            sql(
                "INSERT INTO message_thread (message_type, title, urgent, sensitive, is_copy) VALUES (${bind(type)}, ${bind(title)}, ${bind(urgent)}, ${bind(sensitive)}, ${bind(isCopy)}) RETURNING id"
            )
        }
        .exactlyOne<MessageThreadId>()
}

fun Database.Transaction.reAssociateMessageAttachments(
    attachmentIds: Set<AttachmentId>,
    messageContentId: MessageContentId,
): Int {
    return createUpdate {
            sql(
                """
UPDATE attachment
SET
    message_content_id = ${bind(messageContentId)},
    message_draft_id = NULL
WHERE
    id = ANY(${bind(attachmentIds)})
"""
            )
        }
        .execute()
}

private data class ReceivedThread(
    val id: MessageThreadId,
    val title: String,
    val type: MessageType,
    val urgent: Boolean,
    val sensitive: Boolean,
    val isCopy: Boolean,
    @Json val children: List<MessageChild>,
)

data class PagedMessageThreads(val data: List<MessageThread>, val total: Int, val pages: Int) {
    fun <T, R> mapTo(f: PagedFactory<T, R>, mapper: (MessageThread) -> T): R =
        f(data.map(mapper), total, pages)
}

data class PagedCitizenMessageThreads(
    val data: List<CitizenMessageThread>,
    val total: Int,
    val pages: Int,
)

private data class PagedReceivedThreads(
    val data: List<ReceivedThread>,
    val total: Int,
    val pages: Int,
)

/** Return all threads that are visible to the account through sent and received messages */
fun Database.Read.getThreads(
    accountId: MessageAccountId,
    pageSize: Int,
    page: Int,
    municipalAccountName: String,
    serviceWorkerAccountName: String,
): PagedMessageThreads {
    val threads =
        createQuery {
                sql(
                    """
SELECT
    COUNT(*) OVER () AS count,
    t.id,
    t.title,
    t.message_type AS type,
    t.urgent, 
    t.sensitive, 
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
WHERE tp.participant_id = ${bind(accountId)} AND tp.folder_id IS NULL
AND EXISTS (SELECT 1 FROM message m WHERE m.thread_id = t.id AND (m.sender_id = ${bind(accountId)} OR m.sent_at IS NOT NULL))
ORDER BY tp.last_message_timestamp DESC
LIMIT ${bind(pageSize)} OFFSET ${bind((page - 1) * pageSize)}
"""
                )
            }
            .mapToPaged(::PagedReceivedThreads, pageSize)

    val messagesByThread =
        getThreadMessages(
            accountId,
            threads.data.map { it.id },
            municipalAccountName,
            serviceWorkerAccountName,
        )
    return combineThreadsAndMessages(accountId, threads, messagesByThread)
}

fun Database.Read.getAccountAccessLimit(
    groupAccountId: MessageAccountId,
    employeeId: EmployeeId,
): AccountAccessLimit {
    return createQuery {
            sql(
                """
SELECT
    dga.created::date
FROM daycare_group_acl dga
JOIN message_account ma ON ma.daycare_group_id = dga.daycare_group_id
JOIN daycare_group dg ON dga.daycare_group_id = dg.id
JOIN daycare_acl da ON da.employee_id = dga.employee_id AND da.daycare_id = dg.daycare_id
WHERE ma.id = ${bind(groupAccountId)} AND dga.employee_id = ${bind(employeeId)} AND da.role != 'UNIT_SUPERVISOR'
"""
            )
        }
        .mapTo<LocalDate>()
        .exactlyOneOrNull()
        .let {
            it?.let { AccountAccessLimit.AvailableFrom(it.minusWeeks(1)) }
                ?: AccountAccessLimit.NoFurtherLimit
        }
}

/** Return all threads in which the account has received messages */
fun Database.Read.getReceivedThreads(
    accountId: MessageAccountId,
    pageSize: Int,
    page: Int,
    municipalAccountName: String,
    serviceWorkerAccountName: String,
    folderId: MessageThreadFolderId? = null,
    accountAccessLimit: AccountAccessLimit = AccountAccessLimit.NoFurtherLimit,
): PagedMessageThreads {
    val accountAccessPredicate =
        if (accountAccessLimit is AccountAccessLimit.AvailableFrom)
            Predicate { where("$it.last_message_timestamp >= ${bind(accountAccessLimit.date)}") }
        else Predicate.alwaysTrue()

    val threads =
        createQuery {
                sql(
                    """
SELECT
    COUNT(*) OVER () AS count,
    t.id,
    t.title,
    t.message_type AS type,
    t.urgent,
    t.sensitive,
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
    tp.participant_id = ${bind(accountId)} AND
    tp.last_received_timestamp IS NOT NULL AND
    NOT t.is_copy AND
    tp.folder_id IS NOT DISTINCT FROM ${bind(folderId)} AND 
    EXISTS (SELECT 1 FROM message m WHERE m.thread_id = t.id AND m.sent_at IS NOT NULL) AND
    ${predicate(accountAccessPredicate.forTable("tp"))}
ORDER BY tp.last_message_timestamp DESC
LIMIT ${bind(pageSize)} OFFSET ${bind((page - 1) * pageSize)}
        """
                )
            }
            .mapToPaged(::PagedReceivedThreads, pageSize)

    val messagesByThread =
        getThreadMessages(
            accountId,
            threads.data.map { it.id },
            municipalAccountName,
            serviceWorkerAccountName,
        )
    return combineThreadsAndMessages(accountId, threads, messagesByThread)
}

private fun Database.Read.getThreadMessages(
    accountId: MessageAccountId,
    threadIds: List<MessageThreadId>,
    municipalAccountName: String,
    serviceWorkerAccountName: String,
): Map<MessageThreadId, List<Message>> {
    if (threadIds.isEmpty()) return mapOf()
    return createQuery {
            sql(
                """
SELECT
    m.id,
    m.thread_id,
    COALESCE(m.sent_at, m.created) AS sent_at,
    mc.content,
    mr_self.read_at,
    (
        SELECT jsonb_build_object(
            'id', mav.id,
            'name', CASE 
                WHEN mav.type = 'MUNICIPAL' THEN ${bind(municipalAccountName)}
                WHEN mav.type = 'SERVICE_WORKER' THEN ${bind(serviceWorkerAccountName)}
                ELSE mav.name 
            END,
            'type', mav.type
        )
        FROM message_account_view mav
        WHERE mav.id = m.sender_id
    ) AS sender,
    (
        SELECT jsonb_agg(
            jsonb_build_object(
                'id', mav.id,
                'name', CASE
                    WHEN mav.type = 'MUNICIPAL' THEN ${bind(municipalAccountName)} 
                    WHEN mav.type = 'SERVICE_WORKER' THEN ${bind(serviceWorkerAccountName)}
                    ELSE mav.name
                END,
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
LEFT JOIN message_recipients mr_self ON mr_self.message_id = m.id AND mr_self.recipient_id = ${bind(accountId)}
WHERE
    m.thread_id = ANY(${bind(threadIds)}) AND
    (m.sender_id = ${bind(accountId)} OR EXISTS (
        SELECT 1
        FROM message_recipients mr
        WHERE mr.message_id = m.id AND mr.recipient_id = ${bind(accountId)}
    )) AND
    (m.sender_id = ${bind(accountId)} OR m.sent_at IS NOT NULL)
ORDER BY m.sent_at
"""
            )
        }
        .toList<Message>()
        .groupBy { it.threadId }
}

private fun combineThreadsAndMessages(
    accountId: MessageAccountId,
    threads: PagedReceivedThreads,
    messagesByThread: Map<MessageThreadId, List<Message>>,
): PagedMessageThreads {
    val messageThreads =
        threads.data.flatMap { thread ->
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
                        sensitive = thread.sensitive,
                        isCopy = thread.isCopy,
                        children = thread.children,
                        messages = messages,
                    )
                )
            }
        }
    return PagedMessageThreads(messageThreads, threads.total, threads.pages)
}

data class MessageCopy(
    val threadId: MessageThreadId,
    val messageId: MessageId,
    val title: String,
    val type: MessageType,
    val urgent: Boolean,
    val sensitive: Boolean,
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
    @Json val attachments: List<MessageAttachment>,
)

data class PagedMessageCopies(val data: List<MessageCopy>, val total: Int, val pages: Int)

fun Database.Read.getMessageCopiesByAccount(
    accountId: MessageAccountId,
    pageSize: Int,
    page: Int,
    accountAccessLimit: AccountAccessLimit = AccountAccessLimit.NoFurtherLimit,
): PagedMessageCopies {
    val accountAccessPredicate =
        if (accountAccessLimit is AccountAccessLimit.AvailableFrom)
            Predicate { where("$it.sent_at >= ${bind(accountAccessLimit.date)}") }
        else Predicate.alwaysTrue()

    return createQuery {
            sql(
                """
SELECT
    COUNT(*) OVER () AS count,
    t.id AS thread_id,
    m.id AS message_id,
    t.title,
    t.message_type AS type,
    t.urgent,
    t.sensitive,
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
WHERE rec.recipient_id = ${bind(accountId)} AND t.is_copy AND m.sent_at IS NOT NULL AND
    ${predicate(accountAccessPredicate.forTable("m"))}
ORDER BY m.sent_at DESC
LIMIT ${bind(pageSize)} OFFSET ${bind((page - 1) * pageSize)}
"""
            )
        }
        .mapToPaged(::PagedMessageCopies, pageSize)
}

fun Database.Read.getSentMessage(
    senderId: MessageAccountId,
    messageId: MessageId,
    serviceWorkerAccountName: String,
): Message {
    return createQuery {
            sql(
                """
SELECT
    m.id,
    m.thread_id,
    COALESCE(m.sent_at, m.created) AS sent_at,  -- use the created timestamp until the asyncjob marks the message as sent
    mc.content,
    (
        SELECT jsonb_build_object('id', mav.id, 'name', CASE mav.type WHEN 'SERVICE_WORKER' THEN ${bind(serviceWorkerAccountName)} ELSE mav.name END, 'type', mav.type)
        FROM message_account_view mav
        WHERE mav.id = m.sender_id
    ) AS sender,
    (
        SELECT jsonb_agg(jsonb_build_object('id', mav.id, 'name', CASE mav.type WHEN 'SERVICE_WORKER' THEN ${bind(serviceWorkerAccountName)} ELSE mav.name END, 'type', mav.type))
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
WHERE m.id = ${bind(messageId)} AND m.sender_id = ${bind(senderId)}
"""
            )
        }
        .exactlyOne<Message>()
}

fun Database.Read.getCitizenReceivers(
    today: LocalDate,
    accountId: MessageAccountId,
): Map<ChildId, List<MessageAccount>> {
    data class MessageAccountWithChildId(
        val id: MessageAccountId,
        val name: String,
        val type: AccountType,
        val childId: ChildId,
    )

    return createQuery {
            sql(
                """
WITH user_account AS (
    SELECT * FROM message_account WHERE id = ${bind(accountId)}
), children AS (
    SELECT g.child_id, g.guardian_id AS parent_id, true AS guardian_relationship
    FROM user_account acc
    JOIN guardian g ON acc.person_id = g.guardian_id

    UNION ALL

    SELECT fp.child_id, fp.parent_id, false AS guardian_relationship
    FROM user_account acc
    JOIN foster_parent fp ON acc.person_id = fp.parent_id AND valid_during @> ${bind(today)}
), backup_care_placements AS (
    SELECT p.id, p.unit_id, p.child_id, p.group_id
    FROM children c
    JOIN backup_care p ON p.child_id = c.child_id AND daterange((p.start_date - INTERVAL '2 weeks')::date, p.end_date, '[]') @> ${bind(today)}
    WHERE EXISTS (
        SELECT 1 FROM daycare u
        WHERE p.unit_id = u.id AND 'MESSAGING' = ANY(u.enabled_pilot_features)
    )
), placements AS (
    SELECT p.id, p.unit_id, p.child_id
    FROM children c
    JOIN placement p ON p.child_id = c.child_id AND daterange((p.start_date - INTERVAL '2 weeks')::date, p.end_date, '[]') @> ${bind(today)}
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
    JOIN daycare_group_placement dgp ON dgp.daycare_placement_id = p.id AND ${bind(today)} BETWEEN (dgp.start_date - INTERVAL '2 weeks')::date AND dgp.end_date
    JOIN daycare_group g ON g.id = dgp.daycare_group_id
    JOIN message_account acc on g.id = acc.daycare_group_id

    UNION ALL

    SELECT acc.id, g.name, 'GROUP' AS type, p.child_id
    FROM backup_care_placements p
    JOIN daycare_group g ON g.id = p.group_id
    JOIN message_account acc on g.id = acc.daycare_group_id
),
citizen_accounts AS (
    SELECT acc.id, acc_name.name, 'CITIZEN' AS type, c.child_id
    FROM children c
    JOIN guardian g ON c.child_id = g.child_id
    JOIN message_account acc ON g.guardian_id = acc.person_id
    JOIN message_account_view acc_name ON acc_name.id = acc.id
    WHERE acc.id != ${bind(accountId)} AND c.guardian_relationship

    UNION ALL

    SELECT acc.id, acc_name.name, 'CITIZEN' AS type, c.child_id
    FROM children c
    JOIN foster_parent fp ON c.child_id = fp.child_id AND valid_during @> ${bind(today)}
    JOIN message_account acc ON fp.parent_id = acc.person_id
    JOIN message_account_view acc_name ON acc_name.id = acc.id
    WHERE acc.id != ${bind(accountId)} AND NOT c.guardian_relationship
),
mixed_accounts AS (
    SELECT id, name, type, child_id FROM personal_accounts
    UNION ALL
    SELECT id, name, type, child_id FROM group_accounts
    UNION ALL
    SELECT id, name, type, child_id FROM citizen_accounts
)
SELECT id, name, type, child_id FROM mixed_accounts
ORDER BY type, name  -- groups first
"""
            )
        }
        .toList<MessageAccountWithChildId>()
        .groupBy({ it.childId }, { MessageAccount(it.id, it.name, it.type) })
        .filterValues { accounts -> accounts.any { it.type.isPrimaryRecipientForCitizenMessage() } }
}

data class PagedSentMessages(val data: List<SentMessage>, val total: Int, val pages: Int)

fun Database.Read.getMessagesSentByAccount(
    accountId: MessageAccountId,
    pageSize: Int,
    page: Int,
    accountAccessLimit: AccountAccessLimit = AccountAccessLimit.NoFurtherLimit,
): PagedSentMessages {
    val accountAccessPredicate =
        if (accountAccessLimit is AccountAccessLimit.AvailableFrom)
            Predicate { where("$it.sent_at >= ${bind(accountAccessLimit.date)}") }
        else Predicate.alwaysTrue()

    return createQuery {
            sql(
                """
WITH pageable_messages AS (
    SELECT
        m.content_id,
        COALESCE(m.sent_at, m.created) AS sent_at,  -- use the created timestamp until the asyncjob marks the message as sent
        m.recipient_names,
        t.title,
        t.message_type,
        t.urgent,
        t.sensitive,
        COUNT(*) OVER () AS count
    FROM message m
    JOIN message_thread t ON m.thread_id = t.id
    WHERE sender_id = ${bind(accountId)} AND
        ${predicate(accountAccessPredicate.forTable("m"))}
    GROUP BY m.content_id, m.sent_at, m.created, m.recipient_names, t.title, t.message_type, t.urgent, t.sensitive
    ORDER BY sent_at DESC
    LIMIT ${bind(pageSize)} OFFSET ${bind((page - 1) * pageSize)}
)
SELECT
    msg.count,
    msg.content_id,
    msg.sent_at,
    msg.recipient_names,
    msg.title AS thread_title,
    msg.message_type AS type,
    msg.urgent,
    msg.sensitive,
    mc.content,
    (SELECT coalesce(jsonb_agg(jsonb_build_object(
           'id', att.id,
           'name', att.name,
           'contentType', att.content_type
        )), '[]'::jsonb)
        FROM attachment att WHERE att.message_content_id = msg.content_id
        ) AS attachments
FROM pageable_messages msg
JOIN message_content mc ON msg.content_id = mc.id
GROUP BY msg.count, msg.content_id, msg.sent_at, msg.recipient_names, mc.content, msg.message_type, msg.urgent, msg.sensitive, msg.title
ORDER BY msg.sent_at DESC
"""
            )
        }
        .mapToPaged(::PagedSentMessages, pageSize)
}

data class ThreadWithParticipants(
    val threadId: MessageThreadId,
    val type: MessageType,
    val isCopy: Boolean,
    val senders: Set<MessageAccountId>,
    val recipients: Set<MessageAccountId>,
    val applicationId: ApplicationId?,
    val children: Set<ChildId>,
)

fun Database.Read.getThreadByMessageId(messageId: MessageId): ThreadWithParticipants? {
    return createQuery {
            sql(
                """
SELECT
    t.id AS threadId,
    t.message_type AS type,
    t.is_copy,
    t.application_id,
    (SELECT array_agg(m2.sender_id)) as senders,
    (SELECT array_agg(rec.recipient_id)) as recipients,
    (SELECT coalesce(array_agg(mtc.child_id) FILTER (WHERE mtc.child_id IS NOT NULL), '{}')) as children
    FROM message m
    JOIN message_thread t ON m.thread_id = t.id
    JOIN message m2 ON m2.thread_id = t.id
    JOIN message_recipients rec ON rec.message_id = m2.id
    LEFT JOIN message_thread_children mtc ON mtc.thread_id = t.id
    WHERE m.id = ${bind(messageId)}
    GROUP BY t.id, t.message_type
"""
            )
        }
        .exactlyOneOrNull<ThreadWithParticipants>()
}

fun Database.Read.getMessageThread(
    accountId: MessageAccountId,
    threadId: MessageThreadId,
    municipalAccountName: String,
    serviceWorkerAccountName: String,
): MessageThread {
    val thread =
        createQuery {
                sql(
                    """
SELECT
    t.id,
    t.title,
    t.message_type AS type,
    t.urgent,
    t.sensitive,
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
FROM message_thread t
JOIN message_thread_participant tp on t.id = tp.thread_id
WHERE t.id = ${bind(threadId)} AND tp.participant_id = ${bind(accountId)}
  AND EXISTS (SELECT 1 FROM message m WHERE m.thread_id = t.id AND (m.sender_id = ${bind(accountId)} OR m.sent_at IS NOT NULL))
"""
                )
            }
            .exactlyOneOrNull<ReceivedThread>() ?: throw NotFound()

    val messagesByThread =
        getThreadMessages(
            accountId,
            listOf(thread.id),
            municipalAccountName,
            serviceWorkerAccountName,
        )
    return combineThreadsAndMessages(
            accountId,
            PagedReceivedThreads(listOf(thread), 1, 1),
            messagesByThread,
        )
        .data
        .firstOrNull() ?: throw NotFound("Thread $threadId not found")
}

fun Database.Read.getMessageThreadByApplicationId(
    accountId: MessageAccountId,
    applicationId: ApplicationId,
    municipalAccountName: String,
    serviceWorkerAccountName: String,
): MessageThread? {
    val thread =
        createQuery {
                sql(
                    """
SELECT
    t.id,
    t.title,
    t.message_type AS type,
    t.urgent,
    t.sensitive,
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
FROM message_thread t
WHERE t.application_id = ${bind(applicationId)}
  AND EXISTS (SELECT 1 FROM message m WHERE m.thread_id = t.id AND (m.sender_id = ${bind(accountId)} OR m.sent_at IS NOT NULL))
GROUP BY t.id
LIMIT 1
        """
                )
            }
            .exactlyOneOrNull<ReceivedThread>()

    if (thread != null) {
        val messagesByThread =
            getThreadMessages(
                accountId,
                listOf(thread.id),
                municipalAccountName,
                serviceWorkerAccountName,
            )
        return combineThreadsAndMessages(
                accountId,
                PagedReceivedThreads(listOf(thread), 1, 1),
                messagesByThread,
            )
            .data
            .firstOrNull()
    }
    return null
}

data class UnitMessageReceiversResult(
    val accountId: MessageAccountId,
    val unitId: DaycareId?,
    val unitName: String?,
    val groupId: GroupId,
    val groupName: String,
    val childId: ChildId,
    val firstName: String,
    val lastName: String,
)

data class MunicipalMessageReceiversResult(
    val accountId: MessageAccountId,
    val areaId: AreaId,
    val areaName: String,
    val unitId: DaycareId,
    val unitName: String,
)

fun Database.Read.getReceiversForNewMessage(
    idFilter: AccessControlFilter<MessageAccountId>,
    today: LocalDate,
): List<MessageReceiversResponse> {
    val unitReceivers =
        createQuery {
                sql(
                    """
        WITH accounts AS (
            SELECT id, employee_id, daycare_group_id FROM message_account
            WHERE
                ${predicate(idFilter.forTable("message_account"))} AND
                type = ANY('{PERSONAL,GROUP}'::message_account_type[])
        ), children AS (
            SELECT a.id AS account_id, p.child_id, NULL AS unit_id, NULL AS unit_name, p.group_id, g.name AS group_name
            FROM accounts a
            JOIN realized_placement_all(${bind(today)}) p ON a.daycare_group_id = p.group_id
            JOIN daycare d ON p.unit_id = d.id
            JOIN daycare_group g ON p.group_id = g.id
            WHERE 'MESSAGING' = ANY(d.enabled_pilot_features)

            UNION ALL

            SELECT a.id AS account_id, p.child_id, p.unit_id, d.name AS unit_name, p.group_id, g.name AS group_name
            FROM accounts a
            JOIN daycare_acl_view acl ON a.employee_id = acl.employee_id
            JOIN daycare d ON acl.daycare_id = d.id
            JOIN daycare_group g ON d.id = g.daycare_id
            JOIN realized_placement_all(${bind(today)}) p ON g.id = p.group_id
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
            WHERE g.child_id = c.child_id

            UNION ALL

            SELECT 1
            FROM foster_parent fp
            WHERE fp.child_id = c.child_id AND fp.valid_during @> ${bind(today)} 
        )
        ORDER BY c.unit_name, c.group_name
        """
                )
            }
            .toList<UnitMessageReceiversResult>()
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
                                    receivers = getReceiverGroups(groups),
                                )
                            )
                    }
                MessageReceiversResponse(accountId = accountId, receivers = accountReceivers)
            }

    val municipalReceivers =
        createQuery {
                sql(
                    """
        WITH accounts AS (
            SELECT id, type, daycare_group_id, employee_id, person_id FROM message_account
            WHERE ${predicate(idFilter.forTable("message_account"))} AND type = 'MUNICIPAL'::message_account_type
        )
        SELECT acc.id AS account_id, a.id AS area_id, a.name AS area_name, d.id AS unit_id, d.name AS unit_name
        FROM accounts acc, care_area a
        JOIN daycare d ON a.id = d.care_area_id
        WHERE
            daterange(d.opening_date, d.closing_date, '[]') @> ${bind(today)} AND
            'MESSAGING' = ANY(d.enabled_pilot_features)
        ORDER BY area_name
        """
                )
            }
            .toList<MunicipalMessageReceiversResult>()
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
                                        name = unit.unitName,
                                    )
                                },
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
                            name = formatName(it.firstName, it.lastName, true),
                        )
                    },
            )
        }

fun Database.Read.getMessageAccountsForRecipients(
    accountId: MessageAccountId,
    recipients: Set<MessageRecipient>,
    filters: MessageController.PostMessageFilters?,
    date: LocalDate,
): List<Pair<MessageAccountId, ChildId?>> {
    val groupedRecipients = recipients.groupBy { it.type }
    val areaRecipients = groupedRecipients[MessageRecipientType.AREA]?.map { it.id } ?: listOf()
    val unitRecipients = groupedRecipients[MessageRecipientType.UNIT]?.map { it.id } ?: listOf()
    val groupRecipients = groupedRecipients[MessageRecipientType.GROUP]?.map { it.id } ?: listOf()
    val childRecipients = groupedRecipients[MessageRecipientType.CHILD]?.map { it.id } ?: listOf()
    val citizenRecipients =
        groupedRecipients[MessageRecipientType.CITIZEN]?.map { it.id } ?: listOf()

    val filterPredicates =
        PredicateSql.allNotNull(
            if (filters?.yearsOfBirth?.isNotEmpty() == true) {
                PredicateSql {
                    where("date_part('year', p.date_of_birth) = ANY(${bind(filters.yearsOfBirth)})")
                }
            } else null,
            if (filters?.shiftCare == true && filters.intermittentShiftCare) {
                PredicateSql {
                    where("sn.shift_care = ANY('{FULL,INTERMITTENT}'::shift_care_type[])")
                }
            } else if (filters?.shiftCare == true) {
                PredicateSql { where("sn.shift_care = 'FULL'::shift_care_type") }
            } else if (filters?.intermittentShiftCare == true) {
                PredicateSql { where("sn.shift_care = 'INTERMITTENT'::shift_care_type") }
            } else null,
            if (filters?.familyDaycare == true) {
                PredicateSql { where("d.type && '{FAMILY,GROUP_FAMILY}'::care_types[]") }
            } else null,
        )

    return createQuery {
            sql(
                """
WITH sender AS (
    SELECT type, daycare_group_id, employee_id FROM message_account WHERE id = ${bind(accountId)}
), children AS (
    SELECT DISTINCT pl.child_id
    FROM realized_placement_all(${bind(date)}) pl
    JOIN daycare d ON pl.unit_id = d.id
    LEFT JOIN person p ON p.id = pl.child_id
    LEFT JOIN service_need sn ON sn.placement_id = pl.placement_id AND daterange(sn.start_date, sn.end_date, '[]') @> ${bind(date)}
    WHERE (d.care_area_id = ANY(${bind(areaRecipients)}) OR pl.unit_id = ANY(${bind(unitRecipients)}) OR pl.group_id = ANY(${bind(groupRecipients)}) OR pl.child_id = ANY(${bind(childRecipients)}))
    AND ${predicate(filterPredicates)}
    AND EXISTS (
        SELECT 1
        FROM child_daycare_acl(${bind(date)})
        JOIN mobile_device_daycare_acl_view USING (daycare_id)
        WHERE mobile_device_id = (SELECT sender.employee_id FROM sender)
        AND child_id = pl.child_id

        UNION ALL

        SELECT 1
        FROM employee_child_daycare_acl(${bind(date)})
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

UNION

SELECT acc.id AS account_id, c.child_id
FROM children c
JOIN foster_parent fp ON fp.child_id = c.child_id AND fp.valid_during @> ${bind(date)}
JOIN message_account acc ON fp.parent_id = acc.person_id

UNION

SELECT acc.id AS account_id, NULL as child_id
FROM person p
JOIN message_account acc ON p.id = acc.person_id
WHERE p.id = ANY(${bind(citizenRecipients)})
"""
            )
        }
        .toList { column<MessageAccountId>("account_id") to column<ChildId?>("child_id") }
}

fun Database.Transaction.markEmailNotificationAsSent(
    id: MessageRecipientId,
    timestamp: HelsinkiDateTime,
) {
    createUpdate {
            sql(
                """
UPDATE message_recipients
SET email_notification_sent_at = ${bind(timestamp)}
WHERE id = ${bind(id)}
"""
            )
        }
        .execute()
}

fun Database.Read.getStaffCopyRecipients(
    senderId: MessageAccountId,
    recipients: Collection<MessageRecipient>,
    date: LocalDate,
): Set<MessageAccountId> {
    val areaIds = recipients.mapNotNull { it.toAreaId() }
    val unitIds = recipients.mapNotNull { it.toUnitId() }
    val groupIds = recipients.mapNotNull { it.toGroupId() }
    if (areaIds.isEmpty() && unitIds.isEmpty() && groupIds.isEmpty()) return emptySet()

    return createQuery {
            sql(
                """
WITH groups AS (
    SELECT u.id AS unit_id, g.id AS group_id
    FROM daycare u
    JOIN daycare_group g ON u.id = g.daycare_id
    JOIN LATERAL (
        SELECT ma.employee_id, ma.type
        FROM message_account ma
        WHERE ma.id = ${bind(senderId)}
    ) sender ON TRUE
    WHERE (u.care_area_id = ANY(${bind(areaIds)}) OR u.id = ANY(${bind(unitIds)}) OR g.id = ANY(${bind(groupIds)}))
      AND (sender.type = 'MUNICIPAL' OR EXISTS(
        SELECT FROM daycare_acl acl
        WHERE acl.daycare_id = u.id AND sender.employee_id = acl.employee_id
      ))
      AND (u.closing_date IS NULL OR u.closing_date >= ${bind(date)})
      AND (g.end_date IS NULL OR g.end_date >= ${bind(date)})
), units AS (
    SELECT DISTINCT unit_id
    FROM groups
), recipients AS (
    -- group accounts
    SELECT receiver_acc.id
    FROM groups g
    JOIN message_account receiver_acc ON receiver_acc.daycare_group_id = g.group_id

    UNION ALL

    -- unit supervisor and special education teacher accounts
    SELECT receiver_acc.id
    FROM units u
    JOIN daycare_acl unit_employee ON unit_employee.daycare_id = u.unit_id
      AND unit_employee.role = ANY('{UNIT_SUPERVISOR,SPECIAL_EDUCATION_TEACHER}'::user_role[])
    JOIN message_account receiver_acc ON receiver_acc.employee_id = unit_employee.employee_id
)
SELECT id
FROM recipients
WHERE id <> ${bind(senderId)}
"""
            )
        }
        .toSet<MessageAccountId>()
}

fun Database.Read.getArchiveFolderId(accountId: MessageAccountId): MessageThreadFolderId? =
    createQuery {
            sql(
                "SELECT id FROM message_thread_folder WHERE owner_id = ${bind(accountId)} AND name = 'ARCHIVE'"
            )
        }
        .exactlyOneOrNull<MessageThreadFolderId>()

fun Database.Read.unreadMessageForRecipientExists(
    messageId: MessageId,
    recipientId: MessageAccountId,
): Boolean {
    return createQuery {
            sql(
                """
SELECT EXISTS (
    SELECT * FROM message_recipients WHERE message_id = ${bind(messageId)} AND recipient_id = ${bind(recipientId)} AND read_at IS NULL
)
"""
            )
        }
        .exactlyOne<Boolean>()
}

fun Database.Read.getMessageThreadStub(id: MessageThreadId): MessageThreadStub =
    createQuery {
            sql(
                """
SELECT id, message_type AS type, title, urgent, sensitive, is_copy
FROM message_thread
WHERE id = ${bind(id)}
    """
            )
        }
        .exactlyOne<MessageThreadStub>()

fun Database.Read.lockMessageContentForUpdate(id: MessageContentId) {
    createQuery { sql("SELECT 1 FROM message_content WHERE id = ${bind(id)} FOR UPDATE ") }
        .exactlyOneOrNull<Int>()
}

// has a child in shift care during next two weeks
fun Database.Read.messageAttachmentsAllowedForCitizen(
    personId: PersonId,
    today: LocalDate,
): Boolean {
    val childIds = getCitizenChildren(today, personId).map { it.id }.toSet()
    val nearFuture = FiniteDateRange(today, today.plusWeeks(2))
    return createQuery {
            sql(
                """
        SELECT EXISTS(
            SELECT FROM service_need sn
            JOIN placement pl ON sn.placement_id = pl.id
            WHERE pl.child_id = ANY(${bind(childIds)})
                AND daterange(sn.start_date, sn.end_date, '[]') && ${bind(nearFuture)}
                AND sn.shift_care IN ('FULL', 'INTERMITTENT')
        )
    """
            )
        }
        .exactlyOne()
}
