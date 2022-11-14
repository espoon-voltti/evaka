// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.pis.employeePinIsCorrect
import fi.espoo.evaka.pis.markEmployeeLastLogin
import fi.espoo.evaka.pis.resetEmployeePinFailureCount
import fi.espoo.evaka.pis.updateEmployeePinFailureCountAndCheckIfLocked
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.QuerySql
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.ActionRuleMapping
import fi.espoo.evaka.shared.security.actionrule.DatabaseActionRule
import fi.espoo.evaka.shared.security.actionrule.ScopedActionRule
import fi.espoo.evaka.shared.security.actionrule.StaticActionRule
import fi.espoo.evaka.shared.security.actionrule.UnscopedActionRule
import java.util.EnumSet

class AccessControl(private val actionRuleMapping: ActionRuleMapping) {
    fun requirePermissionFor(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: Action.UnscopedAction
    ) = checkPermissionFor(tx, user, clock, action).assert()

    fun hasPermissionFor(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: Action.UnscopedAction
    ): Boolean = checkPermissionFor(tx, user, clock, action).isPermitted()

    fun checkPermissionFor(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: Action.UnscopedAction
    ): AccessControlDecision {
        if (user.isAdmin) {
            return AccessControlDecision.PermittedToAdmin
        }

        val queryCache = UnscopedEvaluator(DatabaseActionRule.QueryContext(tx, user, clock.now()))
        fun evaluate(rule: UnscopedActionRule): AccessControlDecision =
            when (rule) {
                is StaticActionRule -> rule.evaluate(user)
                is DatabaseActionRule.Unscoped<*> -> queryCache.evaluate(rule)
            }

        val rules = actionRuleMapping.rulesOf(action).sortedByDescending { it is StaticActionRule }
        val decision = rules.map { evaluate(it) }.find { it != AccessControlDecision.None }
        return decision ?: AccessControlDecision.None
    }

    inline fun <reified A> getPermittedActions(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock
    ) where A : Action.UnscopedAction, A : Enum<A> =
        getPermittedActions(tx, user, clock, A::class.java)

    fun <A> getPermittedActions(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        actionClass: Class<A>
    ): Set<A> where A : Action.UnscopedAction, A : Enum<A> {
        val allActions = EnumSet.allOf(actionClass)
        if (user.isAdmin) {
            return allActions
        }

        val permittedActions = EnumSet.noneOf(actionClass)
        val queryCache = UnscopedEvaluator(DatabaseActionRule.QueryContext(tx, user, clock.now()))
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
        return permittedActions
    }

    inline fun <reified A> isPermittedForSomeTarget(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: A
    ): Boolean where A : Action.ScopedAction<*>, A : Enum<A> =
        getPermittedActionsForSomeTarget(tx, user, clock, A::class.java).contains(action)
    inline fun <reified A> getPermittedActionsForSomeTarget(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock
    ): Set<A> where A : Action.ScopedAction<*>, A : Enum<A> =
        getPermittedActionsForSomeTarget(tx, user, clock, A::class.java)
    fun <A> getPermittedActionsForSomeTarget(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        actionClass: Class<A>
    ): Set<A> where A : Action.ScopedAction<*>, A : Enum<A> {
        val allActions = EnumSet.allOf(actionClass)
        if (user.isAdmin) {
            return allActions
        }
        val queryCtx = DatabaseActionRule.QueryContext(tx, user, clock.now())
        val unscopedEvaluator = UnscopedEvaluator(queryCtx)
        val scopedCache = mutableMapOf<DatabaseActionRule.Params, Boolean>()
        val permittedActions = mutableSetOf<A>()
        fun isPermittedForSomeTarget(rule: ScopedActionRule<*>): Boolean =
            when (rule) {
                is StaticActionRule -> rule.evaluate(user).isPermitted()
                is DatabaseActionRule.Unscoped<*> -> unscopedEvaluator.evaluate(rule).isPermitted()
                is DatabaseActionRule.Scoped<*, *> ->
                    scopedCache.getOrPut(rule.params) {
                        rule.params.isPermittedForSomeTarget(queryCtx)
                    }
            }

        for (action in allActions) {
            val rules =
                actionRuleMapping.rulesOf(action).sortedByDescending { it is StaticActionRule }
            if (rules.any { isPermittedForSomeTarget(it) }) {
                permittedActions += action
            }
        }
        return permittedActions
    }

    fun <T> requirePermissionFor(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: Action.ScopedAction<T>,
        target: T
    ) = requirePermissionFor(tx, user, clock, action, listOf(target))
    fun <T> requirePermissionFor(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: Action.ScopedAction<T>,
        targets: Iterable<T>
    ) = checkPermissionFor(tx, user, clock, action, targets).values.forEach { it.assert() }
    fun <T> requirePermissionForSomeTarget(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: Action.ScopedAction<T>,
        targets: Iterable<T>
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
        target: T
    ): Boolean = hasPermissionFor(tx, user, clock, action, listOf(target))
    fun <T> hasPermissionFor(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: Action.ScopedAction<T>,
        targets: Iterable<T>
    ): Boolean =
        checkPermissionFor(tx, user, clock, action, targets).values.all { it.isPermitted() }

    fun <T> checkPermissionFor(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: Action.ScopedAction<T>,
        targets: Iterable<T>
    ): Map<T, AccessControlDecision> {
        if (user.isAdmin) {
            return targets.associateWith { AccessControlDecision.PermittedToAdmin }
        }

        val decided = mutableMapOf<T, AccessControlDecision>()
        var undecided = targets.toSet()
        val queryCtx = DatabaseActionRule.QueryContext(tx, user, clock.now())
        val unscopedEvaluator = UnscopedEvaluator(queryCtx)
        val scopedEvaluator = ScopedEvaluator<T>(queryCtx)
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
        return decided + undecided.associateWith { AccessControlDecision.None }
    }

    fun <T> requireAuthorizationFilter(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: Action.ScopedAction<T>
    ): AccessControlFilter<T> = getAuthorizationFilter(tx, user, clock, action) ?: throw Forbidden()
    fun <T> getAuthorizationFilter(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: Action.ScopedAction<T>
    ): AccessControlFilter<T>? {
        if (user.isAdmin) {
            return AccessControlFilter.PermitAll
        }
        val queryCtx = DatabaseActionRule.QueryContext(tx, user, clock.now())
        val unscopedEvaluator = UnscopedEvaluator(queryCtx)
        val scopedEvaluator = ScopedEvaluator<T>(queryCtx)
        val filters: MutableList<QuerySql<T>> = mutableListOf()
        val rules = actionRuleMapping.rulesOf(action).sortedByDescending { it is StaticActionRule }
        for (rule in rules) {
            when (rule) {
                is StaticActionRule ->
                    if (rule.evaluate(user).isPermitted()) {
                        return AccessControlFilter.PermitAll
                    }
                is DatabaseActionRule.Unscoped<*> ->
                    when (val decision = unscopedEvaluator.evaluate(rule)) {
                        is AccessControlDecision.Denied -> throw decision.toException()
                        AccessControlDecision.None -> {}
                        is AccessControlDecision.Permitted -> return AccessControlFilter.PermitAll
                        AccessControlDecision.PermittedToAdmin ->
                            return AccessControlFilter.PermitAll
                    }
                is DatabaseActionRule.Scoped<in T, *> ->
                    scopedEvaluator.queryWithParams(rule)?.let { filter -> filters += filter }
            }
        }
        return if (filters.isEmpty()) {
            null
        } else {
            AccessControlFilter.Some(
                QuerySql.of {
                    sql(filters.joinToString(separator = " UNION ALL ") { "(${subquery(it)})" })
                }
            )
        }
    }

    inline fun <T, reified A> getPermittedActions(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        target: T
    ) where A : Action.ScopedAction<T>, A : Enum<A> =
        getPermittedActions(tx, user, clock, A::class.java, setOf(target)).values.first()
    inline fun <T, reified A> getPermittedActions(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        targets: Iterable<T>
    ) where A : Action.ScopedAction<T>, A : Enum<A> =
        getPermittedActions(tx, user, clock, A::class.java, targets.toSet())

    fun <T, A> getPermittedActions(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        actionClass: Class<A>,
        targets: Set<T>
    ): Map<T, Set<A>> where A : Action.ScopedAction<T>, A : Enum<A> {
        val allActions: Set<A> = EnumSet.allOf(actionClass)
        if (user.isAdmin) {
            return targets.associateWith { allActions }
        }

        val queryCtx = DatabaseActionRule.QueryContext(tx, user, clock.now())
        val unscopedEvaluator = UnscopedEvaluator(queryCtx)
        val scopedEvaluator = ScopedEvaluator<T>(queryCtx)
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
        return individualPermissions
    }

    private class UnscopedEvaluator(private val queryCtx: DatabaseActionRule.QueryContext) {
        private val cache =
            mutableMapOf<
                DatabaseActionRule.Unscoped.Query<out Any?>, DatabaseActionRule.Deferred<out Any>?
            >()

        fun <P : Any> evaluate(rule: DatabaseActionRule.Unscoped<P>): AccessControlDecision {
            @Suppress("UNCHECKED_CAST")
            val deferred =
                cache.getOrPut(rule.query) { rule.query.execute(queryCtx) }
                    as DatabaseActionRule.Deferred<P>?
            return deferred?.evaluate(rule.params) ?: AccessControlDecision.None
        }
    }
    private class ScopedEvaluator<T>(private val queryCtx: DatabaseActionRule.QueryContext) {
        private val query =
            mutableMapOf<
                DatabaseActionRule.Scoped.Query<in T, Any>,
                Map<in T, DatabaseActionRule.Deferred<Any>>
            >()

        fun evaluateWithTargets(
            rule: DatabaseActionRule.Scoped<in T, *>,
            targets: Set<T>
        ): Sequence<Pair<T, AccessControlDecision>> {
            @Suppress("UNCHECKED_CAST")
            val query = rule.query as DatabaseActionRule.Scoped.Query<in T, Any>
            val deferreds =
                this.query.getOrPut(query) { query.executeWithTargets(queryCtx, targets) }
            return targets.asSequence().map { target ->
                target to (deferreds[target]?.evaluate(rule.params) ?: AccessControlDecision.None)
            }
        }
        fun queryWithParams(rule: DatabaseActionRule.Scoped<in T, *>): QuerySql<T>? {
            @Suppress("UNCHECKED_CAST")
            val query = rule.query as DatabaseActionRule.Scoped.Query<in T, Any>
            return query.queryWithParams(queryCtx, rule.params)
        }
    }
    enum class PinError {
        PIN_LOCKED,
        WRONG_PIN
    }

    fun verifyPinCode(
        tx: Database.Transaction,
        employeeId: EmployeeId,
        pinCode: String,
        clock: EvakaClock
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
        tx: Database.Transaction,
        employeeId: EmployeeId,
        pinCode: String,
        clock: EvakaClock
    ) {
        val errorCode = verifyPinCode(tx, employeeId, pinCode, clock)
        if (errorCode != null) throw Forbidden("Invalid pin code", errorCode.name)
    }
}
