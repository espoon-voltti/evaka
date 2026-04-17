// SPDX-FileCopyrightText: 2021-2022 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.security

import evaka.core.shared.auth.UserRole
import evaka.core.shared.security.Action
import evaka.core.shared.security.actionrule.ActionRuleMapping
import evaka.core.shared.security.actionrule.HasGlobalRole
import evaka.core.shared.security.actionrule.HasUnitRole
import evaka.core.shared.security.actionrule.ScopedActionRule
import evaka.core.shared.security.actionrule.UnscopedActionRule

class TampereActionRuleMapping(private val commonRules: ActionRuleMapping) : ActionRuleMapping {
    override fun rulesOf(action: Action.UnscopedAction): Sequence<UnscopedActionRule> =
        when (action) {
            Action.Global.SETTINGS_PAGE,
            Action.Global.UPDATE_SETTINGS ->
                action.defaultRules.asSequence() +
                    sequenceOf(HasGlobalRole(UserRole.SERVICE_WORKER))

            else -> commonRules.rulesOf(action)
        }

    override fun <T> rulesOf(action: Action.ScopedAction<in T>): Sequence<ScopedActionRule<in T>> =
        when (action) {
            Action.Unit.READ_PRESCHOOL_APPLICATION_REPORT -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(
                        HasGlobalRole(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.DIRECTOR)
                            as ScopedActionRule<in T>
                    ) +
                    sequenceOf(
                        HasUnitRole(UserRole.UNIT_SUPERVISOR).inUnit() as ScopedActionRule<in T>
                    )
            }

            else -> commonRules.rulesOf(action)
        }
}
