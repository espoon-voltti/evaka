// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidaypediod

import fi.espoo.evaka.shared.HolidayPeriodId
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.Translatable
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.mapper.PropagateNull
import org.jdbi.v3.json.Json
import java.time.LocalDate

data class FreeAbsencePeriod(
    @PropagateNull
    val deadline: LocalDate,
    @Json
    val questionLabel: Translatable, // "Will your children be on holiday for 8 consecutive weeks during...?"

    val periodOptions: List<FiniteDateRange>,
    @Json
    val periodOptionLabel: Translatable
)

data class HolidayPeriod(
    val id: HolidayPeriodId,
    val created: HelsinkiDateTime,
    val updated: HelsinkiDateTime,
    val period: FiniteDateRange,
    val reservationDeadline: LocalDate,
    val showReservationBannerFrom: LocalDate,
    @Json
    val description: Translatable,
    @Json
    val descriptionLink: Translatable,
    @Nested("free_period")
    val freePeriod: FreeAbsencePeriod?
)

data class HolidayPeriodBody(
    val period: FiniteDateRange,
    val reservationDeadline: LocalDate,
    val showReservationBannerFrom: LocalDate,
    @Json
    val description: Translatable,
    @Json
    val descriptionLink: Translatable,
    @Nested("free_period")
    val freePeriod: FreeAbsencePeriod?
)
