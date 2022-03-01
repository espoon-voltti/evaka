// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.security.Action

interface ActionRuleMapping {
    fun rulesOf(action: Action.StaticAction): Sequence<StaticActionRule>
}

class DefaultActionRuleMapping : ActionRuleMapping {
    override fun rulesOf(action: Action.StaticAction): Sequence<StaticActionRule> = action.permittedIf.asSequence()
}
