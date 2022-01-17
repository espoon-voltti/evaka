// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import React, { useRef } from 'react'
import styled from 'styled-components'
import { faCheck } from 'lib-icons'
import { BaseProps } from '../../utils'
import { defaultMargins } from '../../white-space'

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
      color: ${(p) => p.theme.colors.grayscale.g35};
      cursor: not-allowed;
    }
  }

  &:hover:not(.disabled) {
    input:checked {
      border-color: ${(p) => p.theme.colors.main.m2Hover};
      background-color: ${(p) => p.theme.colors.main.m2Hover};
    }

    input:not(:checked) {
      border-color: ${(p) => p.theme.colors.grayscale.g100};
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
  border-color: ${(p) => p.theme.colors.grayscale.g70};
  margin: 0;

  background-color: ${(p) => p.theme.colors.grayscale.g0};

  &:checked {
    border-color: ${(p) => p.theme.colors.main.m2};
    background-color: ${(p) => p.theme.colors.main.m2};

    &:disabled {
      background-color: ${(p) => p.theme.colors.grayscale.g35};
    }
  }

  &:focus {
    border-width: 2px;
    border-color: ${(p) => p.theme.colors.main.m2Focus};
  }

  &:disabled {
    border-color: ${(p) => p.theme.colors.grayscale.g35};
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
  color: ${(p) => p.theme.colors.grayscale.g0};
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

export default React.memo(function Checkbox({
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
          data-qa={dataQa ? `${dataQa}-input` : undefined}
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
})
