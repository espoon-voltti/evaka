// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { mutation, query } from 'lib-common/query'

import { createQueryKeys } from '../query'

import {
  getCalendarEvents,
  getDailyServiceTimeNotifications,
  getReservations,
  postAbsences,
  postReservations
} from './api'

export const queryKeys = createQueryKeys('calendar', {
  allReservations: () => ['reservations'],
  reservations: (from: LocalDate, to: LocalDate) => [
    'reservations',
    from.formatIso(),
    to.formatIso()
  ],
  calendarEvents: (from: LocalDate, to: LocalDate) => [
    'calendarEvents',
    from.formatIso(),
    to.formatIso()
  ],
  dailyServiceTimeNotifications: () => ['dailyServiceTimeNotifications']
})

export const reservationsQuery = query({
  api: getReservations,
  queryKey: queryKeys.reservations
})

export const calendarEventsQuery = query({
  api: getCalendarEvents,
  queryKey: queryKeys.calendarEvents
})

export const dailyServiceTimeNotificationsQuery = query({
  api: getDailyServiceTimeNotifications,
  queryKey: queryKeys.dailyServiceTimeNotifications
})

export const postReservationsMutation = mutation({
  api: postReservations,
  invalidateQueryKeys: () => [queryKeys.allReservations()]
})

export const postAbsencesMutation = mutation({
  api: postAbsences,
  invalidateQueryKeys: () => [queryKeys.allReservations()]
})
