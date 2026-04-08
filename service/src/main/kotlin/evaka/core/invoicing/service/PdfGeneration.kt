// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.service

import evaka.core.invoicing.domain.FeeDecisionDetailed
import evaka.core.invoicing.domain.VoucherValueDecisionDetailed
import evaka.core.setting.SettingType
import evaka.core.shared.domain.OfficialLanguage

data class FeeDecisionPdfData(
    val decision: FeeDecisionDetailed,
    val settings: Map<SettingType, String>,
    val lang: OfficialLanguage,
)

data class VoucherValueDecisionPdfData(
    val decision: VoucherValueDecisionDetailed,
    val settings: Map<SettingType, String>,
    val lang: OfficialLanguage,
)
