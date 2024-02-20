// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import LocalDate from 'lib-common/local-date'
import { CalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import { CalendarEventForm } from 'lib-common/generated/api-types/calendarevent'
import { CalendarEventTimeForm } from 'lib-common/generated/api-types/calendarevent'
import { CalendarEventTimeReservationForm } from 'lib-common/generated/api-types/calendarevent'
import { CalendarEventUpdateForm } from 'lib-common/generated/api-types/calendarevent'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonCalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.addCalendarEventTime
*/
export async function addCalendarEventTime(
  request: {
    id: UUID,
    body: CalendarEventTimeForm
  }
): Promise<UUID> {
  const { data: json } = await client.request<JsonOf<UUID>>({
    url: uri`/calendar-event/${request.id}/time`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<CalendarEventTimeForm>
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
): Promise<UUID> {
  const { data: json } = await client.request<JsonOf<UUID>>({
    url: uri`/calendar-event`.toString(),
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
    id: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/calendar-event/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.deleteCalendarEventTime
*/
export async function deleteCalendarEventTime(
  request: {
    id: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/calendar-event-time/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.getCalendarEvent
*/
export async function getCalendarEvent(
  request: {
    id: UUID
  }
): Promise<CalendarEvent> {
  const { data: json } = await client.request<JsonOf<CalendarEvent>>({
    url: uri`/calendar-event/${request.id}`.toString(),
    method: 'GET'
  })
  return deserializeJsonCalendarEvent(json)
}


/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.getUnitCalendarEvents
*/
export async function getUnitCalendarEvents(
  request: {
    unitId: UUID,
    start: LocalDate,
    end: LocalDate
  }
): Promise<CalendarEvent[]> {
  const params = createUrlSearchParams(
    ['start', request.start.formatIso()],
    ['end', request.end.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<CalendarEvent[]>>({
    url: uri`/units/${request.unitId}/calendar-events`.toString(),
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
    id: UUID,
    body: CalendarEventUpdateForm
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/calendar-event/${request.id}`.toString(),
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
    body: CalendarEventTimeReservationForm
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/calendar-event/reservation`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<CalendarEventTimeReservationForm>
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
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/calendar-event/${request.id}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<CalendarEventUpdateForm>
  })
  return json
}
