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
import fi.espoo.evaka.shared.db.QueryFragment
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControlDecision
import fi.espoo.evaka.shared.utils.toEnumSet
import java.util.EnumSet

private typealias Filter<T> =
    (user: AuthenticatedUser.Employee, now: HelsinkiDateTime) -> QueryFragment<T>

data class HasGlobalRole(val oneOf: EnumSet<UserRole>) : StaticActionRule {
    init {
        oneOf.forEach { check(it.isGlobalRole()) { "Expected a global role, got $it" } }
    }
    constructor(vararg oneOf: UserRole) : this(oneOf.toEnumSet())

    override fun evaluate(user: AuthenticatedUser): AccessControlDecision =
        if (
            user is AuthenticatedUser.Employee && user.globalRoles.any { this.oneOf.contains(it) }
        ) {
            AccessControlDecision.Permitted(this)
        } else AccessControlDecision.None

    private fun <T : Id<*>> rule(filter: Filter<T>): DatabaseActionRule.Scoped<T, HasGlobalRole> =
        DatabaseActionRule.Scoped.Simple(this, Query(filter))
    data class Query<T : Id<*>>(private val filter: Filter<T>) :
        DatabaseActionRule.Scoped.Query<T, HasGlobalRole> {
        override fun executeWithTargets(
            ctx: DatabaseActionRule.QueryContext,
            targets: Set<T>
        ): Map<T, DatabaseActionRule.Deferred<HasGlobalRole>> =
            when (ctx.user) {
                is AuthenticatedUser.Employee ->
                    filter(ctx.user, ctx.now)
                        .let { subquery ->
                            ctx.tx
                                .createQuery(
                                    QueryFragment<Any>(
                                        """
                    SELECT id
                    FROM (${subquery.sql}) fragment
                    WHERE id = ANY(:ids)
                        """.trimIndent(
                                        ),
                                        subquery.bindings
                                    )
                                )
                                .bind("ids", targets.map { it.raw })
                                .mapTo<Id<DatabaseTable>>()
                                .toSet()
                        }
                        .let { matched ->
                            targets
                                .filter { matched.contains(it) }
                                .associateWith { Deferred(ctx.user.globalRoles) }
                        }
                else -> emptyMap()
            }

        override fun executeWithParams(
            ctx: DatabaseActionRule.QueryContext,
            params: HasGlobalRole
        ): AccessControlFilter<T>? =
            when (ctx.user) {
                is AuthenticatedUser.Employee ->
                    if (ctx.user.globalRoles.any { params.oneOf.contains(it) }) {
                        filter(ctx.user, ctx.now).let { subquery ->
                            ctx.tx.createQuery(subquery).mapTo<Id<DatabaseTable>>().toSet().let {
                                ids ->
                                AccessControlFilter.Some(ids)
                            }
                        }
                    } else null
                else -> null
            }
    }
    private class Deferred(private val globalRoles: Set<UserRole>) :
        DatabaseActionRule.Deferred<HasGlobalRole> {
        override fun evaluate(params: HasGlobalRole): AccessControlDecision =
            if (globalRoles.any { params.oneOf.contains(it) }) {
                AccessControlDecision.Permitted(params)
            } else {
                AccessControlDecision.None
            }
    }

    fun andAttachmentWasUploadedByAnyEmployee() = rule { _, _ ->
        QueryFragment<AttachmentId>(
            """
SELECT attachment.id
FROM attachment
JOIN evaka_user ON uploaded_by = evaka_user.id
WHERE evaka_user.type = 'EMPLOYEE'
            """.trimIndent(
            )
        )
    }

    fun andIsDecisionMakerForAssistanceNeedDecision() = rule { employee, _ ->
        QueryFragment<AssistanceNeedDecisionId>(
                """
SELECT id
FROM assistance_need_decision
WHERE decision_maker_employee_id = :employeeId
AND sent_for_decision IS NOT NULL
            """.trimIndent(
                )
            )
            .bind("employeeId", employee.id)
    }

    fun andAssistanceNeedDecisionHasBeenSent() = rule { _, _ ->
        QueryFragment<AssistanceNeedDecisionId>(
            """
SELECT id
FROM assistance_need_decision
WHERE sent_for_decision IS NOT NULL
            """.trimIndent(
            )
        )
    }

    fun andChildHasServiceVoucherPlacement() = rule { _, _ ->
        QueryFragment<ChildId>(
            """
SELECT p.child_id AS id
FROM placement p
JOIN daycare pd ON pd.id = p.unit_id
WHERE pd.provider_type = 'PRIVATE_SERVICE_VOUCHER'
            """.trimIndent(
            )
        )
    }
}
