// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import LocalDate from 'lib-common/local-date'
import { CalendarEventId } from 'lib-common/generated/api-types/shared'
import { CalendarEventTime } from 'lib-common/generated/api-types/calendarevent'
import { CalendarEventTimeCitizenReservationForm } from 'lib-common/generated/api-types/calendarevent'
import { CalendarEventTimeId } from 'lib-common/generated/api-types/shared'
import { CitizenCalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { PersonId } from 'lib-common/generated/api-types/shared'
import { client } from '../../api-client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonCalendarEventTime } from 'lib-common/generated/api-types/calendarevent'
import { deserializeJsonCitizenCalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.addCalendarEventTimeReservation
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
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.deleteCalendarEventTimeReservation
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
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.getCitizenCalendarEvents
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


/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.getReservableCalendarEventTimes
*/
export async function getReservableCalendarEventTimes(
  request: {
    eventId: CalendarEventId,
    childId: PersonId
  }
): Promise<CalendarEventTime[]> {
  const params = createUrlSearchParams(
    ['childId', request.childId]
  )
  const { data: json } = await client.request<JsonOf<CalendarEventTime[]>>({
    url: uri`/citizen/calendar-event/${request.eventId}/time`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonCalendarEventTime(e))
}
