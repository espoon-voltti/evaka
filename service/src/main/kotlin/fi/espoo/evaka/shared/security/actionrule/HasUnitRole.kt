// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AssistanceActionId
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.AssistanceNeedId
import fi.espoo.evaka.shared.AssistanceNeedVoucherCoefficientId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.BackupCareId
import fi.espoo.evaka.shared.BackupPickupId
import fi.espoo.evaka.shared.CalendarEventId
import fi.espoo.evaka.shared.ChildDailyNoteId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildImageId
import fi.espoo.evaka.shared.ChildStickyNoteId
import fi.espoo.evaka.shared.DailyServiceTimesId
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
import fi.espoo.evaka.shared.VasuDocumentId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.QueryFragment
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControlDecision
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.shared.utils.emptyEnumSet
import fi.espoo.evaka.shared.utils.toEnumSet
import java.util.EnumSet

private typealias GetUnitRoles = (user: AuthenticatedUser.Employee, now: HelsinkiDateTime) -> QueryFragment

data class HasUnitRole(val oneOf: EnumSet<UserRole>, val unitFeatures: EnumSet<PilotFeature>) : ActionRuleParams<HasUnitRole> {
    init {
        oneOf.forEach { check(it.isUnitScopedRole()) { "Expected a unit-scoped role, got $it" } }
    }
    constructor(vararg oneOf: UserRole) : this(oneOf.toEnumSet(), emptyEnumSet())

    fun withUnitFeatures(vararg allOf: PilotFeature) = copy(unitFeatures = allOf.toEnumSet())

    private data class Query<T : Id<*>>(val getUnitRoles: GetUnitRoles) :
        DatabaseActionRule.Query<T, HasUnitRole> {
        override fun execute(
            tx: Database.Read,
            user: AuthenticatedUser,
            now: HelsinkiDateTime,
            targets: Set<T>
        ): Map<T, DatabaseActionRule.Deferred<HasUnitRole>> = when (user) {
            is AuthenticatedUser.Employee -> getUnitRoles(user, now).let { subquery ->
                tx.createQuery(
                    QueryFragment(
                        """
                    SELECT id, role, unit_features
                    FROM (${subquery.sql}) fragment
                    WHERE id = ANY(:ids)
                        """.trimIndent(),
                        subquery.bindings
                    )
                )
                    .bind("ids", targets.map { it.raw })
                    .mapTo<IdRoleFeatures>()
            }
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
        Query<ApplicationId> { user, _ ->
            QueryFragment(
                """
SELECT av.id, role, daycare.enabled_pilot_features AS unit_features
FROM application_view av
JOIN placement_plan pp ON pp.application_id = av.id
JOIN daycare_acl acl ON acl.daycare_id = pp.unit_id
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId AND av.status = ANY ('{WAITING_CONFIRMATION,WAITING_MAILING,WAITING_UNIT_CONFIRMATION,ACTIVE}'::application_status_type[])
                """.trimIndent()
            ).bind("userId", user.id)
        }
    )

    fun inPlacementUnitOfChildOfAssistanceAction() = DatabaseActionRule(
        this,
        Query<AssistanceActionId> { user, now, ->
            QueryFragment(
                """
SELECT aa.id, role, daycare.enabled_pilot_features AS unit_features
FROM assistance_action aa
JOIN employee_child_daycare_acl(:today) acl USING (child_id)
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("userId", user.id)
        }
    )

    fun inPlacementUnitOfChildOfAssistanceNeed() = DatabaseActionRule(
        this,
        Query<AssistanceNeedId> { user, now ->
            QueryFragment(
                """
SELECT an.id, role, enabled_pilot_features AS unit_features
FROM assistance_need an
JOIN employee_child_daycare_acl(:today) acl USING (child_id)
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("userId", user.id)
        }
    )

    fun inPlacementUnitOfChildOfAssistanceNeedDecision() = DatabaseActionRule(
        this,
        Query<AssistanceNeedDecisionId> { user, now ->
            QueryFragment(
                """
SELECT ad.id, role, enabled_pilot_features AS unit_features
FROM assistance_need_decision ad
JOIN employee_child_daycare_acl(:today) acl USING (child_id)
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("userId", user.id)
        }
    )

    fun inPlacementUnitOfChildOfAssistanceNeedVoucherCoefficientWithServiceVoucherPlacement() = DatabaseActionRule(
        this,
        Query<AssistanceNeedVoucherCoefficientId> { user, now ->
            QueryFragment(
                """
SELECT avc.id, role, enabled_pilot_features AS unit_features
FROM assistance_need_voucher_coefficient avc
JOIN employee_child_daycare_acl(:today) acl USING (child_id)
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId AND EXISTS(
    SELECT 1 FROM placement p
    JOIN daycare pd ON pd.id = p.unit_id
    WHERE p.child_id = acl.child_id
      AND pd.provider_type = 'PRIVATE_SERVICE_VOUCHER'
)
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("userId", user.id)
        }
    )

    fun inPlacementUnitOfChildOfBackupCare() = DatabaseActionRule(
        this,
        Query<BackupCareId> { user, now ->
            QueryFragment(
                """
SELECT bc.id, role, enabled_pilot_features AS unit_features
FROM backup_care bc
JOIN employee_child_daycare_acl(:today) acl USING (child_id)
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("userId", user.id)
        }
    )

    fun inPlacementUnitOfChildOfBackupPickup() = DatabaseActionRule(
        this,
        Query<BackupPickupId> { user, now ->
            QueryFragment(
                """
SELECT bp.id, role, enabled_pilot_features AS unit_features
FROM backup_pickup bp
JOIN employee_child_daycare_acl(:today) acl USING (child_id)
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("userId", user.id)
        }
    )

    fun inPlacementUnitOfChild() = DatabaseActionRule(
        this,
        Query<ChildId> { user, now ->
            QueryFragment(
                """
SELECT child_id AS id, role, enabled_pilot_features AS unit_features
FROM employee_child_daycare_acl(:today) acl
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("userId", user.id)
        }
    )

    fun inPlacementUnitOfChildOfChildDailyNote() = DatabaseActionRule(
        this,
        Query<ChildDailyNoteId> { user, now ->
            QueryFragment(
                """
SELECT cdn.id, role, enabled_pilot_features AS unit_features
FROM child_daily_note cdn
JOIN employee_child_daycare_acl(:today) acl USING (child_id)
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("userId", user.id)
        }
    )

    fun inPlacementUnitOfChildOfChildStickyNote() = DatabaseActionRule(
        this,
        Query<ChildStickyNoteId> { user, now ->
            QueryFragment(
                """
SELECT csn.id, role, enabled_pilot_features AS unit_features
FROM child_sticky_note csn
JOIN employee_child_daycare_acl(:today) acl USING (child_id)
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("userId", user.id)
        }
    )

    fun inPlacementUnitOfChildOfChildImage() = DatabaseActionRule(
        this,
        Query<ChildImageId> { user, now ->
            QueryFragment(
                """
SELECT img.id, role, enabled_pilot_features AS unit_features
FROM child_images img
JOIN employee_child_daycare_acl(:today) acl USING (child_id)
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("userId", user.id)
        }
    )

    fun inPlacementUnitOfChildOfDecision() = DatabaseActionRule(
        this,
        Query<DecisionId> { user, _ ->
            QueryFragment(
                """
SELECT decision.id, role, daycare.enabled_pilot_features AS unit_features
FROM decision
JOIN daycare_acl acl ON decision.unit_id = acl.daycare_id
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
                """.trimIndent()
            )
                .bind("userId", user.id)
        }
    )

    fun inPlacementUnitOfChildOfParentship() = DatabaseActionRule(
        this,
        Query<ParentshipId> { user, _ ->
            QueryFragment(

                """
SELECT fridge_child.id, role, enabled_pilot_features AS unit_features
FROM fridge_child
JOIN person_acl_view acl ON fridge_child.head_of_child = acl.person_id OR fridge_child.child_id = acl.person_id
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
                """.trimIndent()
            )
                .bind("userId", user.id)
        }
    )

    fun inPlacementUnitOfChildOfPartnership() = DatabaseActionRule(
        this,
        Query<PartnershipId> { user, _ ->
            QueryFragment(

                """
SELECT fridge_partner.partnership_id AS id, role, enabled_pilot_features AS unit_features
FROM fridge_partner
JOIN person_acl_view acl ON fridge_partner.person_id = acl.person_id
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
                """.trimIndent()
            )
                .bind("userId", user.id)
        }
    )

    fun inPlacementUnitOfChildOfPedagogicalDocument() = DatabaseActionRule(
        this,
        Query<PedagogicalDocumentId> { user, now ->
            QueryFragment(
                """
SELECT pd.id, role, enabled_pilot_features AS unit_features
FROM pedagogical_document pd
JOIN employee_child_daycare_acl(:today) acl USING (child_id)
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("userId", user.id)
        }
    )

    fun inPlacementUnitOfChildOfPedagogicalDocumentOfAttachment() = DatabaseActionRule(
        this,
        Query<AttachmentId> { user, now ->
            QueryFragment(
                """
SELECT attachment.id, role, enabled_pilot_features AS unit_features
FROM attachment
JOIN pedagogical_document pd ON attachment.pedagogical_document_id = pd.id
JOIN employee_child_daycare_acl(:today) acl USING (child_id)
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("userId", user.id)
        }
    )

    fun inPlacementUnitOfChildOfPerson() = DatabaseActionRule(
        this,
        Query<PersonId> { user, _ ->
            QueryFragment(
                """
SELECT person_id AS id, role, enabled_pilot_features AS unit_features
FROM person_acl_view acl
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
                """.trimIndent()
            )
                .bind("userId", user.id)
        }
    )

    fun inPlacementUnitOfChildOfPlacement() = DatabaseActionRule(
        this,
        Query<PlacementId> { user, _ ->
            QueryFragment(
                """
SELECT placement.id, role, enabled_pilot_features AS unit_features
FROM placement
JOIN daycare_acl acl ON placement.unit_id = acl.daycare_id
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
                """.trimIndent()
            )
                .bind("userId", user.id)
        }
    )

    fun inPlacementUnitOfChildOfServiceNeed() = DatabaseActionRule(
        this,
        Query<ServiceNeedId> { user, _ ->
            QueryFragment(
                """
SELECT service_need.id, role, enabled_pilot_features AS unit_features
FROM service_need
JOIN placement ON placement.id = service_need.placement_id
JOIN daycare_acl acl ON placement.unit_id = acl.daycare_id
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
                """.trimIndent()
            )
                .bind("userId", user.id)
        }
    )

    fun inPlacementUnitOfChildOfVasuDocument() = DatabaseActionRule(
        this,
        Query<VasuDocumentId> { user, now ->
            QueryFragment(
                """
SELECT curriculum_document.id AS id, role, enabled_pilot_features AS unit_features
FROM curriculum_document
JOIN employee_child_daycare_acl(:today) acl USING (child_id)
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("userId", user.id)
        }
    )

    fun inPlacementUnitOfChildOfDailyServiceTime() = DatabaseActionRule(
        this,
        Query<DailyServiceTimesId> { user, now ->
            QueryFragment(
                """
SELECT dst.id, role, enabled_pilot_features AS unit_features
FROM daily_service_time dst
JOIN employee_child_daycare_acl(:today) acl USING (child_id)
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("userId", user.id)
        }
    )

    fun inPlacementUnitOfChildOfFutureDailyServiceTime() = DatabaseActionRule(
        this,
        Query<DailyServiceTimesId> { user, now ->
            QueryFragment(
                """
SELECT dst.id, role, enabled_pilot_features AS unit_features
FROM daily_service_time dst
JOIN employee_child_daycare_acl(:today) acl USING (child_id)
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
  AND lower(dst.validity_period) > current_date
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("userId", user.id)
        }
    )

    fun inPreferredUnitOfApplication() = DatabaseActionRule(
        this,
        Query<ApplicationId> { user, _ ->
            QueryFragment(
                """
SELECT av.id, role, enabled_pilot_features AS unit_features
FROM application_view av
JOIN daycare_acl acl ON acl.daycare_id = ANY (av.preferredunits)
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
                """.trimIndent()
            )
                .bind("userId", user.id)
        }
    )

    fun inUnitOfGroup() = DatabaseActionRule(
        this,
        Query<GroupId> { user, _ ->
            QueryFragment(
                """
SELECT g.id, role, enabled_pilot_features AS unit_features
FROM daycare_group g
JOIN daycare_acl acl USING (daycare_id)
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
                """.trimIndent()
            )
                .bind("userId", user.id)
        }
    )

    fun inUnitOfGroupNote() = DatabaseActionRule(
        this,
        Query<GroupNoteId> { user, _ ->
            QueryFragment(
                """
SELECT gn.id, role, enabled_pilot_features AS unit_features
FROM group_note gn
JOIN daycare_group g ON gn.group_id = g.id
JOIN daycare_acl acl USING (daycare_id)
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
                """.trimIndent()
            )
                .bind("userId", user.id)
        }
    )

    fun inUnitOfGroupPlacement() = DatabaseActionRule(
        this,
        Query<GroupPlacementId> { user, _ ->
            QueryFragment(
                """
SELECT daycare_group_placement.id, role, enabled_pilot_features AS unit_features
FROM placement
JOIN daycare_acl acl ON placement.unit_id = acl.daycare_id
JOIN daycare_group_placement on placement.id = daycare_group_placement.daycare_placement_id
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
                """.trimIndent()
            )
                .bind("userId", user.id)
        }
    )

    fun inUnitOfMobileDevice() = DatabaseActionRule(
        this,
        Query<MobileDeviceId> { user, _ ->
            QueryFragment(
                """
SELECT d.id, role, enabled_pilot_features AS unit_features
FROM daycare_acl acl
JOIN mobile_device d ON acl.daycare_id = d.unit_id
JOIN daycare ON acl.daycare_id = daycare.id
WHERE acl.employee_id = :userId
                """.trimIndent()
            )
                .bind("userId", user.id)
        }
    )

    fun inUnitOfPairing() = DatabaseActionRule(
        this,
        Query<PairingId> { user, _ ->
            QueryFragment(
                """
SELECT p.id, role, enabled_pilot_features AS unit_features
FROM daycare_acl acl
JOIN pairing p ON acl.daycare_id = p.unit_id
JOIN daycare ON acl.daycare_id = daycare.id
WHERE acl.employee_id = :userId
                """.trimIndent()
            )
                .bind("userId", user.id)
        }
    )

    fun inUnit() = DatabaseActionRule(
        this,
        Query<DaycareId> { user, _ ->
            QueryFragment(
                """
SELECT daycare_id AS id, role, enabled_pilot_features AS unit_features
FROM daycare_acl acl
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
                """.trimIndent()
            )
                .bind("userId", user.id)
        }
    )

    fun inUnitOfApplicationAttachment() = DatabaseActionRule(
        this,
        Query<AttachmentId> { user, _ ->
            QueryFragment(
                """
SELECT attachment.id, role, enabled_pilot_features AS unit_features
FROM attachment
JOIN placement_plan ON attachment.application_id = placement_plan.application_id
JOIN daycare ON placement_plan.unit_id = daycare.id AND daycare.round_the_clock
JOIN daycare_acl ON daycare.id = daycare_acl.daycare_id
WHERE employee_id = :userId
AND attachment.type = 'EXTENDED_CARE'
                """.trimIndent()
            )
                .bind("userId", user.id)
        }
    )

    fun inPlacementUnitOfChildWithServiceVoucherPlacement() = DatabaseActionRule(
        this,
        Query<ChildId> { user, now ->
            QueryFragment(
                """
SELECT child_id AS id, role, enabled_pilot_features AS unit_features
FROM employee_child_daycare_acl(:today) acl
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId AND EXISTS(
    SELECT 1 FROM placement p
    JOIN daycare pd ON pd.id = p.unit_id
    WHERE p.child_id = acl.child_id
      AND pd.provider_type = 'PRIVATE_SERVICE_VOUCHER'
)
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("userId", user.id)
        }
    )

    fun inUnitOfCalendarEvent() = DatabaseActionRule(
        this,
        Query<CalendarEventId> { user, now ->
            tx.createQuery(
                """
SELECT cea.calendar_event_id id, acl.role, enabled_pilot_features AS unit_features
FROM calendar_event_attendee cea
JOIN daycare_acl acl ON acl.daycare_id = cea.unit_id
JOIN daycare ON acl.daycare_id = daycare.id
WHERE employee_id = :userId
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("userId", user.id)
        }
    )
}
