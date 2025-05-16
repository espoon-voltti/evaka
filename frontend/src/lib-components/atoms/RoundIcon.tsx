// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import { readableColor, shade } from 'polished'
import React from 'react'
import styled, { useTheme } from 'styled-components'

import { tabletMin } from 'lib-components/breakpoints'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'

import { fontWeights } from '../typography'
import type { BaseProps } from '../utils'

export type IconSize = 'xs' | 's' | 'm' | 'L' | 'XL' | 'XXL'

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
  border: 2px solid ${(p) => p.color};
  background-color: ${(p) => p.theme.colors.grayscale.g0};
  color: ${(p) => p.theme.colors.grayscale.g100};
  user-select: none;

  font-family: Montserrat, sans-serif;
  font-weight: ${fontWeights.bold};

  &.active {
    background-color: ${(props) => props.color};
    color: ${(p) =>
      readableColor(
        p.color,
        p.theme.colors.grayscale.g0,
        p.theme.colors.grayscale.g100
      )};
  }

  &.clickable:hover {
    background-color: ${(p) => shade(0.2, p.color)};
    border-color: ${(p) => shade(0.2, p.color)};
    color: ${(p) =>
      readableColor(
        shade(0.2, p.color),
        p.theme.colors.grayscale.g0,
        p.theme.colors.grayscale.g100
      )};
    cursor: pointer;
  }

  &.xs {
    font-size: 8px;
    ${diameter(16)}
  }

  &.s {
    font-size: 12px;
    ${diameter(20)}
  }

  &.m {
    font-size: 16px;
    ${diameter(24)}
  }

  &.l {
    font-size: 18px;
    ${diameter(34)}
  }

  &.xl {
    @media (max-width: ${tabletMin}) {
      font-size: 28px;
      ${diameter(56)};
    }
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
  label?: string
  bubble?: boolean
  bubblecolor?: string
  number?: number
}

const RoundIcon = React.memo(function RoundIcon({
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
  'aria-hidden': ariaHidden,
  label,
  bubble,
  bubblecolor,
  number
}: RoundIconProps) {
  const { colors } = useTheme()

  function onKeyDown(e: React.KeyboardEvent) {
    if (onClick && (e.key === ' ' || e.key === 'Enter')) onClick(e)
  }

  const readableIconColor = readableColor(
    color,
    colors.grayscale.g0,
    colors.grayscale.g100
  )

  if (label)
    return (
      <WithLabel
        content={content}
        color={color}
        size={size}
        onClick={onClick}
        active={active}
        className={className}
        data-qa={dataQa}
        tabindex={tabindex}
        role={role}
        label={label}
        aria-label={ariaLabel}
        aria-hidden={ariaHidden}
        number={number}
        bubble={bubble}
        bubblecolor={bubblecolor}
      />
    )

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
        <FontAwesomeIcon icon={content} color={readableIconColor} />
      )}
    </IconContainer>
  )
})

type WithLabelProps = RoundIconProps & {
  label: string
}

function WithLabel({
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
  'aria-hidden': ariaHidden,
  label,
  bubble,
  bubblecolor,
  number
}: WithLabelProps) {
  return (
    <FixedSpaceColumnRelative spacing="xxs" alignItems="center">
      <RoundIcon
        content={content}
        color={color}
        size={size}
        onClick={onClick}
        active={active}
        className={className}
        data-qa={dataQa}
        tabindex={tabindex}
        role={role}
        aria-label={ariaLabel}
        aria-hidden={ariaHidden}
      />
      <Text color={color}>{label}</Text>
      {bubble && (size === 'L' || size === 'XL') && (
        <Circle
          smaller={size === 'L'}
          color={bubblecolor}
          data-qa={dataQa ? `${dataQa}-bubble` : ''}
        >
          {number}
        </Circle>
      )}
    </FixedSpaceColumnRelative>
  )
}

const Text = styled.span<{ color: string }>`
  font-style: normal;
  font-weight: ${fontWeights.semibold};
  font-size: 16px;
  line-height: 21px;
  color: ${(props) => props.color};
  white-space: nowrap;
`

const Circle = styled.span<{ smaller: boolean; color?: string }>`
  min-width: ${(p) => (p.smaller ? '16px' : '20px')};
  min-height: ${(p) => (p.smaller ? '16px' : '20px')};
  padding: 3px 5.5px;
  font-size: ${(p) => (p.smaller ? '12px' : '16px')};
  line-height: ${(p) => (p.smaller ? '10px' : '13px')};
  border-radius: 10px;
  background-color: ${(p) =>
    p.color ? p.color : p.theme.colors.status.success};
  color: ${(p) => p.theme.colors.grayscale.g0};
  display: inline-block;
  position: absolute;
  right: -3px;
  top: 0;
  font-weight: ${fontWeights.semibold};
`

const FixedSpaceColumnRelative = styled(FixedSpaceColumn)`
  position: relative;
`

export default RoundIcon
