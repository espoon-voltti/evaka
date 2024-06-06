// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { animated, useSpring } from '@react-spring/web'
import classNames from 'classnames'
import React from 'react'
import styled, { useTheme } from 'styled-components'

import { useTranslations } from 'lib-components/i18n'
import { faCheck, faTimes } from 'lib-icons'

import { ScreenReaderOnly } from '../ScreenReaderOnly'

import { StyledButton } from './LegacyButton'
import {
  AsyncButtonBehaviorProps,
  useAsyncButtonBehavior
} from './async-button-behavior'

export interface AsyncButtonProps<T> extends AsyncButtonBehaviorProps<T> {
  text: string
  textInProgress?: string
  textDone?: string
  type?: 'button' | 'submit'
  primary?: boolean
  disabled?: boolean
  className?: string
  'data-qa'?: string
  hideSuccess?: boolean
  icon?: IconDefinition
}

function AsyncButton<T>({
  className,
  text,
  textInProgress = text,
  textDone = text,
  type = 'button',
  preventDefault = type === 'submit',
  stopPropagation = false,
  primary,
  disabled,
  onClick,
  onSuccess,
  onFailure,
  hideSuccess = false,
  icon,
  ...props
}: AsyncButtonProps<T>) {
  const i18n = useTranslations()

  const { colors } = useTheme()
  const { state, handleClick } = useAsyncButtonBehavior({
    preventDefault,
    stopPropagation,
    onClick,
    onSuccess,
    onFailure
  })

  const showIcon = state !== 'idle'

  const container = useSpring<{ x: number }>({
    x: ((!hideSuccess || state !== 'success') && showIcon) || icon ? 1 : 0
  })
  const iconSpring = useSpring<{ opacity: number }>({
    opacity: icon && !showIcon ? 1 : 0
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

  return (
    <StyledButton
      type={type}
      className={classNames(className, {
        primary: !!primary && !showIcon,
        disabled
      })}
      disabled={disabled}
      onClick={handleClick}
      {...props}
      data-status={state === 'idle' ? '' : state}
      aria-busy={state === 'in-progress'}
    >
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
      <Content>
        <IconContainer
          style={{
            width: container.x.to((x) => `${32 * x}px`),
            paddingRight: container.x.to((x) => `${8 * x}px`)
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
      </Content>
    </StyledButton>
  )
}

export default React.memo(AsyncButton) as typeof AsyncButton

const Content = styled.div`
  display: flex;
  flex-wrap: nowrap;
  flex-direction: row;
  justify-content: center;
  align-items: center;
`

const IconContainer = animated(styled.div`
  position: relative;
  overflow: hidden;
  height: 24px;
  flex: none;
`)

const Spinner = animated(styled.div`
  position: absolute;
  left: 0;
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
