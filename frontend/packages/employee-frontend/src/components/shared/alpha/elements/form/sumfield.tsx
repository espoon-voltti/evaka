// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import classNames from 'classnames'

import { statusClassNames, FieldState } from './types'
import { Children } from '../../types/common'

interface Props {
  message: React.ReactNode | string
  className?: string
  dataQa?: string
  state?: FieldState
}

export const SumField: React.FunctionComponent<Props & Children> = ({
  message,
  className,
  dataQa,
  state,
  children
}) => (
  <div
    className={classNames(
      'sum-field',
      'control',
      className,
      state && statusClassNames[state]
    )}
    data-qa={dataQa}
  >
    {children}
    <p className={classNames('help', state && statusClassNames[state])}>
      {message}
    </p>
  </div>
)
