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
    reservationDeadline = summerRange.start.minusWeeks(3),
)
