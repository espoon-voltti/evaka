// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback, useEffect, useMemo, useRef } from 'react'
import styled, { useTheme } from 'styled-components'

import { CitizenCalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import { ReservationResponseDay } from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { capitalizeFirstLetter } from 'lib-common/string'
import { scrollToPos } from 'lib-common/utils/scrolling'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faCalendar } from 'lib-icons'

import { useLang, useTranslation } from '../localization'
import { headerHeightMobile } from '../navigation/const'

import {
  CalendarEventCount,
  CalendarEventCountContainer
} from './CalendarEventCount'
import { countEventsForDay } from './CalendarGridView'
import { HistoryOverlay } from './HistoryOverlay'
import { ChildImageData } from './RoundChildImages'
import { Reservations } from './calendar-elements'

interface DayProps {
  calendarDay: ReservationResponseDay
  selectDate: (date: LocalDate) => void
  isReservable: boolean
  isHolidayPeriod: boolean
  childImages: ChildImageData[]
  events: CitizenCalendarEvent[]
}

export default React.memo(function DayElem({
  calendarDay,
  selectDate,
  isReservable,
  isHolidayPeriod,
  childImages,
  events
}: DayProps) {
  const [lang] = useLang()
  const ref = useRef<HTMLButtonElement>()

  const isToday = calendarDay.date.isToday()
  const setRef = useCallback(
    (e: HTMLButtonElement) => {
      if (isToday) {
        ref.current = e ?? undefined
      }
    },
    [isToday]
  )

  const highlight = useMemo(
    () =>
      calendarDay.children.every((c) => c.absence && !c.absence.editable)
        ? 'nonEditableAbsence'
        : isHolidayPeriod
          ? 'holidayPeriod'
          : undefined,
    [calendarDay.children, isHolidayPeriod]
  )

  const handleClick = useCallback(() => {
    selectDate(calendarDay.date)
  }, [selectDate, calendarDay.date])

  useEffect(() => {
    const top = ref.current?.getBoundingClientRect().top

    if (top) {
      scrollToPos({
        top: top - headerHeightMobile - 32
      })
    }
  }, [])

  const eventCount = useMemo(
    () => countEventsForDay(events, calendarDay.date),
    [calendarDay.date, events]
  )

  const i18n = useTranslation()
  const theme = useTheme()

  return (
    <Day
      ref={setRef}
      $today={calendarDay.date.isToday()}
      $highlight={highlight}
      onClick={handleClick}
      data-qa={`mobile-calendar-day-${calendarDay.date.formatIso()}`}
    >
      <DayColumn
        spacing="xxs"
        inactive={!isReservable}
        holiday={calendarDay.holiday}
      >
        <div aria-label={calendarDay.date.formatExotic('EEEE', lang)}>
          {capitalizeFirstLetter(calendarDay.date.format('EEEEEE', lang))}
        </div>
        <div aria-label={calendarDay.date.formatExotic('do MMMM', lang)}>
          {calendarDay.date.format('d.M.')}
        </div>
      </DayColumn>
      <Gap size="s" horizontal />
      <ReservationsContainer data-qa="reservations">
        <Reservations
          data={calendarDay}
          childImages={childImages}
          isReservable={isReservable}
          backgroundHighlight={highlight}
        />
      </ReservationsContainer>
      {eventCount > 0 && (
        <CalendarEventCountContainer
          aria-label={`${eventCount} ${i18n.calendar.eventsCount}`}
        >
          <FontAwesomeIcon color={theme.colors.main.m2} icon={faCalendar} />
          <CalendarEventCount data-qa="event-count">
            {eventCount}
          </CalendarEventCount>
        </CalendarEventCountContainer>
      )}
      {calendarDay.date.isBefore(LocalDate.todayInSystemTz()) && (
        <HistoryOverlay />
      )}
    </Day>
  )
})
const ReservationsContainer = styled.div`
  flex: 1 0 0;
`
const Day = styled.button<{
  $today: boolean
  $highlight?: 'nonEditableAbsence' | 'holidayPeriod' | undefined
}>`
  display: flex;
  flex-direction: row;
  width: 100%;
  position: relative;
  padding: calc(${defaultMargins.s} - 6px) ${defaultMargins.s};
  background: transparent;
  margin: 0;
  border: none;
  border-bottom: 1px solid ${colors.grayscale.g15};
  border-left: 6px solid
    ${(p) => (p.$today ? colors.status.success : 'transparent')};
  cursor: pointer;
  text-align: left;
  color: ${(p) => p.theme.colors.grayscale.g100};

  ${(p) =>
    p.$highlight === 'nonEditableAbsence'
      ? `background-color: ${colors.grayscale.g15}`
      : p.$highlight === 'holidayPeriod'
        ? `background-color: ${colors.accents.a10powder}`
        : undefined};

  :focus {
    outline: 2px solid ${(p) => p.theme.colors.main.m2Focus};
  }
`
const DayColumn = styled(FixedSpaceColumn)<{
  inactive: boolean
  holiday: boolean
}>`
  width: 3rem;
  color: ${(p) =>
    p.inactive
      ? colors.grayscale.g70
      : p.holiday
        ? colors.accents.a2orangeDark
        : colors.main.m1};
  font-weight: ${fontWeights.semibold};
  font-size: 1.25rem;
`
