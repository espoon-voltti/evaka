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

const diameter = '36px'

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
  border-color: ${colors.greyscale.dark};
  margin: 0;

  &:focus {
    border-width: 2px;
    border-color: ${colors.accents.petrol};
  }

  &:checked {
    border-color: ${colors.primary};
    background-color: ${colors.primary};

    &:disabled {
      background-color: ${colors.greyscale.medium};
    }
  }

  &:disabled {
    border-color: ${colors.greyscale.medium};
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
  color: ${colors.greyscale.white};
`

interface RadioProps extends BaseProps {
  checked: boolean
  label: string
  labelIcon?: JSX.Element
  onChange?: () => void
  name?: string
  disabled?: boolean
  small?: boolean
  id?: string
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
  small,
  id
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
          id={id}
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
