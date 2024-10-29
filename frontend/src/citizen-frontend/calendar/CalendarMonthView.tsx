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
  useRef,
  useState
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
import { Button } from 'lib-components/atoms/buttons/Button'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import LegacyInlineButton from 'lib-components/atoms/buttons/LegacyInlineButton'
import Container, { ContentArea } from 'lib-components/layout/Container'
import {
  ExpandingInfoBox,
  InlineInfoButton
} from 'lib-components/molecules/ExpandingInfo'
import { fontWeights, H2 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/citizen'
import colors from 'lib-customizations/common'
import {
  faCalendar,
  faCalendarPlus,
  faChevronLeft,
  faChevronRight,
  faComment,
  faTreePalm,
  faUserMinus
} from 'lib-icons'

import { useUser } from '../auth/state'
import { useLang, useTranslation } from '../localization'

import {
  CalendarEventCount,
  CalendarEventCountContainer
} from './CalendarEventCount'
import { getSummaryForMonth, InlineWarningIcon } from './MonthElem'
import MonthlyHoursSummary, { MonthlyTimeSummary } from './MonthlyHoursSummary'
import ReportHolidayLabel from './ReportHolidayLabel'
import { ChildImageData, getChildImages } from './RoundChildImages'
import { BackgroundHighlightType, Reservations } from './calendar-elements'
import { useSummaryInfo } from './hooks'
import { activeQuestionnaireQuery, holidayPeriodsQuery } from './queries'
import { isQuestionnaireAvailable } from './utils'

export interface Props {
  childData: ReservationChild[]
  calendarDays: ReservationResponseDay[]
  onCreateReservationClicked: () => void
  onCreateAbsencesClicked: (initialDate: LocalDate | undefined) => void
  onOpenDiscussionReservationsClicked: () => void
  onReportHolidaysClicked: () => void
  selectedDate: LocalDate | undefined
  selectDate: (date: LocalDate) => void
  includeWeekends: boolean
  dayIsReservable: (date: LocalDate) => boolean
  events: CitizenCalendarEvent[]
  isDiscussionActionVisible: boolean
}

export function countEventsForDay(
  events: CitizenCalendarEvent[],
  day: LocalDate
) {
  const currentEvents = events.filter((e) => e.period.includes(day))

  if (currentEvents.length > 0) {
    const daycareEvents = currentEvents.filter(
      (e) => e.eventType === 'DAYCARE_EVENT'
    )
    const discussionSurveys = currentEvents.filter(
      (e) => e.eventType === 'DISCUSSION_SURVEY'
    )
    //the number of children that are attending the event at this calendar day
    const eventCount = sum(
      daycareEvents.map(
        ({ attendingChildren }) =>
          Object.values(attendingChildren).filter((ac) =>
            ac.some(({ periods }) => periods.some((p) => p.includes(day)))
          ).length
      )
    )

    const discussionReservationCount = featureFlags.discussionReservations
      ? discussionSurveys.reduce(
          (acc, curr) =>
            //the number of children that have a reserved discussion time for this survey at this calendar date
            //(if a reserved time is returned, it belongs to the child)
            acc +
            Object.values(curr.timesByChild).filter((times) =>
              times.some((t) => t.date.isEqual(day) && t.childId)
            ).length,
          0
        )
      : 0

    return eventCount + discussionReservationCount
  } else {
    return 0
  }
}

export default React.memo(function CalendarMonthView({
  childData,
  calendarDays,
  onCreateReservationClicked,
  onCreateAbsencesClicked,
  onOpenDiscussionReservationsClicked,
  onReportHolidaysClicked,
  selectedDate,
  selectDate,
  includeWeekends,
  dayIsReservable,
  events,
  isDiscussionActionVisible
}: Props) {
  const i18n = useTranslation()
  const calendarMonths = useMemo(
    () => groupByMonth(calendarDays),
    [calendarDays]
  )
  const todayRef = useRef<HTMLButtonElement>(null)

  // Based on the initial data fetch, index 1 represents the current month
  const [selectedMonthIndex, setSelectedMonthIndex] = useState(1)
  const selectedMonthData = calendarMonths[selectedMonthIndex]

  const prevMonth = useCallback(() => {
    setSelectedMonthIndex((prevIndex) =>
      prevIndex > 0 ? prevIndex - 1 : prevIndex
    )
  }, [])
  const nextMonth = useCallback(() => {
    setSelectedMonthIndex((prevIndex) =>
      prevIndex < calendarMonths.length - 1 ? prevIndex + 1 : prevIndex
    )
  }, [calendarMonths.length])

  const isDateInCurrentMonth = useCallback(() => {
    if (!selectedDate) return false
    return selectedMonthData.weeks.some((w) => {
      return w.calendarDays.some((d) => d.date.isEqual(selectedDate))
    })
  }, [selectedDate, selectedMonthData.weeks])

  const firstDayOfMonth = useMemo(() => {
    return selectedMonthData.weeks[0].calendarDays[0].date
  }, [selectedMonthData.weeks])

  useEffect(() => {
    if (selectedDate && !isDateInCurrentMonth()) {
      if (selectedDate.isBefore(firstDayOfMonth)) {
        prevMonth()
      } else {
        nextMonth()
      }
    }
  }, [
    firstDayOfMonth,
    isDateInCurrentMonth,
    nextMonth,
    prevMonth,
    selectedDate
  ])

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

  const childSummaries = getSummaryForMonth(
    childData,
    selectedMonthData.year,
    selectedMonthData.month
  )
  const { summaryInfoOpen, toggleSummaryInfo, displayAlert } =
    useSummaryInfo(childSummaries)

  return (
    <>
      <StickyTopBar>
        <ButtonContainer>
          {questionnaireAvailable && (
            <LegacyInlineButton
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
          {featureFlags.discussionReservations && isDiscussionActionVisible && (
            <Button
              appearance="inline"
              onClick={onOpenDiscussionReservationsClicked}
              text={
                i18n.calendar.discussionTimeReservation.surveyModalButtonText
              }
              icon={faComment}
              data-qa="open-discussions-modal"
            />
          )}
          <Button
            appearance="inline"
            onClick={onCreateAbsences}
            text={i18n.calendar.newAbsence}
            icon={faUserMinus}
            data-qa="open-absences-modal"
          />
          <Button
            appearance="inline"
            onClick={onCreateReservationClicked}
            text={i18n.calendar.newReservationBtn}
            icon={faCalendarPlus}
            data-qa="open-reservations-modal"
          />
        </ButtonContainer>
      </StickyTopBar>
      <Container>
        <MonthPicker
          childSummaries={childSummaries}
          selectedMonthData={selectedMonthData}
          currentIndex={selectedMonthIndex}
          monthDataLength={calendarMonths.length}
          prevMonth={prevMonth}
          nextMonth={nextMonth}
          toggleSummaryInfo={toggleSummaryInfo}
          summaryInfoOpen={summaryInfoOpen}
          displayAlert={displayAlert}
        />
        <Month
          key={`${selectedMonthData?.month}${selectedMonthData?.year}`}
          year={selectedMonthData.year}
          month={selectedMonthData.month}
          weeks={selectedMonthData.weeks}
          holidayPeriods={holidayPeriods}
          todayRef={todayRef}
          selectedDate={selectedDate}
          selectDate={selectDate}
          includeWeekends={includeWeekends}
          dayIsReservable={dayIsReservable}
          childImages={childImages}
          events={events}
          childSummaries={childSummaries}
          toggleSummaryInfo={toggleSummaryInfo}
          summaryInfoOpen={childSummaries.length > 0 ? summaryInfoOpen : false}
        />
      </Container>
    </>
  )
})

interface CalendarWeek {
  year: number
  weekNumber: number
  calendarDays: ReservationResponseDay[]
}

function groupByWeek(days: ReservationResponseDay[]): CalendarWeek[] {
  const weeks: CalendarWeek[] = []
  let currentWeek: CalendarWeek | undefined = undefined
  days.forEach((d) => {
    if (!currentWeek || currentWeek.weekNumber !== d.date.getIsoWeek()) {
      currentWeek = {
        year: d.date.year,
        weekNumber: d.date.getIsoWeek(),
        calendarDays: []
      }
      weeks.push(currentWeek)
    }
    currentWeek.calendarDays.push(d)
  })
  return weeks
}

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
  events,
  childSummaries,
  summaryInfoOpen,
  toggleSummaryInfo
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
  childSummaries: MonthlyTimeSummary[]
  summaryInfoOpen: boolean
  toggleSummaryInfo: () => void
}) {
  const i18n = useTranslation()

  return (
    <ContentArea opaque={false} key={`${month}${year}`}>
      {summaryInfoOpen && (
        <MonthSummaryInfoBox
          info={
            <MonthlyHoursSummary
              year={year}
              month={month}
              childSummaries={childSummaries}
            />
          }
          data-qa={`monthly-summary-info-container-${month}-${year}`}
          close={toggleSummaryInfo}
        />
      )}
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
          dateType={dateType(d.date)}
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

type DateType = 'past' | 'today' | 'future'

function dateType(date: LocalDate): DateType {
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
      dateType === 'past'
        ? 'past'
        : day.holiday
          ? 'holiday'
          : day.children.length > 0 &&
              day.children.every((c) => c.absence && !c.absence.editable)
            ? 'nonEditableAbsence'
            : holidayPeriods.some((p) => p.includes(day.date))
              ? 'holidayPeriod'
              : undefined,
    [day.children, day.date, day.holiday, holidayPeriods, dateType]
  )
  const onClick = useCallback(
    () => selectDate(day.date),
    [day.date, selectDate]
  )

  const eventCount = useMemo(
    () => countEventsForDay(events, day.date),
    [day.date, events]
  )

  const theme = useTheme()

  const i18n = useTranslation()

  return (
    <DayCell
      ref={ref}
      $today={dateType === 'today'}
      $highlight={highlight}
      $selected={selected}
      onClick={onClick}
      data-qa={`desktop-calendar-day-${day.date.formatIso()}`}
      id={`calendar-day-${day.date.formatIso()}`}
    >
      <DayCellHeader>
        <DayCellDate
          inactive={!dayIsReservable(day.date)}
          holiday={day.holiday}
          aria-label={day.date.formatExotic('cccc do MMMM', lang)}
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
          backgroundHighlight={highlight}
        />
      </div>
    </DayCell>
  )
})

const MonthPicker = React.memo(function MonthPicker({
  childSummaries,
  selectedMonthData,
  currentIndex,
  monthDataLength,
  prevMonth,
  nextMonth,
  toggleSummaryInfo,
  summaryInfoOpen,
  displayAlert
}: {
  childSummaries: MonthlyTimeSummary[]
  selectedMonthData: MonthlyData
  currentIndex: number
  monthDataLength: number
  prevMonth: () => void
  nextMonth: () => void
  toggleSummaryInfo: () => void
  summaryInfoOpen: boolean
  displayAlert: boolean
}) {
  const i18n = useTranslation()
  return (
    <MonthPickerContainer>
      <MonthTitle>
        {`${i18n.common.datetime.months[selectedMonthData.month - 1]} ${selectedMonthData.year}`}
        {childSummaries.length > 0 && (
          <InlineInfoButton
            onClick={toggleSummaryInfo}
            aria-label={i18n.common.openExpandingInfo}
            margin="zero"
            data-qa={`monthly-summary-info-button-${selectedMonthData.month}-${selectedMonthData.year}`}
            open={summaryInfoOpen}
          />
        )}
        {displayAlert && <InlineWarningIcon />}
      </MonthTitle>
      <Gap size="s" horizontal />
      <IconOnlyButton
        icon={faChevronLeft}
        onClick={prevMonth}
        disabled={currentIndex === 0}
        aria-label={i18n.calendar.previousMonth}
      />
      <Gap size="s" horizontal />
      <IconOnlyButton
        icon={faChevronRight}
        onClick={nextMonth}
        disabled={currentIndex === monthDataLength - 1}
        aria-label={i18n.calendar.nextMonth}
      />
    </MonthPickerContainer>
  )
})

const StickyTopBar = styled.div`
  position: sticky;
  top: 0;
  z-index: 2;
  width: 100%;
  height: 80px;
  background: ${(p) => p.theme.colors.grayscale.g0};
  box-shadow: 0 -4px 8px 2px #0000000a;
  margin-bottom: ${defaultMargins.L};
`

const ButtonContainer = styled.div`
  height: 100%;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: ${defaultMargins.L};
  margin: 0 auto;
  padding-right: ${defaultMargins.s};

  @media screen and (min-width: 1152px) and (max-width: 1215px) {
    max-width: 1152px;
    width: 1152px;
  }
  @media screen and (min-width: 1216px) {
    max-width: 1152px;
    width: 1152px;
  }
  @media screen and (min-width: 1408px) {
    max-width: 1344px;
    width: 1344px;
  }
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
  align-items: center;
  display: flex;
  min-width: 240px;
`

const DayCell = styled.button<{
  $today: boolean
  $highlight: BackgroundHighlightType
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
    p.$highlight === 'nonEditableAbsence' || p.$highlight === 'holiday'
      ? p.theme.colors.grayscale.g15
      : p.$highlight === 'holidayPeriod'
        ? p.theme.colors.accents.a10powder
        : p.$highlight === 'past'
          ? p.theme.colors.grayscale.g4
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

  &:focus {
    outline: 2px solid ${(p) => p.theme.colors.main.m2};
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

const MonthSummaryInfoBox = styled(ExpandingInfoBox)`
  margin: 0 0 ${defaultMargins.xs};
  width: 100%;
`

const MonthPickerContainer = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  align-items: center;
  padding-left: ${defaultMargins.L};
  padding-right: ${defaultMargins.L};
`
