// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Translatable
import java.time.LocalDate

val emptyTranslatable = Translatable("", "", "")
val summerRange = FiniteDateRange(start = LocalDate.of(2021, 6, 1), end = LocalDate.of(2021, 8, 31))
val summerPeriod = HolidayPeriodBody(
    period = summerRange,
    description = Translatable("Varaathan \n 'quote' \"double\" loma-aikasi", "", ""),
    descriptionLink = emptyTranslatable,
    showReservationBannerFrom = summerRange.start.minusWeeks(6),
    reservationDeadline = summerRange.start.minusWeeks(3),
    freePeriod = null
)

val summerPeriodWithFreePeriod = summerPeriod.copy(
    freePeriod = FreeAbsencePeriod(
        deadline = LocalDate.of(2021, 6, 1),
        periodOptions = listOf(
            FiniteDateRange(LocalDate.of(2021, 7, 1), LocalDate.of(2021, 7, 7)),
            FiniteDateRange(LocalDate.of(2021, 7, 8), LocalDate.of(2021, 7, 14)),
        ),
        periodOptionLabel = emptyTranslatable,
        questionLabel = emptyTranslatable
    )
)
