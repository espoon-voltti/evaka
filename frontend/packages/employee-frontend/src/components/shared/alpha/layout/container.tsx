// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import classNames from 'classnames'

export interface ContainerProps {
  children: React.ReactNode
  className?: string
  dataQa?: string
}

export const Container: React.FunctionComponent<ContainerProps> = ({
  children,
  className,
  dataQa
}) => (
  <div className={classNames('container', className)} data-qa={dataQa}>
    {children}
  </div>
)
