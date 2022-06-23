// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import * as Sentry from '@sentry/browser'
import classNames from 'classnames'
import React, {
  FormEvent,
  useCallback,
  useEffect,
  useRef,
  useState
} from 'react'
import { animated, useSpring } from 'react-spring'
import styled, { useTheme } from 'styled-components'

import { Failure, Result } from 'lib-common/api'
import { isAutomatedTest } from 'lib-common/utils/helpers'
import { faCheck, faTimes } from 'lib-icons'

import { StyledButton } from './Button'

const onSuccessTimeout = isAutomatedTest ? 10 : 500
const clearStateTimeout = isAutomatedTest ? 25 : 2000

type ButtonState<T> =
  | { state: 'idle' | 'in-progress' | 'failure' }
  | { state: 'success'; value: T }

/* eslint-disable @typescript-eslint/no-explicit-any */

const idle: ButtonState<any> = { state: 'idle' }
const inProgress: ButtonState<any> = { state: 'in-progress' }
const failure: ButtonState<any> = { state: 'failure' }

/* eslint-enable @typescript-eslint/no-explicit-any */

export interface Props<T> {
  text: string
  textInProgress?: string
  textDone?: string
  /** Return a promise to start an async action, or `undefined` to do a sync action (or nothing at all) */
  onClick: () => Promise<Result<T>> | void
  /** Called when the promise has resolved with a Success value and the success animation has finished */
  onSuccess: (value: T) => void
  /** Called immediately when the promis has resolved with a Failure value */
  onFailure?: (failure: Failure<T>) => void
  type?: 'button' | 'submit'
  preventDefault?: boolean
  primary?: boolean
  disabled?: boolean
  className?: string
  'data-qa'?: string
}

function AsyncButton<T>({
  className,
  text,
  textInProgress = text,
  textDone = text,
  type = 'button',
  preventDefault = type === 'submit',
  primary,
  disabled,
  onClick,
  onSuccess,
  onFailure,
  ...props
}: Props<T>) {
  const { colors } = useTheme()
  const [buttonState, setButtonState] = useState<ButtonState<T>>(idle)
  const onSuccessRef = useRef(onSuccess)

  const mountedRef = useRef(true)
  useEffect(
    () => () => {
      mountedRef.current = false
    },
    []
  )

  const handleSuccess = useCallback((value: T) => {
    setButtonState({ state: 'success', value })
  }, [])

  const handleFailure = useCallback(
    (value: Failure<T> | undefined) => {
      if (!mountedRef.current) return
      setButtonState(failure)
      onFailure && value !== undefined && onFailure(value)
    },
    [onFailure]
  )

  const isInProgress = buttonState.state === 'in-progress'
  const isSuccess = buttonState.state === 'success'
  const isFailure = buttonState.state === 'failure'

  const handleClick = useCallback(
    (e: FormEvent) => {
      if (preventDefault) e.preventDefault()

      if (!mountedRef.current) return
      if (isInProgress) return

      const maybePromise = onClick()
      if (maybePromise === undefined) {
        // The click handler didn't do an async call, nothing to do here
      } else {
        setButtonState(inProgress)
        maybePromise
          .then((result) => {
            if (!mountedRef.current) return
            if (result.isSuccess) {
              handleSuccess(result.value)
            }
            if (result.isLoading) {
              handleFailure(undefined)
              Sentry.captureMessage(
                'BUG: AsyncButton promise resolved to a Loading value',
                'error'
              )
            } else if (result.isFailure) {
              handleFailure(result)
            } else {
              handleSuccess(result.value)
            }
          })
          .catch((originalErr: Error) => {
            handleFailure(undefined)
            if ('message' in originalErr && 'stack' in originalErr) {
              const err = new Error(
                `AsyncButton promise was rejected: ${originalErr.message}`
              )
              err.stack = originalErr.stack
              Sentry.captureException(err)
            } else {
              Sentry.captureException(originalErr)
            }
          })
      }
    },
    [preventDefault, isInProgress, onClick, handleSuccess, handleFailure]
  )

  useEffect(() => {
    onSuccessRef.current = (value: T) => onSuccess(value)
  }, [onSuccess])

  useEffect(() => {
    if (buttonState.state === 'success') {
      const runOnSuccess = setTimeout(
        () => onSuccessRef.current(buttonState.value),
        onSuccessTimeout
      )
      const clearState = setTimeout(
        () => mountedRef.current && setButtonState(idle),
        clearStateTimeout
      )
      return () => {
        clearTimeout(runOnSuccess)
        clearTimeout(clearState)
      }
    } else if (buttonState.state === 'failure') {
      const clearState = setTimeout(
        () => mountedRef.current && setButtonState(idle),
        clearStateTimeout
      )
      return () => clearTimeout(clearState)
    }
    return undefined
  }, [buttonState])

  const showIcon = buttonState.state !== 'idle'

  const container = useSpring<{ x: number }>({ x: showIcon ? 1 : 0 })
  const spinner = useSpring<{ opacity: number }>({
    opacity: isInProgress ? 1 : 0
  })
  const checkmark = useSpring<{ opacity: number }>({
    opacity: isSuccess ? 1 : 0
  })
  const cross = useSpring<{ opacity: number }>({
    opacity: isFailure ? 1 : 0
  })

  return (
    <StyledButton
      type={type}
      className={classNames(className, {
        primary: !!primary && !showIcon,
        disabled
      })}
      disabled={disabled || showIcon}
      onClick={handleClick}
      {...props}
      data-status={buttonState.state === 'idle' ? '' : buttonState.state}
    >
      <Content>
        <IconContainer
          style={{
            width: container.x.to((x) => `${32 * x}px`),
            paddingRight: container.x.to((x) => `${8 * x}px`)
          }}
        >
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
        <span>
          {isInProgress ? textInProgress : isSuccess ? textDone : text}
        </span>
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

const IconWrapper = animated(
  styled.div`
    position: absolute;

    svg.svg-inline--fa {
      width: 24px;
      height: 24px;
    }
  `
)
