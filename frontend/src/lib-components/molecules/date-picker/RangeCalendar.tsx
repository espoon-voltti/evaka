// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  createCalendar,
  getWeeksInMonth,
  CalendarDate,
  startOfWeek,
  today,
  isSameDay,
  getLocalTimeZone
} from '@internationalized/date'
import {
  useCalendarCell,
  useCalendarGrid,
  useRangeCalendar
} from '@react-aria/calendar'
import { useDateFormatter, useLocale } from '@react-aria/i18n'
import {
  RangeCalendarStateOptions,
  useRangeCalendarState,
  RangeCalendarState
} from '@react-stately/calendar'
import classNames from 'classnames'
import React, { useMemo } from 'react'
import styled from 'styled-components'

import IconButton from 'lib-components/atoms/buttons/IconButton'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { faChevronLeft, faChevronRight } from 'lib-icons'

import { CalendarAriaProps } from './Calendar'

const CalendarContainer = styled.div`
  padding: ${defaultMargins.s};
`

const CalendarHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: ${defaultMargins.s};
`

export default React.memo(function RangeCalendar({
  previousMonthLabel,
  nextMonthLabel,
  ...props
}: Omit<RangeCalendarStateOptions, 'locale' | 'createCalendar'> &
  CalendarAriaProps) {
  const { locale } = useLocale()
  const state = useRangeCalendarState({
    ...props,
    locale,
    createCalendar
  })

  const ref = React.useRef<HTMLDivElement>(null)
  const { calendarProps, prevButtonProps, nextButtonProps, title } =
    useRangeCalendar(props, state, ref)

  return (
    <CalendarContainer {...calendarProps} ref={ref}>
      <CalendarHeader>
        <IconButton
          {...prevButtonProps}
          icon={faChevronLeft}
          aria-label={previousMonthLabel}
        />
        <div>{title}</div>
        <IconButton
          {...nextButtonProps}
          icon={faChevronRight}
          aria-label={nextMonthLabel}
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

function CalendarGrid({ state }: { state: RangeCalendarState }) {
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
    <CalendarGridTable {...gridProps}>
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
    </CalendarGridTable>
  )
}

const CalendarGridTable = styled.table`
  border-collapse: collapse;
`

const Cell = styled.div`
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  /**
    * the width should be enough to span the width of the
    * day header, even on other languages (e.g., "MON")
    */
  width: 36px;
  height: 36px;
  color: ${(p) => p.theme.colors.grayscale.g100};
  font-size: 10pt;
  outline: none;
  -webkit-tap-highlight-color: transparent;
  border-radius: 4px;

  &.selected {
    background-color: ${(p) => p.theme.colors.main.m2};
    font-weight: bold;
    color: ${(p) => p.theme.colors.grayscale.g0};
  }

  &.selected-start {
    border-radius: 4px 0 0 4px;
  }

  &.selected-end {
    border-radius: 0 4px 4px 0;
  }

  &.selected-mid {
    border-radius: 0;
    background-color: ${(p) => p.theme.colors.main.m4};
  }

  &.disabled,
  &.unavailable {
    color: ${(p) => p.theme.colors.grayscale.g35};
    cursor: default;
  }

  &.today:not(&.selected) {
    color: ${(p) => p.theme.colors.main.m2};
    font-weight: ${fontWeights.bold};
  }

  &:focus {
    box-shadow: 0 0 0 2px ${(p) => p.theme.colors.main.m2Focus};
  }
`

function CalendarCell({
  state,
  date
}: {
  state: RangeCalendarState
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

  const isSelectionStart = state.highlightedRange
    ? isSameDay(date, state.highlightedRange.start)
    : isSelected
  const isSelectionEnd = state.highlightedRange
    ? isSameDay(date, state.highlightedRange.end)
    : isSelected

  return (
    <CalendarCellTd {...cellProps}>
      <Cell
        {...buttonProps}
        ref={ref}
        hidden={isOutsideVisibleRange}
        className={classNames({
          selected: isSelectionStart || isSelectionEnd,
          'selected-start': isSelectionStart && !isSelectionEnd,
          'selected-end': isSelectionEnd && !isSelectionStart,
          'selected-mid': isSelected && !isSelectionStart && !isSelectionEnd,
          disabled: isDisabled,
          unavailable: isUnavailable,
          today: date.compare(today(getLocalTimeZone())) === 0
        })}
      >
        {formattedDate}
      </Cell>
    </CalendarCellTd>
  )
}

const CalendarCellTd = styled.td`
  padding: 0;
`
