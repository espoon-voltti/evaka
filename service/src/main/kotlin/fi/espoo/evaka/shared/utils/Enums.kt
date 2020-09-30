// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.utils

inline fun <reified T : Enum<T>> parseEnum(value: String): T? {
    return try {
        enumValueOf<T>(value)
    } catch (e: IllegalArgumentException) {
        null
    }
}
