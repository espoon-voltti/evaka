// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import { ReservationChild } from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'

export const getEarliestReservableDate = (
  childInfo: ReservationChild[],
  reservableDays: Record<string, FiniteDateRange[]>
) => {
  const earliestReservableDateByChild = childInfo.map((c) =>
    reservableDays[c.id].reduce<LocalDate | undefined>(
      (acc, cur) => (!acc || cur.start.isBefore(acc) ? cur.start : acc),
      undefined
    )
  )
  const earliestReservableDate = earliestReservableDateByChild.reduce<
    LocalDate | undefined
  >((acc, cur) => (!acc || cur?.isBefore(acc) ? cur : acc), undefined)
  return earliestReservableDate
}

export const getLatestReservableDate = (
  childInfo: ReservationChild[],
  reservableDays: Record<string, FiniteDateRange[]>
) => {
  const earliestReservableDateByChild = childInfo.map((c) =>
    reservableDays[c.id].reduce(
      (acc, cur) => (cur.end.isAfter(acc) ? cur.end : acc),
      LocalDate.todayInSystemTz()
    )
  )
  const earliestReservableDate = earliestReservableDateByChild.reduce(
    (acc, cur) => (cur.isAfter(acc) ? cur : acc),
    LocalDate.todayInSystemTz()
  )
  return earliestReservableDate
}

export const isDayReservableForSomeone = (
  date: LocalDate,
  reservableDays: Record<string, FiniteDateRange[]>
) =>
  Object.entries(reservableDays).some(([_childId, ranges]) =>
    ranges.some((r) => r.includes(date))
  )
