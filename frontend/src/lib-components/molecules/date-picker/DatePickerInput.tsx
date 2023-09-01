// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'
import styled from 'styled-components'

import LocalDate from 'lib-common/local-date'
import { useUniqueId } from 'lib-common/utils/useUniqueId'

import InputField, { InputInfo } from '../../atoms/form/InputField'
import { useTranslations } from '../../i18n'

interface Props {
  value: string
  onChange: (value: string) => void
  info?: InputInfo
  hideErrorsBeforeTouched?: boolean
  disabled?: boolean
  onFocus: (e: React.FocusEvent<HTMLInputElement>) => void
  onBlur: (e: React.FocusEvent<HTMLInputElement>) => void
  'data-qa'?: string
  id?: string
  required?: boolean
  locale: 'fi' | 'sv' | 'en'
  useBrowserPicker?: boolean
}

const DISALLOWED_CHARACTERS = /[^0-9.]+/g

const DateInputField = styled(InputField)`
  &::-webkit-date-and-time-value {
    margin: 0; // remove Android chevron spacing
  }
`

export default React.memo(function DatePickerInput({
  value,
  onChange,
  info,
  hideErrorsBeforeTouched,
  disabled,
  onFocus,
  onBlur,
  id,
  required,
  locale,
  useBrowserPicker = false,
  'data-qa': dataQa
}: Props) {
  const i18n = useTranslations()
  const ariaId = useUniqueId('date-picker-input')

  const handleChange = useCallback(
    (target: EventTarget & HTMLInputElement) => {
      if (useBrowserPicker) {
        onChange(
          target.valueAsDate
            ? LocalDate.fromSystemTzDate(target.valueAsDate).format(
                'dd.MM.yyyy'
              )
            : ''
        )
      } else {
        const value = target.value.replace(DISALLOWED_CHARACTERS, '')
        onChange(value)
      }
    },
    [onChange, useBrowserPicker]
  )

  return (
    <>
      <DateInputField
        placeholder={i18n.datePicker.placeholder}
        value={value}
        onChangeTarget={handleChange}
        onFocus={onFocus}
        onBlur={onBlur}
        aria-describedby={ariaId}
        info={info}
        hideErrorsBeforeTouched={hideErrorsBeforeTouched}
        readonly={disabled}
        data-qa={dataQa}
        id={id}
        required={required}
        width="s"
      />
      <HelpTextForScreenReader lang={locale} id={ariaId}>
        {i18n.datePicker.description}
      </HelpTextForScreenReader>
    </>
  )
})

const HelpTextForScreenReader = styled.p`
  border: 0;
  clip: rect(0 0 0 0);
  height: 1px;
  width: 1px;
  margin: -1px;
  padding: 0;
  overflow: hidden;
  position: absolute;
`
