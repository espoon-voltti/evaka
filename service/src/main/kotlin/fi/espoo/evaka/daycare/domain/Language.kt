// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.domain

@Suppress("EnumEntryName", "ktlint:enum-entry-name-case")
enum class Language {
    fi,
    sv,
    en;

    companion object {
        fun tryValueOf(value: String?): Language? =
            when (value) {
                "fi" -> fi
                "sv" -> sv
                "en" -> en
                else -> null
            }
    }
}
