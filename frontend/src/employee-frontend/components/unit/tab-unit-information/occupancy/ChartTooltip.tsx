// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { TooltipModel } from 'chart.js'
import React, { useRef } from 'react'
import styled from 'styled-components'

import colors from 'lib-customizations/common'

type Position = {
  x: number
  y: number
}

export type ChartTooltipData = {
  tooltip?: TooltipModel<'line'>
  children: JSX.Element
}

export const ChartTooltip = React.memo(function ChartTooltip({
  data: { tooltip, children },
  visible
}: {
  data: ChartTooltipData
  visible: boolean
}) {
  const bodyRef = useRef<HTMLDivElement>(null)
  if (tooltip === undefined) {
    return null
  }

  const bodyRect = bodyRef.current?.getBoundingClientRect()
  const tooltipWidth = bodyRect ? bodyRect.width : 265
  const tooltipHeight = bodyRect ? bodyRect.height : 150

  const align =
    tooltip.xAlign !== 'center'
      ? tooltip.xAlign
      : tooltip.yAlign !== 'center'
      ? tooltip.yAlign
      : 'left'
  const pos =
    align === 'left'
      ? {
          x: tooltip.caretX + caretSize,
          y: tooltip.y
        }
      : align === 'right'
      ? {
          x: tooltip.caretX - caretSize - tooltipWidth,
          y: tooltip.y
        }
      : align === 'bottom'
      ? {
          x: tooltip.x,
          y: tooltip.caretY - caretSize - tooltipHeight
        }
      : {
          // top
          x: tooltip.x - tooltipWidth / 2,
          y: tooltip.caretY + caretSize
        }

  // make sure the tooltip fits inside the chart width
  const rect = tooltip.chart.canvas.getBoundingClientRect()
  if (pos.x + tooltipWidth > rect.right) {
    pos.x = pos.x - (tooltipWidth - (rect.right - pos.x)) - 16
  }
  if (pos.x < rect.left) {
    pos.x = rect.left + 16
  }

  const caretPos = {
    x: tooltip.caretX - caretSize,
    y: tooltip.caretY - caretSize
  }
  return (
    <>
      <Caret pos={caretPos} className={align} opacity={visible ? 1 : 0} />
      <PositionedDiv pos={pos} opacity={visible ? 1 : 0} className={align}>
        <TooltipBody ref={bodyRef}>{children}</TooltipBody>
      </PositionedDiv>
    </>
  )
})

const PositionedDiv = styled.div<{ pos: Position; opacity: number }>`
  position: absolute;
  opacity: ${(p) => p.opacity};
  z-index: 9999;
  min-width: 265px;

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
