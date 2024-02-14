// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace, @typescript-eslint/no-redundant-type-constituents */

import LocalDate from 'lib-common/local-date'
import { CalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { client } from '../../client'
import { deserializeJsonCalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import { uri } from 'lib-common/uri'


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
  const { data: json } = await client.request<JsonOf<CalendarEvent[]>>({
    url: uri`/units/${request.unitId}/calendar-events`.toString(),
    method: 'GET',
    params: {
      start: request.start.formatIso(),
      end: request.end.formatIso()
    }
  })
  return json.map(e => deserializeJsonCalendarEvent(e))
}
