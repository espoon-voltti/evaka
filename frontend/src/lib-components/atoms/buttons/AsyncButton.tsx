// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
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

export type AsyncClickCallback = (
  cancel: () => Promise<void>
) => Promise<void | Result<unknown>>

type Props = {
  text: string
  textInProgress?: string
  textDone?: string
  onClick: AsyncClickCallback
  onSuccess: () => void
  onFailure?: (result?: Failure<unknown> | undefined) => void
  type?: 'button' | 'submit'
  preventDefault?: boolean
  primary?: boolean
  disabled?: boolean
  className?: string
  'data-qa'?: string
}

export default React.memo(function AsyncButton({
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
}: Props) {
  const { colors } = useTheme()
  const [inProgress, setInProgress] = useState(false)
  const [showSuccess, setShowSuccess] = useState(false)
  const [showFailure, setShowFailure] = useState(false)
  const onSuccessRef = useRef(onSuccess)
  const canceledRef = useRef(false)

  const mountedRef = useRef(true)
  useEffect(
    () => () => {
      mountedRef.current = false
    },
    []
  )

  const handleFailure = useCallback(
    (result: Failure<unknown> | undefined) => {
      if (!mountedRef.current) return
      setShowFailure(true)
      onFailure && onFailure(result)
    },
    [onFailure]
  )

  const callback = useCallback(
    (e: FormEvent) => {
      if (preventDefault) e.preventDefault()

      if (!mountedRef.current) return
      setInProgress(true)

      onClick(() => {
        canceledRef.current = true
        return Promise.resolve()
      })
        .then((result) => {
          if (!mountedRef.current) return
          if (canceledRef.current) {
            canceledRef.current = false
            return
          }

          if (result && result.isFailure) {
            handleFailure(result)
          } else {
            setShowSuccess(true)
          }
        })
        .catch(() => handleFailure(undefined))
        .finally(() => mountedRef.current && setInProgress(false))
    },
    [preventDefault, handleFailure, onClick]
  )

  useEffect(() => {
    onSuccessRef.current = onSuccess
  }, [onSuccess])

  useEffect(() => {
    const runOnSuccess = showSuccess
      ? setTimeout(() => onSuccessRef.current(), isAutomatedTest ? 10 : 500)
      : undefined
    const clearShowSuccess = showSuccess
      ? setTimeout(
          () => mountedRef.current && setShowSuccess(false),
          isAutomatedTest ? 25 : 2000
        )
      : undefined

    return () => {
      if (runOnSuccess) clearTimeout(runOnSuccess)
      if (clearShowSuccess) clearTimeout(clearShowSuccess)
    }
  }, [showSuccess])

  useEffect(() => {
    const clearShowFailure = showFailure
      ? setTimeout(
          () => mountedRef.current && setShowFailure(false),
          isAutomatedTest ? 25 : 2000
        )
      : undefined

    return () => {
      if (clearShowFailure) clearTimeout(clearShowFailure)
    }
  }, [showFailure])

  const showIcon = inProgress || showSuccess || showFailure

  const container = useSpring<{ x: number }>({ x: showIcon ? 1 : 0 })
  const spinner = useSpring<{ opacity: number }>({
    opacity: inProgress ? 1 : 0
  })
  const checkmark = useSpring<{ opacity: number }>({
    opacity: showSuccess ? 1 : 0
  })
  const cross = useSpring<{ opacity: number }>({ opacity: showFailure ? 1 : 0 })

  return (
    <StyledButton
      type={type}
      className={classNames(className, {
        primary: !!primary && !showIcon,
        disabled
      })}
      disabled={disabled || showIcon}
      onClick={callback}
      {...props}
      data-status={
        inProgress
          ? 'in-progress'
          : showSuccess
          ? 'success'
          : showFailure
          ? 'failure'
          : ''
      }
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
          {inProgress ? textInProgress : showSuccess ? textDone : text}
        </span>
      </Content>
    </StyledButton>
  )
})

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
