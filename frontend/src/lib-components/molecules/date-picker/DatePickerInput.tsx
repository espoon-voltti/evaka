// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import LocalDate from 'lib-common/local-date'
import { useUniqueId } from 'lib-common/utils/useUniqueId'

import InputField, { InputInfo } from '../../atoms/form/InputField'
import { useTranslations } from '../../i18n'

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
  datePickerVisible,
  ...props
}: Props) {
  const i18n = useTranslations()
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

  return (
    <>
      <DateInputField
        placeholder={i18n.datePicker.placeholder}
        value={useBrowserPicker ? date?.formatIso() ?? '' : inputValue}
        onChangeTarget={handleChange}
        aria-describedby={ariaId}
        info={
          !datePickerVisible && !isValid && inputValue !== ''
            ? {
                status: 'warning',
                text: i18n.datePicker.validationErrors.validDate
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
      <StyledP lang={locale} id={ariaId}>
        {i18n.datePicker.description}
      </StyledP>
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
