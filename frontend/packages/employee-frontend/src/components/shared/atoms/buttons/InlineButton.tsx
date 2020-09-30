// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import Colors, { Greyscale } from 'components/shared/Colors'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import { DefaultMargins } from 'components/shared/layout/white-space'
import classNames from 'classnames'
import { defaultButtonTextStyle } from 'components/shared/atoms/buttons/button-commons'
import { BaseProps } from 'components/shared/utils'

const StyledButton = styled.button`
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
    color: ${Colors.primaryHover};
  }

  &:active {
    color: ${Colors.primaryActive};
  }

  &.disabled {
    color: ${Greyscale.medium};
    cursor: not-allowed;
  }

  svg {
    margin-right: ${DefaultMargins.xs};
  }

  ${defaultButtonTextStyle}
`

interface InlineButtonProps extends BaseProps {
  onClick: () => unknown
  text: string

  icon?: IconDefinition
  disabled?: boolean
}

function InlineButton({
  className,
  dataQa,
  onClick,
  text,
  icon,
  disabled = false
}: InlineButtonProps) {
  return (
    <StyledButton
      className={classNames(className, { disabled })}
      data-qa={dataQa}
      onClick={onClick}
      disabled={disabled}
    >
      {icon && <FontAwesomeIcon icon={icon} />}
      <span>{text}</span>
    </StyledButton>
  )
}

export default InlineButton
