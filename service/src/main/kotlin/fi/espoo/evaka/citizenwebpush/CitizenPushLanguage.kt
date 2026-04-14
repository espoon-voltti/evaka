// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenwebpush

enum class CitizenPushLanguage {
    FI,
    SV,
    EN;

    companion object {
        fun fromPersonLanguage(language: String?): CitizenPushLanguage =
            when (language?.lowercase()) {
                "sv" -> SV
                "en" -> EN
                else -> FI
            }
    }
}
