// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.ApplicationNoteId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.MessageDraftId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.PairingId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControlDecision

private typealias FilterByEmployee<T> = (tx: Database.Read, user: AuthenticatedUser.Employee, now: HelsinkiDateTime, targets: Set<T>) -> Iterable<T>

object IsEmployee : ActionRuleParams<IsEmployee> {
    private fun <T> rule(filter: FilterByEmployee<T>): DatabaseActionRule<T, IsEmployee> =
        DatabaseActionRule.Simple(this, Query(filter))
    private data class Query<T>(private val filter: FilterByEmployee<T>) : DatabaseActionRule.Query<T, IsEmployee> {
        override fun executeWithTargets(
            tx: Database.Read,
            user: AuthenticatedUser,
            now: HelsinkiDateTime,
            targets: Set<T>
        ): Map<T, DatabaseActionRule.Deferred<IsEmployee>> = when (user) {
            is AuthenticatedUser.Employee -> filter(tx, user, now, targets).associateWith { Deferred }
            else -> emptyMap()
        }

        override fun executeWithParams(
            tx: Database.Read,
            user: AuthenticatedUser,
            now: HelsinkiDateTime,
            params: IsEmployee
        ): AccessControlFilter<T>? = TODO("unsupported for this rule type")
    }
    private object Deferred : DatabaseActionRule.Deferred<IsEmployee> {
        override fun evaluate(params: IsEmployee): AccessControlDecision = AccessControlDecision.Permitted(params)
    }

    fun self() = rule<EmployeeId> { _, user, _, ids -> ids.filter { it == user.id } }

    fun ownerOfMobileDevice() = rule<MobileDeviceId> { tx, user, _, ids ->
        tx.createQuery(
            """
SELECT id
FROM mobile_device
WHERE employee_id = :userId
AND id = ANY(:ids)
            """.trimIndent()
        )
            .bind("userId", user.id)
            .bind("ids", ids)
            .mapTo()
    }

    fun ownerOfPairing() = rule<PairingId> { tx, user, _, ids ->
        tx.createQuery(
            """
SELECT id
FROM pairing
WHERE employee_id = :userId
AND id = ANY(:ids)
            """.trimIndent()
        )
            .bind("userId", user.id)
            .bind("ids", ids)
            .mapTo()
    }

    fun authorOfApplicationNote() = rule<ApplicationNoteId> { tx, user, _, ids ->
        tx.createQuery("SELECT id FROM application_note WHERE created_by = :userId AND id = ANY(:ids)")
            .bind("userId", user.id)
            .bind("ids", ids)
            .mapTo()
    }

    fun hasPermissionForMessageDraft() = rule<MessageDraftId> { tx, user, _, ids ->
        tx.createQuery(
            """
SELECT draft.id
FROM message_draft draft
JOIN message_account_access_view access ON access.account_id = draft.account_id
WHERE
    draft.id = ANY(:ids) AND
    access.employee_id = :employeeId
            """.trimIndent()
        )
            .bind("employeeId", user.id)
            .bind("ids", ids)
            .mapTo()
    }

    fun hasPermissionForAttachmentThroughMessageContent() = rule<AttachmentId> { tx, user, _, ids ->
        tx.createQuery(
            """
SELECT att.id
FROM attachment att
JOIN message_content content ON att.message_content_id = content.id
JOIN message msg ON content.id = msg.content_id
JOIN message_recipients rec ON msg.id = rec.message_id
JOIN message_account_access_view access ON access.account_id = msg.sender_id OR access.account_id = rec.recipient_id
WHERE att.id = ANY(:ids) AND access.employee_id = :employeeId
            """.trimIndent()
        )
            .bind("employeeId", user.id)
            .bind("ids", ids)
            .mapTo()
    }

    fun hasPermissionForAttachmentThroughMessageDraft() = rule<AttachmentId> { tx, user, _, ids ->
        tx.createQuery(
            """
SELECT att.id
FROM attachment att
JOIN message_draft draft ON att.message_draft_id = draft.id
JOIN message_account_access_view access ON access.account_id = draft.account_id
WHERE att.id = ANY(:ids) AND access.employee_id = :employeeId
            """.trimIndent()
        )
            .bind("employeeId", user.id)
            .bind("ids", ids)
            .mapTo()
    }
}
