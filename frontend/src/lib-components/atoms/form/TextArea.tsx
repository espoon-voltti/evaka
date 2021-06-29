// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { RefObject, useState } from 'react'
import styled from 'styled-components'
import classNames from 'classnames'
import TextareaAutosize from 'react-autosize-textarea'
import { BaseProps } from '../../utils'
import UnderRowStatusIcon from '../StatusIcon'
import { InputFieldUnderRow, InputInfo, InputRow } from './InputField'

interface TextAreaInputProps extends BaseProps {
  value: string
  onChange?: (e: React.ChangeEvent<HTMLTextAreaElement>) => void
  onFocus?: (e: React.FocusEvent<HTMLTextAreaElement>) => void
  onBlur?: (e: React.FocusEvent<HTMLTextAreaElement>) => void
  onKeyPress?: (e: React.KeyboardEvent) => void
  readonly?: boolean
  rows?: number
  maxLength?: number
  type?: string
  autoFocus?: boolean
  placeholder?: string
  info?: InputInfo
  align?: 'left' | 'right'
  id?: string
  'data-qa'?: string
  className?: string
  'aria-describedby'?: string
  hideErrorsBeforeTouched?: boolean
  required?: boolean
  inputRef?: RefObject<HTMLTextAreaElement>
}

export function TextArea({
  value,
  onChange,
  onFocus,
  onBlur,
  onKeyPress,
  readonly,
  rows,
  maxLength,
  type,
  autoFocus,
  placeholder,
  info,
  id,
  'data-qa': dataQa,
  className,
  'aria-describedby': ariaId,
  hideErrorsBeforeTouched,
  required,
  inputRef
}: TextAreaInputProps) {
  const [touched, setTouched] = useState(false)

  const hideError =
    hideErrorsBeforeTouched && !touched && info?.status === 'warning'
  const infoText = hideError ? undefined : info?.text
  const infoStatus = hideError ? undefined : info?.status

  return (
    <div>
      <InputRow>
        <StyledTextArea
          value={value}
          onChange={onChange}
          onFocus={onFocus}
          onBlur={(e) => {
            setTouched(true)
            onBlur && onBlur(e)
          }}
          onKeyPress={onKeyPress}
          placeholder={placeholder}
          readOnly={readonly}
          disabled={readonly}
          maxLength={maxLength}
          type={type}
          autoFocus={autoFocus}
          className={classNames(className, infoStatus)}
          data-qa={dataQa}
          id={id}
          aria-describedby={ariaId}
          required={required ?? false}
          ref={inputRef}
          rows={rows}
        />
      </InputRow>
      {infoText && (
        <InputFieldUnderRow className={classNames(infoStatus)}>
          <span data-qa={`${dataQa ?? ''}-info`}>{infoText}</span>
          <UnderRowStatusIcon status={info?.status} />
        </InputFieldUnderRow>
      )}
    </div>
  )
}

const StyledTextArea = styled(TextareaAutosize)`
  display: block;
  position: relative;
  align-items: center;
  justify-content: flex-start;

  width: 100%;
  max-width: 100%;
  height: 38px;
  min-height: 2.5em;
  padding: calc(0.5em - 1px) calc(0.625em - 1px) calc(0.5em - 1px);

  font-size: 1rem;
  font-family: 'Open Sans', Arial, sans-serif;
  color: #0f0f0f;
  line-height: 1.5;
  overflow: hidden;
  overflow-wrap: break-word;
  resize: none;

  background-color: transparent;
  border: 2px solid transparent;
  border-bottom: 1px solid ${({ theme: { colors } }) => colors.greyscale.medium};
  border-radius: 0;
  box-shadow: none;

  outline: none;
  &:focus {
    border-width: 2px;
    border-style: solid;
    border-color: ${({ theme: { colors } }) => colors.accents.petrol};
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
    border-bottom: 2px solid ${({ theme: { colors } }) => colors.accents.green};
    &:focus {
      border-color: ${({ theme: { colors } }) => colors.accents.green};
    }
  }

  &.warning {
    border-bottom: 2px solid ${({ theme: { colors } }) => colors.accents.orange};
    &:focus {
      border-color: ${({ theme: { colors } }) => colors.accents.orange};
    }
  }
`
export default TextArea
