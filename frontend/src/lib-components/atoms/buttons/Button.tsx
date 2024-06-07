// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'

import { useThrottledEventHandler } from './button-commons'
import { BaseButtonVisualProps, renderBaseButton } from './button-visuals'

export type ButtonProps = BaseButtonVisualProps & {
  onClick?: (e: React.MouseEvent<HTMLButtonElement>) => void
}

export const Button = React.memo(function Button({
  onClick,
  ...props
}: ButtonProps) {
  const handleOnClick = useThrottledEventHandler(onClick)
  return renderBaseButton(props, handleOnClick, ({ text, icon, order }) => (
    <>
      {icon && order == 'icon-text' && <FontAwesomeIcon icon={icon} />}
      <span>{text}</span>
      {icon && order == 'text-icon' && <FontAwesomeIcon icon={icon} />}
    </>
  ))
})
