// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

import fi.espoo.evaka.ConstList
import fi.espoo.evaka.shared.db.DatabaseEnum

/** Supported languages in eVaka UI */
@ConstList("uiLanguages")
enum class UiLanguage(val isoLanguage: IsoLanguage) : DatabaseEnum {
    FI(IsoLanguage.FIN),
    SV(IsoLanguage.SWE),
    EN(IsoLanguage.ENG);

    override val sqlType: String = "ui_language"
}
