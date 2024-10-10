// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'
import styled from 'styled-components'

import { BoundFormState } from 'lib-common/form/hooks'
import { autocomplete } from 'lib-common/time'

import InputField, { TextInputProps } from './InputField'

export interface TimeInputProps
  extends Pick<
    TextInputProps,
    | 'placeholder'
    | 'hideErrorsBeforeTouched'
    | 'onBlur'
    | 'info'
    | 'id'
    | 'name'
    | 'required'
    | 'readonly'
    | 'inputRef'
    | 'aria-describedby'
    | 'aria-description'
    | 'data-qa'
  > {
  size?: 'wide' | 'normal' | 'narrow'
  value: string
  onChange: (v: string) => void
  onFocus?: (event: React.FocusEvent<HTMLInputElement>) => void
}

const TimeInput = React.memo(function TimeInput({
  value,
  onChange,
  onFocus,
  size,
  ...props
}: TimeInputProps) {
  const onChangeWithAutocomplete = useCallback(
    (v: string) => onChange(autocomplete(value, v)),
    [value, onChange]
  )

  const InputComponent = size
    ? size === 'narrow'
      ? MinimalInput
      : size === 'wide'
        ? ScalingInput
        : size === 'normal'
          ? ShorterInput
          : ShorterInput
    : ShorterInput

  return (
    <InputComponent
      {...props}
      value={value}
      onChange={onChangeWithAutocomplete}
      type="text"
      inputMode="numeric"
      onFocus={(event) => {
        event.target.select()
        onFocus?.(event)
      }}
    />
  )
})

export default TimeInput

const MinimalInput = styled(InputField)`
  width: calc(4em);
  max-width: calc(4em);

  input {
    font-size: 1em;
  }
`

const ShorterInput = styled(InputField)`
  width: calc(3.2em + 24px);
  max-width: calc(3.2em + 24px);

  input {
    font-size: 1em;
  }
`

const ScalingInput = styled(InputField)`
  div.warning {
    line-height: 1.125rem;
  }

  input {
    font-size: 1em;
    min-width: 80px;
  }
`

export interface TimeInputFProps
  extends Omit<TimeInputProps, 'value' | 'onChange'> {
  bind: BoundFormState<string>
}

export const TimeInputF = React.memo(function TimeInputF({
  bind,
  ...props
}: TimeInputFProps) {
  const { state, set, inputInfo } = bind
  return (
    <TimeInput
      {...props}
      value={state}
      onChange={set}
      info={'info' in props ? props.info : inputInfo()}
    />
  )
})
