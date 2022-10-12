// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.ApplicationNoteId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.MessageDraftId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.PairingId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.QueryFragment
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControlDecision

private typealias FilterByEmployee<T> = (user: AuthenticatedUser.Employee, now: HelsinkiDateTime) -> QueryFragment<T>

object IsEmployee {
    private fun <T : Id<*>> rule(filter: FilterByEmployee<T>): DatabaseActionRule.Scoped<T, IsEmployee> =
        DatabaseActionRule.Scoped.Simple(this, Query(filter))
    private data class Query<T : Id<*>>(private val filter: FilterByEmployee<T>) : DatabaseActionRule.Scoped.Query<T, IsEmployee> {
        override fun executeWithTargets(
            ctx: DatabaseActionRule.QueryContext,
            targets: Set<T>
        ): Map<T, DatabaseActionRule.Deferred<IsEmployee>> = when (ctx.user) {
            is AuthenticatedUser.Employee -> filter(ctx.user, ctx.now).let { subquery ->
                ctx.tx.createQuery(
                    QueryFragment<Any>(
                        """
                    SELECT id
                    FROM (${subquery.sql}) fragment
                    WHERE id = ANY(:ids)
                        """.trimIndent(),
                        subquery.bindings
                    )
                ).bind("ids", targets.map { it.raw })
                    .mapTo<Id<DatabaseTable>>()
                    .toSet()
            }.let { matched ->
                targets.filter { matched.contains(it) }.associateWith { Permitted }
            }
            else -> emptyMap()
        }

        override fun executeWithParams(
            ctx: DatabaseActionRule.QueryContext,
            params: IsEmployee
        ): AccessControlFilter<T>? = when (ctx.user) {
            is AuthenticatedUser.Employee -> filter(ctx.user, ctx.now).let { subquery ->
                ctx.tx.createQuery(subquery)
                    .mapTo<Id<DatabaseTable>>()
                    .toSet()
                    .let { ids -> AccessControlFilter.Some(ids) }
            }
            else -> null
        }
    }
    private object Permitted : DatabaseActionRule.Deferred<IsEmployee> {
        override fun evaluate(params: IsEmployee): AccessControlDecision = AccessControlDecision.Permitted(params)
    }

    fun any() = object : StaticActionRule {
        override fun evaluate(user: AuthenticatedUser): AccessControlDecision = when (user) {
            is AuthenticatedUser.Employee -> AccessControlDecision.Permitted(this)
            else -> AccessControlDecision.None
        }
    }

    fun self() = object : DatabaseActionRule.Scoped<EmployeeId, IsEmployee> {
        override val params = IsEmployee
        override val query = object : DatabaseActionRule.Scoped.Query<EmployeeId, IsEmployee> {
            override fun executeWithTargets(
                ctx: DatabaseActionRule.QueryContext,
                targets: Set<EmployeeId>
            ): Map<EmployeeId, DatabaseActionRule.Deferred<IsEmployee>> = when (ctx.user) {
                is AuthenticatedUser.Employee -> targets.filter { it == ctx.user.id }
                    .associateWith { Permitted }
                else -> emptyMap()
            }

            override fun executeWithParams(
                ctx: DatabaseActionRule.QueryContext,
                params: IsEmployee
            ): AccessControlFilter<EmployeeId>? = when (ctx.user) {
                is AuthenticatedUser.Employee -> AccessControlFilter.Some(setOf(ctx.user.id))
                else -> null
            }

            override fun equals(other: Any?): Boolean = other?.javaClass == this.javaClass
            override fun hashCode(): Int = this.javaClass.hashCode()
        }
    }

    fun ownerOfAnyMobileDevice() =
        DatabaseActionRule.Unscoped(
            this,
            object : DatabaseActionRule.Unscoped.Query<IsEmployee> {
                override fun execute(ctx: DatabaseActionRule.QueryContext): DatabaseActionRule.Deferred<IsEmployee>? =
                    when (ctx.user) {
                        is AuthenticatedUser.Employee -> ctx.tx.createQuery(
                            """
SELECT EXISTS (
    SELECT 1
    FROM mobile_device
    WHERE employee_id = :userId
)
                            """.trimIndent()
                        )
                            .bind("userId", ctx.user.id)
                            .mapTo<Boolean>()
                            .single()
                            .let { isPermitted -> if (isPermitted) Permitted else null }
                        else -> null
                    }

                override fun hashCode(): Int = this.javaClass.hashCode()
                override fun equals(other: Any?): Boolean = other?.javaClass == this.javaClass
            }
        )

    fun ownerOfMobileDevice() = rule { user, _ ->
        QueryFragment<MobileDeviceId>(
            """
SELECT id
FROM mobile_device
WHERE employee_id = :userId
            """.trimIndent()
        )
            .bind("userId", user.id)
    }

    fun ownerOfPairing() = rule { user, _ ->
        QueryFragment<PairingId>(
            """
SELECT id
FROM pairing
WHERE employee_id = :userId
            """.trimIndent()
        )
            .bind("userId", user.id)
    }

    fun authorOfApplicationNote() = rule { user, _ ->
        QueryFragment<ApplicationNoteId>(
            """
SELECT id
FROM application_note
WHERE created_by = :userId
            """.trimIndent()
        )
            .bind("userId", user.id)
    }

    fun hasPermissionForMessageDraft() = rule { user, _ ->
        QueryFragment<MessageDraftId>(
            """
SELECT draft.id
FROM message_draft draft
JOIN message_account_access_view access ON access.account_id = draft.account_id
WHERE access.employee_id = :employeeId
            """.trimIndent()
        )
            .bind("employeeId", user.id)
    }

    fun hasPermissionForAttachmentThroughMessageContent() = rule { user, _ ->
        QueryFragment<AttachmentId>(
            """
SELECT att.id
FROM attachment att
JOIN message_content content ON att.message_content_id = content.id
JOIN message msg ON content.id = msg.content_id
JOIN message_recipients rec ON msg.id = rec.message_id
JOIN message_account_access_view access ON access.account_id = msg.sender_id OR access.account_id = rec.recipient_id
WHERE access.employee_id = :employeeId
            """.trimIndent()
        )
            .bind("employeeId", user.id)
    }

    fun hasPermissionForAttachmentThroughMessageDraft() = rule { user, _ ->
        QueryFragment<AttachmentId>(
            """
SELECT att.id
FROM attachment att
JOIN message_draft draft ON att.message_draft_id = draft.id
JOIN message_account_access_view access ON access.account_id = draft.account_id
WHERE access.employee_id = :employeeId
            """.trimIndent()
        )
            .bind("employeeId", user.id)
    }
}
