// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dataremoval

import evaka.core.dataremoval.ExpirationRule.After
import evaka.core.dataremoval.ExpirationRule.AllOf
import evaka.core.dataremoval.ExpirationRule.Always
import evaka.core.dataremoval.ExpirationRule.AnyOf
import evaka.core.dataremoval.ExpirationRule.ArchivedIfRequired
import evaka.core.dataremoval.ExpirationRule.Coalesce
import evaka.core.dataremoval.ExpirationRule.Never
import evaka.core.dataremoval.ExpirationRule.SafeForIntegrations

// Nothing in the application calls this validation: `SchemaDefinitionTest` runs it with a
// hand-written predicate and `DataRetentionSchemaTest` with one read from the database schema,
// which is why it lives in the test sources both source sets compile against.

/**
 * Null never reaches all of or any of: every member of them returns a result, and a rule that may
 * not is wrapped directly in a coalesce whose fallback always does. Nothing after that fallback can
 * be reached, so a rule there is a mistake. [mayHaveNoDate] tells which sources can give no date
 * for some node.
 */
fun ExpirationRule.validate(context: String, mayHaveNoDate: (DateSource) -> Boolean) {
    validateMembers(context, mayHaveNoDate)
    require(alwaysApplies(mayHaveNoDate)) {
        "$context: rule $this may find no date for some node, add a fallback with Coalesce"
    }
}

/**
 * Whether the rule returns a result for every node. Only [After] can fail to, when its date is
 * missing.
 */
private fun ExpirationRule.alwaysApplies(mayHaveNoDate: (DateSource) -> Boolean): Boolean =
    when (this) {
        Always,
        Never,
        is ArchivedIfRequired -> true
        is After -> !mayHaveNoDate(dateSource)
        is AllOf -> rules.all { it.alwaysApplies(mayHaveNoDate) }
        is AnyOf -> rules.all { it.alwaysApplies(mayHaveNoDate) }
        is Coalesce -> rules.any { it.alwaysApplies(mayHaveNoDate) }
        is SafeForIntegrations -> rule.alwaysApplies(mayHaveNoDate)
    }

private fun ExpirationRule.validateMembers(
    context: String,
    mayHaveNoDate: (DateSource) -> Boolean,
) {
    when (this) {
        is AllOf -> rules.forEach { it.validateMember(context, mayHaveNoDate) }
        is AnyOf -> rules.forEach { it.validateMember(context, mayHaveNoDate) }
        is Coalesce -> {
            val fallback = rules.indexOfFirst { it.alwaysApplies(mayHaveNoDate) }
            require(fallback != -1) { "$context: no rule in $this always returns a result" }
            require(fallback == rules.lastIndex) {
                "$context: the rules after ${rules[fallback]} in $this are never reached"
            }
            rules.forEach { it.validateMembers(context, mayHaveNoDate) }
        }
        is SafeForIntegrations -> rule.validateMembers(context, mayHaveNoDate)
        Always,
        Never,
        is After,
        is ArchivedIfRequired -> {}
    }
}

private fun ExpirationRule.validateMember(context: String, mayHaveNoDate: (DateSource) -> Boolean) {
    require(alwaysApplies(mayHaveNoDate)) {
        "$context: rule $this may find no date for some node, wrap it in Coalesce with a fallback"
    }
    validateMembers(context, mayHaveNoDate)
}
