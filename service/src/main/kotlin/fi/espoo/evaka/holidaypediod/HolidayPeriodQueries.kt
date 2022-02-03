// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidaypediod

import fi.espoo.evaka.shared.HolidayPeriodId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.updateExactlyOne
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo

fun Database.Read.getHolidayPeriods(): List<HolidayPeriod> =
    this.createQuery(
        """
SELECT id, created, updated, period, reservation_deadline, show_reservation_banner_from, description, description_link 
FROM holiday_period
ORDER BY period
        """.trimIndent()
    )
        .mapTo<HolidayPeriod>()
        .list()

fun Database.Read.getHolidayPeriod(id: HolidayPeriodId): HolidayPeriod? =
    this.createQuery(
        """
SELECT id, created, updated, period, reservation_deadline, show_reservation_banner_from, description, description_link
FROM holiday_period
WHERE id = :id
        """.trimIndent()
    ).bind("id", id)
        .mapTo<HolidayPeriod>()
        .firstOrNull()

fun Database.Transaction.createHolidayPeriod(data: HolidayPeriodBody): HolidayPeriod =
    this.createQuery(
        """
INSERT INTO holiday_period (period, reservation_deadline, show_reservation_banner_from, description, description_link)
VALUES (:period, :reservationDeadline, :showReservationBannerFrom, :description, :descriptionLink)
RETURNING *
        """.trimIndent()
    )
        .bindKotlin(data)
        .mapTo<HolidayPeriod>()
        .one()

fun Database.Transaction.updateHolidayPeriod(id: HolidayPeriodId, data: HolidayPeriodBody) =
    this.createUpdate(
        """
UPDATE holiday_period
SET
    period = :period,
    reservation_deadline = :reservationDeadline,
    show_reservation_banner_from = :showReservationBannerFrom,
    description = :description,
    description_link = :descriptionLink
WHERE id = :id
    """
            .trimIndent()
    )
        .bindKotlin(data)
        .bind("id", id)
        .updateExactlyOne()

fun Database.Transaction.deleteHolidayPeriod(id: HolidayPeriodId) =
    this.createUpdate("DELETE FROM holiday_period WHERE id = :id")
        .bind("id", id)
        .execute()
