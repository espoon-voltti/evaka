// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import React, { ReactNode } from 'react'
import styled from 'styled-components'

import { BaseProps } from '../../utils'
import { defaultMargins } from '../../white-space'

import { buttonBorderRadius, defaultButtonTextStyle } from './button-commons'

const StyledButton = styled.button<{ color?: string; iconRight?: boolean }>`
  width: fit-content;
  display: inline-block;

  border: none;
  padding: 0;
  margin: 0;
  border-radius: ${buttonBorderRadius};
  background: none;

  outline: none;
  cursor: pointer;

  &:hover {
    color: ${(p) => p.theme.colors.main.m2Hover};
  }

  &:active {
    color: ${(p) => p.theme.colors.main.m2Active};
  }

  &:focus {
    outline: 2px solid ${(p) => p.theme.colors.main.m2Focus};
    outline-offset: 2px;
  }

  &.disabled {
    color: ${(p) => p.theme.colors.grayscale.g70};
    cursor: not-allowed;
  }

  &.darker:not(.disabled) {
    color: ${(p) => p.theme.colors.main.m1};
  }

  svg {
    ${({ iconRight }) =>
      iconRight ? 'margin-left' : 'margin-right'}: ${defaultMargins.xs};
    font-size: 1.25em;
  }

  ${defaultButtonTextStyle};
  color: ${(p) => p.color ?? p.theme.colors.main.m2};
`

export interface InlineButtonProps extends BaseProps {
  onClick: (e: React.MouseEvent) => unknown
  text: ReactNode
  altText?: string
  color?: string
  icon?: IconDefinition
  disabled?: boolean
  darker?: boolean
  iconRight?: boolean
}

/**
 * @deprecated use Button and appearance="inline" instead
 */
const LegacyInlineButton = React.forwardRef(function InlineButton(
  {
    className,
    'data-qa': dataQa,
    onClick,
    text,
    altText,
    icon,
    disabled = false,
    darker = false,
    color,
    iconRight
  }: InlineButtonProps,
  ref: React.ForwardedRef<HTMLButtonElement>
) {
  return (
    <StyledButton
      className={classNames(className, { disabled, darker })}
      data-qa={dataQa}
      onClick={onClick}
      disabled={disabled}
      aria-label={altText}
      color={color}
      type="button"
      iconRight={iconRight}
      ref={ref}
    >
      {icon && !iconRight && <FontAwesomeIcon icon={icon} />}
      {typeof text === 'string' || typeof text === 'number' ? (
        <span>{text}</span>
      ) : (
        text
      )}
      {icon && iconRight && <FontAwesomeIcon icon={icon} />}
    </StyledButton>
  )
})

LegacyInlineButton.displayName = 'ForwardedRef(LegacyInlineButton)'

export default LegacyInlineButton
