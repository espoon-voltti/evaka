// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import org.jdbi.v3.core.mapper.ColumnMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.time.Duration

val REPORT_STATEMENT_TIMEOUT: Duration = Duration.ofMinutes(10)

fun getPrimaryUnitType(careTypes: Set<String>): UnitType? {
    if (careTypes.contains("FAMILY")) return UnitType.FAMILY
    if (careTypes.contains("GROUP_FAMILY")) return UnitType.GROUP_FAMILY
    if (careTypes.contains("CENTRE") || careTypes.contains("PRESCHOOL")) return UnitType.DAYCARE
    if (careTypes.contains("CLUB")) return UnitType.CLUB
    return null
}

enum class UnitType {
    DAYCARE,
    FAMILY,
    GROUP_FAMILY,
    CLUB;

    companion object {
        val JDBI_COLUMN_MAPPER: ColumnMapper<UnitType> = ColumnMapper<UnitType> { rs: ResultSet, columnNumber: Int, _: StatementContext ->
            @Suppress("UNCHECKED_CAST")
            (rs.getArray(columnNumber).array as Array<out Any>).map { it.toString() }.toSet().let(::getPrimaryUnitType)
        }
    }
}
