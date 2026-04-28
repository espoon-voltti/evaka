// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import type { Property } from 'csstype'
import { readableColor } from 'polished'
import React, { useCallback } from 'react'
import styled, { css, useTheme } from 'styled-components'

import { faCheck } from 'lib-icons'

import { tabletMin } from '../breakpoints'
import { fontWeights } from '../typography'
import { defaultMargins } from '../white-space'

export const StaticChip = styled.div<{
  $color: string
  $textColor?: string
  $fitContent?: boolean
  $nowrap?: boolean
}>`
  display: inline-block;
  font-family: 'Open Sans', sans-serif;
  font-weight: ${fontWeights.semibold};
  font-size: ${defaultMargins.s};
  line-height: ${defaultMargins.s};
  user-select: none;
  border: 1px solid ${(p) => p.$color};
  border-radius: 1000px;
  background-color: ${(p) => p.$color};
  color: ${(p) =>
    p.$textColor ??
    readableColor(
      p.$color,
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
  ${(p) =>
    p.$fitContent
      ? css`
          width: fit-content;
          height: fit-content;
        `
      : ''}
  ${(p) =>
    p.$nowrap
      ? css`
          white-space: nowrap;
        `
      : ''}
`

type SelectionChipProps = {
  text: string
  selected: boolean
  onChange: (selected: boolean) => void
  onBlur?: (e: React.FocusEvent<HTMLInputElement>) => void
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
  onBlur,
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
      onBlur={onBlur}
      {...(disabled
        ? { 'aria-disabled': 'true', tabIndex: -1 }
        : { tabIndex: 0 })}
    >
      <SelectionChipInnerWrapper
        className={classNames({ checked: selected, disabled })}
      >
        {renderIcon && (
          <IconWrapper>
            <FontAwesomeIcon icon={faCheck} />
          </IconWrapper>
        )}
        <StyledSpan
          className={classNames({ disabled })}
          aria-hidden="true"
          onClick={preventDefault}
          translate={translate}
        >
          {text}
        </StyledSpan>
      </SelectionChipInnerWrapper>
    </SelectionChipWrapper>
  )
})

const StyledSpan = styled.span`
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
  $margin?: keyof typeof defaultMargins
  $justifyContent?: Property.JustifyContent
}>`
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  justify-content: ${(p) => p.$justifyContent ?? 'flex-start'};

  ${(p) => css`
    > div {
      margin-bottom: ${defaultMargins[p.$margin ?? 's']};
    }
  `};
`

// ---- Chip ----

type ChipColorPaletteKey =
  | 'green'
  | 'orange'
  | 'purple'
  | 'red'
  | 'blue'
  | 'gray'

interface ChipColorPalette {
  backgroundColor: string
  textColor: string
  borderColor?: string
  iconColor: string
  iconColorInner?: string
}

type ChipSize = 'small' | 'large'

export type ChipProps = {
  label: string
  icon?: IconDefinition
  size: ChipSize
  colorPalette: ChipColorPaletteKey
  iconCircle?: boolean
  'data-qa'?: string
}

const chipSizes: Record<
  ChipSize,
  {
    height: string
    fontSize: string
    iconSize: string
    circleSize: string
    iconInCircleSize: string
  }
> = {
  small: {
    height: '24px',
    fontSize: '14px',
    iconSize: '16px',
    circleSize: '18px',
    iconInCircleSize: '12px'
  },
  large: {
    height: '32px',
    fontSize: '16px',
    iconSize: '16px',
    circleSize: '24px',
    iconInCircleSize: '16px'
  }
}

const ChipContainer = styled.div<{
  $size: ChipSize
  $backgroundColor: string
  $textColor: string
  $borderColor: string
  $iconCircle: boolean
}>`
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: ${(p) => chipSizes[p.$size].height};
  padding: 0 ${defaultMargins.xs} 0
    ${(p) => (p.$iconCircle ? defaultMargins.xxs : defaultMargins.xs)};
  border-radius: ${defaultMargins.s};
  border: 2px solid ${(p) => p.$borderColor};
  background-color: ${(p) => p.$backgroundColor};
  color: ${(p) => p.$textColor};
  font-family: 'Open Sans', sans-serif;
  font-weight: ${fontWeights.semibold};
  font-size: ${(p) => chipSizes[p.$size].fontSize};
  white-space: nowrap;
  width: fit-content;
`

const ChipIconCircle = styled.div<{ $size: ChipSize; $color: string }>`
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: ${(p) => chipSizes[p.$size].circleSize};
  height: ${(p) => chipSizes[p.$size].circleSize};
  border-radius: 100%;
  background-color: ${(p) => p.$color};
`

const ChipIcon = styled(FontAwesomeIcon)<{ $px: string }>`
  width: ${(p) => p.$px};
  height: ${(p) => p.$px};
`

export const Chip = React.memo(function Chip({
  label,
  icon,
  size,
  colorPalette,
  iconCircle,
  'data-qa': dataQa
}: ChipProps) {
  const { colors } = useTheme()

  const palettes: Record<ChipColorPaletteKey, ChipColorPalette> = {
    green: {
      backgroundColor: '#E9F6EA',
      textColor: '#2A6A2C',
      iconColor: colors.status.success
    },
    orange: {
      backgroundColor: '#FFEEE0',
      textColor: '#814113',
      iconColor: colors.status.warning
    },
    purple: {
      backgroundColor: '#F0E5F6',
      textColor: '#693088',
      iconColor: colors.accents.a4violet
    },
    red: {
      backgroundColor: '#FDECEA',
      textColor: '#820014',
      iconColor: colors.status.danger
    },
    blue: {
      backgroundColor: '#e3eaf6',
      textColor: colors.main.m1,
      iconColor: colors.status.info
    },
    gray: {
      backgroundColor: colors.grayscale.g15,
      textColor: colors.grayscale.g100,
      iconColor: colors.grayscale.g70
    }
  }

  const palette = palettes[colorPalette]
  const iconColorInner = palette.iconColorInner ?? colors.grayscale.g0
  const borderColor = palette.borderColor ?? palette.backgroundColor
  const sizes = chipSizes[size]

  return (
    <ChipContainer
      $size={size}
      $backgroundColor={palette.backgroundColor}
      $textColor={palette.textColor}
      $borderColor={borderColor}
      $iconCircle={!!iconCircle}
      data-qa={dataQa}
    >
      {icon && iconCircle && (
        <ChipIconCircle $size={size} $color={palette.iconColor}>
          <ChipIcon
            icon={icon}
            $px={sizes.iconInCircleSize}
            color={iconColorInner}
          />
        </ChipIconCircle>
      )}
      {icon && !iconCircle && (
        <ChipIcon icon={icon} $px={sizes.iconSize} color={palette.iconColor} />
      )}
      <span>{label}</span>
    </ChipContainer>
  )
})
