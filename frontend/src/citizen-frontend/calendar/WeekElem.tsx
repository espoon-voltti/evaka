// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useRef } from 'react'
import styled from 'styled-components'

import { headerHeightMobile } from 'citizen-frontend/header/const'
import { DailyReservationData } from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { scrollToPos } from 'lib-common/utils/scrolling'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { useLang, useTranslation } from '../localization'

import { WeeklyData } from './CalendarListView'
import { Reservations } from './calendar-elements'

interface Props extends WeeklyData {
  selectDate: (date: LocalDate) => void
  dayIsReservable: (dailyData: DailyReservationData) => boolean
}

export default React.memo(function WeekElem({
  weekNumber,
  dailyReservations,
  selectDate,
  dayIsReservable
}: Props) {
  const i18n = useTranslation()
  return (
    <div>
      <WeekDiv>
        {i18n.common.datetime.week} {weekNumber}
      </WeekDiv>
      <div>
        {dailyReservations.map((d) => (
          <DayElem
            dailyReservations={d}
            key={d.date.formatIso()}
            selectDate={selectDate}
            dayIsReservable={dayIsReservable}
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
  background-color: ${colors.main.m4};
  color: ${colors.main.m1};
  font-weight: ${fontWeights.semibold};
  font-size: 0.875rem;
  border-bottom: 1px solid ${colors.grayscale.g15};
`

interface DayProps {
  dailyReservations: DailyReservationData
  selectDate: (date: LocalDate) => void
  dayIsReservable: (dailyData: DailyReservationData) => boolean
}

const DayElem = React.memo(function DayElem({
  dailyReservations,
  selectDate,
  dayIsReservable
}: DayProps) {
  const [lang] = useLang()
  const ref = useRef<HTMLDivElement>()

  useEffect(() => {
    if (ref.current) {
      const pos = ref.current?.getBoundingClientRect().top

      if (pos) {
        const offset = headerHeightMobile + 16
        scrollToPos({ left: 0, top: pos - offset })
      }
    }
  }, [])

  return (
    <DayDiv
      ref={(e) => {
        if (dailyReservations.date.isToday()) {
          ref.current = e ?? undefined
        }
      }}
      alignItems="center"
      today={dailyReservations.date.isToday()}
      onClick={() => selectDate(dailyReservations.date)}
      data-qa={`mobile-calendar-day-${dailyReservations.date.formatIso()}`}
    >
      <DayColumn spacing="xxs" inactive={!dayIsReservable(dailyReservations)}>
        <div>{dailyReservations.date.format('EEEEEE', lang)}</div>
        <div>{dailyReservations.date.format('d.M.')}</div>
      </DayColumn>
      <div data-qa="reservations">
        <Reservations data={dailyReservations} />
      </div>
      {dailyReservations.date.isBefore(LocalDate.today()) && <HistoryOverlay />}
    </DayDiv>
  )
})

const DayDiv = styled(FixedSpaceRow)<{ today: boolean }>`
  position: relative;
  padding: ${defaultMargins.s} ${defaultMargins.s};
  border-bottom: 1px solid ${colors.grayscale.g15};
  border-left: 6px solid
    ${(p) => (p.today ? colors.status.success : 'transparent')};
  cursor: pointer;
`

const DayColumn = styled(FixedSpaceColumn)<{ inactive: boolean }>`
  width: 3rem;
  color: ${(p) => (p.inactive ? colors.grayscale.g70 : colors.main.m1)};
  font-weight: ${fontWeights.semibold};
`

const HistoryOverlay = styled.div`
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0.3;
  background-color: ${colors.main.m4};
`
