// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useEffect, useRef } from 'react'
import styled, { css } from 'styled-components'

import { DailyReservationData } from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { fontWeights, H2, H3 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
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
      <WeekTitle>
        {i18n.common.datetime.week} {weekNumber}
      </WeekTitle>
      <div>
        {dailyReservations.map((d) => (
          <Fragment key={d.date.formatIso()}>
            {d.date.date === 1 && (
              <MonthTitle>
                {i18n.common.datetime.months[d.date.month - 1]}
              </MonthTitle>
            )}
            <DayElem
              dailyReservations={d}
              key={d.date.formatIso()}
              selectDate={selectDate}
              isReservable={dayIsReservable(d)}
              isHolidayPeriod={dayIsHolidayPeriod(d.date)}
            />
          </Fragment>
        ))}
      </div>
    </div>
  )
})

const titleStyles = css`
  margin: 0;
  display: flex;
  justify-content: flex-start;
  align-items: center;
  padding: ${defaultMargins.s};
  background-color: ${(p) => p.theme.colors.main.m4};
  border-bottom: 1px solid ${colors.grayscale.g15};
  color: ${(p) => p.theme.colors.grayscale.g100};
  font-family: 'Open Sans', 'Arial', sans-serif;
  font-weight: ${fontWeights.semibold};
`

const WeekTitle = styled(H3)`
  font-size: 1em;
  ${titleStyles}
`

const MonthTitle = styled(H2)`
  font-size: 1.25em;
  ${titleStyles}
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
  const ref = useRef<HTMLButtonElement>()

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
    <Day
      ref={(e) => {
        if (dailyReservations.date.isToday()) {
          ref.current = e ?? undefined
        }
      }}
      today={dailyReservations.date.isToday()}
      holidayPeriod={isHolidayPeriod}
      onClick={() => selectDate(dailyReservations.date)}
      data-qa={`mobile-calendar-day-${dailyReservations.date.formatIso()}`}
    >
      <DayColumn spacing="xxs" inactive={!isReservable}>
        <div aria-label={dailyReservations.date.formatExotic('EEEE', lang)}>
          {dailyReservations.date.format('EEEEEE', lang)}
        </div>
        <div aria-label={dailyReservations.date.formatExotic('do MMMM', lang)}>
          {dailyReservations.date.format('d.M.')}
        </div>
      </DayColumn>
      <Gap size="s" horizontal />
      <div data-qa="reservations">
        <Reservations data={dailyReservations} />
      </div>
      <Gap size="s" horizontal />
      {dailyReservations.date.isBefore(LocalDate.today()) && <HistoryOverlay />}
    </Day>
  )
})

const Day = styled.button<{
  today: boolean
  holidayPeriod: boolean
}>`
  display: flex;
  flex-direction: row;
  align-items: center;
  width: 100%;
  position: relative;
  padding: ${defaultMargins.s} ${defaultMargins.s};
  background: transparent;
  margin: 0;
  border: none;
  border-bottom: 1px solid ${colors.grayscale.g15};
  border-left: 6px solid
    ${(p) => (p.today ? colors.status.success : 'transparent')};
  cursor: pointer;
  ${(p) =>
    p.holidayPeriod &&
    css`
      background-color: ${colors.accents.a10powder};
    `}

  :focus {
    outline: 2px solid ${(p) => p.theme.colors.main.m2Focus};
  }
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
  opacity: 0.6;
  background-color: ${(p) => p.theme.colors.grayscale.g0};
`
