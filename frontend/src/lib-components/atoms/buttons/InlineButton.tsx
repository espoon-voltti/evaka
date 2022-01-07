// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import React, { ReactNode } from 'react'
import styled from 'styled-components'
import { BaseProps } from '../../utils'
import { defaultMargins } from '../../white-space'
import { defaultButtonTextStyle } from './button-commons'

const StyledButton = styled.button<{ color?: string; iconRight?: boolean }>`
  width: fit-content;
  display: inline-block;

  border: none;
  padding: 0;
  margin: 0;
  border-radius: 2px;
  background: none;

  outline: none;
  cursor: pointer;

  &:hover {
    color: ${({ theme: { colors } }) => colors.main.primaryHover};
  }

  &:active {
    color: ${({ theme: { colors } }) => colors.main.primaryActive};
  }

  &:focus {
    outline: 2px solid ${({ theme: { colors } }) => colors.main.primaryFocus};
    outline-offset: 2px;
  }

  &.disabled {
    color: ${({ theme: { colors } }) => colors.greyscale.dark};
    cursor: not-allowed;
  }

  &.darker:not(.disabled) {
    color: ${({ theme: { colors } }) => colors.main.dark};
  }

  svg {
    ${({ iconRight }) =>
      iconRight ? 'margin-left' : 'margin-right'}: ${defaultMargins.xs};
    font-size: 1.25em;
  }

  ${({ theme }) => defaultButtonTextStyle(theme)}
  color: ${(p) => p.color ?? p.theme.colors.main.primary};
`

export interface InlineButtonProps extends BaseProps {
  onClick: () => unknown
  text: ReactNode
  altText?: string
  color?: string
  icon?: IconDefinition
  disabled?: boolean
  darker?: boolean
  iconRight?: boolean
}

export default React.memo(function InlineButton({
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
}: InlineButtonProps) {
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
