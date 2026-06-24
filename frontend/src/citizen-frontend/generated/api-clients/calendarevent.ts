// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { CalendarEventId } from 'lib-common/generated/api-types/shared'
import type { CalendarEventTimeCitizenReservationForm } from 'lib-common/generated/api-types/calendarevent'
import type { CalendarEventTimeId } from 'lib-common/generated/api-types/shared'
import type { CitizenCalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import type { Uri } from 'lib-common/uri'
import { client } from '../../api-client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonCitizenCalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import { uri } from 'lib-common/uri'


/**
* Generated from evaka.core.calendarevent.CalendarEventController.addCalendarEventTimeReservation
*/
export async function addCalendarEventTimeReservation(
  request: {
    body: CalendarEventTimeCitizenReservationForm
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/calendar-event/reservation`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<CalendarEventTimeCitizenReservationForm>
  })
  return json
}


/**
* Generated from evaka.core.calendarevent.CalendarEventController.deleteCalendarEventTimeReservation
*/
export async function deleteCalendarEventTimeReservation(
  request: {
    calendarEventTimeId: CalendarEventTimeId,
    childId: PersonId
  }
): Promise<void> {
  const params = createUrlSearchParams(
    ['calendarEventTimeId', request.calendarEventTimeId],
    ['childId', request.childId]
  )
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/calendar-event/reservation`.toString(),
    method: 'DELETE',
    params
  })
  return json
}


/**
* Generated from evaka.core.calendarevent.CalendarEventController.exportCitizenCalendarEventIcs
*/
export function exportCitizenCalendarEventIcs(
  request: {
    eventId: CalendarEventId,
    childId: PersonId,
    date: LocalDate
  }
): { url: Uri } {
  const params = createUrlSearchParams(
    ['childId', request.childId],
    ['date', request.date.formatIso()]
  )
  return {
    url: uri`/citizen/calendar-events/${request.eventId}/ics`.withBaseUrl(client.defaults.baseURL ?? '').appendQuery(params)
  }
}


/**
* Generated from evaka.core.calendarevent.CalendarEventController.exportCitizenDiscussionReservationIcs
*/
export function exportCitizenDiscussionReservationIcs(
  request: {
    eventTimeId: CalendarEventTimeId
  }
): { url: Uri } {
  return {
    url: uri`/citizen/calendar-event-times/${request.eventTimeId}/ics`.withBaseUrl(client.defaults.baseURL ?? '')
  }
}


/**
* Generated from evaka.core.calendarevent.CalendarEventController.getCitizenCalendarEvents
*/
export async function getCitizenCalendarEvents(
  request: {
    start: LocalDate,
    end: LocalDate
  }
): Promise<CitizenCalendarEvent[]> {
  const params = createUrlSearchParams(
    ['start', request.start.formatIso()],
    ['end', request.end.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<CitizenCalendarEvent[]>>({
    url: uri`/citizen/calendar-events`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonCitizenCalendarEvent(e))
}
