// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.security.actionrule

import evaka.core.shared.security.Action

interface ActionRuleMapping {
    fun rulesOf(action: Action.UnscopedAction): Sequence<UnscopedActionRule>

    fun <T> rulesOf(action: Action.ScopedAction<in T>): Sequence<ScopedActionRule<in T>>
}

class DefaultActionRuleMapping : ActionRuleMapping {
    override fun rulesOf(action: Action.UnscopedAction): Sequence<UnscopedActionRule> =
        action.defaultRules.asSequence()

    override fun <T> rulesOf(action: Action.ScopedAction<in T>): Sequence<ScopedActionRule<in T>> =
        action.defaultRules.asSequence()
}
