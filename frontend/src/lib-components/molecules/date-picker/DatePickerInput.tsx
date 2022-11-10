// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import LocalDate from 'lib-common/local-date'
import { useUniqueId } from 'lib-common/utils/useUniqueId'

import InputField, { InputInfo } from '../../atoms/form/InputField'

interface Props {
  date: LocalDate | null
  setDate: (date: LocalDate | null) => void
  info?: InputInfo
  hideErrorsBeforeTouched?: boolean
  disabled?: boolean
  onFocus: (e: React.FocusEvent<HTMLInputElement>) => void
  onBlur: (e: React.FocusEvent<HTMLInputElement>) => void
  onKeyPress?: (e: React.KeyboardEvent) => void
  'data-qa'?: string
  id?: string
  required?: boolean
  locale: 'fi' | 'sv' | 'en'
  useBrowserPicker?: boolean
  minDate?: LocalDate
  maxDate?: LocalDate
  errorTexts: {
    validDate: string
  }
  datePickerVisible: boolean
}

const DISALLOWED_CHARACTERS = /[^0-9./-]+/g

const DateInputField = styled(InputField)`
  &::-webkit-date-and-time-value {
    margin: 0; // remove Android chevron spacing
  }
`

export default React.memo(function DatePickerInput({
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
  useBrowserPicker = false,
  minDate,
  maxDate,
  errorTexts,
  datePickerVisible,
  ...props
}: Props) {
  const ariaId = useUniqueId('date-picker-input')
  const formattedDate = useMemo(() => (date ? date.format() : ''), [date])

  const [inputValue, setInputValue] = useState(formattedDate)
  const [hasFocus, setHasFocus] = useState(false)

  if (!hasFocus && formattedDate !== inputValue) {
    // When the input has no focus, keep it in sync with the date prop
    setInputValue(formattedDate)
  }
  const isValid = useMemo(
    () => LocalDate.parseFiOrNull(inputValue) !== null,
    [inputValue]
  )

  const handleFocus = useCallback(
    (e: React.FocusEvent<HTMLInputElement>) => {
      setHasFocus(true)
      onFocus(e)
    },
    [onFocus]
  )

  const handleBlur = useCallback(
    (e: React.FocusEvent<HTMLInputElement>) => {
      setHasFocus(false)
      onBlur(e)
    },
    [onBlur]
  )

  const handleChange = useCallback(
    (target: EventTarget & HTMLInputElement) => {
      if (useBrowserPicker) {
        setDate(
          target.valueAsDate
            ? LocalDate.fromSystemTzDate(target.valueAsDate)
            : null
        )
      } else {
        const value = target.value.replace(DISALLOWED_CHARACTERS, '')
        setInputValue(value)
        setDate(LocalDate.parseFiOrNull(value))
      }
    },
    [setDate, useBrowserPicker]
  )

  const dateProps = useBrowserPicker
    ? {
        type: 'date' as const,
        min: minDate?.formatIso(),
        max: maxDate?.formatIso()
      }
    : {}

  const placeholder =
    locale === 'en'
      ? 'dd.mm.yyyy'
      : locale === 'sv'
      ? 'dd.mm.åååå'
      : 'pp.kk.vvvv'

  return (
    <>
      <DateInputField
        placeholder={placeholder}
        value={useBrowserPicker ? date?.formatIso() ?? '' : inputValue}
        onChangeTarget={handleChange}
        aria-describedby={ariaId}
        info={
          !datePickerVisible && !isValid && inputValue !== ''
            ? {
                status: 'warning',
                text: errorTexts.validDate
              }
            : info
        }
        hideErrorsBeforeTouched={hideErrorsBeforeTouched}
        readonly={disabled}
        onFocus={handleFocus}
        onBlur={handleBlur}
        onKeyPress={onKeyPress}
        data-qa={props['data-qa']}
        id={id}
        required={required}
        width="s"
        {...dateProps}
      />
      <DatePickerDescription id={ariaId} locale={locale} />
    </>
  )
})

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

const DatePickerDescription = React.memo(function DatePickerDescription({
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
})
