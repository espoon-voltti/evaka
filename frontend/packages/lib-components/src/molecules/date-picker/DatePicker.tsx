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

const DatePickerWrapper = styled.div`
  position: relative;
  display: inline-block;
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
`

const DayPickerDiv = styled.div`
  background-color: ${greyscale.white};
  padding: ${defaultMargins.s} 0;
  border-radius: 2px;
  box-shadow: 0px 0px 4px rgba(0, 0, 0, 0.25);
  display: flex;
  justify-content: center;

  p:not(:last-child) {
    margin-bottom: 8px;
  }
`

type TooltipProps = {
  date: string
  onChange: (date: string) => void
}

function DatePicker({ date, onChange }: TooltipProps) {
  const [show, setShow] = useState<boolean>(false)
  const ref = useRef<HTMLDivElement>(null)

  function handleUserKeyPress(e: React.KeyboardEvent) {
    if (e.key === 'Esc' || e.key === 'Escape') setShow(false)
  }

  function handleDayClick(day: Date) {
    setShow(false)
    onChange(LocalDate.fromSystemTzDate(day).format())
  }

  function onInputBlur(e: React.FocusEvent<HTMLInputElement>) {
    if (e.relatedTarget instanceof Element) {
      if (ref.current === null || !ref.current?.contains(e.relatedTarget))
        setShow(false)
    }

    if (e.relatedTarget === null) {
      setShow(false)
    }
  }

  return (
    <DatePickerWrapper ref={ref} onKeyDown={handleUserKeyPress}>
      <DatePickerInput
        date={date}
        setDate={onChange}
        onFocus={() => setShow(true)}
        onBlur={onInputBlur}
      />
      <DayPickerPositioner show={show}>
        <DayPickerDiv>
          <DatePickerDay inputValue={date} handleDayClick={handleDayClick} />
        </DayPickerDiv>
      </DayPickerPositioner>
    </DatePickerWrapper>
  )
}

export default DatePicker
