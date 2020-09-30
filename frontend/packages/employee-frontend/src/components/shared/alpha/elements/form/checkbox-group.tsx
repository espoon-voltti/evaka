// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import classNames from 'classnames'
import { FieldState } from './types'
import { GroupLabel } from './group-label'

interface Props {
  children: React.ReactNodeArray
  required?: boolean
  label: React.ReactNode
  dataQa?: string
  message?: React.ReactNode
  className?: string
  state?: FieldState
}

export const VerticalCheckboxGroup = ({
  children,
  className,
  label,
  required,
  dataQa,
  message,
  state
}: Props) => (
  <div
    className={classNames('input-group is-vertical', className, {
      'is-required': required
    })}
    data-qa={dataQa}
  >
    <GroupLabel
      label={label}
      required={required}
      message={message}
      state={state}
    />

    <div className="input-group-content">
      {children.map((c, i) => (
        <div className="input-group-item" key={i}>
          {c}
        </div>
      ))}
    </div>
  </div>
)
