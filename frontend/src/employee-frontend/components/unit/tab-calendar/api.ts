// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import {
  CalendarEvent,
  CalendarEventForm,
  CalendarEventTime,
  CalendarEventUpdateForm
} from 'lib-common/generated/api-types/calendarevent'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { UUID } from 'lib-common/types'

import { client } from '../../../api/client'

export function getCalendarEventsOfUnit(unitId: UUID): Promise<CalendarEvent> {
  return client
    .get<JsonOf<CalendarEvent>>(`/units/${unitId}/calendar-events`)
    .then((res) => deserializeCalendarEvent(res.data))
}

export function getCalendarEvent(id: UUID): Promise<CalendarEvent> {
  return client
    .get<JsonOf<CalendarEvent>>(`/calendar-event/${id}`)
    .then((res) => deserializeCalendarEvent(res.data))
}

export function getDiscussionSurveysOfGroup(
  unitId: UUID,
  groupId: UUID
): Promise<CalendarEvent[]> {
  return client
    .get<
      JsonOf<CalendarEvent[]>
    >(`/units/${unitId}/groups/${groupId}/discussion-surveys`)
    .then((res) => res.data.map(deserializeCalendarEvent))
}

export function createCalendarEvent({
  form
}: {
  form: CalendarEventForm
}): Promise<void> {
  return client.post(`/calendar-event`, form).then(() => undefined)
}

export function updateCalendarEvent({
  eventId,
  form
}: {
  eventId: UUID
  form: CalendarEventUpdateForm
}): Promise<void> {
  return client.put(`/calendar-event/${eventId}`, form).then(() => undefined)
}

export function deleteCalendarEventOfUnit(eventId: UUID): Promise<void> {
  return client.delete(`/calendar-event/${eventId}`).then(() => undefined)
}

export const deserializeCalendarEvent = (
  data: JsonOf<CalendarEvent>
): CalendarEvent => ({
  ...data,
  period: FiniteDateRange.parseJson(data.period),
  times: data.times.map((t) => parseCalendarEventTime(t))
})

function parseCalendarEventTime(
  eventTime: JsonOf<CalendarEventTime>
): CalendarEventTime {
  return {
    ...eventTime,
    date: LocalDate.parseIso(eventTime.date),
    startTime: LocalTime.parseIso(eventTime.startTime),
    endTime: LocalTime.parseIso(eventTime.endTime)
  }
}
