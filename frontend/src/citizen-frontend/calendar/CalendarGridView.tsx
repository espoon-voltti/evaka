// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import sum from 'lodash/sum'
import React, {
  MutableRefObject,
  useCallback,
  useEffect,
  useMemo,
  useRef
} from 'react'
import styled, { css, useTheme } from 'styled-components'

import FiniteDateRange from 'lib-common/finite-date-range'
import { CitizenCalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import {
  ReservationChild,
  ReservationResponseDay
} from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { scrollToPos } from 'lib-common/utils/scrolling'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { fontWeights, H2 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faCalendar, faCalendarPlus, faTreePalm, faUserMinus } from 'lib-icons'

import { useUser } from '../auth/state'
import { useLang, useTranslation } from '../localization'
import { headerHeightDesktop } from '../navigation/const'

import {
  CalendarEventCount,
  CalendarEventCountContainer
} from './CalendarEventCount'
import { CalendarWeek, groupByWeek } from './CalendarListView'
import { HistoryOverlay } from './HistoryOverlay'
import ReportHolidayLabel from './ReportHolidayLabel'
import { ChildImageData, getChildImages } from './RoundChildImages'
import { Reservations } from './calendar-elements'
import { activeQuestionnaireQuery, holidayPeriodsQuery } from './queries'
import { isQuestionnaireAvailable } from './utils'

export interface Props {
  childData: ReservationChild[]
  calendarDays: ReservationResponseDay[]
  onCreateReservationClicked: () => void
  onCreateAbsencesClicked: (initialDate: LocalDate | undefined) => void
  onReportHolidaysClicked: () => void
  selectedDate: LocalDate | undefined
  selectDate: (date: LocalDate) => void
  includeWeekends: boolean
  dayIsReservable: (date: LocalDate) => boolean
  events: CitizenCalendarEvent[]
}

export default React.memo(function CalendarGridView({
  childData,
  calendarDays,
  onCreateReservationClicked,
  onCreateAbsencesClicked,
  onReportHolidaysClicked,
  selectedDate,
  selectDate,
  includeWeekends,
  dayIsReservable,
  events
}: Props) {
  const i18n = useTranslation()
  const calendarMonths = useMemo(
    () => groupByMonth(calendarDays),
    [calendarDays]
  )
  const todayRef = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    const top = todayRef.current?.getBoundingClientRect().top

    if (top) {
      scrollToPos({
        top: top - headerHeightDesktop * 2 - 32
      })
    }
  }, [])

  const onCreateAbsences = useCallback(
    () => onCreateAbsencesClicked(undefined),
    [onCreateAbsencesClicked]
  )

  const holidayPeriodResult = useQueryResult(holidayPeriodsQuery())
  const holidayPeriods = useMemo<FiniteDateRange[]>(
    () => holidayPeriodResult.map((p) => p.map((i) => i.period)).getOrElse([]),
    [holidayPeriodResult]
  )

  const questionnaireAvailable = isQuestionnaireAvailable(
    useQueryResult(activeQuestionnaireQuery()),
    useUser()
  )

  const childImages = useMemo(() => getChildImages(childData), [childData])

  return (
    <>
      <StickyBottomBar>
        <ButtonContainer>
          {questionnaireAvailable && (
            <InlineButton
              onClick={onReportHolidaysClicked}
              text={
                <ReportHolidayLabel
                  questionnaireAvailable={questionnaireAvailable}
                  iconRight
                />
              }
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
      </StickyBottomBar>
      <Container>
        {calendarMonths.map(({ month, year, weeks }) => (
          <Month
            key={`${month}${year}`}
            year={year}
            month={month}
            weeks={weeks}
            holidayPeriods={holidayPeriods}
            todayRef={todayRef}
            selectedDate={selectedDate}
            selectDate={selectDate}
            includeWeekends={includeWeekends}
            dayIsReservable={dayIsReservable}
            childImages={childImages}
            events={events}
          />
        ))}
      </Container>
    </>
  )
})

interface MonthlyData {
  month: number
  year: number
  weeks: CalendarWeek[]
}

const groupByMonth = (dailyData: ReservationResponseDay[]): MonthlyData[] => {
  const getWeekMonths = (weeklyData: CalendarWeek) => {
    const firstDay = weeklyData.calendarDays[0].date
    const lastDay =
      weeklyData.calendarDays[weeklyData.calendarDays.length - 1].date

    return firstDay.month === lastDay.month
      ? [[firstDay.month, firstDay.year]]
      : [
          [firstDay.month, firstDay.year],
          [lastDay.month, lastDay.year]
        ]
  }

  return groupByWeek(dailyData).reduce<MonthlyData[]>(
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
          firstWeekOfTheMonth.weeks[0].calendarDays.some(
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

const Month = React.memo(function Month({
  year,
  month,
  weeks,
  holidayPeriods,
  todayRef,
  selectDate,
  selectedDate,
  includeWeekends,
  dayIsReservable,
  childImages,
  events
}: {
  year: number
  month: number
  weeks: CalendarWeek[]
  holidayPeriods: FiniteDateRange[]
  todayRef: MutableRefObject<HTMLButtonElement | null>
  selectedDate: LocalDate | undefined
  selectDate: (date: LocalDate) => void
  includeWeekends: boolean
  dayIsReservable: (date: LocalDate) => boolean
  childImages: ChildImageData[]
  events: CitizenCalendarEvent[]
}) {
  const i18n = useTranslation()
  return (
    <ContentArea opaque={false} key={`${month}${year}`}>
      <MonthTitle>{`${
        i18n.common.datetime.months[month - 1]
      } ${year}`}</MonthTitle>
      <CalendarHeader includeWeekends={includeWeekends}>
        <HeadingCell />
        {(includeWeekends ? daysWithWeekends : daysWithoutWeekends).map((d) => (
          <HeadingCell key={d}>
            {i18n.common.datetime.weekdaysShort[d]}
          </HeadingCell>
        ))}
      </CalendarHeader>
      <Grid includeWeekends={includeWeekends}>
        {weeks.map((w) => (
          <Week
            key={`${w.weekNumber}${month}${year}`}
            year={year}
            month={month}
            week={w}
            holidayPeriods={holidayPeriods}
            todayRef={todayRef}
            selectedDate={selectedDate}
            selectDate={selectDate}
            dayIsReservable={dayIsReservable}
            childImages={childImages}
            events={events}
          />
        ))}
      </Grid>
    </ContentArea>
  )
})

const Week = React.memo(function Week({
  year,
  month,
  week,
  holidayPeriods,
  todayRef,
  selectedDate,
  selectDate,
  dayIsReservable,
  childImages,
  events
}: {
  year: number
  month: number
  week: CalendarWeek
  holidayPeriods: FiniteDateRange[]
  todayRef: MutableRefObject<HTMLButtonElement | null>
  selectedDate: LocalDate | undefined
  selectDate: (date: LocalDate) => void
  dayIsReservable: (date: LocalDate) => boolean
  childImages: ChildImageData[]
  events: CitizenCalendarEvent[]
}) {
  return (
    <>
      <WeekNumber>{week.weekNumber}</WeekNumber>
      {week.calendarDays.map((d) => (
        <Day
          key={`${d.date.formatIso()}${month}${year}`}
          day={d}
          holidayPeriods={holidayPeriods}
          todayRef={todayRef}
          dateType={dateType(year, month, d.date)}
          selected={selectedDate !== undefined && d.date.isEqual(selectedDate)}
          selectDate={selectDate}
          dayIsReservable={dayIsReservable}
          childImages={childImages}
          events={events}
        />
      ))}
    </>
  )
})

type DateType = 'past' | 'today' | 'future' | 'otherMonth'

function dateType(year: number, month: number, date: LocalDate): DateType {
  if (date.year !== year || date.month !== month) return 'otherMonth'
  const today = LocalDate.todayInSystemTz()
  return date.isBefore(today) ? 'past' : date.isToday() ? 'today' : 'future'
}

const Day = React.memo(function Day({
  day,
  holidayPeriods,
  todayRef,
  dateType,
  selected,
  selectDate,
  dayIsReservable,
  childImages,
  events
}: {
  day: ReservationResponseDay
  holidayPeriods: FiniteDateRange[]
  todayRef: MutableRefObject<HTMLButtonElement | null>
  dateType: DateType
  selected: boolean
  selectDate: (date: LocalDate) => void
  dayIsReservable: (date: LocalDate) => boolean
  childImages: ChildImageData[]
  events: CitizenCalendarEvent[]
}) {
  const [lang] = useLang()
  const ref = useCallback(
    (e: HTMLButtonElement) => {
      if (dateType === 'today') {
        todayRef.current = e ?? undefined
      }
    },
    [dateType, todayRef]
  )
  const highlight = useMemo(
    () =>
      day.children.length > 0 &&
      day.children.every((c) => c.absence && !c.absence.editable)
        ? 'nonEditableAbsence'
        : holidayPeriods.some((p) => p.includes(day.date))
          ? 'holidayPeriod'
          : undefined,
    [day.children, day.date, holidayPeriods]
  )
  const onClick = useCallback(
    () => selectDate(day.date),
    [day.date, selectDate]
  )

  const eventCount = useMemo(
    () =>
      sum(
        events.map(
          ({ attendingChildren }) =>
            Object.values(attendingChildren).filter((attending) =>
              attending.some(({ periods }) =>
                periods.some((period) => period.includes(day.date))
              )
            ).length
        )
      ),
    [day.date, events]
  )

  const theme = useTheme()

  const i18n = useTranslation()

  if (dateType === 'otherMonth') {
    return <InactiveCell />
  }

  return (
    <DayCell
      ref={ref}
      $today={dateType === 'today'}
      $highlight={highlight}
      $selected={selected}
      onClick={onClick}
      data-qa={`desktop-calendar-day-${day.date.formatIso()}`}
    >
      <DayCellHeader>
        <DayCellDate
          inactive={!dayIsReservable(day.date)}
          holiday={day.holiday}
          aria-label={day.date.formatExotic('EEEE do MMMM', lang)}
        >
          {day.date.format('d.M.')}
        </DayCellDate>
        {eventCount > 0 && (
          <CalendarEventCountContainer
            aria-label={`${eventCount} ${i18n.calendar.eventsCount}`}
          >
            <FontAwesomeIcon color={theme.colors.main.m2} icon={faCalendar} />
            <CalendarEventCount data-qa="event-count">
              {eventCount}
            </CalendarEventCount>
          </CalendarEventCountContainer>
        )}
      </DayCellHeader>
      <div data-qa="reservations">
        <Reservations
          data={day}
          childImages={childImages}
          isReservable={dayIsReservable(day.date)}
          backgroundHighlight={highlight}
        />
      </div>
      {dateType === 'past' && <HistoryOverlay />}
    </DayCell>
  )
})

const StickyBottomBar = styled.div`
  position: fixed;
  bottom: 0;
  z-index: 2;
  width: 100%;
  height: 80px;
  background: ${(p) => p.theme.colors.grayscale.g0};
  box-shadow: 0 -4px 8px 2px #0000000a;
`

const ButtonContainer = styled(Container)`
  height: 100%;
  display: flex;
  justify-content: flex-end;
  align-items: center;
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
  $today: boolean
  $highlight: 'nonEditableAbsence' | 'holidayPeriod' | undefined
  $selected: boolean
}>`
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: flex-start;
  position: relative;
  min-height: 150px;
  padding: ${defaultMargins.s};
  background-color: ${(p) =>
    p.$highlight === 'nonEditableAbsence'
      ? p.theme.colors.grayscale.g15
      : p.$highlight === 'holidayPeriod'
        ? p.theme.colors.accents.a10powder
        : p.theme.colors.grayscale.g0};
  border: none;
  outline: 1px solid ${colors.grayscale.g15};
  cursor: pointer;
  user-select: none;
  text-align: left;

  ${(p) =>
    p.$today
      ? css`
          border-left: 4px solid ${colors.status.success};
          padding-left: calc(${defaultMargins.s} - 3px);
        `
      : ''};

  ${(p) =>
    p.$selected
      ? css`
          box-shadow: 0 2px 3px 2px #00000030;
          z-index: 1;
        `
      : ''};

  :focus {
    box-shadow: 0 0 0 4px ${(p) => p.theme.colors.main.m2Focus};
    z-index: 1;
  }
`

const DayCellHeader = styled.div`
  display: flex;
  justify-content: space-between;
  margin-bottom: ${defaultMargins.s};
  width: 100%;
`

const DayCellDate = styled.div<{ inactive: boolean; holiday: boolean }>`
  font-family: Montserrat, sans-serif;
  font-style: normal;
  color: ${(p) =>
    p.inactive
      ? colors.grayscale.g70
      : p.holiday
        ? colors.accents.a2orangeDark
        : colors.main.m1};
  font-weight: ${fontWeights.semibold};
  font-size: 1.25rem;
`

const InactiveCell = styled.div`
  background-color: transparent;
`
