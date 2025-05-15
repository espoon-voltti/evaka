// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo } from 'react'
import styled from 'styled-components'

import LocalDate from 'lib-common/local-date'

import InputField, { InputInfo } from '../../atoms/form/InputField'
import { useTranslations } from '../../i18n'

export interface Props {
  value: string
  onChange: (value: string) => void
  info?: InputInfo
  hideErrorsBeforeTouched?: boolean
  disabled?: boolean
  onBlur: ((e: React.FocusEvent<HTMLInputElement>) => void) | undefined
  'data-qa'?: string
  id?: string
  required?: boolean
  locale: 'fi' | 'sv' | 'en'
  useBrowserPicker: boolean
  minDate: LocalDate | undefined
  maxDate: LocalDate | undefined
}

const DISALLOWED_CHARACTERS = /[^0-9.]+/g

const DateInputField = styled(InputField)`
  &::-webkit-date-and-time-value {
    margin: 0; // remove Android chevron spacing
  }
`

export default React.memo(function DatePickerInput(props: Props) {
  const { locale, useBrowserPicker, ...rest } = props

  return (
    <>
      {useBrowserPicker ? (
        <DateInputNative locale={locale} {...rest} />
      ) : (
        <DateInputText locale={locale} {...rest} />
      )}
    </>
  )
})

type InternalProps = Omit<Props, 'useBrowserPicker'>

const DateInputText = React.memo(function DateInputText({
  value,
  onChange,
  onBlur,
  info,
  hideErrorsBeforeTouched,
  disabled,
  'data-qa': dataQa,
  id,
  required
}: InternalProps) {
  const i18n = useTranslations()

  const handleChange = useCallback(
    (newValue: string) => {
      onChange(newValue.replace(DISALLOWED_CHARACTERS, ''))
    },
    [onChange]
  )

  return (
    <DateInputField
      placeholder={i18n.datePicker.placeholder}
      value={value}
      onChange={handleChange}
      onBlur={onBlur}
      aria-label={i18n.datePicker.description}
      info={info}
      hideErrorsBeforeTouched={hideErrorsBeforeTouched}
      readonly={disabled}
      data-qa={dataQa}
      id={id}
      required={required}
      width="s"
    />
  )
})

const DateInputNative = React.memo(function DateInputNative({
  value,
  onChange,
  onBlur,
  info,
  hideErrorsBeforeTouched,
  disabled,
  'data-qa': dataQa,
  id,
  required,
  minDate,
  maxDate
}: InternalProps) {
  const i18n = useTranslations()

  const valueAsIsoDate = useMemo(() => {
    const date = LocalDate.parseFiOrNull(value)
    return date ? date.formatIso() : value
  }, [value])

  const handleChange = useCallback(
    (target: EventTarget & HTMLInputElement) => {
      onChange(
        target.valueAsDate
          ? LocalDate.fromSystemTzDate(target.valueAsDate).format()
          : target.value
      )
    },
    [onChange]
  )

  return (
    <DateInputField
      placeholder={i18n.datePicker.placeholder}
      value={valueAsIsoDate}
      onChangeTarget={handleChange}
      onBlur={onBlur}
      aria-label={i18n.datePicker.description}
      info={info}
      hideErrorsBeforeTouched={hideErrorsBeforeTouched}
      readonly={disabled}
      data-qa={dataQa}
      id={id}
      required={required}
      width="s"
      type="date"
      min={minDate?.formatIso()}
      max={maxDate?.formatIso()}
    />
  )
})
