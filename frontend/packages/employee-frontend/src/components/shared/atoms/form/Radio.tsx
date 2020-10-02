// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useRef } from 'react'
import styled from 'styled-components'
import Colors from 'components/shared/Colors'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faCheck } from '@evaka/icons'
import { DefaultMargins } from 'components/shared/layout/white-space'
import { BaseProps } from 'components/shared/utils'
import classNames from 'classnames'

const diameter = '36px'

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

interface SizeProps {
  small?: boolean
}

const Circle = styled.div<SizeProps>`
  position: relative;
  width: ${(p) => (p.small ? '30px' : diameter)};
  height: ${(p) => (p.small ? '30px' : diameter)};
`

const RadioInput = styled.input<SizeProps>`
  outline: none;
  appearance: none;
  width: ${(p) => (p.small ? '30px' : diameter)};
  height: ${(p) => (p.small ? '30px' : diameter)};
  border-radius: 100%;
  border-width: 1px;
  border-style: solid;
  border-color: ${Colors.greyscale.dark};
  margin: 0;

  &:focus {
    border-width: 2px;
    border-color: ${Colors.accents.petrol};
  }

  &:checked {
    border-color: ${Colors.primary};
    background-color: ${Colors.primary};

    &:disabled {
      background-color: ${Colors.greyscale.medium};
    }
  }

  &:disabled {
    border-color: ${Colors.greyscale.medium};
  }
`

const IconWrapper = styled.div<SizeProps>`
  position: absolute;
  left: 0;
  top: 0;

  display: flex;
  justify-content: center;
  align-items: center;
  width: ${(p) => (p.small ? '30px' : diameter)};
  height: ${(p) => (p.small ? '30px' : diameter)};

  font-size: ${(p) => (p.small ? '20px' : '25px')};
  color: ${Colors.greyscale.white};
`

interface RadioProps extends BaseProps {
  checked: boolean
  label: string
  labelIcon?: JSX.Element
  onChange?: () => void
  name?: string
  disabled?: boolean
  small?: boolean
}

function Radio({
  checked,
  label,
  labelIcon,
  onChange,
  name,
  disabled,
  className,
  dataQa,
  small
}: RadioProps) {
  const inputRef = useRef<HTMLInputElement>(null)

  return (
    <Wrapper
      onClick={() => {
        inputRef.current?.focus()
        if (!disabled && onChange) onChange()
      }}
      className={classNames(className, { disabled })}
      data-qa={dataQa}
    >
      <Circle small={small}>
        <RadioInput
          type="radio"
          checked={checked}
          name={name}
          aria-label={label}
          disabled={disabled}
          onChange={(e) => {
            e.stopPropagation()
            if (onChange) onChange()
          }}
          readOnly={!onChange}
          ref={inputRef}
          small={small}
        />
        <IconWrapper small={small}>
          <FontAwesomeIcon icon={faCheck} />
        </IconWrapper>
      </Circle>
      <label>{label}</label>
      {labelIcon && labelIcon}
    </Wrapper>
  )
}

export default Radio
