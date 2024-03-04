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
import fi.espoo.evaka.shared.security.ChildAclConfig

private typealias FilterByMobile =
    QuerySql.Builder.(user: AuthenticatedUser.MobileDevice, now: HelsinkiDateTime) -> QuerySql

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

    private fun <T : Id<*>> rule(filter: FilterByMobile): DatabaseActionRule.Scoped<T, IsMobile> =
        DatabaseActionRule.Scoped.Simple(this, Query(filter))

    /**
     * Creates a rule that is based on the relation between a mobile device and a child.
     *
     * @param cfg configuration for the mobile/child relation
     * @param idChildQuery a query that must return rows with columns `id` and `child_id`
     */
    private fun <T : Id<*>> ruleViaChildAcl(
        cfg: ChildAclConfig,
        idChildQuery:
            QuerySql.Builder.(
                user: AuthenticatedUser.MobileDevice, now: HelsinkiDateTime
            ) -> QuerySql
    ): DatabaseActionRule.Scoped<T, IsMobile> =
        DatabaseActionRule.Scoped.Simple(
            this,
            Query { user, now ->
                val aclQueries = cfg.aclQueries(user, now)
                union(
                    all = true,
                    aclQueries.map { aclQuery ->
                        QuerySql.of {
                            sql(
                                """
SELECT target.id
FROM (${subquery { idChildQuery(user, now) }}) target
JOIN (${subquery(aclQuery)}) acl USING (child_id)
"""
                            )
                        }
                    }
                )
            }
        )

    private data class Query<T : Id<*>>(private val filter: FilterByMobile) :
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
                        .createQuery {
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
        ): QuerySql? =
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

    fun inPlacementUnitOfChild(cfg: ChildAclConfig = ChildAclConfig()) =
        rule<ChildId> { user, now ->
            union(
                all = true,
                cfg.aclQueries(user, now).map { aclQuery ->
                    QuerySql.of {
                        sql("""
SELECT acl.child_id AS id
FROM (${subquery(aclQuery)}) acl
""")
                    }
                }
            )
        }

    fun inPlacementUnitOfChildOfChildDailyNote(cfg: ChildAclConfig = ChildAclConfig()) =
        ruleViaChildAcl<ChildDailyNoteId>(cfg) { _, _ ->
            sql("""
SELECT cdn.id, cdn.child_id
FROM child_daily_note cdn
""")
        }

    fun inPlacementUnitOfChildOfChildStickyNote(cfg: ChildAclConfig = ChildAclConfig()) =
        ruleViaChildAcl<ChildStickyNoteId>(cfg) { _, _ ->
            sql("""
SELECT csn.id, csn.child_id
FROM child_sticky_note csn
""")
        }

    fun inPlacementUnitOfChildOfChildImage(cfg: ChildAclConfig = ChildAclConfig()) =
        ruleViaChildAcl<ChildImageId>(cfg) { _, _ ->
            sql("""
SELECT img.id, img.child_id
FROM child_images img
""")
        }

    fun inUnitOfGroup() =
        rule<GroupId> { user, _ ->
            sql(
                """
SELECT g.id
FROM daycare_group g
WHERE EXISTS (
    SELECT FROM mobile_device md
    LEFT JOIN daycare_acl acl ON md.employee_id = acl.employee_id
    WHERE md.id = ${bind(user.id)} AND (md.unit_id = g.daycare_id OR acl.daycare_id = g.daycare_id)
)
"""
            )
        }

    fun inUnitOfGroupNote() =
        rule<GroupNoteId> { user, _ ->
            sql(
                """
SELECT gn.id
FROM group_note gn
JOIN daycare_group g ON gn.group_id = g.id
WHERE EXISTS (
    SELECT FROM mobile_device md
    LEFT JOIN daycare_acl acl ON md.employee_id = acl.employee_id
    WHERE md.id = ${bind(user.id)} AND (md.unit_id = g.daycare_id OR acl.daycare_id = g.daycare_id)
)
"""
            )
        }

    fun inUnit() =
        rule<DaycareId> { user, _ ->
            sql(
                """
SELECT id
FROM daycare d
WHERE EXISTS (
    SELECT FROM mobile_device md
    LEFT JOIN daycare_acl acl ON md.employee_id = acl.employee_id
    WHERE md.id = ${bind(user.id)} AND (md.unit_id = d.id OR acl.daycare_id = d.id)
)
"""
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
