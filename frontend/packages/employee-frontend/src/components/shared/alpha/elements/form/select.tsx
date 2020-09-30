// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import { FieldState } from './types'
import { ControlField } from './control'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faAngleDown } from 'icon-set'

export interface SelectOptionProps {
  dataQa?: string
  id: string
  label: string
}

export interface SelectProps {
  disabled?: boolean
  id?: string
  name?: string
  onBlur?: (e: React.FocusEvent<HTMLSelectElement>) => void
  onFocus?: (e: React.FocusEvent<HTMLSelectElement>) => void
  onChange?: (e: React.ChangeEvent<HTMLSelectElement>) => void
  options: SelectOptionProps[]
  placeholder?: string
  value?: string
  dataQa?: string
}

export const Select: React.FunctionComponent<SelectProps> = ({
  id,
  disabled = false,
  name,
  onBlur,
  onFocus,
  onChange,
  options,
  placeholder,
  value,
  dataQa
}) => (
  <select
    className={'input'}
    id={id}
    name={name}
    onBlur={onBlur}
    onFocus={onFocus}
    onChange={onChange}
    value={value}
    disabled={disabled}
    data-qa={dataQa}
  >
    {placeholder && (
      <option key="default" value="">
        {placeholder}
      </option>
    )}
    {options.map(({ id, dataQa, label }) => (
      <option key={id} id={id} value={id} data-qa={dataQa}>
        {label}
      </option>
    ))}
  </select>
)

export interface SelectFieldProps {
  disabled?: boolean
  label?: React.ReactNode
  value: string
  placeholder?: string
  message?: React.ReactNode
  name: string
  onBlur: (e: React.SyntheticEvent<HTMLSelectElement>) => void
  onChange: (e: React.ChangeEvent<HTMLSelectElement>) => void
  options: SelectOptionProps[]
  required?: boolean
  state?: FieldState
  dataQa?: string
}

export class SelectField extends React.PureComponent<SelectFieldProps> {
  public render() {
    const {
      disabled,
      name,
      onBlur,
      onChange,
      options,
      value,
      placeholder,
      ...containerProps
    } = this.props

    return (
      <ControlField name={name} {...containerProps}>
        <Select
          disabled={disabled}
          id={name}
          name={name}
          onBlur={onBlur}
          onChange={onChange}
          options={options}
          value={value}
          placeholder={placeholder}
        />
        <div className="select-caret">
          <FontAwesomeIcon icon={faAngleDown} />
        </div>
      </ControlField>
    )
  }
}
