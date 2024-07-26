// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import React, { ReactNode, useRef } from 'react'
import styled from 'styled-components'

import { useUniqueId } from 'lib-common/utils/useUniqueId'
import { ExpandingInfoButtonSlot } from 'lib-components/molecules/ExpandingInfo'
import { faCheck } from 'lib-icons'

import { BaseProps } from '../../utils'
import { defaultMargins } from '../../white-space'

const diameter = '36px'

const Wrapper = styled.div`
  display: inline-flex;
  align-items: flex-start;
  width: fit-content;

  &.disabled {
    cursor: not-allowed;

    label {
      color: ${(p) => p.theme.colors.grayscale.g35};
      cursor: not-allowed;
    }
  }

  @media (hover: hover) {
    &:hover:not(.disabled) {
      input[type='radio'] {
        &:checked {
          border-color: ${(p) => p.theme.colors.main.m2Hover};
          background-color: ${(p) => p.theme.colors.main.m2Hover};
        }

        &:not(:checked) {
          border-color: ${(p) => p.theme.colors.grayscale.g100};
        }
      }
    }
  }
`

const LabelContainer = styled.div<SizeProps>`
  margin-top: ${(p) => (p.small ? '3px' : '6px')};
  margin-left: ${defaultMargins.s};
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
    box-shadow:
      0 0 0 2px ${(p) => p.theme.colors.grayscale.g0},
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

  pointer-events: none; // let click event go through icon to the radio button
`

type RadioProps = BaseProps & {
  checked: boolean
  onChange?: () => void
  name?: string
  disabled?: boolean
  small?: boolean
  id?: string
  translate?: 'yes' | 'no'
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
  translate,
  ...props
}: RadioProps) {
  const inputRef = useRef<HTMLInputElement>(null)

  const ariaId = useUniqueId('radio')

  return (
    <Wrapper
      onClick={() => {
        inputRef.current?.focus()
      }}
      className={classNames(className, { disabled })}
      data-qa={dataQa}
    >
      <Circle small={small}>
        <RadioInput
          type="radio"
          checked={checked}
          name={name}
          id={id ?? ariaId}
          aria-label={'ariaLabel' in props ? props.ariaLabel : undefined}
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
      <LabelContainer small={small}>
        <label htmlFor={id ?? ariaId} translate={translate}>
          {props.label}
        </label>
        <ExpandingInfoButtonSlot />
      </LabelContainer>
    </Wrapper>
  )
})
