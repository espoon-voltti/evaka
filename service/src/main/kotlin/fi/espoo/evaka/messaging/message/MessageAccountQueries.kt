// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.message

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.kotlin.mapTo
import java.util.UUID

fun Database.Read.getMessageAccountsForUser(user: AuthenticatedUser): Set<MessageAccount> {
    // language=SQL
    val employeeSql = """
        WITH accounts AS (
            SELECT acc.id
            FROM daycare_acl acl
            JOIN daycare_group dg ON acl.daycare_id = dg.daycare_id
            JOIN message_account acc ON acc.daycare_group_id = dg.id
            WHERE acl.employee_id = :userId
            
            UNION
            
            SELECT id
            FROM message_account
            WHERE message_account.employee_id = :userId
        )
        SELECT accounts.id, name_view.account_name AS name
        FROM accounts
            JOIN message_account_name_view name_view ON name_view.id = accounts.id
    """.trimIndent()

    // language=SQL
    val citizenSql = """
        SELECT acc.id, name_view.account_name AS name
        FROM message_account acc
        JOIN message_account_name_view name_view ON name_view.id = acc.id
        WHERE acc.person_id = :userId
    """.trimIndent()

    return this.createQuery(if (user.isEndUser) citizenSql else employeeSql)
        .bind("userId", user.id)
        .mapTo<MessageAccount>()
        .toSet()
}

fun Database.Read.getEnrichedMessageAccountsForUser(user: AuthenticatedUser): Set<EnrichedMessageAccount> {
    // language=SQL
    val employeeSql = """
        WITH accounts AS (
            SELECT acc.id, dg.id as groupId, dg.name as groupName, dc.id as unitId, dc.name as unitName, false as personal
            FROM daycare_acl acl
            JOIN daycare_group dg ON acl.daycare_id = dg.daycare_id
            JOIN daycare dc ON dc.id = acl.daycare_id
            JOIN message_account acc ON acc.daycare_group_id = dg.id
            WHERE acl.employee_id = :userId
            
            UNION
            
            SELECT id, NULL as groupId, NULL as groupName, NULL as unitId, NULL as unitName, true as personal
            FROM message_account
            WHERE message_account.employee_id = :userId
            )
            
        SELECT accounts.id as accountId, name_view.account_name, groupId, groupName, unitId, unitName, personal
        FROM accounts
            JOIN message_account_name_view name_view ON name_view.id = accounts.id
    """.trimIndent()

    return this.createQuery(employeeSql)
            .bind("userId", user.id)
            .mapTo<EnrichedMessageAccount>()
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
