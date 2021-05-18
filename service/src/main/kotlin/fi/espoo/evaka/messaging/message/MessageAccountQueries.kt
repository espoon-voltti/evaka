// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.message

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.kotlin.mapTo
import java.util.UUID

fun Database.Read.getMessageAccountForEndUser(user: AuthenticatedUser): MessageAccount {
    // language=SQL
    val sql = """
SELECT acc.id, name_view.account_name AS name
FROM message_account acc
    JOIN message_account_name_view name_view ON name_view.id = acc.id
WHERE acc.person_id = :userId AND acc.active = true
    """.trimIndent()
    return this.createQuery(sql)
        .bind("userId", user.id)
        .mapTo<MessageAccount>()
        .one()
}

fun Database.Read.getMessageAccountsForEmployee(user: AuthenticatedUser): Set<MessageAccount> {
    // language=SQL
    val sql = """
SELECT acc.id, name_view.account_name AS name
FROM message_account acc
    JOIN message_account_name_view name_view ON name_view.id = acc.id
    LEFT JOIN daycare_group dg ON acc.daycare_group_id = dg.id
    LEFT JOIN daycare dc ON dc.id = dg.daycare_id
    LEFT JOIN daycare_acl acl ON acl.daycare_id = dg.daycare_id
WHERE (acc.employee_id = :userId OR acl.employee_id = :userId)
    AND acc.active = true
    """.trimIndent()

    return this.createQuery(sql)
        .bind("userId", user.id)
        .mapTo<MessageAccount>()
        .toSet()
}

fun Database.Read.getAuthorizedMessageAccountsForEmployee(user: AuthenticatedUser): Set<AuthorizedMessageAccount> {
    val employeeSql = """
SELECT acc.id,
       name_view.account_name AS name,
       CASE
           WHEN acc.daycare_group_id IS NOT NULL THEN FALSE
           ELSE TRUE
       END                    AS personal,
       dg.id                  AS group_id,
       dg.name                AS group_name,
       dc.id                  AS group_unitId,
       dc.name                AS group_unitName,
       COUNT(rec.id)          AS unreadCount
FROM message_account acc
    JOIN message_account_name_view name_view ON name_view.id = acc.id
    LEFT JOIN message_recipients rec ON acc.id = rec.recipient_id AND rec.read_at IS NULL
    LEFT JOIN daycare_group dg ON acc.daycare_group_id = dg.id
    LEFT JOIN daycare dc ON dc.id = dg.daycare_id
    LEFT JOIN daycare_acl acl ON acl.daycare_id = dg.daycare_id
WHERE acc.employee_id = :userId
   OR acl.employee_id = :userId
GROUP BY acc.id, account_name, personal, group_id, group_name, group_unitId, group_unitName
    """.trimIndent()

    return this.createQuery(employeeSql)
        .bind("userId", user.id)
        .mapTo<AuthorizedMessageAccount>()
        .toSet()
}

fun Database.Transaction.createMessageAccountForDaycareGroup(daycareGroupId: UUID) {
    // language=SQL
    val sql = """
        INSERT INTO message_account (daycare_group_id) VALUES (:daycareGroupId)
    """.trimIndent()
    createUpdate(sql)
        .bind("daycareGroupId", daycareGroupId)
        .execute()
}

fun Database.Transaction.deleteDaycareGroupMessageAccount(daycareGroupId: UUID) {
    // language=SQL
    val sql = """
        DELETE FROM message_account WHERE daycare_group_id =:daycareGroupId
    """.trimIndent()
    createUpdate(sql)
        .bind("daycareGroupId", daycareGroupId)
        .execute()
}

fun Database.Transaction.createMessageAccountForPerson(personId: UUID) {
    // language=SQL
    val sql = """
        INSERT INTO message_account (person_id) VALUES (:personId)
    """.trimIndent()
    createUpdate(sql)
        .bind("personId", personId)
        .execute()
}

fun Database.Transaction.upsertMessageAccountForEmployee(employeeId: UUID) {
    // language=SQL
    val sql = """
        INSERT INTO message_account (employee_id) VALUES (:employeeId)
        ON CONFLICT (employee_id) DO UPDATE SET active = true
    """.trimIndent()
    createUpdate(sql)
        .bind("employeeId", employeeId)
        .execute()
}

fun Database.Transaction.deactivateEmployeeMessageAccount(employeeId: UUID) {
    // language=SQL
    val sql = """
        UPDATE message_account SET active = false
        WHERE employee_id = :employeeId
    """.trimIndent()
    createUpdate(sql)
        .bind("employeeId", employeeId)
        .execute()
}

fun Database.Read.groupRecipientAccountsByGuardianship(accountIds: Set<UUID>): Set<Set<UUID>> {
    data class AccountToChild(val id: UUID, val childId: UUID? = null)

    val accountsWithCommonChildren = createQuery(
        """     
WITH person_accounts AS (
    SELECT id, person_id from message_account WHERE id = ANY(:accountIds) AND person_id IS NOT NULL
), 
common_guardians AS (
    SELECT g.child_id, ma.id
    FROM guardian g
    JOIN message_account ma on g.guardian_id = ma.person_id
    WHERE ma.person_id = ANY(SELECT person_id from person_accounts) AND EXISTS(
        SELECT 1
        FROM guardian g2
        WHERE g2.guardian_id = ANY(SELECT person_id from person_accounts)
          AND g2.guardian_id <> g.guardian_id
          AND g2.child_id = g.child_id
    )
)
SELECT * FROM common_guardians
        """.trimIndent()
    )
        .bind("accountIds", accountIds.toTypedArray())
        .mapTo<AccountToChild>()
        .list()

    // For each account that share children, create a recipient group
    val distinctSetsOfAccountsWithCommonChildren = accountsWithCommonChildren
        .groupBy { it.childId }
        .values
        .map { row -> row.map { it.id }.toSet() }
        .toSet()

    // all accounts that do not have common children get their own threads
    val singleRecipients = (accountIds - accountsWithCommonChildren.map { it.id }).map { setOf(it) }

    return distinctSetsOfAccountsWithCommonChildren + singleRecipients
}
