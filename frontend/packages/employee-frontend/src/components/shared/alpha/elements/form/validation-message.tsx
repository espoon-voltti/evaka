// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import classNames from 'classnames'

import { FieldState, icons } from './types'
import { Icon } from '../icon'

interface Props {
  state?: FieldState
  message?: React.ReactNode
}

export const getValidationMessageIcon = (state: FieldState) => (
  <Icon
    icon={icons[state]}
    className={classNames({ [`is-${state}`]: state })}
  />
)

export const ValidationMessage = (p: Props) =>
  p.message ? (
    <span className="help valdiation-message">
      {p.state ? getValidationMessageIcon(p.state) : null}
      {p.message}
    </span>
  ) : null
