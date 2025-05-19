// SPDX-FileCopyrightText: 2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled, { css } from 'styled-components'

import type { CitizenCalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import type {
  ReservationChild,
  ReservationResponseDay
} from 'lib-common/generated/api-types/reservations'
import type LocalDate from 'lib-common/local-date'
import { formatPreferredName } from 'lib-common/names'
import { Button } from 'lib-components/atoms/buttons/Button'
import {
  ExpandingInfoBox,
  InlineInfoButton
} from 'lib-components/molecules/ExpandingInfo'
import { fontWeights, H2, H3 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faArrowsRotate, fasExclamationTriangle } from 'lib-icons'

import { useTranslation } from '../localization'

import DayElem from './DayElem'
import type { MonthlyTimeSummary } from './MonthlyHoursSummary'
import MonthlyHoursSummary from './MonthlyHoursSummary'
import type { ChildImageData } from './RoundChildImages'
import { useSummaryInfo } from './hooks'

export function getSummaryForMonth(
  childData: ReservationChild[],
  year: number,
  month: number
): MonthlyTimeSummary[] {
  return childData.flatMap(({ monthSummaries, firstName, preferredName }) => {
    const summaryForMonth = monthSummaries?.find(
      (monthSummary) =>
        monthSummary.year === year && monthSummary.month === month
    )
    if (!summaryForMonth) {
      return []
    }
    return {
      name: formatPreferredName({
        firstName,
        preferredName
      }),
      ...summaryForMonth
    }
  })
}

interface MonthProps {
  calendarMonth: CalendarMonth
  selectDate: (date: LocalDate) => void
  dayIsReservable: (date: LocalDate) => boolean
  dayIsHolidayPeriod: (date: LocalDate) => boolean
  events: CitizenCalendarEvent[]
  childImages: ChildImageData[]
  childSummaries: MonthlyTimeSummary[]
  monthIndex: number
  fetchPrevious: () => void
  scrollToDate: LocalDate
  loading: boolean
}

export default React.memo(function MonthElem({
  calendarMonth,
  dayIsHolidayPeriod,
  selectDate,
  dayIsReservable,
  events,
  childImages,
  childSummaries,
  monthIndex,
  fetchPrevious,
  scrollToDate,
  loading
}: MonthProps) {
  const i18n = useTranslation()

  const { summaryInfoOpen, toggleSummaryInfo, displayAlert } =
    useSummaryInfo(childSummaries)
  return (
    <div>
      <MonthSummaryContainer>
        <MonthTitle>
          {`${i18n.common.datetime.months[calendarMonth.monthNumber - 1]} ${calendarMonth.year}`}
          {childSummaries.length > 0 && (
            <InlineInfoButton
              onClick={toggleSummaryInfo}
              aria-label={i18n.common.openExpandingInfo}
              margin="zero"
              data-qa={`mobile-monthly-summary-info-button-${calendarMonth.monthNumber}-${calendarMonth.year}`}
              open={summaryInfoOpen}
            />
          )}

          {displayAlert && <InlineWarningIcon />}
        </MonthTitle>
        {summaryInfoOpen && (
          <MonthlySummaryInfoBox
            info={
              <MonthlyHoursSummary
                year={calendarMonth.year}
                month={calendarMonth.monthNumber}
                childSummaries={childSummaries}
              />
            }
            data-qa={`mobile-monthly-summary-info-container-${calendarMonth.monthNumber}-${calendarMonth.year}`}
            close={toggleSummaryInfo}
          />
        )}
      </MonthSummaryContainer>
      {calendarMonth.calendarDays.map((day, index) => (
        <div key={day.date.formatIso()}>
          {monthIndex === 0 && index === 0 && (
            <MonthFetchPrevious>
              <MonthFetchButton
                primary
                icon={faArrowsRotate}
                text={i18n.calendar.fetchPrevious}
                onClick={fetchPrevious}
                disabled={loading}
                data-qa="fetch-previous-button"
              />
            </MonthFetchPrevious>
          )}
          {day.date.getIsoDayOfWeek() === 1 && (
            <WeekTitle>
              {i18n.common.datetime.week} {day.date.getIsoWeek()}
            </WeekTitle>
          )}
          <DayElem
            calendarDay={day}
            selectDate={selectDate}
            isReservable={dayIsReservable(day.date)}
            isHolidayPeriod={dayIsHolidayPeriod(day.date)}
            childImages={childImages}
            events={events}
            scrollToDate={scrollToDate}
          />
        </div>
      ))}
    </div>
  )
})

export interface CalendarMonth {
  year: number
  monthNumber: number
  calendarDays: ReservationResponseDay[]
}

export function groupByMonth(days: ReservationResponseDay[]): CalendarMonth[] {
  const months: CalendarMonth[] = []
  let currentMonth: CalendarMonth | undefined = undefined
  days.forEach((d) => {
    if (
      !currentMonth ||
      currentMonth.year !== d.date.year ||
      currentMonth.monthNumber !== d.date.month
    ) {
      currentMonth = {
        year: d.date.year,
        monthNumber: d.date.month,
        calendarDays: []
      }
      months.push(currentMonth)
    }
    currentMonth.calendarDays.push(d)
  })
  return months
}

const titleStyles = css`
  margin: 0;
  display: flex;
  justify-content: flex-start;
  align-items: center;
  background-color: ${(p) => p.theme.colors.main.m4};
  color: ${(p) => p.theme.colors.grayscale.g100};
  font-family: 'Open Sans', 'Arial', sans-serif;
  font-weight: ${fontWeights.semibold};
`
const MonthTitle = styled(H2)`
  font-size: 1.25em;
  padding: 0 ${defaultMargins.s} 0 0;
  ${titleStyles};
`
const MonthFetchPrevious = styled.div`
  background-color: ${(p) => p.theme.colors.main.m4};
  padding: ${defaultMargins.s};
  display: flex;
  justify-content: center;
`
const MonthFetchButton = styled(Button)`
  border-radius: 40px;
`
const WeekTitle = styled(H3)`
  padding: ${defaultMargins.s};
  border-bottom: 1px solid ${colors.grayscale.g15};
  ${titleStyles};
`
const MonthSummaryContainer = styled.div`
  position: sticky;
  top: 54px;
  z-index: 1;

  padding: ${defaultMargins.s};
  background-color: ${(p) => p.theme.colors.main.m4};
  border-top: 6px solid ${colors.main.m3};
  color: ${(p) => p.theme.colors.grayscale.g100};
  font-family: 'Open Sans', 'Arial', sans-serif;
`

const MonthlySummaryInfoBox = styled(ExpandingInfoBox)`
  margin-top: 0;
  margin-bottom: 0;
  max-height: 400px;
  overflow-y: auto;

  section {
    padding-bottom: 0;
  }
`
const InlineIconContainer = styled.div`
  margin-left: ${defaultMargins.xs};
`

export const InlineWarningIcon = () => (
  <InlineIconContainer>
    <FontAwesomeIcon
      icon={fasExclamationTriangle}
      color={colors.status.warning}
    />
  </InlineIconContainer>
)
