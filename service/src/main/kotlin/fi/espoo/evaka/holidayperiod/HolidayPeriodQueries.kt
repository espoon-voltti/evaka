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
    this.createQuery(
            "SELECT id, period, reservation_deadline FROM holiday_period h WHERE h.period && :range"
        )
        .bind("range", range)
        .mapTo<HolidayPeriod>()
        .list()

fun Database.Read.getHolidayPeriods(): List<HolidayPeriod> =
    this.createQuery("SELECT id, period, reservation_deadline FROM holiday_period ORDER BY period")
        .mapTo<HolidayPeriod>()
        .list()

fun Database.Read.getHolidayPeriod(id: HolidayPeriodId): HolidayPeriod? =
    this.createQuery("SELECT id, period, reservation_deadline FROM holiday_period WHERE id = :id")
        .bind("id", id)
        .mapTo<HolidayPeriod>()
        .firstOrNull()

fun Database.Transaction.createHolidayPeriod(
    period: FiniteDateRange,
    reservationDeadline: LocalDate
): HolidayPeriod =
    this.createQuery(
            """
INSERT INTO holiday_period (period, reservation_deadline)
VALUES (:period, :reservationDeadline)
RETURNING *
        """
                .trimIndent()
        )
        .bind("period", period)
        .bind("reservationDeadline", reservationDeadline)
        .mapTo<HolidayPeriod>()
        .one()

fun Database.Transaction.updateHolidayPeriod(
    id: HolidayPeriodId,
    period: FiniteDateRange,
    reservationDeadline: LocalDate
) =
    this.createUpdate(
            """
UPDATE holiday_period
SET period = :period,
    reservation_deadline = :reservationDeadline
WHERE id = :id
        """
                .trimIndent()
        )
        .bind("period", period)
        .bind("reservationDeadline", reservationDeadline)
        .bind("id", id)
        .updateExactlyOne()

fun Database.Transaction.deleteHolidayPeriod(id: HolidayPeriodId) =
    this.createUpdate("DELETE FROM holiday_period WHERE id = :id").bind("id", id).execute()
