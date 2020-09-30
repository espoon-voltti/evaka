// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import classNames from 'classnames'

interface TitleProps {
  size: 1 | 2 | 3 | 4
  children: React.ReactNode
  className?: string
  tag?: 1 | 2 | 3 | 4 | 5 | 6
  dataQa?: string
}

export const Title: React.FunctionComponent<TitleProps> = ({
  size,
  children,
  className,
  tag,
  dataQa
}) =>
  React.createElement(
    `h${tag || size}`,
    {
      className: classNames('title', `is-${size}`, className),
      'data-qa': dataQa
    },
    children
  )

interface SubTitleProps {
  children: React.ReactNode
  className?: string
  dataQa?: string
}

export const Subtitle: React.FunctionComponent<SubTitleProps> = ({
  children,
  className,
  dataQa
}) => (
  <h4 data-qa={dataQa} className={classNames('subtitle', 'is-4', className)}>
    {children}
  </h4>
)
