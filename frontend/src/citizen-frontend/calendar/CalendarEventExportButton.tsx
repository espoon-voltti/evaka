// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import ical, { ICalCalendarMethod } from 'ical-generator'
import React, { useCallback } from 'react'

import { Button } from 'lib-components/atoms/buttons/Button'
import { faCalendar } from 'lib-icons'

import { useTranslation } from '../localization'

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

interface EventLocationInfo {
  groupName: string | null
  unitName: string | null
}

interface ExportEventDetails {
  fileName: string
  title: string
  helsinkiStartTime: string
  helsinkiEndTime: string
  allDay?: boolean
  locationInfo?: EventLocationInfo
}

interface CalendarEventExportProps {
  eventDetails: ExportEventDetails
  'data-qa': string
}

export const CalendarEventExportButton = React.memo(
  function CalendarEventExportButton({
    eventDetails,
    'data-qa': dataQa
  }: CalendarEventExportProps) {
    const i18n = useTranslation()
    const downloadIcs = useCallback(() => {
      const calendar = ical()
      calendar.method(ICalCalendarMethod.REQUEST)

      //all day events should not use timezones
      if (!eventDetails.allDay) {
        calendar.timezone({
          name: 'Europe/Helsinki',
          generator: () => helsinkiVTZLines.join('\r\n')
        })
      }
      calendar.createEvent({
        summary: eventDetails.title,
        location: `${eventDetails.locationInfo?.unitName ?? ''} ${eventDetails.locationInfo?.groupName ? `(${eventDetails.locationInfo.groupName})` : ''}`,
        start: eventDetails.helsinkiStartTime,
        end: eventDetails.helsinkiEndTime,
        timezone: eventDetails.allDay ? undefined : 'Europe/Helsinki',
        allDay: eventDetails.allDay
      })

      const link = document.createElement('a')
      link.download = eventDetails.fileName
      link.href = `data:text/calendar;charset=utf-8,${encodeURIComponent(calendar.toString())}`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
    }, [eventDetails])

    return (
      <Button
        data-qa={dataQa}
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
