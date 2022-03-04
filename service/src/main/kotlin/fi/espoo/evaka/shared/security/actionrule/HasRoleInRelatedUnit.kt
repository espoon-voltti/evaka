// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupNoteId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.PairingId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControlDecision
import fi.espoo.evaka.shared.utils.emptyEnumSet
import fi.espoo.evaka.shared.utils.toEnumSet
import org.jdbi.v3.core.kotlin.mapTo
import java.util.EnumSet

private typealias GetRolesInRelatedUnit<T> = (tx: Database.Read, employeeId: EmployeeId, targets: Set<T>) -> Iterable<IdAndRole>

data class HasRoleInRelatedUnit(val oneOf: EnumSet<UserRole>) : ActionRuleParams<HasRoleInRelatedUnit> {
    init {
        oneOf.forEach { check(it.isUnitScopedRole()) { "Expected a unit-scoped role, got $it" } }
    }
    constructor(vararg oneOf: UserRole) : this(oneOf.toEnumSet())

    override fun merge(other: HasRoleInRelatedUnit): HasRoleInRelatedUnit = HasRoleInRelatedUnit(
        (this.oneOf.asSequence() + other.oneOf.asSequence()).toEnumSet()
    )

    private data class Query<T : Id<*>>(private val getRolesInRelatedUnit: GetRolesInRelatedUnit<T>) :
        DatabaseActionRule.Query<T, HasRoleInRelatedUnit> {
        override fun execute(tx: Database.Read, user: AuthenticatedUser, targets: Set<T>): Map<T, DatabaseActionRule.Deferred<HasRoleInRelatedUnit>> = when (user) {
            is AuthenticatedUser.Employee -> getRolesInRelatedUnit(tx, EmployeeId(user.id), targets)
                .fold(targets.associateTo(linkedMapOf()) { (it to emptyEnumSet<UserRole>()) }) { acc, (target, role) ->
                    acc[target]?.plusAssign(role)
                    acc
                }
                .mapValues { (_, rolesInUnit) -> Deferred(rolesInUnit) }
            else -> emptyMap()
        }
    }
    private data class Deferred(private val rolesInUnit: Set<UserRole>) : DatabaseActionRule.Deferred<HasRoleInRelatedUnit> {
        override fun evaluate(params: HasRoleInRelatedUnit): AccessControlDecision = if (rolesInUnit.any { params.oneOf.contains(it) }) {
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
    val groupPlacement = DatabaseActionRule(
        this,
        Query<GroupPlacementId> { tx, employeeId, ids ->
            tx.createQuery(
                """
SELECT daycare_group_placement.id, role
FROM placement
JOIN daycare_acl_view ON placement.unit_id = daycare_acl_view.daycare_id
JOIN daycare_group_placement on placement.id = daycare_group_placement.daycare_placement_id
WHERE employee_id = :userId
AND daycare_group_placement.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", employeeId)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )
    val mobileDevice = DatabaseActionRule(
        this,
        Query<MobileDeviceId> { tx, employeeId, ids ->
            tx.createQuery(
                """
SELECT d.id, role
FROM daycare_acl acl
JOIN mobile_device d ON acl.daycare_id = d.unit_id
WHERE acl.employee_id = :userId
AND d.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", employeeId)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )
    val pairing = DatabaseActionRule(
        this,
        Query<PairingId> { tx, employeeId, ids ->
            tx.createQuery(
                """
SELECT p.id, role
FROM daycare_acl acl
JOIN pairing p ON acl.daycare_id = p.unit_id
WHERE acl.employee_id = :userId
AND p.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", employeeId)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )
    val unit = DatabaseActionRule(
        this,
        Query<DaycareId> { tx, employeeId, ids ->
            tx.createQuery(
                """
SELECT daycare_id AS id, role
FROM daycare_acl_view
WHERE employee_id = :userId
AND daycare_id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", employeeId)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )
}
