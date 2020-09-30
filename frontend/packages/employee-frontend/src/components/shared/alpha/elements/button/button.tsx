// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import { useTimeoutFn } from 'react-use'
import classNames from 'classnames'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { IconDefinition, SizeProp } from '@fortawesome/fontawesome-svg-core'

type Width = 'wide' | 'narrow' | 'full'

export interface ButtonProps {
  children?: React.ReactNode
  className?: string
  width?: Width
  dark?: boolean
  dataQa?: string
  disabled?: boolean
  onClick?: (e: React.MouseEvent<HTMLButtonElement>) => void
  plain?: boolean
  primary?: boolean
  type?: 'submit' | 'button'
  icon?: IconDefinition
  iconSize?: SizeProp
}

export const Button: React.FunctionComponent<ButtonProps> = ({
  children,
  className,
  width,
  dataQa,
  disabled = false,
  dark = false,
  plain = false,
  primary = false,
  onClick,
  type = 'button',
  icon,
  iconSize
}) => {
  const [ignoreClick, setIgnoreClick] = React.useState(false)
  const [, , startUnignoreClickTimer] = useTimeoutFn(() => {
    if (ignoreClick) {
      setIgnoreClick(false)
    }
  }, 300)

  const handleOnClick = (e: React.MouseEvent<HTMLButtonElement>) => {
    if (!ignoreClick) {
      setIgnoreClick(true)
      startUnignoreClickTimer()
      if (onClick) {
        onClick(e)
      }
    }
  }

  return (
    <button
      className={classNames(
        'button',
        {
          'is-primary': primary,
          'is-plain': plain,
          'is-dark': dark,
          'has-icon': icon,
          [`is-${width ?? ''}`]: !!width
        },
        className
      )}
      type={type}
      data-qa={dataQa}
      disabled={disabled}
      onClick={handleOnClick}
    >
      {icon && <FontAwesomeIcon icon={icon} size={iconSize} />}
      {children}
    </button>
  )
}
