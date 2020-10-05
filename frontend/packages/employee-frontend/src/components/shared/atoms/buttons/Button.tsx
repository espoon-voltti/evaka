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

export const StyledButton = styled.button`
  height: 45px;
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

interface ButtonProps extends BaseProps {
  onClick: () => unknown
  text: string
  primary?: boolean
  disabled?: boolean
  type?: 'submit' | 'button'
}

function Button({
  className,
  dataQa,
  onClick,
  text,
  primary = false,
  disabled = false,
  type = 'button'
}: ButtonProps) {
  const [ignoreClick, setIgnoreClick] = React.useState(false)
  const [, , startUnignoreClickTimer] = useTimeoutFn(() => {
    if (ignoreClick) {
      setIgnoreClick(false)
    }
  }, 300)

  const handleOnClick = () => {
    if (!ignoreClick) {
      setIgnoreClick(true)
      startUnignoreClickTimer()
      if (onClick) {
        onClick()
      }
    }
  }
  return (
    <StyledButton
      className={classNames(className, { primary, disabled })}
      data-qa={dataQa}
      onClick={handleOnClick}
      disabled={disabled}
      type={type}
    >
      {text}
    </StyledButton>
  )
}

export default Button
