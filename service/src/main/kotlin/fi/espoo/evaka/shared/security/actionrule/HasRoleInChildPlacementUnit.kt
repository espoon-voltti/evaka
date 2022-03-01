// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AssistanceActionId
import fi.espoo.evaka.shared.AssistanceNeedId
import fi.espoo.evaka.shared.BackupCareId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildImageId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControlDecision
import fi.espoo.evaka.shared.utils.emptyEnumSet
import fi.espoo.evaka.shared.utils.toEnumSet
import org.jdbi.v3.core.kotlin.mapTo
import java.util.EnumSet

private typealias GetRolesInPlacementUnit<T> = (tx: Database.Read, employeeId: EmployeeId, targets: Set<T>) -> Iterable<UnitRole>
private data class UnitRole(val id: Id<*>, val role: UserRole)

data class HasRoleInChildPlacementUnit(val oneOf: EnumSet<UserRole>) {
    init {
        oneOf.forEach { check(it.isUnitScopedRole()) { "Expected a unit-scoped role, got $it" } }
    }
    constructor(vararg oneOf: UserRole) : this(oneOf.toEnumSet())

    private data class Query<T : Id<*>>(private val getRolesInPlacementUnit: GetRolesInPlacementUnit<T>) :
        DatabaseActionRule.Query<T, HasRoleInChildPlacementUnit> {
        override fun execute(tx: Database.Read, user: AuthenticatedUser, targets: Set<T>): Map<T, DatabaseActionRule.Deferred<HasRoleInChildPlacementUnit>> = when (user) {
            is AuthenticatedUser.Employee -> getRolesInPlacementUnit(tx, EmployeeId(user.id), targets)
                .fold(targets.associateTo(linkedMapOf()) { (it to emptyEnumSet<UserRole>()) }) { acc, (target, role) ->
                    acc[target]?.plusAssign(role)
                    acc
                }
                .mapValues { (_, rolesInUnit) -> Deferred(rolesInUnit) }
            else -> emptyMap()
        }
    }
    private data class Deferred(private val rolesInUnit: Set<UserRole>) : DatabaseActionRule.Deferred<HasRoleInChildPlacementUnit> {
        override fun evaluate(params: HasRoleInChildPlacementUnit): AccessControlDecision = if (rolesInUnit.any { params.oneOf.contains(it) }) {
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
LEFT JOIN placement_plan pp ON pp.application_id = av.id
JOIN daycare_acl_view acl ON acl.daycare_id = ANY(av.preferredunits) OR acl.daycare_id = pp.unit_id
WHERE employee_id = :userId AND av.status = ANY ('{SENT,WAITING_PLACEMENT,WAITING_CONFIRMATION,WAITING_DECISION,WAITING_MAILING,WAITING_UNIT_CONFIRMATION,ACTIVE}'::application_status_type[])
AND av.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", employeeId)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )
    val assistanceAction = DatabaseActionRule(
        this,
        Query<AssistanceActionId> { tx, employeeId, ids ->
            tx.createQuery(
                """
SELECT ac.id, role
FROM child_acl_view acl
JOIN assistance_action ac ON acl.child_id = ac.child_id
WHERE acl.employee_id = :userId
AND ac.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", employeeId)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )
    val assistanceNeed = DatabaseActionRule(
        this,
        Query<AssistanceNeedId> { tx, employeeId, ids ->
            tx.createQuery(
                """
SELECT an.id, role
FROM child_acl_view acl
JOIN assistance_need an ON acl.child_id = an.child_id
WHERE acl.employee_id = :userId
AND an.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", employeeId)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )
    val backupCare = DatabaseActionRule(
        this,
        Query<BackupCareId> { tx, employeeId, ids ->
            tx.createQuery(
                """
SELECT bc.id, role
FROM child_acl_view acl
JOIN backup_care bc ON acl.child_id = bc.child_id
WHERE acl.employee_id = :userId
AND bc.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", employeeId)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )
    val child = DatabaseActionRule(
        this,
        Query<ChildId> { tx, employeeId, ids ->
            tx.createQuery(
                """
SELECT child_id AS id, role
FROM child_acl_view
WHERE employee_id = :userId
AND child_id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", employeeId)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )
    val childImage = DatabaseActionRule(
        this,
        Query<ChildImageId> { tx, employeeId, ids ->
            tx.createQuery(
                """
SELECT img.id, role
FROM child_acl_view
JOIN child_images img ON child_acl_view.child_id = img.child_id
WHERE employee_id = :userId
AND img.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", employeeId)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )
    val placement = DatabaseActionRule(
        this,
        Query<PlacementId> { tx, employeeId, ids ->
            tx.createQuery(
                """
SELECT placement.id, role
FROM placement
JOIN daycare_acl_view ON placement.unit_id = daycare_acl_view.daycare_id
WHERE employee_id = :userId
AND placement.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", employeeId)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )
}
