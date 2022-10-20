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
import fi.espoo.evaka.shared.db.QuerySql
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControlDecision

private typealias FilterByMobile<T> =
    QuerySql.Builder<T>.(mobileId: MobileDeviceId, now: HelsinkiDateTime) -> QuerySql<T>

data class IsMobile(val requirePinLogin: Boolean) : DatabaseActionRule.Params {
    fun isPermittedAuthLevel(authLevel: MobileAuthLevel) =
        when (authLevel) {
            MobileAuthLevel.PIN_LOGIN -> true
            MobileAuthLevel.DEFAULT -> !requirePinLogin
        }

    override fun isPermittedForSomeTarget(ctx: DatabaseActionRule.QueryContext): Boolean =
        when (ctx.user) {
            is AuthenticatedUser.MobileDevice -> isPermittedAuthLevel(ctx.user.authLevel)
            else -> false
        }

    private fun <T : Id<*>> rule(
        filter: FilterByMobile<T>
    ): DatabaseActionRule.Scoped<T, IsMobile> =
        DatabaseActionRule.Scoped.Simple(this, Query(filter))
    private data class Query<T : Id<*>>(private val filter: FilterByMobile<T>) :
        DatabaseActionRule.Scoped.Query<T, IsMobile> {
        override fun executeWithTargets(
            ctx: DatabaseActionRule.QueryContext,
            targets: Set<T>
        ): Map<T, DatabaseActionRule.Deferred<IsMobile>> =
            when (ctx.user) {
                is AuthenticatedUser.MobileDevice ->
                    ctx.tx
                        .createQuery<T> {
                            sql(
                                """
                    SELECT id
                    FROM (${subquery { filter(ctx.user.id, ctx.now) } }) fragment
                    WHERE id = ANY(${bind(targets.map { it.raw })})
                    """
                                    .trimIndent()
                            )
                        }
                        .mapTo<Id<DatabaseTable>>()
                        .toSet()
                        .let { matched ->
                            targets
                                .filter { matched.contains(it) }
                                .associateWith { Deferred(ctx.user.authLevel) }
                        }
                else -> emptyMap()
            }

        override fun queryWithParams(
            ctx: DatabaseActionRule.QueryContext,
            params: IsMobile
        ): QuerySql<T>? =
            when (ctx.user) {
                is AuthenticatedUser.MobileDevice ->
                    if (params.isPermittedAuthLevel(ctx.user.authLevel)) {
                        QuerySql.of { filter(ctx.user.id, ctx.now) }
                    } else {
                        null
                    }
                else -> null
            }
    }
    private data class Deferred(private val authLevel: MobileAuthLevel) :
        DatabaseActionRule.Deferred<IsMobile> {
        override fun evaluate(params: IsMobile): AccessControlDecision =
            if (params.isPermittedAuthLevel(authLevel)) {
                AccessControlDecision.Permitted(params)
            } else {
                AccessControlDecision.Denied(params, "PIN login required", "PIN_LOGIN_REQUIRED")
            }
    }

    fun any() =
        object : StaticActionRule {
            override fun evaluate(user: AuthenticatedUser): AccessControlDecision =
                if (
                    user is AuthenticatedUser.MobileDevice && isPermittedAuthLevel(user.authLevel)
                ) {
                    AccessControlDecision.Permitted(this)
                } else {
                    AccessControlDecision.None
                }
        }

    fun inPlacementUnitOfChild() =
        rule<ChildId> { mobileId, now ->
            sql(
                """
SELECT child_id AS id
FROM child_daycare_acl(${bind(now.toLocalDate())})
JOIN mobile_device_daycare_acl_view USING (daycare_id)
WHERE mobile_device_id = ${bind(mobileId)}
            """
                    .trimIndent()
            )
        }

    fun inPlacementUnitOfChildOfChildDailyNote() =
        rule<ChildDailyNoteId> { mobileId, now ->
            sql(
                """
SELECT cdn.id
FROM child_daily_note cdn
JOIN child_daycare_acl(${bind(now.toLocalDate())}) USING (child_id)
JOIN mobile_device_daycare_acl_view USING (daycare_id)
WHERE mobile_device_id = ${bind(mobileId)}
            """
                    .trimIndent()
            )
        }

    fun inPlacementUnitOfChildOfChildStickyNote() =
        rule<ChildStickyNoteId> { mobileId, now ->
            sql(
                """
SELECT csn.id
FROM child_sticky_note csn
JOIN child_daycare_acl(${bind(now.toLocalDate())}) USING (child_id)
JOIN mobile_device_daycare_acl_view USING (daycare_id)
WHERE mobile_device_id = ${bind(mobileId)}
            """
                    .trimIndent()
            )
        }

    fun inPlacementUnitOfChildOfChildImage() =
        rule<ChildImageId> { mobileId, now ->
            sql(
                """
SELECT img.id
FROM child_images img
JOIN child_daycare_acl(${bind(now.toLocalDate())}) USING (child_id)
JOIN mobile_device_daycare_acl_view USING (daycare_id)
WHERE mobile_device_id = ${bind(mobileId)}
            """
                    .trimIndent()
            )
        }

    fun inUnitOfGroup() =
        rule<GroupId> { mobileId, _ ->
            sql(
                """
SELECT g.id
FROM daycare_group g
JOIN mobile_device_daycare_acl_view acl USING (daycare_id)
WHERE acl.mobile_device_id = ${bind(mobileId)}
            """
                    .trimIndent()
            )
        }

    fun inUnitOfGroupNote() =
        rule<GroupNoteId> { mobileId, _ ->
            sql(
                """
SELECT gn.id
FROM group_note gn
JOIN daycare_group g ON gn.group_id = g.id
JOIN mobile_device_daycare_acl_view acl USING (daycare_id)
WHERE acl.mobile_device_id = ${bind(mobileId)}
            """
                    .trimIndent()
            )
        }

    fun inUnit() =
        rule<DaycareId> { mobileId, _ ->
            sql(
                """
SELECT daycare_id AS id
FROM mobile_device_daycare_acl_view
WHERE mobile_device_id = ${bind(mobileId)}
            """
                    .trimIndent()
            )
        }
}
