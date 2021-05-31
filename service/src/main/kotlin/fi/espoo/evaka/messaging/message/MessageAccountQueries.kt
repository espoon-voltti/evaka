// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.message

import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.kotlin.mapTo
import java.util.UUID

fun Database.Read.getDaycareGroupMessageAccount(daycareGroupId: UUID): UUID? {
    val sql = """
SELECT acc.id FROM message_account acc
WHERE acc.daycare_group_id = :daycareGroupId AND acc.active = true
"""
    return this.createQuery(sql)
        .bind("daycareGroupId", daycareGroupId)
        .mapTo<UUID>()
        .firstOrNull()
}

fun Database.Read.getCitizenMessageAccount(personId: UUID): UUID {
    val sql = """
SELECT acc.id FROM message_account acc
WHERE acc.person_id = :personId AND acc.active = true
"""
    return this.createQuery(sql)
        .bind("personId", personId)
        .mapTo<UUID>()
        .one()
}

fun Database.Read.getEmployeeMessageAccounts(employeeId: UUID): Set<UUID> {
    val sql = """
SELECT acc.id
FROM message_account acc
LEFT JOIN daycare_group dg ON acc.daycare_group_id = dg.id
    LEFT JOIN daycare dc ON dc.id = dg.daycare_id
    LEFT JOIN daycare_acl acl ON acl.daycare_id = dg.daycare_id
WHERE (acc.employee_id = :employeeId OR acl.employee_id = :employeeId)
    AND acc.active = true
"""
    return this.createQuery(sql)
        .bind("employeeId", employeeId)
        .mapTo<UUID>()
        .toSet()
}

fun Database.Read.getEmployeeDetailedMessageAccounts(employeeId: UUID): Set<DetailedMessageAccount> {
    val accountIds = getEmployeeMessageAccounts(employeeId)

    val sql = """
SELECT acc.id,
       name_view.account_name AS name,
       CASE
           WHEN dg.id IS NOT NULL THEN 'group'
           ELSE 'personal'
       END                    AS type,
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
WHERE acc.id = ANY(:accountIds)
GROUP BY acc.id, account_name, type, group_id, group_name, group_unitId, group_unitName
"""
    return this.createQuery(sql)
        .bind("accountIds", accountIds.toTypedArray())
        .mapTo<DetailedMessageAccount>()
        .toSet()
}

fun Database.Read.getAccountNames(accountIds: Set<UUID>): List<String> {
    val sql = """
        SELECT account_name
        FROM message_account_name_view
        WHERE id = ANY(:ids)
    """.trimIndent()

    return this.createQuery(sql)
        .bind("ids", accountIds.toTypedArray())
        .mapTo<String>()
        .list()
}

fun Database.Read.getAccountName(accountId: UUID): String =
    getAccountNames(setOf(accountId)).first()

fun Database.Transaction.createDaycareGroupMessageAccount(daycareGroupId: UUID): UUID {
    // language=SQL
    val sql = """
        INSERT INTO message_account (daycare_group_id) VALUES (:daycareGroupId)
        RETURNING id
    """.trimIndent()
    return createQuery(sql)
        .bind("daycareGroupId", daycareGroupId)
        .mapTo<UUID>()
        .one()
}

fun Database.Transaction.deleteDaycareGroupMessageAccount(daycareGroupId: UUID) {
    // language=SQL
    val sql = """
        DELETE FROM message_account WHERE daycare_group_id = :daycareGroupId
    """.trimIndent()
    createUpdate(sql)
        .bind("daycareGroupId", daycareGroupId)
        .execute()
}

fun Database.Transaction.deactivateDaycareGroupMessageAccount(daycareGroupId: UUID) {
    // language=SQL
    val sql = """
        UPDATE message_account acc
        SET daycare_group_id = NULL,
            active = false,
            -- The daycare group is going to be deleted, so save its name
            deleted_owner_name = (
                SELECT account_name FROM message_account_name_view n WHERE n.id = acc.id
            )
        WHERE daycare_group_id = :daycareGroupId
    """.trimIndent()
    createUpdate(sql)
        .bind("daycareGroupId", daycareGroupId)
        .execute()
}

fun Database.Transaction.createPersonMessageAccount(personId: UUID): UUID {
    // language=SQL
    val sql = """
        INSERT INTO message_account (person_id) VALUES (:personId)
        RETURNING id
    """.trimIndent()
    return createQuery(sql)
        .bind("personId", personId)
        .mapTo<UUID>()
        .one()
}

fun Database.Transaction.upsertEmployeeMessageAccount(employeeId: UUID): UUID {
    // language=SQL
    val sql = """
        INSERT INTO message_account (employee_id) VALUES (:employeeId)
        ON CONFLICT (employee_id) DO UPDATE SET active = true
        RETURNING id
    """.trimIndent()
    return createQuery(sql)
        .bind("employeeId", employeeId)
        .mapTo<UUID>()
        .one()
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
