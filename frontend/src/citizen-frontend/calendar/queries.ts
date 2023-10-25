// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'

import LocalDate from 'lib-common/local-date'
import { mutation, query } from 'lib-common/query'

import { createQueryKeys } from '../query'

import {
  getActiveQuestionnaires,
  getCalendarEvents,
  getDailyServiceTimeNotifications,
  getHolidayPeriods,
  getIncomeExpirationDates,
  getReservations,
  postAbsences,
  postFixedPeriodQuestionnaireAnswer,
  postReservations
} from './api'

const queryKeys = createQueryKeys('calendar', {
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
  dailyServiceTimeNotifications: () => ['dailyServiceTimeNotifications'],
  holidayPeriods: () => ['holidayPeriods'],
  activeQuestionnaires: () => ['activeQuestionnaires'],
  incomeExpirationDates: () => ['incomeExpirationDates']
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

export const holidayPeriodsQuery = query({
  api: getHolidayPeriods,
  queryKey: queryKeys.holidayPeriods
})

export const activeQuestionnaireQuery = query({
  api: () =>
    getActiveQuestionnaires().then((questionnaires) =>
      questionnaires.length > 0 ? questionnaires[0] : null
    ),
  queryKey: queryKeys.activeQuestionnaires
})

export const answerFixedPeriodQuestionnaireMutation = mutation({
  api: postFixedPeriodQuestionnaireAnswer,
  invalidateQueryKeys: () => [
    activeQuestionnaireQuery().queryKey,
    queryKeys.allReservations()
  ]
})

export const incomeExpirationDatesQuery = query({
  api: () =>
    getIncomeExpirationDates().then((incomeExpirationDates) =>
      incomeExpirationDates.length > 0
        ? sortBy(incomeExpirationDates, (d) => d)[0]
        : null
    ),
  queryKey: queryKeys.incomeExpirationDates
})
