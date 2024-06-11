// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'

import { useThrottledEventHandler } from './button-commons'
import {
  BaseIconOnlyButtonVisualProps,
  renderBaseIconOnlyButton
} from './icon-only-button-visuals'

export type IconOnlyButtonProps = BaseIconOnlyButtonVisualProps & {
  onClick?: (e: React.MouseEvent<HTMLButtonElement>) => void
}

/**
 * An HTML button that looks like an icon.
 */
export const IconOnlyButton = React.memo(function IconOnlyButton({
  onClick,
  ...props
}: IconOnlyButtonProps) {
  const handleOnClick = useThrottledEventHandler(onClick)
  return renderBaseIconOnlyButton(props, handleOnClick, (icon) => (
    <FontAwesomeIcon icon={icon} />
  ))
})
