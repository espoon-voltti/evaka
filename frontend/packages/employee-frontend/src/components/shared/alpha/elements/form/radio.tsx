// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import classNames from 'classnames'

import { faCheck } from 'icon-set'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'

interface Props<T> {
  id: string
  label: React.ReactNode
  value: T
  model: T
  onChange: (value: T) => void
  dataQa?: string
  disabled?: boolean
  required?: boolean
}

export function Radio<T>({
  id,
  label,
  value,
  model,
  onChange,
  dataQa,
  disabled,
  required
}: Props<T>) {
  const handleChange = (v: T) => () => onChange(v)

  return (
    <div className={classNames('radio', { 'is-required': required })}>
      <input
        className="input"
        type="radio"
        id={id}
        checked={model === value}
        data-qa={dataQa}
        onChange={handleChange(value)}
        disabled={disabled}
      />
      <label className="label" htmlFor={id}>
        <div className="tick">
          <FontAwesomeIcon icon={faCheck} aria-hidden />
        </div>
        {label}
      </label>
    </div>
  )
}
