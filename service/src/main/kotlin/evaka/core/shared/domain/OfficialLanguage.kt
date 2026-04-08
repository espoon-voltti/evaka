// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.domain

import com.fasterxml.jackson.annotation.JsonAlias
import evaka.core.ConstList
import evaka.core.shared.db.DatabaseEnum

/** Official language of Finland */
@ConstList("officialLanguages")
enum class OfficialLanguage(val isoLanguage: IsoLanguage) : DatabaseEnum {
    @JsonAlias("fi") // support legacy data deserialization
    FI(IsoLanguage.FIN),
    @JsonAlias("sv") // support legacy data deserialization
    SV(IsoLanguage.SWE);

    override val sqlType: String = "official_language"
}
