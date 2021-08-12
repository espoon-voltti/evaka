// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import {
  fasCaretDown,
  fasCaretLeft,
  fasCaretRight,
  fasCaretUp
} from 'lib-icons'
import { defaultMargins } from '../white-space'
import { BaseProps } from '../utils'

const TooltipWrapper = styled.div`
  position: relative;
  display: inline-block;
  &:not(:hover) {
    .tooltip {
      display: none;
    }
  }
`

const tooltipPositions = (position: Position) => {
  const top = (() => {
    switch (position) {
      case 'top':
        return 'auto'
      case 'bottom':
        return `calc(100% + ${defaultMargins.xs})`
      case 'left':
      case 'right':
        return '-70px'
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
        return '-70px'
    }
  }

  const left = () => {
    switch (position) {
      case 'top':
      case 'bottom':
        return '-70px'
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
        return '-70px'
      case 'left':
        return `calc(100% + ${defaultMargins.xs})`
      case 'right':
        return 'auto'
    }
  }

  return { top, bottom, left, right }
}

const TooltipPositioner = styled.div<{ position: Position }>`
  position: absolute;
  z-index: 99999;
  display: flex;
  justify-content: center;
  align-items: center;

  top: calc(100% + ${defaultMargins.xs});
  left: -70px;
  right: -70px;

  top: ${({ position }) => tooltipPositions(position).top};
  bottom: ${({ position }) => tooltipPositions(position).bottom};
  left: ${({ position }) => tooltipPositions(position).left};
  right: ${({ position }) => tooltipPositions(position).right};
`

const TooltipDiv = styled.div`
  color: ${({ theme: { colors } }) => colors.greyscale.white};
  font-size: 15px;
  line-height: 22px;

  background-color: ${({ theme: { colors } }) => colors.greyscale.dark};
  padding: ${defaultMargins.s};
  border-radius: 2px;
  box-shadow: 0 4px 4px rgba(0, 0, 0, 0.25);

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
  color: ${({ theme: { colors } }) => colors.greyscale.dark};

  top: ${({ position }) => beakPositions(position).top};
  bottom: ${({ position }) => beakPositions(position).bottom};
  left: ${({ position }) => beakPositions(position).left};
  right: ${({ position }) => beakPositions(position).right};
`

type Position = 'top' | 'bottom' | 'right' | 'left'

type TooltipProps = BaseProps & {
  children: React.ReactNode
  tooltip: JSX.Element
  position?: Position
}

export default function Tooltip({
  children,
  tooltip,
  'data-qa': dataQa,
  ...props
}: TooltipProps) {
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
    <TooltipWrapper>
      <div>{children}</div>

      <TooltipPositioner className="tooltip" position={position}>
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
    </TooltipWrapper>
  )
}
