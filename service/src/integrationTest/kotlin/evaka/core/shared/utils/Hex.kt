// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.utils

fun String.decodeHex(): ByteArray {
    return filterNot { it.isWhitespace() }.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
