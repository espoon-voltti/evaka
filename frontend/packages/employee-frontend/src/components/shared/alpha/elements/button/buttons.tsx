// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import classNames from 'classnames'

export interface ButtonsProps {
  children: React.ReactNode
  className?: string
  dataQa?: string
  centered?: boolean
}

export const Buttons: React.FunctionComponent<ButtonsProps> = ({
  children,
  className,
  dataQa,
  centered
}) => (
  <div
    className={classNames('buttons', className, { 'is-centered': centered })}
    data-qa={dataQa}
  >
    {children}
  </div>
)
