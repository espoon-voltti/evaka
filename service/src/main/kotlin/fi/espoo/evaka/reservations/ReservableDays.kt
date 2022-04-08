// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.holidayperiod.HolidayPeriodDeadline
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate

private fun getNextMonday(now: LocalDate): LocalDate = now.plusDays(7 - now.dayOfWeek.value + 1L)

private fun getNextReservableMonday(now: HelsinkiDateTime, thresholdHours: Long, nextMonday: LocalDate): LocalDate =
    if (nextMonday.isAfter(now.plusHours(thresholdHours).toLocalDate())) {
        nextMonday
    } else {
        getNextReservableMonday(now, thresholdHours, nextMonday.plusWeeks(1))
    }

fun getReservableDays(now: HelsinkiDateTime, thresholdHours: Long, holidays: List<HolidayPeriodDeadline>): List<FiniteDateRange> {
    val today = now.toLocalDate()
    val nextReservableMonday = getNextReservableMonday(now, thresholdHours, getNextMonday(today))

    val firstOfJuly = nextReservableMonday.withMonth(7).withDayOfMonth(1)
    val lastReservableDay = if (nextReservableMonday.isBefore(firstOfJuly)) {
        firstOfJuly.plusMonths(1).withDayOfMonth(31)
    } else {
        firstOfJuly.plusYears(1).plusMonths(1).withDayOfMonth(31)
    }

    val nonReservableHolidays = holidays.filter { it.reservationDeadline < today }.map { it.period }
    return FiniteDateRange(nextReservableMonday, lastReservableDay).complement(nonReservableHolidays)
}
