// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

fun getPrimaryUnitType(careTypes: Set<String>): UnitType? {
    if (careTypes.contains("FAMILY")) return UnitType.FAMILY
    if (careTypes.contains("GROUP_FAMILY")) return UnitType.GROUP_FAMILY
    if (careTypes.contains("CENTRE")) return UnitType.DAYCARE
    if (careTypes.contains("CLUB")) return UnitType.CLUB
    return null
}

enum class UnitType {
    DAYCARE,
    FAMILY,
    GROUP_FAMILY,
    CLUB
}
