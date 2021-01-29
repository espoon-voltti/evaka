import React from 'react'
import styled from 'styled-components'

import { InfoStatus } from '../../atoms/StatusIcon'
import InputField from '../../atoms/form/InputField'

interface Props {
  date: string
  setDate: (date: string) => void
  info?: {
    text: string
    status?: InfoStatus
  }
  onFocus: () => void
  onBlur: (e: React.FocusEvent<HTMLInputElement>) => void
  onKeyPress?: (e: React.KeyboardEvent) => void
}

const DISALLOWED_CHARACTERS = /[^0-9./-]+/g
const SHORT_DATE_FORMAT = /^(\d)\.(\d)\.(\d{4})$/

function DatePickerInput({
  date,
  setDate,
  info,
  onFocus,
  onBlur,
  onKeyPress
}: Props) {
  const ariaId = Math.random().toString(36).substring(2, 15)

  function changeHandler(e: string) {
    // clean up any invalid characters
    let cleaned = e.replace(DISALLOWED_CHARACTERS, '')
    const shortMatch = SHORT_DATE_FORMAT.exec(cleaned)

    if (shortMatch) {
      cleaned = `0${cleaned.slice(0, 1)}.0${cleaned.slice(
        2,
        3
      )}.${cleaned.slice(4, 8)}`
    }

    setDate(cleaned)
  }

  return (
    <>
      <InputField
        placeholder={'pp.kk.vvvv'}
        value={date}
        onChange={changeHandler}
        aria-describedby={ariaId}
        info={info}
        onFocus={onFocus}
        onBlur={onBlur}
        onKeyPress={onKeyPress}
      />
      <DatePickerDescription id={ariaId} />
    </>
  )
}

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
