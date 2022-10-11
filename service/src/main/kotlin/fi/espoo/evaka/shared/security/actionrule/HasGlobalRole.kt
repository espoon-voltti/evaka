// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.QueryBuilder
import fi.espoo.evaka.shared.db.QueryFunction
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControlDecision
import fi.espoo.evaka.shared.utils.toEnumSet
import java.util.EnumSet

private typealias Filter<T> = QueryBuilder<T>.(user: AuthenticatedUser.Employee, now: HelsinkiDateTime) -> QueryBuilder.Sql

data class HasGlobalRole(val oneOf: EnumSet<UserRole>) : StaticActionRule {
    init {
        oneOf.forEach { check(it.isGlobalRole()) { "Expected a global role, got $it" } }
    }
    constructor(vararg oneOf: UserRole) : this(oneOf.toEnumSet())

    override fun evaluate(user: AuthenticatedUser): AccessControlDecision =
        if (user is AuthenticatedUser.Employee && user.globalRoles.any { this.oneOf.contains(it) }) {
            AccessControlDecision.Permitted(this)
        } else {
            AccessControlDecision.None
        }

    private fun <T : Id<*>> rule(filter: Filter<T>): DatabaseActionRule.Scoped<T, HasGlobalRole> =
        DatabaseActionRule.Scoped.Simple(this, Query(filter))
    data class Query<T : Id<*>>(private val filter: Filter<T>) : DatabaseActionRule.Scoped.Query<T, HasGlobalRole> {
        override fun executeWithTargets(
            ctx: DatabaseActionRule.QueryContext,
            targets: Set<T>
        ): Map<T, DatabaseActionRule.Deferred<HasGlobalRole>> = when (ctx.user) {
            is AuthenticatedUser.Employee -> ctx.tx.createQuery<T> {
                sql(
                    """
                    SELECT id
                    FROM (${subquery { filter(ctx.user, ctx.now) }}) fragment
                    WHERE id = ANY(${bind(targets.map {it.raw })})
                    """.trimIndent()
                )
            }
                .mapTo<Id<DatabaseTable>>()
                .toSet()
                .let { matched ->
                    targets.filter { matched.contains(it) }.associateWith { Deferred(ctx.user.globalRoles) }
                }
            else -> emptyMap()
        }

        override fun filterForParams(
            ctx: DatabaseActionRule.QueryContext,
            params: HasGlobalRole
        ): QueryFunction<T>? = when (ctx.user) {
            is AuthenticatedUser.Employee -> if (ctx.user.globalRoles.any { params.oneOf.contains(it) }) {
                { filter(ctx.user, ctx.now) }
            } else {
                null
            }
            else -> null
        }
    }
    private class Deferred(private val globalRoles: Set<UserRole>) : DatabaseActionRule.Deferred<HasGlobalRole> {
        override fun evaluate(params: HasGlobalRole): AccessControlDecision = if (globalRoles.any { params.oneOf.contains(it) }) {
            AccessControlDecision.Permitted(params)
        } else {
            AccessControlDecision.None
        }
    }

    fun andAttachmentWasUploadedByAnyEmployee() = rule<AttachmentId> { _, _ ->
        sql(
            """
SELECT attachment.id
FROM attachment
JOIN evaka_user ON uploaded_by = evaka_user.id
WHERE evaka_user.type = 'EMPLOYEE'
            """.trimIndent()
        )
    }

    fun andIsDecisionMakerForAssistanceNeedDecision() = rule<AssistanceNeedDecisionId> { employee, _ ->
        sql(
            """
SELECT id
FROM assistance_need_decision
WHERE decision_maker_employee_id = ${bind(employee.id)}
AND sent_for_decision IS NOT NULL
            """.trimIndent()
        )
    }

    fun andAssistanceNeedDecisionHasBeenSent() = rule<AssistanceNeedDecisionId> { _, _ ->
        sql(
            """
SELECT id
FROM assistance_need_decision
WHERE sent_for_decision IS NOT NULL
            """.trimIndent()
        )
    }

    fun andChildHasServiceVoucherPlacement() = rule<ChildId> { _, _ ->
        sql(
            """
SELECT p.child_id AS id
FROM placement p
JOIN daycare pd ON pd.id = p.unit_id
WHERE pd.provider_type = 'PRIVATE_SERVICE_VOUCHER'
            """.trimIndent()
        )
    }
}
