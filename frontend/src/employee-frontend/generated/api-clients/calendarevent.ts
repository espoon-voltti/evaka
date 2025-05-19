// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { CalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import type { CalendarEventForm } from 'lib-common/generated/api-types/calendarevent'
import type { CalendarEventId } from 'lib-common/generated/api-types/shared'
import type { CalendarEventTimeClearingForm } from 'lib-common/generated/api-types/calendarevent'
import type { CalendarEventTimeEmployeeReservationForm } from 'lib-common/generated/api-types/calendarevent'
import type { CalendarEventTimeForm } from 'lib-common/generated/api-types/calendarevent'
import type { CalendarEventTimeId } from 'lib-common/generated/api-types/shared'
import type { CalendarEventUpdateForm } from 'lib-common/generated/api-types/calendarevent'
import type { DaycareId } from 'lib-common/generated/api-types/shared'
import type { DiscussionReservationDay } from 'lib-common/generated/api-types/calendarevent'
import type { GroupId } from 'lib-common/generated/api-types/shared'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
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
    id: CalendarEventId,
    body: CalendarEventTimeForm
  }
): Promise<CalendarEventTimeId> {
  const { data: json } = await client.request<JsonOf<CalendarEventTimeId>>({
    url: uri`/employee/calendar-event/${request.id}/time`.toString(),
    method: 'POST',
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
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/calendar-event/clear-survey-reservations-for-child`.toString(),
    method: 'POST',
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
  }
): Promise<CalendarEventId> {
  const { data: json } = await client.request<JsonOf<CalendarEventId>>({
    url: uri`/employee/calendar-event`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<CalendarEventForm>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.deleteCalendarEvent
*/
export async function deleteCalendarEvent(
  request: {
    id: CalendarEventId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/calendar-event/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.deleteCalendarEventTime
*/
export async function deleteCalendarEventTime(
  request: {
    id: CalendarEventTimeId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/calendar-event-time/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.getCalendarEvent
*/
export async function getCalendarEvent(
  request: {
    id: CalendarEventId
  }
): Promise<CalendarEvent> {
  const { data: json } = await client.request<JsonOf<CalendarEvent>>({
    url: uri`/employee/calendar-event/${request.id}`.toString(),
    method: 'GET'
  })
  return deserializeJsonCalendarEvent(json)
}


/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.getGroupDiscussionReservationDays
*/
export async function getGroupDiscussionReservationDays(
  request: {
    unitId: DaycareId,
    groupId: GroupId,
    start: LocalDate,
    end: LocalDate
  }
): Promise<DiscussionReservationDay[]> {
  const params = createUrlSearchParams(
    ['start', request.start.formatIso()],
    ['end', request.end.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<DiscussionReservationDay[]>>({
    url: uri`/employee/units/${request.unitId}/groups/${request.groupId}/discussion-reservation-days`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonDiscussionReservationDay(e))
}


/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.getGroupDiscussionSurveys
*/
export async function getGroupDiscussionSurveys(
  request: {
    unitId: DaycareId,
    groupId: GroupId
  }
): Promise<CalendarEvent[]> {
  const { data: json } = await client.request<JsonOf<CalendarEvent[]>>({
    url: uri`/employee/units/${request.unitId}/groups/${request.groupId}/discussion-surveys`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonCalendarEvent(e))
}


/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.getUnitCalendarEvents
*/
export async function getUnitCalendarEvents(
  request: {
    unitId: DaycareId,
    start: LocalDate,
    end: LocalDate
  }
): Promise<CalendarEvent[]> {
  const params = createUrlSearchParams(
    ['start', request.start.formatIso()],
    ['end', request.end.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<CalendarEvent[]>>({
    url: uri`/employee/units/${request.unitId}/calendar-events`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonCalendarEvent(e))
}


/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.modifyCalendarEvent
*/
export async function modifyCalendarEvent(
  request: {
    id: CalendarEventId,
    body: CalendarEventUpdateForm
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/calendar-event/${request.id}`.toString(),
    method: 'PATCH',
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
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/calendar-event/reservation`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<CalendarEventTimeEmployeeReservationForm>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.updateCalendarEvent
*/
export async function updateCalendarEvent(
  request: {
    id: CalendarEventId,
    body: CalendarEventUpdateForm
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/calendar-event/${request.id}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<CalendarEventUpdateForm>
  })
  return json
}
