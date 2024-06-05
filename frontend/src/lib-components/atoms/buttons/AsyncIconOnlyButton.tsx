// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { animated, useSpring } from '@react-spring/web'
import React from 'react'
import styled, { useTheme } from 'styled-components'

import { faCheck, faTimes } from 'lib-icons'

import { IconSize } from '../icon-size'

import {
  AsyncButtonBehaviorProps,
  useAsyncButtonBehavior
} from './async-button-behavior'
import {
  IconOnlyButtonVisualProps,
  renderIconOnlyButton
} from './icon-only-button-visuals'

export type AsyncIconOnlyButtonProps<T> = IconOnlyButtonVisualProps &
  AsyncButtonBehaviorProps<T>

const AsyncIconOnlyButton_ = function AsyncIconOnlyButton<T>({
  preventDefault,
  stopPropagation,
  onClick,
  onSuccess,
  onFailure,
  ...props
}: AsyncIconOnlyButtonProps<T>) {
  const { state, handleClick } = useAsyncButtonBehavior({
    preventDefault,
    stopPropagation,
    onClick,
    onSuccess,
    onFailure
  })
  const { colors } = useTheme()

  const showIcon = state !== 'idle'
  const hideSuccess = false

  const iconSpring = useSpring<{ opacity: number }>({
    opacity: props.icon && !showIcon ? 1 : 0
  })
  const spinner = useSpring<{ opacity: number }>({
    opacity: state === 'in-progress' ? 1 : 0
  })
  const checkmark = useSpring<{ opacity: number }>({
    opacity: !hideSuccess && state === 'success' ? 1 : 0
  })
  const cross = useSpring<{ opacity: number }>({
    opacity: state === 'failure' ? 1 : 0
  })

  return renderIconOnlyButton(props, handleClick, (icon, size) => (
    <RelativeContainer>
      <IconContainer $size={size}>
        <Spinner style={spinner} />
      </IconContainer>
      <IconContainer $size={size}>
        <animated.div
          style={{
            opacity: checkmark.opacity,
            transform: checkmark.opacity.to((x) => `scale(${x ?? 0})`)
          }}
        >
          <FontAwesomeIcon icon={faCheck} color={colors.main.m2} />
        </animated.div>
      </IconContainer>
      <IconContainer $size={size}>
        <animated.div
          style={{
            opacity: cross.opacity,
            transform: cross.opacity.to((x) => `scale(${x ?? 0})`)
          }}
        >
          <FontAwesomeIcon icon={faTimes} color={colors.status.danger} />
        </animated.div>
      </IconContainer>
      <animated.div
        style={{
          opacity: iconSpring.opacity,
          transform: iconSpring.opacity.to((x) => `scale(${x ?? 0})`)
        }}
      >
        <FontAwesomeIcon icon={icon} />
      </animated.div>
    </RelativeContainer>
  ))
}

export const AsyncIconOnlyButton = React.memo(
  AsyncIconOnlyButton_
) as typeof AsyncIconOnlyButton_

const RelativeContainer = styled.div`
  position: relative;
  display: flex;
  width: 100%;
  height: 100%;
  align-items: center;
  justify-content: center;
`

const IconContainer = styled.div<{ $size: IconSize }>`
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
`
const Spinner = animated(styled.div`
  display: inline-block;
  border-radius: 50%;
  width: 70%;
  height: 70%;

  border: 2px solid ${(p) => p.theme.colors.grayscale.g15};
  border-left-color: ${(p) => p.theme.colors.main.m2};
  animation: spin 1s infinite linear;

  @keyframes spin {
    0% {
      transform: rotate(0deg);
    }
    100% {
      transform: rotate(360deg);
    }
  }
`)
