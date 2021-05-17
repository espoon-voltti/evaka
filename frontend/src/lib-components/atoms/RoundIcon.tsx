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
import { tabletMin } from 'lib-components/breakpoints'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'

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

    @media (max-width: ${tabletMin}) {
      font-size: 28px;
      ${diameter(56)};
    }
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
  'aria-hidden': ariaHidden,
  label,
  bubble,
  bubblecolor,
  number
}: RoundIconProps) {
  if (typeof content === 'string' && size !== 'm') {
    console.warn('Text symbol is designed only for size m')
  }

  function onKeyDown(e: React.KeyboardEvent) {
    if (onClick && (e.key === ' ' || e.key === 'Enter')) onClick(e)
  }

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
        <FontAwesomeIcon icon={content} />
      )}
    </IconContainer>
  )
}

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
    <FixedSpaceColumnRelative spacing={'xxs'} alignItems={'center'}>
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
        <Circle smaller={size === 'L'} color={bubblecolor}>
          {number}
        </Circle>
      )}
    </FixedSpaceColumnRelative>
  )
}

const Text = styled.span<{ color: string }>`
  font-style: normal;
  font-weight: 600;
  font-size: 16px;
  line-height: 21px;
  color: ${(props) => props.color};
`

const Circle = styled.span<{ smaller: boolean; color?: string }>`
  min-width: ${(p) => (p.smaller ? '16px' : '20px')};
  min-height: ${(p) => (p.smaller ? '16px' : '20px')};
  padding: 3px 5.5px;
  font-size: ${(p) => (p.smaller ? '12px' : '16px')};
  line-height: ${(p) => (p.smaller ? '10px' : '13px')};
  border-radius: 10px;
  background-color: ${({ theme: { colors }, ...p }) =>
    p.color ? p.color : colors.accents.green};
  color: ${({ theme: { colors } }) => colors.greyscale.white};
  display: inline-block;
  position: absolute;
  right: -3px;
  top: 0;
  font-weight: 600;
`

const FixedSpaceColumnRelative = styled(FixedSpaceColumn)`
  position: relative;
`

export default RoundIcon
