// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faPlus } from '@evaka/icons'
import Colors from 'components/shared/Colors'
import classNames from 'classnames'
import { DefaultMargins } from 'components/shared/layout/white-space'
import { defaultButtonTextStyle } from 'components/shared/atoms/buttons/button-commons'
import { BaseProps } from 'components/shared/utils'

const StyledButton = styled.button`
  width: fit-content;

  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;

  background: none;
  border: none;
  outline: none;
  cursor: pointer;

  &.disabled {
    color: ${Colors.greyscale.medium};

    .icon-wrapper {
      background: ${Colors.greyscale.medium};
    }
  }

  .icon-wrapper {
    height: 35px !important;
    width: 35px !important;
    display: flex;
    justify-content: center;
    align-items: center;

    font-size: 18px;
    color: ${Colors.greyscale.white};
    font-weight: normal;
    background: ${Colors.primary};
    border-radius: 100%;
    margin: 0 ${DefaultMargins.s} 0 0;
  }

  &.flipped {
    flex-direction: row-reverse;

    .icon-wrapper {
      margin: 0 0 0 ${DefaultMargins.s};
    }
  }

  ${defaultButtonTextStyle}
`

interface AddButtonProps extends BaseProps {
  text?: string
  onClick: () => unknown
  disabled?: boolean
  flipped?: boolean
}

function AddButton({
  className,
  dataQa,
  text,
  onClick,
  disabled = false,
  flipped = false
}: AddButtonProps) {
  return (
    <StyledButton
      className={classNames(className, { disabled, flipped })}
      data-qa={dataQa}
      onClick={onClick}
      disabled={disabled}
    >
      <div className="icon-wrapper">
        <FontAwesomeIcon icon={faPlus} />
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
