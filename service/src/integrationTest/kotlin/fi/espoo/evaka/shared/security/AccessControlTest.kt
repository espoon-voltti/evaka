// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevMobileDevice
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestMobileDevice
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.security.actionrule.ActionRuleMapping
import fi.espoo.evaka.shared.security.actionrule.ScopedActionRule
import fi.espoo.evaka.shared.security.actionrule.StaticActionRule
import org.junit.jupiter.api.BeforeEach

abstract class AccessControlTest : PureJdbiTest(resetDbBeforeEach = true) {
    protected lateinit var rules: TestActionRuleMapping
    protected lateinit var accessControl: AccessControl

    @BeforeEach
    private fun prepareRules() {
        rules = TestActionRuleMapping()
        accessControl = AccessControl(rules, AccessControlList(jdbi), jdbi)
    }

    class TestActionRuleMapping : ActionRuleMapping {
        private var staticRules: Map<Action.StaticAction, List<StaticActionRule>> = emptyMap()
        private var scopedRules: Map<Action.ScopedAction<*>, List<ScopedActionRule<*>>> = emptyMap()

        fun add(action: Action.StaticAction, rule: StaticActionRule) {
            staticRules = staticRules + (action to ((staticRules[action] ?: emptyList()) + rule))
        }

        fun <T> add(action: Action.ScopedAction<T>, rule: ScopedActionRule<T>) {
            scopedRules = scopedRules + (action to ((scopedRules[action] ?: emptyList()) + rule))
        }

        override fun rulesOf(action: Action.StaticAction): Sequence<StaticActionRule> =
            staticRules[action]?.asSequence() ?: emptySequence()

        @Suppress("UNCHECKED_CAST")
        override fun <T> rulesOf(action: Action.ScopedAction<in T>): Sequence<ScopedActionRule<in T>> =
            scopedRules[action as Action.ScopedAction<Any>]?.asSequence()?.let { it as Sequence<ScopedActionRule<in T>> }
                ?: emptySequence()
    }

    protected fun createTestCitizen(authLevel: CitizenAuthLevel) = db.transaction { tx ->
        val id = tx.insertTestPerson(DevPerson())
        tx.upsertCitizenUser(id)
        AuthenticatedUser.Citizen(id, authLevel)
    }

    protected fun createTestEmployee(globalRoles: Set<UserRole>, unitRoles: Map<DaycareId, UserRole> = emptyMap()) = db.transaction { tx ->
        assert(globalRoles.all { it.isGlobalRole() })
        val id = tx.insertTestEmployee(DevEmployee(roles = globalRoles))
        tx.upsertEmployeeUser(id)
        unitRoles.forEach { (daycareId, role) ->
            tx.insertDaycareAclRow(daycareId, id, role)
        }
        val globalAndUnitRoles = unitRoles.values.fold(globalRoles) { acc, roles -> acc + roles }
        AuthenticatedUser.Employee(id, globalAndUnitRoles)
    }

    protected fun createTestMobile(daycareId: DaycareId) = db.transaction { tx ->
        val mobileId = tx.insertTestMobileDevice(DevMobileDevice(unitId = daycareId))
        AuthenticatedUser.MobileDevice(mobileId)
    }
}
