// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import classNames from 'classnames'
import { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import colors, { greyscale } from '../../colors'
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
    color: ${colors.primaryHover};
  }

  &:active {
    color: ${colors.primaryActive};
  }

  &:focus {
    outline: 2px solid ${colors.accents.petrol};
    outline-offset: 2px;
  }

  &.disabled {
    color: ${greyscale.medium};
    cursor: not-allowed;
  }

  svg {
    margin-right: ${defaultMargins.xs};
    font-size: 1.25em;
  }

  ${defaultButtonTextStyle}
  color: ${(p) => p.color ?? colors.primary};
`

interface InlineButtonProps extends BaseProps {
  onClick: () => unknown
  text: string
  altText?: string
  color?: string
  icon?: IconDefinition
  disabled?: boolean
}

function InlineButton({
  className,
  dataQa,
  onClick,
  text,
  altText,
  icon,
  disabled = false,
  color
}: InlineButtonProps) {
  return (
    <StyledButton
      className={classNames(className, { disabled })}
      data-qa={dataQa}
      onClick={onClick}
      disabled={disabled}
      aria-label={altText}
      color={color}
    >
      {icon && <FontAwesomeIcon icon={icon} />}
      <span>{text}</span>
    </StyledButton>
  )
}

export default InlineButton
