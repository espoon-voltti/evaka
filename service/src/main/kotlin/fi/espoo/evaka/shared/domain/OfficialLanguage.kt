// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

import com.fasterxml.jackson.annotation.JsonAlias
import fi.espoo.evaka.ConstList
import fi.espoo.evaka.shared.db.DatabaseEnum

/** Official language of Finland */
@ConstList("officialLanguages")
enum class OfficialLanguage(
    val isoLanguage: IsoLanguage
) : DatabaseEnum {
    @JsonAlias("fi") // support legacy data deserialization
    FI(IsoLanguage.FIN),

    @JsonAlias("sv") // support legacy data deserialization
    SV(IsoLanguage.SWE);

    override val sqlType: String = "official_language"
}
