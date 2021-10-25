// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.kotlin.mapTo
import java.util.UUID

fun Database.Read.getDaycareGroupMessageAccount(daycareGroupId: GroupId): MessageAccountId {
    val sql = """
SELECT acc.id FROM message_account acc
WHERE acc.daycare_group_id = :daycareGroupId AND acc.active = true
"""
    return this.createQuery(sql)
        .bind("daycareGroupId", daycareGroupId)
        .mapTo<MessageAccountId>()
        .one()
}

fun Database.Read.getCitizenMessageAccount(personId: UUID): MessageAccountId {
    val sql = """
SELECT acc.id FROM message_account acc
WHERE acc.person_id = :personId AND acc.active = true
"""
    return this.createQuery(sql)
        .bind("personId", personId)
        .mapTo<MessageAccountId>()
        .one()
}

fun Database.Read.getEmployeeMessageAccountIds(employeeId: UUID): Set<MessageAccountId> {
    return this.createQuery("SELECT account_id FROM message_account_access_view WHERE employee_id = :employeeId")
        .bind("employeeId", employeeId)
        .mapTo<MessageAccountId>()
        .toSet()
}

fun Database.Read.getEmployeeNestedMessageAccounts(employeeId: UUID): Set<NestedMessageAccount> {
    val accountIds = getEmployeeMessageAccountIds(employeeId)

    val sql = """
SELECT acc.id AS account_id,
       name_view.account_name AS account_name,
       acc.type               AS account_type,
       dg.id                  AS group_id,
       dg.name                AS group_name,
       dc.id                  AS group_unitId,
       dc.name                AS group_unitName
FROM message_account acc
    JOIN message_account_name_view name_view ON name_view.id = acc.id
    LEFT JOIN daycare_group dg ON acc.daycare_group_id = dg.id
    LEFT JOIN daycare dc ON dc.id = dg.daycare_id
    LEFT JOIN daycare_acl acl ON acc.employee_id = acl.employee_id AND acl.role = 'UNIT_SUPERVISOR'
    LEFT JOIN daycare supervisor_dc ON supervisor_dc.id = acl.daycare_id
WHERE acc.id = ANY(:accountIds)
AND (
    'MESSAGING' = ANY(dc.enabled_pilot_features)
    OR 'MESSAGING' = ANY(supervisor_dc.enabled_pilot_features)
)
"""
    return this.createQuery(sql)
        .bind("accountIds", accountIds.toTypedArray())
        .mapTo<NestedMessageAccount>()
        .toSet()
}

fun Database.Read.getAccountNames(accountIds: Set<MessageAccountId>): List<String> {
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

fun Database.Transaction.createDaycareGroupMessageAccount(daycareGroupId: GroupId): MessageAccountId {
    // language=SQL
    val sql = """
        INSERT INTO message_account (daycare_group_id) VALUES (:daycareGroupId)
        RETURNING id
    """.trimIndent()
    return createQuery(sql)
        .bind("daycareGroupId", daycareGroupId)
        .mapTo<MessageAccountId>()
        .one()
}

fun Database.Transaction.deleteDaycareGroupMessageAccount(daycareGroupId: GroupId) {
    // language=SQL
    val sql = """
        DELETE FROM message_account WHERE daycare_group_id = :daycareGroupId
    """.trimIndent()
    createUpdate(sql)
        .bind("daycareGroupId", daycareGroupId)
        .execute()
}

fun Database.Transaction.createPersonMessageAccount(personId: UUID): MessageAccountId {
    // language=SQL
    val sql = """
        INSERT INTO message_account (person_id) VALUES (:personId)
        RETURNING id
    """.trimIndent()
    return createQuery(sql)
        .bind("personId", personId)
        .mapTo<MessageAccountId>()
        .one()
}

fun Database.Transaction.upsertEmployeeMessageAccount(employeeId: UUID): MessageAccountId {
    // language=SQL
    val sql = """
        INSERT INTO message_account (employee_id) VALUES (:employeeId)
        ON CONFLICT (employee_id) DO UPDATE SET active = true
        RETURNING id
    """.trimIndent()
    return createQuery(sql)
        .bind("employeeId", employeeId)
        .mapTo<MessageAccountId>()
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

fun Database.Read.groupRecipientAccountsByGuardianship(accountIds: Set<MessageAccountId>): Set<Set<MessageAccountId>> {
    data class AccountToChild(val id: MessageAccountId, val childId: UUID? = null)

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
