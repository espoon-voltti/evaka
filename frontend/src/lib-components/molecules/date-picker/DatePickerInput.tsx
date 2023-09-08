// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo } from 'react'
import styled from 'styled-components'

import LocalDate from 'lib-common/local-date'
import { useUniqueId } from 'lib-common/utils/useUniqueId'

import InputField, { InputInfo } from '../../atoms/form/InputField'
import { useTranslations } from '../../i18n'

export interface Props {
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
  const i18n = useTranslations()
  const { locale, useBrowserPicker, ...rest } = props
  const ariaId = useUniqueId('date-picker-input')

  return (
    <>
      {useBrowserPicker ? (
        <DateInputNative {...rest} locale={locale} ariaId={ariaId} />
      ) : (
        <DateInputText {...rest} locale={locale} ariaId={ariaId} />
      )}
      <HelpTextForScreenReader lang={locale} id={ariaId}>
        {i18n.datePicker.description}
      </HelpTextForScreenReader>
    </>
  )
})

interface InternalProps extends Omit<Props, 'useBrowserPicker'> {
  ariaId: string
}

const DateInputText = React.memo(function DateInputText({
  value,
  onChange,
  onFocus,
  onBlur,
  info,
  hideErrorsBeforeTouched,
  disabled,
  'data-qa': dataQa,
  id,
  required,
  ariaId
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
  )
})

const DateInputNative = React.memo(function DateInputNative({
  value,
  onChange,
  onFocus,
  onBlur,
  info,
  hideErrorsBeforeTouched,
  disabled,
  'data-qa': dataQa,
  id,
  required,
  minDate,
  maxDate,
  ariaId
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
      type="date"
      min={minDate?.formatIso()}
      max={maxDate?.formatIso()}
    />
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
