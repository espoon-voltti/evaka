// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import mapValues from 'lodash/mapValues'

import FiniteDateRange from 'lib-common/finite-date-range'
import { CitizenCalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import { DailyServiceTimeNotification } from 'lib-common/generated/api-types/dailyservicetimes'
import {
  AbsenceRequest,
  DailyReservationRequest,
  ReservationsResponse
} from 'lib-common/generated/api-types/reservations'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { client } from '../api-client'

export async function getReservations(
  from: LocalDate,
  to: LocalDate
): Promise<ReservationsResponse> {
  return client
    .get<JsonOf<ReservationsResponse>>('/citizen/reservations', {
      params: { from: from.formatIso(), to: to.formatIso() }
    })
    .then((res) => ({
      ...res.data,
      dailyData: res.data.dailyData.map((data) => ({
        ...data,
        date: LocalDate.parseIso(data.date)
      })),
      children: res.data.children.map((child) => ({
        ...child,
        placements: child.placements.map((r) => FiniteDateRange.parseJson(r))
      })),
      reservableDays: Object.entries(res.data.reservableDays).reduce<
        Record<string, FiniteDateRange[]>
      >((acc, [childId, ranges]) => {
        acc[childId] = ranges.map((r) => FiniteDateRange.parseJson(r))
        return acc
      }, {})
    }))
}

export async function postReservations(
  reservations: DailyReservationRequest[]
): Promise<void> {
  return client
    .post('/citizen/reservations', reservations)
    .then(() => undefined)
}

export async function postAbsences(request: AbsenceRequest): Promise<void> {
  return client.post('/citizen/absences', request).then(() => undefined)
}

export async function getDailyServiceTimeNotifications(): Promise<
  DailyServiceTimeNotification[]
> {
  return client
    .get<JsonOf<DailyServiceTimeNotification[]>>(
      '/citizen/daily-service-time-notifications'
    )
    .then(({ data }) =>
      data.map((notif) => ({
        ...notif,
        dateFrom: LocalDate.parseIso(notif.dateFrom)
      }))
    )
}

export async function dismissDailyServiceTimeNotifications(
  notificationIds: UUID[]
): Promise<void> {
  return client
    .post('/citizen/daily-service-time-notifications/dismiss', notificationIds)
    .then(() => undefined)
}

export async function getCalendarEvents(
  start: LocalDate,
  end: LocalDate
): Promise<CitizenCalendarEvent[]> {
  return client
    .get<JsonOf<CitizenCalendarEvent[]>>('/citizen/calendar-events', {
      params: { start: start.formatIso(), end: end.formatIso() }
    })
    .then((res) =>
      res.data.map((event) => ({
        ...event,
        attendingChildren: mapValues(event.attendingChildren, (attending) =>
          attending.map((a) => ({
            ...a,
            periods: a.periods.map((period) =>
              FiniteDateRange.parseJson(period)
            )
          }))
        )
      }))
    )
}
