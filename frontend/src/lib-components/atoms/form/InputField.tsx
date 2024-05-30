// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { IconProp } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import React, { HTMLAttributes, RefObject, useState } from 'react'
import styled, { css } from 'styled-components'

import { BoundFormState } from 'lib-common/form/hooks'
import { faTimes } from 'lib-icons'

import { tabletMin } from '../../breakpoints'
import { BaseProps } from '../../utils'
import { defaultMargins } from '../../white-space'
import UnderRowStatusIcon, { InfoStatus } from '../StatusIcon'
import { IconButton } from '../buttons/IconButton'

const inputWidths = {
  xs: '60px',
  s: '120px',
  m: '240px',
  L: '480px',
  XL: '720px',
  full: '100%'
} as const

type InputWidth = keyof typeof inputWidths

const width = (width: InputWidth) => css`
  width: ${inputWidths[width]};
  max-width: ${inputWidths[width]};

  @media (max-width: ${tabletMin}) {
    ${width === 'L' || width === 'XL'
      ? css`
          width: 100%;
          max-width: 100%;
        `
      : ''}
  }

  @media (max-width: 700px) {
    ${width === 'XL'
      ? css`
          width: 100%;
          max-width: 100%;
        `
      : ''}
  }
`

const Wrapper = styled.div<{ width: InputWidth }>`
  position: relative;
  display: inline-block;
  ${(p) => width(p.width)}
`

interface StyledInputProps {
  width: InputWidth
  align?: 'left' | 'right'
  icon?: boolean
}

export const StyledInput = styled.input<StyledInputProps>`
  ${(p) => width(p.width)}
  margin: 0;
  border: none;
  border-top: 2px solid transparent;
  border-bottom: 1px solid ${(p) => p.theme.colors.grayscale.g70};
  border-radius: 0;
  outline: none;
  text-align: ${(p) => p.align ?? 'left'};
  background-color: ${(p) => p.theme.colors.grayscale.g0};
  font-size: 1rem;
  color: ${(p) => p.theme.colors.grayscale.g100};
  padding: 6px 10px;

  ${({ icon }) =>
    icon
      ? css`
          padding-right: calc(10px + 1rem + 12px);
        `
      : ''}
  &::placeholder {
    color: ${(p) => p.theme.colors.grayscale.g70};
    font-family: 'Open Sans', 'Arial', sans-serif;
  }

  &:focus,
  &.success,
  &.warning {
    border-bottom-width: 2px;
    margin-bottom: -1px;
  }

  &:focus {
    border: 2px solid ${(p) => p.theme.colors.main.m2Focus};
    border-radius: 2px;
    padding-left: 8px;
    padding-right: 8px;
    ${({ icon }) =>
      icon
        ? css`
            padding-right: calc(8px + 1rem + 12px);
          `
        : ''}
  }

  &.success {
    border-bottom-color: ${(p) => p.theme.colors.status.success};

    &:focus {
      border-color: ${(p) => p.theme.colors.status.success};
    }
  }

  &.warning {
    border-bottom-color: ${(p) => p.theme.colors.status.warning};

    &:focus {
      border-color: ${(p) => p.theme.colors.status.warning};
    }
  }

  &:read-only {
    border-bottom-style: dashed;
    color: ${(p) => p.theme.colors.grayscale.g70};
    background: none;
  }
`

const IconContainer = styled.div<{ clickable: boolean }>`
  position: absolute;
  right: 12px;
  top: 0;
  bottom: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 1rem;

  ${(p) =>
    !p.clickable
      ? css`
          pointer-events: none;
        `
      : ''}
`

const StyledIconButton = styled(IconButton)`
  color: ${(p) => p.theme.colors.grayscale.g70};

  &:hover {
    color: ${(p) => p.theme.colors.grayscale.g100};
  }
`

export const InputFieldUnderRow = styled.div`
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
  font-size: 1rem;
  line-height: 1rem;
  margin-top: ${defaultMargins.xxs};

  color: ${(p) => p.theme.colors.grayscale.g70};

  &.success {
    color: ${(p) => p.theme.colors.accents.a1greenDark};
  }

  &.warning {
    color: ${(p) => p.theme.colors.accents.a2orangeDark};
  }
`

export type InputInfo = {
  text: string
  status?: InfoStatus
}

export interface InputProps extends BaseProps {
  value: string
  onChange?: (value: string) => void
  onChangeTarget?: (target: EventTarget & HTMLInputElement) => void
  onFocus?: (e: React.FocusEvent<HTMLInputElement>) => void
  onBlur?: (e: React.FocusEvent<HTMLInputElement>) => void
  onKeyPress?: (e: React.KeyboardEvent) => void
  readonly?: boolean
  width?: InputWidth

  autoComplete?: string
  placeholder?: string
  info?: InputInfo
  align?: 'left' | 'right'
  icon?: IconProp
  inputMode?: HTMLAttributes<HTMLInputElement>['inputMode']
  onKeyDown?: HTMLAttributes<HTMLInputElement>['onKeyDown']
  symbol?: string
  type?: string
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

interface ClearableInputProps extends OtherInputProps {
  clearable: true
  clearLabel: string
}

interface DateInputProps extends InputProps {
  type: 'date'
  min?: string
  max?: string
}

interface OtherInputProps extends InputProps {
  min?: number
  max?: number
}

export type TextInputProps =
  | OtherInputProps
  | DateInputProps
  | ClearableInputProps

const InputField = React.memo(function InputField({
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
  onChangeTarget,
  ...rest
}: TextInputProps) {
  const [touched, setTouched] = useState(false)

  const hideError =
    hideErrorsBeforeTouched && !touched && info?.status === 'warning'
  const infoText = hideError ? undefined : info?.text
  const infoStatus = hideError ? undefined : info?.status

  const clearable = 'clearable' in rest && rest.clearable

  const showIcon = !!(clearable || icon || symbol)

  return (
    <Wrapper className={className} width={width}>
      <StyledInput
        autoComplete={autoComplete}
        value={value}
        onChange={(e) => {
          e.preventDefault()
          if (!readonly) {
            onChange?.(e.target.value)
            onChangeTarget?.(e.target)
          }
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
        icon={showIcon}
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
        {...rest}
      />
      {showIcon && (
        <IconContainer clickable={clearable}>
          {clearable ? (
            <StyledIconButton
              icon={faTimes}
              onClick={() => onChange && onChange('')}
              aria-label={rest.clearLabel}
            />
          ) : icon ? (
            <FontAwesomeIcon icon={icon} />
          ) : (
            symbol
          )}
        </IconContainer>
      )}
      {!!infoText && (
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

export default InputField

interface InputFieldFProps extends Omit<InputProps, 'value' | 'onChange'> {
  bind: BoundFormState<string>
}

export const InputFieldF = React.memo(function InputFieldF({
  bind: { state, set, inputInfo },
  ...props
}: InputFieldFProps) {
  return (
    <InputField
      {...props}
      value={state}
      onChange={set}
      info={
        'info' in props ? props.info : props.readonly ? undefined : inputInfo()
      }
    />
  )
})
