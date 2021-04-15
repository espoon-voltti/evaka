// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import classNames from 'classnames'
import { tabletMin } from '../../breakpoints'
import colors, { greyscale } from '../../colors'
import { BaseProps } from '../../utils'
import { defaultButtonTextStyle } from './button-commons'

export const StyledButton = styled.button`
  min-height: 45px;
  padding: 0 27px;
  min-width: 100px;

  display: block;
  text-align: center;
  overflow-x: hidden;

  border: 1px solid ${colors.primary};
  border-radius: 2px;
  background: ${colors.greyscale.white};

  outline: none;
  cursor: pointer;
  &.disabled {
    cursor: not-allowed;
  }

  &:focus {
    outline: 2px solid ${colors.accents.petrol};
    outline-offset: 2px;
  }

  &:hover {
    color: ${colors.primaryHover};
    border-color: ${colors.primaryHover};
  }

  &:active {
    color: ${colors.primaryActive};
    border-color: ${colors.primaryActive};
  }

  &.disabled {
    color: ${greyscale.dark};
    border-color: ${greyscale.dark};
  }

  &.primary {
    color: ${greyscale.white};
    background: ${colors.primary};

    &:hover {
      background: ${colors.primaryHover};
    }

    &:active {
      background: ${colors.primaryActive};
    }

    &.disabled {
      border-color: ${greyscale.medium};
      background: ${greyscale.medium};
    }
  }

  @media (min-width: ${tabletMin}) {
    width: fit-content;
  }

  ${defaultButtonTextStyle}
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
  dataQa,
  'data-qa': dataQa2,
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
      data-qa={dataQa2 ?? dataQa}
      onClick={handleOnClick}
      disabled={disabled}
      type={type}
    >
      {text}
    </StyledButton>
  )
}

export default Button
