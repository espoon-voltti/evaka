// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import classNames from 'classnames'

export interface StaticInputProps {
  borderless?: boolean
  id?: string
  children: React.ReactNode
}

export const StaticInput: React.FunctionComponent<StaticInputProps> = ({
  borderless = false,
  children,
  id
}) => (
  <span
    className={classNames('input', {
      'is-borderless': borderless
    })}
    id={id}
  >
    {children}
  </span>
)
