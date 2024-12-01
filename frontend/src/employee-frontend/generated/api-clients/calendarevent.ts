// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import LocalDate from 'lib-common/local-date'
import { AxiosHeaders } from 'axios'
import { CalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import { CalendarEventForm } from 'lib-common/generated/api-types/calendarevent'
import { CalendarEventTimeClearingForm } from 'lib-common/generated/api-types/calendarevent'
import { CalendarEventTimeEmployeeReservationForm } from 'lib-common/generated/api-types/calendarevent'
import { CalendarEventTimeForm } from 'lib-common/generated/api-types/calendarevent'
import { CalendarEventUpdateForm } from 'lib-common/generated/api-types/calendarevent'
import { DiscussionReservationDay } from 'lib-common/generated/api-types/calendarevent'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonCalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import { deserializeJsonDiscussionReservationDay } from 'lib-common/generated/api-types/calendarevent'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.addCalendarEventTime
*/
export async function addCalendarEventTime(
  request: {
    id: UUID,
    body: CalendarEventTimeForm
  },
  headers?: AxiosHeaders
): Promise<UUID> {
  const { data: json } = await client.request<JsonOf<UUID>>({
    url: uri`/employee/calendar-event/${request.id}/time`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<CalendarEventTimeForm>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.clearEventTimesInEventForChild
*/
export async function clearEventTimesInEventForChild(
  request: {
    body: CalendarEventTimeClearingForm
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/calendar-event/clear-survey-reservations-for-child`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<CalendarEventTimeClearingForm>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.createCalendarEvent
*/
export async function createCalendarEvent(
  request: {
    body: CalendarEventForm
  },
  headers?: AxiosHeaders
): Promise<UUID> {
  const { data: json } = await client.request<JsonOf<UUID>>({
    url: uri`/employee/calendar-event`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<CalendarEventForm>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.deleteCalendarEvent
*/
export async function deleteCalendarEvent(
  request: {
    id: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/calendar-event/${request.id}`.toString(),
    method: 'DELETE',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.deleteCalendarEventTime
*/
export async function deleteCalendarEventTime(
  request: {
    id: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/calendar-event-time/${request.id}`.toString(),
    method: 'DELETE',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.getCalendarEvent
*/
export async function getCalendarEvent(
  request: {
    id: UUID
  },
  headers?: AxiosHeaders
): Promise<CalendarEvent> {
  const { data: json } = await client.request<JsonOf<CalendarEvent>>({
    url: uri`/employee/calendar-event/${request.id}`.toString(),
    method: 'GET',
    headers
  })
  return deserializeJsonCalendarEvent(json)
}


/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.getGroupDiscussionReservationDays
*/
export async function getGroupDiscussionReservationDays(
  request: {
    unitId: UUID,
    groupId: UUID,
    start: LocalDate,
    end: LocalDate
  },
  headers?: AxiosHeaders
): Promise<DiscussionReservationDay[]> {
  const params = createUrlSearchParams(
    ['start', request.start.formatIso()],
    ['end', request.end.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<DiscussionReservationDay[]>>({
    url: uri`/employee/units/${request.unitId}/groups/${request.groupId}/discussion-reservation-days`.toString(),
    method: 'GET',
    headers,
    params
  })
  return json.map(e => deserializeJsonDiscussionReservationDay(e))
}


/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.getGroupDiscussionSurveys
*/
export async function getGroupDiscussionSurveys(
  request: {
    unitId: UUID,
    groupId: UUID
  },
  headers?: AxiosHeaders
): Promise<CalendarEvent[]> {
  const { data: json } = await client.request<JsonOf<CalendarEvent[]>>({
    url: uri`/employee/units/${request.unitId}/groups/${request.groupId}/discussion-surveys`.toString(),
    method: 'GET',
    headers
  })
  return json.map(e => deserializeJsonCalendarEvent(e))
}


/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.getUnitCalendarEvents
*/
export async function getUnitCalendarEvents(
  request: {
    unitId: UUID,
    start: LocalDate,
    end: LocalDate
  },
  headers?: AxiosHeaders
): Promise<CalendarEvent[]> {
  const params = createUrlSearchParams(
    ['start', request.start.formatIso()],
    ['end', request.end.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<CalendarEvent[]>>({
    url: uri`/employee/units/${request.unitId}/calendar-events`.toString(),
    method: 'GET',
    headers,
    params
  })
  return json.map(e => deserializeJsonCalendarEvent(e))
}


/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.modifyCalendarEvent
*/
export async function modifyCalendarEvent(
  request: {
    id: UUID,
    body: CalendarEventUpdateForm
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/calendar-event/${request.id}`.toString(),
    method: 'PATCH',
    headers,
    data: request.body satisfies JsonCompatible<CalendarEventUpdateForm>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.setCalendarEventTimeReservation
*/
export async function setCalendarEventTimeReservation(
  request: {
    body: CalendarEventTimeEmployeeReservationForm
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/calendar-event/reservation`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<CalendarEventTimeEmployeeReservationForm>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.updateCalendarEvent
*/
export async function updateCalendarEvent(
  request: {
    id: UUID,
    body: CalendarEventUpdateForm
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/calendar-event/${request.id}`.toString(),
    method: 'PUT',
    headers,
    data: request.body satisfies JsonCompatible<CalendarEventUpdateForm>
  })
  return json
}
