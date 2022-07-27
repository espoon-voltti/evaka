// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import React from 'react'
import styled from 'styled-components'

import type { BaseProps } from '../../utils'
import type { IconSize } from '../icon-size'

interface ButtonProps {
  size: IconSize | undefined
  gray?: boolean
  white?: boolean
  color?: string
}

const StyledButton = styled.button<ButtonProps>`
  display: flex;
  justify-content: center;
  align-items: center;
  box-sizing: border-box;
  width: calc(
    12px +
      ${(props: ButtonProps) => {
        switch (props.size) {
          case 's':
            return '20px'
          case 'm':
            return '24px'
          case 'L':
            return '34px'
          case 'XL':
            return '64px'
          default:
            return '20px'
        }
      }}
  );
  height: calc(
    12px +
      ${(props: ButtonProps) => {
        switch (props.size) {
          case 's':
            return '20px'
          case 'm':
            return '24px'
          case 'L':
            return '34px'
          case 'XL':
            return '64px'
          default:
            return '20px'
        }
      }}
  );
  font-size: ${(props: ButtonProps) => {
    switch (props.size) {
      case 's':
        return '20px'
      case 'm':
        return '24px'
      case 'L':
        return '34px'
      case 'XL':
        return '64px'
      default:
        return '20px'
    }
  }};
  color: ${(p) =>
    p.color
      ? p.color
      : p.gray
      ? p.theme.colors.grayscale.g70
      : p.white
      ? p.theme.colors.grayscale.g0
      : p.theme.colors.main.m2};
  border: none;
  border-radius: 100%;
  background: none;
  outline: none;
  cursor: pointer;
  padding: 0;
  margin: -6px;

  &:focus {
    border: 2px solid ${(p) => p.theme.colors.main.m2Focus};
  }

  .icon-wrapper {
    display: flex;
    justify-content: center;
    align-items: center;
    margin: 2px;
  }

  &:hover .icon-wrapper {
    color: ${(p) =>
      p.color
        ? p.color
        : p.gray
        ? p.theme.colors.grayscale.g70
        : p.white
        ? p.theme.colors.grayscale.g0
        : p.theme.colors.main.m2Hover};
  }

  &:active .icon-wrapper {
    color: ${(p) =>
      p.color
        ? p.color
        : p.gray
        ? p.theme.colors.grayscale.g100
        : p.theme.colors.main.m2Active};
  }

  &.disabled {
    cursor: not-allowed;

    .icon-wrapper {
      color: ${(p) => p.theme.colors.grayscale.g35};
    }
  }
`

export interface IconButtonProps extends BaseProps {
  onClick?: (e: React.MouseEvent<HTMLButtonElement>) => void
  icon: IconDefinition
  altText?: string
  disabled?: boolean
  size?: IconSize
  gray?: boolean
  white?: boolean
  'data-qa'?: string
}

export default React.memo(function IconButton({
  className,
  'data-qa': dataQa,
  icon,
  altText,
  onClick,
  disabled,
  size,
  gray,
  white,
  color,
  ...props
}: IconButtonProps & React.ButtonHTMLAttributes<HTMLButtonElement>) {
  return (
    <StyledButton
      type="button"
      className={classNames(className, { disabled })}
      data-qa={dataQa}
      onClick={onClick}
      disabled={disabled}
      aria-label={altText}
      size={size}
      gray={gray}
      white={white}
      color={color}
      {...props}
    >
      <div className="icon-wrapper">
        <FontAwesomeIcon icon={icon} />
      </div>
    </StyledButton>
  )
})
