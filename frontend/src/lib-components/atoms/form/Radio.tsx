// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import React, { ReactNode, useRef } from 'react'
import styled from 'styled-components'

import { faCheck } from 'lib-icons'

import { BaseProps } from '../../utils'
import { defaultMargins } from '../../white-space'

const diameter = '36px'

const Wrapper = styled.div<SizeProps>`
  display: inline-flex;
  align-items: flex-start;
  cursor: pointer;
  width: fit-content;

  label {
    margin-top: ${(p) => (p.small ? '3px' : '6px')};
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

  @media (hover: hover) {
    &:hover:not(.disabled) {
      input:checked {
        border-color: ${(p) => p.theme.colors.main.m2Hover};
        background-color: ${(p) => p.theme.colors.main.m2Hover};
      }
      input:not(:checked) {
        border-color: ${(p) => p.theme.colors.grayscale.g100};
      }
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
  border-color: ${(p) => p.theme.colors.grayscale.g70};
  margin: 0;

  &:checked {
    border-color: ${(p) => p.theme.colors.main.m2};
    background-color: ${(p) => p.theme.colors.main.m2};

    &:disabled {
      background-color: ${(p) => p.theme.colors.grayscale.g35};
    }
  }

  &:focus {
    box-shadow: 0 0 0 2px ${(p) => p.theme.colors.grayscale.g0},
      0 0 0 4px ${(p) => p.theme.colors.main.m2Focus};
  }

  &:disabled {
    border-color: ${(p) => p.theme.colors.grayscale.g35};
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
  color: ${(p) => p.theme.colors.grayscale.g0};
`

type RadioProps = BaseProps & {
  checked: boolean
  onChange?: () => void
  name?: string
  disabled?: boolean
  small?: boolean
  id?: string
} & ({ label: string } | { label: ReactNode; ariaLabel: string })

export default React.memo(function Radio({
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
})
