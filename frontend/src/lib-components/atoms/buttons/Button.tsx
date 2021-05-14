// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import classNames from 'classnames'
import { tabletMin } from '../../breakpoints'
import { BaseProps } from '../../utils'
import { defaultButtonTextStyle } from './button-commons'

export const StyledButton = styled.button`
  min-height: 45px;
  padding: 0 27px;
  min-width: 100px;

  display: block;
  text-align: center;
  overflow-x: hidden;

  border: 1px solid ${({ theme: { colors } }) => colors.main.primary};
  border-radius: 2px;
  background: ${({ theme: { colors } }) => colors.greyscale.white};

  outline: none;
  cursor: pointer;
  &.disabled {
    cursor: not-allowed;
  }

  &:focus {
    outline: 2px solid ${({ theme: { colors } }) => colors.accents.petrol};
    outline-offset: 2px;
  }

  &:hover {
    color: ${({ theme: { colors } }) => colors.main.primaryHover};
    border-color: ${({ theme: { colors } }) => colors.main.primaryHover};
  }

  &:active {
    color: ${({ theme: { colors } }) => colors.main.primaryActive};
    border-color: ${({ theme: { colors } }) => colors.main.primaryActive};
  }

  &.disabled {
    color: ${({ theme: { colors } }) => colors.greyscale.dark};
    border-color: ${({ theme: { colors } }) => colors.greyscale.dark};
  }

  &.primary {
    color: ${({ theme: { colors } }) => colors.greyscale.white};
    background: ${({ theme: { colors } }) => colors.main.primary};

    &:hover {
      background: ${({ theme: { colors } }) => colors.main.primaryHover};
    }

    &:active {
      background: ${({ theme: { colors } }) => colors.main.primaryActive};
    }

    &.disabled {
      border-color: ${({ theme: { colors } }) => colors.greyscale.medium};
      background: ${({ theme: { colors } }) => colors.greyscale.medium};
    }
  }

  @media (min-width: ${tabletMin}) {
    width: fit-content;
  }

  ${({ theme }) => defaultButtonTextStyle(theme)}
  letter-spacing: 0.2px;
`

interface ButtonProps extends BaseProps {
  onClick?: (e: React.MouseEvent) => unknown
  text: string
  primary?: boolean
  disabled?: boolean
  type?: 'submit' | 'button'
  'data-qa'?: string
}

function Button({
  className,
  'data-qa': dataQa,
  onClick,
  text,
  primary = false,
  disabled = false,
  type = 'button'
}: ButtonProps) {
  const [ignoreClick, setIgnoreClick] = React.useState(false)
  React.useEffect(() => {
    if (ignoreClick) {
      const id = setTimeout(() => setIgnoreClick(false), 300)
      return () => clearTimeout(id)
    }
    return undefined
  }, [ignoreClick])

  const handleOnClick = (e: React.MouseEvent) => {
    if (!ignoreClick) {
      setIgnoreClick(true)
      if (onClick) {
        onClick(e)
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
