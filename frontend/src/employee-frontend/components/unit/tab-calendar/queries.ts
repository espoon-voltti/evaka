// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { CalendarEventForm } from 'lib-common/generated/api-types/calendarevent'
import { mutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { createQueryKeys } from '../../../query'

import {
  createCalendarEvent,
  deleteCalendarEventOfUnit,
  getCalendarEvent,
  getDiscussionSurveysOfGroup,
  updateCalendarEvent
} from './api'

export const queryKeys = createQueryKeys('calendarEvent', {
  groupDiscussionSurveys: (groupId: UUID) => [
    'groupDiscussionSurveys',
    groupId
  ],
  discussionSurvey: (eventId: UUID) => ['discussionSurvey', eventId]
})

export const groupdiscussionSurveysQuery = query({
  api: getDiscussionSurveysOfGroup,
  queryKey: queryKeys.groupDiscussionSurveys
})

export const discussionSurveyQuery = query({
  api: getCalendarEvent,
  queryKey: queryKeys.discussionSurvey
})

export const createCalendarEventMutation = mutation({
  api: ({ eventId: _, form }: { eventId: UUID; form: CalendarEventForm }) =>
    createCalendarEvent({ form }),
  invalidateQueryKeys: () => []
})

export const deleteCalendarEventMutation = mutation({
  api: ({ eventId }: { unitId: UUID; eventId: UUID }) =>
    deleteCalendarEventOfUnit(eventId),
  invalidateQueryKeys: ({ eventId }) => [queryKeys.discussionSurvey(eventId)]
})

export const updateCalendarEventMutation = mutation({
  api: ({ eventId, form }: { eventId: UUID; form: CalendarEventForm }) =>
    updateCalendarEvent({ eventId, form }),
  invalidateQueryKeys: ({ eventId }) => [queryKeys.discussionSurvey(eventId)]
})
