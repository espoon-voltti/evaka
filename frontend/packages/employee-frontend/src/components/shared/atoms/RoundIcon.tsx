// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { BaseProps } from 'components/shared/utils'
import classNames from 'classnames'
import Colors from 'components/shared/Colors'
import { shade } from 'polished'

export type IconSize = 's' | 'm' | 'L' | 'XL' | 'XXL'

interface IconContainerProps {
  color: string
  size: IconSize
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
  display: flex;
  justify-content: center;
  align-items: center;

  border-radius: 100%;
  border: 1px solid ${(props) => props.color};
  background: ${Colors.greyscale.white};
  color: ${(props) => props.color};
  user-select: none;

  &.active {
    background: ${(props) => props.color};
    color: ${Colors.greyscale.white};
  }

  &.clickable:hover {
    background: ${(props) => shade(0.2, props.color)};
    border-color: ${(props) => shade(0.2, props.color)};
    color: ${Colors.greyscale.white};
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

interface RoundIconProps extends BaseProps {
  content: IconDefinition | string
  color: string
  size: IconSize
  onClick?: (e: React.MouseEvent<HTMLDivElement, MouseEvent>) => void
  active?: boolean
}

function RoundIcon({
  content,
  color,
  size,
  onClick,
  active = true,
  className,
  dataQa
}: RoundIconProps) {
  if (typeof content === 'string' && size !== 'm') {
    console.warn('Text symbol is designed only for size m')
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
