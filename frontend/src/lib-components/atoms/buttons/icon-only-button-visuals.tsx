// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import classNames from 'classnames'
import React from 'react'
import styled from 'styled-components'

import { Theme } from 'lib-common/theme'

import { BaseProps } from '../../utils'
import { diameterByIconSize, IconSize } from '../icon-size'

type PredefinedColor = 'default' | 'gray' | 'white'

/**
 * Visual/semantic props for an icon-only button
 */
export type BaseIconOnlyButtonVisualProps = {
  /**
   * ID of the button
   */
  id?: string
  /**
   * Icon to be displayed in the button
   */
  icon: IconDefinition
  /**
   * HTML type of the button
   */
  type?: 'button' | 'submit'
  /**
   * If true, the button is disabled and can't be clicked
   */
  disabled?: boolean
  /**
   * Size of the icon
   */
  size?: IconSize | undefined
  /**
   * Selects one of the predefined color themes
   */
  color?: PredefinedColor
} & BaseProps &
  ({ 'aria-label': string } | { 'aria-labelledby': string })

export const renderBaseIconOnlyButton = (
  {
    id,
    icon,
    type = 'button',
    disabled,
    size = 's',
    color = 'default',
    className,
    ...props
  }: BaseIconOnlyButtonVisualProps & {
    'data-status'?: string
    'aria-busy'?: boolean
  },
  onClick: (e: React.MouseEvent<HTMLButtonElement>) => void,
  children: (icon: IconDefinition, size: IconSize) => React.ReactNode
) => (
  <StyledButton
    id={id}
    type={type}
    disabled={disabled}
    className={classNames(className, { disabled })}
    $size={size}
    $color={color}
    onClick={onClick}
    {...props}
  >
    {children(icon, size)}
  </StyledButton>
)

type CssColors = {
  base: string
  hover: string
  active: string
  focus: string
}

const cssColors = (theme: Theme, color: PredefinedColor): CssColors => {
  switch (color) {
    case 'default':
      return {
        base: theme.colors.main.m2,
        hover: theme.colors.main.m2Hover,
        active: theme.colors.main.m2Active,
        focus: theme.colors.main.m2Focus
      }
    case 'gray':
      return {
        base: theme.colors.grayscale.g70,
        hover: theme.colors.grayscale.g70,
        active: theme.colors.grayscale.g100,
        focus: theme.colors.main.m2Focus
      }
    case 'white':
      return {
        base: theme.colors.grayscale.g0,
        hover: theme.colors.grayscale.g0,
        active: theme.colors.main.m2Active,
        focus: theme.colors.main.m2Focus
      }
  }
}

const StyledButton = styled.button<{
  $size: IconSize
  $color: PredefinedColor
}>`
  display: flex;
  justify-content: center;
  align-items: center;
  box-sizing: border-box;
  width: calc(12px + ${(p) => diameterByIconSize(p.$size)}px);
  height: calc(12px + ${(p) => diameterByIconSize(p.$size)}px);
  font-size: ${(p) => diameterByIconSize(p.$size)}px;
  color: ${(p) => cssColors(p.theme, p.$color).base};
  border: none;
  border-radius: 100%;
  background: none;
  outline: none;
  cursor: pointer;
  padding: 0;
  margin: -6px;
  -webkit-tap-highlight-color: transparent;

  &:focus {
    box-shadow: 0 0 0 2px ${(p) => cssColors(p.theme, p.$color).focus};
  }

  &:hover {
    color: ${(p) => cssColors(p.theme, p.$color).hover};
  }

  &:active {
    color: ${(p) => cssColors(p.theme, p.$color).active};
  }

  &.disabled,
  &:disabled {
    cursor: not-allowed;

    color: ${(p) => p.theme.colors.grayscale.g35};
  }
`
