// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from '../finite-date-range'
import { HolidayPeriod } from '../generated/api-types/holidayperiod'
import { JsonOf } from '../json'
import LocalDate from '../local-date'

export const deserializeHolidayPeriod = ({
  showReservationBannerFrom,
  period,
  reservationDeadline,
  freePeriod,
  ...rest
}: JsonOf<HolidayPeriod>): HolidayPeriod => ({
  ...rest,
  reservationDeadline: LocalDate.parseIso(reservationDeadline),
  showReservationBannerFrom: LocalDate.parseIso(showReservationBannerFrom),
  period: FiniteDateRange.parseJson(period),
  freePeriod: freePeriod
    ? {
        ...freePeriod,
        deadline: LocalDate.parseIso(freePeriod.deadline),
        periodOptions: freePeriod.periodOptions.map((opt) =>
          FiniteDateRange.parseJson(opt)
        )
      }
    : null
})
