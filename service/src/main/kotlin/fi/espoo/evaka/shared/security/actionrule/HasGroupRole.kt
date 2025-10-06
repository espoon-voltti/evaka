// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.document.childdocument.DocumentStatus
import fi.espoo.evaka.shared.AbsenceApplicationId
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.Id
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
    QuerySql.Builder.(user: AuthenticatedUser.Employee, now: HelsinkiDateTime) -> QuerySql

data class HasGroupRole(
    val oneOf: EnumSet<UserRole>,
    val unitFeatures: Set<PilotFeature>,
    val unitProviderTypes: EnumSet<ProviderType>?,
) : DatabaseActionRule.Params {
    init {
        oneOf.forEach { check(it.isUnitScopedRole()) { "Expected a unit-scoped role, got $it" } }
    }

    constructor(vararg oneOf: UserRole) : this(oneOf.toEnumSet(), emptyEnumSet(), null)

    fun withUnitFeatures(vararg allOf: PilotFeature) = copy(unitFeatures = allOf.toEnumSet())

    fun withUnitProviderTypes(vararg allOf: ProviderType) =
        copy(unitProviderTypes = allOf.toEnumSet())

    private fun <T : Id<*>> rule(
        getGroupRoles: GetGroupRoles
    ): DatabaseActionRule.Scoped<T, HasGroupRole> =
        DatabaseActionRule.Scoped.Simple(this, Query(getGroupRoles))

    private class Query<T : Id<*>>(private val getGroupRoles: GetGroupRoles) :
        DatabaseActionRule.Scoped.Query<T, HasGroupRole> {
        override fun cacheKey(user: AuthenticatedUser, now: HelsinkiDateTime): Any =
            when (user) {
                is AuthenticatedUser.Employee -> QuerySql { getGroupRoles(user, now) }
                else -> Pair(user, now)
            }

        override fun executeWithTargets(
            ctx: DatabaseActionRule.QueryContext,
            targets: Set<T>,
        ): Map<T, DatabaseActionRule.Deferred<HasGroupRole>> =
            when (ctx.user) {
                is AuthenticatedUser.Employee -> {
                    val targetCheck = targets.idTargetPredicate()
                    ctx.tx
                        .createQuery {
                            sql(
                                """
                    SELECT id, role, unit_features, unit_provider_type
                    FROM (${subquery { getGroupRoles(ctx.user, ctx.now) } }) fragment
                    WHERE ${predicate(targetCheck.forTable("fragment"))}
                    """
                                    .trimIndent()
                            )
                        }
                        .mapTo<IdRoleFeatures>()
                        .useIterable { rows ->
                            rows.fold(
                                targets.associateTo(linkedMapOf()) {
                                    (it to mutableSetOf<RoleAndFeatures>())
                                }
                            ) { acc, (target, result) ->
                                acc[target]?.plusAssign(result)
                                acc
                            }
                        }
                        .mapValues { (_, queryResult) -> Deferred(queryResult) }
                }
                else -> emptyMap()
            }

        override fun queryWithParams(
            ctx: DatabaseActionRule.QueryContext,
            params: HasGroupRole,
        ): QuerySql? =
            when (ctx.user) {
                is AuthenticatedUser.Employee ->
                    QuerySql {
                        sql(
                            """
                    SELECT id
                    FROM (${subquery { getGroupRoles(ctx.user, ctx.now) } }) fragment
                    WHERE role = ANY(${bind(params.oneOf.toSet())})
                    AND unit_features @> ${bind(params.unitFeatures.toSet())}
                    ${if (params.unitProviderTypes != null) "AND unit_provider_type = ANY(${bind(params.unitProviderTypes.toSet())})" else ""}
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
                        it.unitFeatures.containsAll(params.unitFeatures) &&
                        (params.unitProviderTypes == null ||
                            params.unitProviderTypes.contains(it.unitProviderType))
                }
            ) {
                AccessControlDecision.Permitted(params)
            } else {
                AccessControlDecision.None
            }
    }

    fun inGroup() =
        rule<GroupId> { user, _ ->
            sql(
                """
SELECT daycare_group.id, role, enabled_pilot_features AS unit_features, provider_type AS unit_provider_type
FROM daycare_acl acl
JOIN daycare ON acl.daycare_id = daycare.id
JOIN daycare_group ON daycare.id = daycare_group.daycare_id
JOIN daycare_group_acl group_acl ON daycare_group.id = group_acl.daycare_group_id AND acl.employee_id = group_acl.employee_id
WHERE acl.employee_id = ${bind(user.id)}
            """
            )
        }

    fun inPlacementGroupOfChild() =
        rule<ChildId> { user, now ->
            sql(
                """
SELECT child_id AS id, role, enabled_pilot_features AS unit_features, provider_type AS unit_provider_type
FROM employee_child_group_acl(${bind(now.toLocalDate())}) acl
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = ${bind(user.id)}
            """
                    .trimIndent()
            )
        }

    fun inPlacementGroupOfChildOfAbsenceApplication() =
        rule<AbsenceApplicationId> { user, now ->
            sql(
                """
SELECT absence_application.id AS id, role, enabled_pilot_features AS unit_features, provider_type AS unit_provider_type
FROM absence_application
JOIN placement ON absence_application.child_id = placement.child_id
JOIN employee_child_group_acl(${bind(now.toLocalDate())}) acl ON placement.child_id = acl.child_id
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = ${bind(user.id)}
  AND absence_application.start_date BETWEEN placement.start_date AND placement.end_date
            """
                    .trimIndent()
            )
        }

    fun inPlacementGroupOfChildOfChildDocument(
        editable: Boolean = false,
        deletable: Boolean = false,
        publishable: Boolean = false,
        canGoToPrevStatus: Boolean = false,
    ) =
        rule<ChildDocumentId> { user, now ->
            sql(
                """
SELECT child_document.id AS id, role, enabled_pilot_features AS unit_features, provider_type AS unit_provider_type
FROM child_document
JOIN employee_child_group_acl(${bind(now.toLocalDate())}) acl USING (child_id)
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = ${bind(user.id)}
${if (editable) "AND status = ANY(${bind(DocumentStatus.entries.filter { it.employeeEditable })}::child_document_status[])" else ""}
${if (deletable) "AND status = 'DRAFT' AND published_at IS NULL" else ""}
${if (publishable) "AND status <> 'COMPLETED'" else ""}
${if (canGoToPrevStatus) "AND child_document.type = 'CITIZEN_BASIC' AND child_document.content -> 'answers' = '[]'::jsonb AND child_document.status <> 'COMPLETED'" else ""}
            """
                    .trimIndent()
            )
        }

    fun inPlacementGroupOfDuplicateChildOfHojksChildDocument() =
        rule<ChildDocumentId> { user, now ->
            sql(
                """
SELECT child_document.id AS id, role, enabled_pilot_features AS unit_features, provider_type AS unit_provider_type
FROM child_document
JOIN document_template ON document_template.id = child_document.template_id
JOIN person ON person.id = child_document.child_id
JOIN employee_child_group_acl(${bind(now.toLocalDate())}) acl ON acl.child_id = person.duplicate_of
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = ${bind(user.id)} AND document_template.type = 'HOJKS'
            """
                    .trimIndent()
            )
        }
}
