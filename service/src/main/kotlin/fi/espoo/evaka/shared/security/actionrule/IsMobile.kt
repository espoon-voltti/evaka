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
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.MobileAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.QuerySql
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControlDecision

private typealias FilterByMobile<T> =
    QuerySql.Builder<T>.(user: AuthenticatedUser.MobileDevice, now: HelsinkiDateTime) -> QuerySql<T>

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
        override fun cacheKey(user: AuthenticatedUser, now: HelsinkiDateTime): Any =
            when (user) {
                is AuthenticatedUser.MobileDevice -> QuerySql.of { filter(user, now) }
                else -> Pair(user, now)
            }

        override fun executeWithTargets(
            ctx: DatabaseActionRule.QueryContext,
            targets: Set<T>
        ): Map<T, DatabaseActionRule.Deferred<IsMobile>> =
            when (ctx.user) {
                is AuthenticatedUser.MobileDevice -> {
                    val targetCheck = targets.idTargetPredicate()
                    ctx.tx
                        .createQuery<T> {
                            sql(
                                """
                    SELECT id
                    FROM (${subquery { filter(ctx.user, ctx.now) } }) fragment
                    WHERE ${predicate(targetCheck.forTable("fragment"))}
                    """
                                    .trimIndent()
                            )
                        }
                        .toSet<Id<DatabaseTable>>()
                        .let { matched ->
                            targets
                                .filter { matched.contains(it) }
                                .associateWith { Deferred(ctx.user.authLevel) }
                        }
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
                        QuerySql.of { filter(ctx.user, ctx.now) }
                    } else {
                        null
                    }
                else -> null
            }
    }

    private data class Deferred(private val authLevel: MobileAuthLevel) :
        DatabaseActionRule.Deferred<IsMobile> {
        private data object PinLoginRequired : AccessControlDecision {
            override fun isPermitted(): Boolean = false

            override fun assert() = throw Forbidden("PIN login required", "PIN_LOGIN_REQUIRED")

            override fun assertIfTerminal() = assert()
        }

        override fun evaluate(params: IsMobile): AccessControlDecision =
            if (params.isPermittedAuthLevel(authLevel)) {
                AccessControlDecision.Permitted(params)
            } else {
                PinLoginRequired
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
        rule<ChildId> { user, now ->
            sql(
                """
SELECT child_id AS id
FROM child_daycare_acl(${bind(now.toLocalDate())})
JOIN mobile_device_daycare_acl_view USING (daycare_id)
WHERE mobile_device_id = ${bind(user.id)}
            """
                    .trimIndent()
            )
        }

    fun inPlacementUnitOfChildOfChildDailyNote() =
        rule<ChildDailyNoteId> { user, now ->
            sql(
                """
SELECT cdn.id
FROM child_daily_note cdn
JOIN child_daycare_acl(${bind(now.toLocalDate())}) USING (child_id)
JOIN mobile_device_daycare_acl_view USING (daycare_id)
WHERE mobile_device_id = ${bind(user.id)}
            """
                    .trimIndent()
            )
        }

    fun inPlacementUnitOfChildOfChildStickyNote() =
        rule<ChildStickyNoteId> { user, now ->
            sql(
                """
SELECT csn.id
FROM child_sticky_note csn
JOIN child_daycare_acl(${bind(now.toLocalDate())}) USING (child_id)
JOIN mobile_device_daycare_acl_view USING (daycare_id)
WHERE mobile_device_id = ${bind(user.id)}
            """
                    .trimIndent()
            )
        }

    fun inPlacementUnitOfChildOfChildImage() =
        rule<ChildImageId> { user, now ->
            sql(
                """
SELECT img.id
FROM child_images img
JOIN child_daycare_acl(${bind(now.toLocalDate())}) USING (child_id)
JOIN mobile_device_daycare_acl_view USING (daycare_id)
WHERE mobile_device_id = ${bind(user.id)}
            """
                    .trimIndent()
            )
        }

    fun inUnitOfGroup() =
        rule<GroupId> { user, _ ->
            sql(
                """
SELECT g.id
FROM daycare_group g
JOIN mobile_device_daycare_acl_view acl USING (daycare_id)
WHERE acl.mobile_device_id = ${bind(user.id)}
            """
                    .trimIndent()
            )
        }

    fun inUnitOfGroupNote() =
        rule<GroupNoteId> { user, _ ->
            sql(
                """
SELECT gn.id
FROM group_note gn
JOIN daycare_group g ON gn.group_id = g.id
JOIN mobile_device_daycare_acl_view acl USING (daycare_id)
WHERE acl.mobile_device_id = ${bind(user.id)}
            """
                    .trimIndent()
            )
        }

    fun inUnit() =
        rule<DaycareId> { user, _ ->
            sql(
                """
SELECT daycare_id AS id
FROM mobile_device_daycare_acl_view
WHERE mobile_device_id = ${bind(user.id)}
            """
                    .trimIndent()
            )
        }

    fun hasPersonalMessageAccount() =
        rule<MessageAccountId> { user, _ ->
            sql(
                """
SELECT acc.id
FROM message_account acc
JOIN employee ON acc.employee_id = employee.id
WHERE employee.id = ${bind(user.employeeId)} AND acc.active = TRUE
                """
                    .trimIndent()
            )
        }

    fun hasDaycareMessageAccount(vararg roles: UserRole) =
        rule<MessageAccountId> { user, _ ->
            sql(
                """
SELECT acc.id
FROM message_account acc
JOIN daycare_group dg ON acc.daycare_group_id = dg.id
JOIN daycare_acl acl ON acl.daycare_id = dg.daycare_id AND acl.role = ANY(${bind(roles.asList())})
WHERE acl.employee_id = ${bind(user.employeeId)} AND acc.active = TRUE
                """
                    .trimIndent()
            )
        }

    fun hasDaycareGroupMessageAccount() =
        rule<MessageAccountId> { user, _ ->
            sql(
                """
SELECT acc.id
FROM message_account acc
JOIN daycare_group_acl gacl ON gacl.daycare_group_id = acc.daycare_group_id
WHERE gacl.employee_id = ${bind(user.employeeId)} AND acc.active = TRUE
                """
                    .trimIndent()
            )
        }
}
