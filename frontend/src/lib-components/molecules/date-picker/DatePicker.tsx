// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { DayModifiers } from 'react-day-picker'
import styled from 'styled-components'

import LocalDate from 'lib-common/local-date'

import { InputInfo } from '../../atoms/form/InputField'
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

type DatePickerProps = {
  date: string
  onChange: (date: string) => void
  onFocus?: () => void
  onBlur?: () => void
  locale: 'fi' | 'sv' | 'en'
  info?: InputInfo
  hideErrorsBeforeTouched?: boolean
  disabled?: boolean
  isValidDate?: (date: LocalDate) => boolean
  'data-qa'?: string
  id?: string
  required?: boolean
  initialMonth?: LocalDate
  openAbove?: boolean
}

export default React.memo(function DatePicker({
  date,
  onChange,
  onFocus = () => undefined,
  onBlur = () => undefined,
  locale,
  info,
  hideErrorsBeforeTouched,
  disabled,
  isValidDate,
  id,
  required,
  initialMonth,
  openAbove,
  ...props
}: DatePickerProps) {
  const [show, setShow] = useState<boolean>(false)
  const [showErrors, setShowErrors] = useState(!hideErrorsBeforeTouched)
  const wrapperRef = useRef<HTMLDivElement>(null)
  const pickerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!hideErrorsBeforeTouched) {
      setShowErrors(true)
    }
  }, [hideErrorsBeforeTouched])

  function hideDatePicker() {
    setShow(false)
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
    onChange(LocalDate.fromSystemTzDate(day).format())
  }

  useLayoutEffect(() => {
    if (show) {
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
  }, [show])

  useEffect(() => {
    function handleEvent(event: { target: EventTarget | null }) {
      if (event.target instanceof Element) {
        if (wrapperRef.current?.contains(event.target)) {
          return
        }
      }

      hideDatePicker()
    }

    if (show) {
      addEventListener('focusin', handleEvent)
      addEventListener('pointerup', handleEvent)

      return () => {
        removeEventListener('focusin', handleEvent)
        removeEventListener('pointerup', handleEvent)
      }
    }

    return () => undefined
  }, [show])

  return (
    <DatePickerWrapper ref={wrapperRef} onKeyDown={handleUserKeyPress}>
      <DatePickerInput
        date={date}
        setDate={(date) => {
          if (LocalDate.parseFiOrNull(date) !== null) {
            hideDatePicker()
          }
          onChange(date)
        }}
        disabled={disabled}
        onFocus={() => {
          setShow(true)
          onFocus()
        }}
        onBlur={() => {
          onBlur()
        }}
        info={showErrors ? info : undefined}
        data-qa={props['data-qa']}
        id={id}
        required={required}
        locale={locale}
      />
      {show ? (
        <DayPickerPositioner ref={pickerRef} openAbove={openAbove}>
          <DayPickerDiv>
            <DatePickerDay
              locale={locale}
              inputValue={date}
              handleDayClick={handleDayClick}
              isValidDate={isValidDate}
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
