// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import classNames from 'classnames'
import * as React from 'react'
import { Children } from '../../types/common'

export interface FieldProps {
  className?: string
  dataQa?: string
  hasAddons?: boolean
  label?: React.ReactNode
  message?: React.ReactNode
  name?: string
  required?: boolean
  disabled?: boolean
}

export const Field: React.FunctionComponent<FieldProps & Children> = ({
  children,
  className,
  dataQa,
  hasAddons = false,
  label,
  message,
  name,
  required,
  disabled
}: FieldProps & Children) => (
  <div
    className={classNames('field', className, {
      'has-addons': hasAddons,
      'is-required': required,
      'is-disabled': disabled
    })}
    data-qa={dataQa}
  >
    {label && (
      <label htmlFor={name} className="label">
        {label}
      </label>
    )}

    {children}

    {message && <p className="help">{message}</p>}
  </div>
)
