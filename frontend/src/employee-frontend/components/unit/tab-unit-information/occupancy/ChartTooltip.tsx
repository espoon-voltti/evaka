// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { ReactNode } from 'react'
import styled from 'styled-components'

import colors from 'lib-customizations/common'

type Position = {
  x: number
  y: number
}

export interface ChartTooltipProps {
  position?: {
    x: number
    y: number
    xAlign: string
    yAlign: string
    caretX: number
    caretY: number
  }
  content: ReactNode
  visible: boolean
}

export const ChartTooltip = React.memo(function ChartTooltip({
  position,
  content,
  visible
}: ChartTooltipProps) {
  if (position === undefined) {
    return null
  }

  const align =
    position.xAlign !== 'center'
      ? position.xAlign
      : position.yAlign !== 'center'
        ? position.yAlign
        : 'left'
  const pos =
    align === 'left'
      ? {
          x: position.caretX + caretSize,
          y: position.y
        }
      : align === 'right'
        ? {
            x: position.caretX - caretSize - tooltipWidth,
            y: position.y
          }
        : align === 'bottom'
          ? {
              x: position.x,
              y: position.caretY - caretSize - tooltipHeight
            }
          : {
              // top
              x: position.x - tooltipWidth / 2,
              y: position.caretY + caretSize
            }

  const caretPos = {
    x: position.caretX - caretSize,
    y: position.caretY - caretSize
  }
  return (
    <>
      <Caret pos={caretPos} className={align} opacity={visible ? 1 : 0} />
      <PositionedDiv pos={pos} opacity={visible ? 1 : 0} className={align}>
        <TooltipBody>{content}</TooltipBody>
      </PositionedDiv>
    </>
  )
})

const tooltipWidth = 275
const tooltipHeight = 150
const PositionedDiv = styled.div<{ pos: Position; opacity: number }>`
  position: absolute;
  pointer-events: none;
  opacity: ${(p) => p.opacity};
  z-index: 9999;
  width: ${tooltipWidth}px;
  height: ${tooltipHeight}px;

  &.top {
    left: ${({ pos }) => pos.x}px;
    bottom: ${({ pos }) => pos.y}px;
  }
  &.left {
    top: ${({ pos }) => pos.y}px;
    left: ${({ pos }) => pos.x}px;
  }
  &.bottom {
    top: ${({ pos }) => pos.y}px;
    left: ${({ pos }) => pos.x}px;
  }
  &.right {
    top: ${({ pos }) => pos.y}px;
    left: ${({ pos }) => pos.x}px;
  }
`

const caretSize = 12
const Caret = styled.span<{ pos: Position; opacity: number }>`
  position: absolute;
  pointer-events: none;
  opacity: ${(p) => p.opacity};
  top: ${({ pos }) => pos.y}px;
  left: ${({ pos }) => pos.x}px;
  width: 0;
  height: 0;
  display: inline-block;
  border: ${caretSize}px solid transparent;
  z-index: 99999;

  &.top {
    border-bottom-color: ${colors.grayscale.g70};
  }
  &.left {
    border-right-color: ${colors.grayscale.g70};
  }
  &.bottom {
    border-top-color: ${colors.grayscale.g70};
  }
  &.right {
    border-left-color: ${colors.grayscale.g70};
  }
`

const TooltipBody = styled.div`
  background: ${colors.grayscale.g70};
  color: white;
  border-radius: 4px;
  padding: 8px;
  font-weight: bold;
  text-align: left;

  table {
    border: 0;
    border-collapse: collapse;

    td {
      padding: 0;
      padding-right: 0.5em;
    }
  }
`
