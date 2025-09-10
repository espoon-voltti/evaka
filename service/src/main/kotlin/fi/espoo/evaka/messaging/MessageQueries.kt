// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.getCitizenChildren
import fi.espoo.evaka.attachment.Attachment
import fi.espoo.evaka.invoicing.controller.SortDirection
import fi.espoo.evaka.messaging.MessageController.MessageThreadFolder
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.*
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.db.PredicateSql
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.formatName
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.forTable
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate
import org.jdbi.v3.json.Json

private val logger = KotlinLogging.logger {}

val accessLimitInterval = "interval '1 week'"

sealed class AccountAccessLimit {
    data object NoFurtherLimit : AccountAccessLimit()

    data class AvailableFrom(val date: LocalDate) : AccountAccessLimit()
}

fun Database.Read.getFolders(filter: AccessControlFilter<MessageAccountId>) =
    createQuery {
            sql(
                """
            SELECT mtf.id, mtf.name, mtf.owner_id
            FROM message_thread_folder mtf
            JOIN message_account acc ON mtf.owner_id = acc.id
            WHERE ${predicate(filter.forTable("acc"))} AND mtf.name != 'ARCHIVE'
        """
            )
        }
        .toList<MessageThreadFolder>()

fun Database.Read.getFolder(id: MessageThreadFolderId) =
    createQuery {
            sql(
                """
            SELECT mtf.id, mtf.name, mtf.owner_id
            FROM message_thread_folder mtf
            WHERE mtf.id = ${bind(id)} AND mtf.name != 'ARCHIVE'
        """
            )
        }
        .exactlyOneOrNull<MessageThreadFolder>()

fun Database.Read.getUnreadMessagesCountsEmployee(
    employeeId: EmployeeId
): Set<UnreadCountByAccount> {
    data class RawData(
        val accountId: MessageAccountId,
        val isCopy: Boolean,
        val folderId: MessageThreadFolderId?,
        val count: Int,
    )

    val query = createQuery {
        sql(
            """
        WITH limits AS (
            SELECT
                daycare_group_id,
                (created - $accessLimitInterval)::date AS access_limit
            FROM daycare_group_acl 
            WHERE employee_id = ${bind(employeeId)}
        )
        SELECT 
            acc.id as account_id, 
            mt.is_copy as is_copy, 
            mtp.folder_id, 
            count(mt.id) AS count
        FROM message_account acc
        LEFT JOIN daycare_acl da ON da.employee_id = ${bind(employeeId)}
        JOIN message_recipients mr ON mr.recipient_id = acc.id
        JOIN message m ON mr.message_id = m.id AND m.sent_at IS NOT NULL
        JOIN message_thread_participant mtp ON m.thread_id = mtp.thread_id AND mtp.participant_id = acc.id 
        JOIN message_thread mt ON m.thread_id = mt.id
        LEFT JOIN message_thread_folder mtf ON mtp.folder_id = mtf.id
        WHERE
          mr.read_at IS NULL AND
          (
            mtp.folder_id IS NOT NULL OR
            da.role = 'UNIT_SUPERVISOR' OR
            mtp.last_message_timestamp >= (SELECT access_limit FROM limits WHERE limits.daycare_group_id = acc.daycare_group_id) OR
            m.sent_at >= (SELECT access_limit FROM limits WHERE limits.daycare_group_id = acc.daycare_group_id) OR
            mt.is_copy = false
         ) AND
         (mtp.folder_id IS NULL OR mtf.name != 'ARCHIVE') AND
         acc.id IN (
            -- Employee's own message account
            SELECT acc.id
            FROM message_account acc
            JOIN employee ON acc.employee_id = employee.id
            WHERE employee.id = ${bind(employeeId)}
              AND acc.active = TRUE
            
            UNION ALL
            
            -- Group-based access (daycare group ACL)
            SELECT acc.id
            FROM message_account acc
            JOIN daycare_group_acl gacl ON gacl.daycare_group_id = acc.daycare_group_id
            WHERE gacl.employee_id = ${bind(employeeId)}
              AND acc.active = TRUE
            
            UNION ALL
            
            -- Unit supervisor access to all group accounts in their units
            SELECT acc.id
            FROM message_account acc
            JOIN daycare_group dg ON acc.daycare_group_id = dg.id
            JOIN daycare_acl da ON da.daycare_id = dg.daycare_id
            WHERE da.employee_id = ${bind(employeeId)}
              AND da.role = 'UNIT_SUPERVISOR'
              AND acc.active = TRUE
            
            UNION ALL
            
            -- Municipal account for admins/messaging roles
            SELECT acc.id
            FROM employee e
            JOIN message_account acc ON acc.type = 'MUNICIPAL'
            WHERE e.id = ${bind(employeeId)}
              AND e.roles && '{ADMIN, MESSAGING}'::user_role[]
            
            UNION ALL
            
            -- Service worker account
            SELECT acc.id
            FROM employee e
            JOIN message_account acc ON acc.type = 'SERVICE_WORKER'
            WHERE e.id = ${bind(employeeId)}
              AND e.roles && '{SERVICE_WORKER}'::user_role[]
            
            UNION ALL
            
            -- Finance account
            SELECT acc.id
            FROM employee e
            JOIN message_account acc ON acc.type = 'FINANCE'
            WHERE e.id = ${bind(employeeId)}
              AND e.roles && '{FINANCE_ADMIN}'::user_role[]
        )
        GROUP BY acc.id, mt.is_copy, mtp.folder_id
        """
        )
    }
    logger.debug { query.toString() }
    val data = query.toList<RawData>()

    return data
        .groupBy { it.accountId }
        .map { (accountId, counts) ->
            UnreadCountByAccount(
                accountId = accountId,
                unreadCount = counts.find { !it.isCopy && it.folderId == null }?.count ?: 0,
                unreadCopyCount = counts.find { it.isCopy && it.folderId == null }?.count ?: 0,
                unreadCountByFolder =
                    counts.filter { it.folderId != null }.associate { it.folderId!! to it.count },
            )
        }
        .toSet()
}

fun Database.Read.getUnreadMessagesCountsCitizen(
    idFilter: AccessControlFilter<MessageAccountId>
): Set<UnreadCountByAccount> {
    data class RawData(
        val accountId: MessageAccountId,
        val isCopy: Boolean,
        val folderId: MessageThreadFolderId?,
        val count: Int,
    )

    val data =
        createQuery {
                sql(
                    """
        SELECT 
            acc.id as account_id, 
            coalesce(mt.is_copy, false) as is_copy, 
            mtp.folder_id, 
            count(mt.id) AS count
        FROM message_account acc
        LEFT JOIN message_recipients mr ON mr.recipient_id = acc.id AND mr.read_at IS NULL
        LEFT JOIN message m ON mr.message_id = m.id AND m.sent_at IS NOT NULL
        LEFT JOIN message_thread mt ON m.thread_id = mt.id
        LEFT JOIN message_thread_participant mtp ON m.thread_id = mtp.thread_id AND mtp.participant_id = acc.id
        LEFT JOIN message_thread_folder mtf ON mtp.folder_id = mtf.id
        WHERE ${predicate(idFilter.forTable("acc"))} AND (mtp.folder_id IS NULL OR mtf.name != 'ARCHIVE')
        GROUP BY acc.id, mt.is_copy, mtp.folder_id
        """
                )
            }
            .toList<RawData>()

    return data
        .groupBy { it.accountId }
        .map { (accountId, counts) ->
            UnreadCountByAccount(
                accountId = accountId,
                unreadCount = counts.find { !it.isCopy && it.folderId == null }?.count ?: 0,
                unreadCopyCount = counts.find { it.isCopy && it.folderId == null }?.count ?: 0,
                unreadCountByFolder =
                    counts.filter { it.folderId != null }.associate { it.folderId!! to it.count },
            )
        }
        .toSet()
}

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
    now: HelsinkiDateTime,
    accountId: MessageAccountId,
    threadId: MessageThreadId,
): Int {
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

fun Database.Transaction.markLastReceivedMessageUnread(
    accountId: MessageAccountId,
    threadId: MessageThreadId,
) = execute {
    sql(
        """
    UPDATE message_recipients
    SET read_at = NULL
    FROM (
        SELECT mr.id
        FROM message_recipients mr
        JOIN message m ON mr.message_id = m.id
        WHERE mr.recipient_id = ${bind(accountId)} AND m.thread_id = ${bind(threadId)}
        ORDER BY m.sent_at DESC
        LIMIT 1
    ) AS to_unread
    WHERE message_recipients.id = to_unread.id
"""
    )
}

fun Database.Transaction.moveThreadToFolder(
    accountId: MessageAccountId,
    threadId: MessageThreadId,
    folderId: MessageThreadFolderId,
) =
    createUpdate {
            sql(
                """
            UPDATE message_thread_participant 
            SET folder_id = ${bind(folderId)} 
            WHERE thread_id = ${bind(threadId)} AND participant_id = ${bind(accountId)}
                AND EXISTS (
                    SELECT FROM message_thread_folder mtf 
                    WHERE mtf.id = ${bind(folderId)} AND mtf.owner_id = ${bind(accountId)}
                )
        """
            )
        }
        .updateExactlyOne()

fun Database.Transaction.archiveThread(accountId: MessageAccountId, threadId: MessageThreadId) {
    val archiveFolderId =
        getArchiveFolderId(accountId)
            ?: createUpdate {
                    sql(
                        """
                            INSERT INTO message_thread_folder (owner_id, name) 
                            VALUES (${bind(accountId)}, 'ARCHIVE') 
                            ON CONFLICT DO NOTHING 
                            RETURNING id
                        """
                    )
                }
                .executeAndReturnGeneratedKeys()
                .exactlyOne<MessageThreadFolderId>()

    return moveThreadToFolder(accountId, threadId, archiveFolderId)
}

fun Database.Transaction.insertMessage(
    now: HelsinkiDateTime,
    contentId: MessageContentId,
    threadId: MessageThreadId,
    sender: MessageAccountId,
    recipientNames: List<String>,
    municipalAccountName: String,
    serviceWorkerAccountName: String,
    financeAccountName: String,
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
        WHEN name_view.type = 'FINANCE' THEN ${bind(financeAccountName)}
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
    initialFolder: MessageThreadFolderId? = null,
) {
    executeBatch(threadIds) {
        sql(
            """
INSERT INTO message_thread_participant as tp (thread_id, participant_id, last_message_timestamp, last_sent_timestamp, folder_id)
VALUES (${bind { threadId -> threadId }}, ${bind(senderId)}, ${bind(now)}, ${bind(now)}, ${bind(initialFolder)})
ON CONFLICT (thread_id, participant_id) DO UPDATE SET last_message_timestamp = ${bind(now)}, last_sent_timestamp = ${bind(now)}
"""
        )
    }
}

fun Database.Transaction.upsertRecipientThreadParticipants(
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

    // If the recipient has archived the thread, move it back to inbox
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
    financeAccountName: String,
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
        WHEN name_view.type = 'FINANCE' THEN ${bind(financeAccountName)}
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
    val applicationId: ApplicationId?,
    val applicationType: ApplicationType?,
    val applicationStatus: ApplicationStatus?,
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
    financeAccountName: String,
    folderId: MessageThreadFolderId? = null,
    personAccountId: MessageAccountId? = null,
    messagesSortDirection: SortDirection = SortDirection.ASC,
): PagedMessageThreads {
    val personAccountPredicate =
        if (personAccountId != null) {
            Predicate {
                where(
                    """
EXISTS (
    SELECT 1
    FROM message_thread_participant tp2
    WHERE tp2.participant_id = ${bind(personAccountId)}
    AND tp2.thread_id = $it.thread_id
)
                    """
                        .trimIndent()
                )
            }
        } else {
            Predicate.alwaysTrue()
        }
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
    a.status as application_status,
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
LEFT JOIN application a ON t.application_id = a.id
WHERE tp.participant_id = ${bind(accountId)}
AND ${predicate(personAccountPredicate.forTable("tp"))}
AND tp.folder_id IS NOT DISTINCT FROM ${bind(folderId)}
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
            financeAccountName,
            messagesSortDirection,
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
    (dga.created - $accessLimitInterval)::date
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
            it?.let { AccountAccessLimit.AvailableFrom(it) } ?: AccountAccessLimit.NoFurtherLimit
        }
}

/** Return all threads in which the account has received messages */
fun Database.Read.getReceivedThreads(
    accountId: MessageAccountId,
    pageSize: Int,
    page: Int,
    municipalAccountName: String,
    serviceWorkerAccountName: String,
    financeAccountName: String,
    folderId: MessageThreadFolderId? = null,
    accountAccessLimit: AccountAccessLimit = AccountAccessLimit.NoFurtherLimit,
    childId: ChildId? = null,
): PagedMessageThreads {
    val accountAccessPredicate =
        if (accountAccessLimit is AccountAccessLimit.AvailableFrom)
            Predicate { where("$it.last_message_timestamp >= ${bind(accountAccessLimit.date)}") }
        else Predicate.alwaysTrue()

    val childPredicate =
        if (childId != null)
            Predicate {
                where(
                    """
            EXISTS(
                SELECT FROM message_thread_children mtc 
                WHERE mtc.thread_id = $it.id AND mtc.child_id = ${bind(childId)}
            )
        """
                )
            }
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
    t.application_id,
    a.type as application_type,
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
LEFT JOIN application a on a.id = t.application_id
WHERE
    tp.participant_id = ${bind(accountId)} AND
    tp.last_received_timestamp IS NOT NULL AND
    NOT t.is_copy AND
    tp.folder_id IS NOT DISTINCT FROM ${bind(folderId)} AND 
    EXISTS (SELECT 1 FROM message m WHERE m.thread_id = t.id AND m.sent_at IS NOT NULL) AND
    ${predicate(accountAccessPredicate.forTable("tp").and(childPredicate.forTable("t")))}
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
            financeAccountName,
        )
    return combineThreadsAndMessages(accountId, threads, messagesByThread)
}

private fun Database.Read.getThreadMessages(
    accountId: MessageAccountId,
    threadIds: List<MessageThreadId>,
    municipalAccountName: String,
    serviceWorkerAccountName: String,
    financeAccountName: String,
    sortDirection: SortDirection = SortDirection.ASC,
): Map<MessageThreadId, List<Message>> {
    if (threadIds.isEmpty()) return mapOf()
    val sortDir = sortDirection.name
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
                WHEN mav.type = 'FINANCE' THEN ${bind(financeAccountName)}
                ELSE mav.name 
            END,
            'type', mav.type,
            'personId', mav.person_id
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
                    WHEN mav.type = 'FINANCE' THEN ${bind(financeAccountName)}
                    ELSE mav.name
                END,
                'type', mav.type,
                'personId', mav.person_id
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
ORDER BY m.sent_at $sortDir
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
                logger.warn { "Thread ${thread.id} has no messages for account $accountId" }
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
                        applicationStatus = thread.applicationStatus,
                        children = thread.children,
                        messages = messages,
                        applicationId = thread.applicationId,
                        applicationType = thread.applicationType,
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
    @Json val attachments: List<Attachment>,
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
    financeAccountName: String,
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
        SELECT jsonb_build_object('id', mav.id, 'name', CASE mav.type WHEN 'SERVICE_WORKER' THEN ${bind(serviceWorkerAccountName)} WHEN 'FINANCE' THEN ${bind(financeAccountName)} ELSE mav.name END, 'type', mav.type)
        FROM message_account_view mav
        WHERE mav.id = m.sender_id
    ) AS sender,
    (
        SELECT jsonb_agg(jsonb_build_object('id', mav.id, 'name', CASE mav.type WHEN 'SERVICE_WORKER' THEN ${bind(serviceWorkerAccountName)} WHEN 'FINANCE' THEN ${bind(financeAccountName)} ELSE mav.name END, 'type', mav.type))
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

data class MessageAccountAccess(
    val newMessage: Set<MessageAccountWithPresence>,
    val reply: Set<MessageAccountWithPresence>,
)

fun Database.Read.getCitizenRecipients(
    today: LocalDate,
    accountId: MessageAccountId,
): Map<ChildId, MessageAccountAccess> {
    data class MessageAccountWithChildId(
        val id: MessageAccountId,
        val name: String,
        val type: AccountType,
        val personId: PersonId?,
        val childId: ChildId,
        val replyOnly: Boolean,
        val oooPeriod: FiniteDateRange?,
    )

    val sendNewMessageWeeksBefore = 2L

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
    SELECT p.id, p.unit_id, p.child_id, p.group_id, p.start_date
    FROM children c
    JOIN backup_care p ON p.child_id = c.child_id AND p.end_date >= ${bind(today)}
    WHERE EXISTS (
        SELECT 1 FROM daycare u
        WHERE p.unit_id = u.id AND 'MESSAGING' = ANY(u.enabled_pilot_features)
    )
), placements AS (
    SELECT p.id, p.unit_id, p.child_id, p.start_date
    FROM children c
    JOIN placement p ON p.child_id = c.child_id AND p.end_date >= ${bind(today)}
    AND EXISTS (
        SELECT 1 FROM daycare u
        WHERE p.unit_id = u.id AND 'MESSAGING' = ANY(u.enabled_pilot_features)
    )
),
relevant_placements AS (
    SELECT p.id, p.unit_id, p.child_id, p.start_date
    FROM placements p

    UNION

    SELECT bc.id, bc.unit_id, bc.child_id, bc.start_date
    FROM backup_care_placements bc
),
personal_accounts AS (
    SELECT
        acc.id,
        acc_name.name,
        'PERSONAL' AS type, 
        p.child_id, 
        (acl.role != 'UNIT_SUPERVISOR' OR ${bind(today.plusWeeks(sendNewMessageWeeksBefore))} < p.start_date) AS reply_only,
        ooo.period AS ooo_period
    FROM (
        SELECT unit_id, child_id, min(start_date) AS start_date 
        FROM relevant_placements 
        GROUP BY unit_id, child_id
    ) p
    JOIN daycare_acl acl ON acl.daycare_id = p.unit_id
    JOIN message_account acc ON acc.employee_id = acl.employee_id
    JOIN message_account_view acc_name ON acc_name.id = acc.id
    LEFT JOIN LATERAL (
        SELECT ooo.period
        FROM out_of_office ooo
        WHERE ooo.employee_id = acc.employee_id AND period && DATERANGE(${bind(today)}, NULL)
        LIMIT 1
    ) ooo ON TRUE
    WHERE active IS TRUE
),
group_accounts AS (
    SELECT acc.id, g.name, 'GROUP' AS type, p.child_id, ${bind(today.plusWeeks(sendNewMessageWeeksBefore))} < dgp.start_date AS reply_only
    FROM placements p
    JOIN daycare_group_placement dgp ON dgp.daycare_placement_id = p.id AND dgp.end_date >= ${bind(today)}
    JOIN daycare_group g ON g.id = dgp.daycare_group_id
    JOIN message_account acc on g.id = acc.daycare_group_id

    UNION ALL

    SELECT acc.id, g.name, 'GROUP' AS type, p.child_id, ${bind(today.plusWeeks(sendNewMessageWeeksBefore))} < p.start_date AS reply_only
    FROM backup_care_placements p
    JOIN daycare_group g ON g.id = p.group_id
    JOIN message_account acc on g.id = acc.daycare_group_id
),
citizen_accounts AS (
    SELECT acc.id, acc_name.name, 'CITIZEN' AS type, acc.person_id, c.child_id
    FROM children c
    JOIN guardian g ON c.child_id = g.child_id
    JOIN message_account acc ON g.guardian_id = acc.person_id
    JOIN message_account_view acc_name ON acc_name.id = acc.id
    WHERE acc.id != ${bind(accountId)} AND c.guardian_relationship

    UNION ALL

    SELECT acc.id, acc_name.name, 'CITIZEN' AS type, acc.person_id, c.child_id
    FROM children c
    JOIN foster_parent fp ON c.child_id = fp.child_id AND valid_during @> ${bind(today)}
    JOIN message_account acc ON fp.parent_id = acc.person_id
    JOIN message_account_view acc_name ON acc_name.id = acc.id
    WHERE acc.id != ${bind(accountId)} AND NOT c.guardian_relationship
),
mixed_accounts AS (
    SELECT id, name, type, null::uuid as person_id, child_id, reply_only, ooo_period FROM personal_accounts
    UNION ALL
    SELECT id, name, type, null::uuid as person_id, child_id, reply_only, null as ooo_period FROM group_accounts
    UNION ALL
    SELECT id, name, type, person_id, child_id, FALSE AS reply_only, null as ooo_period FROM citizen_accounts
)
SELECT id, name, type, person_id, child_id, reply_only, ooo_period FROM mixed_accounts
ORDER BY type, name  -- groups first
"""
            )
        }
        .toList<MessageAccountWithChildId>()
        .groupBy { it.childId }
        .mapValues { (_, accounts) ->
            val newMessage =
                accounts
                    .filter { !it.replyOnly }
                    .map {
                        MessageAccountWithPresence(
                            account =
                                MessageAccount(
                                    id = it.id,
                                    name = it.name,
                                    type = it.type,
                                    personId = it.personId,
                                ),
                            outOfOffice = it.oooPeriod,
                        )
                    }
            val reply =
                accounts.map {
                    MessageAccountWithPresence(
                        account =
                            MessageAccount(
                                id = it.id,
                                name = it.name,
                                type = it.type,
                                personId = it.personId,
                            ),
                        outOfOffice = it.oooPeriod,
                    )
                }
            MessageAccountAccess(newMessage.toSet(), reply.toSet())
        }
        .filterValues { accounts ->
            (accounts.newMessage + accounts.reply).any {
                it.account.type.isPrimaryRecipientForCitizenMessage()
            }
        }
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
    val applicationStatus: ApplicationStatus?,
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
    a.status AS application_status,
    (SELECT array_agg(m2.sender_id)) as senders,
    (SELECT array_agg(rec.recipient_id)) as recipients,
    (SELECT coalesce(array_agg(mtc.child_id) FILTER (WHERE mtc.child_id IS NOT NULL), '{}')) as children
    FROM message m
    JOIN message_thread t ON m.thread_id = t.id
    JOIN message m2 ON m2.thread_id = t.id
    JOIN message_recipients rec ON rec.message_id = m2.id
    LEFT JOIN message_thread_children mtc ON mtc.thread_id = t.id
    LEFT JOIN application a ON t.application_id = a.id
    WHERE m.id = ${bind(messageId)}
    GROUP BY t.id, t.message_type, a.status
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
    financeAccountName: String,
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
            financeAccountName,
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
    financeAccountName: String,
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
                financeAccountName,
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

fun Database.Read.getSelectableRecipients(
    idFilter: AccessControlFilter<MessageAccountId>,
    today: LocalDate,
): List<SelectableRecipientsResponse> {
    data class GroupAccountRecipientRow(
        val accountId: MessageAccountId,
        override val groupId: GroupId,
        override val groupName: String,
        override val childId: ChildId,
        override val firstName: String,
        override val lastName: String,
        override val startDate: LocalDate?,
    ) : SelectableRecipientRow

    val groupRecipients =
        createQuery {
                sql(
                    """
WITH group_accounts AS (
    SELECT a.id, a.daycare_group_id AS group_id, dg.name AS group_name
    FROM message_account a
    JOIN daycare_group dg ON a.daycare_group_id = dg.id
    JOIN daycare d ON dg.daycare_id = d.id
    WHERE
        ${predicate(idFilter.forTable("a"))} AND
        'MESSAGING' = ANY(d.enabled_pilot_features)
), children AS (
    -- normal placements
    SELECT a.id AS account_id, p.child_id, a.group_id, a.group_name, NULL::date AS start_date
    FROM group_accounts a
    JOIN daycare_group_placement dgp ON dgp.daycare_group_id = a.group_id
    JOIN placement p ON p.id = dgp.daycare_placement_id
    WHERE
        daterange(p.start_date, p.end_date, '[]') @> ${bind(today)} AND
        daterange(dgp.start_date, dgp.end_date, '[]') @> ${bind(today)}

    UNION ALL

    -- backup placements
    SELECT a.id AS account_id, bc.child_id, a.group_id, a.group_name, NULL::date AS start_date
    FROM group_accounts a
    JOIN backup_care bc ON bc.group_id = a.group_id
    WHERE daterange(bc.start_date, bc.end_date, '[]') @> ${bind(today)}

    UNION ALL

    -- starting children
    SELECT a.id AS account_id, p.child_id, a.group_id, a.group_name, dgp.start_date
    FROM group_accounts a
    JOIN daycare_group_placement dgp ON dgp.daycare_group_id = a.group_id
    JOIN placement p ON p.id = dgp.daycare_placement_id
    WHERE
        dgp.start_date > ${bind(today)} AND
        NOT EXISTS (
            SELECT
            FROM daycare_group_placement earlier_dgp
            JOIN placement earlier_p ON earlier_dgp.daycare_placement_id = earlier_p.id
            WHERE
                earlier_p.child_id = p.child_id AND
                earlier_dgp.daycare_group_id = dgp.daycare_group_id AND
                earlier_dgp.end_date < dgp.start_date AND
                earlier_dgp.end_date >= ${bind(today)}
        )
)
SELECT DISTINCT
    c.account_id,
    c.group_id,
    c.group_name,
    c.child_id,
    c.start_date,
    p.first_name,
    p.last_name
FROM children c
JOIN person p ON p.id = c.child_id
WHERE EXISTS (
    SELECT 1
    FROM guardian g
    WHERE g.child_id = c.child_id)
OR EXISTS (
    SELECT 1
    FROM foster_parent fp
    WHERE fp.child_id = c.child_id AND fp.valid_during @> ${bind(today)}
)
ORDER BY c.group_name
"""
                )
            }
            .toList<GroupAccountRecipientRow>()
            .groupBy { it.accountId }
            .map { (groupKey, recipients) ->
                SelectableRecipientsResponse(
                    accountId = groupKey,
                    receivers =
                        recipients
                            .groupBy { it.startDate != null }
                            .flatMap { (hasStarters, groups) ->
                                getRecipientGroups(hasStarters, groups)
                            },
                )
            }

    data class UnitAccountRecipientRow(
        val accountId: MessageAccountId,
        val unitId: DaycareId,
        val unitName: String,
        override val groupId: GroupId,
        override val groupName: String,
        override val childId: ChildId,
        override val firstName: String,
        override val lastName: String,
        override val startDate: LocalDate?,
    ) : SelectableRecipientRow

    val personalRecipients =
        createQuery {
                sql(
                    """
WITH personal_accounts AS (
    SELECT a.id, acl.daycare_id AS unit_id, d.name AS unit_name
    FROM message_account a
    JOIN daycare_acl_view acl ON acl.employee_id = a.employee_id
    JOIN daycare d ON d.id = acl.daycare_id
    WHERE
        ${predicate(idFilter.forTable("a"))} AND
        'MESSAGING' = ANY(d.enabled_pilot_features)
), children AS (
    -- normal placements
    SELECT a.id AS account_id, p.child_id, a.unit_id, a.unit_name, dgp.daycare_group_id AS group_id, g.name AS group_name, NULL::date AS start_date
    FROM personal_accounts a
    JOIN placement p ON p.unit_id = a.unit_id
    JOIN daycare_group_placement dgp ON dgp.daycare_placement_id = p.id
    JOIN daycare_group g ON g.id = dgp.daycare_group_id
    WHERE
        daterange(p.start_date, p.end_date, '[]') @> ${bind(today)} AND
        daterange(dgp.start_date, dgp.end_date, '[]') @> ${bind(today)}

    UNION ALL

    -- backup placements
    SELECT a.id AS account_id, bc.child_id, a.unit_id, a.unit_name, bc.group_id, g.name AS group_name, NULL::date AS start_date
    FROM personal_accounts a
    JOIN backup_care bc ON bc.unit_id = a.unit_id
    JOIN daycare_group g ON g.id = bc.group_id
    WHERE daterange(bc.start_date, bc.end_date, '[]') @> ${bind(today)}

    UNION ALL

    -- starting children
    SELECT a.id AS account_id, p.child_id, a.unit_id, a.unit_name, dgp.daycare_group_id AS group_id, g.name AS group_name, p.start_date
    FROM personal_accounts a
    JOIN placement p ON p.unit_id = a.unit_id
    JOIN daycare_group_placement dgp ON dgp.daycare_placement_id = p.id
    JOIN daycare_group g ON g.id = dgp.daycare_group_id
    WHERE
        dgp.start_date > ${bind(today)} AND
        NOT EXISTS (
            SELECT
            FROM placement earlier_p
            JOIN daycare_group_placement earlier_dgp ON earlier_dgp.daycare_placement_id = earlier_p.id
            WHERE
                earlier_p.child_id = p.child_id AND
                earlier_dgp.daycare_group_id = dgp.daycare_group_id AND
                earlier_dgp.end_date < dgp.start_date AND
                earlier_dgp.end_date >= ${bind(today)}
        )
)
SELECT DISTINCT
    c.account_id,
    c.unit_id,
    c.unit_name,
    c.group_id,
    c.group_name,
    c.child_id,
    c.start_date,
    p.first_name,
    p.last_name
FROM children c
JOIN person p ON c.child_id = p.id
WHERE EXISTS (
    SELECT 1
    FROM guardian g
    WHERE g.child_id = c.child_id)
OR EXISTS (
    SELECT 1
    FROM foster_parent fp
    WHERE fp.child_id = c.child_id AND fp.valid_during @> ${bind(today)}
)
ORDER BY c.unit_name, c.group_name
"""
                )
            }
            .toList<UnitAccountRecipientRow>()
            .groupBy { it.accountId }
            .map { (groupKey, recipients) ->
                SelectableRecipientsResponse(
                    accountId = groupKey,
                    receivers =
                        recipients
                            .groupBy { Triple(it.unitId, it.unitName, it.startDate != null) }
                            .map { (unit, groups) ->
                                val (unitId, unitName, hasStarters) = unit
                                SelectableRecipient.Unit(
                                    id = unitId,
                                    name = unitName,
                                    hasStarters = hasStarters,
                                    receivers = getRecipientGroups(hasStarters, groups),
                                )
                            },
                )
            }

    data class MunicipalAccountRecipientRow(
        val accountId: MessageAccountId,
        val areaId: AreaId,
        val areaName: String,
        val unitId: DaycareId,
        val unitName: String,
    )

    val municipalRecipients =
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
            .toList<MunicipalAccountRecipientRow>()
            .groupBy { it.accountId }
            .map { (accountId, recipients) ->
                val accountRecipients =
                    recipients
                        .groupBy { it.areaId to it.areaName }
                        .map { (area, units) ->
                            val (areaId, areaName) = area
                            SelectableRecipient.Area(
                                id = areaId,
                                name = areaName,
                                receivers =
                                    units.map { unit ->
                                        SelectableRecipient.UnitInArea(
                                            id = unit.unitId,
                                            name = unit.unitName,
                                        )
                                    },
                            )
                        }
                SelectableRecipientsResponse(accountId = accountId, receivers = accountRecipients)
            }

    return groupRecipients + personalRecipients + municipalRecipients
}

private interface SelectableRecipientRow {
    val groupId: GroupId
    val groupName: String
    val childId: ChildId
    val firstName: String
    val lastName: String
    val startDate: LocalDate?
}

private fun getRecipientGroups(
    hasStarters: Boolean,
    recipients: List<SelectableRecipientRow>,
): List<SelectableRecipient.Group> =
    recipients
        .groupBy { it.groupId to it.groupName }
        .map { (group, children) ->
            val (groupId, groupName) = group
            SelectableRecipient.Group(
                id = groupId,
                name = groupName,
                hasStarters = hasStarters,
                receivers =
                    children.map {
                        SelectableRecipient.Child(
                            id = it.childId,
                            name = formatName(it.firstName, it.lastName, true),
                            startDate = it.startDate,
                        )
                    },
            )
        }

private fun Iterable<MessageRecipient>.areaIds() =
    filterIsInstance<MessageRecipient.Area>().map { it.id }

private fun Iterable<MessageRecipient>.unitIds() =
    filterIsInstance<MessageRecipient.Unit>().map { it.id }

private fun Iterable<MessageRecipient>.groupIds() =
    filterIsInstance<MessageRecipient.Group>().map { it.id }

private fun Iterable<MessageRecipient>.childIds() =
    filterIsInstance<MessageRecipient.Child>().map { it.id }

private fun Iterable<MessageRecipient>.citizenIds() =
    filterIsInstance<MessageRecipient.Citizen>().map { it.id }

fun Database.Read.getMessageAccountsForRecipients(
    accountId: MessageAccountId,
    recipients: Set<MessageRecipient>,
    filters: MessageController.PostMessageFilters?,
    date: LocalDate,
): List<Pair<MessageAccountId, ChildId?>> {
    val (starterRecipients, currentRecipients) = recipients.partition { it.isStarter() }

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

    val placementPredicate = { col: String ->
        if (filters?.placementTypes?.isNotEmpty() == true) {
            val placementTypesInCategory =
                PlacementType.fromMessagingCategories(filters.placementTypes)
            PredicateSql { where("pl.$col = ANY(${bind(placementTypesInCategory)})") }
        } else PredicateSql.alwaysTrue()
    }

    return createQuery {
            sql(
                """
WITH sender AS (
    SELECT type, daycare_group_id, employee_id FROM message_account WHERE id = ${bind(accountId)}
), current_children AS (
    SELECT DISTINCT pl.child_id
    FROM realized_placement_all(${bind(date)}) pl
    JOIN daycare d ON pl.unit_id = d.id
    LEFT JOIN person p ON p.id = pl.child_id
    LEFT JOIN service_need sn ON sn.placement_id = pl.placement_id AND daterange(sn.start_date, sn.end_date, '[]') @> ${bind(date)}
    JOIN sender ON TRUE
    WHERE (d.care_area_id = ANY(${bind(currentRecipients.areaIds())})
        OR pl.unit_id = ANY(${bind(currentRecipients.unitIds())})
        OR pl.group_id = ANY(${bind(currentRecipients.groupIds())})
        OR pl.child_id = ANY(${bind(currentRecipients.childIds())}))
    AND ${predicate(filterPredicates.and(placementPredicate("placement_type")))}
    AND (sender.type = 'MUNICIPAL'::message_account_type OR pl.group_id IS NOT NULL)
    AND (
        EXISTS (
            SELECT 1
            FROM child_daycare_acl(${bind(date)})
            JOIN mobile_device_daycare_acl_view USING (daycare_id)
            WHERE mobile_device_id = (SELECT sender.employee_id FROM sender)
                AND child_id = pl.child_id
        ) OR EXISTS (
            SELECT 1
            FROM employee_child_daycare_acl(${bind(date)})
            WHERE employee_id = (SELECT sender.employee_id FROM sender)
                AND child_id = pl.child_id
        ) OR EXISTS (
            SELECT 1
            FROM sender
            WHERE pl.group_id = sender.daycare_group_id
        ) OR EXISTS (
            SELECT 1
            FROM sender
            WHERE type = 'MUNICIPAL'
        )
    )
    AND 'MESSAGING' = ANY(d.enabled_pilot_features)
), starting_children AS (
    SELECT DISTINCT pl.child_id
    FROM placement pl
    JOIN daycare d ON pl.unit_id = d.id
    LEFT JOIN daycare_group_placement dgp ON pl.id = dgp.daycare_placement_id
    LEFT JOIN person p ON p.id = pl.child_id
    LEFT JOIN service_need sn ON false
    JOIN sender ON TRUE
    WHERE (pl.start_date > ${bind(date)} OR dgp.start_date > ${bind(date)})
        AND NOT EXISTS (
            SELECT
            FROM daycare_group_placement earlier_dgp
            JOIN placement earlier_p ON earlier_dgp.daycare_placement_id = earlier_p.id
            WHERE
                earlier_p.child_id = pl.child_id AND
                earlier_dgp.daycare_group_id = dgp.daycare_group_id AND
                earlier_dgp.end_date < dgp.start_date AND
                earlier_dgp.end_date >= ${bind(date)}
        )
        AND (d.care_area_id = ANY(${bind(starterRecipients.areaIds())})
            OR pl.unit_id = ANY(${bind(starterRecipients.unitIds())})
            OR dgp.daycare_group_id = ANY(${bind(starterRecipients.groupIds())})
            OR pl.child_id = ANY(${bind(starterRecipients.childIds())}))
    AND ${predicate(filterPredicates.and(placementPredicate("type")))}
    AND (sender.type = 'MUNICIPAL'::message_account_type OR dgp.daycare_group_id IS NOT NULL)
    AND (
        EXISTS (
            SELECT 1
            FROM daycare_acl_view acl
            WHERE acl.daycare_id = pl.unit_id AND acl.employee_id = (SELECT sender.employee_id FROM sender)
        ) OR EXISTS (
            SELECT 1
            FROM sender
            WHERE dgp.daycare_group_id = sender.daycare_group_id
        ) OR EXISTS (
            SELECT 1
            FROM sender
            WHERE type = 'MUNICIPAL'
        )
    )
    AND 'MESSAGING' = ANY(d.enabled_pilot_features)
), children AS (
    SELECT child_id FROM current_children
    UNION
    SELECT child_id FROM starting_children
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
WHERE p.id = ANY(${bind(currentRecipients.citizenIds())})
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
    recipients: Set<MessageRecipient>,
    date: LocalDate,
): Set<MessageAccountId> {
    val areaIds = recipients.areaIds()
    val unitIds = recipients.unitIds()
    val groupIds = recipients.groupIds()
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
    SELECT recipient_acc.id
    FROM groups g
    JOIN message_account recipient_acc ON recipient_acc.daycare_group_id = g.group_id

    UNION ALL

    -- unit supervisor and special education teacher accounts
    SELECT recipient_acc.id
    FROM units u
    JOIN daycare_acl unit_employee ON unit_employee.daycare_id = u.unit_id
      AND unit_employee.role = ANY('{UNIT_SUPERVISOR,SPECIAL_EDUCATION_TEACHER}'::user_role[])
    JOIN message_account recipient_acc ON recipient_acc.employee_id = unit_employee.employee_id
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
