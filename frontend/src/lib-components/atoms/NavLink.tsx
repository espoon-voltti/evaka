// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import classNames from 'classnames'
import React from 'react'
import { Link } from 'wouter'

type HTMLLinkAttributes = Omit<
  React.AnchorHTMLAttributes<HTMLAnchorElement>,
  'href' | 'className'
>

export interface Props extends HTMLLinkAttributes {
  className?: string
  to: string
  children: React.ReactNode
}

export default React.forwardRef(function NavLink(
  { className, ...props }: Props,
  ref: React.ForwardedRef<HTMLAnchorElement>
) {
  return (
    <Link
      className={(active) => classNames(className, { active })}
      ref={ref}
      {...props}
    />
  )
})
