// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.pis.employeePinIsCorrect
import fi.espoo.evaka.pis.updateEmployeePinFailureCountAndCheckIfLocked
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.security.actionrule.ActionRuleMapping
import fi.espoo.evaka.shared.security.actionrule.DatabaseActionRule
import fi.espoo.evaka.shared.security.actionrule.StaticActionRule
import fi.espoo.evaka.shared.security.actionrule.TargetActionRule
import fi.espoo.evaka.shared.utils.toEnumSet
import org.jdbi.v3.core.Jdbi
import java.util.EnumSet

class AccessControl(
    private val actionRuleMapping: ActionRuleMapping,
    private val acl: AccessControlList,
    private val jdbi: Jdbi
) {
    fun getPermittedFeatures(user: AuthenticatedUser.Employee): EmployeeFeatures =
        @Suppress("DEPRECATION")
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
                UserRole.SPECIAL_EDUCATION_TEACHER
            ),
            reports = user.hasOneOfRoles(
                UserRole.ADMIN,
                UserRole.DIRECTOR,
                UserRole.REPORT_VIEWER,
                UserRole.SERVICE_WORKER,
                UserRole.FINANCE_ADMIN,
                UserRole.UNIT_SUPERVISOR,
                UserRole.SPECIAL_EDUCATION_TEACHER
            ),
            settings = user.isAdmin,
            unitFeatures = user.hasOneOfRoles(UserRole.ADMIN),
            units = user.hasOneOfRoles(
                UserRole.ADMIN,
                UserRole.SERVICE_WORKER,
                UserRole.FINANCE_ADMIN,
                UserRole.UNIT_SUPERVISOR,
                UserRole.STAFF,
                UserRole.SPECIAL_EDUCATION_TEACHER
            ),
            createUnits = hasPermissionFor(user, Action.Global.CREATE_UNIT),
            vasuTemplates = user.hasOneOfRoles(UserRole.ADMIN),
            personalMobileDevice = user.hasOneOfRoles(UserRole.UNIT_SUPERVISOR),

            // Everyone else except FINANCE_ADMIN
            pinCode = user.hasOneOfRoles(
                UserRole.ADMIN,
                UserRole.REPORT_VIEWER,
                UserRole.DIRECTOR,
                UserRole.SERVICE_WORKER,
                UserRole.UNIT_SUPERVISOR,
                UserRole.STAFF,
                UserRole.SPECIAL_EDUCATION_TEACHER,
                UserRole.GROUP_STAFF,
            )
        )

    private fun isMessagingEnabled(user: AuthenticatedUser.Employee): Boolean {
        @Suppress("DEPRECATION")
        return acl.getRolesForPilotFeature(user, PilotFeature.MESSAGING)
            .intersect(setOf(UserRole.STAFF, UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER)).isNotEmpty()
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.StaticAction) = checkPermissionFor(user, action).assert()
    fun hasPermissionFor(user: AuthenticatedUser, action: Action.StaticAction): Boolean = checkPermissionFor(user, action).isPermitted()
    fun checkPermissionFor(user: AuthenticatedUser, action: Action.StaticAction): AccessControlDecision {
        if (user.isAdmin) {
            return AccessControlDecision.PermittedToAdmin
        }
        for (rule in actionRuleMapping.rulesOf(action)) {
            if (rule.isPermitted(user)) {
                return AccessControlDecision.Permitted(rule)
            }
        }
        return AccessControlDecision.None
    }

    fun getPermittedGlobalActions(user: AuthenticatedUser): Set<Action.Global> {
        if (user.isAdmin) {
            return EnumSet.allOf(Action.Global::class.java)
        }

        return Action.Global.values()
            .filter { action -> checkPermissionFor(user, action).isPermitted() }
            .toEnumSet()
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
    ): Map<T, AccessControlDecision> {
        if (user.isAdmin) {
            return targets.associateWith { AccessControlDecision.PermittedToAdmin }
        }
        val decisions = Decisions(targets.toSet())
        val rules = actionRuleMapping.rulesOf(action).sortedByDescending { it is StaticActionRule }.iterator()
        while (rules.hasNext() && decisions.undecided.isNotEmpty()) {
            when (val rule = rules.next()) {
                is StaticActionRule -> if (rule.isPermitted(user)) {
                    decisions.decideAll(AccessControlDecision.Permitted(rule))
                }
                is TargetActionRule<in T> -> {
                    for (target in targets) {
                        decisions.decide(target, rule.evaluate(user, target))
                    }
                }
                is DatabaseActionRule<in T, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    val query = rule.query as DatabaseActionRule.Query<T, Any?>
                    dbc.read { tx -> query.execute(tx, user, decisions.undecided) }
                        .forEach { (target, deferred) -> decisions.decide(target, deferred.evaluate(rule.params)) }
                }
            }
        }
        return decisions.finish()
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
        val undecidedActions = EnumSet.allOf(actionClass)
        val permittedActions = EnumSet.noneOf(actionClass)
        for (action in allActions) {
            val staticRules = actionRuleMapping.rulesOf(action).mapNotNull { it as? StaticActionRule }
            if (staticRules.any { it.isPermitted(user) }) {
                permittedActions += action
                undecidedActions -= action
            }
        }

        val result = targets.associateWith { EnumSet.copyOf(permittedActions) }

        for (action in allActions) {
            val targetRules = actionRuleMapping.rulesOf(action).mapNotNull { it as? TargetActionRule<in T> }
            for (rule in targetRules) {
                for (target in targets) {
                    if (rule.evaluate(user, target).isPermitted()) {
                        result[target]?.add(action)
                    }
                }
            }
        }

        val databaseRuleTypes = EnumSet.copyOf(undecidedActions)
            .flatMap { action ->
                actionRuleMapping.rulesOf(action).mapNotNull { it as? DatabaseActionRule<in T, *> }
            }
            .distinctBy { it.classifier }
            .iterator()

        while (undecidedActions.isNotEmpty() && databaseRuleTypes.hasNext()) {
            val ruleType = databaseRuleTypes.next()
            @Suppress("UNCHECKED_CAST")
            val deferred = ruleType.query.execute(tx, user, targets.toSet()) as Map<T, DatabaseActionRule.Deferred<Any?>>

            for (action in EnumSet.copyOf(undecidedActions)) {
                val compatibleRules = actionRuleMapping.rulesOf(action)
                    .mapNotNull { it as? DatabaseActionRule<in T, *> }
                    .filter { it.classifier == ruleType.classifier }
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
