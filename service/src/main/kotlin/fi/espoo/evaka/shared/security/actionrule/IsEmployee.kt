// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.ApplicationNoteId
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.AssistanceNeedPreschoolDecisionId
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.PairingId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.QuerySql
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControlDecision

private typealias FilterByEmployee =
    QuerySql.Builder.(user: AuthenticatedUser.Employee, now: HelsinkiDateTime) -> QuerySql

data object IsEmployee : DatabaseActionRule.Params {
    private fun <T : Id<*>> rule(
        filter: FilterByEmployee
    ): DatabaseActionRule.Scoped<T, IsEmployee> =
        DatabaseActionRule.Scoped.Simple(this, Query(filter))

    private data class Query<T : Id<*>>(private val filter: FilterByEmployee) :
        DatabaseActionRule.Scoped.Query<T, IsEmployee> {
        override fun cacheKey(user: AuthenticatedUser, now: HelsinkiDateTime): Any =
            when (user) {
                is AuthenticatedUser.Employee -> QuerySql { filter(user, now) }
                else -> Pair(user, now)
            }

        override fun executeWithTargets(
            ctx: DatabaseActionRule.QueryContext,
            targets: Set<T>,
        ): Map<T, DatabaseActionRule.Deferred<IsEmployee>> =
            when (ctx.user) {
                is AuthenticatedUser.Employee -> {
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
                                .associateWith { DatabaseActionRule.Deferred.Permitted }
                        }
                }
                else -> emptyMap()
            }

        override fun queryWithParams(
            ctx: DatabaseActionRule.QueryContext,
            params: IsEmployee,
        ): QuerySql? =
            when (ctx.user) {
                is AuthenticatedUser.Employee -> QuerySql { filter(ctx.user, ctx.now) }
                else -> null
            }
    }

    fun any() =
        object : StaticActionRule {
            override fun evaluate(user: AuthenticatedUser): AccessControlDecision =
                when (user) {
                    is AuthenticatedUser.Employee -> AccessControlDecision.Permitted(this)
                    else -> AccessControlDecision.None
                }
        }

    fun self() =
        object : DatabaseActionRule.Scoped<EmployeeId, IsEmployee> {
            override val params = IsEmployee
            override val query =
                object : DatabaseActionRule.Scoped.Query<EmployeeId, IsEmployee> {
                    override fun cacheKey(user: AuthenticatedUser, now: HelsinkiDateTime): Any =
                        Pair(user, now)

                    override fun executeWithTargets(
                        ctx: DatabaseActionRule.QueryContext,
                        targets: Set<EmployeeId>,
                    ): Map<EmployeeId, DatabaseActionRule.Deferred<IsEmployee>> =
                        when (ctx.user) {
                            is AuthenticatedUser.Employee ->
                                targets
                                    .filter { it == ctx.user.id }
                                    .associateWith { DatabaseActionRule.Deferred.Permitted }
                            else -> emptyMap()
                        }

                    override fun queryWithParams(
                        ctx: DatabaseActionRule.QueryContext,
                        params: IsEmployee,
                    ): QuerySql? =
                        when (ctx.user) {
                            is AuthenticatedUser.Employee ->
                                QuerySql { sql("SELECT ${bind(ctx.user.id)} AS id") }
                            else -> null
                        }
                }
        }

    fun ownerOfAnyMobileDevice() =
        DatabaseActionRule.Unscoped(
            this,
            object : DatabaseActionRule.Unscoped.Query<IsEmployee> {
                override fun cacheKey(user: AuthenticatedUser, now: HelsinkiDateTime): Any =
                    Pair(user, now)

                override fun execute(
                    ctx: DatabaseActionRule.QueryContext
                ): DatabaseActionRule.Deferred<IsEmployee> =
                    when (ctx.user) {
                        is AuthenticatedUser.Employee ->
                            ctx.tx
                                .createQuery {
                                    sql(
                                        """
SELECT EXISTS (
    SELECT 1
    FROM mobile_device
    WHERE employee_id = ${bind(ctx.user.id)}
)
                                """
                                            .trimIndent()
                                    )
                                }
                                .exactlyOne<Boolean>()
                                .let { isPermitted ->
                                    if (isPermitted) DatabaseActionRule.Deferred.Permitted
                                    else DatabaseActionRule.Deferred.None
                                }
                        else -> DatabaseActionRule.Deferred.None
                    }
            },
        )

    fun ownerOfMobileDevice() =
        rule<MobileDeviceId> { user, _ ->
            sql(
                """
SELECT id
FROM mobile_device
WHERE employee_id = ${bind(user.id)}
            """
                    .trimIndent()
            )
        }

    fun ownerOfPairing() =
        rule<PairingId> { user, _ ->
            sql(
                """
SELECT id
FROM pairing
WHERE employee_id = ${bind(user.id)}
            """
                    .trimIndent()
            )
        }

    fun authorOfApplicationNote() =
        rule<ApplicationNoteId> { user, _ ->
            sql(
                """
SELECT id
FROM application_note
WHERE created_by = ${bind(user.id)}
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
WHERE employee.id = ${bind(user.id)} AND acc.active = TRUE
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
WHERE gacl.employee_id = ${bind(user.id)} AND acc.active = TRUE
                """
                    .trimIndent()
            )
        }

    fun hasMunicipalMessageAccount() =
        rule<MessageAccountId> { user, _ ->
            sql(
                """
SELECT acc.id
FROM employee e
JOIN message_account acc ON acc.type = 'MUNICIPAL'
WHERE e.id = ${bind(user.id)} AND e.roles && '{ADMIN, MESSAGING}'::user_role[]
                """
                    .trimIndent()
            )
        }

    fun hasServiceWorkerMessageAccount() =
        rule<MessageAccountId> { user, _ ->
            sql(
                """
SELECT acc.id
FROM employee e
JOIN message_account acc ON acc.type = 'SERVICE_WORKER'
WHERE e.id = ${bind(user.id)} AND e.roles && '{SERVICE_WORKER}'::user_role[]
                """
                    .trimIndent()
            )
        }

    fun andIsDecisionMakerForAssistanceNeedDecision() =
        rule<AssistanceNeedDecisionId> { employee, _ ->
            sql(
                """
SELECT id
FROM assistance_need_decision
WHERE decision_maker_employee_id = ${bind(employee.id)}
AND sent_for_decision IS NOT NULL
            """
                    .trimIndent()
            )
        }

    fun andIsDecisionMakerForAssistanceNeedPreschoolDecision() =
        rule<AssistanceNeedPreschoolDecisionId> { employee, _ ->
            sql(
                """
SELECT id
FROM assistance_need_preschool_decision
WHERE decision_maker_employee_id = ${bind(employee.id)}
AND sent_for_decision IS NOT NULL
            """
                    .trimIndent()
            )
        }

    fun andIsDecisionMakerForAnyAssistanceNeedDecision() =
        DatabaseActionRule.Unscoped(
            this,
            object : DatabaseActionRule.Unscoped.Query<IsEmployee> {
                override fun cacheKey(user: AuthenticatedUser, now: HelsinkiDateTime): Any =
                    Pair(user, now)

                override fun execute(
                    ctx: DatabaseActionRule.QueryContext
                ): DatabaseActionRule.Deferred<IsEmployee> =
                    when (ctx.user) {
                        is AuthenticatedUser.Employee ->
                            ctx.tx
                                .createQuery {
                                    sql(
                                        """
SELECT EXISTS (
    SELECT 1
    FROM assistance_need_decision
    WHERE decision_maker_employee_id = ${bind(ctx.user.id)}
    AND sent_for_decision IS NOT NULL
)
                                """
                                            .trimIndent()
                                    )
                                }
                                .exactlyOne<Boolean>()
                                .let { isPermitted ->
                                    if (isPermitted) DatabaseActionRule.Deferred.Permitted
                                    else DatabaseActionRule.Deferred.None
                                }
                        else -> DatabaseActionRule.Deferred.None
                    }
            },
        )

    fun isInSameUnitWithEmployee() =
        rule<EmployeeId> { user, _ ->
            sql(
                """
    SELECT targetacl.employee_id as id
    FROM daycare_acl useracl
    JOIN daycare_acl targetacl ON useracl.daycare_id = targetacl.daycare_id
    WHERE useracl.employee_id = ${bind(user.id)}
"""
            )
        }
}
