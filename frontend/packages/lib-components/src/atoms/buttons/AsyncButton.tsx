{
  /*
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import React, { useEffect, useState } from 'react'
import styled from 'styled-components'
import { animated, useSpring } from 'react-spring'
import classNames from 'classnames'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faCheck, faTimes } from '@evaka/lib-icons'
import colors from '../../colors'
import { StyledButton } from './Button'

type Props = {
  text: string
  textInProgress?: string
  textDone?: string
  onClick: () => Promise<void | Result>
  onSuccess: () => void
  primary?: boolean
  disabled?: boolean
  className?: string
  'data-qa'?: string
}

type Result = {
  isFailure: boolean
}

export default React.memo(function AsyncButton({
  className,
  text,
  textInProgress = text,
  textDone = text,
  primary,
  disabled,
  onClick,
  onSuccess,
  ...props
}: Props) {
  const [inProgress, setInProgress] = useState(false)
  const [showSuccess, setShowSuccess] = useState(false)
  const [showFailure, setShowFailure] = useState(false)

  const callback = () => {
    setInProgress(true)
    onClick()
      .then((result) => {
        if (result && result.isFailure) {
          setShowFailure(true)
        } else {
          setShowSuccess(true)
        }
      })
      .catch(() => setShowFailure(true))
      .finally(() => setInProgress(false))
  }

  useEffect(() => {
    if (showSuccess) {
      void delay(() => setShowSuccess(false), 2000).then(onSuccess)
    }
  }, [showSuccess])

  useEffect(() => {
    if (showFailure) {
      void delay(() => setShowFailure(false), 2000)
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
      className={classNames(className, {
        primary: !!primary && !showIcon,
        disabled
      })}
      disabled={disabled || showIcon}
      onClick={callback}
      {...props}
    >
      <Content>
        <IconContainer
          style={{
            width: container.x.interpolate((x) => `${32 * x}px`),
            paddingRight: container.x.interpolate((x) => `${8 * x}px`)
          }}
        >
          <Spinner style={spinner} />
          <IconWrapper
            style={{
              opacity: checkmark.opacity,
              transform: checkmark.opacity.interpolate(
                (x) => `scale(${x ?? 0})`
              )
            }}
          >
            <FontAwesomeIcon icon={faCheck} color={colors.primary} />
          </IconWrapper>
          <IconWrapper
            style={{
              opacity: cross.opacity,
              transform: cross.opacity.interpolate((x) => `scale(${x ?? 0})`)
            }}
          >
            <FontAwesomeIcon icon={faTimes} color={colors.accents.red} />
          </IconWrapper>
        </IconContainer>
        <span>
          {inProgress ? textInProgress : showSuccess ? textDone : text}
        </span>
      </Content>
    </StyledButton>
  )
})

function delay<T>(cb: () => T, ms: number): Promise<T> {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(cb())
    }, ms)
  })
}

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
  width: 24px;
  height: 24px;

  border: 2px solid ${colors.greyscale.lighter};
  border-left-color: ${colors.primary};
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
