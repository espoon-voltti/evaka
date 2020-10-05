// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { H1, H2, H3, H4 } from '../Typography'
import { BaseProps } from '../utils'

interface Props extends BaseProps {
  'data-qa'?: string
  size?: 1 | 2 | 3 | 4
  children: React.ReactNode
  centered?: boolean
  noMargin?: boolean
}

export default function Title({
  'data-qa': dataQa,
  size,
  children,
  centered,
  className,
  noMargin
}: Props) {
  switch (size) {
    case 1:
      return (
        <H1
          data-qa={dataQa}
          centered={centered}
          fitted
          className={className}
          noMargin={noMargin}
        >
          {children}
        </H1>
      )
    case 2:
      return (
        <H2
          data-qa={dataQa}
          centered={centered}
          fitted
          className={className}
          noMargin={noMargin}
        >
          {children}
        </H2>
      )
    case 3:
      return (
        <H3
          data-qa={dataQa}
          centered={centered}
          fitted
          className={className}
          noMargin={noMargin}
        >
          {children}
        </H3>
      )
    case 4:
      return (
        <H4
          data-qa={dataQa}
          centered={centered}
          fitted
          className={className}
          noMargin={noMargin}
        >
          {children}
        </H4>
      )
    default:
      return (
        <H1
          data-qa={dataQa}
          centered={centered}
          fitted
          className={className}
          noMargin={noMargin}
        >
          {children}
        </H1>
      )
  }
}
