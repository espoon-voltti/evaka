// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type {
  CalendarEventId,
  DaycareId,
  GroupId
} from 'lib-common/generated/api-types/shared'
import { Queries } from 'lib-common/query'

import {
  addCalendarEventTime,
  clearEventTimesInEventForChild,
  createCalendarEvent,
  deleteCalendarEvent,
  deleteCalendarEventTime,
  getCalendarEvent,
  getGroupDiscussionReservationDays,
  getGroupDiscussionSurveys,
  setCalendarEventTimeReservation,
  updateCalendarEvent
} from '../../../generated/api-clients/calendarevent'

const q = new Queries()

export const groupDiscussionSurveysQuery = q.query(getGroupDiscussionSurveys)

export const discussionSurveyQuery = q.query(getCalendarEvent)

export const groupDiscussionReservationDaysQuery = q.query(
  getGroupDiscussionReservationDays
)

export const createCalendarEventMutation = q.mutation(createCalendarEvent)

export const deleteCalendarEventMutation = q.parametricMutation<{
  unitId: DaycareId
  groupId: GroupId
}>()(deleteCalendarEvent, [
  ({ unitId, groupId }) => groupDiscussionSurveysQuery({ unitId, groupId })
])

export const updateCalendarEventMutation = q.mutation(updateCalendarEvent, [
  ({ id }) => discussionSurveyQuery({ id })
])

export const setCalendarEventTimeReservationMutation = q.parametricMutation<{
  eventId: CalendarEventId
}>()(setCalendarEventTimeReservation, [
  ({ eventId }) => discussionSurveyQuery({ id: eventId })
])

export const clearChildCalendarEventTimeReservationsForSurveyMutation =
  q.mutation(clearEventTimesInEventForChild, [
    ({ body: { calendarEventId } }) =>
      discussionSurveyQuery({ id: calendarEventId })
  ])

export const addCalendarEventTimeMutation = q.mutation(addCalendarEventTime, [
  ({ id }) => discussionSurveyQuery({ id })
])

export const deleteCalendarEventTimeMutation = q.parametricMutation<{
  eventId: CalendarEventId
}>()(deleteCalendarEventTime, [
  ({ eventId }) => discussionSurveyQuery({ id: eventId })
])
