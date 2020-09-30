// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import classNames from 'classnames'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import { Button } from './button'

export interface IconButtonProps {
  icon: IconDefinition
  className?: string
  dark?: boolean
  dataQa?: string
  disabled?: boolean
  narrow?: boolean
  onClick?: (e: React.MouseEvent<HTMLButtonElement>) => void
  type?: 'submit' | 'button'
  plain?: boolean
  success?: boolean
  warning?: boolean
  danger?: boolean
  primary?: boolean
  opaque?: boolean
  muted?: boolean
  size?: 'xlarge' | 'large' | 'medium' | 'small'
}

export const IconButton: React.FunctionComponent<IconButtonProps> = ({
  size,
  className,
  icon,
  success,
  warning,
  danger,
  primary,
  plain,
  muted,
  ...buttonProps
}: IconButtonProps) => (
  <Button
    className={classNames(
      'is-round-icon',
      {
        [`is-${size ?? ''}`]: size,
        'is-success': success,
        'is-warning': warning,
        'is-danger': danger,
        'is-primary': primary,
        'is-plain': plain,
        'is-muted': muted
      },
      className
    )}
    {...buttonProps}
  >
    <FontAwesomeIcon icon={icon} />
  </Button>
)
