// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidaypediod

import fi.espoo.evaka.shared.HolidayPeriodId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.updateExactlyOne
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo

val holidayPeriodSelect = """
    SELECT id,
           created,
           updated,
           period,
           reservation_deadline,
           show_reservation_banner_from,
           description,
           description_link,
           free_period_deadline,
           free_period_question_label,
           free_period_period_options,
           free_period_period_option_label
    FROM holiday_period
""".trimIndent()

fun Database.Read.getHolidayPeriods(): List<HolidayPeriod> =
    this.createQuery("$holidayPeriodSelect ORDER BY period")
        .mapTo<HolidayPeriod>()
        .list()

fun Database.Read.getHolidayPeriod(id: HolidayPeriodId): HolidayPeriod? =
    this.createQuery("$holidayPeriodSelect WHERE id = :id")
        .bind("id", id)
        .mapTo<HolidayPeriod>()
        .firstOrNull()

fun Database.Transaction.createHolidayPeriod(data: HolidayPeriodBody): HolidayPeriod =
    this.createQuery(
        """
INSERT INTO holiday_period (
    period,
    reservation_deadline,
    show_reservation_banner_from,
    description,
    description_link,
    free_period_deadline,
    free_period_question_label,
    free_period_period_options,
    free_period_period_option_label
)
VALUES (
    :period,
    :reservationDeadline,
    :showReservationBannerFrom,
    :description,
    :descriptionLink,
    :freePeriod?.deadline,
    :freePeriod?.questionLabel,
    :freePeriod?.periodOptions,
    :freePeriod?.periodOptionLabel
)
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
    description_link = :descriptionLink,
    free_period_deadline = :freePeriod?.deadline,
    free_period_question_label = :freePeriod?.questionLabel,
    free_period_period_options = :freePeriod?.periodOptions,
    free_period_period_option_label = :freePeriod?.periodOptionLabel
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
