// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useCallback, useEffect, useMemo, useRef } from 'react'
import styled, { css } from 'styled-components'

import FiniteDateRange from 'lib-common/finite-date-range'
import { DailyReservationData } from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { fontWeights, H1, H2 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faCalendarPlus, faTreePalm, faUserMinus } from 'lib-icons'

import { bannerHeightDesktop, headerHeightDesktop } from '../header/const'
import { useHolidayPeriods } from '../holiday-periods/state'
import { useLang, useTranslation } from '../localization'
import { scrollMainToPos } from '../utils'

import { asWeeklyData, WeeklyData } from './CalendarListView'
import ReportHolidayLabel from './ReportHolidayLabel'
import { Reservations } from './calendar-elements'

export interface Props {
  dailyData: DailyReservationData[]
  onCreateReservationClicked: () => void
  onCreateAbsencesClicked: (initialDate: LocalDate | undefined) => void
  onReportHolidaysClicked: () => void
  selectedDate: LocalDate | undefined
  selectDate: (date: LocalDate) => void
  includeWeekends: boolean
  dayIsReservable: (dailyData: DailyReservationData) => boolean
}

export default React.memo(function CalendarGridView({
  dailyData,
  onCreateReservationClicked,
  onCreateAbsencesClicked,
  onReportHolidaysClicked,
  selectedDate,
  selectDate,
  includeWeekends,
  dayIsReservable
}: Props) {
  const i18n = useTranslation()
  const [lang] = useLang()
  const monthlyData = useMemo(() => asMonthlyData(dailyData), [dailyData])
  const headerRef = useRef<HTMLDivElement>(null)
  const todayRef = useRef<HTMLButtonElement>()

  useEffect(() => {
    const pos = todayRef.current?.getBoundingClientRect().top

    if (pos) {
      const offset =
        headerHeightDesktop +
        bannerHeightDesktop +
        (headerRef.current?.clientHeight ?? 0) +
        16

      scrollMainToPos({
        left: 0,
        top: pos - offset
      })
    }
  }, [])

  const onCreateAbsences = useCallback(
    () => onCreateAbsencesClicked(undefined),
    [onCreateAbsencesClicked]
  )

  const {
    holidayPeriods: holidayPeriodResult,
    questionnaireAvailable,
    holidayBanner
  } = useHolidayPeriods()
  const holidayPeriods = useMemo<FiniteDateRange[]>(
    () => holidayPeriodResult.map((p) => p.map((i) => i.period)).getOrElse([]),
    [holidayPeriodResult]
  )
  const showBanner = useMemo(
    () => holidayBanner.map<boolean>((b) => b.type !== 'none').getOrElse(false),
    [holidayBanner]
  )

  return (
    <>
      <StickyHeader ref={headerRef} bannerIsVisible={showBanner}>
        <Container>
          <PageHeaderRow>
            <H1 noMargin>{i18n.calendar.title}</H1>
            <ButtonContainer>
              {questionnaireAvailable && (
                <InlineButton
                  onClick={onReportHolidaysClicked}
                  text={<ReportHolidayLabel iconRight />}
                  icon={faTreePalm}
                  data-qa="open-holiday-modal"
                />
              )}
              <InlineButton
                onClick={onCreateAbsences}
                text={i18n.calendar.newAbsence}
                icon={faUserMinus}
                data-qa="open-absences-modal"
              />
              <InlineButton
                onClick={onCreateReservationClicked}
                text={i18n.calendar.newReservationBtn}
                icon={faCalendarPlus}
                data-qa="open-reservations-modal"
              />
            </ButtonContainer>
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

                    return dateIsOnMonth ? (
                      <DayCell
                        key={`${d.date.formatIso()}${month}${year}`}
                        ref={(e) => {
                          if (isToday) {
                            todayRef.current = e ?? undefined
                          }
                        }}
                        today={isToday}
                        holidayPeriod={holidayPeriods.some((p) =>
                          p.includes(d.date)
                        )}
                        selected={
                          d.date.formatIso() === selectedDate?.formatIso()
                        }
                        onClick={() => selectDate(d.date)}
                        data-qa={`desktop-calendar-day-${d.date.formatIso()}`}
                      >
                        <DayCellHeader>
                          <DayCellDate
                            inactive={!dayIsReservable(d)}
                            aria-label={d.date.formatExotic(
                              'EEEE do MMMM',
                              lang
                            )}
                          >
                            {d.date.format('d.M.')}
                          </DayCellDate>
                        </DayCellHeader>
                        <DayCellReservations data-qa="reservations">
                          <Reservations data={d} />
                        </DayCellReservations>
                      </DayCell>
                    ) : (
                      <InactiveCell />
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

const StickyHeader = styled.div<{ bannerIsVisible: boolean }>`
  position: sticky;
  top: ${(p) => (p.bannerIsVisible ? bannerHeightDesktop : 0)}px;
  z-index: 2;
  width: 100%;
  background: ${(p) => p.theme.colors.grayscale.g0};
  box-shadow: 0 4px 8px 2px #0000000a;
`

const PageHeaderRow = styled(ContentArea).attrs({ opaque: false })`
  display: flex;
  justify-content: space-between;
  align-items: center;
`

const ButtonContainer = styled.div`
  display: flex;
  gap: ${defaultMargins.L};
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
  > * {
    margin-top: 0;
    margin-left: 0;
    margin-bottom: 1px;
    margin-right: 1px;
  }
`

const HeadingCell = styled.div`
  color: ${colors.main.m1};
  font-family: 'Open Sans', sans-serif;
  font-style: normal;
  padding: ${defaultMargins.xxs} ${defaultMargins.s};
`

const WeekNumber = styled(HeadingCell)`
  padding: ${defaultMargins.s} ${defaultMargins.xs} 0 0;
  text-align: right;
`

const MonthTitle = styled(H2).attrs({ noMargin: true })`
  color: ${(p) => p.theme.colors.main.m1};
`

const DayCell = styled.button<{
  today: boolean
  selected: boolean
  holidayPeriod: boolean
}>`
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: flex-start;
  position: relative;
  min-height: 150px;
  padding: ${defaultMargins.s};
  background-color: ${(p) =>
    p.holidayPeriod
      ? p.theme.colors.accents.a10powder
      : p.theme.colors.grayscale.g0};
  border: none;
  outline: 1px solid ${colors.grayscale.g15};
  cursor: pointer;
  user-select: none;

  ${(p) =>
    p.today
      ? css`
          border-left: 4px solid ${colors.status.success};
          padding-left: calc(${defaultMargins.s} - 3px);
        `
      : ''}

  ${(p) =>
    p.selected
      ? css`
          box-shadow: 0 2px 3px 2px #00000030;
          z-index: 1;
        `
      : ''}

  :focus {
    box-shadow: 0 0 0 4px ${(p) => p.theme.colors.main.m2Focus};
    z-index: 1;
  }
`

const DayCellHeader = styled.div`
  display: flex;
  justify-content: space-between;
  margin-bottom: ${defaultMargins.m};
`

const DayCellDate = styled.div<{ inactive: boolean }>`
  font-family: Montserrat, sans-serif;
  font-style: normal;
  color: ${(p) => (p.inactive ? colors.grayscale.g70 : colors.main.m1)};
  font-weight: ${fontWeights.semibold};
  font-size: 1.25rem;
`

const DayCellReservations = styled.div``

const InactiveCell = styled.div`
  background-color: transparent;
`
