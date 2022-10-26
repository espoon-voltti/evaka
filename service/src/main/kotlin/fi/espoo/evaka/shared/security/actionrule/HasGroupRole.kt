// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.VasuDocumentId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.QuerySql
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControlDecision
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.shared.utils.emptyEnumSet
import fi.espoo.evaka.shared.utils.toEnumSet
import java.util.EnumSet

private typealias GetGroupRoles =
    QuerySql.Builder<IdRoleFeatures>.(
        user: AuthenticatedUser.Employee, now: HelsinkiDateTime
    ) -> QuerySql<IdRoleFeatures>

data class HasGroupRole(val oneOf: EnumSet<UserRole>, val unitFeatures: Set<PilotFeature>) :
    DatabaseActionRule.Params {
    init {
        oneOf.forEach { check(it.isUnitScopedRole()) { "Expected a unit-scoped role, got $it" } }
    }
    constructor(vararg oneOf: UserRole) : this(oneOf.toEnumSet(), emptyEnumSet())

    fun withUnitFeatures(vararg allOf: PilotFeature) = copy(unitFeatures = allOf.toEnumSet())

    override fun isPermittedForSomeTarget(ctx: DatabaseActionRule.QueryContext): Boolean =
        when (ctx.user) {
            is AuthenticatedUser.Employee ->
                ctx.tx
                    .createQuery<Boolean> {
                        sql(
                            """
SELECT EXISTS (
    SELECT 1
    FROM daycare
    JOIN daycare_acl acl ON daycare.id = acl.daycare_id
    JOIN daycare_group ON daycare.id = daycare_group.daycare_id
    JOIN daycare_group_acl group_acl ON daycare_group.id = group_acl.daycare_group_id
    JOIN evaka_service.public.daycare_group_acl
    WHERE acl.employee_id = ${bind(ctx.user.id)}
    AND group_acl.employee_id = ${bind(ctx.user.id)}
    AND role = ANY(${bind(oneOf.toSet())})
    AND daycare.enabled_pilot_features @> ${bind(unitFeatures.toSet())}
)
                """
                                .trimIndent()
                        )
                    }
                    .mapTo<Boolean>()
                    .single()
            else -> false
        }

    private fun <T : Id<*>> rule(
        getGroupRoles: GetGroupRoles
    ): DatabaseActionRule.Scoped<T, HasGroupRole> =
        DatabaseActionRule.Scoped.Simple(this, Query(getGroupRoles))
    private data class Query<T : Id<*>>(private val getGroupRoles: GetGroupRoles) :
        DatabaseActionRule.Scoped.Query<T, HasGroupRole> {
        override fun executeWithTargets(
            ctx: DatabaseActionRule.QueryContext,
            targets: Set<T>
        ): Map<T, DatabaseActionRule.Deferred<HasGroupRole>> =
            when (ctx.user) {
                is AuthenticatedUser.Employee ->
                    ctx.tx
                        .createQuery<T> {
                            sql(
                                """
                    SELECT id, role, unit_features
                    FROM (${subquery { getGroupRoles(ctx.user, ctx.now) } }) fragment
                    WHERE id = ANY(${bind(targets.map { it.raw })})
                    """
                                    .trimIndent()
                            )
                        }
                        .mapTo<IdRoleFeatures>()
                        .fold(
                            targets.associateTo(linkedMapOf()) {
                                (it to mutableSetOf<RoleAndFeatures>())
                            }
                        ) { acc, (target, result) ->
                            acc[target]?.plusAssign(result)
                            acc
                        }
                        .mapValues { (_, queryResult) -> Deferred(queryResult) }
                else -> emptyMap()
            }
        override fun queryWithParams(
            ctx: DatabaseActionRule.QueryContext,
            params: HasGroupRole
        ): QuerySql<T>? =
            when (ctx.user) {
                is AuthenticatedUser.Employee ->
                    QuerySql.of {
                        sql(
                            """
                    SELECT id
                    FROM (${subquery { getGroupRoles(ctx.user, ctx.now) } }) fragment
                    WHERE role = ANY(${bind(params.oneOf.toSet())})
                    AND unit_features @> ${bind(params.unitFeatures.toSet())}
                        """
                                .trimIndent()
                        )
                    }
                else -> null
            }
    }
    private data class Deferred(private val queryResult: Set<RoleAndFeatures>) :
        DatabaseActionRule.Deferred<HasGroupRole> {
        override fun evaluate(params: HasGroupRole): AccessControlDecision =
            if (
                queryResult.any {
                    params.oneOf.contains(it.role) &&
                        it.unitFeatures.containsAll(params.unitFeatures)
                }
            ) {
                AccessControlDecision.Permitted(params)
            } else {
                AccessControlDecision.None
            }
    }

    fun inPlacementGroupOfChild() =
        rule<ChildId> { user, now ->
            sql(
                """
SELECT child_id AS id, role, enabled_pilot_features AS unit_features
FROM employee_child_group_acl(${bind(now.toLocalDate())}) acl
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = ${bind(user.id)}
            """
                    .trimIndent()
            )
        }

    fun inPlacementGroupOfChildOfVasuDocument() =
        rule<VasuDocumentId> { user, now ->
            sql(
                """
SELECT curriculum_document.id AS id, role, enabled_pilot_features AS unit_features
FROM curriculum_document
JOIN employee_child_group_acl(${bind(now.toLocalDate())}) acl USING (child_id)
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = ${bind(user.id)}
            """
                    .trimIndent()
            )
        }
}
