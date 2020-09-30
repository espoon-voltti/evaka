// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import classNames from 'classnames'
import { FieldState } from './types'
import { Title } from '../title'
import { ValidationMessage } from './validation-message'

interface GroupLabelProps {
  state?: FieldState
  message?: React.ReactNode
  label?: React.ReactNode
  required?: boolean
}

export const GroupLabel = ({
  state,
  message,
  label,
  required
}: GroupLabelProps) => (
  <div className={classNames('group-label', { 'is-required': required })}>
    <span className="group-label-content">{label}</span>

    <ValidationMessage message={message} state={state} />
  </div>
)

export const GroupTitleLabel = ({
  state,
  message,
  label,
  required
}: GroupLabelProps) => (
  <Title
    size={3}
    className={classNames('group-label-title', { 'is-required': required })}
  >
    <span className="group-label-content">{label}</span>

    <ValidationMessage message={message} state={state} />
  </Title>
)
