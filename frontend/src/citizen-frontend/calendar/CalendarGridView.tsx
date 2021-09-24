// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useMemo } from 'react'
import LocalDate from 'lib-common/local-date'
import styled, { css } from 'styled-components'
import { useTranslation } from '../localization'
import colors from 'lib-customizations/common'
import { fontWeights, H1, H2 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import Container, { ContentArea } from 'lib-components/layout/Container'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { faCalendarPlus, faUserMinus } from 'lib-icons'
import { DailyReservationData } from './api'
import { Reservations } from './calendar-elements'
import { asWeeklyData, WeeklyData } from './CalendarListView'
import { headerHeightDesktop } from 'citizen-frontend/header/const'

export interface Props {
  dailyData: DailyReservationData[]
  onCreateReservationClicked: () => void
  onCreateAbsencesClicked: () => void
  selectDate: (date: LocalDate) => void
}

export default React.memo(function CalendarGridView({
  dailyData,
  onCreateReservationClicked,
  onCreateAbsencesClicked,
  selectDate
}: Props) {
  const i18n = useTranslation()
  const monthlyData = useMemo(() => asMonthlyData(dailyData), [dailyData])

  return (
    <>
      <StickyHeader>
        <Container>
          <PageHeaderRow>
            <H1 noMargin>{i18n.calendar.title}</H1>
            <div>
              <InlineButton
                onClick={onCreateAbsencesClicked}
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
            <CalendarHeader>
              <HeadingCell />
              {[0, 1, 2, 3, 4].map((d) => (
                <HeadingCell key={d}>
                  {i18n.common.datetime.weekdaysShort[d]}
                </HeadingCell>
              ))}
            </CalendarHeader>
            <Grid>
              {weeks.map((w) => (
                <Fragment key={`${w.weekNumber}${month}${year}`}>
                  <WeekNumber>{w.weekNumber}</WeekNumber>
                  {w.dailyReservations.map((d) => (
                    <DayCell
                      key={`${d.date.formatIso()}${month}${year}`}
                      today={d.date.isToday() && d.date.month === month}
                      onClick={() => selectDate(d.date)}
                      data-qa={`desktop-calendar-day-${d.date.formatIso()}`}
                    >
                      <DayCellHeader>
                        <DayCellDate holiday={d.isHoliday}>
                          {d.date.format('d.M.')}
                        </DayCellDate>
                      </DayCellHeader>
                      <DayCellReservations data-qa="reservations">
                        <Reservations data={d} />
                      </DayCellReservations>
                      {d.date.month !== month ? <FadeOverlay /> : null}
                    </DayCell>
                  ))}
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

const StickyHeader = styled.div`
  position: sticky;
  top: ${headerHeightDesktop};
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

const gridPattern = css`
  display: grid;
  grid-template-columns: 28px repeat(5, 1fr);
`

const CalendarHeader = styled.div`
  ${gridPattern}
`

const Grid = styled.div`
  ${gridPattern}
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

const DayCell = styled.div<{ today: boolean }>`
  position: relative;
  min-height: 150px;
  padding: ${defaultMargins.s};
  background: ${({ theme }) => theme.colors.greyscale.white};
  border-bottom: 1px solid ${colors.greyscale.lighter};
  border-right: 1px solid ${colors.greyscale.lighter};
  ${(p) =>
    p.today
      ? `
    border-left: 4px solid ${colors.brandEspoo.espooTurquoise};
    padding-left: calc(${defaultMargins.s} - 3px);
  `
      : `
    /* left border for second cell (first day cell) of each row */
    &:nth-child(6n+2) {
      border-left: 1px solid ${colors.greyscale.lighter};
    }
      `}

  /* top border for every day cell of first row */
  &:nth-child(-n + 7) {
    border-top: 1px solid ${colors.greyscale.lighter};
  }
`

const DayCellHeader = styled.div`
  display: flex;
  justify-content: space-between;
  margin-bottom: ${defaultMargins.m};
`

const DayCellDate = styled.div<{ holiday: boolean }>`
  font-family: Montserrat, sans-serif;
  font-style: normal;
  color: ${(p) => (p.holiday ? colors.greyscale.dark : colors.blues.dark)};
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
