// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useLayoutEffect, useRef, useState } from 'react'
import styled from 'styled-components'

import { greyscale } from '../../colors'
import { defaultMargins } from '../../white-space'
import DatePickerInput from './DatePickerInput'
import DatePickerDay from './DatePickerDay'
import LocalDate from '@evaka/lib-common/local-date'
import { InputInfo } from '../../atoms/form/InputField'
import { DayModifiers } from 'react-day-picker'

const inputWidth = 120
const DatePickerWrapper = styled.div`
  position: relative;
  display: inline-block;
  width: ${inputWidth}px;
`
const overflow = 70
const DayPickerPositioner = styled.div<{ show: boolean }>`
  position: absolute;
  top: calc(100% + 15px);
  left: -${overflow}px;
  right: -${overflow}px;
  z-index: 99999;
  justify-content: center;
  align-items: center;
  display: ${(p) => (p.show ? 'inline-block' : 'none')};
`

const DayPickerDiv = styled.div`
  background-color: ${greyscale.white};
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
  locale: 'fi' | 'sv' | 'en'
  info?: InputInfo
  hideErrorsBeforeTouched?: boolean
  disabled?: boolean
  isValidDate?: (date: LocalDate) => boolean
  'data-qa'?: string
  id?: string
  required?: boolean
}

function DatePicker({
  date,
  onChange,
  locale,
  info,
  hideErrorsBeforeTouched,
  disabled,
  isValidDate,
  id,
  required,
  ...props
}: DatePickerProps) {
  const [show, setShow] = useState<boolean>(false)
  const wrapperRef = useRef<HTMLDivElement>(null)
  const pickerRef = useRef<HTMLDivElement>(null)

  function handleUserKeyPress(e: React.KeyboardEvent) {
    if (e.key === 'Esc' || e.key === 'Escape') setShow(false)
  }

  function handleDayClick(day: Date, modifiers?: DayModifiers) {
    if (modifiers?.disabled) {
      return
    }
    setShow(false)
    onChange(LocalDate.fromSystemTzDate(day).format())
  }

  function onInputBlur(e: React.FocusEvent<HTMLInputElement>) {
    if (e.relatedTarget === null) {
      setShow(false)
    }

    if (e.relatedTarget instanceof Element) {
      if (
        wrapperRef.current === null ||
        !wrapperRef.current?.contains(e.relatedTarget)
      )
        setShow(false)
    }
  }

  useLayoutEffect(() => {
    const realignPicker = () => {
      if (wrapperRef.current) {
        const minMargin = 16

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
  }, [])

  return (
    <DatePickerWrapper ref={wrapperRef} onKeyDown={handleUserKeyPress}>
      <DatePickerInput
        date={date}
        setDate={(date) => {
          if (LocalDate.parseFiOrNull(date) !== null) {
            setShow(false)
          }
          onChange(date)
        }}
        disabled={disabled}
        onFocus={() => setShow(true)}
        onBlur={onInputBlur}
        info={info}
        hideErrorsBeforeTouched={hideErrorsBeforeTouched}
        data-qa={props['data-qa']}
        id={id}
        required={required}
        locale={locale}
      />
      <DayPickerPositioner ref={pickerRef} show={show}>
        <DayPickerDiv>
          <DatePickerDay
            locale={locale}
            inputValue={date}
            handleDayClick={handleDayClick}
            isValidDate={isValidDate}
          />
        </DayPickerDiv>
      </DayPickerPositioner>
    </DatePickerWrapper>
  )
}

export default DatePicker
