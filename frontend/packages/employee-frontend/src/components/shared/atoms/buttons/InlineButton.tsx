// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import Colors, { Greyscale } from 'components/shared/Colors'
import { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import { DefaultMargins } from 'components/shared/layout/white-space'
import classNames from 'classnames'
import { defaultButtonTextStyle } from 'components/shared/atoms/buttons/button-commons'
import { BaseProps } from 'components/shared/utils'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import UnderRowStatusIcon, {
  InfoStatus
} from 'components/shared/UnderRowStatusIcon'

const Wrapper = styled.div`
  min-width: 0; // needed for correct overflow behavior
`

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

const InlineButtonUnderRow = styled.div`
  height: 16px;
  padding: 0 12px;
  margin-top: ${DefaultMargins.xxs};
  margin-bottom: -20px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  position: relative;
  left: -50%;

  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;

  color: ${Colors.greyscale.dark};

  &.success {
    color: ${Colors.accents.greenDark};
  }

  &.warning {
    color: ${Colors.accents.orangeDark};
  }
`

const UnderRowWrapper = styled.div`
  position: absolute;
  left: 50%;
`

interface InlineButtonProps extends BaseProps {
  onClick: () => unknown
  text: string

  icon?: IconDefinition
  disabled?: boolean
  info?: {
    text: string
    status?: InfoStatus
  }
}

function InlineButton({
  className,
  dataQa,
  onClick,
  text,
  icon,
  disabled = false,
  info = undefined
}: InlineButtonProps) {
  return (
    <Wrapper>
      <StyledButton
        className={classNames(className, { disabled })}
        data-qa={dataQa}
        onClick={onClick}
        disabled={disabled}
      >
        {icon && <FontAwesomeIcon icon={icon} />}
        <span>{text}</span>
      </StyledButton>
      {info && (
        <UnderRowWrapper>
          <InlineButtonUnderRow className={classNames(info.status)}>
            <span>{info.text}</span>
            <UnderRowStatusIcon status={info?.status} />
          </InlineButtonUnderRow>
        </UnderRowWrapper>
      )}
    </Wrapper>
  )
}

export default InlineButton
