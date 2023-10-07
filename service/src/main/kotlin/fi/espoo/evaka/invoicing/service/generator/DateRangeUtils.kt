// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service.generator

import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate

interface WithRange {
    val range: DateRange
}

interface WithFiniteRange : WithRange {
    val finiteRange: FiniteDateRange
    override val range: DateRange
        get() = finiteRange.asDateRange()
}

fun getDatesOfChange(vararg ranges: WithRange): Set<LocalDate> {
    return (ranges.map { it.range.start } + ranges.mapNotNull { it.range.end?.plusDays(1) }).toSet()
}

fun buildFiniteDateRanges(datesOfChange: Set<LocalDate>): List<FiniteDateRange> {
    return datesOfChange.sorted().zipWithNext().map { (first, second) ->
        FiniteDateRange(first, second.minusDays(1))
    }
}

fun buildFiniteDateRanges(vararg ranges: WithRange): List<FiniteDateRange> {
    return buildFiniteDateRanges(getDatesOfChange(*ranges))
}

fun buildDateRanges(datesOfChange: Set<LocalDate>): List<DateRange> {
    val finiteRanges = buildFiniteDateRanges(datesOfChange)
    val finalRange = datesOfChange.maxOrNull()?.let { DateRange(it, null) }
    return finiteRanges.map { it.asDateRange() } + listOfNotNull(finalRange)
}

fun buildDateRanges(vararg ranges: WithRange): List<DateRange> {
    val datesOfChange = getDatesOfChange(*ranges)
    return buildDateRanges(datesOfChange)
}
