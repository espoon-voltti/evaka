// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import { Property } from 'csstype'
import { readableColor } from 'polished'
import React, { useCallback } from 'react'
import styled, { css } from 'styled-components'

import { faCheck } from 'lib-icons'

import { tabletMin } from '../breakpoints'
import { fontWeights } from '../typography'
import { defaultMargins } from '../white-space'

export const StaticChip = styled.div<{
  color: string
  textColor?: string
  fitContent?: boolean
}>`
  display: inline-block;
  font-family: 'Open Sans', sans-serif;
  font-weight: ${fontWeights.semibold};
  font-size: ${defaultMargins.s};
  line-height: ${defaultMargins.s};
  user-select: none;
  border: 1px solid ${(p) => p.color};
  border-radius: 1000px;
  background-color: ${(p) => p.color};
  color: ${(p) =>
    p.textColor ??
    readableColor(
      p.color,
      p.theme.colors.grayscale.g0,
      p.theme.colors.grayscale.g100
    )};
  padding: ${defaultMargins.xxs}
    calc(${defaultMargins.xs} + ${defaultMargins.xxs});

  outline: none;
  &:focus {
    outline: 2px solid ${(p) => p.theme.colors.main.m3};
    outline-offset: 2px;
  }
  ${(p) => (p.fitContent ? 'width: fit-content;' : '')}
`

type SelectionChipProps = {
  text: string
  selected: boolean
  onChange: (selected: boolean) => void
  disabled?: boolean
  'data-qa'?: string
  hideIcon?: boolean
  translate?: 'yes' | 'no'
}

function preventDefault(e: React.UIEvent<unknown>) {
  e.preventDefault()
}

export const SelectionChip = React.memo(function SelectionChip({
  text,
  selected,
  onChange,
  disabled,
  'data-qa': dataQa,
  hideIcon = false,
  translate
}: SelectionChipProps) {
  const onClick = useCallback(
    (e: React.UIEvent<HTMLElement>) => {
      e.preventDefault()
      onChange(!selected)
    },
    [onChange, selected]
  )

  const renderIcon = !hideIcon && selected

  return (
    <SelectionChipWrapper
      role="checkbox"
      aria-label={text}
      aria-checked={selected}
      onClick={(e) => (!disabled ? onClick(e) : undefined)}
      onKeyUp={(ev) => ev.key === 'Enter' && onClick(ev)}
      data-qa={dataQa}
      tabIndex={0}
    >
      <SelectionChipInnerWrapper
        className={classNames({ checked: selected, disabled })}
      >
        {renderIcon && (
          <IconWrapper>
            <FontAwesomeIcon icon={faCheck} />
          </IconWrapper>
        )}
        <StyledLabel
          className={classNames({ disabled })}
          aria-hidden="true"
          onClick={preventDefault}
          translate={translate}
        >
          {text}
        </StyledLabel>
      </SelectionChipInnerWrapper>
    </SelectionChipWrapper>
  )
})

const StyledLabel = styled.label`
  cursor: pointer;
  &.disabled {
    cursor: not-allowed;
  }
`

const SelectionChipWrapper = styled.div`
  font-family: 'Open Sans', sans-serif;
  font-weight: ${fontWeights.semibold};
  font-size: 14px;
  line-height: 18px;
  user-select: none;
  border-radius: 1000px;
  cursor: pointer;
  outline: none;
  border: 2px solid transparent;

  &:focus,
  &:focus-within {
    border-color: ${(p) => p.theme.colors.main.m1};
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
  background-color: ${(p) => p.theme.colors.grayscale.g0};
  color: ${(p) => p.theme.colors.main.m2};
  border: 1px solid ${(p) => p.theme.colors.main.m2};
  min-height: 36px;
  &.checked {
    background-color: ${(p) => p.theme.colors.main.m2};
    color: ${(p) => p.theme.colors.grayscale.g0};
  }
  &.disabled {
    background-color: ${(p) => p.theme.colors.grayscale.g4};
    color: ${(p) => p.theme.colors.grayscale.g70};
    border: 1px solid ${(p) => p.theme.colors.grayscale.g35};
  }

  @media (max-width: ${tabletMin}) {
    height: 40px;
  }
`

const IconWrapper = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  margin-right: ${defaultMargins.xs};

  font-size: 24px;
  color: ${(p) => p.theme.colors.grayscale.g0};
`

export const ChipWrapper = styled.div<{
  margin?: keyof typeof defaultMargins
  $justifyContent?: Property.JustifyContent
}>`
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  justify-content: ${(p) => p.$justifyContent ?? 'flex-start'};

  ${(p) => css`
    > div {
      margin-bottom: ${defaultMargins[p.margin ?? 's']};
    }
  `};
`
