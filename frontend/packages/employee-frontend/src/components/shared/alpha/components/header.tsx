// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import classNames from 'classnames'

import { Title } from '../elements/title'

interface Props {
  title: React.ReactNode
  children?: React.ReactNode
  logo: React.ReactNode
  className?: string
  dataQa?: string
  link?: string
}

export const Header: React.FunctionComponent<Props> = ({
  title,
  children,
  logo,
  className,
  dataQa,
  link
}: Props) => {
  const [menuActive, setMenuActive] = React.useState(false)

  return (
    <header data-qa={dataQa} className={classNames('header', className)}>
      <nav className="navbar container">
        <div className="navbar-brand">
          <a href={link ? link : '/'}>
            <div className="header-title">
              {logo}
              <Title size={2}>{title}</Title>
            </div>
          </a>
          {children && (
            <a
              role="button"
              className={classNames({
                burger: true,
                'navbar-burger': true,
                'is-active': menuActive
              })}
              aria-label="menu"
              aria-expanded={menuActive}
              onClick={() => setMenuActive(!menuActive)}
            >
              <span aria-hidden="true"></span>
              <span aria-hidden="true"></span>
              <span aria-hidden="true"></span>
            </a>
          )}
        </div>
        <div
          className={classNames({
            'navbar-menu': true,
            'is-active': menuActive
          })}
        >
          {children}
        </div>
      </nav>
    </header>
  )
}

interface NavbarItemProps {
  children: React.ReactNode
  className?: string
  align?: 'start' | 'end'
  border?: boolean
  dataQa?: string
  tab?: boolean
  active?: boolean
}

export const NavbarItem = ({
  children,
  className,
  align,
  border,
  dataQa,
  tab = false,
  active = false
}: NavbarItemProps) => (
  <div
    data-qa={dataQa}
    className={classNames('navbar-item', className, {
      [`navbar-${align ?? ''}`]: align,
      'is-tab': tab,
      'is-active': active,
      border
    })}
  >
    {children}
  </div>
)
