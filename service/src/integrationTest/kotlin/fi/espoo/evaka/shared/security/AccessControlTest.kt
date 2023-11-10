// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevMobileDevice
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.security.actionrule.ActionRuleMapping
import fi.espoo.evaka.shared.security.actionrule.ScopedActionRule
import fi.espoo.evaka.shared.security.actionrule.UnscopedActionRule
import org.junit.jupiter.api.BeforeEach

abstract class AccessControlTest : PureJdbiTest(resetDbBeforeEach = true) {
    protected lateinit var rules: TestActionRuleMapping
    protected lateinit var accessControl: AccessControl

    @BeforeEach
    fun prepareRules() {
        rules = TestActionRuleMapping()
        accessControl = AccessControl(rules, noopTracer)
    }

    class TestActionRuleMapping : ActionRuleMapping {
        private var unscopedRules: Map<Action.UnscopedAction, List<UnscopedActionRule>> = emptyMap()
        private var scopedRules: Map<Action.ScopedAction<*>, List<ScopedActionRule<*>>> = emptyMap()

        fun add(action: Action.UnscopedAction, rule: UnscopedActionRule) {
            unscopedRules =
                unscopedRules + (action to ((unscopedRules[action] ?: emptyList()) + rule))
        }

        fun <T> add(action: Action.ScopedAction<T>, rule: ScopedActionRule<in T>) {
            scopedRules = scopedRules + (action to ((scopedRules[action] ?: emptyList()) + rule))
        }

        override fun rulesOf(action: Action.UnscopedAction): Sequence<UnscopedActionRule> =
            unscopedRules[action]?.asSequence() ?: emptySequence()

        @Suppress("UNCHECKED_CAST")
        override fun <T> rulesOf(
            action: Action.ScopedAction<in T>
        ): Sequence<ScopedActionRule<in T>> =
            scopedRules[action as Action.ScopedAction<Any>]?.asSequence()?.let {
                it as Sequence<ScopedActionRule<in T>>
            } ?: emptySequence()
    }

    protected fun createTestCitizen(authLevel: CitizenAuthLevel) =
        db.transaction { tx ->
            val id = tx.insert(DevPerson(), DevPersonType.ADULT)
            AuthenticatedUser.Citizen(id, authLevel)
        }

    protected fun createTestEmployee(
        globalRoles: Set<UserRole>,
        unitRoles: Map<DaycareId, UserRole> = emptyMap()
    ) =
        db.transaction { tx ->
            assert(globalRoles.all { it.isGlobalRole() })
            val id = tx.insert(DevEmployee(roles = globalRoles))
            tx.upsertEmployeeUser(id)
            unitRoles.forEach { (daycareId, role) -> tx.insertDaycareAclRow(daycareId, id, role) }
            val globalAndUnitRoles =
                unitRoles.values.fold(globalRoles) { acc, roles -> acc + roles }
            AuthenticatedUser.Employee(id, globalAndUnitRoles)
        }

    protected fun createTestMobile(daycareId: DaycareId) =
        db.transaction { tx ->
            val mobileId = tx.insert(DevMobileDevice(unitId = daycareId))
            AuthenticatedUser.MobileDevice(mobileId)
        }
}
