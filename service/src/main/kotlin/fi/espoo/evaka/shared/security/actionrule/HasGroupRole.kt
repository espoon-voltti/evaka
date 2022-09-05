// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.VasuDocumentId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.QueryFragment
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControlDecision
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.shared.utils.emptyEnumSet
import fi.espoo.evaka.shared.utils.toEnumSet
import java.util.EnumSet
import java.util.UUID

private typealias GetGroupRoles = (user: AuthenticatedUser.Employee, now: HelsinkiDateTime) -> QueryFragment

data class HasGroupRole(val oneOf: EnumSet<UserRole>, val unitFeatures: Set<PilotFeature>) : ActionRuleParams<HasGroupRole> {
    init {
        oneOf.forEach { check(it.isUnitScopedRole()) { "Expected a unit-scoped role, got $it" } }
    }
    constructor(vararg oneOf: UserRole) : this(oneOf.toEnumSet(), emptyEnumSet())

    fun withUnitFeatures(vararg allOf: PilotFeature) = copy(unitFeatures = allOf.toEnumSet())

    private fun <T : Id<*>> rule(getGroupRoles: GetGroupRoles): DatabaseActionRule<T, HasGroupRole> =
        DatabaseActionRule.Simple(this, Query(getGroupRoles))
    private data class Query<T : Id<*>>(private val getGroupRoles: GetGroupRoles) : DatabaseActionRule.Query<T, HasGroupRole> {
        override fun executeWithTargets(
            ctx: DatabaseActionRule.QueryContext,
            targets: Set<T>
        ): Map<T, DatabaseActionRule.Deferred<HasGroupRole>> = when (ctx.user) {
            is AuthenticatedUser.Employee -> getGroupRoles(ctx.user, ctx.now).let { subquery ->
                ctx.tx.createQuery(
                    QueryFragment(
                        """
                    SELECT id, role, unit_features
                    FROM (${subquery.sql}) fragment
                    WHERE id = ANY(:ids)
                        """.trimIndent(),
                        subquery.bindings
                    )
                ).bind("ids", targets.map { it.raw })
                    .mapTo<IdRoleFeatures>()
            }
                .fold(targets.associateTo(linkedMapOf()) { (it to mutableSetOf<RoleAndFeatures>()) }) { acc, (target, result) ->
                    acc[target]?.plusAssign(result)
                    acc
                }
                .mapValues { (_, queryResult) -> Deferred(queryResult) }
            else -> emptyMap()
        }
        override fun executeWithParams(
            ctx: DatabaseActionRule.QueryContext,
            params: HasGroupRole
        ): AccessControlFilter<T>? = when (ctx.user) {
            is AuthenticatedUser.Employee -> getGroupRoles(ctx.user, ctx.now).let { subquery ->
                ctx.tx.createQuery(
                    QueryFragment(
                        """
                    SELECT id
                    FROM (${subquery.sql}) fragment
                    WHERE role = ANY(:roles)
                    AND unit_features @> :features
                        """.trimIndent(),
                        subquery.bindings
                    )
                )
                    .bind("roles", params.oneOf.toSet())
                    .bind("features", params.unitFeatures.toSet())
                    .mapTo<UUID>()
                    .map { Id<DatabaseTable>(it) }
                    .toSet()
                    .let { ids -> AccessControlFilter.Some(ids) }
            }
            else -> null
        }
    }
    private data class Deferred(private val queryResult: Set<RoleAndFeatures>) : DatabaseActionRule.Deferred<HasGroupRole> {
        override fun evaluate(params: HasGroupRole): AccessControlDecision =
            if (queryResult.any { params.oneOf.contains(it.role) && it.unitFeatures.containsAll(params.unitFeatures) }) {
                AccessControlDecision.Permitted(params)
            } else {
                AccessControlDecision.None
            }
    }

    fun inPlacementGroupOfChild() = rule<ChildId> { user, now ->
        QueryFragment(
            """
SELECT child_id AS id, role, enabled_pilot_features AS unit_features
FROM employee_child_group_acl(:today) acl
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
            """.trimIndent()
        )
            .bind("today", now.toLocalDate())
            .bind("userId", user.id)
    }

    fun inPlacementGroupOfChildOfVasuDocument() = rule<VasuDocumentId> { user, now ->
        QueryFragment(
            """
SELECT curriculum_document.id AS id, role, enabled_pilot_features AS unit_features
FROM curriculum_document
JOIN employee_child_group_acl(:today) acl USING (child_id)
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
            """.trimIndent()
        )
            .bind("today", now.toLocalDate())
            .bind("userId", user.id)
    }
}
