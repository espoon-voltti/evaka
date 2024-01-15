// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import React from 'react'
import styled from 'styled-components'

import {
  fasCaretDown,
  fasCaretLeft,
  fasCaretRight,
  fasCaretUp
} from 'lib-icons'

import { BaseProps } from '../utils'
import { defaultMargins } from '../white-space'

const TooltipWrapper = styled.div`
  position: relative;
  display: inline-block;
  &:not(:hover) {
    .tooltip {
      display: none;
    }
  }
`

const tooltipPositions = (position: Position, width: Width) => {
  const widthInPx = width === 'large' ? '-200px' : '-70px'
  const top = (() => {
    switch (position) {
      case 'top':
        return 'auto'
      case 'bottom':
        return `calc(100% + ${defaultMargins.xs})`
      case 'left':
      case 'right':
        return widthInPx
    }
  })()

  const bottom = () => {
    switch (position) {
      case 'top':
        return `calc(100% + ${defaultMargins.xs})`
      case 'bottom':
        return 'auto'
      case 'left':
      case 'right':
        return widthInPx
    }
  }

  const left = () => {
    switch (position) {
      case 'top':
      case 'bottom':
        return widthInPx
      case 'left':
        return 'auto'
      case 'right':
        return `calc(100% + ${defaultMargins.xs})`
    }
  }

  const right = () => {
    switch (position) {
      case 'top':
      case 'bottom':
        return widthInPx
      case 'left':
        return `calc(100% + ${defaultMargins.xs})`
      case 'right':
        return 'auto'
    }
  }

  return { top, bottom, left, right }
}

const TooltipPositioner = styled.div<{
  position: Position
  width: Width
}>`
  position: absolute;
  z-index: 99999;
  display: flex;
  justify-content: center;
  align-items: center;

  top: ${({ position, width }) => tooltipPositions(position, width).top};
  bottom: ${({ position, width }) => tooltipPositions(position, width).bottom};
  left: ${({ position, width }) => tooltipPositions(position, width).left};
  right: ${({ position, width }) => tooltipPositions(position, width).right};
`

const TooltipDiv = styled.div`
  color: ${(p) => p.theme.colors.grayscale.g0};
  font-size: 15px;
  line-height: 22px;

  background-color: ${(p) => p.theme.colors.grayscale.g70};
  padding: ${defaultMargins.s};
  border-radius: 2px;
  box-shadow: 0 4px 4px rgba(0, 0, 0, 0.25);

  p {
    margin: 0;
  }
  p:not(:last-child) {
    margin-bottom: 8px;
  }
`

const beakPositions = (position: Position) => {
  const top = (() => {
    switch (position) {
      case 'top':
        return 'auto'
      case 'bottom':
        return '-24px'
      case 'left':
      case 'right':
        return '0'
    }
  })()

  const bottom = () => {
    switch (position) {
      case 'top':
        return '-24px'
      case 'bottom':
        return 'auto'
      case 'left':
      case 'right':
        return '0'
    }
  }

  const left = () => {
    switch (position) {
      case 'top':
      case 'bottom':
        return '0'
      case 'left':
        return 'auto'
      case 'right':
        return '-10px'
    }
  }

  const right = () => {
    switch (position) {
      case 'top':
      case 'bottom':
        return '0'
      case 'left':
        return '-10px'
      case 'right':
        return 'auto'
    }
  }

  return { top, bottom, left, right }
}

const Beak = styled.div<{ position: Position }>`
  pointer-events: none;
  position: absolute;
  display: flex;
  justify-content: center;
  align-items: center;
  color: ${(p) => p.theme.colors.grayscale.g70};

  top: ${(p) => beakPositions(p.position).top};
  bottom: ${(p) => beakPositions(p.position).bottom};
  left: ${(p) => beakPositions(p.position).left};
  right: ${(p) => beakPositions(p.position).right};
`

type Position = 'top' | 'bottom' | 'right' | 'left'
type Width = 'small' | 'large'

export type TooltipProps = BaseProps & {
  children: React.ReactNode
  tooltip: React.ReactNode
  position?: Position
  width?: Width
  className?: string
}

export default React.memo(function Tooltip({
  children,
  className,
  ...props
}: TooltipProps) {
  return (
    <TooltipWrapper className={className}>
      {children}
      <TooltipWithoutAnchor {...props} />
    </TooltipWrapper>
  )
})

export const TooltipWithoutAnchor = React.memo(function Tooltip({
  tooltip,
  'data-qa': dataQa,
  className,
  ...props
}: Omit<TooltipProps, 'children'>) {
  if (!tooltip) return null

  const position = props.position ?? 'bottom'
  const icon = (() => {
    switch (position) {
      case 'top':
        return fasCaretDown
      case 'bottom':
        return fasCaretUp
      case 'left':
        return fasCaretRight
      case 'right':
        return fasCaretLeft
    }
  })()

  return (
    <TooltipPositioner
      className={classNames('tooltip', className)}
      position={position}
      width={props.width ?? 'small'}
    >
      <TooltipDiv data-qa={dataQa}>
        <Beak position={position}>
          <FontAwesomeIcon
            icon={icon}
            size={['top', 'bottom'].includes(position) ? '3x' : '2x'}
          />
        </Beak>
        <div>{tooltip}</div>
      </TooltipDiv>
    </TooltipPositioner>
  )
})
