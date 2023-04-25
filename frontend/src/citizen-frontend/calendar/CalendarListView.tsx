// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useMemo } from 'react'
import styled from 'styled-components'

import type { CitizenCalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import type {
  ReservationChild,
  ReservationResponseDay
} from 'lib-common/generated/api-types/reservations'
import type LocalDate from 'lib-common/local-date'
import Button from 'lib-components/atoms/buttons/Button'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { defaultMargins } from 'lib-components/white-space'
import { faPlus } from 'lib-icons'

import { useTranslation } from '../localization'
import { mobileBottomNavHeight } from '../navigation/const'

import { getChildImages } from './RoundChildImages'
import WeekElem from './WeekElem'

export interface Props {
  childData: ReservationChild[]
  calendarDays: ReservationResponseDay[]
  onHoverButtonClick: () => void
  selectDate: (date: LocalDate) => void
  dayIsReservable: (date: LocalDate) => boolean
  dayIsHolidayPeriod: (date: LocalDate) => boolean
  events: CitizenCalendarEvent[]
}

export default React.memo(function CalendarListView({
  childData,
  calendarDays,
  dayIsHolidayPeriod,
  onHoverButtonClick,
  selectDate,
  dayIsReservable,
  events
}: Props) {
  const i18n = useTranslation()
  const calendarWeeks = useMemo(() => groupByWeek(calendarDays), [calendarDays])
  const childImages = useMemo(() => getChildImages(childData), [childData])

  return (
    <>
      <FixedSpaceColumn spacing="zero">
        {calendarWeeks.map((w) => (
          <WeekElem
            key={`${w.year}-${w.weekNumber}`}
            weekNumber={w.weekNumber}
            calendarDays={w.calendarDays}
            selectDate={selectDate}
            dayIsReservable={dayIsReservable}
            dayIsHolidayPeriod={dayIsHolidayPeriod}
            childImages={childImages}
            events={events}
          />
        ))}
      </FixedSpaceColumn>
      <HoverButton
        onClick={onHoverButtonClick}
        primary
        type="button"
        data-qa="open-calendar-actions-modal"
      >
        <Icon icon={faPlus} />
        {i18n.calendar.newReservationOrAbsence}
      </HoverButton>
    </>
  )
})

export interface CalendarWeek {
  year: number
  weekNumber: number
  calendarDays: ReservationResponseDay[]
}

export function groupByWeek(days: ReservationResponseDay[]): CalendarWeek[] {
  const weeks: CalendarWeek[] = []
  let currentWeek: CalendarWeek | undefined = undefined
  days.forEach((d) => {
    if (!currentWeek || currentWeek.weekNumber !== d.date.getIsoWeek()) {
      currentWeek = {
        year: d.date.year,
        weekNumber: d.date.getIsoWeek(),
        calendarDays: []
      }
      weeks.push(currentWeek)
    }
    currentWeek.calendarDays.push(d)
  })
  return weeks
}

const HoverButton = styled(Button)`
  position: fixed;
  bottom: calc(${defaultMargins.s} + ${mobileBottomNavHeight}px);
  right: ${defaultMargins.s};
  border-radius: 40px;
`

const Icon = styled(FontAwesomeIcon)`
  margin-right: ${defaultMargins.xs};
`
