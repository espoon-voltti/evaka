// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { IconDefinition } from '@fortawesome/fontawesome-common-types'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import type { ReactNode } from 'react'
import React from 'react'
import styled from 'styled-components'

import { faPlus } from 'lib-icons'

import { fontWeights } from '../../typography'
import type { BaseProps } from '../../utils'
import { defaultMargins } from '../../white-space'

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
    color: ${(p) => p.theme.colors.grayscale.g70};
    cursor: not-allowed;

    .icon-wrapper-inner {
      background: ${(p) => p.theme.colors.grayscale.g70};
    }
  }

  &.darker:not(.disabled) {
    color: ${(p) => p.theme.colors.main.m2Active};

    .icon-wrapper-inner {
      background: ${(p) => p.theme.colors.main.m2Active};
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
    border: 2px solid ${(p) => p.theme.colors.main.m2Focus};
    border-radius: 100%;
  }

  .icon-wrapper-inner {
    height: 35px !important;
    width: 35px !important;
    display: flex;
    justify-content: center;
    align-items: center;

    font-size: 18px;
    color: ${(p) => p.theme.colors.grayscale.g0};
    font-weight: ${fontWeights.normal};
    background: ${(p) => p.theme.colors.main.m2};
    border-radius: 100%;
  }

  &.flipped {
    flex-direction: row-reverse;

    .icon-wrapper-outer {
      margin: -4px -4px -4px ${defaultMargins.s};
    }
  }

  ${defaultButtonTextStyle};
`

export interface AddButtonProps extends BaseProps {
  text?: ReactNode
  onClick: () => unknown
  disabled?: boolean
  flipped?: boolean
  darker?: boolean
  'data-qa'?: string
  icon?: IconDefinition
}

const AddButton = React.memo(function AddButton({
  className,
  'data-qa': dataQa,
  text,
  onClick,
  disabled = false,
  flipped = false,
  darker = false,
  icon
}: AddButtonProps) {
  return (
    <StyledButton
      type="button"
      className={classNames(className, { disabled, flipped, darker })}
      data-qa={dataQa}
      onClick={onClick}
      disabled={disabled}
    >
      <div className="icon-wrapper-outer">
        <div className="icon-wrapper-inner">
          <FontAwesomeIcon icon={icon ?? faPlus} />
        </div>
      </div>
      {typeof text === 'string' || typeof text === 'number' ? (
        <span>{text}</span>
      ) : (
        text
      )}
    </StyledButton>
  )
})

const FlexRowRight = styled.div`
  display: flex;
  justify-content: flex-end;
`

export const AddButtonRow = React.memo(function AddButtonRow(
  props: AddButtonProps
) {
  return (
    <FlexRowRight>
      <AddButton flipped {...props} />
    </FlexRowRight>
  )
})

export default AddButton
