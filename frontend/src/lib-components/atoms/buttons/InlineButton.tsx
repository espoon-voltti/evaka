// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import classNames from 'classnames'
import { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { defaultMargins } from '../../white-space'
import { BaseProps } from '../../utils'
import { defaultButtonTextStyle } from './button-commons'

const StyledButton = styled.button<{ color?: string }>`
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
    outline: 2px solid ${({ theme: { colors } }) => colors.accents.petrol};
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
    margin-right: ${defaultMargins.xs};
    font-size: 1.25em;
  }

  ${({ theme }) => defaultButtonTextStyle(theme)}
  color: ${(p) => p.color ?? p.theme.colors.main.primary};
`

const Margin = styled.span`
  margin-left: ${defaultMargins.s};
`

interface InlineButtonProps extends BaseProps {
  onClick: () => unknown
  text: string
  altText?: string
  color?: string
  icon?: IconDefinition
  disabled?: boolean
  darker?: boolean
  iconRight?: boolean
}

function InlineButton({
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
    >
      {icon && !iconRight && <FontAwesomeIcon icon={icon} />}
      <span>{text}</span>
      {icon && iconRight && (
        <Margin>
          <FontAwesomeIcon icon={icon} />
        </Margin>
      )}
    </StyledButton>
  )
}

export default InlineButton
