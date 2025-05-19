// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import classNames from 'classnames'
import React from 'react'
import styled, { css } from 'styled-components'

import { defaultMargins } from 'lib-components/white-space'

import { tabletMin } from '../../breakpoints'
import type { BaseProps } from '../../utils'

import { buttonBorderRadius, defaultButtonTextStyle } from './button-commons'

/**
 * Visual appearance of a button
 *
 * button = a normal button. Block-level element
 * inline = no border, no background, no padding. Inline-level element
 * link = like inline but looks more like a link. Inline-level element
 */
export type ButtonAppearance = 'button' | 'inline' | 'link'

/**
 * Visual order of the icon and text of a button.
 *
 * icon-text = icon on the left, text on the right
 * text-icon = text on the left, icon on the right
 */
export type ButtonOrder = 'icon-text' | 'text-icon'

/**
 * Visual/semantic props for a button
 */
export type BaseButtonVisualProps = {
  /**
   * Text to be displayed on the button
   */
  text: string
  /**
   * An ARIA label that should be used as the "accessible name" instead of the text
   */
  'aria-label'?: string
  /**
   * Icon to be displayed on the button
   */
  icon?: IconDefinition
  /**
   * Visual appearance of the button
   */
  appearance?: ButtonAppearance
  /**
   * Visual order of the icon and text
   */
  order?: ButtonOrder
  /**
   * HTML type of the button
   */
  type?: 'button' | 'submit'
  /**
   * If true, the button is rendered using "primary colors"
   */
  primary?: boolean
  /**
   * If true, the button is disabled and can't be clicked
   */
  disabled?: boolean
} & BaseProps

const StyledButton = styled.button<{
  $appearance: ButtonAppearance
  $order: ButtonOrder
}>`
  outline: none;
  cursor: pointer;
  border-style: solid;
  border-color: ${(p) => p.theme.colors.main.m2};
  border-radius: ${buttonBorderRadius};

  &.disabled {
    cursor: not-allowed;
  }

  &:focus {
    outline: 2px solid ${(p) => p.theme.colors.main.m2Focus};
    outline-offset: 2px;
  }

  &:hover {
    color: ${(p) => p.theme.colors.main.m2Hover};
    border-color: ${(p) => p.theme.colors.main.m2Hover};
  }

  &:active {
    color: ${(p) => p.theme.colors.main.m2Active};
    border-color: ${(p) => p.theme.colors.main.m2Active};
  }

  &.disabled {
    color: ${(p) => p.theme.colors.grayscale.g70};
    border-color: ${(p) => p.theme.colors.grayscale.g70};
  }

  @media (min-width: ${tabletMin}) {
    width: fit-content;
  }

  svg {
    ${({ $order }) =>
      $order === 'icon-text'
        ? 'margin-right'
        : 'margin-left'}: ${defaultMargins.xs};
    font-size: 1.25em;
  }

  ${(p) =>
    p.$appearance === 'link'
      ? css`
          text-decoration: underline;
          text-align: unset;

          &:focus {
            outline: none;
          }

          &:hover {
            text-decoration: none;
          }
        `
      : css`
          ${defaultButtonTextStyle};
        `};

  ${(p) =>
    p.$appearance === 'button'
      ? css`
          min-height: 45px;
          padding: 0 24px;
          min-width: 100px;
          border-width: 1px;
          background: ${(p) => p.theme.colors.grayscale.g0};
          display: flex;
          flex-wrap: nowrap;
          align-items: center;
          justify-content: center;
          overflow-x: hidden;
          letter-spacing: 0.2px;

          &.primary {
            background: ${(p) => p.theme.colors.main.m2};
            color: ${(p) => p.theme.colors.grayscale.g0};

            &:hover {
              background: ${(p) => p.theme.colors.main.m2Hover};
            }

            &:active {
              background: ${(p) => p.theme.colors.main.m2Active};
            }

            &.disabled {
              border-color: ${(p) => p.theme.colors.grayscale.g35};
              background: ${(p) => p.theme.colors.grayscale.g35};
            }
          }
        `
      : css`
          color: ${(p) => p.theme.colors.main.m2};
          border-width: 0;
          background: none;
          padding: 0;
          display: inline-flex;
          flex-wrap: nowrap;
          align-items: center;
          justify-content: center;

          &:hover {
            color: ${(p) => p.theme.colors.main.m1};
          }
        `}
`

export const renderBaseButton = (
  {
    text,
    icon,
    appearance = 'button',
    order = 'icon-text',
    type = 'button',
    disabled,
    primary,
    className,
    ...props
  }: BaseButtonVisualProps & { 'data-status'?: string; 'aria-busy'?: boolean },
  onClick: (e: React.MouseEvent<HTMLButtonElement>) => void,
  children: (props: {
    text: string
    icon: IconDefinition | undefined
    appearance: ButtonAppearance
    order: ButtonOrder
  }) => React.ReactNode
) => (
  <StyledButton
    type={type}
    disabled={disabled}
    className={classNames(className, { disabled, primary })}
    onClick={onClick}
    $appearance={appearance}
    $order={order}
    {...props}
  >
    {children({ text, icon, appearance, order })}
  </StyledButton>
)
