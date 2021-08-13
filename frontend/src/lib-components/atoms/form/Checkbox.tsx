// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useRef } from 'react'
import styled from 'styled-components'
import classNames from 'classnames'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faCheck } from 'lib-icons'
import { defaultMargins } from '../../white-space'
import { BaseProps } from '../../utils'

const diameter = '30px'

const Wrapper = styled.div`
  display: flex;
  align-items: flex-start;
  cursor: pointer;

  label {
    font-size: 1rem;
    margin-top: 6px;
    margin-left: ${defaultMargins.s};
    cursor: pointer;
  }

  &.disabled {
    cursor: not-allowed;

    label {
      color: ${({ theme: { colors } }) => colors.greyscale.medium};
      cursor: not-allowed;
    }
  }

  &:hover:not(.disabled) {
    input:checked {
      border-color: ${({ theme: { colors } }) => colors.main.primaryHover};
      background-color: ${({ theme: { colors } }) => colors.main.primaryHover};
    }
    input:not(:checked) {
      border-color: ${({ theme: { colors } }) => colors.greyscale.darkest};
    }
  }
`

const Box = styled.div`
  position: relative;
  width: ${diameter};
  height: ${diameter};
  margin-top: ${defaultMargins.xxs};
`

const CheckboxInput = styled.input`
  outline: none;
  appearance: none;
  width: ${diameter};
  height: ${diameter};
  border-radius: 2px;
  border-width: 1px;
  border-style: solid;
  border-color: ${({ theme: { colors } }) => colors.greyscale.dark};
  margin: 0;

  background-color: ${({ theme: { colors } }) => colors.greyscale.white};
  &:checked {
    border-color: ${({ theme: { colors } }) => colors.main.primary};
    background-color: ${({ theme: { colors } }) => colors.main.primary};

    &:disabled {
      background-color: ${({ theme: { colors } }) => colors.greyscale.medium};
    }
  }

  &:focus {
    border-width: 2px;
    border-color: ${({ theme: { colors } }) => colors.accents.petrol};
  }

  &:disabled {
    border-color: ${({ theme: { colors } }) => colors.greyscale.medium};
  }
`

const IconWrapper = styled.div`
  position: absolute;
  left: 0;
  top: 0;

  display: flex;
  justify-content: center;
  align-items: center;
  width: ${diameter};
  height: ${diameter};

  font-size: 25px;
  color: ${({ theme: { colors } }) => colors.greyscale.white};
`

interface CommonProps extends BaseProps {
  checked: boolean
}

export const StaticCheckBox = React.memo(function StaticCheckBox({
  checked
}: CommonProps) {
  return (
    <Box>
      <CheckboxInput type="checkbox" checked={checked} readOnly={true} />
      <IconWrapper>{checked && <FontAwesomeIcon icon={faCheck} />}</IconWrapper>
    </Box>
  )
})

interface CheckboxProps extends CommonProps {
  label: string
  hiddenLabel?: boolean
  onChange?: (checked: boolean) => void
  disabled?: boolean
}

function Checkbox({
  checked,
  label,
  hiddenLabel,
  onChange,
  disabled,
  className,
  'data-qa': dataQa
}: CheckboxProps) {
  const inputRef = useRef<HTMLInputElement>(null)

  return (
    <Wrapper
      onClick={() => {
        inputRef.current?.focus()
        if (!disabled && onChange) onChange(!checked)
      }}
      className={classNames(className, { disabled })}
      data-qa={dataQa}
    >
      <Box>
        <CheckboxInput
          type="checkbox"
          checked={checked}
          aria-label={label}
          disabled={disabled}
          onChange={(e) => {
            e.stopPropagation()
            if (onChange) onChange(!checked)
          }}
          readOnly={!onChange}
          ref={inputRef}
        />
        <IconWrapper>
          <FontAwesomeIcon icon={faCheck} />
        </IconWrapper>
      </Box>
      {!hiddenLabel && <label>{label}</label>}
    </Wrapper>
  )
}

export default Checkbox
