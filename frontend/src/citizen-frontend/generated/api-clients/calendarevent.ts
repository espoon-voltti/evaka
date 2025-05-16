// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { CalendarEventTimeCitizenReservationForm } from 'lib-common/generated/api-types/calendarevent'
import type { CalendarEventTimeId } from 'lib-common/generated/api-types/shared'
import type { CitizenCalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import { client } from '../../api-client'
import { createUrlSearchParams } from 'lib-common/api'
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
