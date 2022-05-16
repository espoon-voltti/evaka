// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AssistanceActionId
import fi.espoo.evaka.shared.AssistanceNeedId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.BackupCareId
import fi.espoo.evaka.shared.BackupPickupId
import fi.espoo.evaka.shared.ChildDailyNoteId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildImageId
import fi.espoo.evaka.shared.ChildStickyNoteId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupNoteId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.PairingId
import fi.espoo.evaka.shared.ParentshipId
import fi.espoo.evaka.shared.PartnershipId
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.VasuDocumentFollowupEntryId
import fi.espoo.evaka.shared.VasuDocumentId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControlDecision
import fi.espoo.evaka.shared.utils.emptyEnumSet
import fi.espoo.evaka.shared.utils.toEnumSet
import org.jdbi.v3.core.kotlin.mapTo
import java.util.EnumSet

private typealias GetUnitRoles<T> = (tx: Database.Read, user: AuthenticatedUser.Employee, targets: Set<T>) -> Iterable<IdAndRole>

data class HasUnitRole(val oneOf: EnumSet<UserRole>) : ActionRuleParams<HasUnitRole> {
    init {
        oneOf.forEach { check(it.isUnitScopedRole()) { "Expected a unit-scoped role, got $it" } }
    }
    constructor(vararg oneOf: UserRole) : this(oneOf.toEnumSet())

    private data class Query<T : Id<*>>(private val getUnitRoles: GetUnitRoles<T>) :
        DatabaseActionRule.Query<T, HasUnitRole> {
        override fun execute(tx: Database.Read, user: AuthenticatedUser, targets: Set<T>): Map<T, DatabaseActionRule.Deferred<HasUnitRole>> = when (user) {
            is AuthenticatedUser.Employee -> getUnitRoles(tx, user, targets)
                .fold(targets.associateTo(linkedMapOf()) { (it to emptyEnumSet<UserRole>()) }) { acc, (target, role) ->
                    acc[target]?.plusAssign(role)
                    acc
                }
                .mapValues { (_, rolesInUnit) -> Deferred(rolesInUnit) }
            else -> emptyMap()
        }

        override fun classifier(): Any = getUnitRoles.javaClass
    }
    private data class Deferred(private val rolesInUnit: Set<UserRole>) : DatabaseActionRule.Deferred<HasUnitRole> {
        override fun evaluate(params: HasUnitRole): AccessControlDecision = if (rolesInUnit.any { params.oneOf.contains(it) }) {
            AccessControlDecision.Permitted(params)
        } else {
            AccessControlDecision.None
        }
    }

    fun inAnyUnit() = object : StaticActionRule {
        override fun isPermitted(user: AuthenticatedUser): Boolean =
            user is AuthenticatedUser.Employee && user.allScopedRoles.any { oneOf.contains(it) }
    }

    fun inPlacementPlanUnitOfApplication() = DatabaseActionRule(
        this,
        Query<ApplicationId> { tx, user, ids ->
            tx.createQuery(
                """
SELECT av.id, role
FROM application_view av
LEFT JOIN placement_plan pp ON pp.application_id = av.id
JOIN daycare_acl acl ON acl.daycare_id = pp.unit_id
WHERE employee_id = :userId AND av.status = ANY ('{WAITING_CONFIRMATION,WAITING_MAILING,WAITING_UNIT_CONFIRMATION,ACTIVE}'::application_status_type[])
AND av.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementUnitOfChildOfAssistanceAction() = DatabaseActionRule(
        this,
        Query<AssistanceActionId> { tx, user, ids ->
            tx.createQuery(
                """
SELECT ac.id, role
FROM child_acl_view acl
JOIN assistance_action ac ON acl.child_id = ac.child_id
WHERE acl.employee_id = :userId
AND ac.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementUnitOfChildOfAssistanceNeed() = DatabaseActionRule(
        this,
        Query<AssistanceNeedId> { tx, user, ids ->
            tx.createQuery(
                """
SELECT an.id, role
FROM child_acl_view acl
JOIN assistance_need an ON acl.child_id = an.child_id
WHERE acl.employee_id = :userId
AND an.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementUnitOfChildOfBackupCare() = DatabaseActionRule(
        this,
        Query<BackupCareId> { tx, user, ids ->
            tx.createQuery(
                """
SELECT bc.id, role
FROM child_acl_view acl
JOIN backup_care bc ON acl.child_id = bc.child_id
WHERE acl.employee_id = :userId
AND bc.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementUnitOfChildOfBackupPickup() = DatabaseActionRule(
        this,
        Query<BackupPickupId> { tx, user, ids ->
            tx.createQuery(
                """
SELECT bp.id, role
FROM child_acl_view acl
JOIN backup_pickup bp ON acl.child_id = bp.child_id
WHERE acl.employee_id = :userId
AND bp.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementUnitOfChild() = DatabaseActionRule(
        this,
        Query<ChildId> { tx, user, ids ->
            tx.createQuery(
                """
SELECT child_id AS id, role
FROM child_acl_view
WHERE employee_id = :userId
AND child_id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementUnitOfChildOfChildDailyNote() = DatabaseActionRule(
        this,
        Query<ChildDailyNoteId> { tx, user, ids ->
            tx.createQuery(
                """
SELECT cdn.id, role
FROM child_acl_view
JOIN child_daily_note cdn ON child_acl_view.child_id = cdn.child_id
WHERE employee_id = :userId
AND cdn.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementUnitOfChildOfChildStickyNote() = DatabaseActionRule(
        this,
        Query<ChildStickyNoteId> { tx, user, ids ->
            tx.createQuery(
                """
SELECT csn.id, role
FROM child_acl_view
JOIN child_sticky_note csn ON child_acl_view.child_id = csn.child_id
WHERE employee_id = :userId
AND csn.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementUnitOfChildOfChildImage() = DatabaseActionRule(
        this,
        Query<ChildImageId> { tx, user, ids ->
            tx.createQuery(
                """
SELECT img.id, role
FROM child_acl_view
JOIN child_images img ON child_acl_view.child_id = img.child_id
WHERE employee_id = :userId
AND img.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementUnitOfChildOfDecision() = DatabaseActionRule(
        this,
        Query<DecisionId> { tx, user, ids ->
            tx.createQuery(
                """
SELECT decision.id, role
FROM decision
JOIN daycare_acl ON decision.unit_id = daycare_acl.daycare_id
WHERE employee_id = :userId
AND decision.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementUnitOfChildOfParentship() = DatabaseActionRule(
        this,
        Query<ParentshipId> { tx, user, ids ->
            tx.createQuery(

                """
SELECT fridge_child.id, role
FROM fridge_child
JOIN person_acl_view ON fridge_child.head_of_child = person_acl_view.person_id OR fridge_child.child_id = person_acl_view.person_id
WHERE employee_id = :userId
AND fridge_child.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementUnitOfChildOfPartnership() = DatabaseActionRule(
        this,
        Query<PartnershipId> { tx, user, ids ->
            tx.createQuery(

                """
SELECT fridge_partner.partnership_id AS id, role
FROM fridge_partner
JOIN person_acl_view ON fridge_partner.person_id = person_acl_view.person_id
WHERE employee_id = :userId
AND partnership_id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementUnitOfChildOfPedagogicalDocument() = DatabaseActionRule(
        this,
        Query<PedagogicalDocumentId> { tx, user, ids ->
            tx.createQuery(
                """
SELECT pd.id, role
FROM pedagogical_document pd
JOIN child_acl_view ON pd.child_id = child_acl_view.child_id
WHERE employee_id = :userId
AND pd.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementUnitOfChildOfPedagogicalDocumentOfAttachment() = DatabaseActionRule(
        this,
        Query<AttachmentId> { tx, user, ids ->
            tx.createQuery(
                """
SELECT attachment.id, role
FROM attachment
JOIN pedagogical_document pd ON attachment.pedagogical_document_id = pd.id
JOIN child_acl_view ON pd.child_id = child_acl_view.child_id
WHERE employee_id = :userId
AND attachment.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementUnitOfChildOfPerson() = DatabaseActionRule(
        this,
        Query<PersonId> { tx, user, ids ->
            tx.createQuery(
                """
SELECT person_id AS id, role
FROM person_acl_view
WHERE employee_id = :userId
AND person_id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementUnitOfChildOfPlacement() = DatabaseActionRule(
        this,
        Query<PlacementId> { tx, user, ids ->
            tx.createQuery(
                """
SELECT placement.id, role
FROM placement
JOIN daycare_acl ON placement.unit_id = daycare_acl.daycare_id
WHERE employee_id = :userId
AND placement.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementUnitOfChildOfServiceNeed() = DatabaseActionRule(
        this,
        Query<ServiceNeedId> { tx, user, ids ->
            tx.createQuery(
                """
SELECT service_need.id, role
FROM service_need
JOIN placement ON placement.id = service_need.placement_id
JOIN daycare_acl ON placement.unit_id = daycare_acl.daycare_id
AND service_need.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementUnitOfChildOfVasuDocument() = DatabaseActionRule(
        this,
        Query<VasuDocumentId> { tx, user, ids ->
            tx.createQuery(
                """
SELECT curriculum_document.id AS id, role
FROM curriculum_document
JOIN child_acl_view ON curriculum_document.child_id = child_acl_view.child_id
WHERE employee_id = :userId
AND curriculum_document.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementUnitOfChildOfVasuDocumentFollowupEntry() = DatabaseActionRule(
        this,
        object : DatabaseActionRule.Query<VasuDocumentFollowupEntryId, HasUnitRole> {
            override fun execute(
                tx: Database.Read,
                user: AuthenticatedUser,
                targets: Set<VasuDocumentFollowupEntryId>
            ): Map<VasuDocumentFollowupEntryId, DatabaseActionRule.Deferred<HasUnitRole>> {
                val vasuDocuments =
                    inPlacementUnitOfChildOfVasuDocument().query.execute(tx, user, targets.map { it.first }.toSet())
                return targets.mapNotNull { target -> vasuDocuments[target.first]?.let { target to it } }.toMap()
            }

            override fun classifier(): Any = this.javaClass
        }
    )

    fun inPreferredUnitOfApplication() = DatabaseActionRule(
        this,
        Query<ApplicationId> { tx, user, ids ->
            tx.createQuery(
                """
SELECT av.id, role
FROM application_view av
JOIN daycare_acl acl ON acl.daycare_id = ANY (av.preferredunits)
WHERE employee_id = :userId
AND av.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inUnitOfGroup() = DatabaseActionRule(
        this,
        Query<GroupId> { tx, user, ids ->
            tx.createQuery(
                """
SELECT g.id, role
FROM daycare_group g
JOIN daycare_acl USING (daycare_id)
WHERE employee_id = :userId
AND g.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inUnitOfGroupNote() = DatabaseActionRule(
        this,
        Query<GroupNoteId> { tx, user, ids ->
            tx.createQuery(
                """
SELECT gn.id, role
FROM group_note gn
JOIN daycare_group g ON gn.group_id = g.id
JOIN daycare_acl USING (daycare_id)
WHERE employee_id = :userId
AND gn.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inUnitOfGroupPlacement() = DatabaseActionRule(
        this,
        Query<GroupPlacementId> { tx, user, ids ->
            tx.createQuery(
                """
SELECT daycare_group_placement.id, role
FROM placement
JOIN daycare_acl ON placement.unit_id = daycare_acl.daycare_id
JOIN daycare_group_placement on placement.id = daycare_group_placement.daycare_placement_id
WHERE employee_id = :userId
AND daycare_group_placement.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inUnitOfMobileDevice() = DatabaseActionRule(
        this,
        Query<MobileDeviceId> { tx, user, ids ->
            tx.createQuery(
                """
SELECT d.id, role
FROM daycare_acl acl
JOIN mobile_device d ON acl.daycare_id = d.unit_id
WHERE acl.employee_id = :userId
AND d.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inUnitOfPairing() = DatabaseActionRule(
        this,
        Query<PairingId> { tx, user, ids ->
            tx.createQuery(
                """
SELECT p.id, role
FROM daycare_acl acl
JOIN pairing p ON acl.daycare_id = p.unit_id
WHERE acl.employee_id = :userId
AND p.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inUnit() = DatabaseActionRule(
        this,
        Query<DaycareId> { tx, user, ids ->
            tx.createQuery(
                """
SELECT daycare_id AS id, role
FROM daycare_acl
WHERE employee_id = :userId
AND daycare_id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inUnitOfApplicationAttachment() = DatabaseActionRule(
        this,
        Query<AttachmentId> { tx, user, ids ->
            tx.createQuery(
                """
SELECT attachment.id, role
FROM attachment
JOIN placement_plan ON attachment.application_id = placement_plan.application_id
JOIN daycare ON placement_plan.unit_id = daycare.id AND daycare.round_the_clock
JOIN daycare_acl ON daycare.id = daycare_acl.daycare_id
WHERE employee_id = :userId
AND attachment.type = 'EXTENDED_CARE'
AND attachment.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )
}
