// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  CalendarEventForm,
  CalendarEventTimeEmployeeReservationForm,
  CalendarEventTimeForm
} from 'lib-common/generated/api-types/calendarevent'
import LocalDate from 'lib-common/local-date'
import { mutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { createQueryKeys } from '../../../query'

import {
  addCalendarEventTime,
  createCalendarEvent,
  deleteCalendarEventOfUnit,
  deleteCalendarEventTime,
  getCalendarEvent,
  getDiscussionReservationDaysForGroup,
  getDiscussionSurveysOfGroup,
  setCalendarEventTimeReservation,
  updateCalendarEvent
} from './api'

export const queryKeys = createQueryKeys('calendarEvent', {
  groupDiscussionSurveys: (groupId: UUID) => [
    'groupDiscussionSurveys',
    groupId
  ],
  discussionSurvey: (eventId: UUID) => ['discussionSurvey', eventId],
  groupDiscussionReservationDays: (
    unitId: UUID,
    groupId: UUID,
    start: LocalDate,
    end: LocalDate
  ) => ['groupDiscussionReservationDays', unitId, groupId, start, end]
})

export const groupDiscussionSurveysQuery = query({
  api: getDiscussionSurveysOfGroup,
  queryKey: queryKeys.groupDiscussionSurveys
})

export const discussionSurveyQuery = query({
  api: getCalendarEvent,
  queryKey: queryKeys.discussionSurvey
})

export const groupDiscussionReservationDaysQuery = query({
  api: getDiscussionReservationDaysForGroup,
  queryKey: queryKeys.groupDiscussionReservationDays
})

export const createCalendarEventMutation = mutation({
  api: ({ eventId: _, form }: { eventId: UUID; form: CalendarEventForm }) =>
    createCalendarEvent({ form }),
  invalidateQueryKeys: () => []
})

export const deleteCalendarEventMutation = mutation({
  api: ({ eventId, groupId: _ }: { eventId: UUID; groupId: UUID }) =>
    deleteCalendarEventOfUnit(eventId),
  invalidateQueryKeys: ({ groupId }) => [
    queryKeys.groupDiscussionSurveys(groupId)
  ]
})

export const updateCalendarEventMutation = mutation({
  api: ({ eventId, form }: { eventId: UUID; form: CalendarEventForm }) =>
    updateCalendarEvent({ eventId, form }),
  invalidateQueryKeys: ({ eventId }) => [queryKeys.discussionSurvey(eventId)]
})

export const setCalendarEventTimeReservationMutation = mutation({
  api: ({
    form,
    eventId: _
  }: {
    form: CalendarEventTimeEmployeeReservationForm
    eventId: UUID
  }) => setCalendarEventTimeReservation({ form }),
  invalidateQueryKeys: ({ eventId }) => [queryKeys.discussionSurvey(eventId)]
})

export const addCalendarEventTimeMutation = mutation({
  api: ({ eventId, form }: { eventId: UUID; form: CalendarEventTimeForm }) =>
    addCalendarEventTime({ eventId, form }),
  invalidateQueryKeys: ({ eventId }) => [queryKeys.discussionSurvey(eventId)]
})

export const deleteCalendarEventTimeMutation = mutation({
  api: ({ eventId: _, eventTimeId }: { eventId: UUID; eventTimeId: UUID }) =>
    deleteCalendarEventTime({ eventTimeId }),
  invalidateQueryKeys: ({ eventId }) => [queryKeys.discussionSurvey(eventId)]
})
