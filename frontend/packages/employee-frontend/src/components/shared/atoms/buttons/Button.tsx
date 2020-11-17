// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { useTimeoutFn } from 'react-use'
import Colors, { Greyscale } from 'components/shared/Colors'
import classNames from 'classnames'
import { defaultButtonTextStyle } from 'components/shared/atoms/buttons/button-commons'
import { BaseProps } from 'components/shared/utils'
import { DefaultMargins } from 'components/shared/layout/white-space'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { fasCheckCircle, fasExclamationTriangle } from 'icon-set'

const Wrapper = styled.div`
  min-width: 0; // needed for correct overflow behavior
`

export const StyledButton = styled.button`
  min-height: 45px;
  padding: 0 27px;
  width: fit-content;
  min-width: 100px;

  display: block;
  text-align: center;
  overflow-x: hidden;

  border: 1px solid ${Colors.primary};
  border-radius: 2px;
  background: none;

  outline: none;
  cursor: pointer;
  &.disabled {
    cursor: not-allowed;
  }

  &:focus {
    box-shadow: 0px 0px 0 1px ${Colors.accents.petrol};
  }

  &:hover {
    color: ${Colors.primaryHover};
    border-color: ${Colors.primaryHover};
  }

  &:active {
    color: ${Colors.primaryActive};
    border-color: ${Colors.primaryActive};
  }

  &.disabled {
    color: ${Greyscale.medium};
    border-color: ${Greyscale.medium};
  }

  &.primary {
    color: ${Greyscale.white};
    background: ${Colors.primary};

    &:hover {
      background: ${Colors.primaryHover};
    }

    &:active {
      background: ${Colors.primaryActive};
    }

    &.disabled {
      background: ${Greyscale.medium};
    }
  }

  ${defaultButtonTextStyle}
`

const UnderRow = styled.div`
  padding: 0 12px;
  margin-top: ${DefaultMargins.xxs};
  margin-bottom: -20px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  position: absolute;

  font-size: 12px;
  text-overflow: ellipsis;

  word-wrap:break-word;
  width: 120px;
  white-space: normal

  color: ${Colors.greyscale.dark};

  &.success {
    color: ${Colors.accents.greenDark};
  }

  &.warning {
    color: ${Colors.accents.orangeDark};
  }
`

const StatusIcon = styled.div`
  font-size: 15px;
  margin-left: ${DefaultMargins.xs};
`

export type InfoStatus = 'warning' | 'success'

interface ButtonProps extends BaseProps {
  onClick?: (e: React.MouseEvent) => unknown
  text: string
  primary?: boolean
  disabled?: boolean
  type?: 'submit' | 'button'
  'data-qa'?: string
  info?: {
    text: string
    status?: InfoStatus
  }
}

function Button({
  className,
  dataQa,
  'data-qa': dataQa2,
  onClick,
  text,
  primary = false,
  disabled = false,
  type = 'button',
  info
}: ButtonProps) {
  const [ignoreClick, setIgnoreClick] = React.useState(false)
  const [, , startUnignoreClickTimer] = useTimeoutFn(() => {
    if (ignoreClick) {
      setIgnoreClick(false)
    }
  }, 300)

  const handleOnClick = (e: React.MouseEvent) => {
    if (!ignoreClick) {
      setIgnoreClick(true)
      startUnignoreClickTimer()
      if (onClick) {
        onClick(e)
      }
    }
  }
  return (
    <Wrapper>
      <StyledButton
        className={classNames(className, { primary, disabled })}
        data-qa={dataQa2 ?? dataQa}
        onClick={handleOnClick}
        disabled={disabled}
        type={type}
      >
        {text}
      </StyledButton>
      <UnderRow className={classNames(info?.status)}>
        <span>{info?.text}</span>
        <StatusIcon>
          {info?.status === 'warning' && (
            <FontAwesomeIcon
              icon={fasExclamationTriangle}
              color={Colors.accents.orange}
            />
          )}
          {info?.status === 'success' && (
            <FontAwesomeIcon
              icon={fasCheckCircle}
              color={Colors.accents.green}
            />
          )}
        </StatusIcon>
      </UnderRow>
    </Wrapper>
  )
}

export default Button
