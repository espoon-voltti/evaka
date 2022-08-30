// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  createCalendar,
  getWeeksInMonth,
  CalendarDate,
  startOfWeek,
  today,
  getLocalTimeZone
} from '@internationalized/date'
import {
  useCalendar,
  useCalendarCell,
  useCalendarGrid
} from '@react-aria/calendar'
import { useDateFormatter, useLocale } from '@react-aria/i18n'
import {
  useCalendarState,
  CalendarStateOptions,
  CalendarState
} from '@react-stately/calendar'
import classNames from 'classnames'
import React, { useMemo } from 'react'
import styled from 'styled-components'

import IconButton from 'lib-components/atoms/buttons/IconButton'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { faChevronLeft, faChevronRight } from 'lib-icons'

const CalendarContainer = styled.div`
  padding: ${defaultMargins.s};
`

const CalendarHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: ${defaultMargins.s};
`

const CalendarTable = styled.table`
  border-spacing: 0;
  border-collapse: collapse;
`

export interface CalendarAriaProps {
  calendarLabel: string
  previousMonthLabel: string
  nextMonthLabel: string
}

export default React.memo(function Calendar(
  props: Omit<CalendarStateOptions, 'locale' | 'createCalendar'> &
    CalendarAriaProps
) {
  const { locale } = useLocale()
  const state = useCalendarState({
    ...props,
    locale,
    createCalendar
  })

  const { calendarProps, prevButtonProps, nextButtonProps, title } =
    useCalendar(props, state)

  return (
    <CalendarContainer {...calendarProps}>
      <CalendarHeader>
        <IconButton
          {...prevButtonProps}
          icon={faChevronLeft}
          aria-label={props.previousMonthLabel}
        />
        <div>{title}</div>
        <IconButton
          {...nextButtonProps}
          icon={faChevronRight}
          aria-label={props.nextMonthLabel}
        />
      </CalendarHeader>
      <CalendarGrid state={state} />
    </CalendarContainer>
  )
})

const GridHeadingCell = styled.th`
  color: ${(p) => p.theme.colors.grayscale.g70};
  font-weight: bold;
  font-size: 10pt;
  text-transform: uppercase;
`

function CalendarGrid({ state }: { state: CalendarState }) {
  const { locale } = useLocale()
  const { gridProps, headerProps } = useCalendarGrid({}, state)

  const dayFormatter = useDateFormatter({
    weekday: 'short',
    timeZone: state.timeZone
  })
  const weekDays = useMemo(() => {
    const weekStart = startOfWeek(today(state.timeZone), locale)
    return Array.from({ length: 7 }, (_, i) =>
      dayFormatter.format(weekStart.add({ days: i }).toDate(state.timeZone))
    )
  }, [locale, state.timeZone, dayFormatter])

  // Get the number of weeks in the month so we can render the proper number of rows.
  const weeksInMonth = getWeeksInMonth(state.visibleRange.start, locale)

  return (
    <CalendarTable {...gridProps}>
      <thead {...headerProps}>
        <tr>
          {weekDays.map((day, index) => (
            <GridHeadingCell key={index}>{day}</GridHeadingCell>
          ))}
        </tr>
      </thead>
      <tbody>
        {[...new Array(weeksInMonth).keys()].map((weekIndex) => (
          <tr key={weekIndex}>
            {state
              .getDatesInWeek(weekIndex)
              .map((date, i) =>
                date &&
                date >= state.visibleRange.start &&
                date <= state.visibleRange.end ? (
                  <CalendarCell key={i} state={state} date={date} />
                ) : (
                  <td key={i} />
                )
              )}
          </tr>
        ))}
      </tbody>
    </CalendarTable>
  )
}

const Cell = styled.div`
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 4px;
  color: ${(p) => p.theme.colors.grayscale.g100};
  font-size: 10pt;
  margin: 4px;

  &.selected {
    background-color: ${(p) => p.theme.colors.main.m2};
    font-weight: bold;
    color: ${(p) => p.theme.colors.grayscale.g0};
  }

  &.today:not(&.selected) {
    color: ${(p) => p.theme.colors.main.m2};
    font-weight: ${fontWeights.bold};
  }

  &.disabled,
  &.unavailable {
    color: ${(p) => p.theme.colors.grayscale.g35};
    cursor: default;
  }
`

function CalendarCell({
  state,
  date
}: {
  state: CalendarState
  date: CalendarDate
}) {
  const ref = React.useRef<HTMLDivElement | null>(null)
  const {
    cellProps,
    buttonProps,
    isSelected,
    isOutsideVisibleRange,
    isDisabled,
    isUnavailable,
    formattedDate
  } = useCalendarCell({ date }, state, ref)

  return (
    <td {...cellProps}>
      <Cell
        {...buttonProps}
        ref={ref}
        hidden={isOutsideVisibleRange}
        className={classNames({
          selected: isSelected,
          disabled: isDisabled,
          unavailable: isUnavailable,
          today: date.compare(today(getLocalTimeZone())) === 0
        })}
      >
        {formattedDate}
      </Cell>
    </td>
  )
}
