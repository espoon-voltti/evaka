// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import LocalDate from 'lib-common/local-date'
import { useTranslation } from '../localization'
import styled from 'styled-components'
import colors from 'lib-customizations/common'

const HistoryOverlay = styled.div`
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 99;
  opacity: 0.3;
  background-color: ${colors.blues.lighter};
`

const DayColumn = styled(FixedSpaceColumn)`
  width: 48px;
  color: ${colors.blues.dark};
  font-weight: 600;
  font-size: 16px;
`

const DayDiv = styled(FixedSpaceRow)<{ today: boolean }>`
  position: relative;
  padding: 8px 16px;
  height: 80px;
  border-bottom: 1px solid ${colors.greyscale.lighter};
  ${(p) =>
    p.today
      ? `
    border-left: 6px solid ${colors.brandEspoo.espooTurquoise};
    padding-left: 10px;
  `
      : ''}
`

interface DayProps {
  date: LocalDate
}

const DayElem = React.memo(function DayElem({ date }: DayProps) {
  const i18n = useTranslation()

  return (
    <DayDiv alignItems="center" today={date.isToday()}>
      <DayColumn spacing="xxs">
        <div>
          {i18n.common.datetime.weekdaysShort[date.getIsoDayOfWeek() - 1]}
        </div>
        <div>{date.format('dd.MM.')}</div>
      </DayColumn>
      <div>09:00 - 15:30</div>
      {date.isBefore(LocalDate.today()) && <HistoryOverlay />}
    </DayDiv>
  )
})

const WeekDiv = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 16px 0 8px;
  background-color: ${colors.blues.lighter};
  color: ${colors.blues.dark};
  font-weight: 600;
  font-size: 14px;
  border-bottom: 1px solid ${colors.greyscale.lighter};
`

interface WeekProps {
  startDate: LocalDate
}

const WeekElem = React.memo(function WeekElem({ startDate }: WeekProps) {
  const dates = [...Array(5).keys()].map((n) => startDate.addDays(n))

  return (
    <div>
      <WeekDiv>Vk {startDate.getIsoWeek()}</WeekDiv>
      <div>
        {dates.map((d) => (
          <DayElem date={d} key={d.formatIso()} />
        ))}
      </div>
    </div>
  )
})

export default React.memo(function CalendarListView() {
  const currentWeekStartDate = LocalDate.today().startOfWeek()

  const dates = [...Array(5).keys()].map((n) =>
    currentWeekStartDate.addWeeks(n)
  )

  return (
    <FixedSpaceColumn spacing={'zero'}>
      {dates.map((d) => (
        <WeekElem startDate={d} key={d.formatIso()} />
      ))}
    </FixedSpaceColumn>
  )
})
