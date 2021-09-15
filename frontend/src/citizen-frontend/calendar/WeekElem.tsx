// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import LocalDate from 'lib-common/local-date'
import colors from 'lib-customizations/common'
import { useTranslation } from '../localization'
import { defaultMargins } from 'lib-components/white-space'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'

export interface WeekProps {
  weekNumber: number
  dailyReservations: Array<{
    date: LocalDate
    isHoliday: boolean
    reservations: string[]
  }>
}

interface Props extends WeekProps {
  selectDate: (date: LocalDate) => void
}

export default React.memo(function WeekElem({
  weekNumber,
  dailyReservations,
  selectDate
}: Props) {
  const i18n = useTranslation()
  return (
    <div>
      <WeekDiv>
        {i18n.common.datetime.weekShort} {weekNumber}
      </WeekDiv>
      <div>
        {dailyReservations.map((d) => (
          <DayElem
            dailyReservations={d}
            key={d.date.formatIso()}
            selectDate={selectDate}
          />
        ))}
      </div>
    </div>
  )
})

const WeekDiv = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  padding: ${defaultMargins.s} 0 ${defaultMargins.xs};
  background-color: ${colors.brandEspoo.espooTurquoiseLight};
  color: ${colors.blues.dark};
  font-weight: 600;
  font-size: 0.875rem;
  border-bottom: 1px solid ${colors.greyscale.lighter};
`

interface DayProps {
  dailyReservations: {
    date: LocalDate
    isHoliday: boolean
    reservations: string[]
  }
  selectDate: (date: LocalDate) => void
}

const DayElem = React.memo(function DayElem({
  dailyReservations: { date, isHoliday, reservations },
  selectDate
}: DayProps) {
  const i18n = useTranslation()

  return (
    <DayDiv
      alignItems="center"
      today={date.isToday()}
      onClick={() => selectDate(date)}
      data-qa={`mobile-calendar-day-${date.formatIso()}`}
    >
      <DayColumn spacing="xxs" holiday={isHoliday}>
        <div>
          {i18n.common.datetime.weekdaysShort[date.getIsoDayOfWeek() - 1]}
        </div>
        <div>{date.format('dd.MM.')}</div>
      </DayColumn>
      <div data-qa="reservations">
        {reservations.length === 0 && isHoliday && (
          <HolidayNote>{i18n.calendar.holiday}</HolidayNote>
        )}
        {reservations.join(', ')}
      </div>
      {date.isBefore(LocalDate.today()) && <HistoryOverlay />}
    </DayDiv>
  )
})

const DayDiv = styled(FixedSpaceRow)<{ today: boolean }>`
  position: relative;
  padding: ${defaultMargins.s} ${defaultMargins.s};
  border-bottom: 1px solid ${colors.greyscale.lighter};
  border-left: 6px solid
    ${(p) => (p.today ? colors.brandEspoo.espooTurquoise : 'transparent')};
  cursor: pointer;
`

const DayColumn = styled(FixedSpaceColumn)<{ holiday: boolean }>`
  width: 3rem;
  color: ${(p) => (p.holiday ? colors.greyscale.dark : colors.blues.dark)};
  font-weight: 600;
`

const HolidayNote = styled.div`
  font-style: italic;
  color: ${colors.greyscale.dark};
`

const HistoryOverlay = styled.div`
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  opacity: 0.3;
  background-color: ${colors.blues.lighter};
`
