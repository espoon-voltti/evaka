// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'
import styled from 'styled-components'
import { readableColor } from 'polished'
import classNames from 'classnames'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'

import { faCheck } from 'lib-icons'
import { defaultMargins } from '../white-space'
import { tabletMin } from '../breakpoints'
import { useUniqueId } from 'lib-common/utils/useUniqueId'

export const StaticChip = styled.div<{ color: string; textColor?: string }>`
  font-family: 'Open Sans', sans-serif;
  font-weight: 600;
  font-size: 16px;
  line-height: 20px;
  user-select: none;
  border: 1px solid ${(p) => p.color};
  border-radius: 1000px;
  background-color: ${(p) => p.color};
  color: ${({ theme: { colors }, ...p }) =>
    p.textColor ??
    readableColor(p.color, colors.greyscale.white, colors.greyscale.darkest)};
  padding: ${defaultMargins.xxs}
    calc(${defaultMargins.xs} + ${defaultMargins.xxs});

  outline: none;
  &:focus {
    outline: 2px solid ${({ theme: { colors } }) => colors.main.light};
    outline-offset: 2px;
  }
`

type SelectionChipProps = {
  text: string
  selected: boolean
  onChange: (selected: boolean) => void
  'data-qa'?: string
  showIcon?: boolean
}

function preventDefault(e: React.UIEvent<unknown>) {
  e.preventDefault()
}

export const SelectionChip = React.memo(function SelectionChip({
  text,
  selected,
  onChange,
  'data-qa': dataQa,
  showIcon = true
}: SelectionChipProps) {
  const ariaId = useUniqueId('selection-chip')

  const onClick = useCallback(
    (e: React.MouseEvent<HTMLElement>) => {
      e.preventDefault()
      onChange(!selected)
    },
    [onChange, selected]
  )

  return (
    <div data-qa={dataQa} onClick={onClick}>
      <SelectionChipWrapper>
        <SelectionChipInnerWrapper
          className={classNames({ checked: selected })}
        >
          <HiddenInput
            type="checkbox"
            onChange={() => onChange(!selected)}
            onClick={preventDefault}
            checked={selected}
            id={ariaId}
          />
          {showIcon && selected && (
            <IconWrapper>
              <FontAwesomeIcon icon={faCheck} />
            </IconWrapper>
          )}
          <StyledLabel
            className={classNames({ checked: showIcon && selected })}
            htmlFor={ariaId}
            onClick={preventDefault}
          >
            {text}
          </StyledLabel>
        </SelectionChipInnerWrapper>
      </SelectionChipWrapper>
    </div>
  )
})

export const ChoiceChip = React.memo(function ChoiceChip({
  text,
  selected,
  onChange,
  'data-qa': dataQa
}: SelectionChipProps) {
  return (
    <SelectionChip
      text={text}
      selected={selected}
      onChange={onChange}
      data-qa={dataQa}
      showIcon={false}
    />
  )
})

const StyledLabel = styled.label`
  cursor: pointer;
  &.checked {
    margin-left: 32px;
  }
`

const SelectionChipWrapper = styled.div`
  font-family: 'Open Sans', sans-serif;
  font-weight: 600;
  font-size: 14px;
  line-height: 18px;
  user-select: none;
  border-radius: 1000px;
  cursor: pointer;
  outline: none;

  &:focus,
  &:focus-within {
    border: 2px solid ${({ theme: { colors } }) => colors.accents.petrol};
    border-radius: 1000px;
    margin: -2px;
  }
  padding: 2px;
`

const SelectionChipInnerWrapper = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  border-radius: 1000px;
  padding: 0 calc(${defaultMargins.xs} + ${defaultMargins.xxs});
  background-color: ${({ theme: { colors } }) => colors.greyscale.white};
  color: ${({ theme: { colors } }) => colors.main.primary};
  border: 1px solid ${({ theme: { colors } }) => colors.main.primary};
  &.checked {
    background-color: ${({ theme: { colors } }) => colors.main.primary};
    color: ${({ theme: { colors } }) => colors.greyscale.white};
  }

  @media (max-width: ${tabletMin}) {
    height: 40px;
  }
`

const HiddenInput = styled.input`
  outline: none;
  appearance: none;
  border: none;
  background: none;
  margin: 0;
  height: 32px;
  width: 0;
`

const IconWrapper = styled.div`
  position: absolute;
  left: calc(${defaultMargins.xs} + ${defaultMargins.xxs});
  top: 0;

  display: flex;
  justify-content: center;
  align-items: center;
  width: 24px;
  height: 32px;

  font-size: 24px;
  color: ${({ theme: { colors } }) => colors.greyscale.white};
`
