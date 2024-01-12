// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate

private fun getNextMonday(now: LocalDate): LocalDate = now.plusDays(7 - now.dayOfWeek.value + 1L)

fun getNextReservableMonday(
    now: HelsinkiDateTime,
    thresholdHours: Long,
    nextMonday: LocalDate = getNextMonday(now.toLocalDate())
): LocalDate =
    if (nextMonday.isAfter(now.plusHours(thresholdHours).toLocalDate())) {
        nextMonday
    } else {
        getNextReservableMonday(now, thresholdHours, nextMonday.plusWeeks(1))
    }

fun getReservableRange(
    now: HelsinkiDateTime,
    thresholdHours: Long,
): FiniteDateRange {
    val today = now.toLocalDate()
    val nextReservableMonday = getNextReservableMonday(now, thresholdHours, getNextMonday(today))

    val firstOfJuly = nextReservableMonday.withMonth(7).withDayOfMonth(1)
    val lastReservableDay =
        if (nextReservableMonday.isBefore(firstOfJuly)) {
            firstOfJuly.plusMonths(1).withDayOfMonth(31)
        } else {
            firstOfJuly.plusYears(1).plusMonths(1).withDayOfMonth(31)
        }

    return FiniteDateRange(nextReservableMonday, lastReservableDay)
}

fun getConfirmedRange(now: HelsinkiDateTime, thresholdHours: Long): FiniteDateRange {
    val startDate = now.toLocalDate().plusDays(1)
    val endDate = getNextReservableMonday(now, thresholdHours).minusDays(1)
    return FiniteDateRange(startDate, endDate)
}
