// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.ktlint

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId

class CustomRuleSetProvider : RuleSetProviderV3(RuleSetId("evaka")) {
    override fun getRuleProviders(): Set<RuleProvider> =
        setOf(
            RuleProvider { NoJUnit4Imports() },
            RuleProvider { NoPrint() },
            RuleProvider { NoTripleEquals() },
        )
}
