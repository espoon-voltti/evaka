// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import classNames from 'classnames'
import { ControlFieldProps, ControlField } from './control'

export interface InputProps {
  borderless?: boolean
  dataQa?: string
  disabled?: boolean
  id?: string
  max?: number
  min?: number
  step?: number
  name?: string
  onBlur?: (e: React.FocusEvent<HTMLInputElement>) => void
  onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void
  onFocus?: (e: React.FocusEvent<HTMLInputElement>) => void
  placeholder?: string
  readOnly?: boolean
  size?: number
  type?: string
  value?: string
  maxLength?: number
}

export const Input: React.FunctionComponent<InputProps> = ({
  borderless = false,
  dataQa,
  disabled = false,
  id,
  min,
  max,
  step,
  name,
  onBlur,
  onChange,
  onFocus,
  placeholder,
  readOnly = false,
  size,
  type = 'text',
  value,
  maxLength
}) => (
  <input
    className={classNames('input', {
      'is-borderless': borderless
    })}
    data-qa={dataQa}
    disabled={disabled}
    id={id}
    max={max}
    min={min}
    step={step}
    name={name}
    onBlur={onBlur}
    onChange={onChange}
    onFocus={onFocus}
    placeholder={placeholder}
    readOnly={readOnly}
    size={size}
    type={type}
    value={value}
    maxLength={maxLength}
  />
)

export const InputField: React.FunctionComponent<
  InputProps & ControlFieldProps
> = ({
  className,
  dataQa,
  label,
  message,
  name,
  onBlur,
  onChange,
  placeholder,
  required,
  value,
  type,
  state,
  disabled,
  step,
  maxLength
}) => (
  <ControlField
    className={className}
    dataQa={dataQa}
    label={label}
    message={message}
    name={name}
    required={required}
    state={state}
    disabled={disabled}
  >
    <Input
      name={name}
      onBlur={onBlur}
      onChange={onChange}
      placeholder={placeholder}
      type={type}
      value={value}
      disabled={disabled}
      step={step}
      maxLength={maxLength}
    />
  </ControlField>
)
