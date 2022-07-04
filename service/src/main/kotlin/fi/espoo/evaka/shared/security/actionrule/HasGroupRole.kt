// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.VasuDocumentId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControlDecision
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.shared.utils.emptyEnumSet
import fi.espoo.evaka.shared.utils.toEnumSet
import org.jdbi.v3.core.kotlin.mapTo
import java.util.EnumSet

private typealias GetGroupRoles<T> = (tx: Database.Read, user: AuthenticatedUser.Employee, now: HelsinkiDateTime, targets: Set<T>) -> Iterable<IdRoleFeatures>

data class HasGroupRole(val oneOf: EnumSet<UserRole>, val unitFeatures: Set<PilotFeature>) : ActionRuleParams<HasGroupRole> {
    init {
        oneOf.forEach { check(it.isUnitScopedRole()) { "Expected a unit-scoped role, got $it" } }
    }
    constructor(vararg oneOf: UserRole) : this(oneOf.toEnumSet(), emptyEnumSet())

    fun withUnitFeatures(vararg allOf: PilotFeature) = copy(unitFeatures = allOf.toEnumSet())

    private data class Query<T : Id<*>>(private val getGroupRoles: GetGroupRoles<T>) : DatabaseActionRule.Query<T, HasGroupRole> {
        override fun execute(
            tx: Database.Read,
            user: AuthenticatedUser,
            now: HelsinkiDateTime,
            targets: Set<T>
        ): Map<T, DatabaseActionRule.Deferred<HasGroupRole>> = when (user) {
            is AuthenticatedUser.Employee -> getGroupRoles(tx, user, now, targets)
                .fold(targets.associateTo(linkedMapOf()) { (it to mutableSetOf<RoleAndFeatures>()) }) { acc, (target, result) ->
                    acc[target]?.plusAssign(result)
                    acc
                }
                .mapValues { (_, queryResult) -> Deferred(queryResult) }
            else -> emptyMap()
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

    fun inPlacementGroupOfChild() = DatabaseActionRule(
        this,
        Query<ChildId> { tx, user, now, ids ->
            tx.createQuery(
                """
SELECT child_id AS id, role, enabled_pilot_features AS unit_features
FROM employee_child_group_acl(:today) acl
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
AND child_id = ANY(:ids)
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementGroupOfChildOfVasuDocument() = DatabaseActionRule(
        this,
        Query<VasuDocumentId> { tx, user, now, ids ->
            tx.createQuery(
                """
SELECT curriculum_document.id AS id, role, enabled_pilot_features AS unit_features
FROM curriculum_document
JOIN employee_child_group_acl(:today) acl USING (child_id)
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
AND curriculum_document.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )
}
