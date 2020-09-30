// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import classNames from 'classnames'

interface Props {
  children: React.ReactNode
  className?: string
  dataQa?: string
  alignContent?: 'center'
}

export const Section = ({
  children,
  dataQa,
  alignContent,
  className
}: Props) => (
  <div
    className={classNames(
      'content-section',
      {
        [`align-content-${alignContent ?? ''}`]: alignContent
      },
      className
    )}
    data-qa={dataQa}
  >
    {children}
  </div>
)
