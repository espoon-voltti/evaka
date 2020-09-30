// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import classNames from 'classnames'

import { Radio } from './radio'
import { FieldState, icons } from './types'
import { GroupLabel, GroupTitleLabel } from './group-label'
import { Icon } from '../icon'

interface RadioGroupProps {
  children: React.ReactNodeArray
  required?: boolean
  label: string
  message?: React.ReactNode
  state?: FieldState
  dataQa?: string
}

export const RadioGroup = ({
  children,
  label,
  required,
  message,
  state,
  dataQa
}: RadioGroupProps) => (
  <div
    className={classNames('input-group is-vertical', {
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
    {children.map((c, i) => (
      <div className="input-group-item" key={i}>
        {c}
      </div>
    ))}
  </div>
)

export const RadioTitleLabelGroup = ({
  children,
  label,
  required,
  message,
  state,
  dataQa
}: RadioGroupProps) => (
  <div
    className={classNames('input-group is-vertical', {
      'is-required': required
    })}
    data-qa={dataQa}
  >
    <GroupTitleLabel
      label={label}
      required={required}
      message={message}
      state={state}
    />
    {children.map((c, i) => (
      <div className="input-group-item" key={i}>
        {c}
      </div>
    ))}
  </div>
)

interface RadioSelectProps {
  label: string
  value: boolean | null
  onChange: (v: boolean | null) => void
  labelLeft: string
  labelRight: string
  required?: boolean
  dataQa?: string
  name: string
  message?: React.ReactNode
  state?: FieldState
}

export const RadioSelect = ({
  label,
  value,
  onChange,
  required,
  labelLeft,
  labelRight,
  dataQa,
  name,
  message,
  state
}: RadioSelectProps) => {
  return (
    <div
      className={classNames('radio-select-list-item', {
        'is-required': required
      })}
      data-qa={dataQa}
    >
      <div className="radio-select-list-item-label">{label}</div>
      <div className="radio-select-list-item-input">
        <Radio<boolean | null>
          dataQa={'true'}
          onChange={onChange}
          id={`${name}_a`}
          label={labelLeft}
          value={true}
          model={value}
        />
        <Radio<boolean | null>
          dataQa={'false'}
          onChange={onChange}
          id={`${name}_b`}
          label={labelRight}
          value={false}
          model={value}
        />
      </div>
      <div className="radio-select-list-item-input-error">
        {message && (
          <span className={'help'}>
            {state && (
              <Icon
                icon={icons[state]}
                className={classNames({ [`is-${state}`]: state })}
              />
            )}
            {message}
          </span>
        )}
      </div>
    </div>
  )
}
