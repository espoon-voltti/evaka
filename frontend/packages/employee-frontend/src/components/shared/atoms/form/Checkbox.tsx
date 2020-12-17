// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useRef } from 'react'
import styled from 'styled-components'
import colors from '@evaka/lib-components/src/colors'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faCheck } from 'icon-set'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
import { BaseProps } from 'components/shared/utils'
import classNames from 'classnames'

const diameter = '30px'

const Wrapper = styled.div`
  display: flex;
  align-items: center;
  cursor: pointer;

  label {
    font-size: 15px;
    margin-left: ${defaultMargins.s};
    cursor: pointer;
  }

  &.disabled {
    cursor: not-allowed;

    label {
      color: ${colors.greyscale.medium};
      cursor: not-allowed;
    }
  }

  &:hover:not(.disabled) input {
    border-color: ${colors.greyscale.darkest};
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
  border-color: ${colors.greyscale.dark};
  margin: 0;

  &:checked {
    border-color: ${colors.primary};
    background-color: ${colors.primary};

    &:disabled {
      background-color: ${colors.greyscale.medium};
    }
  }

  &:focus {
    border-width: 2px;
    border-color: ${colors.accents.petrol};
  }

  &:disabled {
    border-color: ${colors.greyscale.medium};
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
  color: ${colors.greyscale.white};
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
          data-qa={dataQa}
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
