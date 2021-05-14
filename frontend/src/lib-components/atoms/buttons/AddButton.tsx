// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import classNames from 'classnames'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faPlus } from 'lib-icons'
import { defaultMargins } from '../../white-space'
import { BaseProps } from '../../utils'
import { defaultButtonTextStyle } from './button-commons'

const StyledButton = styled.button`
  width: fit-content;

  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  padding: 0;

  background: none;
  border: none;
  outline: none;
  cursor: pointer;

  &.disabled {
    color: ${({ theme: { colors } }) => colors.greyscale.dark};
    cursor: not-allowed;

    .icon-wrapper-inner {
      background: ${({ theme: { colors } }) => colors.greyscale.dark};
    }
  }

  &.darker:not(.disabled) {
    color: ${({ theme: { colors } }) => colors.main.primaryActive};

    .icon-wrapper-inner {
      background: ${({ theme: { colors } }) => colors.main.primaryActive};
    }
  }

  .icon-wrapper-outer {
    height: 43px !important;
    width: 43px !important;
    display: flex;
    justify-content: center;
    align-items: center;
    box-sizing: border-box;
    margin: -4px ${defaultMargins.s} -4px -4px;
  }

  &:focus .icon-wrapper-outer {
    border: 2px solid ${({ theme: { colors } }) => colors.accents.petrol};
    border-radius: 100%;
  }

  .icon-wrapper-inner {
    height: 35px !important;
    width: 35px !important;
    display: flex;
    justify-content: center;
    align-items: center;

    font-size: 18px;
    color: ${({ theme: { colors } }) => colors.greyscale.white};
    font-weight: normal;
    background: ${({ theme: { colors } }) => colors.main.primary};
    border-radius: 100%;
  }

  &.flipped {
    flex-direction: row-reverse;

    .icon-wrapper-outer {
      margin: -4px -4px -4px ${defaultMargins.s};
    }
  }

  ${({ theme }) => defaultButtonTextStyle(theme)}
`

interface AddButtonProps extends BaseProps {
  text?: string
  onClick: () => unknown
  disabled?: boolean
  flipped?: boolean
  darker?: boolean
  'data-qa'?: string
}

function AddButton({
  className,
  'data-qa': dataQa,
  text,
  onClick,
  disabled = false,
  flipped = false,
  darker = false
}: AddButtonProps) {
  return (
    <StyledButton
      className={classNames(className, { disabled, flipped, darker })}
      data-qa={dataQa}
      onClick={onClick}
      disabled={disabled}
    >
      <div className="icon-wrapper-outer">
        <div className="icon-wrapper-inner">
          <FontAwesomeIcon icon={faPlus} />
        </div>
      </div>
      {text && <span>{text}</span>}
    </StyledButton>
  )
}

const FlexRowRight = styled.div`
  display: flex;
  justify-content: flex-end;
`

export function AddButtonRow(props: AddButtonProps) {
  return (
    <FlexRowRight>
      <AddButton flipped {...props} />
    </FlexRowRight>
  )
}

export default AddButton
