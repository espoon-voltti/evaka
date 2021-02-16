// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useRef, useState } from 'react'
import styled from 'styled-components'

import { greyscale } from '../../colors'
import { defaultMargins } from '../../white-space'
import DatePickerInput from './DatePickerInput'
import DatePickerDay from './DatePickerDay'
import LocalDate from '~../../lib-common/src/local-date'
import { InputInfo } from '../../atoms/form/InputField'
import { tabletMin } from '../../breakpoints'
import { Modifiers } from 'react-day-picker'

const DatePickerWrapper = styled.div`
  position: relative;
  display: inline-block;
  width: 120px;
`
const DayPickerPositioner = styled.div<{ show: boolean }>`
  position: absolute;
  top: calc(100% + 15px);
  left: -70px;
  right: -70px;
  z-index: 99999;
  justify-content: center;
  align-items: center;
  display: ${(p) => (p.show ? 'inline-block' : 'none')};

  @media (max-width: ${tabletMin}) {
    width: ${`calc(100vw - ${2 * parseInt(defaultMargins.L)}px)`};
    left: 0;
  }
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
  const ref = useRef<HTMLDivElement>(null)

  function handleUserKeyPress(e: React.KeyboardEvent) {
    if (e.key === 'Esc' || e.key === 'Escape') setShow(false)
  }

  function handleDayClick(day: Date, modifiers?: Modifiers) {
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
      if (ref.current === null || !ref.current?.contains(e.relatedTarget))
        setShow(false)
    }
  }

  return (
    <DatePickerWrapper ref={ref} onKeyDown={handleUserKeyPress}>
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
      <DayPickerPositioner show={show}>
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
