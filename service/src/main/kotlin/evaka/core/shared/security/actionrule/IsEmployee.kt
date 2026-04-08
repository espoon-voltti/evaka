// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.security.actionrule

import evaka.core.shared.ApplicationNoteId
import evaka.core.shared.AttachmentId
import evaka.core.shared.ChildDocumentId
import evaka.core.shared.DatabaseTable
import evaka.core.shared.EmployeeId
import evaka.core.shared.FinanceNoteId
import evaka.core.shared.Id
import evaka.core.shared.MessageAccountId
import evaka.core.shared.MobileDeviceId
import evaka.core.shared.PairingId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.QuerySql
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.security.AccessControlDecision

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

                else -> {
                    emptyMap()
                }
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
                            is AuthenticatedUser.Employee -> {
                                targets
                                    .filter { it == ctx.user.id }
                                    .associateWith { DatabaseActionRule.Deferred.Permitted }
                            }

                            else -> {
                                emptyMap()
                            }
                        }

                    override fun queryWithParams(
                        ctx: DatabaseActionRule.QueryContext,
                        params: IsEmployee,
                    ): QuerySql? =
                        when (ctx.user) {
                            is AuthenticatedUser.Employee -> {
                                QuerySql { sql("SELECT ${bind(ctx.user.id)} AS id") }
                            }

                            else -> {
                                null
                            }
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
                        is AuthenticatedUser.Employee -> {
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
                        }

                        else -> {
                            DatabaseActionRule.Deferred.None
                        }
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

    fun authorOfFinanceNote() =
        rule<FinanceNoteId> { user, _ ->
            sql(
                """
SELECT id
FROM finance_note
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

    fun hasFinanceMessageAccount() =
        rule<MessageAccountId> { user, _ ->
            sql(
                """
SELECT acc.id
FROM employee e 
JOIN message_account acc ON acc.type = 'FINANCE'
WHERE e.id = ${bind(user.id)} AND e.roles && '{FINANCE_ADMIN}'::user_role[]
                """
                    .trimIndent()
            )
        }

    fun andIsDecisionMakerForChildDocumentDecision() =
        rule<ChildDocumentId> { employee, _ ->
            sql(
                """
SELECT id
FROM child_document
WHERE decision_maker = ${bind(employee.id)}
            """
                    .trimIndent()
            )
        }

    fun andIsDecisionMakerForAnyChildDocumentDecision() =
        DatabaseActionRule.Unscoped(
            this,
            object : DatabaseActionRule.Unscoped.Query<IsEmployee> {
                override fun cacheKey(user: AuthenticatedUser, now: HelsinkiDateTime): Any =
                    Pair(user, now)

                override fun execute(
                    ctx: DatabaseActionRule.QueryContext
                ): DatabaseActionRule.Deferred<IsEmployee> =
                    when (ctx.user) {
                        is AuthenticatedUser.Employee -> {
                            ctx.tx
                                .createQuery {
                                    sql(
                                        """
SELECT EXISTS (
    SELECT FROM child_document
    WHERE decision_maker = ${bind(ctx.user.id)}
)
                                """
                                    )
                                }
                                .exactlyOne<Boolean>()
                                .let { isPermitted ->
                                    if (isPermitted) DatabaseActionRule.Deferred.Permitted
                                    else DatabaseActionRule.Deferred.None
                                }
                        }

                        else -> {
                            DatabaseActionRule.Deferred.None
                        }
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

    fun uploaderOfAttachment() =
        rule<AttachmentId> { employee, _ ->
            sql(
                """
SELECT id
FROM attachment
WHERE uploaded_by = ${bind(employee.evakaUserId)}
            """
            )
        }
}
