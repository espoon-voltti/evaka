// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { useTimeoutFn } from 'react-use'
import colors, { greyscale } from '@evaka/lib-components/src/colors'
import classNames from 'classnames'
import { defaultButtonTextStyle } from 'components/shared/atoms/buttons/button-commons'
import { BaseProps } from 'components/shared/utils'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
import UnderRowStatusIcon, {
  InfoStatus
} from 'components/shared/UnderRowStatusIcon'

const Wrapper = styled.div`
  min-width: 0; // needed for correct overflow behavior
`

export const StyledButton = styled.button`
  min-height: 45px;
  padding: 0 27px;
  width: fit-content;
  min-width: 100px;

  display: block;
  text-align: center;
  overflow-x: hidden;

  border: 1px solid ${colors.primary};
  border-radius: 2px;
  background: none;

  outline: none;
  cursor: pointer;
  &.disabled {
    cursor: not-allowed;
  }

  &:focus {
    box-shadow: 0px 0px 0 1px ${colors.accents.petrol};
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
    color: ${greyscale.medium};
    border-color: ${greyscale.medium};
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
      background: ${greyscale.medium};
    }
  }

  ${defaultButtonTextStyle}
`

const ButtonUnderRow = styled.div`
  padding: 0 12px;
  margin-top: ${defaultMargins.xxs};
  margin-bottom: -20px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  position: absolute;

  font-size: 12px;
  text-overflow: ellipsis;

  word-wrap:break-word;
  width: 120px;
  white-space: normal

  color: ${colors.greyscale.dark};

  &.success {
    color: ${colors.accents.greenDark};
  }

  &.warning {
    color: ${colors.accents.orangeDark};
  }
`

interface ButtonProps extends BaseProps {
  onClick?: (e: React.MouseEvent) => unknown
  text: string
  primary?: boolean
  disabled?: boolean
  type?: 'submit' | 'button'
  'data-qa'?: string
  info?: {
    text: string
    status?: InfoStatus
  }
}

function Button({
  className,
  dataQa,
  'data-qa': dataQa2,
  onClick,
  text,
  primary = false,
  disabled = false,
  type = 'button',
  info
}: ButtonProps) {
  const [ignoreClick, setIgnoreClick] = React.useState(false)
  const [, , startUnignoreClickTimer] = useTimeoutFn(() => {
    if (ignoreClick) {
      setIgnoreClick(false)
    }
  }, 300)

  const handleOnClick = (e: React.MouseEvent) => {
    if (!ignoreClick) {
      setIgnoreClick(true)
      startUnignoreClickTimer()
      if (onClick) {
        onClick(e)
      }
    }
  }
  return (
    <Wrapper>
      <StyledButton
        className={classNames(className, { primary, disabled })}
        data-qa={dataQa2 ?? dataQa}
        onClick={handleOnClick}
        disabled={disabled}
        type={type}
      >
        {text}
      </StyledButton>
      <ButtonUnderRow className={classNames(info?.status)}>
        <span>{info?.text}</span>
        <UnderRowStatusIcon status={info?.status} />
      </ButtonUnderRow>
    </Wrapper>
  )
}

export default Button
