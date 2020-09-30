// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import classNames from 'classnames'

import { Icon } from '../icon'
import { Children } from '../../types/common'
import { FieldState, statusClassNames, icons } from './types'
import { FieldProps, Field } from './field'

export interface ControlProps {
  className?: string
  dataQa?: string
  expanded?: boolean
  state?: FieldState
}

export const Control: React.FunctionComponent<ControlProps & Children> = ({
  className,
  children,
  dataQa,
  expanded = false,
  state
}) => (
  <div
    className={classNames(
      'control',
      className,
      {
        'is-expanded': expanded,
        'has-icons': !!state
      },
      state && statusClassNames[state]
    )}
    data-qa={dataQa}
  >
    {children}
    {state && <Icon icon={icons[state]} />}
  </div>
)

export interface ControlFieldProps extends FieldProps {
  state?: FieldState
}

export const ControlField: React.FunctionComponent<
  ControlFieldProps & Children
> = ({
  children,
  className,
  dataQa,
  label,
  message,
  name,
  required,
  state,
  hasAddons,
  disabled
}: ControlFieldProps & Children) => (
  <Field
    className={className}
    dataQa={dataQa}
    label={label}
    message={message}
    name={name}
    required={required}
    hasAddons={hasAddons}
    disabled={disabled}
  >
    <Control state={state}>{children}</Control>
  </Field>
)
