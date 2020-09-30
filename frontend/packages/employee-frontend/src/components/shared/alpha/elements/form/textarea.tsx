// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import TextareaAutosize from 'react-autosize-textarea'
import { ControlFieldProps, ControlField } from './control'

export interface TextAreaProps {
  disabled?: boolean
  id?: string
  name?: string
  onBlur?: (e: React.FocusEvent<HTMLTextAreaElement>) => void
  onChange?: (e: React.ChangeEvent<HTMLTextAreaElement>) => void
  placeholder?: string
  value?: string
  rows?: number
  maxLength?: number
  autoFocus?: boolean
}

export const TextArea: React.FunctionComponent<TextAreaProps> = ({
  disabled = false,
  id,
  name,
  onBlur,
  onChange,
  placeholder,
  value,
  rows,
  maxLength,
  autoFocus
}) => (
  <TextareaAutosize
    className={'textarea'}
    id={id}
    name={name}
    onBlur={onBlur}
    onChange={onChange}
    placeholder={placeholder}
    value={value}
    disabled={disabled}
    rows={rows}
    maxLength={maxLength}
    autoFocus={autoFocus}
  />
)

export const TextAreaField: React.FunctionComponent<
  TextAreaProps & ControlFieldProps
> = ({
  className,
  dataQa,
  label,
  message,
  name,
  state,
  required,
  onBlur,
  onChange,
  placeholder,
  value,
  rows,
  disabled,
  maxLength,
  autoFocus
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
    <TextArea
      name={name}
      onBlur={onBlur}
      onChange={onChange}
      value={value}
      placeholder={placeholder}
      rows={rows}
      disabled={disabled}
      maxLength={maxLength}
      autoFocus={autoFocus}
    />
  </ControlField>
)
