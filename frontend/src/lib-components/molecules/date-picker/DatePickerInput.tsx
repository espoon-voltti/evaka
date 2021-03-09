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
  id?: string
  required?: boolean
  locale: 'fi' | 'sv' | 'en'
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
  id,
  required,
  locale,
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
        id={id}
        required={required}
      />
      <DatePickerDescription id={ariaId} locale={locale} />
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

function DatePickerDescription({
  id,
  locale
}: {
  id: string
  locale: 'fi' | 'sv' | 'en'
}) {
  // TODO: add translation
  return (
    <>
      {locale === 'en' ? (
        <StyledP lang="en" id={id}>
          Type the date in dd.mm.yyyy format. You can get to month picker with
          the tab key.
        </StyledP>
      ) : locale === 'sv' ? (
        <StyledP lang="sv" id={id}>
          Skriv in datumet i formatet dd.mm.åååå. Du kan komma till
          månadsväljaren med tabbtangenten.
        </StyledP>
      ) : (
        <StyledP lang="fi" id={id}>
          Kirjoita päivämäärä muodossa pp.kk.vvvv kenttään. Tab-näppäimellä
          pääset kuukausivalitsimeen.
        </StyledP>
      )}
    </>
  )
}

export default DatePickerInput
