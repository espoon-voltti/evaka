// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.ktlint

import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId

private val about =
    Rule.About(
        maintainer = "eVaka core team",
        repositoryUrl = "https://github.com/espoon-voltti/evaka"
    )

open class EvakaRule(
    name: String
) : Rule(RuleId("evaka:$name"), about)
