// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'

import { useThrottledEventHandler } from './button-commons'
import {
  IconOnlyButtonVisualProps,
  renderIconOnlyButton
} from './icon-only-button-visuals'

export type IconOnlyButtonProps = IconOnlyButtonVisualProps & {
  onClick?: (e: React.MouseEvent<HTMLButtonElement>) => void
}

export const IconOnlyButton = React.memo(function IconOnlyButton({
  onClick,
  ...props
}: IconOnlyButtonProps) {
  const handleOnClick = useThrottledEventHandler(onClick)
  return renderIconOnlyButton(props, handleOnClick, (icon) => (
    <FontAwesomeIcon icon={icon} />
  ))
})
