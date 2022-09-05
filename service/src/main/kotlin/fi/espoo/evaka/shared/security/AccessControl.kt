// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.pis.employeePinIsCorrect
import fi.espoo.evaka.pis.updateEmployeePinFailureCountAndCheckIfLocked
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.ActionRuleMapping
import fi.espoo.evaka.shared.security.actionrule.DatabaseActionRule
import fi.espoo.evaka.shared.security.actionrule.StaticActionRule
import org.jdbi.v3.core.Jdbi
import java.util.EnumSet

class AccessControl(
    private val actionRuleMapping: ActionRuleMapping,
    private val acl: AccessControlList,
    private val jdbi: Jdbi
) {
    fun getPermittedFeatures(user: AuthenticatedUser.Employee): EmployeeFeatures =
        EmployeeFeatures(
            applications = user.hasOneOfRoles(
                UserRole.ADMIN,
                UserRole.SERVICE_WORKER,
                UserRole.SPECIAL_EDUCATION_TEACHER
            ),
            employees = user.hasOneOfRoles(UserRole.ADMIN),
            financeBasics = user.hasOneOfRoles(UserRole.ADMIN, UserRole.FINANCE_ADMIN),
            finance = user.hasOneOfRoles(UserRole.ADMIN, UserRole.FINANCE_ADMIN),
            holidayPeriods = user.hasOneOfRoles(UserRole.ADMIN),
            messages = isMessagingEnabled(user),
            personSearch = user.hasOneOfRoles(
                UserRole.ADMIN,
                UserRole.SERVICE_WORKER,
                UserRole.FINANCE_ADMIN,
                UserRole.UNIT_SUPERVISOR,
                UserRole.SPECIAL_EDUCATION_TEACHER,
                UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY,
            ),
            reports = user.hasOneOfRoles(
                UserRole.ADMIN,
                UserRole.DIRECTOR,
                UserRole.REPORT_VIEWER,
                UserRole.SERVICE_WORKER,
                UserRole.FINANCE_ADMIN,
                UserRole.UNIT_SUPERVISOR,
                UserRole.SPECIAL_EDUCATION_TEACHER,
                UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY,
            ),
            settings = user.isAdmin,
            unitFeatures = user.hasOneOfRoles(UserRole.ADMIN),
            units = user.hasOneOfRoles(
                UserRole.ADMIN,
                UserRole.SERVICE_WORKER,
                UserRole.FINANCE_ADMIN,
                UserRole.UNIT_SUPERVISOR,
                UserRole.STAFF,
                UserRole.SPECIAL_EDUCATION_TEACHER,
                UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY,
            ),
            createUnits = hasPermissionFor(user, Action.Global.CREATE_UNIT),
            vasuTemplates = user.hasOneOfRoles(UserRole.ADMIN),
            personalMobileDevice = user.hasOneOfRoles(UserRole.UNIT_SUPERVISOR),

            // Everyone else except FINANCE_ADMIN & EARLY_CHILDHOOD_EDUCATION_SECRETARY
            pinCode = user.hasOneOfRoles(
                UserRole.ADMIN,
                UserRole.REPORT_VIEWER,
                UserRole.DIRECTOR,
                UserRole.SERVICE_WORKER,
                UserRole.UNIT_SUPERVISOR,
                UserRole.STAFF,
                UserRole.SPECIAL_EDUCATION_TEACHER,
            )
        )

    private fun isMessagingEnabled(user: AuthenticatedUser.Employee): Boolean {
        @Suppress("DEPRECATION")
        return acl.getRolesForPilotFeature(user, PilotFeature.MESSAGING)
            .intersect(setOf(UserRole.STAFF, UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER, UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY)).isNotEmpty()
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.UnscopedAction) = Database(jdbi).connect { dbc ->
        checkPermissionFor(dbc, user, action).assert()
    }
    fun hasPermissionFor(user: AuthenticatedUser, action: Action.UnscopedAction): Boolean = Database(jdbi).connect { dbc ->
        checkPermissionFor(dbc, user, action).isPermitted()
    }
    fun checkPermissionFor(dbc: Database.Connection, user: AuthenticatedUser, action: Action.UnscopedAction): AccessControlDecision {
        if (user.isAdmin) {
            return AccessControlDecision.PermittedToAdmin
        }
        val now = HelsinkiDateTime.now()
        val rules = actionRuleMapping.rulesOf(action).sortedByDescending { it is StaticActionRule }
        for (rule in rules) {
            when (rule) {
                is StaticActionRule -> {
                    val decision = rule.evaluate(user)
                    if (decision != AccessControlDecision.None) {
                        return decision
                    }
                }
                is DatabaseActionRule.Unscoped<*> -> {
                    val decision = dbc.read { tx ->
                        rule.executeAndEvaluate(DatabaseActionRule.QueryContext(tx, user, now))
                    }
                    if (decision != AccessControlDecision.None) {
                        return decision
                    }
                }
            }
        }
        return AccessControlDecision.None
    }

    fun getPermittedGlobalActions(tx: Database.Read, user: AuthenticatedUser): Set<Action.Global> {
        val allActions = EnumSet.allOf(Action.Global::class.java)
        if (user.isAdmin) {
            return EnumSet.allOf(Action.Global::class.java)
        }
        val now = HelsinkiDateTime.now()
        val undecidedActions = EnumSet.allOf(Action.Global::class.java)
        val permittedActions = EnumSet.noneOf(Action.Global::class.java)
        for (action in allActions) {
            val staticRules = actionRuleMapping.rulesOf(action).mapNotNull { it as? StaticActionRule }
            if (staticRules.any { it.evaluate(user).isPermitted() }) {
                permittedActions += action
                undecidedActions -= action
            }
        }

        val databaseRuleTypes = EnumSet.copyOf(undecidedActions)
            .flatMap { action ->
                actionRuleMapping.rulesOf(action).mapNotNull { it as? DatabaseActionRule.Unscoped<*> }
            }
            .distinct()
            .iterator()

        val queryCtx = DatabaseActionRule.QueryContext(tx, user, now)
        while (undecidedActions.isNotEmpty() && databaseRuleTypes.hasNext()) {
            val ruleType = databaseRuleTypes.next()

            @Suppress("UNCHECKED_CAST")
            val deferred = ruleType.query.execute(queryCtx) as DatabaseActionRule.Deferred<Any?>

            for (action in EnumSet.copyOf(undecidedActions)) {
                val compatibleRules = actionRuleMapping.rulesOf(action)
                    .mapNotNull { it as? DatabaseActionRule.Unscoped<*> }
                    .filter { it == ruleType }
                for (rule in compatibleRules) {
                    if (deferred.evaluate(rule.params).isPermitted()) {
                        permittedActions += action
                        undecidedActions -= action
                    }
                }
            }
        }
        return permittedActions
    }

    fun <T> requirePermissionFor(user: AuthenticatedUser, action: Action.ScopedAction<T>, target: T) =
        requirePermissionFor(user, action, listOf(target))
    fun <T> requirePermissionFor(user: AuthenticatedUser, action: Action.ScopedAction<T>, targets: Iterable<T>) = Database(jdbi).connect { dbc ->
        checkPermissionFor(dbc, user, action, targets).values.forEach { it.assert() }
    }

    fun <T> hasPermissionFor(user: AuthenticatedUser, action: Action.ScopedAction<T>, target: T): Boolean =
        hasPermissionFor(user, action, listOf(target))
    fun <T> hasPermissionFor(user: AuthenticatedUser, action: Action.ScopedAction<T>, targets: Iterable<T>): Boolean = Database(jdbi).connect { dbc ->
        checkPermissionFor(dbc, user, action, targets).values.all { it.isPermitted() }
    }

    fun <T> checkPermissionFor(
        dbc: Database.Connection,
        user: AuthenticatedUser,
        action: Action.ScopedAction<T>,
        target: T
    ): AccessControlDecision = checkPermissionFor(dbc, user, action, listOf(target)).values.first()
    fun <T> checkPermissionFor(
        dbc: Database.Connection,
        user: AuthenticatedUser,
        action: Action.ScopedAction<T>,
        targets: Iterable<T>
    ): Map<T, AccessControlDecision> = dbc.read { tx -> checkPermissionFor(tx, user, action, targets) }
    fun <T> checkPermissionFor(
        tx: Database.Read,
        user: AuthenticatedUser,
        action: Action.ScopedAction<T>,
        targets: Iterable<T>
    ): Map<T, AccessControlDecision> {
        if (user.isAdmin) {
            return targets.associateWith { AccessControlDecision.PermittedToAdmin }
        }
        val now = HelsinkiDateTime.now()
        val decisions = Decisions(targets.toSet())
        val rules = actionRuleMapping.rulesOf(action).sortedByDescending { it is StaticActionRule }.iterator()
        val queryCtx = DatabaseActionRule.QueryContext(tx, user, now)
        while (rules.hasNext() && decisions.undecided.isNotEmpty()) {
            when (val rule = rules.next()) {
                is StaticActionRule -> decisions.decideAll(rule.evaluate(user))
                is DatabaseActionRule.Scoped<in T, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    val deferreds = rule.query.executeWithTargets(queryCtx, decisions.undecided) as Map<T, DatabaseActionRule.Deferred<Any?>>
                    deferreds
                        .forEach { (target, deferred) -> decisions.decide(target, deferred.evaluate(rule.params)) }
                }
                is DatabaseActionRule.Unscoped<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    val query = rule.query as DatabaseActionRule.Unscoped.Query<Any?>
                    val deferred = query.execute(queryCtx)
                    decisions.decideAll(deferred.evaluate(rule.params))
                }
            }
        }
        return decisions.finish()
    }

    fun requireAuthorizationFilter(tx: Database.Read, user: AuthenticatedUser, clock: EvakaClock, action: Action.Unit): AccessControlFilter<DaycareId> =
        getAuthorizationFilter(tx, user, clock, action) ?: throw Forbidden()
    fun getAuthorizationFilter(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        // WARN: don't enable this for other action types. This design can have severe performance issues!
        action: Action.Unit
    ): AccessControlFilter<DaycareId>? {
        if (user.isAdmin) {
            return AccessControlFilter.PermitAll
        }
        val now = clock.now()
        val queryCtx = DatabaseActionRule.QueryContext(tx, user, now)
        var result: AccessControlFilter.Some<DaycareId>? = null
        val rules = actionRuleMapping.rulesOf(action).sortedByDescending { it is StaticActionRule }.iterator()
        while (rules.hasNext()) {
            when (val rule = rules.next()) {
                is StaticActionRule -> if (rule.evaluate(user).isPermitted()) {
                    return AccessControlFilter.PermitAll
                }
                is DatabaseActionRule.Scoped<in DaycareId, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    val ruleFilter = (rule as DatabaseActionRule.Scoped<in DaycareId, Any>).query.executeWithParams(queryCtx, rule.params)
                    when (ruleFilter) {
                        null -> {}
                        AccessControlFilter.PermitAll -> return AccessControlFilter.PermitAll
                        is AccessControlFilter.Some -> result = AccessControlFilter.Some(ruleFilter.filter + (result?.filter ?: emptySet()))
                    }
                }
                is DatabaseActionRule.Unscoped<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    val query = rule.query as DatabaseActionRule.Unscoped.Query<Any?>
                    val deferred = query.execute(queryCtx)
                    when (val decision = deferred.evaluate(rule.params)) {
                        is AccessControlDecision.Denied -> throw decision.toException()
                        AccessControlDecision.None -> {}
                        is AccessControlDecision.Permitted -> return AccessControlFilter.PermitAll
                        AccessControlDecision.PermittedToAdmin -> return AccessControlFilter.PermitAll
                    }
                }
            }
        }
        return result
    }

    inline fun <T, reified A> getPermittedActions(
        tx: Database.Read,
        user: AuthenticatedUser,
        target: T
    ) where A : Action.ScopedAction<T>, A : Enum<A> = getPermittedActions(tx, user, A::class.java, listOf(target)).values.first()
    inline fun <T, reified A> getPermittedActions(
        tx: Database.Read,
        user: AuthenticatedUser,
        targets: Iterable<T>
    ) where A : Action.ScopedAction<T>, A : Enum<A> = getPermittedActions(tx, user, A::class.java, targets)

    fun <T, A> getPermittedActions(
        tx: Database.Read,
        user: AuthenticatedUser,
        actionClass: Class<A>,
        targets: Iterable<T>
    ): Map<T, Set<A>> where A : Action.ScopedAction<T>, A : Enum<A> {
        val allActions: Set<A> = EnumSet.allOf(actionClass)
        if (user.isAdmin) {
            return targets.associateWith { allActions }
        }
        val now = HelsinkiDateTime.now()
        val undecidedActions = EnumSet.allOf(actionClass)
        val permittedActions = EnumSet.noneOf(actionClass)
        for (action in allActions) {
            val staticRules = actionRuleMapping.rulesOf(action).mapNotNull { it as? StaticActionRule }
            if (staticRules.any { it.evaluate(user).isPermitted() }) {
                permittedActions += action
                undecidedActions -= action
            }
        }

        val result = targets.associateWith { EnumSet.copyOf(permittedActions) }

        val databaseRuleTypes = EnumSet.copyOf(undecidedActions)
            .flatMap { action ->
                actionRuleMapping.rulesOf(action).mapNotNull { it as? DatabaseActionRule.Scoped<in T, *> }
            }
            .distinct()
            .iterator()

        val queryCtx = DatabaseActionRule.QueryContext(tx, user, now)
        while (undecidedActions.isNotEmpty() && databaseRuleTypes.hasNext()) {
            val ruleType = databaseRuleTypes.next()
            @Suppress("UNCHECKED_CAST")
            val deferred = ruleType.query.executeWithTargets(queryCtx, targets.toSet()) as Map<T, DatabaseActionRule.Deferred<Any?>>

            for (action in EnumSet.copyOf(undecidedActions)) {
                val compatibleRules = actionRuleMapping.rulesOf(action)
                    .mapNotNull { it as? DatabaseActionRule.Scoped<in T, *> }
                    .filter { it == ruleType }
                for (rule in compatibleRules) {
                    for (target in targets) {
                        if (deferred[target]?.evaluate(rule.params)?.isPermitted() == true) {
                            result[target]?.add(action)
                        }
                    }
                }
            }
        }
        return result
    }

    private class Decisions<T>(targets: Iterable<T>) {
        private val result = mutableMapOf<T, AccessControlDecision>()
        var undecided: Set<T> = targets.toSet()
            private set

        fun decideAll(decision: AccessControlDecision) {
            if (decision != AccessControlDecision.None) {
                result += undecided.associateWith { decision }
                undecided = emptySet()
            }
        }
        fun decide(target: T, decision: AccessControlDecision) {
            if (decision != AccessControlDecision.None) {
                result[target] = decision
                undecided = undecided - target
            }
        }
        fun finish(): Map<T, AccessControlDecision> = result + undecided.associateWith { AccessControlDecision.None }
    }

    enum class PinError {
        PIN_LOCKED,
        WRONG_PIN
    }

    fun verifyPinCode(
        employeeId: EmployeeId,
        pinCode: String
    ) {
        val errorCode = Database(jdbi).connect {
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
