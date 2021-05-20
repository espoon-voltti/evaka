// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.ChildDailyNoteId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildImageId
import fi.espoo.evaka.shared.ChildStickyNoteId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupNoteId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.MobileAuthLevel
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControlDecision
import org.jdbi.v3.core.kotlin.mapTo

private typealias FilterMobileByTarget<T> = (tx: Database.Read, mobileId: MobileDeviceId, now: HelsinkiDateTime, targets: Set<T>) -> Iterable<T>

data class IsMobile(val requirePinLogin: Boolean) : ActionRuleParams<IsMobile> {
    fun isPermittedAuthLevel(authLevel: MobileAuthLevel) = when (authLevel) {
        MobileAuthLevel.PIN_LOGIN -> true
        MobileAuthLevel.DEFAULT -> !requirePinLogin
    }

    private data class Query<T>(private val filter: FilterMobileByTarget<T>) : DatabaseActionRule.Query<T, IsMobile> {
        override fun execute(
            tx: Database.Read,
            user: AuthenticatedUser,
            now: HelsinkiDateTime,
            targets: Set<T>
        ): Map<T, DatabaseActionRule.Deferred<IsMobile>> = when (user) {
            is AuthenticatedUser.MobileDevice -> filter(
                tx,
                user.id,
                now,
                targets
            ).associateWith { Deferred(user.authLevel) }
            else -> emptyMap()
        }

        override fun classifier(): Any = filter.javaClass
    }
    private data class Deferred(private val authLevel: MobileAuthLevel) : DatabaseActionRule.Deferred<IsMobile> {
        override fun evaluate(params: IsMobile): AccessControlDecision =
            if (params.isPermittedAuthLevel(authLevel)) {
                AccessControlDecision.Permitted(params)
            } else {
                AccessControlDecision.Denied(params, "PIN login required", "PIN_LOGIN_REQUIRED")
            }
    }

    fun any() = object : StaticActionRule {
        override fun isPermitted(user: AuthenticatedUser): Boolean = when (user) {
            is AuthenticatedUser.MobileDevice -> isPermittedAuthLevel(user.authLevel)
            else -> false
        }
    }

    fun inPlacementUnitOfChild() = DatabaseActionRule(
        this,
        Query<ChildId> { tx, mobileId, now, ids ->
            tx.createQuery(
                """
SELECT child_id
FROM child_daycare_acl(:today)
JOIN mobile_device_daycare_acl_view USING (daycare_id)
WHERE mobile_device_id = :userId
AND child_id = ANY(:ids)
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("ids", ids.toTypedArray())
                .bind("userId", mobileId)
                .mapTo()
        }
    )

    fun inPlacementUnitOfChildOfChildDailyNote() = DatabaseActionRule(
        this,
        Query<ChildDailyNoteId> { tx, mobileId, now, ids ->
            tx.createQuery(
                """
SELECT cdn.id
FROM child_daily_note cdn
JOIN child_daycare_acl(:today) USING (child_id)
JOIN mobile_device_daycare_acl_view USING (daycare_id)
WHERE mobile_device_id = :userId
AND cdn.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("ids", ids.toTypedArray())
                .bind("userId", mobileId)
                .mapTo()
        }
    )

    fun inPlacementUnitOfChildOfChildStickyNote() = DatabaseActionRule(
        this,
        Query<ChildStickyNoteId> { tx, mobileId, now, ids ->
            tx.createQuery(
                """
SELECT csn.id
FROM child_sticky_note csn
JOIN child_daycare_acl(:today) USING (child_id)
JOIN mobile_device_daycare_acl_view USING (daycare_id)
WHERE mobile_device_id = :userId
AND csn.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("ids", ids.toTypedArray())
                .bind("userId", mobileId)
                .mapTo()
        }
    )

    fun inPlacementUnitOfChildOfChildImage() = DatabaseActionRule(
        this,
        Query<ChildImageId> { tx, mobileId, now, ids ->
            tx.createQuery(
                """
SELECT img.id
FROM child_images img
JOIN child_daycare_acl(:today) USING (child_id)
JOIN mobile_device_daycare_acl_view USING (daycare_id)
WHERE mobile_device_id = :userId
AND img.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("today", now.toLocalDate())
                .bind("ids", ids.toTypedArray())
                .bind("userId", mobileId)
                .mapTo()
        }
    )

    fun inUnitOfGroup() = DatabaseActionRule(
        this,
        Query<GroupId> { tx, mobileId, _, ids ->
            tx.createQuery(
                """
SELECT g.id
FROM daycare_group g
JOIN mobile_device_daycare_acl_view acl USING (daycare_id)
WHERE acl.mobile_device_id = :userId
AND g.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("ids", ids.toTypedArray())
                .bind("userId", mobileId)
                .mapTo()
        }
    )

    fun inUnitOfGroupNote() = DatabaseActionRule(
        this,
        Query<GroupNoteId> { tx, mobileId, _, ids ->
            tx.createQuery(
                """
SELECT gn.id
FROM group_note gn
JOIN daycare_group g ON gn.group_id = g.id
JOIN mobile_device_daycare_acl_view acl USING (daycare_id)
WHERE acl.mobile_device_id = :userId
AND gn.id = ANY(:ids)
                """.trimIndent()
            )
                .bind("ids", ids.toTypedArray())
                .bind("userId", mobileId)
                .mapTo()
        }
    )

    fun inUnit() = DatabaseActionRule(
        this,
        Query<DaycareId> { tx, mobileId, _, ids ->
            tx.createQuery(
                """
SELECT daycare_id AS id
FROM mobile_device_daycare_acl_view
WHERE mobile_device_id = :userId
AND daycare_id = ANY(:ids)
                """.trimIndent()
            )
                .bind("ids", ids.toTypedArray())
                .bind("userId", mobileId)
                .mapTo()
        }
    )
}
