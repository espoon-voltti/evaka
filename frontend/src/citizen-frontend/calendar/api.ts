// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import mapValues from 'lodash/mapValues'

import { parseIsoTimeRange } from 'lib-common/api-types/daily-service-times'
import {
  deserializeActiveQuestionnaire,
  deserializeHolidayPeriod
} from 'lib-common/api-types/holiday-period'
import FiniteDateRange from 'lib-common/finite-date-range'
import { CitizenCalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import { DailyServiceTimeNotification } from 'lib-common/generated/api-types/dailyservicetimes'
import {
  ActiveQuestionnaire,
  FixedPeriodsBody,
  HolidayPeriod
} from 'lib-common/generated/api-types/holidayperiod'
import {
  AbsenceRequest,
  DailyReservationRequest,
  ReservationResponseDay,
  ReservationsResponse
} from 'lib-common/generated/api-types/reservations'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { parseReservation } from 'lib-common/reservations'
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
      days: res.data.days.map(
        (day): ReservationResponseDay => ({
          ...day,
          date: LocalDate.parseIso(day.date),
          children: day.children.map((child) => ({
            ...child,
            reservableTimeRange:
              child.reservableTimeRange.type === 'NORMAL'
                ? {
                    type: 'NORMAL',
                    range: parseIsoTimeRange(child.reservableTimeRange.range)
                  }
                : {
                    type: 'INTERMITTENT_SHIFT_CARE',
                    placementUnitOperationTime:
                      child.reservableTimeRange.placementUnitOperationTime !==
                      null
                        ? parseIsoTimeRange(
                            child.reservableTimeRange.placementUnitOperationTime
                          )
                        : null
                  },
            attendances: child.attendances.map((r) => ({
              startTime: LocalTime.parseIso(r.startTime),
              endTime: r.endTime ? LocalTime.parseIso(r.endTime) : null
            })),
            reservations: child.reservations.map(parseReservation)
          }))
        })
      ),
      reservableRange: FiniteDateRange.parseJson(res.data.reservableRange)
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
    .get<
      JsonOf<DailyServiceTimeNotification[]>
    >('/citizen/daily-service-time-notifications')
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

export function getHolidayPeriods(): Promise<HolidayPeriod[]> {
  return client
    .get<JsonOf<HolidayPeriod[]>>(`/citizen/holiday-period`)
    .then((res) => res.data.map(deserializeHolidayPeriod))
}

export function getActiveQuestionnaires(): Promise<ActiveQuestionnaire[]> {
  return client
    .get<JsonOf<ActiveQuestionnaire[]>>(`/citizen/holiday-period/questionnaire`)
    .then((res) => res.data.map(deserializeActiveQuestionnaire))
}

export async function postFixedPeriodQuestionnaireAnswer({
  id,
  body
}: {
  id: UUID
  body: FixedPeriodsBody
}): Promise<void> {
  return client
    .post(`/citizen/holiday-period/questionnaire/fixed-period/${id}`, body)
    .then(() => undefined)
}

export function getIncomeExpirationDates(): Promise<LocalDate[]> {
  return client
    .get<JsonOf<LocalDate[]>>(`/citizen/income/expiring`)
    .then((res) =>
      res.data.map((expirationDate) => LocalDate.parseIso(expirationDate))
    )
}
