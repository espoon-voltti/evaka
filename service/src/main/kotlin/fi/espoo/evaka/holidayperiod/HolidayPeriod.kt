// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import fi.espoo.evaka.shared.HolidayPeriodId
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate

data class HolidayPeriod(
    val id: HolidayPeriodId,
    val period: FiniteDateRange,
    val reservationDeadline: LocalDate
)

data class HolidayPeriodBody(
    val period: FiniteDateRange,
    val reservationDeadline: LocalDate
)
