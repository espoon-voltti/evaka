// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useRef } from 'react'
import styled from 'styled-components'
import Colors from 'components/shared/Colors'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faCheck } from 'icon-set'
import { DefaultMargins } from 'components/shared/layout/white-space'
import { BaseProps } from 'components/shared/utils'
import classNames from 'classnames'

const diameter = '30px'

const Wrapper = styled.div`
  display: flex;
  align-items: center;
  cursor: pointer;

  label {
    font-size: 15px;
    margin-left: ${DefaultMargins.s};
    cursor: pointer;
  }

  &.disabled {
    cursor: not-allowed;

    label {
      color: ${Colors.greyscale.medium};
      cursor: not-allowed;
    }
  }

  &:hover:not(.disabled) input {
    border-color: ${Colors.greyscale.darkest};
  }
`

const Box = styled.div`
  position: relative;
  width: ${diameter};
  height: ${diameter};
`

const CheckboxInput = styled.input`
  outline: none;
  appearance: none;
  width: ${diameter};
  height: ${diameter};
  border-radius: 2px;
  border-width: 1px;
  border-style: solid;
  border-color: ${Colors.greyscale.dark};
  margin: 0;

  &:checked {
    border-color: ${Colors.primary};
    background-color: ${Colors.primary};

    &:disabled {
      background-color: ${Colors.greyscale.medium};
    }
  }

  &:focus {
    border-width: 2px;
    border-color: ${Colors.accents.petrol};
  }

  &:disabled {
    border-color: ${Colors.greyscale.medium};
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
  color: ${Colors.greyscale.white};
`

interface CheckboxProps extends BaseProps {
  checked: boolean
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
  dataQa
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
