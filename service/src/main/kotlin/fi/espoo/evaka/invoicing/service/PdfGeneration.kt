// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.invoicing.domain.FeeDecisionDetailed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionDetailed
import fi.espoo.evaka.setting.SettingType
import fi.espoo.evaka.shared.message.MessageLanguage

data class FeeDecisionPdfData(
    val decision: FeeDecisionDetailed,
    val settings: Map<SettingType, String>,
    val lang: DocumentLang
)

data class VoucherValueDecisionPdfData(
    val decision: VoucherValueDecisionDetailed,
    val settings: Map<SettingType, String>,
    val lang: DocumentLang
)

enum class DocumentLang(val langCode: String) {
    FI("fi"),
    SV("sv");

    val messageLang: MessageLanguage
        get() =
            when (this) {
                FI -> MessageLanguage.FI
                SV -> MessageLanguage.SV
            }
}
