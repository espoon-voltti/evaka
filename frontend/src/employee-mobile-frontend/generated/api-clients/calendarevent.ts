// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import LocalDate from 'lib-common/local-date'
import { CalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import { DiscussionReservationDay } from 'lib-common/generated/api-types/calendarevent'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { client } from '../../client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonCalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import { deserializeJsonDiscussionReservationDay } from 'lib-common/generated/api-types/calendarevent'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventController.getGroupCalendarEventsWithTimes
*/
export async function getGroupCalendarEventsWithTimes(
  request: {
    unitId: UUID,
    groupId: UUID
  }
): Promise<CalendarEvent[]> {
  const { data: json } = await client.request<JsonOf<CalendarEvent[]>>({
    url: uri`/units/${request.unitId}/groups/${request.groupId}/discussion-surveys`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonCalendarEvent(e))
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
  }
): Promise<DiscussionReservationDay[]> {
  const params = createUrlSearchParams(
    ['start', request.start.formatIso()],
    ['end', request.end.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<DiscussionReservationDay[]>>({
    url: uri`/units/${request.unitId}/groups/${request.groupId}/discussion-reservation-days`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonDiscussionReservationDay(e))
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
