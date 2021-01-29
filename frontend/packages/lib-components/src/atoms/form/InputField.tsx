// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import classNames from 'classnames'
import TextareaAutosize from 'react-autosize-textarea'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { IconProp } from '@fortawesome/fontawesome-svg-core'
import { faTimes } from '@evaka/lib-icons'
import colors from '../../colors'
import { defaultMargins } from '../../white-space'
import { BaseProps } from '../../utils'
import UnderRowStatusIcon, { InfoStatus } from '../StatusIcon'

const Wrapper = styled.div`
  min-width: 0; // needed for correct overflow behavior
`

type InputWidth = 'xs' | 's' | 'm' | 'L' | 'full'

const inputWidths: Record<InputWidth, string> = {
  xs: '60px',
  s: '120px',
  m: '240px',
  L: '480px',
  full: '100%'
}

interface StyledInputProps {
  width: InputWidth
  clearable: boolean
  align?: 'left' | 'right'
}
const StyledInput = styled.input<StyledInputProps>`
  width: ${(p) => inputWidths[p.width]};
  border-style: none none solid none;
  border-width: 1px;
  border-color: ${colors.greyscale.medium};
  border-radius: 2px;
  outline: none;
  box-sizing: border-box;
  text-align: ${(p) => p.align ?? 'left'};
  background-color: ${colors.greyscale.white};

  font-size: 15px;
  color: ${colors.greyscale.darkest};
  padding: 6px ${(p) => (p.clearable ? '36px' : '12px')} 6px 12px;

  &::placeholder {
    color: ${colors.greyscale.dark};
    font-size: 15px;
    font-family: 'Open Sans', 'Arial', sans-serif;
  }

  &:focus {
    border-width: 2px;
    border-style: solid;
    border-color: ${colors.accents.petrol};
    margin-top: -2px;
    margin-bottom: -1px;
    padding-${(p) => (p.align === 'right' ? 'right' : 'left')}: 10px;
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
    border-color: ${colors.accents.green};
  }

  &.warning {
    border-color: ${colors.accents.orange};
  }

  &:read-only {
    border-bottom-style: dotted;
    color: ${colors.greyscale.dark};
    background: none;
  }
`

const InputRow = styled.div`
  position: relative;
  width: 100%;
`

const InputIcon = styled.div`
  font-size: 20px;
  position: absolute;
  right: 8px;
  top: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: ${colors.greyscale.medium};

  &:hover {
    color: ${colors.greyscale.dark};
  }
`

const InputFieldUnderRow = styled.div`
  height: 16px;
  padding: 0 12px;
  margin-top: ${defaultMargins.xxs};
  margin-bottom: -20px;
  display: flex;
  align-items: center;
  justify-content: flex-start;

  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;

  color: ${colors.greyscale.dark};

  &.success {
    color: ${colors.accents.greenDark};
  }

  &.warning {
    color: ${colors.accents.orangeDark};
  }
`

interface TextInputProps extends BaseProps {
  value: string
  onChange?: (value: string) => void
  onFocus?: (e: React.FocusEvent<HTMLInputElement>) => void
  onBlur?: (e: React.FocusEvent<HTMLInputElement>) => void
  onKeyPress?: (e: React.KeyboardEvent) => void
  readonly?: boolean
  width?: InputWidth

  placeholder?: string
  info?: {
    text: string
    status?: InfoStatus
  }
  clearable?: boolean
  align?: 'left' | 'right'
  icon?: IconProp
  type?: string
  min?: number
  max?: number
  step?: number
  id?: string
  'data-qa'?: string
  name?: string
  'aria-describedby'?: string
}

function InputField({
  value,
  onChange,
  onFocus,
  onBlur,
  onKeyPress,
  readonly,
  width = 'full',
  placeholder,
  info,
  clearable = false,
  align,
  dataQa,
  className,
  icon,
  type,
  min,
  max,
  step,
  id,
  'data-qa': dataQa2,
  'aria-describedby': ariaId
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
          onBlur={onBlur}
          onKeyPress={onKeyPress}
          placeholder={placeholder}
          readOnly={readonly}
          disabled={readonly}
          width={width}
          clearable={clearable}
          align={align}
          className={classNames(className, info?.status)}
          data-qa={dataQa2 ?? dataQa}
          type={type}
          min={min}
          max={max}
          step={step}
          id={id}
          aria-describedby={ariaId}
        />
        {clearable && !icon && (
          <InputIcon onClick={() => onChange && onChange('')}>
            <FontAwesomeIcon icon={faTimes} />
          </InputIcon>
        )}
        {icon && !clearable && (
          <InputIcon>
            <FontAwesomeIcon icon={icon} />
          </InputIcon>
        )}
      </InputRow>
      {info && (
        <InputFieldUnderRow className={classNames(info.status)}>
          <span>{info.text}</span>
          <UnderRowStatusIcon status={info?.status} />
        </InputFieldUnderRow>
      )}
    </Wrapper>
  )
}

export const TextArea = styled(TextareaAutosize)`
  font-family: Open Sans, Arial, sans-serif;
  align-items: center;
  border: 1px solid transparent;
  font-size: 1rem;
  justify-content: flex-start;
  line-height: 1.5;
  padding: calc(0.5em - 1px) calc(0.625em - 1px);
  position: relative;
  border-color: #9e9e9e;
  color: #0f0f0f;
  display: block;
  box-shadow: none;
  max-width: 100%;
  width: 100%;
  min-height: 2.5em;
  border-radius: 0;
  border-width: 0 0 1px;
  background-color: transparent;
  padding-bottom: calc(0.5em - 1px);
  overflow: hidden;
  overflow-wrap: break-word;
  resize: none;
  height: 38px;
`

export default InputField
