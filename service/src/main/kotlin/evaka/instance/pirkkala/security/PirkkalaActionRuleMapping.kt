// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.pirkkala.security

import evaka.core.shared.auth.UserRole
import evaka.core.shared.security.Action
import evaka.core.shared.security.actionrule.ActionRuleMapping
import evaka.core.shared.security.actionrule.HasGlobalRole
import evaka.core.shared.security.actionrule.HasUnitRole
import evaka.core.shared.security.actionrule.ScopedActionRule
import evaka.core.shared.security.actionrule.UnscopedActionRule

class PirkkalaActionRuleMapping(private val commonRules: ActionRuleMapping) : ActionRuleMapping {
    override fun rulesOf(action: Action.UnscopedAction): Sequence<UnscopedActionRule> =
        when (action) {
            else -> commonRules.rulesOf(action)
        }

    override fun <T> rulesOf(action: Action.ScopedAction<in T>): Sequence<ScopedActionRule<in T>> =
        when (action) {
            Action.Unit.READ_EXCEEDED_SERVICE_NEEDS_REPORT ->
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(
                        HasGlobalRole(UserRole.SERVICE_WORKER, UserRole.FINANCE_ADMIN)
                            as ScopedActionRule<in T>
                    ) +
                    sequenceOf(
                        HasUnitRole(
                                UserRole.UNIT_SUPERVISOR,
                                UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                            )
                            .inUnit() as ScopedActionRule<in T>
                    )

            else -> commonRules.rulesOf(action)
        }
}
