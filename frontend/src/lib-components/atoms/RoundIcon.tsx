// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import classNames from 'classnames'
import styled from 'styled-components'
import { shade } from 'polished'
import { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { BaseProps } from '../utils'

export type IconSize = 's' | 'm' | 'L' | 'XL' | 'XXL'

type IconContainerProps = {
  color: string
  size: IconSize
  tabIndex?: number
  'aria-hidden'?: 'true' | 'false' | undefined
}

const diameter = (px: number) => `
  line-height: inherit;
  width: ${px}px;
  height: ${px}px;
  min-width: ${px}px;
  min-height: ${px}px;
  max-width: ${px}px;
  max-height: ${px}px;
`

const IconContainer = styled.div<IconContainerProps>`
  display: inline-flex;
  justify-content: center;
  align-items: center;

  border-radius: 100%;
  border: 1px solid ${(props) => props.color};
  background: ${({ theme: { colors } }) => colors.greyscale.white};
  color: ${(props) => props.color};
  user-select: none;

  &.active {
    background: ${(props) => props.color};
    color: ${({ theme: { colors } }) => colors.greyscale.white};
  }

  &.clickable:hover {
    background: ${(props) => shade(0.2, props.color)};
    border-color: ${(props) => shade(0.2, props.color)};
    color: ${({ theme: { colors } }) => colors.greyscale.white};
    cursor: pointer;
  }

  &.s {
    font-size: 12px;
    ${diameter(20)}
  }
  &.m {
    font-size: 16px;
    ${diameter(24)}

    span.text {
      font-family: Montserrat, sans-serif;
      font-weight: bold;
    }
  }
  &.l {
    font-size: 18px;
    ${diameter(34)}
  }
  &.xl {
    font-size: 44px;
    ${diameter(64)}
  }
  &.xxl {
    font-size: 80px;
    ${diameter(128)}
  }
`

type RoundIconProps = BaseProps & {
  content: IconDefinition | string
  color: string
  size: IconSize
  onClick?: (
    e: React.MouseEvent<HTMLDivElement, MouseEvent> | React.KeyboardEvent
  ) => void
  active?: boolean
  tabindex?: number
  role?: string
  'aria-label'?: string
  'aria-hidden'?: 'true' | 'false' | undefined
}

function RoundIcon({
  content,
  color,
  size,
  onClick,
  active = true,
  className,
  'data-qa': dataQa,
  tabindex,
  role,
  'aria-label': ariaLabel,
  'aria-hidden': ariaHidden
}: RoundIconProps) {
  if (typeof content === 'string' && size !== 'm') {
    console.warn('Text symbol is designed only for size m')
  }

  function onKeyDown(e: React.KeyboardEvent) {
    if (onClick && (e.key === ' ' || e.key === 'Enter')) onClick(e)
  }

  return (
    <IconContainer
      color={color}
      size={size}
      className={classNames(className, size.toLowerCase(), {
        clickable: !!onClick,
        active
      })}
      data-qa={dataQa}
      onClick={onClick}
      onKeyDown={onKeyDown}
      tabIndex={tabindex ?? -1}
      role={role}
      aria-label={ariaLabel}
      aria-hidden={ariaHidden}
    >
      {typeof content === 'string' ? (
        <span className="text">{content}</span>
      ) : (
        <FontAwesomeIcon icon={content} />
      )}
    </IconContainer>
  )
}

export default RoundIcon
