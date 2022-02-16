// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { HolidayPeriod } from 'lib-common/generated/api-types/holidayperiod'
import LocalDate from 'lib-common/local-date'

export function isHolidayFormCurrentlyActive() {
  const today = LocalDate.today()
  return (p: HolidayPeriod) =>
    p.showReservationBannerFrom.isEqualOrBefore(today) &&
    p.reservationDeadline.isEqualOrAfter(today)
}
