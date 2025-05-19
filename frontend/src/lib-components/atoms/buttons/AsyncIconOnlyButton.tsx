// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { animated, useSpring } from '@react-spring/web'
import React from 'react'
import styled, { useTheme } from 'styled-components'

import { faCheck, faTimes } from 'lib-icons'

import { useTranslations } from '../../i18n'
import { ScreenReaderOnly } from '../ScreenReaderOnly'
import type { IconSize } from '../icon-size'

import type { AsyncButtonBehaviorProps } from './async-button-behavior'
import { useAsyncButtonBehavior } from './async-button-behavior'
import type { BaseIconOnlyButtonVisualProps } from './icon-only-button-visuals'
import { renderBaseIconOnlyButton } from './icon-only-button-visuals'

export type AsyncIconOnlyButtonProps<T> = BaseIconOnlyButtonVisualProps &
  AsyncButtonBehaviorProps<T> & {
    hideSuccess?: boolean
  }

const AsyncIconOnlyButton_ = function AsyncIconOnlyButton<T>({
  preventDefault,
  stopPropagation,
  onClick,
  onSuccess,
  onFailure,
  hideSuccess,
  ...props
}: AsyncIconOnlyButtonProps<T>) {
  const { state, handleClick } = useAsyncButtonBehavior({
    preventDefault,
    stopPropagation,
    onClick,
    onSuccess,
    onFailure
  })
  const i18n = useTranslations()
  const { colors } = useTheme()

  const showIcon = state !== 'idle'

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

  return renderBaseIconOnlyButton(
    {
      'data-status': state === 'idle' ? '' : state,
      'aria-busy': state === 'in-progress',
      ...props
    },
    handleClick,
    (icon, size) => (
      <>
        {state === 'in-progress' && (
          <ScreenReaderOnly aria-live="polite" id="in-progress">
            {i18n.asyncButton.inProgress}
          </ScreenReaderOnly>
        )}
        {state === 'failure' && (
          <ScreenReaderOnly aria-live="assertive" id="failure">
            {i18n.asyncButton.failure}
          </ScreenReaderOnly>
        )}
        {state === 'success' && (
          <ScreenReaderOnly aria-live="assertive" id="success">
            {i18n.asyncButton.success}
          </ScreenReaderOnly>
        )}
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
      </>
    )
  )
}

/**
 * An HTML button that looks like an icon and triggers an async action when clicked.
 *
 * Loading/success/failure states are indicated by temporarily replacing the default icon with a spinner or a checkmark/cross icon.
 */
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
