// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AssistanceActionId
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
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
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControlDecision
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.shared.utils.emptyEnumSet
import fi.espoo.evaka.shared.utils.toEnumSet
import org.jdbi.v3.core.kotlin.mapTo
import java.util.EnumSet

private typealias GetUnitRoles<T> = (tx: Database.Read, user: AuthenticatedUser.Employee, now: HelsinkiDateTime, targets: Set<T>) -> Iterable<IdRoleFeatures>

data class HasUnitRole(val oneOf: EnumSet<UserRole>, val unitFeatures: EnumSet<PilotFeature>) : ActionRuleParams<HasUnitRole> {
    init {
        oneOf.forEach { check(it.isUnitScopedRole()) { "Expected a unit-scoped role, got $it" } }
    }
    constructor(vararg oneOf: UserRole) : this(oneOf.toEnumSet(), emptyEnumSet())

    fun withUnitFeatures(vararg allOf: PilotFeature) = copy(unitFeatures = allOf.toEnumSet())

    private data class Query<T : Id<*>>(val getUnitRoles: GetUnitRoles<T>) :
        DatabaseActionRule.Query<T, HasUnitRole> {
        override fun execute(
            tx: Database.Read,
            user: AuthenticatedUser,
            now: HelsinkiDateTime,
            targets: Set<T>
        ): Map<T, DatabaseActionRule.Deferred<HasUnitRole>> = when (user) {
            is AuthenticatedUser.Employee -> getUnitRoles(tx, user, now, targets)
                .fold(targets.associateTo(linkedMapOf()) { (it to mutableSetOf<RoleAndFeatures>()) }) { acc, (target, result) ->
                    acc[target]?.plusAssign(result)
                    acc
                }
                .mapValues { (_, queryResult) -> Deferred(queryResult) }
            else -> emptyMap()
        }
    }
    private data class Deferred(private val queryResult: Set<RoleAndFeatures>) : DatabaseActionRule.Deferred<HasUnitRole> {
        override fun evaluate(params: HasUnitRole): AccessControlDecision =
            if (queryResult.any { params.oneOf.contains(it.role) && it.unitFeatures.containsAll(params.unitFeatures) }) {
                AccessControlDecision.Permitted(params)
            } else {
                AccessControlDecision.None
            }
    }

    fun inAnyUnit() = UnscopedDatabaseActionRule(
        this,
        object : UnscopedDatabaseActionRule.Query<HasUnitRole> {
            override fun execute(
                tx: Database.Read,
                user: AuthenticatedUser,
                now: HelsinkiDateTime
            ): DatabaseActionRule.Deferred<HasUnitRole> = Deferred(
                when (user) {
                    is AuthenticatedUser.Employee ->
                        tx.createQuery(
                            """
SELECT role, enabled_pilot_features AS unit_features
FROM daycare_acl acl
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
                            """.trimIndent()
                        )
                            .bind("userId", user.id)
                            .mapTo<RoleAndFeatures>()
                            .toSet()
                    else -> emptySet()
                }
            )
            override fun equals(other: Any?): Boolean = other?.javaClass == this.javaClass
            override fun hashCode(): Int = this.javaClass.hashCode()
        }
    )

    fun inPlacementPlanUnitOfApplication() = DatabaseActionRule(
        this,
        Query<ApplicationId> { tx, user, _, ids ->
            tx.createQuery(
                """
SELECT av.id, role, daycare.enabled_pilot_features AS unit_features
FROM application_view av
JOIN placement_plan pp ON pp.application_id = av.id
JOIN daycare_acl acl ON acl.daycare_id = pp.unit_id
JOIN daycare ON acl.daycare_id = daycare.id
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
        Query<AssistanceActionId> { tx, user, now, ids ->
            tx.createQuery(
                """
SELECT aa.id, role, daycare.enabled_pilot_features AS unit_features
FROM assistance_action aa
JOIN employee_child_daycare_acl(:today) acl USING (child_id)
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
AND aa.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementUnitOfChildOfAssistanceNeed() = DatabaseActionRule(
        this,
        Query<AssistanceNeedId> { tx, user, now, ids ->
            tx.createQuery(
                """
SELECT an.id, role, enabled_pilot_features AS unit_features
FROM assistance_need an
JOIN employee_child_daycare_acl(:today) acl USING (child_id)
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
AND an.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementUnitOfChildOfAssistanceNeedDecision() = DatabaseActionRule(
        this,
        Query<AssistanceNeedDecisionId> { tx, user, now, ids ->
            tx.createQuery(
                """
SELECT ad.id, role, enabled_pilot_features AS unit_features
FROM assistance_need_decision ad
JOIN employee_child_daycare_acl(:today) acl USING (child_id)
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
AND ad.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementUnitOfChildOfBackupCare() = DatabaseActionRule(
        this,
        Query<BackupCareId> { tx, user, now, ids ->
            tx.createQuery(
                """
SELECT bc.id, role, enabled_pilot_features AS unit_features
FROM backup_care bc
JOIN employee_child_daycare_acl(:today) acl USING (child_id)
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
AND bc.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementUnitOfChildOfBackupPickup() = DatabaseActionRule(
        this,
        Query<BackupPickupId> { tx, user, now, ids ->
            tx.createQuery(
                """
SELECT bp.id, role, enabled_pilot_features AS unit_features
FROM backup_pickup bp
JOIN employee_child_daycare_acl(:today) acl USING (child_id)
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
AND bp.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementUnitOfChild() = DatabaseActionRule(
        this,
        Query<ChildId> { tx, user, now, ids ->
            tx.createQuery(
                """
SELECT child_id AS id, role, enabled_pilot_features AS unit_features
FROM employee_child_daycare_acl(:today) acl
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

    fun inPlacementUnitOfChildOfChildDailyNote() = DatabaseActionRule(
        this,
        Query<ChildDailyNoteId> { tx, user, now, ids ->
            tx.createQuery(
                """
SELECT cdn.id, role, enabled_pilot_features AS unit_features
FROM child_daily_note cdn
JOIN employee_child_daycare_acl(:today) acl USING (child_id)
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
AND cdn.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementUnitOfChildOfChildStickyNote() = DatabaseActionRule(
        this,
        Query<ChildStickyNoteId> { tx, user, now, ids ->
            tx.createQuery(
                """
SELECT csn.id, role, enabled_pilot_features AS unit_features
FROM child_sticky_note csn
JOIN employee_child_daycare_acl(:today) acl USING (child_id)
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
AND csn.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementUnitOfChildOfChildImage() = DatabaseActionRule(
        this,
        Query<ChildImageId> { tx, user, now, ids ->
            tx.createQuery(
                """
SELECT img.id, role, enabled_pilot_features AS unit_features
FROM child_images img
JOIN employee_child_daycare_acl(:today) acl USING (child_id)
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
AND img.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementUnitOfChildOfDecision() = DatabaseActionRule(
        this,
        Query<DecisionId> { tx, user, _, ids ->
            tx.createQuery(
                """
SELECT decision.id, role, daycare.enabled_pilot_features AS unit_features
FROM decision
JOIN daycare_acl acl ON decision.unit_id = acl.daycare_id
JOIN daycare ON acl.daycare_id = daycare.id
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
        Query<ParentshipId> { tx, user, _, ids ->
            tx.createQuery(

                """
SELECT fridge_child.id, role, enabled_pilot_features AS unit_features
FROM fridge_child
JOIN person_acl_view acl ON fridge_child.head_of_child = acl.person_id OR fridge_child.child_id = acl.person_id
JOIN daycare ON acl.daycare_id = daycare.id
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
        Query<PartnershipId> { tx, user, _, ids ->
            tx.createQuery(

                """
SELECT fridge_partner.partnership_id AS id, role, enabled_pilot_features AS unit_features
FROM fridge_partner
JOIN person_acl_view acl ON fridge_partner.person_id = acl.person_id
JOIN daycare ON acl.daycare_id = daycare.id
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
        Query<PedagogicalDocumentId> { tx, user, now, ids ->
            tx.createQuery(
                """
SELECT pd.id, role, enabled_pilot_features AS unit_features
FROM pedagogical_document pd
JOIN employee_child_daycare_acl(:today) acl USING (child_id)
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
AND pd.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementUnitOfChildOfPedagogicalDocumentOfAttachment() = DatabaseActionRule(
        this,
        Query<AttachmentId> { tx, user, now, ids ->
            tx.createQuery(
                """
SELECT attachment.id, role, enabled_pilot_features AS unit_features
FROM attachment
JOIN pedagogical_document pd ON attachment.pedagogical_document_id = pd.id
JOIN employee_child_daycare_acl(:today) acl USING (child_id)
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
AND attachment.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun inPlacementUnitOfChildOfPerson() = DatabaseActionRule(
        this,
        Query<PersonId> { tx, user, _, ids ->
            tx.createQuery(
                """
SELECT person_id AS id, role, enabled_pilot_features AS unit_features
FROM person_acl_view acl
JOIN daycare ON acl.daycare_id = daycare.id
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
        Query<PlacementId> { tx, user, _, ids ->
            tx.createQuery(
                """
SELECT placement.id, role, enabled_pilot_features AS unit_features
FROM placement
JOIN daycare_acl acl ON placement.unit_id = acl.daycare_id
JOIN daycare ON acl.daycare_id = daycare.id
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
        Query<ServiceNeedId> { tx, user, _, ids ->
            tx.createQuery(
                """
SELECT service_need.id, role, enabled_pilot_features AS unit_features
FROM service_need
JOIN placement ON placement.id = service_need.placement_id
JOIN daycare_acl acl ON placement.unit_id = acl.daycare_id
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
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
        Query<VasuDocumentId> { tx, user, now, ids ->
            tx.createQuery(
                """
SELECT curriculum_document.id AS id, role, enabled_pilot_features AS unit_features
FROM curriculum_document
JOIN employee_child_daycare_acl(:today) acl USING (child_id)
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

    fun inPlacementUnitOfChildOfVasuDocumentFollowupEntry() = DatabaseActionRule(
        this,
        object : DatabaseActionRule.Query<VasuDocumentFollowupEntryId, HasUnitRole> {
            override fun execute(
                tx: Database.Read,
                user: AuthenticatedUser,
                now: HelsinkiDateTime,
                targets: Set<VasuDocumentFollowupEntryId>
            ): Map<VasuDocumentFollowupEntryId, DatabaseActionRule.Deferred<HasUnitRole>> {
                val vasuDocuments =
                    inPlacementUnitOfChildOfVasuDocument().query.execute(tx, user, now, targets.map { it.first }.toSet())
                return targets.mapNotNull { target -> vasuDocuments[target.first]?.let { target to it } }.toMap()
            }
            override fun equals(other: Any?): Boolean = other?.javaClass == javaClass
            override fun hashCode(): Int = this.javaClass.hashCode()
        }
    )

    fun inPreferredUnitOfApplication() = DatabaseActionRule(
        this,
        Query<ApplicationId> { tx, user, _, ids ->
            tx.createQuery(
                """
SELECT av.id, role, enabled_pilot_features AS unit_features
FROM application_view av
JOIN daycare_acl acl ON acl.daycare_id = ANY (av.preferredunits)
JOIN daycare ON acl.daycare_id = daycare.id
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
        Query<GroupId> { tx, user, _, ids ->
            tx.createQuery(
                """
SELECT g.id, role, enabled_pilot_features AS unit_features
FROM daycare_group g
JOIN daycare_acl acl USING (daycare_id)
JOIN daycare ON acl.daycare_id = daycare.id
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
        Query<GroupNoteId> { tx, user, _, ids ->
            tx.createQuery(
                """
SELECT gn.id, role, enabled_pilot_features AS unit_features
FROM group_note gn
JOIN daycare_group g ON gn.group_id = g.id
JOIN daycare_acl acl USING (daycare_id)
JOIN daycare ON acl.daycare_id = daycare.id
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
        Query<GroupPlacementId> { tx, user, _, ids ->
            tx.createQuery(
                """
SELECT daycare_group_placement.id, role, enabled_pilot_features AS unit_features
FROM placement
JOIN daycare_acl acl ON placement.unit_id = acl.daycare_id
JOIN daycare_group_placement on placement.id = daycare_group_placement.daycare_placement_id
JOIN daycare ON acl.daycare_id = daycare.id
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
        Query<MobileDeviceId> { tx, user, _, ids ->
            tx.createQuery(
                """
SELECT d.id, role, enabled_pilot_features AS unit_features
FROM daycare_acl acl
JOIN mobile_device d ON acl.daycare_id = d.unit_id
JOIN daycare ON acl.daycare_id = daycare.id
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
        Query<PairingId> { tx, user, _, ids ->
            tx.createQuery(
                """
SELECT p.id, role, enabled_pilot_features AS unit_features
FROM daycare_acl acl
JOIN pairing p ON acl.daycare_id = p.unit_id
JOIN daycare ON acl.daycare_id = daycare.id
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
        Query<DaycareId> { tx, user, _, ids ->
            tx.createQuery(
                """
SELECT daycare_id AS id, role, enabled_pilot_features AS unit_features
FROM daycare_acl acl
JOIN daycare ON acl.daycare_id = daycare.id
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
        Query<AttachmentId> { tx, user, _, ids ->
            tx.createQuery(
                """
SELECT attachment.id, role, enabled_pilot_features AS unit_features
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
