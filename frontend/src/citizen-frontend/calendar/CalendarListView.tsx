// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useMemo } from 'react'
import styled from 'styled-components'

import { CitizenCalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import {
  ReservationChild,
  ReservationResponseDay
} from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import Button from 'lib-components/atoms/buttons/Button'
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
  const months = useMemo(() => groupByMonth(calendarDays), [calendarDays])
  const childImages = useMemo(() => getChildImages(childData), [childData])

  return (
    <>
      <FixedSpaceColumn spacing="zero">
        {months.map((m, index) => (
          <MonthElem
            key={`month-${index}`}
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

const HoverButton = styled(Button)`
  position: fixed;
  bottom: calc(${defaultMargins.s} + ${mobileBottomNavHeight}px);
  right: ${defaultMargins.s};
  border-radius: 40px;
  z-index: 2;
`

const Icon = styled(FontAwesomeIcon)`
  margin-right: ${defaultMargins.xs};
`
