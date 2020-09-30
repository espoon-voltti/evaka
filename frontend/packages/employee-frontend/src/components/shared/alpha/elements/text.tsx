// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import classNames from 'classnames'

interface TextModifierProps {
  weight?: 'bold' | 'semibold' | 'medium' | 'normal' | 'light'
  textTransform?: 'uppercase' | 'lowercase' | 'capitalized'
  align?: 'left' | 'right' | 'centered'
  color?: 'muted'
  size?: 1 | 2 | 3 | 4 | 5 | 6 | 7
  nowrap?: true
  display?: 'block' | 'inline' | 'inline-block' | 'flex'
}

export const getTextModifierClassnames = (p: TextModifierProps) =>
  classNames('text', {
    [`has-text-weight-${p.weight ?? ''}`]: p.weight,
    [`is-${p.textTransform ?? ''}`]: p.textTransform,
    [`has-text-${p.align ?? ''}`]: p.align,
    [`is-size-${p.size ?? ''}`]: p.size,
    [`is-nowrap`]: p.nowrap,
    [`is-text-${p.color ?? ''}`]: p.color,
    [`display-${p.display ?? ''}`]: p.display
  })

interface TextImplProps extends TextModifierProps {
  dataQa?: string
  children: React.ReactNode
}

const TextImpl: React.FC<TextImplProps> = ({ children, dataQa, ...props }) => (
  <span className={getTextModifierClassnames(props)} data-qa={dataQa}>
    {children}
  </span>
)

export const Text = React.memo(TextImpl)
