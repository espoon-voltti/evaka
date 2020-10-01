// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'

import { faCheck } from '@evaka/icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'

interface Props {
  name: string
  label: React.ReactNode
  checked: boolean
  onChange: (value: boolean) => void
  dataQa?: string
  disabled?: boolean
}

export function Checkbox({
  name,
  label,
  checked,
  onChange,
  dataQa,
  disabled
}: Props) {
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) =>
    onChange(e.target.checked)

  return (
    <div className="checkbox">
      <input
        className="input"
        type="checkbox"
        id={name}
        name={name}
        checked={checked}
        data-qa={dataQa}
        onChange={handleChange}
        disabled={disabled}
      />
      <label className="label" htmlFor={name}>
        <div className="tick">
          <FontAwesomeIcon icon={faCheck} aria-hidden />
        </div>
        {label}
      </label>
    </div>
  )
}
