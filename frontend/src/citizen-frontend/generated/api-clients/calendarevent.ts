// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace, @typescript-eslint/no-redundant-type-constituents */

import LocalDate from 'lib-common/local-date'
import { CalendarEventTime } from 'lib-common/generated/api-types/calendarevent'
import { CalendarEventTimeReservationForm } from 'lib-common/generated/api-types/calendarevent'
import { CitizenCalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { client } from '../../api-client'
import { deserializeJsonCalendarEventTime } from 'lib-common/generated/api-types/calendarevent'
import { deserializeJsonCitizenCalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.addCalendarEventTimeReservation
*/
export async function addCalendarEventTimeReservation(
  request: {
    body: CalendarEventTimeReservationForm
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/calendar-event/reservation`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<CalendarEventTimeReservationForm>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.deleteCalendarEventTimeReservation
*/
export async function deleteCalendarEventTimeReservation(
  request: {
    calendarEventTimeId: UUID,
    childId: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/calendar-event/reservation`.toString(),
    method: 'DELETE',
    params: {
      calendarEventTimeId: request.calendarEventTimeId,
      childId: request.childId
    }
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
  const { data: json } = await client.request<JsonOf<CitizenCalendarEvent[]>>({
    url: uri`/citizen/calendar-events`.toString(),
    method: 'GET',
    params: {
      start: request.start.formatIso(),
      end: request.end.formatIso()
    }
  })
  return json.map(e => deserializeJsonCitizenCalendarEvent(e))
}


/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.getReservableCalendarEventTimes
*/
export async function getReservableCalendarEventTimes(
  request: {
    eventId: UUID,
    childId: UUID
  }
): Promise<CalendarEventTime[]> {
  const { data: json } = await client.request<JsonOf<CalendarEventTime[]>>({
    url: uri`/citizen/calendar-event/${request.eventId}/time`.toString(),
    method: 'GET',
    params: {
      childId: request.childId
    }
  })
  return json.map(e => deserializeJsonCalendarEventTime(e))
}
