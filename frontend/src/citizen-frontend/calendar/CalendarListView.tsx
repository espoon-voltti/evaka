// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useMemo, useRef } from 'react'
import styled from 'styled-components'

import type { CitizenCalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import type {
  ReservationChild,
  ReservationResponseDay
} from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { defaultMargins } from 'lib-components/white-space'
import { faPlus } from 'lib-icons'

import { useTranslation } from '../localization'
import { mobileBottomNavHeight } from '../navigation/const'

import MonthElem, { getSummaryForMonth, groupByMonth } from './MonthElem'
import { getChildImages } from './RoundChildImages'

export interface Props {
  childData: ReservationChild[]
  calendarDays: ReservationResponseDay[]
  onHoverButtonClick: () => void
  selectDate: (date: LocalDate) => void
  dayIsReservable: (date: LocalDate) => boolean
  dayIsHolidayPeriod: (date: LocalDate) => boolean
  events: CitizenCalendarEvent[]
  showDiscussionAction: boolean
  fetchPrevious: (beforeDate: LocalDate) => void
  loading: boolean
}

export default React.memo(function CalendarListView({
  childData,
  calendarDays,
  dayIsHolidayPeriod,
  onHoverButtonClick,
  selectDate,
  dayIsReservable,
  events,
  showDiscussionAction,
  fetchPrevious,
  loading
}: Props) {
  const i18n = useTranslation()
  const months = useMemo(() => groupByMonth(calendarDays), [calendarDays])
  const childImages = useMemo(() => getChildImages(childData), [childData])
  const scrollToDate = useRef<LocalDate>(LocalDate.todayInHelsinkiTz())

  const fetchPreviousMonths = () => {
    const beforeDate = months[0].calendarDays[0].date
    scrollToDate.current = beforeDate
    fetchPrevious(beforeDate)
  }

  return (
    <>
      <FixedSpaceColumn spacing="zero">
        {months.map((m, index) => (
          <MonthElem
            key={`month-${m.year}-${m.monthNumber}`}
            calendarMonth={m}
            selectDate={selectDate}
            dayIsReservable={dayIsReservable}
            dayIsHolidayPeriod={dayIsHolidayPeriod}
            childImages={childImages}
            events={events}
            childSummaries={getSummaryForMonth(
              childData,
              m.year,
              m.monthNumber
            )}
            monthIndex={index}
            fetchPrevious={fetchPreviousMonths}
            scrollToDate={scrollToDate.current}
            loading={loading}
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
        {showDiscussionAction
          ? i18n.calendar.newReservationOrAbsenceOrDiscussion
          : i18n.calendar.newReservationOrAbsence}
      </HoverButton>
    </>
  )
})

const HoverButton = styled(LegacyButton)`
  position: fixed;
  bottom: calc(${defaultMargins.s} + ${mobileBottomNavHeight}px);
  right: ${defaultMargins.s};
  border-radius: 40px;
  z-index: 2;

  @keyframes pulse {
    0% {
      transform: scale(1);
    }
    50% {
      transform: scale(1.1);
    }
    100% {
      transform: scale(1);
    }
  }
  animation: pulse 0.35s 2;
  animation-delay: 1.5s;
`

const Icon = styled(FontAwesomeIcon)`
  margin-right: ${defaultMargins.xs};
`
