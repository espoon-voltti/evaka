// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageContentId
import fi.espoo.evaka.shared.MessageDraftId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.forTable

fun Database.Read.getCitizenMessageAccount(personId: PersonId): MessageAccountId {
    return createQuery {
            sql(
                """
SELECT acc.id FROM message_account acc
WHERE acc.person_id = ${bind(personId)} AND acc.active = true
"""
            )
        }
        .exactlyOne<MessageAccountId>()
}

fun Database.Read.getEmployeeMessageAccountIds(
    idFilter: AccessControlFilter<MessageAccountId>
): Set<MessageAccountId> {
    return createQuery {
            sql(
                """
SELECT id
FROM message_account
WHERE (${predicate(idFilter.forTable("message_account"))})
        """
            )
        }
        .toSet<MessageAccountId>()
}

fun Database.Read.getAuthorizedMessageAccountsForEmployee(
    idFilter: AccessControlFilter<MessageAccountId>,
    municipalAccountName: String,
    serviceWorkerAccountName: String
): List<AuthorizedMessageAccount> {
    return createQuery {
            sql(
                """
SELECT DISTINCT ON (acc.id)
    acc.id AS account_id,
    (CASE
        WHEN acc.type = 'MUNICIPAL'::message_account_type THEN ${bind(municipalAccountName)}
        WHEN acc.type = 'SERVICE_WORKER'::message_account_type THEN ${bind(serviceWorkerAccountName)}
        ELSE name_view.name
    END) AS account_name,
    acc.type AS account_type,
    dg.id AS group_id,
    dg.name AS group_name,
    dc.id AS group_unitId,
    dc.name AS group_unitName
FROM message_account acc
    JOIN message_account_view name_view ON name_view.id = acc.id
    LEFT JOIN daycare_group dg ON acc.daycare_group_id = dg.id
    LEFT JOIN daycare dc ON dc.id = dg.daycare_id
    LEFT JOIN daycare_acl acl ON acc.employee_id = acl.employee_id AND (acl.role = 'UNIT_SUPERVISOR' OR acl.role = 'SPECIAL_EDUCATION_TEACHER' OR acl.role = 'EARLY_CHILDHOOD_EDUCATION_SECRETARY')
    LEFT JOIN daycare supervisor_dc ON supervisor_dc.id = acl.daycare_id
WHERE ${predicate(idFilter.forTable("acc"))}
AND (
    'MESSAGING' = ANY(dc.enabled_pilot_features)
    OR 'MESSAGING' = ANY(supervisor_dc.enabled_pilot_features)
    OR acc.type = 'MUNICIPAL'
    OR acc.type = 'SERVICE_WORKER'
)
"""
            )
        }
        .toList()
}

fun Database.Read.getAccountNames(
    accountIds: Set<MessageAccountId>,
    serviceWorkerAccountName: String
): List<String> {

    return createQuery {
            sql(
                """
SELECT CASE mav.type WHEN 'SERVICE_WORKER' THEN ${bind(serviceWorkerAccountName)} ELSE mav.name END as name
FROM message_account_view mav
WHERE mav.id = ANY(${bind(accountIds)})
"""
            )
        }
        .toList<String>()
}

fun Database.Transaction.createMunicipalMessageAccount(): MessageAccountId {
    return createUpdate {
            sql("INSERT INTO message_account (type) VALUES (${bind(AccountType.MUNICIPAL)})")
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()
}

fun Database.Transaction.createDaycareGroupMessageAccount(
    daycareGroupId: GroupId
): MessageAccountId {
    return createQuery {
            sql(
                """
INSERT INTO message_account (daycare_group_id, type) VALUES (${bind(daycareGroupId)}, 'GROUP')
RETURNING id
"""
            )
        }
        .exactlyOne<MessageAccountId>()
}

fun Database.Transaction.deleteDaycareGroupMessageAccount(daycareGroupId: GroupId) {
    createUpdate {
            sql(
                """
DELETE FROM message_account WHERE daycare_group_id = ${bind(daycareGroupId)}
"""
            )
        }
        .execute()
}

fun Database.Transaction.createPersonMessageAccount(personId: PersonId): MessageAccountId {
    return createQuery {
            sql(
                """
INSERT INTO message_account (person_id, type) VALUES (${bind(personId)}, 'CITIZEN')
RETURNING id
"""
            )
        }
        .exactlyOne<MessageAccountId>()
}

fun Database.Transaction.upsertEmployeeMessageAccount(
    employeeId: EmployeeId,
    accountType: AccountType = AccountType.PERSONAL
): MessageAccountId {
    return createQuery {
            sql(
                """
INSERT INTO message_account (employee_id, type) VALUES (${bind(employeeId)}, ${bind(accountType)})
ON CONFLICT (employee_id) WHERE employee_id IS NOT NULL DO UPDATE SET active = true
RETURNING id
"""
            )
        }
        .exactlyOne<MessageAccountId>()
}

fun Database.Transaction.deactivateEmployeeMessageAccount(employeeId: EmployeeId) {
    createUpdate {
            sql(
                """
UPDATE message_account SET active = false
WHERE employee_id = ${bind(employeeId)}
"""
            )
        }
        .execute()
}

fun Database.Read.getMessageAccountType(accountId: MessageAccountId): AccountType {
    return createQuery { sql("SELECT type FROM message_account WHERE id = ${bind(accountId)}") }
        .exactlyOne<AccountType>()
}

fun Database.Read.findMessageAccountIdByDraftId(id: MessageDraftId): MessageAccountId? =
    createQuery { sql("SELECT account_id FROM message_draft WHERE id = ${bind(id)}") }
        .exactlyOneOrNull<MessageAccountId>()

fun Database.Read.getMessageAccountIdsByContentId(id: MessageContentId): List<MessageAccountId> =
    createQuery {
            sql(
                """
SELECT msg.sender_id
FROM message_content content
JOIN message msg ON content.id = msg.content_id
WHERE content.id = ${bind(id)}
UNION
SELECT rec.recipient_id
FROM message_content content
JOIN message msg ON content.id = msg.content_id
JOIN message_recipients rec ON msg.id = rec.message_id
WHERE content.id = ${bind(id)}
"""
            )
        }
        .toList<MessageAccountId>()

fun Database.Read.getServiceWorkerAccountId(): MessageAccountId? =
    createQuery { sql("SELECT id FROM message_account WHERE type = 'SERVICE_WORKER'") }
        .exactlyOneOrNull<MessageAccountId>()
