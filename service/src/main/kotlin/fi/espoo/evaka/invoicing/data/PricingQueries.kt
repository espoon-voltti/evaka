// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.invoicing.domain.Pricing
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.time.LocalDate

fun Database.Read.getPricing(from: LocalDate): List<Pair<DateRange, Pricing>> {
    return createQuery("SELECT * FROM pricing WHERE valid_to IS NULL OR valid_to >= :from")
        .bind("from", from)
        .map(toPricing)
        .toList()
}

val toPricing = { rs: ResultSet, _: StatementContext ->
    DateRange(
        start = rs.getDate("valid_from").toLocalDate(),
        end = rs.getDate("valid_to")?.toLocalDate()
    ) to Pricing(
        multiplier = rs.getBigDecimal("multiplier"),
        maxThresholdDifference = rs.getInt("max_threshold_difference"),
        minThreshold2 = rs.getInt("min_threshold_2"),
        minThreshold3 = rs.getInt("min_threshold_3"),
        minThreshold4 = rs.getInt("min_threshold_4"),
        minThreshold5 = rs.getInt("min_threshold_5"),
        minThreshold6 = rs.getInt("min_threshold_6"),
        thresholdIncrease6Plus = rs.getInt("threshold_increase_6_plus")
    )
}
