// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import classNames from 'classnames'
import React from 'react'
import styled from 'styled-components'

import { tabletMin } from '../../breakpoints'
import { BaseProps } from '../../utils'

import {
  buttonBorderRadius,
  defaultButtonTextStyle,
  useThrottledEventHandler
} from './button-commons'

export const StyledButton = styled.button`
  min-height: 45px;
  padding: 0 24px;
  min-width: 100px;

  display: block;
  text-align: center;
  overflow-x: hidden;

  border: 1px solid ${(p) => p.theme.colors.main.m2};
  border-radius: ${buttonBorderRadius};
  background: ${(p) => p.theme.colors.grayscale.g0};

  outline: none;
  cursor: pointer;

  &.disabled {
    cursor: not-allowed;
  }

  &:focus {
    outline: 2px solid ${(p) => p.theme.colors.main.m2Focus};
    outline-offset: 2px;
  }

  &:hover {
    color: ${(p) => p.theme.colors.main.m2Hover};
    border-color: ${(p) => p.theme.colors.main.m2Hover};
  }

  &:active {
    color: ${(p) => p.theme.colors.main.m2Active};
    border-color: ${(p) => p.theme.colors.main.m2Active};
  }

  &.disabled {
    color: ${(p) => p.theme.colors.grayscale.g70};
    border-color: ${(p) => p.theme.colors.grayscale.g70};
  }

  &.primary {
    color: ${(p) => p.theme.colors.grayscale.g0};
    background: ${(p) => p.theme.colors.main.m2};

    &:hover {
      background: ${(p) => p.theme.colors.main.m2Hover};
    }

    &:active {
      background: ${(p) => p.theme.colors.main.m2Active};
    }

    &.disabled {
      border-color: ${(p) => p.theme.colors.grayscale.g35};
      background: ${(p) => p.theme.colors.grayscale.g35};
    }
  }

  @media (min-width: ${tabletMin}) {
    width: fit-content;
  }

  ${defaultButtonTextStyle};
  letter-spacing: 0.2px;
`

export interface ButtonProps extends BaseProps {
  onClick?: (e: React.MouseEvent) => unknown
  children?: React.ReactNode | React.ReactNodeArray
  text?: string
  primary?: boolean
  disabled?: boolean
  type?: 'submit' | 'button'
  id?: string
}

/**
 * @deprecated use Button instead
 */
export const LegacyButton = React.memo(function LegacyButton({
  className,
  'data-qa': dataQa,
  onClick,
  primary = false,
  disabled = false,
  type = 'button',
  id,
  ...props
}: ButtonProps) {
  const handleOnClick = useThrottledEventHandler(onClick)
  return (
    <StyledButton
      className={classNames(className, { primary, disabled })}
      data-qa={dataQa}
      onClick={handleOnClick}
      disabled={disabled}
      type={type}
      id={id}
    >
      {'children' in props ? props.children : props.text}
    </StyledButton>
  )
})
