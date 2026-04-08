// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.security.actionrule

import evaka.core.daycare.CareType
import evaka.core.daycare.domain.ProviderType
import evaka.core.shared.AttachmentId
import evaka.core.shared.ChildId
import evaka.core.shared.DatabaseTable
import evaka.core.shared.DaycareId
import evaka.core.shared.Id
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.UserRole
import evaka.core.shared.db.QuerySql
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.security.AccessControlDecision
import evaka.core.shared.utils.toEnumSet
import java.util.EnumSet

private typealias Filter =
    QuerySql.Builder.(user: AuthenticatedUser.Employee, now: HelsinkiDateTime) -> QuerySql

data class HasGlobalRole(val oneOf: EnumSet<UserRole>) :
    StaticActionRule, DatabaseActionRule.Params {
    init {
        oneOf.forEach { check(it.isGlobalRole()) { "Expected a global role, got $it" } }
    }

    constructor(vararg oneOf: UserRole) : this(oneOf.toEnumSet())

    override fun evaluate(user: AuthenticatedUser): AccessControlDecision =
        if (
            user is AuthenticatedUser.Employee && user.globalRoles.any { this.oneOf.contains(it) }
        ) {
            AccessControlDecision.Permitted(this)
        } else {
            AccessControlDecision.None
        }

    private fun <T : Id<*>> rule(filter: Filter): DatabaseActionRule.Scoped<T, HasGlobalRole> =
        DatabaseActionRule.Scoped.Simple(this, Query(filter))

    data class Query<T : Id<*>>(private val filter: Filter) :
        DatabaseActionRule.Scoped.Query<T, HasGlobalRole> {
        override fun cacheKey(user: AuthenticatedUser, now: HelsinkiDateTime): Any =
            when (user) {
                is AuthenticatedUser.Employee -> QuerySql { filter(user, now) }
                else -> Pair(user, now)
            }

        override fun executeWithTargets(
            ctx: DatabaseActionRule.QueryContext,
            targets: Set<T>,
        ): Map<T, DatabaseActionRule.Deferred<HasGlobalRole>> =
            when (ctx.user) {
                is AuthenticatedUser.Employee -> {
                    val targetCheck = targets.idTargetPredicate()
                    ctx.tx
                        .createQuery {
                            sql(
                                """
                    SELECT id
                    FROM (${subquery { filter(ctx.user, ctx.now) } }) fragment
                    WHERE ${predicate(targetCheck.forTable("fragment"))}
                    """
                            )
                        }
                        .toSet<Id<DatabaseTable>>()
                        .let { matched ->
                            targets
                                .filter { matched.contains(it) }
                                .associateWith { Deferred(ctx.user.globalRoles) }
                        }
                }

                else -> {
                    emptyMap()
                }
            }

        override fun queryWithParams(
            ctx: DatabaseActionRule.QueryContext,
            params: HasGlobalRole,
        ): QuerySql? =
            when (ctx.user) {
                is AuthenticatedUser.Employee -> {
                    if (ctx.user.globalRoles.any { params.oneOf.contains(it) }) {
                        QuerySql { filter(ctx.user, ctx.now) }
                    } else {
                        null
                    }
                }

                else -> {
                    null
                }
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

    fun andAttachmentWasUploadedByAnyEmployee() =
        rule<AttachmentId> { _, _ ->
            sql(
                """
SELECT attachment.id
FROM attachment
JOIN evaka_user ON uploaded_by = evaka_user.id
WHERE evaka_user.type = 'EMPLOYEE'
            """
            )
        }

    fun andChildHasServiceVoucherPlacement() =
        rule<ChildId> { _, _ ->
            sql(
                """
SELECT p.child_id AS id
FROM placement p
JOIN daycare pd ON pd.id = p.unit_id
WHERE pd.provider_type = 'PRIVATE_SERVICE_VOUCHER'
            """
            )
        }

    fun andUnitProviderAndCareTypeEquals(
        providerTypes: Set<ProviderType>,
        careTypes: Set<CareType>,
    ) =
        rule<DaycareId> { _, _ ->
            sql(
                """
SELECT id
FROM daycare
WHERE provider_type = ANY(${bind(providerTypes)})
${
    if (careTypes.isNotEmpty()) "AND (${bind(careTypes)} && daycare.type)"
    else ""
}
            """
            )
        }
}
