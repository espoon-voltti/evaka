// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import classNames from 'classnames'
import React, { useMemo } from 'react'
import { Link, matchRoute, useLocation, useRouter } from 'wouter'

type HTMLLinkAttributes = Omit<
  React.AnchorHTMLAttributes<HTMLAnchorElement>,
  'href' | 'className'
>

export interface Props extends HTMLLinkAttributes {
  className?: string
  to: string
  matchRoutes?: string | string[]
}

export default React.forwardRef(function NavLink(
  { className, to, matchRoutes, ...props }: Props,
  ref: React.ForwardedRef<HTMLAnchorElement>
) {
  const isActive = useIsRouteActive(matchRoutes ?? to)
  return (
    <Link
      className={classNames(className, { active: isActive })}
      ref={ref}
      to={to}
      {...props}
    />
  )
})

export function useIsRouteActive(to: string | string[]): boolean {
  const parser = useRouter().parser
  const path = useLocation()[0]
  return useMemo(() => {
    if (typeof to === 'string') {
      return matchRoute(parser, `${to}/*?`, path)[0]
    } else {
      return to.some((route) => {
        return matchRoute(parser, `${route}/*?`, path)[0]
      })
    }
  }, [parser, path, to])
}
