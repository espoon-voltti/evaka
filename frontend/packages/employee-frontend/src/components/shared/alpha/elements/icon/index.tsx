// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import classNames from 'classnames'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { States } from '../../types/common'
import { IconProp, SizeProp } from '@fortawesome/fontawesome-svg-core'

export interface IconProps {
  icon: IconProp
  className?: string
  round?: true
  status?: States
  size?: 'small' | 'medium' | 'large'
  iconSize?: SizeProp
}

export const Icon: React.FunctionComponent<IconProps> = ({
  icon,
  className,
  status,
  round,
  size,
  iconSize
}) => (
  <span
    className={classNames(
      'icon',
      {
        [`is-${size ?? ''}`]: size,
        [`is-${status ?? ''}`]: status,
        'is-round': round
      },
      className
    )}
  >
    <FontAwesomeIcon icon={icon} size={iconSize} />
  </span>
)
