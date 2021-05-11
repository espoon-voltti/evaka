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
