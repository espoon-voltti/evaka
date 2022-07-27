// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useLayoutEffect, useRef, useState } from 'react'
import type { DayModifiers } from 'react-day-picker'
import styled from 'styled-components'

import LocalDate from 'lib-common/local-date'

import type { InputInfo } from '../../atoms/form/InputField'
import { defaultMargins } from '../../white-space'

import DatePickerDay from './DatePickerDay'
import DatePickerInput from './DatePickerInput'

const inputWidth = 120
const DatePickerWrapper = styled.div`
  position: relative;
  display: inline-block;
  width: ${inputWidth}px;
`
const minMargin = 16
const overflow = 70

const DayPickerPositioner = styled.div<{ openAbove?: boolean }>`
  position: absolute;
  ${({ openAbove }) =>
    openAbove ? 'bottom' : 'top'}: calc(2.5rem + ${minMargin}px);
  left: -${overflow}px;
  right: -${overflow}px;
  z-index: 99999;
  justify-content: center;
  align-items: center;
  display: inline-block;
`

const DayPickerDiv = styled.div`
  background-color: ${(p) => p.theme.colors.grayscale.g0};
  padding: ${defaultMargins.s} 0;
  border-radius: 2px;
  box-shadow: 0 0 4px rgba(0, 0, 0, 0.25);
  display: flex;
  justify-content: center;

  p:not(:last-child) {
    margin-bottom: 8px;
  }
`

interface BaseDatePickerProps {
  date: LocalDate | null
  onChange: (date: LocalDate | null) => void
  onFocus?: (e: React.FocusEvent<HTMLInputElement>) => void
  onBlur?: () => void
  locale: 'fi' | 'sv' | 'en'
  info?: InputInfo
  hideErrorsBeforeTouched?: boolean
  disabled?: boolean
  'data-qa'?: string
  id?: string
  required?: boolean
  initialMonth?: LocalDate
  openAbove?: boolean
  errorTexts: {
    validDate: string
  }

  /**
   * It is preferable to use `minDate` and `maxDate` instead, if possible.
   * The native date pickers only support minimum and maximum dates, so
   * it is more user-friendly to use min/max whenever possible.
   *
   * Should return a localized error string if the date is invalid, null
   * if the date is valid.
   */
  isInvalidDate?: (date: LocalDate) => string | null

  minDate?: never
  maxDate?: never
}

type WithoutMinMaxBaseProps = Omit<BaseDatePickerProps, 'minDate' | 'maxDate'>

interface MinDatePickerProps extends WithoutMinMaxBaseProps {
  minDate?: LocalDate
  maxDate?: never
  errorTexts: BaseDatePickerProps['errorTexts'] & {
    dateTooEarly: string
  }
}

interface MaxDatePickerProps extends WithoutMinMaxBaseProps {
  minDate?: never
  maxDate?: LocalDate
  errorTexts: BaseDatePickerProps['errorTexts'] & {
    dateTooLate: string
  }
}

interface MinMaxDatePickerProps extends WithoutMinMaxBaseProps {
  minDate?: LocalDate
  maxDate?: LocalDate
  errorTexts: BaseDatePickerProps['errorTexts'] & {
    dateTooEarly: string
    dateTooLate: string
  }
}

export type DatePickerProps =
  | BaseDatePickerProps
  | MinDatePickerProps
  | MaxDatePickerProps
  | MinMaxDatePickerProps

const nativeDatePickerAgentMatchers = [/Android/i]

const checkDateInputSupport = () => {
  if (!nativeDatePickerAgentMatchers.some((m) => m.test(navigator.userAgent))) {
    return false
  }

  const input = document.createElement('input')
  input.setAttribute('type', 'date')

  const testDate = LocalDate.of(2020, 2, 11)
  input.setAttribute('value', testDate.formatIso())

  return (
    !!input.valueAsDate &&
    LocalDate.fromSystemTzDate(input.valueAsDate).isEqual(testDate)
  )
}

export const nativeDatePickerEnabled = checkDateInputSupport()

export default React.memo(function DatePicker({
  date,
  onChange,
  onFocus = () => undefined,
  onBlur = () => undefined,
  locale,
  info,
  hideErrorsBeforeTouched,
  disabled,
  isInvalidDate,
  id,
  required,
  initialMonth,
  openAbove,
  maxDate,
  minDate,
  errorTexts,
  ...props
}: DatePickerProps) {
  const [showDatePicker, setShowDatePicker] = useState<boolean>(false)
  const [showErrors, setShowErrors] = useState(!hideErrorsBeforeTouched)
  const wrapperRef = useRef<HTMLDivElement>(null)
  const pickerRef = useRef<HTMLDivElement>(null)

  const [internalDate, setInternalDate] = useState<LocalDate | null>(date)
  const [parentDate, setParentDate] = useState<LocalDate | null>(date)
  const [internalError, setInternalError] = useState<InputInfo>()

  useEffect(() => {
    setParentDate(date)
    setInternalDate(date)
  }, [date])

  useEffect(() => {
    setInternalError(undefined)

    if (parentDate?.formatIso() !== internalDate?.formatIso()) {
      if (internalDate === null) {
        onChange(null)
      } else if (minDate && internalDate.isBefore(minDate)) {
        setInternalError({ text: errorTexts.dateTooEarly, status: 'warning' })
      } else if (maxDate && internalDate.isAfter(maxDate)) {
        setInternalError({ text: errorTexts.dateTooLate, status: 'warning' })
      } else {
        const validationError = isInvalidDate?.(internalDate)
        if (validationError) {
          setInternalError({ text: validationError, status: 'warning' })
        } else {
          onChange(internalDate)
        }
      }
    }
  }, [
    internalDate,
    isInvalidDate,
    onChange,
    errorTexts,
    minDate,
    maxDate,
    parentDate
  ])

  useEffect(() => {
    if (!hideErrorsBeforeTouched) {
      setShowErrors(true)
    }
  }, [hideErrorsBeforeTouched])

  function hideDatePicker() {
    setShowDatePicker(false)
    setShowErrors(true)
  }

  function handleUserKeyPress(e: React.KeyboardEvent) {
    if (e.key === 'Esc' || e.key === 'Escape') {
      hideDatePicker()
    }
  }

  function handleDayClick(day: Date, modifiers?: DayModifiers) {
    if (modifiers?.disabled) {
      return
    }
    hideDatePicker()
    setInternalDate(LocalDate.fromSystemTzDate(day))
  }

  useLayoutEffect(() => {
    if (showDatePicker) {
      const realignPicker = () => {
        if (wrapperRef.current) {
          const distanceFromLeftEdge = wrapperRef.current.offsetLeft
          const distanceFromRightEdge =
            window.innerWidth - wrapperRef.current.offsetLeft - inputWidth

          const leftOffset =
            overflow - Math.min(overflow, distanceFromLeftEdge - minMargin)
          const rightOffset =
            overflow - Math.min(overflow, distanceFromRightEdge - minMargin)

          if (pickerRef.current && (leftOffset !== 0 || rightOffset !== 0)) {
            const left = -overflow + leftOffset - rightOffset
            pickerRef.current.style['left'] = `${left}px`
            const right = -overflow - leftOffset + rightOffset
            pickerRef.current.style['right'] = `${right}px`
          }
        }
      }
      realignPicker()
      addEventListener('resize', realignPicker, { passive: true })
      return () => removeEventListener('resize', realignPicker)
    }

    return
  }, [showDatePicker])

  useEffect(() => {
    function handleEvent(event: { target: EventTarget | null }) {
      if (event.target instanceof Element) {
        if (wrapperRef.current?.contains(event.target)) {
          return
        }
      }

      hideDatePicker()
    }

    if (showDatePicker) {
      addEventListener('focusin', handleEvent)
      addEventListener('pointerup', handleEvent)

      return () => {
        removeEventListener('focusin', handleEvent)
        removeEventListener('pointerup', handleEvent)
      }
    }

    return () => undefined
  }, [showDatePicker])

  return (
    <DatePickerWrapper ref={wrapperRef} onKeyDown={handleUserKeyPress}>
      <DatePickerInput
        date={internalDate}
        setDate={(date) => {
          if (date !== null) {
            hideDatePicker()
          }
          setInternalDate(date)
        }}
        disabled={disabled}
        onFocus={(ev) => {
          setShowDatePicker(true)
          onFocus(ev)
        }}
        onBlur={() => {
          onBlur()
        }}
        info={showErrors ? internalError ?? info : undefined}
        data-qa={props['data-qa']}
        id={id}
        required={required}
        locale={locale}
        useBrowserPicker={nativeDatePickerEnabled}
        minDate={minDate}
        maxDate={maxDate}
        errorTexts={errorTexts}
        hideErrorsBeforeTouched={hideErrorsBeforeTouched}
        datePickerVisible={showDatePicker}
      />
      {!nativeDatePickerEnabled && showDatePicker ? (
        <DayPickerPositioner ref={pickerRef} openAbove={openAbove}>
          <DayPickerDiv>
            <DatePickerDay
              locale={locale}
              inputValue={internalDate}
              handleDayClick={handleDayClick}
              minDate={minDate}
              maxDate={maxDate}
              isInvalidDate={isInvalidDate && ((date) => !!isInvalidDate(date))}
              initialMonth={initialMonth}
            />
          </DayPickerDiv>
        </DayPickerPositioner>
      ) : null}
    </DatePickerWrapper>
  )
})

export const DatePickerSpacer = React.memo(function DatePickerSpacer() {
  return <DateInputSpacer>–</DateInputSpacer>
})

const DateInputSpacer = styled.div`
  padding: 6px;
`
