// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.ChildDailyNoteId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildImageId
import fi.espoo.evaka.shared.ChildStickyNoteId
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupNoteId
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.MobileAuthLevel
import fi.espoo.evaka.shared.db.QueryFragment
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControlDecision

private typealias FilterMobileByTarget<T> = (mobileId: MobileDeviceId, now: HelsinkiDateTime) -> QueryFragment<T>

data class IsMobile(val requirePinLogin: Boolean) {
    fun isPermittedAuthLevel(authLevel: MobileAuthLevel) = when (authLevel) {
        MobileAuthLevel.PIN_LOGIN -> true
        MobileAuthLevel.DEFAULT -> !requirePinLogin
    }

    private fun <T : Id<*>> rule(filter: FilterMobileByTarget<T>): DatabaseActionRule.Scoped<T, IsMobile> =
        DatabaseActionRule.Scoped.Simple(this, Query(filter))
    private data class Query<T : Id<*>>(private val filter: FilterMobileByTarget<T>) : DatabaseActionRule.Scoped.Query<T, IsMobile> {
        override fun executeWithTargets(
            ctx: DatabaseActionRule.QueryContext,
            targets: Set<T>
        ): Map<T, DatabaseActionRule.Deferred<IsMobile>> = when (ctx.user) {
            is AuthenticatedUser.MobileDevice -> filter(ctx.user.id, ctx.now).let { subquery ->
                ctx.tx.createQuery(
                    QueryFragment<Any>(
                        """
                    SELECT id
                    FROM (${subquery.sql}) fragment
                    WHERE id = ANY(:ids)
                        """.trimIndent(),
                        subquery.bindings
                    )
                )
                    .bind("ids", targets.map { it.raw })
                    .mapTo<Id<DatabaseTable>>()
                    .toSet()
            }.let { matched ->
                targets.filter { matched.contains(it) }.associateWith { Deferred(ctx.user.authLevel) }
            }
            else -> emptyMap()
        }
        override fun executeWithParams(
            ctx: DatabaseActionRule.QueryContext,
            params: IsMobile
        ): AccessControlFilter<T>? = when (ctx.user) {
            is AuthenticatedUser.MobileDevice -> if (params.isPermittedAuthLevel(ctx.user.authLevel)) {
                filter(ctx.user.id, ctx.now).let { subquery ->
                    ctx.tx.createQuery(subquery)
                        .mapTo<Id<DatabaseTable>>()
                        .toSet()
                        .let { ids -> AccessControlFilter.Some(ids) }
                }
            } else null
            else -> null
        }
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
        override fun evaluate(user: AuthenticatedUser): AccessControlDecision =
            if (user is AuthenticatedUser.MobileDevice && isPermittedAuthLevel(user.authLevel)) {
                AccessControlDecision.Permitted(this)
            } else AccessControlDecision.None
    }

    fun inPlacementUnitOfChild() = rule { mobileId, now ->
        QueryFragment<ChildId>(
            """
SELECT child_id AS id
FROM child_daycare_acl(:today)
JOIN mobile_device_daycare_acl_view USING (daycare_id)
WHERE mobile_device_id = :userId
            """.trimIndent()
        )
            .bind("today", now.toLocalDate())
            .bind("userId", mobileId)
    }

    fun inPlacementUnitOfChildOfChildDailyNote() = rule { mobileId, now ->
        QueryFragment<ChildDailyNoteId>(
            """
SELECT cdn.id
FROM child_daily_note cdn
JOIN child_daycare_acl(:today) USING (child_id)
JOIN mobile_device_daycare_acl_view USING (daycare_id)
WHERE mobile_device_id = :userId
            """.trimIndent()
        )
            .bind("today", now.toLocalDate())
            .bind("userId", mobileId)
    }

    fun inPlacementUnitOfChildOfChildStickyNote() = rule { mobileId, now ->
        QueryFragment<ChildStickyNoteId>(
            """
SELECT csn.id
FROM child_sticky_note csn
JOIN child_daycare_acl(:today) USING (child_id)
JOIN mobile_device_daycare_acl_view USING (daycare_id)
WHERE mobile_device_id = :userId
            """.trimIndent()
        )
            .bind("today", now.toLocalDate())
            .bind("userId", mobileId)
    }

    fun inPlacementUnitOfChildOfChildImage() = rule { mobileId, now ->
        QueryFragment<ChildImageId>(
            """
SELECT img.id
FROM child_images img
JOIN child_daycare_acl(:today) USING (child_id)
JOIN mobile_device_daycare_acl_view USING (daycare_id)
WHERE mobile_device_id = :userId
            """.trimIndent()
        )
            .bind("today", now.toLocalDate())
            .bind("userId", mobileId)
    }

    fun inUnitOfGroup() = rule { mobileId, _ ->
        QueryFragment<GroupId>(
            """
SELECT g.id
FROM daycare_group g
JOIN mobile_device_daycare_acl_view acl USING (daycare_id)
WHERE acl.mobile_device_id = :userId
            """.trimIndent()
        )
            .bind("userId", mobileId)
    }

    fun inUnitOfGroupNote() = rule { mobileId, _ ->
        QueryFragment<GroupNoteId>(
            """
SELECT gn.id
FROM group_note gn
JOIN daycare_group g ON gn.group_id = g.id
JOIN mobile_device_daycare_acl_view acl USING (daycare_id)
WHERE acl.mobile_device_id = :userId
            """.trimIndent()
        )
            .bind("userId", mobileId)
    }

    fun inUnit() = rule { mobileId, _ ->
        QueryFragment<DaycareId>(
            """
SELECT daycare_id AS id
FROM mobile_device_daycare_acl_view
WHERE mobile_device_id = :userId
            """.trimIndent()
        )
            .bind("userId", mobileId)
    }
}
