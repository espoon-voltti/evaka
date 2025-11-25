// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useEffect } from 'react'

import { useThrottledEventHandler } from './button-commons'
import type { BaseIconOnlyButtonVisualProps } from './icon-only-button-visuals'
import { renderBaseIconOnlyButton } from './icon-only-button-visuals'

export type IconOnlyButtonProps = BaseIconOnlyButtonVisualProps & {
  onClick?: (e: React.MouseEvent<HTMLButtonElement>) => void
}
export type IconOnlyRefButtonProps = IconOnlyButtonProps & {
  ref?: React.Ref<HTMLButtonElement>
  returnFocusRef?: React.RefObject<HTMLElement | null>
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

export const IconOnlyRefButton = React.memo(function IconOnlyRefButton({
  onClick,
  ref,
  returnFocusRef,
  ...props
}: IconOnlyRefButtonProps) {
  const handleOnClick = useThrottledEventHandler(onClick)

  useFocusRefElementOnUnmount(returnFocusRef)

  return renderBaseIconOnlyButton(
    props,
    handleOnClick,
    (icon) => <FontAwesomeIcon icon={icon} />,
    ref
  )
})

const useFocusRefElementOnUnmount = (
  ref?: React.RefObject<HTMLElement | null>
) => {
  useEffect(() => {
    const target = ref?.current
    return () => {
      if (target && document.contains(target)) {
        target.focus()
      }
    }
  }, [ref])
}
