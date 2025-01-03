// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

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
} from 'employee-frontend/generated/api-clients/calendarevent'
import { CalendarEventId, GroupId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { mutation, parametricMutation, query } from 'lib-common/query'
import { Arg0, UUID } from 'lib-common/types'

import { createQueryKeys } from '../../../query'

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
  api: getGroupDiscussionSurveys,
  queryKey: ({ groupId }) => queryKeys.groupDiscussionSurveys(groupId)
})

export const discussionSurveyQuery = query({
  api: getCalendarEvent,
  queryKey: ({ id }) => queryKeys.discussionSurvey(id)
})

export const groupDiscussionReservationDaysQuery = query({
  api: getGroupDiscussionReservationDays,
  queryKey: ({ unitId, groupId, start, end }) =>
    queryKeys.groupDiscussionReservationDays(unitId, groupId, start, end)
})

export const createCalendarEventMutation = mutation({
  api: (arg: Arg0<typeof createCalendarEvent>) => createCalendarEvent(arg),
  invalidateQueryKeys: () => []
})

export const deleteCalendarEventMutation = parametricMutation<GroupId>()({
  api: deleteCalendarEvent,
  invalidateQueryKeys: (groupId) => [queryKeys.groupDiscussionSurveys(groupId)]
})

export const updateCalendarEventMutation = mutation({
  api: (arg: Arg0<typeof updateCalendarEvent>) => updateCalendarEvent(arg),
  invalidateQueryKeys: ({ id }) => [queryKeys.discussionSurvey(id)]
})

export const setCalendarEventTimeReservationMutation =
  parametricMutation<CalendarEventId>()({
    api: setCalendarEventTimeReservation,
    invalidateQueryKeys: (eventId) => [queryKeys.discussionSurvey(eventId)]
  })

export const clearChildCalendarEventTimeReservationsForSurveyMutation =
  mutation({
    api: (arg: Arg0<typeof clearEventTimesInEventForChild>) =>
      clearEventTimesInEventForChild(arg),
    invalidateQueryKeys: ({ body }) => [
      queryKeys.discussionSurvey(body.calendarEventId)
    ]
  })

export const addCalendarEventTimeMutation = mutation({
  api: (arg: Arg0<typeof addCalendarEventTime>) => addCalendarEventTime(arg),
  invalidateQueryKeys: ({ id }) => [queryKeys.discussionSurvey(id)]
})

export const deleteCalendarEventTimeMutation =
  parametricMutation<CalendarEventId>()({
    api: deleteCalendarEventTime,
    invalidateQueryKeys: (eventId) => [queryKeys.discussionSurvey(eventId)]
  })
