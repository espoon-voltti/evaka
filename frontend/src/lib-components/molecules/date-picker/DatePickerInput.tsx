// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect } from 'react'
import styled from 'styled-components'

import LocalDate from 'lib-common/local-date'
import { usePendingUserInput } from 'lib-common/utils/usePendingUserInput'
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
  onInputStatus?: (hasText: boolean) => void
  errorTexts: {
    validDate: string
  }
  datePickerVisible: boolean
}

const DISALLOWED_CHARACTERS = /[^0-9./-]+/g

const transformDate = (d: string) => LocalDate.parseFiOrNull(d)

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
  onInputStatus,
  errorTexts,
  datePickerVisible,
  ...props
}: Props) {
  const ariaId = useUniqueId('date-picker-input')

  const [rawDate, setRawDate, parsedDate] = usePendingUserInput(transformDate)

  useEffect(() => {
    if (date) {
      setRawDate(date.format())
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [date])

  function changeHandler(target: EventTarget & HTMLInputElement) {
    if (useBrowserPicker) {
      setDate(
        target.valueAsDate && LocalDate.fromSystemTzDate(target.valueAsDate)
      )
    } else {
      setRawDate(target.value.replace(DISALLOWED_CHARACTERS, ''))
    }
  }

  useEffect(() => {
    setDate(parsedDate ?? null)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [parsedDate])

  useEffect(() => {
    onInputStatus?.(rawDate.length > 0)
  }, [rawDate, onInputStatus])

  const dateProps = useBrowserPicker
    ? {
        type: 'date' as const,
        min: minDate?.formatIso(),
        max: maxDate?.formatIso()
      }
    : {}

  return (
    <>
      <DateInputField
        placeholder="pp.kk.vvvv"
        value={useBrowserPicker ? date?.formatIso() ?? '' : rawDate}
        onChangeTarget={changeHandler}
        aria-describedby={ariaId}
        info={
          !datePickerVisible && !parsedDate
            ? {
                status: 'warning',
                text: errorTexts.validDate
              }
            : info
        }
        hideErrorsBeforeTouched={hideErrorsBeforeTouched}
        readonly={disabled}
        onFocus={onFocus}
        onBlur={onBlur}
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
