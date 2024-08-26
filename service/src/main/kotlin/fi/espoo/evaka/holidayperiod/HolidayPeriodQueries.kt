// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import fi.espoo.evaka.shared.HolidayPeriodId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate

private fun Database.Read.getHolidayPeriods(where: Predicate): Database.Result<HolidayPeriod> =
    createQuery {
            sql(
                """
SELECT id, period, reservations_open_on, reservation_deadline
FROM holiday_period h
WHERE ${predicate(where.forTable("h"))}
"""
            )
        }
        .mapTo<HolidayPeriod>()

fun Database.Read.getHolidayPeriodsInRange(range: FiniteDateRange): List<HolidayPeriod> =
    getHolidayPeriods(Predicate { where("$it.period && ${bind(range)}") }).toList()

fun Database.Read.getHolidayPeriodsWithReservationDeadline(
    reservationDeadline: LocalDate
): List<HolidayPeriod> =
    getHolidayPeriods(
            Predicate { where("$it.reservation_deadline = ${bind(reservationDeadline)}") }
        )
        .toList()

fun Database.Read.getHolidayPeriods(): List<HolidayPeriod> =
    getHolidayPeriods(Predicate.alwaysTrue()).toList().sortedBy { it.period.start }

fun Database.Read.getHolidayPeriod(id: HolidayPeriodId): HolidayPeriod? =
    getHolidayPeriods(Predicate { where("$it.id = ${bind(id)}") }).exactlyOneOrNull()

fun Database.Transaction.insertHolidayPeriod(
    period: FiniteDateRange,
    reservationsOpenOn: LocalDate,
    reservationDeadline: LocalDate,
): HolidayPeriod =
    createQuery {
            sql(
                """
INSERT INTO holiday_period (period, reservations_open_on, reservation_deadline)
VALUES (${bind(period)}, ${bind(reservationsOpenOn)}, ${bind(reservationDeadline)})
RETURNING *
        """
            )
        }
        .exactlyOne<HolidayPeriod>()

fun Database.Transaction.updateHolidayPeriod(
    id: HolidayPeriodId,
    period: FiniteDateRange,
    reservationsOpenOn: LocalDate,
    reservationDeadline: LocalDate,
) =
    createUpdate {
            sql(
                """
UPDATE holiday_period
SET
    period = ${bind(period)},
    reservations_open_on = ${bind(reservationsOpenOn)},
    reservation_deadline = ${bind(reservationDeadline)}
WHERE id = ${bind(id)}
        """
            )
        }
        .updateExactlyOne()

fun Database.Transaction.deleteHolidayPeriod(id: HolidayPeriodId) =
    createUpdate { sql("DELETE FROM holiday_period WHERE id = ${bind(id)}") }.execute()
