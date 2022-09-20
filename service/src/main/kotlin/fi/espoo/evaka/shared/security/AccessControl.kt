// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.pis.employeePinIsCorrect
import fi.espoo.evaka.pis.updateEmployeePinFailureCountAndCheckIfLocked
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.ActionRuleMapping
import fi.espoo.evaka.shared.security.actionrule.DatabaseActionRule
import fi.espoo.evaka.shared.security.actionrule.StaticActionRule
import fi.espoo.evaka.shared.security.actionrule.UnscopedActionRule
import java.util.EnumSet
import org.jdbi.v3.core.Jdbi

class AccessControl(private val actionRuleMapping: ActionRuleMapping, private val jdbi: Jdbi) {
    fun getPermittedFeatures(
        tx: Database.Read,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock
    ): EmployeeFeatures {
        return EmployeeFeatures(
            applications =
                checkPermissionFor(tx, user, clock, Action.Global.APPLICATIONS_PAGE).isPermitted(),
            employees =
                checkPermissionFor(tx, user, clock, Action.Global.EMPLOYEES_PAGE).isPermitted(),
            financeBasics =
                checkPermissionFor(tx, user, clock, Action.Global.FINANCE_BASICS_PAGE)
                    .isPermitted(),
            finance = checkPermissionFor(tx, user, clock, Action.Global.FINANCE_PAGE).isPermitted(),
            holidayPeriods =
                checkPermissionFor(tx, user, clock, Action.Global.HOLIDAY_PERIODS_PAGE)
                    .isPermitted(),
            messages =
                checkPermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.MESSAGES_PAGE,
                        allowedToAdmin = false
                    )
                    .isPermitted(),
            personSearch =
                checkPermissionFor(tx, user, clock, Action.Global.PERSON_SEARCH_PAGE).isPermitted(),
            reports = checkPermissionFor(tx, user, clock, Action.Global.REPORTS_PAGE).isPermitted(),
            settings =
                checkPermissionFor(tx, user, clock, Action.Global.SETTINGS_PAGE).isPermitted(),
            unitFeatures =
                checkPermissionFor(tx, user, clock, Action.Global.UNIT_FEATURES_PAGE).isPermitted(),
            units = checkPermissionFor(tx, user, clock, Action.Global.UNITS_PAGE).isPermitted(),
            createUnits =
                checkPermissionFor(tx, user, clock, Action.Global.CREATE_UNIT).isPermitted(),
            vasuTemplates =
                checkPermissionFor(tx, user, clock, Action.Global.VASU_TEMPLATES_PAGE)
                    .isPermitted(),
            personalMobileDevice =
                checkPermissionFor(tx, user, clock, Action.Global.PERSONAL_MOBILE_DEVICE_PAGE)
                    .isPermitted(),
            pinCode =
                checkPermissionFor(tx, user, clock, Action.Global.PIN_CODE_PAGE).isPermitted(),
        )
    }

    fun requirePermissionFor(
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: Action.UnscopedAction
    ) =
        Database(jdbi).connect { dbc ->
            dbc.read { tx -> checkPermissionFor(tx, user, clock, action) }.assert()
        }

    fun hasPermissionFor(
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: Action.UnscopedAction
    ): Boolean =
        Database(jdbi).connect { dbc ->
            dbc.read { tx -> checkPermissionFor(tx, user, clock, action) }.isPermitted()
        }

    fun checkPermissionFor(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: Action.UnscopedAction,
        allowedToAdmin: Boolean = true
    ): AccessControlDecision {
        if (allowedToAdmin && user.isAdmin) {
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

    fun getPermittedGlobalActions(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock
    ): Set<Action.Global> {
        val allActions = EnumSet.allOf(Action.Global::class.java)
        if (user.isAdmin) {
            return EnumSet.allOf(Action.Global::class.java)
        }

        val permittedActions = EnumSet.noneOf(Action.Global::class.java)
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

    fun <T> requirePermissionFor(
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: Action.ScopedAction<T>,
        target: T
    ) = requirePermissionFor(user, clock, action, listOf(target))
    fun <T> requirePermissionFor(
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: Action.ScopedAction<T>,
        targets: Iterable<T>
    ) =
        Database(jdbi).connect { dbc ->
            dbc.read { tx ->
                checkPermissionFor(tx, user, clock, action, targets).values.forEach { it.assert() }
            }
        }

    fun <T> hasPermissionFor(
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: Action.ScopedAction<T>,
        target: T
    ): Boolean = hasPermissionFor(user, clock, action, listOf(target))
    fun <T> hasPermissionFor(
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: Action.ScopedAction<T>,
        targets: Iterable<T>
    ): Boolean =
        Database(jdbi).connect { dbc ->
            dbc.read { tx ->
                checkPermissionFor(tx, user, clock, action, targets).values.all { it.isPermitted() }
            }
        }

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

    fun requireAuthorizationFilter(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        action: Action.Unit
    ): AccessControlFilter<DaycareId> =
        getAuthorizationFilter(tx, user, clock, action) ?: throw Forbidden()
    fun getAuthorizationFilter(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        // WARN: don't enable this for other action types. This design can have severe performance
        // issues!
        action: Action.Unit
    ): AccessControlFilter<DaycareId>? {
        if (user.isAdmin) {
            return AccessControlFilter.PermitAll
        }
        val queryCtx = DatabaseActionRule.QueryContext(tx, user, clock.now())
        val unscopedEvaluator = UnscopedEvaluator(queryCtx)
        val scopedEvaluator = ScopedEvaluator<DaycareId>(queryCtx)
        var result: AccessControlFilter.Some<DaycareId>? = null
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
                is DatabaseActionRule.Scoped<in DaycareId, *> ->
                    scopedEvaluator.evaluateWithParams(rule)?.let { filter ->
                        when (filter) {
                            AccessControlFilter.PermitAll -> return AccessControlFilter.PermitAll
                            is AccessControlFilter.Some ->
                                result =
                                    AccessControlFilter.Some(
                                        filter.filter + (result?.filter ?: emptySet())
                                    )
                        }
                    }
            }
        }
        return result
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
                DatabaseActionRule.Unscoped.Query<out Any?>, DatabaseActionRule.Deferred<out Any>
            >(
            )

        fun <P : Any> evaluate(rule: DatabaseActionRule.Unscoped<P>): AccessControlDecision {
            @Suppress("UNCHECKED_CAST")
            val deferred =
                cache.getOrPut(rule.query) { rule.query.execute(queryCtx) }
                    as DatabaseActionRule.Deferred<P>
            return deferred.evaluate(rule.params)
        }
    }
    private class ScopedEvaluator<T>(private val queryCtx: DatabaseActionRule.QueryContext) {
        private val targetCache =
            mutableMapOf<
                DatabaseActionRule.Scoped.Query<in T, Any>,
                Map<in T, DatabaseActionRule.Deferred<Any>>
            >(
            )
        private val paramsCache =
            mutableMapOf<DatabaseActionRule.Scoped.Query<in T, Any>, AccessControlFilter<T>?>()

        fun evaluateWithTargets(
            rule: DatabaseActionRule.Scoped<in T, *>,
            targets: Set<T>
        ): Sequence<Pair<T, AccessControlDecision>> {
            @Suppress("UNCHECKED_CAST")
            val query = rule.query as DatabaseActionRule.Scoped.Query<in T, Any>
            val deferreds =
                targetCache.getOrPut(query) { query.executeWithTargets(queryCtx, targets) }
            return targets.asSequence().map { target ->
                target to (deferreds[target]?.evaluate(rule.params) ?: AccessControlDecision.None)
            }
        }
        fun evaluateWithParams(rule: DatabaseActionRule.Scoped<in T, *>): AccessControlFilter<T>? {
            @Suppress("UNCHECKED_CAST")
            val query = rule.query as DatabaseActionRule.Scoped.Query<in T, Any>
            return paramsCache.getOrPut(query) { query.executeWithParams(queryCtx, rule.params) }
        }
    }
    enum class PinError {
        PIN_LOCKED,
        WRONG_PIN
    }

    fun verifyPinCode(employeeId: EmployeeId, pinCode: String) {
        val errorCode =
            Database(jdbi).connect {
                it.transaction { tx ->
                    if (tx.employeePinIsCorrect(employeeId, pinCode)) {
                        null
                    } else {
                        val locked = tx.updateEmployeePinFailureCountAndCheckIfLocked(employeeId)
                        if (locked) PinError.PIN_LOCKED else PinError.WRONG_PIN
                    }
                }
            }

        // throw must be outside transaction to not rollback failure count increase
        if (errorCode != null) throw Forbidden("Invalid pin code", errorCode.name)
    }
}
