// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { animated, useSpring } from '@react-spring/web'
import React from 'react'
import styled, { useTheme } from 'styled-components'

import { faCheck, faTimes } from 'lib-icons'

import {
  AsyncButtonBehaviorProps,
  useAsyncButtonBehavior
} from './async-button-behavior'
import { BaseButtonVisualProps, renderBaseButton } from './button-visuals'

export type AsyncButtonProps<T> = BaseButtonVisualProps &
  AsyncButtonBehaviorProps<T> & {
    textInProgress?: string
    textDone?: string
  }

const AsyncButton_ = function AsyncButton<T>({
  type,
  preventDefault = type === 'submit',
  stopPropagation = false,
  onClick,
  onSuccess,
  onFailure,
  text,
  textInProgress = text,
  textDone = text,
  ...props
}: AsyncButtonProps<T>) {
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

  const container = useSpring<{ x: number }>({
    x: ((!hideSuccess || state !== 'success') && showIcon) || props.icon ? 1 : 0
  })
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

  return renderBaseButton({ text, ...props }, handleClick, ({ icon }) => (
    <>
      <IconContainer
        style={{
          width: container.x.to((x) => `${24 * x}px`),
          marginRight: container.x.to((x) => `${8 * x}px`)
        }}
      >
        {icon && (
          <IconWrapper
            style={{
              opacity: iconSpring.opacity,
              transform: iconSpring.opacity.to((x) => `scale(${x ?? 0})`)
            }}
          >
            <FontAwesomeIcon icon={icon} color={colors.main.m2} />
          </IconWrapper>
        )}
        <Spinner style={spinner} />
        <IconWrapper
          style={{
            opacity: checkmark.opacity,
            transform: checkmark.opacity.to((x) => `scale(${x ?? 0})`)
          }}
        >
          <FontAwesomeIcon icon={faCheck} color={colors.main.m2} />
        </IconWrapper>
        <IconWrapper
          style={{
            opacity: cross.opacity,
            transform: cross.opacity.to((x) => `scale(${x ?? 0})`)
          }}
        >
          <FontAwesomeIcon icon={faTimes} color={colors.status.danger} />
        </IconWrapper>
      </IconContainer>
      <TextWrapper>
        {state === 'in-progress'
          ? textInProgress
          : state === 'success'
            ? textDone
            : text}
      </TextWrapper>
    </>
  ))
}

export const AsyncButton = React.memo(AsyncButton_) as typeof AsyncButton_

const IconContainer = animated(styled.div`
  position: relative;
  overflow: hidden;
  height: 24px;
  flex: 0 0 auto;
`)

const Spinner = animated(styled.div`
  position: absolute;
  top: 2px;
  left: 2px;
  display: inline-block;
  border-radius: 50%;
  width: 20px;
  height: 20px;

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

const IconWrapper = animated(styled.div`
  position: absolute;

  svg.svg-inline--fa {
    width: 24px;
    height: 24px;
  }
`)

const TextWrapper = styled.div`
  white-space: pre-wrap;
`
