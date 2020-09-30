// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import classnames from 'classnames'

interface ColumnsProps {
  children: React.ReactNode
  className?: string
  dataQa?: string
  gapless?: boolean
  multiline?: boolean
}

export const Columns: React.FunctionComponent<ColumnsProps> = ({
  children,
  className,
  dataQa,
  gapless = false,
  multiline = false
}) => (
  <div
    className={classnames(
      'columns',
      { 'is-multiline': multiline, 'is-gapless': gapless },
      className
    )}
    data-qa={dataQa}
  >
    {children}
  </div>
)

export type NumericSizes = 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12

export type NamedSizes =
  | 'three-quarters'
  | 'two-thirds'
  | 'half'
  | 'one-third'
  | 'one-quarter'
  | 'full'
  | 'four-fifths'
  | 'three-fifths'
  | 'two-fifths'
  | 'one-fifth'
  | 'narrow'

export type Sizes = NumericSizes | NamedSizes

export const sizes: Sizes[] = [
  'three-quarters',
  'two-thirds',
  'half',
  'one-third',
  'one-quarter',
  'full',
  'four-fifths',
  'three-fifths',
  'two-fifths',
  'one-fifth',
  'narrow',
  1,
  2,
  3,
  4,
  5,
  6,
  7,
  8,
  9,
  10,
  11,
  12
]

interface ColumnProps {
  align?: 'start' | 'end'
  size?: Sizes | null
  tabletSize?: Sizes | null
  desktopSize?: Sizes | null
  children?: React.ReactNode
  className?: string
  dataQa?: string
}

export const Column: React.FunctionComponent<ColumnProps> = ({
  children,
  className,
  align,
  size,
  desktopSize,
  tabletSize,
  dataQa
}) => (
  <div
    className={classnames(
      'column',
      align ? `is-align-${align}` : '',
      size ? `is-${size}` : '',
      desktopSize ? `is-${desktopSize}-desktop` : '',
      tabletSize ? `is-${tabletSize}-tablet` : '',
      tabletSize ? `is-${tabletSize}-tablet` : '',
      className
    )}
    data-qa={dataQa}
  >
    {children}
  </div>
)
