// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupNoteId
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControlDecision
import fi.espoo.evaka.shared.utils.emptyEnumSet
import fi.espoo.evaka.shared.utils.toEnumSet
import org.jdbi.v3.core.kotlin.mapTo
import java.util.EnumSet

private typealias GetRolesInGroupUnit<T> = (tx: Database.Read, employeeId: EmployeeId, targets: Set<T>) -> Iterable<IdAndRole>

data class HasRoleInGroupUnit(val oneOf: EnumSet<UserRole>) : ActionRuleParams<HasRoleInGroupUnit> {
    init {
        oneOf.forEach { check(it.isUnitScopedRole()) { "Expected a unit-scoped role, got $it" } }
    }
    constructor(vararg oneOf: UserRole) : this(oneOf.toEnumSet())

    override fun merge(other: HasRoleInGroupUnit): HasRoleInGroupUnit = HasRoleInGroupUnit(
        (this.oneOf.asSequence() + other.oneOf.asSequence()).toEnumSet()
    )

    private data class Query<T : Id<*>>(private val getRolesInGroupUnit: GetRolesInGroupUnit<T>) :
        DatabaseActionRule.Query<T, HasRoleInGroupUnit> {
        override fun execute(tx: Database.Read, user: AuthenticatedUser, targets: Set<T>): Map<T, DatabaseActionRule.Deferred<HasRoleInGroupUnit>> = when (user) {
            is AuthenticatedUser.Employee -> getRolesInGroupUnit(tx, EmployeeId(user.id), targets)
                .fold(targets.associateTo(linkedMapOf()) { (it to emptyEnumSet<UserRole>()) }) { acc, (target, role) ->
                    acc[target]?.plusAssign(role)
                    acc
                }
                .mapValues { (_, rolesInUnit) -> Deferred(rolesInUnit) }
            else -> emptyMap()
        }
    }
    private data class Deferred(private val rolesInUnit: Set<UserRole>) : DatabaseActionRule.Deferred<HasRoleInGroupUnit> {
        override fun evaluate(params: HasRoleInGroupUnit): AccessControlDecision = if (rolesInUnit.any { params.oneOf.contains(it) }) {
            AccessControlDecision.Permitted(params)
        } else {
            AccessControlDecision.None
        }
    }

    val group = DatabaseActionRule(
        this,
        Query<GroupId> { tx, employeeId, ids ->
            tx.createQuery(
                """
SELECT daycare_group_id AS id, role
FROM daycare_group_acl_view
WHERE employee_id = :userId
AND daycare_group_id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", employeeId)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )
    val groupNote = DatabaseActionRule(
        this,
        Query<GroupNoteId> { tx, employeeId, ids ->
            tx.createQuery(
                """
SELECT gn.id, role
FROM daycare_group_acl_view
JOIN group_note gn ON gn.group_id = daycare_group_acl_view.daycare_group_id
WHERE employee_id = :userId
AND gn.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", employeeId)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )
}
