// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.oulu.util

enum class FieldType {
    ALPHANUMERIC,
    NUMERIC,

    // we need a specific monetary type because they are prescaled by 100, so they include two
    // decimals
    MONETARY,
}
