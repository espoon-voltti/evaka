// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.AssistanceActionId
import fi.espoo.evaka.shared.AssistanceNeedId
import fi.espoo.evaka.shared.BackupCareId
import fi.espoo.evaka.shared.BackupPickupId
import fi.espoo.evaka.shared.ChildDailyNoteId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildImageId
import fi.espoo.evaka.shared.ChildStickyNoteId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.ParentshipId
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.VasuDocumentId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControlDecision
import fi.espoo.evaka.shared.utils.emptyEnumSet
import fi.espoo.evaka.shared.utils.toEnumSet
import org.jdbi.v3.core.kotlin.mapTo
import java.util.EnumSet

private typealias GetRolesInPlacementUnit<T> = (tx: Database.Read, employeeId: EmployeeId, targets: Set<T>) -> Iterable<IdAndRole>

data class HasRoleInChildPlacementUnit(val oneOf: EnumSet<UserRole>) : ActionRuleParams<HasRoleInChildPlacementUnit> {
    init {
        oneOf.forEach { check(it.isUnitScopedRole()) { "Expected a unit-scoped role, got $it" } }
    }
    constructor(vararg oneOf: UserRole) : this(oneOf.toEnumSet())

    override fun merge(other: HasRoleInChildPlacementUnit): HasRoleInChildPlacementUnit = HasRoleInChildPlacementUnit(
        (this.oneOf.asSequence() + other.oneOf.asSequence()).toEnumSet()
    )

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
    val backupPickup = DatabaseActionRule(
        this,
        Query<BackupPickupId> { tx, employeeId, ids ->
            tx.createQuery(
                """
SELECT bp.id, role
FROM child_acl_view acl
JOIN backup_pickup bp ON acl.child_id = bp.child_id
WHERE acl.employee_id = :userId
AND bp.id = ANY(:ids)
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
    val childDailyNote = DatabaseActionRule(
        this,
        Query<ChildDailyNoteId> { tx, employeeId, ids ->
            tx.createQuery(
                """
SELECT cdn.id, role
FROM child_acl_view
JOIN child_daily_note cdn ON child_acl_view.child_id = cdn.child_id
WHERE employee_id = :userId
AND cdn.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", employeeId)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )
    val childStickyNote = DatabaseActionRule(
        this,
        Query<ChildStickyNoteId> { tx, employeeId, ids ->
            tx.createQuery(
                """
SELECT csn.id, role
FROM child_acl_view
JOIN child_sticky_note csn ON child_acl_view.child_id = csn.child_id
WHERE employee_id = :userId
AND csn.id = ANY(:ids)
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
    val decision = DatabaseActionRule(
        this,
        Query<DecisionId> { tx, employeeId, ids ->
            tx.createQuery(
                """
SELECT decision.id, role
FROM decision
JOIN daycare_acl_view ON decision.unit_id = daycare_acl_view.daycare_id
WHERE employee_id = :userId
AND decision.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", employeeId)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )
    val parentship = DatabaseActionRule(
        this,
        Query<ParentshipId> { tx, employeeId, ids ->
            tx.createQuery(

                """
SELECT fridge_child.id, role
FROM fridge_child
JOIN person_acl_view ON fridge_child.head_of_child = person_acl_view.person_id OR fridge_child.child_id = person_acl_view.person_id
WHERE employee_id = :userId
AND fridge_child.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", employeeId)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )
    val pedagogicalDocument = DatabaseActionRule(
        this,
        Query<PedagogicalDocumentId> { tx, employeeId, ids ->
            tx.createQuery(
                """
SELECT pd.id, role
FROM pedagogical_document pd
JOIN child_acl_view ON pd.child_id = child_acl_view.child_id
WHERE employee_id = :userId
AND pd.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", employeeId)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )
    val person = DatabaseActionRule(
        this,
        Query<PersonId> { tx, employeeId, ids ->
            tx.createQuery(
                """
SELECT person_id AS id, role
FROM person_acl_view
WHERE employee_id = :userId
AND person_id = ANY(:ids)
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
    val serviceNeed = DatabaseActionRule(
        this,
        Query<ServiceNeedId> { tx, employeeId, ids ->
            tx.createQuery(
                """
SELECT service_need.id, role
FROM service_need
JOIN placement ON placement.id = service_need.placement_id
JOIN daycare_acl_view ON placement.unit_id = daycare_acl_view.daycare_id
AND service_need.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", employeeId)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )
    val vasuDocument = DatabaseActionRule(
        this,
        Query<VasuDocumentId> { tx, employeeId, ids ->
            tx.createQuery(
                """
SELECT curriculum_document.id AS id, role
FROM curriculum_document
JOIN child_acl_view ON curriculum_document.child_id = child_acl_view.child_id
WHERE employee_id = :userId
AND curriculum_document.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", employeeId)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )
}
