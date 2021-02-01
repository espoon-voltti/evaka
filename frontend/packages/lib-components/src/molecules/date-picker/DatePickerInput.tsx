// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import InputField, { InputInfo } from '../../atoms/form/InputField'

interface Props {
  date: string
  setDate: (date: string) => void
  info?: InputInfo
  hideErrorsBeforeTouched?: boolean
  disabled?: boolean
  onFocus: () => void
  onBlur: (e: React.FocusEvent<HTMLInputElement>) => void
  onKeyPress?: (e: React.KeyboardEvent) => void
  'data-qa'?: string
}

const DISALLOWED_CHARACTERS = /[^0-9./-]+/g
const DATE_FORMAT = /^(\d){1,2}\.(\d){1,2}\.(\d{4})$/

function DatePickerInput({
  date,
  setDate,
  info,
  hideErrorsBeforeTouched,
  disabled,
  onFocus,
  onBlur,
  onKeyPress,
  ...props
}: Props) {
  const ariaId = Math.random().toString(36).substring(2, 15)

  function changeHandler(e: string) {
    // clean up any invalid characters
    const cleaned = e.replace(DISALLOWED_CHARACTERS, '')

    // convert d.M.yyyy to dd.MM.yyyy
    if (DATE_FORMAT.test(cleaned)) {
      const parts = cleaned.split('.')
      const d = parts[0].length < 2 ? `0${parts[0]}` : parts[0]
      const m = parts[1].length < 2 ? `0${parts[1]}` : parts[1]
      const y = parts[2]
      setDate(`${d}.${m}.${y}`)
    } else {
      setDate(cleaned)
    }
  }

  return (
    <Wrapper>
      <InputField
        placeholder={'pp.kk.vvvv'}
        value={date}
        onChange={changeHandler}
        aria-describedby={ariaId}
        info={info}
        hideErrorsBeforeTouched={hideErrorsBeforeTouched}
        readonly={disabled}
        onFocus={onFocus}
        onBlur={onBlur}
        onKeyPress={onKeyPress}
        data-qa={props['data-qa']}
      />
      <DatePickerDescription id={ariaId} />
    </Wrapper>
  )
}

const Wrapper = styled.div`
  width: 120px;
`

const StyledP = styled.p`
  border: 0;
  clip: rect(0 0 0 0);
  height: 1px;
  width: 1px;
  margin: -1px;
  padding: 0;
  overflow: hidden;
  position: absolute;
`

function DatePickerDescription({ id }: { id: string }) {
  // TODO: add translation
  return (
    <StyledP lang="fi" id={id}>
      Kirjoita päivämäärä muodossa pp.kk.vvvv kenttään. Tab-näppäimellä pääset
      kuukausivalitsimeen.
    </StyledP>
  )
}

export default DatePickerInput
