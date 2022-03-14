// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useRef } from 'react'
import styled, { css } from 'styled-components'

import { DailyReservationData } from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { bannerApproxHeightMobile, headerHeightMobile } from '../header/const'
import { useLang, useTranslation } from '../localization'
import { scrollMainToPos } from '../utils'

import { WeeklyData } from './CalendarListView'
import { Reservations } from './calendar-elements'

interface Props extends WeeklyData {
  selectDate: (date: LocalDate) => void
  dayIsReservable: (dailyData: DailyReservationData) => boolean
  dayIsHolidayPeriod: (date: LocalDate) => boolean
}

export default React.memo(function WeekElem({
  weekNumber,
  dailyReservations,
  dayIsHolidayPeriod,
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
            isReservable={dayIsReservable(d)}
            isHolidayPeriod={dayIsHolidayPeriod(d.date)}
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
  isReservable: boolean
  isHolidayPeriod: boolean
}

const DayElem = React.memo(function DayElem({
  dailyReservations,
  selectDate,
  isReservable,
  isHolidayPeriod
}: DayProps) {
  const [lang] = useLang()
  const ref = useRef<HTMLDivElement>()

  useEffect(() => {
    if (ref.current) {
      const pos = ref.current?.getBoundingClientRect().top

      if (pos) {
        const offset = bannerApproxHeightMobile + headerHeightMobile + 16
        scrollMainToPos({
          left: 0,
          top: pos - offset
        })
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
      holidayPeriod={isHolidayPeriod}
      onClick={() => selectDate(dailyReservations.date)}
      data-qa={`mobile-calendar-day-${dailyReservations.date.formatIso()}`}
    >
      <DayColumn spacing="xxs" inactive={!isReservable}>
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

const DayDiv = styled(FixedSpaceRow)<{
  today: boolean
  holidayPeriod: boolean
}>`
  position: relative;
  padding: ${defaultMargins.s} ${defaultMargins.s};
  border-bottom: 1px solid ${colors.grayscale.g15};
  border-left: 6px solid
    ${(p) => (p.today ? colors.status.success : 'transparent')};
  cursor: pointer;
  ${(p) =>
    p.holidayPeriod &&
    css`
      background-color: ${colors.accents.a10powder};
    `}
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
