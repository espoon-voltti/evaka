// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControlDecision
import fi.espoo.evaka.shared.security.actionrule.ActionRuleParams
import fi.espoo.evaka.shared.security.actionrule.DatabaseActionRule
import fi.espoo.evaka.shared.security.actionrule.IdAndRole
import fi.espoo.evaka.shared.utils.emptyEnumSet
import fi.espoo.evaka.shared.utils.toEnumSet
import org.jdbi.v3.core.kotlin.mapTo
import java.util.EnumSet

private typealias GetRolesInApplicationPreferredUnit<T> = (tx: Database.Read, employeeId: EmployeeId, targets: Set<T>) -> Iterable<IdAndRole>

data class HasRoleInApplicationPreferredUnit(val oneOf: EnumSet<UserRole>) : ActionRuleParams<HasRoleInApplicationPreferredUnit> {
    init {
        oneOf.forEach { check(it.isUnitScopedRole()) { "Expected a unit-scoped role, got $it" } }
    }
    constructor(vararg oneOf: UserRole) : this(oneOf.toEnumSet())

    override fun merge(other: HasRoleInApplicationPreferredUnit): HasRoleInApplicationPreferredUnit = HasRoleInApplicationPreferredUnit(
        (this.oneOf.asSequence() + other.oneOf.asSequence()).toEnumSet()
    )

    private data class Query<T : Id<*>>(private val getRolesInPlacementUnit: GetRolesInApplicationPreferredUnit<T>) :
        DatabaseActionRule.Query<T, HasRoleInApplicationPreferredUnit> {
        override fun execute(tx: Database.Read, user: AuthenticatedUser, targets: Set<T>): Map<T, DatabaseActionRule.Deferred<HasRoleInApplicationPreferredUnit>> = when (user) {
            is AuthenticatedUser.Employee -> getRolesInPlacementUnit(tx, EmployeeId(user.id), targets)
                .fold(targets.associateTo(linkedMapOf()) { (it to emptyEnumSet<UserRole>()) }) { acc, (target, role) ->
                    acc[target]?.plusAssign(role)
                    acc
                }
                .mapValues { (_, rolesInUnit) -> Deferred(rolesInUnit) }
            else -> emptyMap()
        }
    }
    private data class Deferred(private val rolesInUnit: Set<UserRole>) : DatabaseActionRule.Deferred<HasRoleInApplicationPreferredUnit> {
        override fun evaluate(params: HasRoleInApplicationPreferredUnit): AccessControlDecision = if (rolesInUnit.any { params.oneOf.contains(it) }) {
            AccessControlDecision.Permitted(params)
        } else {
            AccessControlDecision.None
        }
    }

    val application = DatabaseActionRule(
        this,
        Query<ApplicationId> { tx, employeeId, ids ->
            tx.createQuery(
                """
SELECT av.id, role
FROM application_view av
JOIN daycare_acl_view acl ON acl.daycare_id = ANY (av.preferredunits)
WHERE employee_id = :userId
AND av.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", employeeId)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )
}
