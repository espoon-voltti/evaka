// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'
import styled from 'styled-components'
import InputField, { TextInputProps } from './InputField'

interface Props
  extends Pick<
    TextInputProps,
    | 'placeholder'
    | 'hideErrorsBeforeTouched'
    | 'info'
    | 'name'
    | 'required'
    | 'inputRef'
    | 'aria-describedby'
    | 'data-qa'
  > {
  value: string
  onChange: (v: string) => void
}

export default React.memo(function TimeInput({
  value,
  onChange,
  ...props
}: Props) {
  const onChangeWithAutocomplete = useCallback(
    (v: string) => onChange(autocomplete(value, v)),
    [value, onChange]
  )

  return (
    <ShorterInput
      {...props}
      value={value}
      onChange={onChangeWithAutocomplete}
      type="text"
      inputMode="numeric"
      width="s"
      onFocus={onFocus}
    />
  )
})

function onFocus(event: React.FocusEvent<HTMLInputElement>) {
  event.target.select()
}

function autocomplete(previous: string, current: string) {
  const filtered = current
    .replace('.', ':')
    .replace(disallowedInput, '')
    .slice(0, 5)

  if (filtered.length <= previous.length) {
    return filtered
  }

  if (singleDigitHour.test(filtered)) {
    return `0${filtered}:`
  }

  if (doubleDigitHour.test(filtered)) {
    return `${filtered}:`
  }

  if (/^([0-9]{2})$/.test(filtered)) {
    const [hour, minutes] = filtered
    return `0${hour}:${minutes}`
  }

  const partBeforeLastChar = filtered.substring(0, filtered.length - 1)
  const lastChar = filtered[filtered.length - 1]

  if (/^([0-9]{2})$/.test(partBeforeLastChar) && /[0-9]/.test(lastChar)) {
    return `${partBeforeLastChar}:${lastChar}`
  }

  if (lastChar === ':') {
    if (/^([0-9])$/.test(partBeforeLastChar)) {
      return `0${partBeforeLastChar}:`
    }

    if (/^([0-9]{2})$/.test(partBeforeLastChar)) {
      return filtered
    }

    return partBeforeLastChar
  }

  return filtered
}

const disallowedInput = /[^0-9\\:]/g
const singleDigitHour = /^([3-9])$/
const doubleDigitHour = /^(([0-1][0-9])|(2[0-3]))$/

const ShorterInput = styled(InputField)`
  width: 80px;
`
