// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

import java.time.DayOfWeek
import java.time.LocalDate

// Butcher-Meeus Algorithm
// Source: https://www.baeldung.com/java-determine-easter-date-specific-year
private fun computeEaster(year: Int): LocalDate {
    val a = year % 19
    val b = year / 100
    val c = year % 100
    val d = b / 4
    val e = b % 4
    val f = (b + 8) / 25
    val g = (b - f + 1) / 3
    val h = (19 * a + b - d - g + 15) % 30
    val i = c / 4
    val k = c % 4
    val l = (2 * e + 2 * i - h - k + 32) % 7
    val m = (a + 11 * h + 22 * l) / 451
    val t = h + l - 7 * m + 114
    val n = t / 31
    val o = t % 31
    return LocalDate.of(year, n, o + 1)
}

private fun holidaysInFinland(year: Int): Sequence<LocalDate> {
    /*
    Finnish holiday calendar since 1992.

    Fixed holidays:
    - New year / Uusivuosi: 1.1.
    - Epiphany / Loppiainen: 6.1.
    - May Day / Vappu: 1.5.
    - Independence Day / Itsenäisyyspäivä: 6.12.
    - Christmas Eve / Jouluaatto: 24.12.
    - Christmas Day / Joulupäivä: 25.12.
    - Boxing Day / Tapaninpäivä: 26.12.

    Moving within one week:
    - Midsummer Day / Juhannuspäivä: Saturday between 20.6. and 26.6.
    - Midsummer Eve / Juhannusaatto: The day before Midsummer Day
    - All Saints' Day / Pyhäinpäivä: Saturday between 31.10. and 6.11.

    Relative to Easter:
    - Easter Sunday / Pääsiäispäivä: Can be computed
    - Good Friday / Pitkäperjantai: 2 days before Easter Sunday
    - Easter Monday / 2. pääsiäispäivä: The day after Easter Sunday
    - Ascension Day / Helatorstai: 39 days after Easter Sunday
    - Pentecost / Helluntai: 49 days after Easter Sunday
    */
    require(year >= 1992)

    val newYear = LocalDate.of(year, 1, 1)
    val epiphany = LocalDate.of(year, 1, 6)
    val mayDay = LocalDate.of(year, 5, 1)
    val independenceDay = LocalDate.of(year, 12, 6)
    val christmasEve = LocalDate.of(year, 12, 24)
    val christmasDay = LocalDate.of(year, 12, 25)
    val boxingDay = LocalDate.of(year, 12, 26)

    val midsummerDay =
        FiniteDateRange(LocalDate.of(year, 6, 20), LocalDate.of(year, 6, 26)).dates().first {
            it.dayOfWeek == DayOfWeek.SATURDAY
        }
    val midsummerEve = midsummerDay.minusDays(1)

    val allSaintsDay =
        FiniteDateRange(LocalDate.of(year, 10, 31), LocalDate.of(year, 11, 6)).dates().first {
            it.dayOfWeek == DayOfWeek.SATURDAY
        }

    val easterSunday = computeEaster(year)
    val goodFriday = easterSunday.minusDays(2)
    val easterMonday = easterSunday.plusDays(1)
    val ascensionDay = easterSunday.plusDays(39)
    val pentecost = easterSunday.plusDays(49)

    return sequenceOf(
        newYear,
        epiphany,
        mayDay,
        independenceDay,
        christmasEve,
        christmasDay,
        boxingDay,
        midsummerEve,
        midsummerDay,
        allSaintsDay,
        goodFriday,
        easterSunday,
        easterMonday,
        ascensionDay,
        pentecost,
    )
}

fun getHolidays(range: FiniteDateRange): Set<LocalDate> =
    (range.start.year..range.end.year)
        .flatMap { year -> holidaysInFinland(year) }
        .filter { range.includes(it) }
        .toSet()

fun isHoliday(date: LocalDate): Boolean = getHolidays(FiniteDateRange(date, date)).isNotEmpty()
