// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import classNames from 'classnames'

export interface LinkButtonProps {
  children: React.ReactNode
  href: string
  className?: string
  dataQa?: string
  dark?: boolean
  disabled?: boolean
  narrow?: boolean
  onClick?: (e: React.MouseEvent<HTMLAnchorElement>) => void
  plain?: boolean
  primary?: boolean
  target?: string
}

export const LinkButton: React.FunctionComponent<LinkButtonProps> = ({
  href,
  children,
  className,
  dataQa,
  disabled,
  dark,
  plain,
  primary,
  narrow,
  target,
  onClick
}) => (
  <a
    className={classNames(
      'button',
      {
        'is-primary': primary,
        'is-plain': plain,
        'is-narrow': narrow,
        'is-dark': dark,
        'is-disabled': disabled
      },
      className
    )}
    data-qa={dataQa}
    href={href}
    onClick={onClick}
    target={target}
  >
    {children}
  </a>
)
