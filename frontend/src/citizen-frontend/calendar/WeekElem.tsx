// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useCallback, useEffect, useMemo, useRef } from 'react'
import styled, { css } from 'styled-components'

import {
  DailyReservationData,
  ReservationChild
} from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { capitalizeFirstLetter } from 'lib-common/string'
import { scrollToPos } from 'lib-common/utils/scrolling'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { fontWeights, H2, H3 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { useLang, useTranslation } from '../localization'

import { WeeklyData } from './CalendarListView'
import { HistoryOverlay } from './HistoryOverlay'
import { ChildImageData } from './RoundChildImages'
import { Reservations } from './calendar-elements'

interface Props extends WeeklyData {
  childData: ReservationChild[]
  selectDate: (date: LocalDate) => void
  dayIsReservable: (dailyData: DailyReservationData) => boolean
  dayIsHolidayPeriod: (date: LocalDate) => boolean
  childImages: ChildImageData[]
}

export default React.memo(function WeekElem({
  weekNumber,
  childData,
  dailyReservations,
  dayIsHolidayPeriod,
  selectDate,
  dayIsReservable,
  childImages
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
              childData={childData}
              dailyReservations={d}
              key={d.date.formatIso()}
              selectDate={selectDate}
              isReservable={dayIsReservable(d)}
              isHolidayPeriod={dayIsHolidayPeriod(d.date)}
              childImages={childImages}
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
  childData: ReservationChild[]
  dailyReservations: DailyReservationData
  selectDate: (date: LocalDate) => void
  isReservable: boolean
  isHolidayPeriod: boolean
  childImages: ChildImageData[]
}

const DayElem = React.memo(function DayElem({
  childData,
  dailyReservations,
  selectDate,
  isReservable,
  isHolidayPeriod,
  childImages
}: DayProps) {
  const [lang] = useLang()
  const ref = useRef<HTMLButtonElement>()

  const markedByEmployee = useMemo(
    () =>
      dailyReservations.children.length > 0 &&
      dailyReservations.children.every((c) => c.markedByEmployee),
    [dailyReservations]
  )

  const isToday = dailyReservations.date.isToday()
  const setRef = useCallback(
    (e: HTMLButtonElement) => {
      if (isToday) {
        ref.current = e ?? undefined
      }
    },
    [isToday]
  )

  const handleClick = useCallback(() => {
    selectDate(dailyReservations.date)
  }, [selectDate, dailyReservations.date])

  useEffect(() => {
    const top = ref.current?.getBoundingClientRect().top

    if (top) {
      scrollToPos({
        top: top - 32
      })
    }
  }, [])

  return (
    <Day
      ref={setRef}
      today={dailyReservations.date.isToday()}
      markedByEmployee={markedByEmployee}
      holidayPeriod={isHolidayPeriod}
      onClick={handleClick}
      data-qa={`mobile-calendar-day-${dailyReservations.date.formatIso()}`}
    >
      <DayColumn
        spacing="xxs"
        inactive={!isReservable}
        holiday={dailyReservations.isHoliday}
      >
        <div aria-label={dailyReservations.date.formatExotic('EEEE', lang)}>
          {capitalizeFirstLetter(dailyReservations.date.format('EEEEEE', lang))}
        </div>
        <div aria-label={dailyReservations.date.formatExotic('do MMMM', lang)}>
          {dailyReservations.date.format('d.M.')}
        </div>
      </DayColumn>
      <Gap size="s" horizontal />
      <ReservationsContainer data-qa="reservations">
        <Reservations
          data={dailyReservations}
          allChildren={childData}
          childImages={childImages}
        />
      </ReservationsContainer>
      {dailyReservations.date.isBefore(LocalDate.todayInSystemTz()) && (
        <HistoryOverlay />
      )}
    </Day>
  )
})

const ReservationsContainer = styled.div`
  flex: 1 0 0;
`

const Day = styled.button<{
  today: boolean
  markedByEmployee: boolean
  holidayPeriod: boolean
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
    ${(p) => (p.today ? colors.status.success : 'transparent')};
  cursor: pointer;
  text-align: left;
  color: ${(p) => p.theme.colors.grayscale.g100};

  ${(p) =>
    p.markedByEmployee
      ? `background-color: ${colors.grayscale.g15}`
      : p.holidayPeriod
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
