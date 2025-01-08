// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'

import { Queries } from 'lib-common/query'

import {
  addCalendarEventTimeReservation,
  deleteCalendarEventTimeReservation,
  getCitizenCalendarEvents
} from '../generated/api-clients/calendarevent'
import { getDailyServiceTimeNotifications } from '../generated/api-clients/dailyservicetimes'
import {
  answerFixedPeriodQuestionnaire,
  answerOpenRangeQuestionnaire,
  getActiveQuestionnaires,
  getHolidayPeriods
} from '../generated/api-clients/holidayperiod'
import { getExpiringIncome } from '../generated/api-clients/invoicing'
import {
  getReservations,
  postAbsences,
  postReservations
} from '../generated/api-clients/reservations'

const q = new Queries()

export const reservationsQuery = q.query(getReservations)

export const calendarEventsQuery = q.query(getCitizenCalendarEvents)

export const dailyServiceTimeNotificationsQuery = q.query(
  getDailyServiceTimeNotifications
)

export const postReservationsMutation = q.mutation(postReservations, [
  reservationsQuery.prefix
])

export const postAbsencesMutation = q.mutation(postAbsences, [
  reservationsQuery.prefix
])

export const addCalendarEventTimeReservationMutation = q.mutation(
  addCalendarEventTimeReservation,
  [calendarEventsQuery.prefix]
)

export const deleteCalendarEventTimeReservationMutation = q.mutation(
  deleteCalendarEventTimeReservation,
  [calendarEventsQuery.prefix]
)

export const holidayPeriodsQuery = q.query(getHolidayPeriods)

export const activeQuestionnaireQuery = q.query(() =>
  getActiveQuestionnaires().then((questionnaires) =>
    questionnaires.length > 0 ? questionnaires[0] : null
  )
)

export const answerFixedPeriodQuestionnaireMutation = q.mutation(
  answerFixedPeriodQuestionnaire,
  [() => activeQuestionnaireQuery(), reservationsQuery.prefix]
)

export const answerOpenRangesQuestionnaireMutation = q.mutation(
  answerOpenRangeQuestionnaire,
  [() => activeQuestionnaireQuery(), reservationsQuery.prefix]
)

export const incomeExpirationDatesQuery = q.query(() =>
  getExpiringIncome().then((incomeExpirationDates) =>
    incomeExpirationDates.length > 0
      ? sortBy(incomeExpirationDates, (d) => d)[0]
      : null
  )
)
