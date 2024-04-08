// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { fi } from 'date-fns/locale/fi'
import React from 'react'
import ReactDatePicker, { ReactDatePickerProps } from 'react-datepicker'
import styled from 'styled-components'

import LocalDate from 'lib-common/local-date'

import 'react-datepicker/dist/react-datepicker.css'

export const DATE_FORMATS_PARSED = [
  'dd.MM.yyyy',
  'dd.MM.yy',
  'd.M.yyyy',
  'd.M.yy',
  'ddMMyyyy',
  'ddMMyy'
]

const StyledInput = styled.input`
  -webkit-font-smoothing: antialiased;
  text-size-adjust: 100%;
  box-sizing: border-box;
  margin: 0;
  font-family: 'Open Sans', 'Arial', sans-serif;
  -webkit-appearance: none;
  align-items: center;
  border: 1px solid transparent;
  font-size: 1rem;
  justify-content: flex-start;
  line-height: 1.5;
  padding-left: calc(0.625em - 1px);
  padding-right: calc(0.625em - 1px);
  padding-top: calc(0.5em - 1px);
  position: relative;
  height: 2.5em;
  border-color: ${(p) => p.theme.colors.grayscale.g70};
  color: ${(p) => p.theme.colors.grayscale.g100};
  display: block;
  box-shadow: none;
  max-width: 100%;
  width: 100%;
  min-height: 2.5em;
  border-radius: 0;
  border-width: 0 0 1px 0;
  background-color: transparent;
  padding-bottom: calc(0.5em - 1px);

  :focus {
    padding-bottom: calc(calc(0.5em - 1px) - 1px);
    border-bottom-width: 2px;
    border-color: ${(p) => p.theme.colors.main.m2};
    outline: none;
  }
`

const DatePickerContainer = styled.div`
  &.full-width {
    width: 100%;
  }

  &.half-width {
    // Daterange separator '-' is 22px width
    width: calc(50% - 12px);
    display: inline-flex;
  }

  &.short {
    width: 120px;
  }

  &.inline-block {
    display: inline-block;
  }

  .react-datepicker-wrapper,
  .react-datepicker__input-container {
    width: 100%;
  }

  .react-datepicker__close-icon {
    right: 0;
    &::after {
      background-color: transparent;
      color: ${(p) => p.theme.colors.grayscale.g15};
      font-size: 25px;
    }
  }

  .react-datepicker__header {
    .react-datepicker__current-month.react-datepicker__current-month--hasYearDropdown.react-datepicker__current-month--hasMonthDropdown {
      display: none;
    }
  }
`

interface CommonProps {
  date: LocalDate | null | undefined
  onChange: (date: LocalDate) => void
  onFocus?: (e: React.FocusEvent<HTMLInputElement>) => void
  minDate?: LocalDate
  maxDate?: LocalDate
  type?: 'full-width' | 'half-width' | 'short'
  dateFormat?: string[]
  'data-qa'?: string
  options?: Partial<ReactDatePickerProps>
  disabled?: boolean
  className?: string
  placeholder?: string
}

interface DatePickerProps extends CommonProps {
  date: LocalDate | undefined
}

interface DatePickerClearableProps extends CommonProps {
  onCleared: () => void
}

const defaultProps: Partial<ReactDatePickerProps> = {
  popperPlacement: 'bottom',
  showMonthDropdown: true,
  showYearDropdown: true
}
/**
 * @deprecated
 */
export function DatePickerDeprecated({
  date,
  onChange,
  onFocus,
  minDate,
  maxDate,
  type = 'half-width',
  dateFormat = DATE_FORMATS_PARSED,
  options,
  disabled,
  className,
  'data-qa': dataQa
}: DatePickerProps) {
  return (
    <DatePickerContainer
      className={`${type} ${className ? className : ''}`}
      data-qa={dataQa}
    >
      <ReactDatePicker
        {...defaultProps}
        locale={fi}
        customInput={<StyledInput />}
        selected={date?.toSystemTzDate()}
        isClearable={false}
        dateFormat={dateFormat}
        minDate={minDate?.toSystemTzDate()}
        maxDate={maxDate?.toSystemTzDate()}
        onChange={(newDate) => {
          // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
          const date: Date = Array.isArray(newDate) ? newDate[0] : newDate
          onChange(
            date
              ? LocalDate.fromSystemTzDate(date)
              : LocalDate.todayInSystemTz()
          )
        }}
        onFocus={onFocus}
        disabled={disabled}
        data-qa={dataQa}
        strictParsing
        {...options}
      />
    </DatePickerContainer>
  )
}

/**
 * @deprecated
 */
export function DatePickerClearableDeprecated({
  date = null,
  onChange,
  onFocus,
  onCleared,
  minDate,
  maxDate,
  dateFormat = DATE_FORMATS_PARSED,
  type = 'half-width',
  options,
  className,
  'data-qa': dataQa
}: DatePickerClearableProps) {
  return (
    <DatePickerContainer
      className={`${type} ${className ? className : ''}`}
      data-qa={dataQa}
    >
      <ReactDatePicker
        {...defaultProps}
        locale={fi}
        customInput={<StyledInput />}
        selected={date?.toSystemTzDate()}
        isClearable={true}
        dateFormat={dateFormat}
        minDate={minDate?.toSystemTzDate()}
        maxDate={maxDate?.toSystemTzDate()}
        strictParsing
        onChange={(newDate) => {
          // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
          const date: Date = Array.isArray(newDate) ? newDate[0] : newDate
          if (!date) {
            onCleared()
          } else {
            onChange(LocalDate.fromSystemTzDate(date))
          }
        }}
        onFocus={onFocus}
        {...options}
      />
    </DatePickerContainer>
  )
}
