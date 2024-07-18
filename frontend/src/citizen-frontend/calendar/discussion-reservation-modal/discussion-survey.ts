// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  CitizenCalendarEvent,
  CitizenCalendarEventTime
} from 'lib-common/generated/api-types/calendarevent'
import LocalDate from 'lib-common/local-date'

export function showModalEventTime(
  et: CitizenCalendarEventTime,
  comparisonDay: LocalDate
): boolean {
  //reserved and not in the past, or editable
  return (et.childId && et.date.isEqualOrAfter(comparisonDay)) || et.isEditable
}

export function showSurveyReservationToast(event: CitizenCalendarEvent) {
  return Object.values(event.timesByChild).some(
    (childTimes) =>
      childTimes.every((t) => t.childId === null) &&
      childTimes.some((t) => t.isEditable)
  )
}
