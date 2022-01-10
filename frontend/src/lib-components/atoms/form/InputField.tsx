// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { IconProp } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import React, { HTMLAttributes, RefObject, useState } from 'react'
import styled from 'styled-components'
import { faTimes } from 'lib-icons'
import { tabletMin } from '../../breakpoints'
import { BaseProps } from '../../utils'
import { defaultMargins } from '../../white-space'
import UnderRowStatusIcon, { InfoStatus } from '../StatusIcon'

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
export const StyledInput = styled.input<StyledInputProps>`
  width: ${(p) => inputWidths[p.width]};

  @media (max-width: ${tabletMin}) {
    ${(p) => (p.width === 'L' || p.width === 'XL' ? 'width: 100%;' : '')}
  }

  @media (max-width: 700px) {
    ${(p) => (p.width === 'XL' ? 'width: 100%; min-width: 100%;' : '')}
  }

  border-style: none none solid none;
  border-width: 1px;
  border-color: ${({ theme: { colors } }) => colors.greyscale.dark};
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
    font-family: 'Open Sans', 'Arial', sans-serif;
  }

  &:focus {
    border-width: 2px;
    border-style: solid;
    border-color: ${({ theme: { colors } }) => colors.main.primaryFocus};
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
    border-color: ${({ theme: { colors } }) => colors.accents.successGreen};
  }

  &.warning {
    border-color: ${({ theme: { colors } }) => colors.accents.warningOrange};
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
  color: ${({ theme: { colors } }) => colors.greyscale.dark};

  &:hover {
    color: ${({ theme: { colors } }) => colors.greyscale.darkest};
  }
`

export const InputFieldUnderRow = styled.div`
  height: 16px;
  margin-top: ${defaultMargins.xxs};
  margin-bottom: -20px;
  display: flex;
  align-items: center;
  justify-content: flex-start;

  font-size: 1rem;
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

export interface TextInputProps extends BaseProps {
  value: string
  onChange?: (value: string) => void
  onFocus?: (e: React.FocusEvent<HTMLInputElement>) => void
  onBlur?: (e: React.FocusEvent<HTMLInputElement>) => void
  onKeyPress?: (e: React.KeyboardEvent) => void
  readonly?: boolean
  width?: InputWidth

  autoComplete?: string
  placeholder?: string
  info?: InputInfo
  clearable?: boolean
  align?: 'left' | 'right'
  icon?: IconProp
  inputMode?: HTMLAttributes<HTMLInputElement>['inputMode']
  onKeyDown?: HTMLAttributes<HTMLInputElement>['onKeyDown']
  symbol?: string
  type?: string
  min?: number
  max?: number
  maxLength?: number
  step?: number
  id?: string
  'data-qa'?: string
  name?: string
  'aria-describedby'?: string
  hideErrorsBeforeTouched?: boolean
  required?: boolean
  autoFocus?: boolean
  inputRef?: RefObject<HTMLInputElement>
  wrapperClassName?: string
}

export default React.memo(function InputField({
  value,
  onChange,
  onFocus,
  onBlur,
  onKeyPress,
  readonly,
  width = 'full',
  placeholder,
  info,
  inputMode,
  clearable = false,
  align,
  autoComplete,
  'data-qa': dataQa,
  className,
  icon,
  symbol,
  type,
  min,
  max,
  maxLength,
  step,
  hideErrorsBeforeTouched,
  id,
  inputRef,
  'aria-describedby': ariaId,
  required,
  autoFocus,
  wrapperClassName = undefined
}: TextInputProps) {
  const [touched, setTouched] = useState(false)

  const hideError =
    hideErrorsBeforeTouched && !touched && info?.status === 'warning'
  const infoText = hideError ? undefined : info?.text
  const infoStatus = hideError ? undefined : info?.status

  return (
    <Wrapper className={wrapperClassName}>
      <InputRow>
        <StyledInput
          autoComplete={autoComplete}
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
          inputMode={inputMode}
          align={align}
          className={classNames(className, infoStatus)}
          data-qa={dataQa}
          type={type}
          min={min}
          max={max}
          maxLength={maxLength}
          step={step}
          id={id}
          aria-describedby={ariaId}
          required={required ?? false}
          ref={inputRef}
          autoFocus={autoFocus}
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
          <span data-qa={dataQa ? `${dataQa}-info` : undefined}>
            {infoText}
          </span>
          <UnderRowStatusIcon status={info?.status} />
        </InputFieldUnderRow>
      )}
    </Wrapper>
  )
})
