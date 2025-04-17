// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import ical, { ICalCalendarMethod } from 'ical-generator'
import React, { useCallback } from 'react'

import { useTranslation } from 'citizen-frontend/localization'
import {
  CitizenCalendarEvent,
  CitizenCalendarEventTime
} from 'lib-common/generated/api-types/calendarevent'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { Button } from 'lib-components/atoms/buttons/Button'
import { faCalendar } from 'lib-icons'

const helsinkiVTZLines = [
  'BEGIN:VTIMEZONE',
  'TZID:Europe/Helsinki',
  'BEGIN:STANDARD',
  'TZNAME:EET',
  'TZOFFSETFROM:+0300',
  'TZOFFSETTO:+0200',
  'DTSTART:19701025T040000',
  'RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU',
  'END:STANDARD',
  'BEGIN:DAYLIGHT',
  'TZNAME:EEST',
  'TZOFFSETFROM:+0200',
  'TZOFFSETTO:+0300',
  'DTSTART:19700329T030000',
  'RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU',
  'END:DAYLIGHT',
  'END:VTIMEZONE'
]

interface CalendarEventAttendeeInfo {
  groupName: string | null
  unitName: string | null
}

interface CalendarEventExportProps {
  discussionTime?: CitizenCalendarEventTime
  calendarEvent: CitizenCalendarEvent
  eventAttendeeInfo?: CalendarEventAttendeeInfo
}

export const CalendarEventExportButton = React.memo(
  function CalendarEventExportButton({
    discussionTime,
    calendarEvent,
    eventAttendeeInfo
  }: CalendarEventExportProps) {
    const i18n = useTranslation()
    const downloadIcs = useCallback(() => {
      const fileName = discussionTime
        ? `${i18n.calendar.discussionTimeReservation.discussionTimeFileName}_${discussionTime.date.formatIso()}.ics`
        : `${i18n.calendar.calendarEventFilename}_${calendarEvent.period.start.formatIso()}-${calendarEvent.period.end.formatIso()}.ics`
      const calendar = ical()
      calendar.method(ICalCalendarMethod.REQUEST)
      calendar.timezone({
        name: 'Europe/Helsinki',
        generator: () => helsinkiVTZLines.join('\r\n')
      })
      calendar.createEvent({
        summary: calendarEvent.title,
        location: `${eventAttendeeInfo?.unitName ?? ''} ${eventAttendeeInfo?.groupName ? `(${eventAttendeeInfo.groupName})` : ''}`,
        start: discussionTime
          ? HelsinkiDateTime.fromLocal(
              discussionTime.date,
              discussionTime.startTime
            ).formatIso()
          : calendarEvent.period.start.formatIso(),
        end: discussionTime
          ? HelsinkiDateTime.fromLocal(
              discussionTime.date,
              discussionTime.endTime
            ).formatIso()
          : calendarEvent.period.end.formatIso(),
        timezone: 'Europe/Helsinki'
      })
      const link = document.createElement('a')
      link.download = fileName
      link.href = `data:text/calendar;charset=utf-8,${encodeURIComponent(calendar.toString())}`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
    }, [i18n, calendarEvent, eventAttendeeInfo, discussionTime])

    return (
      <Button
        data-qa={`event-export-button-${discussionTime ? discussionTime.id : calendarEvent.id}`}
        appearance="inline"
        text={i18n.calendar.discussionTimeReservation.calendarExportButtonLabel}
        onClick={() => {
          downloadIcs()
        }}
        icon={faCalendar}
      />
    )
  }
)
