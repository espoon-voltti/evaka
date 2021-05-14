// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { ReactNode, useRef } from 'react'
import styled from 'styled-components'
import classNames from 'classnames'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faCheck } from 'lib-icons'
import { defaultMargins } from '../../white-space'
import { BaseProps } from '../../utils'

const diameter = '36px'

const Wrapper = styled.div<SizeProps>`
  display: inline-flex;
  align-items: flex-start;
  cursor: pointer;

  label {
    margin-top: ${(p) => (p.small ? '3px' : '6px')};
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
  border-color: ${({ theme: { colors } }) => colors.greyscale.dark};
  margin: 0;

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
  color: ${({ theme: { colors } }) => colors.greyscale.white};
`

type RadioProps = BaseProps & {
  checked: boolean
  onChange?: () => void
  name?: string
  disabled?: boolean
  small?: boolean
  id?: string
} & ({ label: string } | { label: ReactNode; ariaLabel: string })

function Radio({
  checked,
  onChange,
  name,
  disabled,
  className,
  'data-qa': dataQa,
  small,
  id,
  ...props
}: RadioProps) {
  const inputRef = useRef<HTMLInputElement>(null)
  const ariaLabel: string = 'ariaLabel' in props ? props.ariaLabel : props.label

  return (
    <Wrapper
      onClick={() => {
        inputRef.current?.focus()
        if (!disabled && onChange) onChange()
      }}
      className={classNames(className, { disabled })}
      small={small}
      data-qa={dataQa}
    >
      <Circle small={small}>
        <RadioInput
          type="radio"
          checked={checked}
          name={name}
          aria-label={ariaLabel}
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
      <label htmlFor={id}>{props.label}</label>
    </Wrapper>
  )
}

export default Radio
