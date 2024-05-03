// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.invoicing.domain.FeeDecisionDetailed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionDetailed
import fi.espoo.evaka.setting.SettingType
import fi.espoo.evaka.shared.domain.OfficialLanguage

data class FeeDecisionPdfData(
    val decision: FeeDecisionDetailed,
    val settings: Map<SettingType, String>,
    val lang: OfficialLanguage
)

data class VoucherValueDecisionPdfData(
    val decision: VoucherValueDecisionDetailed,
    val settings: Map<SettingType, String>,
    val lang: OfficialLanguage
)

@Deprecated(
    message = "use OfficialLanguage instead",
    replaceWith =
        ReplaceWith("OfficialLanguage", imports = ["fi.espoo.evaka.shared.domain.OfficialLanguage"])
)
typealias DocumentLang = OfficialLanguage
