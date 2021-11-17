// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'
import styled from 'styled-components'
import { autocomplete } from 'lib-common/time'
import InputField, { TextInputProps } from './InputField'

interface Props
  extends Pick<
    TextInputProps,
    | 'placeholder'
    | 'hideErrorsBeforeTouched'
    | 'info'
    | 'id'
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
      onFocus={onFocus}
    />
  )
})

function onFocus(event: React.FocusEvent<HTMLInputElement>) {
  event.target.select()
}

const ShorterInput = styled(InputField)`
  width: calc(3.4em + 24px);
`
