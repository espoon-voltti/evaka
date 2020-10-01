// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { BaseProps } from 'components/shared/utils'
import styled from 'styled-components'
import Colors from 'components/shared/Colors'
import classNames from 'classnames'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { fasCheckCircle, fasExclamationTriangle, faTimes } from '@evaka/icons'
import { DefaultMargins } from 'components/shared/layout/white-space'

const Wrapper = styled.div`
  min-width: 0; // needed for correct overflow behavior
`

type InputWidth = 's' | 'm' | 'L' | 'full'

const inputWidths: Record<InputWidth, string> = {
  s: '120px',
  m: '240px',
  L: '480px',
  full: '100%'
}

interface StyledInputProps {
  width: InputWidth
  clearable: boolean
}
const StyledInput = styled.input<StyledInputProps>`
  width: ${(p) => inputWidths[p.width]};
  border-style: none none solid none;
  border-width: 1px;
  border-color: ${Colors.greyscale.medium};
  border-radius: 2px;
  outline: none;
  box-sizing: border-box;

  font-size: 15px;
  color: ${Colors.greyscale.darkest};
  padding: 6px ${(p) => (p.clearable ? '36px' : '12px')} 6px 12px;

  &::placeholder {
    color: ${Colors.greyscale.dark};
    font-style: italic;
    font-size: 15px;
    font-family: 'Open Sans', 'Arial', sans-serif;
  }

  &:focus {
    border-width: 2px;
    border-style: solid;
    border-color: ${Colors.accents.petrol};
    margin-top: -2px;
    margin-bottom: -1px;
    padding-left: 10px;
  }

  &.success,
  &.warning {
    border-width: 2px;
    margin-bottom: -1px;
    &:focus {
      margin-bottom: -1px;
    }
  }

  &.success {
    border-color: ${Colors.accents.green};
  }

  &.warning {
    border-color: ${Colors.accents.orange};
  }

  &:read-only {
    border-bottom-style: dotted;
    color: ${Colors.greyscale.dark};
    background: none;
  }
`

const InputRow = styled.div`
  position: relative;
  width: 100%;
`

const ClearableIcon = styled.div`
  font-size: 20px;
  position: absolute;
  right: 8px;
  top: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: ${Colors.greyscale.medium};

  &:hover {
    color: ${Colors.greyscale.dark};
  }
`

const UnderRow = styled.div`
  height: 16px;
  padding: 0 12px;
  margin-top: ${DefaultMargins.xxs};
  margin-bottom: -20px;
  display: flex;
  align-items: center;
  justify-content: flex-start;

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

const StatusIcon = styled.div`
  font-size: 15px;
  margin-left: ${DefaultMargins.xs};
`

type InfoStatus = 'warning' | 'success'

interface TextInputProps extends BaseProps {
  value: string
  onChange?: (value: string) => void
  onFocus?: (e: React.FocusEvent<HTMLInputElement>) => void
  readonly?: boolean
  width?: InputWidth

  placeholder?: string
  info?: {
    text: string
    status?: InfoStatus
  }
  clearable?: boolean
}

function InputField({
  value,
  onChange,
  onFocus,
  readonly,
  width = 'full',
  placeholder,
  info,
  clearable = false,
  dataQa,
  className
}: TextInputProps) {
  return (
    <Wrapper>
      <InputRow>
        <StyledInput
          value={value}
          onChange={(e) => {
            e.preventDefault()
            if (onChange && !readonly) onChange(e.target.value)
          }}
          onFocus={onFocus}
          placeholder={placeholder}
          readOnly={readonly}
          disabled={readonly}
          width={width}
          clearable={clearable}
          className={classNames(className, info?.status)}
          data-qa={dataQa}
        />
        {clearable && (
          <ClearableIcon onClick={() => onChange && onChange('')}>
            <FontAwesomeIcon icon={faTimes} />
          </ClearableIcon>
        )}
      </InputRow>
      {info && (
        <UnderRow className={classNames(info.status)}>
          <span>{info.text}</span>
          <StatusIcon>
            {info.status === 'warning' && (
              <FontAwesomeIcon
                icon={fasExclamationTriangle}
                color={Colors.accents.orange}
              />
            )}
            {info.status === 'success' && (
              <FontAwesomeIcon
                icon={fasCheckCircle}
                color={Colors.accents.green}
              />
            )}
          </StatusIcon>
        </UnderRow>
      )}
    </Wrapper>
  )
}

export default InputField
