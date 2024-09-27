// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useLayoutEffect, useRef } from 'react'
import { Modifiers } from 'react-day-picker'
import styled from 'styled-components'

import { useBoolean } from 'lib-common/form/hooks'
import LocalDate from 'lib-common/local-date'

import { InputInfo } from '../../atoms/form/InputField'
import { fontWeights } from '../../typography'
import { defaultMargins } from '../../white-space'

import DatePickerDay from './DatePickerDay'
import DatePickerInput from './DatePickerInput'
import { nativeDatePickerEnabled } from './helpers'

const inputWidth = 120

const DatePickerWrapper = styled.div`
  position: relative;
  display: inline-block;
  width: ${inputWidth}px;
`
const minMargin = 16
const overflow = 100

const DayPickerPositioner = styled.div<{
  openAbove?: boolean
}>`
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

  .rdp-root {
    --rdp-day-width: 40px;
    --rdp-day-height: 40px;
    --rdp-weekday-padding: 0;
    --rdp-selected-font: inherit;
    --rdp-selected-border: none;
  }

  .rdp-month_caption {
    font-family: inherit;
    font-size: 18px;
    font-weight: ${fontWeights.medium};
  }

  .rdp-chevron {
    fill: #0f0f0f;
  }

  .rdp-caption_label {
    padding: 0 0.5em;
  }

  .rdp-day {
    padding: 0;
  }

  .rdp-weekday {
    text-transform: none;
    font-size: 1em;
    font-weight: ${fontWeights.normal};
  }

  .rdp-today {
    color: ${(p) => p.theme.colors.accents.a2orangeDark};
    font-weight: ${fontWeights.bold};
  }

  .rdp-day_button:not([disabled]) {
    &:hover {
      color: ${(p) => p.theme.colors.grayscale.g100};
      background-color: ${(p) => p.theme.colors.accents.a8lightBlue};
    }

    &:active,
    &:focus {
      color: ${(p) => p.theme.colors.grayscale.g100};
      background-color: ${(p) => p.theme.colors.grayscale.g0};
      border: 2px solid ${(p) => p.theme.colors.main.m2Active};
    }
  }

  .rdp-selected:not([disabled]) .rdp-day_button {
    color: ${(p) => p.theme.colors.grayscale.g0};
    background-color: ${(p) => p.theme.colors.main.m2Active};
  }
`

export interface DatePickerLowLevelProps {
  value: string
  onChange: (value: string) => void
  onFocus?: (e: React.FocusEvent<HTMLInputElement>) => void
  onBlur?: (e: React.FocusEvent<HTMLInputElement>) => void
  locale: 'fi' | 'sv' | 'en'
  info?: InputInfo
  hideErrorsBeforeTouched?: boolean
  disabled?: boolean
  'data-qa'?: string
  id?: string
  required?: boolean
  openAbove?: boolean
  initialMonth?: LocalDate
  minDate?: LocalDate
  maxDate?: LocalDate
  useBrowserPicker?: boolean
}

export default React.memo(function DatePickerLowLevel({
  value,
  onChange,
  onFocus,
  onBlur,
  locale,
  info,
  hideErrorsBeforeTouched,
  disabled,
  id,
  required,
  openAbove,
  initialMonth,
  minDate,
  maxDate,
  useBrowserPicker = nativeDatePickerEnabled,
  'data-qa': dataQa
}: DatePickerLowLevelProps) {
  const [showDatePicker, useShowDatePicker] = useBoolean(false)
  const wrapperRef = useRef<HTMLDivElement>(null)
  const pickerRef = useRef<HTMLDivElement>(null)

  const showDatePickerOn = useShowDatePicker.on
  const showDatePickerOff = useShowDatePicker.off

  const handleUserKeyPress = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === 'Esc' || e.key === 'Escape' || e.key === 'Enter') {
        e.stopPropagation()
        showDatePickerOff()
      }
    },
    [showDatePickerOff]
  )

  const handleDayClick = useCallback(
    (day: Date, modifiers?: Modifiers) => {
      if (modifiers?.disabled) {
        return
      }
      showDatePickerOff()
      onChange(LocalDate.fromSystemTzDate(day).format())
    },
    [onChange, showDatePickerOff]
  )

  const handleFocus = useCallback(
    (e: React.FocusEvent<HTMLInputElement>) => {
      showDatePickerOn()
      onFocus?.(e)
    },
    [onFocus, showDatePickerOn]
  )

  const handleBlur = useCallback(
    (e: React.FocusEvent<HTMLInputElement>) => {
      const date = LocalDate.parseFiOrNull(e.target.value)
      if (date !== null) {
        onChange(date.format())
      }
      onBlur?.(e)
    },
    [onBlur, onChange]
  )

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
            pickerRef.current.style.left = `${left}px`
            const right = -overflow - leftOffset + rightOffset
            pickerRef.current.style.right = `${right}px`
          }
        }
      }
      realignPicker()
      addEventListener('resize', realignPicker, { passive: true })
      return () => removeEventListener('resize', realignPicker)
    }
    return undefined
  }, [showDatePicker])

  useEffect(() => {
    function handleEvent(event: { target: EventTarget | null; type: string }) {
      if (event.target instanceof Element) {
        if (wrapperRef.current?.contains(event.target)) {
          return
        }
        if (
          event.type === 'pointerup' &&
          event.target.classList.contains('modal-container')
        ) {
          return // do not close when clicking on modal scrollbar
        }
      }

      showDatePickerOff()
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
  }, [showDatePickerOff, showDatePicker])

  return (
    <DatePickerWrapper ref={wrapperRef} onKeyDown={handleUserKeyPress}>
      <DatePickerInput
        value={value}
        onChange={onChange}
        onFocus={handleFocus}
        onBlur={!showDatePicker ? handleBlur : onBlur}
        disabled={disabled}
        info={showDatePicker ? undefined : info}
        data-qa={dataQa}
        id={id}
        required={required}
        locale={locale}
        useBrowserPicker={useBrowserPicker}
        hideErrorsBeforeTouched={hideErrorsBeforeTouched}
        minDate={minDate}
        maxDate={maxDate}
      />
      {!nativeDatePickerEnabled && showDatePicker ? (
        <DayPickerPositioner ref={pickerRef} openAbove={openAbove}>
          <DayPickerDiv>
            <DatePickerDay
              locale={locale}
              inputValue={value}
              initialMonth={initialMonth}
              handleDayClick={handleDayClick}
              minDate={minDate}
              maxDate={maxDate}
            />
          </DayPickerDiv>
        </DayPickerPositioner>
      ) : null}
    </DatePickerWrapper>
  )
})
