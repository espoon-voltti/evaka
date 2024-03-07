// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import fi.espoo.evaka.shared.HolidayPeriodId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate

fun Database.Read.getHolidayPeriodsInRange(
    range: FiniteDateRange,
): List<HolidayPeriod> =
    createQuery {
            sql(
                "SELECT id, period, reservation_deadline FROM holiday_period h WHERE h.period && ${bind(range)}"
            )
        }
        .toList<HolidayPeriod>()

fun Database.Read.getHolidayPeriods(): List<HolidayPeriod> =
    createQuery {
            sql("SELECT id, period, reservation_deadline FROM holiday_period ORDER BY period")
        }
        .toList<HolidayPeriod>()

fun Database.Read.getHolidayPeriod(id: HolidayPeriodId): HolidayPeriod? =
    createQuery {
            sql(
                "SELECT id, period, reservation_deadline FROM holiday_period WHERE id = ${bind(id)}"
            )
        }
        .exactlyOneOrNull<HolidayPeriod>()

fun Database.Transaction.insertHolidayPeriod(
    period: FiniteDateRange,
    reservationDeadline: LocalDate
): HolidayPeriod =
    createQuery {
            sql(
                """
INSERT INTO holiday_period (period, reservation_deadline)
VALUES (${bind(period)}, ${bind(reservationDeadline)})
RETURNING *
        """
            )
        }
        .exactlyOne<HolidayPeriod>()

fun Database.Transaction.updateHolidayPeriod(
    id: HolidayPeriodId,
    period: FiniteDateRange,
    reservationDeadline: LocalDate
) =
    createUpdate {
            sql(
                """
UPDATE holiday_period
SET period = ${bind(period)}, reservation_deadline = ${bind(reservationDeadline)}
WHERE id = ${bind(id)}
        """
            )
        }
        .updateExactlyOne()

fun Database.Transaction.deleteHolidayPeriod(id: HolidayPeriodId) =
    createUpdate { sql("DELETE FROM holiday_period WHERE id = ${bind(id)}") }.execute()
