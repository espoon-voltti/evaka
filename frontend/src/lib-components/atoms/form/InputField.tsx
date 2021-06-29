// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { RefObject, useState } from 'react'
import styled from 'styled-components'
import classNames from 'classnames'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { IconProp } from '@fortawesome/fontawesome-svg-core'
import { faTimes } from 'lib-icons'
import { defaultMargins } from '../../white-space'
import { BaseProps } from '../../utils'
import UnderRowStatusIcon, { InfoStatus } from '../StatusIcon'
import { tabletMin } from '../../breakpoints'

const Wrapper = styled.div`
  display: inline-block;
  min-width: 0; // needed for correct overflow behavior
`

type InputWidth = 'xs' | 's' | 'm' | 'L' | 'XL' | 'full'

const inputWidths: Record<InputWidth, string> = {
  xs: '60px',
  s: '120px',
  m: '240px',
  L: '480px',
  XL: '720px',
  full: '100%'
}

interface StyledInputProps {
  width: InputWidth
  clearable: boolean
  align?: 'left' | 'right'
}
const StyledInput = styled.input<StyledInputProps>`
  width: ${(p) => inputWidths[p.width]};

  @media (max-width: ${tabletMin}) {
    ${(p) => (p.width === 'L' || p.width === 'XL' ? 'width: 100%;' : '')}
  }

  @media (max-width: 700px) {
    ${(p) => (p.width === 'XL' ? 'width: 100%; min-width: 100%;' : '')}
  }

  border-style: none none solid none;
  border-width: 1px;
  border-color: ${({ theme: { colors } }) => colors.greyscale.medium};
  border-radius: 2px;
  outline: none;
  box-sizing: border-box;
  text-align: ${(p) => p.align ?? 'left'};
  background-color: ${({ theme: { colors } }) => colors.greyscale.white};

  font-size: 1rem;
  color: ${({ theme: { colors } }) => colors.greyscale.darkest};
  padding: 6px ${(p) => (p.clearable ? '36px' : '12px')} 6px 12px;

  &::placeholder {
    color: ${({ theme: { colors } }) => colors.greyscale.dark};
    font-size: 15px;
    font-family: 'Open Sans', 'Arial', sans-serif;
  }

  &:focus {
    border-width: 2px;
    border-style: solid;
    border-color: ${({ theme: { colors } }) => colors.accents.petrol};
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
    border-color: ${({ theme: { colors } }) => colors.accents.green};
  }

  &.warning {
    border-color: ${({ theme: { colors } }) => colors.accents.orange};
  }

  &:read-only {
    border-bottom-style: dotted;
    color: ${({ theme: { colors } }) => colors.greyscale.dark};
    background: none;
  }
`

export const InputRow = styled.div`
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
  color: ${({ theme: { colors } }) => colors.greyscale.medium};

  &:hover {
    color: ${({ theme: { colors } }) => colors.greyscale.dark};
  }
`

export const InputFieldUnderRow = styled.div`
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
  width: fit-content;

  color: ${({ theme: { colors } }) => colors.greyscale.dark};

  &.success {
    color: ${({ theme: { colors } }) => colors.accents.greenDark};
  }

  &.warning {
    color: ${({ theme: { colors } }) => colors.accents.orangeDark};
  }
`

const Symbol = styled.span`
  margin-left: ${defaultMargins.xxs};
`

export type InputInfo = {
  text: string
  status?: InfoStatus
}

interface TextInputProps extends BaseProps {
  value: string
  onChange?: (value: string) => void
  onFocus?: (e: React.FocusEvent<HTMLInputElement>) => void
  onBlur?: (e: React.FocusEvent<HTMLInputElement>) => void
  onKeyPress?: (e: React.KeyboardEvent) => void
  readonly?: boolean
  width?: InputWidth

  placeholder?: string
  info?: InputInfo
  clearable?: boolean
  align?: 'left' | 'right'
  icon?: IconProp
  symbol?: string
  type?: string
  min?: number
  max?: number
  step?: number
  id?: string
  'data-qa'?: string
  name?: string
  'aria-describedby'?: string
  hideErrorsBeforeTouched?: boolean
  required?: boolean
  inputRef?: RefObject<HTMLInputElement>
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
  'data-qa': dataQa,
  className,
  icon,
  symbol,
  type,
  min,
  max,
  step,
  hideErrorsBeforeTouched,
  id,
  inputRef,
  'aria-describedby': ariaId,
  required
}: TextInputProps) {
  const [touched, setTouched] = useState(false)

  const hideError =
    hideErrorsBeforeTouched && !touched && info?.status === 'warning'
  const infoText = hideError ? undefined : info?.text
  const infoStatus = hideError ? undefined : info?.status

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
          onBlur={(e) => {
            setTouched(true)
            onBlur && onBlur(e)
          }}
          onKeyPress={onKeyPress}
          placeholder={placeholder}
          readOnly={readonly}
          disabled={readonly}
          width={width}
          clearable={clearable}
          align={align}
          className={classNames(className, infoStatus)}
          data-qa={dataQa}
          type={type}
          min={min}
          max={max}
          step={step}
          id={id}
          aria-describedby={ariaId}
          required={required ?? false}
          ref={inputRef}
        />
        {clearable && (
          <InputIcon onClick={() => onChange && onChange('')}>
            <FontAwesomeIcon icon={faTimes} />
          </InputIcon>
        )}
        {!clearable && icon && (
          <InputIcon>
            <FontAwesomeIcon icon={icon} />
          </InputIcon>
        )}
        {!clearable && !icon && symbol ? <Symbol>{symbol}</Symbol> : null}
      </InputRow>
      {infoText && (
        <InputFieldUnderRow className={classNames(infoStatus)}>
          <span data-qa={`${dataQa ?? ''}-info`}>{infoText}</span>
          <UnderRowStatusIcon status={info?.status} />
        </InputFieldUnderRow>
      )}
    </Wrapper>
  )
}

export default InputField
