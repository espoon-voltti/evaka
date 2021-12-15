// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useCallback, useEffect, useMemo, useRef } from 'react'
import LocalDate from 'lib-common/local-date'
import styled, { css } from 'styled-components'
import { useTranslation } from '../localization'
import colors from 'lib-customizations/common'
import { fontWeights, H1, H2 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import Container, { ContentArea } from 'lib-components/layout/Container'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { faCalendarPlus, faUserMinus } from 'lib-icons'
import { DailyReservationData } from 'lib-common/generated/api-types/reservations'
import { Reservations } from './calendar-elements'
import { asWeeklyData, WeeklyData } from './CalendarListView'
import { headerHeightDesktop } from 'citizen-frontend/header/const'
import { scrollToPos } from 'lib-common/utils/scrolling'

export interface Props {
  dailyData: DailyReservationData[]
  onCreateReservationClicked: () => void
  onCreateAbsencesClicked: (initialDate: LocalDate | undefined) => void
  selectedDate: LocalDate | undefined
  selectDate: (date: LocalDate) => void
  includeWeekends: boolean
  dayIsReservable: (dailyData: DailyReservationData) => boolean
}

export default React.memo(function CalendarGridView({
  dailyData,
  onCreateReservationClicked,
  onCreateAbsencesClicked,
  selectedDate,
  selectDate,
  includeWeekends,
  dayIsReservable
}: Props) {
  const i18n = useTranslation()
  const monthlyData = useMemo(() => asMonthlyData(dailyData), [dailyData])
  const headerRef = useRef<HTMLDivElement>(null)
  const todayRef = useRef<HTMLDivElement>()

  useEffect(() => {
    const pos = todayRef.current?.getBoundingClientRect().top

    if (pos) {
      const offset =
        headerHeightDesktop + (headerRef.current?.clientHeight ?? 0) + 16

      scrollToPos({ left: 0, top: pos - offset })
    }
  }, [])

  const onCreateAbsences = useCallback(
    () => onCreateAbsencesClicked(undefined),
    [onCreateAbsencesClicked]
  )

  return (
    <>
      <StickyHeader ref={headerRef}>
        <Container>
          <PageHeaderRow>
            <H1 noMargin>{i18n.calendar.title}</H1>
            <div>
              <InlineButton
                onClick={onCreateAbsences}
                text={i18n.calendar.newAbsence}
                icon={faUserMinus}
                data-qa="open-absences-modal"
              />
              <Gap size="L" horizontal />
              <InlineButton
                onClick={onCreateReservationClicked}
                text={i18n.calendar.newReservationBtn}
                icon={faCalendarPlus}
                data-qa="open-reservations-modal"
              />
            </div>
          </PageHeaderRow>
        </Container>
      </StickyHeader>
      <Container>
        {monthlyData.map(({ month, year, weeks }) => (
          <ContentArea opaque={false} key={`${month}${year}`}>
            <MonthTitle>{`${
              i18n.common.datetime.months[month - 1]
            } ${year}`}</MonthTitle>
            <CalendarHeader includeWeekends={includeWeekends}>
              <HeadingCell />
              {(includeWeekends ? daysWithWeekends : daysWithoutWeekends).map(
                (d) => (
                  <HeadingCell key={d}>
                    {i18n.common.datetime.weekdaysShort[d]}
                  </HeadingCell>
                )
              )}
            </CalendarHeader>
            <Grid includeWeekends={includeWeekends}>
              {weeks.map((w) => (
                <Fragment key={`${w.weekNumber}${month}${year}`}>
                  <WeekNumber>{w.weekNumber}</WeekNumber>
                  {w.dailyReservations.map((d) => {
                    const dateIsOnMonth = d.date.month === month
                    const isToday = d.date.isToday() && dateIsOnMonth

                    return (
                      <DayCell
                        key={`${d.date.formatIso()}${month}${year}`}
                        ref={(e) => {
                          if (isToday) {
                            todayRef.current = e ?? undefined
                          }
                        }}
                        today={isToday}
                        selected={
                          dateIsOnMonth &&
                          d.date.formatIso() === selectedDate?.formatIso()
                        }
                        onClick={() => selectDate(d.date)}
                        data-qa={
                          dateIsOnMonth
                            ? `desktop-calendar-day-${d.date.formatIso()}`
                            : undefined
                        }
                      >
                        <DayCellHeader>
                          <DayCellDate inactive={!dayIsReservable(d)}>
                            {d.date.format('d.M.')}
                          </DayCellDate>
                        </DayCellHeader>
                        <DayCellReservations data-qa="reservations">
                          <Reservations data={d} />
                        </DayCellReservations>
                        {!dateIsOnMonth ? <FadeOverlay /> : null}
                      </DayCell>
                    )
                  })}
                </Fragment>
              ))}
            </Grid>
          </ContentArea>
        ))}
      </Container>
    </>
  )
})

interface MonthlyData {
  month: number
  year: number
  weeks: WeeklyData[]
}

const asMonthlyData = (dailyData: DailyReservationData[]): MonthlyData[] => {
  const getWeekMonths = (weeklyData: WeeklyData) => {
    const firstDay = weeklyData.dailyReservations[0].date
    const lastDay =
      weeklyData.dailyReservations[weeklyData.dailyReservations.length - 1].date

    return firstDay.month === lastDay.month
      ? [[firstDay.month, firstDay.year]]
      : [
          [firstDay.month, firstDay.year],
          [lastDay.month, lastDay.year]
        ]
  }

  return asWeeklyData(dailyData).reduce<MonthlyData[]>(
    (monthlyData, weeklyData) => {
      const weekMonths = getWeekMonths(weeklyData).map(([month, year]) => ({
        month,
        year,
        weeks: [weeklyData]
      }))

      if (monthlyData.length === 0) {
        // The first week in the data can be the last and first week of a month.
        // In that case we don't want to include the incomplete month.
        const firstWeekOfTheMonth = weekMonths[weekMonths.length - 1]

        // Drop the week altogether if it does not actually include the first
        // days of the month. This can happen because the first day of the month
        // can be eg. a sunday, which might not be shown on the calendar.
        if (
          firstWeekOfTheMonth.weeks[0].dailyReservations.some(
            ({ date }) => date.date <= 3
          )
        ) {
          return [firstWeekOfTheMonth]
        }

        return []
      }

      const lastMonth = monthlyData[monthlyData.length - 1]
      const monthsBeforeLast = monthlyData.slice(0, monthlyData.length - 1)

      if (lastMonth.month === weekMonths[0].month) {
        return [
          ...monthsBeforeLast,
          {
            ...lastMonth,
            weeks: [...lastMonth.weeks, weeklyData]
          },
          ...(weekMonths[1] ? [weekMonths[1]] : [])
        ]
      }

      return [...monthsBeforeLast, lastMonth, ...weekMonths]
    },
    []
  )
}

const daysWithoutWeekends = [0, 1, 2, 3, 4]
const daysWithWeekends = [0, 1, 2, 3, 4, 5, 6]

const StickyHeader = styled.div`
  position: sticky;
  top: ${headerHeightDesktop}px;
  z-index: 2;
  width: 100%;
  background: ${({ theme }) => theme.colors.greyscale.white};
  box-shadow: 0px 4px 8px 2px #0000000a;
`

const PageHeaderRow = styled(ContentArea).attrs({ opaque: false })`
  display: flex;
  justify-content: space-between;
  align-items: center;
`

const gridPattern = (includeWeekends: boolean) => css`
  display: grid;
  grid-template-columns: 28px repeat(${includeWeekends ? 7 : 5}, 1fr);
`

const CalendarHeader = styled.div<{ includeWeekends: boolean }>`
  ${({ includeWeekends }) => gridPattern(includeWeekends)}
`

const Grid = styled.div<{ includeWeekends: boolean }>`
  ${({ includeWeekends }) => gridPattern(includeWeekends)}
`

const HeadingCell = styled.div`
  color: ${colors.blues.dark};
  font-family: 'Open Sans', sans-serif;
  font-style: normal;
  padding: ${defaultMargins.xxs} ${defaultMargins.s};
`

const WeekNumber = styled(HeadingCell)`
  padding: ${defaultMargins.s} ${defaultMargins.xs} 0 0;
  text-align: right;
`

const MonthTitle = styled(H2).attrs({ noMargin: true })`
  color: ${({ theme }) => theme.colors.main.dark};
`

const DayCell = styled.div<{ today: boolean; selected: boolean }>`
  position: relative;
  min-height: 150px;
  padding: ${defaultMargins.s};
  background: ${({ theme }) => theme.colors.greyscale.white};
  outline: 1px solid ${colors.greyscale.lighter};
  cursor: pointer;
  user-select: none;

  ${(p) =>
    p.today
      ? css`
          border-left: 4px solid ${colors.brandEspoo.espooTurquoise};
          padding-left: calc(${defaultMargins.s} - 3px);
        `
      : ''}

  ${({ selected }) =>
    selected
      ? css`
          box-shadow: 0px 2px 3px 2px #00000030;
          z-index: 1;
          /* higher z-index causes right and bottom borders to shift when using outline */
          margin-left: -1px;
          margin-top: -1px;
        `
      : ''}
`

const DayCellHeader = styled.div`
  display: flex;
  justify-content: space-between;
  margin-bottom: ${defaultMargins.m};
`

const DayCellDate = styled.div<{ inactive: boolean }>`
  font-family: Montserrat, sans-serif;
  font-style: normal;
  color: ${(p) => (p.inactive ? colors.greyscale.dark : colors.blues.dark)};
  font-weight: ${fontWeights.semibold};
  font-size: 1.25rem;
`

const DayCellReservations = styled.div``

const FadeOverlay = styled.div`
  position: absolute;
  top: 1px;
  left: 1px;
  width: calc(100% - 2px);
  height: calc(100% - 2px);
  z-index: 1;
  opacity: 0.8;
  background-color: ${colors.greyscale.white};
`
