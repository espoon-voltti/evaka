// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.pis.employeePinIsCorrect
import fi.espoo.evaka.pis.markEmployeeLastLogin
import fi.espoo.evaka.pis.resetEmployeePinFailureCount
import fi.espoo.evaka.pis.updateEmployeePinFailureCountAndCheckIfLocked
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.Tracing
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.QuerySql
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.ActionRuleMapping
import fi.espoo.evaka.shared.security.actionrule.DatabaseActionRule
import fi.espoo.evaka.shared.security.actionrule.DatabaseActionRule.QueryContext
import fi.espoo.evaka.shared.security.actionrule.ScopedActionRule
import fi.espoo.evaka.shared.security.actionrule.StaticActionRule
import fi.espoo.evaka.shared.security.actionrule.UnscopedActionRule
import fi.espoo.evaka.shared.withSpan
import fi.espoo.evaka.shared.withValue
import io.opentracing.Tracer
import java.util.EnumSet

class AccessControl(private val actionRuleMapping: ActionRuleMapping, private val tracer: Tracer) {
    fun requirePermissionFor(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: Action.UnscopedAction,
    ) = checkPermissionFor(tx, user, clock, action).assert()

    fun hasPermissionFor(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: Action.UnscopedAction,
    ): Boolean = checkPermissionFor(tx, user, clock, action).isPermitted()

    fun checkPermissionFor(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: Action.UnscopedAction,
    ): AccessControlDecision =
        tracer.withSpan("checkPermissionFor", Tracing.action withValue action) {
            val queryCache = UnscopedEvaluator(QueryContext(tx, user, clock.now()))
            fun evaluate(rule: UnscopedActionRule): AccessControlDecision =
                when (rule) {
                    is StaticActionRule -> rule.evaluate(user)
                    is DatabaseActionRule.Unscoped<*> -> queryCache.evaluate(rule)
                }

            val rules =
                actionRuleMapping.rulesOf(action).sortedByDescending { it is StaticActionRule }
            val decision = rules.map { evaluate(it) }.find { it != AccessControlDecision.None }
            return@withSpan decision ?: AccessControlDecision.None
        }

    inline fun <reified A> getPermittedActions(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
    ) where A : Action.UnscopedAction, A : Enum<A> =
        getPermittedActions(tx, user, clock, A::class.java)

    fun <A> getPermittedActions(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        actionClass: Class<A>,
    ): Set<A> where A : Action.UnscopedAction, A : Enum<A> =
        tracer.withSpan("getPermittedActions", Tracing.actionClass withValue actionClass) {
            val allActions = EnumSet.allOf(actionClass)
            val permittedActions = EnumSet.noneOf(actionClass)
            val queryCache = UnscopedEvaluator(QueryContext(tx, user, clock.now()))
            fun isPermitted(rule: UnscopedActionRule): Boolean =
                when (rule) {
                    is StaticActionRule -> rule.evaluate(user).isPermitted()
                    is DatabaseActionRule.Unscoped<*> -> queryCache.evaluate(rule).isPermitted()
                }

            for (action in allActions) {
                val rules =
                    actionRuleMapping.rulesOf(action).sortedByDescending { it is StaticActionRule }
                if (rules.any { isPermitted(it) }) {
                    permittedActions += action
                }
            }
            return@withSpan permittedActions
        }

    inline fun <reified A> isPermittedForSomeTarget(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: A,
    ): Boolean where A : Action.ScopedAction<*>, A : Enum<A> =
        getPermittedActionsForSomeTarget(tx, user, clock, A::class.java).contains(action)

    inline fun <reified A> getPermittedActionsForSomeTarget(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
    ): Set<A> where A : Action.ScopedAction<*>, A : Enum<A> =
        getPermittedActionsForSomeTarget(tx, user, clock, A::class.java)

    fun <A> getPermittedActionsForSomeTarget(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        actionClass: Class<A>,
    ): Set<A> where A : Action.ScopedAction<*>, A : Enum<A> =
        tracer.withSpan(
            "getPermittedActionsForSomeTarget",
            Tracing.actionClass withValue actionClass,
        ) {
            val allActions = EnumSet.allOf(actionClass)
            val queryCtx = QueryContext(tx, user, clock.now())
            val unscopedCache = UnscopedEvaluator(queryCtx)
            val scopedCache = ScopedSomeTargetEvaluator(queryCtx)
            val permittedActions = mutableSetOf<A>()
            fun isPermittedForSomeTarget(rule: ScopedActionRule<*>): Boolean =
                when (rule) {
                    is StaticActionRule -> rule.evaluate(user).isPermitted()
                    is DatabaseActionRule.Unscoped<*> -> unscopedCache.evaluate(rule).isPermitted()
                    is DatabaseActionRule.Scoped<*, *> -> scopedCache.isPermittedForSomeTarget(rule)
                }

            for (action in allActions) {
                val rules =
                    actionRuleMapping.rulesOf(action).sortedByDescending { it is StaticActionRule }
                if (rules.any { isPermittedForSomeTarget(it) }) {
                    permittedActions += action
                }
            }
            return@withSpan permittedActions
        }

    fun <T> requirePermissionFor(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: Action.ScopedAction<T>,
        target: T,
    ) = requirePermissionFor(tx, user, clock, action, listOf(target))

    fun <T> requirePermissionFor(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: Action.ScopedAction<T>,
        targets: Iterable<T>,
    ) = checkPermissionFor(tx, user, clock, action, targets).values.forEach { it.assert() }

    fun <T> requirePermissionForSomeTarget(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: Action.ScopedAction<T>,
        targets: Iterable<T>,
    ) {
        if (!checkPermissionFor(tx, user, clock, action, targets).values.any { it.isPermitted() }) {
            throw Forbidden()
        }
    }

    fun <T> hasPermissionFor(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: Action.ScopedAction<T>,
        target: T,
    ): Boolean = checkPermissionFor(tx, user, clock, action, target).isPermitted()

    fun <T> hasPermissionFor(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: Action.ScopedAction<T>,
        targets: Iterable<T>,
    ): Boolean =
        checkPermissionFor(tx, user, clock, action, targets).values.all { it.isPermitted() }

    fun <T> checkPermissionFor(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: Action.ScopedAction<T>,
        target: T,
    ): AccessControlDecision =
        checkPermissionFor(tx, user, clock, action, listOf(target)).values.single()

    fun <T> checkPermissionFor(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: Action.ScopedAction<T>,
        targets: Iterable<T>,
    ): Map<T, AccessControlDecision> =
        tracer.withSpan("checkPermissionFor", Tracing.action withValue action) {
            val decided = mutableMapOf<T, AccessControlDecision>()
            var undecided = targets.toSet()
            val queryCtx = QueryContext(tx, user, clock.now())
            val unscopedEvaluator = UnscopedEvaluator(queryCtx)
            val scopedEvaluator = ScopedEvaluator(queryCtx)
            fun decideAll(decision: AccessControlDecision) {
                if (decision != AccessControlDecision.None) {
                    decided += undecided.associateWith { decision }
                    undecided = emptySet()
                }
            }

            val rules =
                actionRuleMapping
                    .rulesOf(action)
                    .sortedByDescending { it is StaticActionRule }
                    .iterator()
            while (rules.hasNext() && undecided.isNotEmpty()) {
                when (val rule = rules.next()) {
                    is StaticActionRule -> decideAll(rule.evaluate(user))
                    is DatabaseActionRule.Unscoped<*> -> decideAll(unscopedEvaluator.evaluate(rule))
                    is DatabaseActionRule.Scoped<in T, *> ->
                        scopedEvaluator.evaluateWithTargets(rule, undecided).forEach {
                            (target, decision) ->
                            if (decision != AccessControlDecision.None) {
                                decided[target] = decision
                                undecided = undecided - target
                            }
                        }
                }
            }
            return@withSpan decided + undecided.associateWith { AccessControlDecision.None }
        }

    fun <T> requireAuthorizationFilter(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: Action.ScopedAction<T>,
    ): AccessControlFilter<T> = getAuthorizationFilter(tx, user, clock, action) ?: throw Forbidden()

    fun <T> getAuthorizationFilter(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: Action.ScopedAction<T>,
    ): AccessControlFilter<T>? =
        tracer.withSpan("getPermittedActions", Tracing.action withValue action) {
            val queryCtx = QueryContext(tx, user, clock.now())
            val unscopedEvaluator = UnscopedEvaluator(queryCtx)
            val filters: MutableList<QuerySql> = mutableListOf()
            val rules =
                actionRuleMapping.rulesOf(action).sortedByDescending { it is StaticActionRule }
            for (rule in rules) {
                when (rule) {
                    is StaticActionRule ->
                        rule.evaluate(user).let { decision ->
                            if (decision.isPermitted()) {
                                return@withSpan AccessControlFilter.PermitAll
                            } else {
                                decision.assertIfTerminal()
                            }
                        }
                    is DatabaseActionRule.Unscoped<*> ->
                        unscopedEvaluator.evaluate(rule).let { decision ->
                            if (decision.isPermitted()) {
                                return@withSpan AccessControlFilter.PermitAll
                            } else {
                                decision.assertIfTerminal()
                            }
                        }
                    is DatabaseActionRule.Scoped<in T, *> ->
                        rule.queryWithParams(queryCtx)?.let { filter -> filters += filter }
                }
            }
            return@withSpan if (filters.isEmpty()) {
                null
            } else {
                AccessControlFilter.Some(
                    QuerySql {
                        sql(filters.joinToString(separator = " UNION ALL ") { "(${subquery(it)})" })
                    }
                )
            }
        }

    inline fun <T, reified A> getPermittedActions(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        target: T,
    ) where A : Action.ScopedAction<T>, A : Enum<A> =
        getPermittedActions(tx, user, clock, A::class.java, setOf(target)).values.first()

    inline fun <T, reified A> getPermittedActions(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        targets: Iterable<T>,
    ) where A : Action.ScopedAction<T>, A : Enum<A> =
        getPermittedActions(tx, user, clock, A::class.java, targets.toSet())

    fun <T, A> getPermittedActions(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        actionClass: Class<A>,
        targets: Set<T>,
    ): Map<T, Set<A>> where A : Action.ScopedAction<T>, A : Enum<A> {
        return tracer.withSpan("getPermittedActions", Tracing.actionClass withValue actionClass) {
            val allActions: Set<A> = EnumSet.allOf(actionClass)
            val queryCtx = QueryContext(tx, user, clock.now())
            val unscopedEvaluator = UnscopedEvaluator(queryCtx)
            val scopedEvaluator = ScopedEvaluator(queryCtx)
            val globalPermissions = EnumSet.noneOf(actionClass)
            val individualPermissions = targets.associateWith { EnumSet.noneOf(actionClass) }
            for (action in allActions) {
                val rules =
                    actionRuleMapping
                        .rulesOf(action)
                        .sortedByDescending { it is StaticActionRule }
                        .iterator()
                for (rule in rules) {
                    when (rule) {
                        is StaticActionRule ->
                            if (rule.evaluate(user).isPermitted()) {
                                globalPermissions.add(action)
                                break
                            }
                        is DatabaseActionRule.Unscoped<*> ->
                            if (unscopedEvaluator.evaluate(rule).isPermitted()) {
                                globalPermissions.add(action)
                                break
                            }
                        is DatabaseActionRule.Scoped<in T, *> ->
                            scopedEvaluator.evaluateWithTargets(rule, targets).forEach {
                                (target, decision) ->
                                if (decision.isPermitted()) {
                                    individualPermissions[target]?.add(action)
                                }
                            }
                    }
                }
            }
            individualPermissions.values.forEach { it.addAll(globalPermissions) }
            return@withSpan individualPermissions
        }
    }

    private data class QueryCacheKey(val queryClass: Class<*>, val cacheKey: Any)

    private class UnscopedEvaluator(private val queryCtx: QueryContext) {
        private val cache = mutableMapOf<QueryCacheKey, DatabaseActionRule.Deferred<Any>>()

        fun <P : Any> evaluate(rule: DatabaseActionRule.Unscoped<P>): AccessControlDecision {
            @Suppress("UNCHECKED_CAST")
            val query = rule.query as DatabaseActionRule.Unscoped.Query<Any>
            val cacheKey =
                QueryCacheKey(query.javaClass, query.cacheKey(queryCtx.user, queryCtx.now))
            val deferred = cache.getOrPut(cacheKey) { rule.query.execute(queryCtx) }
            return deferred.evaluate(rule.params)
        }
    }

    private class ScopedSomeTargetEvaluator(private val queryCtx: QueryContext) {
        private data class CacheKey(val params: Any, val queryType: Class<*>, val cacheKey: Any)

        private val cache = mutableMapOf<CacheKey, Boolean>()

        fun isPermittedForSomeTarget(rule: DatabaseActionRule.Scoped<*, *>): Boolean {
            val cacheKey =
                CacheKey(
                    rule.params,
                    rule.query.javaClass,
                    rule.query.cacheKey(queryCtx.user, queryCtx.now),
                )
            return cache.getOrPut(cacheKey) {
                val sql = rule.queryWithParams(queryCtx) ?: return@getOrPut false
                queryCtx.tx.createQuery { sql("SELECT EXISTS (${subquery(sql)})") }.exactlyOne()
            }
        }
    }

    private class ScopedEvaluator(private val queryCtx: QueryContext) {
        private val cache = mutableMapOf<QueryCacheKey, Map<*, DatabaseActionRule.Deferred<Any>>>()

        fun <T> evaluateWithTargets(
            rule: DatabaseActionRule.Scoped<in T, *>,
            targets: Set<T>,
        ): Sequence<Pair<T, AccessControlDecision>> {
            @Suppress("UNCHECKED_CAST")
            val query = rule.query as DatabaseActionRule.Scoped.Query<in T, Any>
            val cacheKey =
                QueryCacheKey(query.javaClass, query.cacheKey(queryCtx.user, queryCtx.now))
            val deferreds =
                this.cache.getOrPut(cacheKey) { query.executeWithTargets(queryCtx, targets) }
            return targets.asSequence().map { target ->
                target to (deferreds[target]?.evaluate(rule.params) ?: AccessControlDecision.None)
            }
        }
    }

    enum class PinError {
        PIN_LOCKED,
        WRONG_PIN,
    }

    fun verifyPinCode(
        tx: Database.Transaction,
        employeeId: EmployeeId,
        pinCode: String,
        clock: EvakaClock,
    ): PinError? {
        return if (tx.employeePinIsCorrect(employeeId, pinCode)) {
            tx.markEmployeeLastLogin(clock, employeeId)
            tx.resetEmployeePinFailureCount(employeeId)
            null
        } else {
            if (tx.updateEmployeePinFailureCountAndCheckIfLocked(employeeId)) {
                PinError.PIN_LOCKED
            } else {
                PinError.WRONG_PIN
            }
        }
    }

    fun verifyPinCodeAndThrow(
        dbc: Database.Connection,
        employeeId: EmployeeId,
        pinCode: String,
        clock: EvakaClock,
    ) {
        val errorCode = dbc.transaction { verifyPinCode(it, employeeId, pinCode, clock) }
        if (errorCode != null) throw Forbidden("Invalid pin code", errorCode.name)
    }
}
