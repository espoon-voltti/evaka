// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import styled from 'styled-components'

import type {
  DataRetentionSchemaGraph,
  Handler
} from 'lib-common/generated/api-types/dataremoval'

import type { HandlerFilter } from './graph'
import { handlerColors } from './graph'
import {
  columnFontSize,
  fontFamily,
  layoutGraph,
  tableFontSize
} from './layout'

const minScale = 0.05
const maxScale = 8
const fitPadding = 16
const backgroundColor = '#fafafa'

const stackOffset = 3

const Viewport = styled.div`
  position: relative;
  flex: 1;
  overflow: hidden;
  background: ${backgroundColor};
  border: 1px solid #dadada;
  cursor: grab;
  touch-action: none;
  user-select: none;

  &:active {
    cursor: grabbing;
  }
`

const Canvas = styled.div`
  position: absolute;
  top: 0;
  left: 0;
  transform-origin: 0 0;

  svg {
    display: block;
  }

  [data-table] {
    cursor: pointer;
  }
`

const Toolbar = styled.div`
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px;
  background: #ffffffe0;
  border: 1px solid #dadada;
  cursor: default;
`

const ToolbarButton = styled.button`
  min-width: 28px;
  height: 24px;
  padding: 0 6px;
  background: #fff;
  border: 1px solid #c4c4c4;
  cursor: pointer;
  font-size: 13px;

  &:hover {
    background: #eee;
  }
`

const ZoomLevel = styled.span`
  min-width: 44px;
  text-align: center;
  font-size: 12px;
`

interface Props {
  graph: DataRetentionSchemaGraph
  filter: HandlerFilter
  onSelectTable: (table: string | null) => void
}

interface View {
  x: number
  y: number
  scale: number
}

const clampScale = (scale: number) =>
  Math.min(maxScale, Math.max(minScale, scale))

const arrowId = (role: Handler) => `data-retention-arrow-${role}`

export default React.memo(function GraphView({
  graph,
  filter,
  onSelectTable
}: Props) {
  const layout = useMemo(() => layoutGraph(graph, filter), [graph, filter])
  const viewportRef = useRef<HTMLDivElement>(null)
  const [view, setView] = useState<View>({
    x: fitPadding,
    y: fitPadding,
    scale: 1
  })
  const dragRef = useRef<{ x: number; y: number; moved: boolean } | null>(null)
  const draggedRef = useRef(false)

  const fit = useCallback((size: { width: number; height: number }) => {
    const viewport = viewportRef.current
    if (!viewport || size.width <= 0 || size.height <= 0) return
    const rect = viewport.getBoundingClientRect()
    const scale = clampScale(
      Math.min(
        (rect.width - 2 * fitPadding) / size.width,
        (rect.height - 2 * fitPadding) / size.height,
        1
      )
    )
    setView({
      scale,
      x: Math.max(fitPadding, (rect.width - size.width * scale) / 2),
      y: Math.max(fitPadding, (rect.height - size.height * scale) / 2)
    })
  }, [])

  useEffect(() => fit(layout), [layout, fit])

  const zoomAbout = useCallback(
    (factor: number, originX: number, originY: number) =>
      setView((current) => {
        const scale = clampScale(current.scale * factor)
        const ratio = scale / current.scale
        return {
          scale,
          x: originX - (originX - current.x) * ratio,
          y: originY - (originY - current.y) * ratio
        }
      }),
    []
  )

  const zoomFromCentre = useCallback(
    (factor: number) => {
      const viewport = viewportRef.current
      if (!viewport) return
      const rect = viewport.getBoundingClientRect()
      zoomAbout(factor, rect.width / 2, rect.height / 2)
    },
    [zoomAbout]
  )

  // React attaches wheel listeners passively, so zooming needs a native one
  useEffect(() => {
    const viewport = viewportRef.current
    if (!viewport) return

    const onWheel = (e: WheelEvent) => {
      e.preventDefault()
      const rect = viewport.getBoundingClientRect()
      zoomAbout(
        Math.exp(-e.deltaY / 500),
        e.clientX - rect.left,
        e.clientY - rect.top
      )
    }

    viewport.addEventListener('wheel', onWheel, { passive: false })
    return () => viewport.removeEventListener('wheel', onWheel)
  }, [zoomAbout])

  const onPointerDown = useCallback((e: React.PointerEvent<HTMLDivElement>) => {
    dragRef.current = { x: e.clientX, y: e.clientY, moved: false }
  }, [])

  // Not pointer capture, because it would retarget the click that ends a pan away from the table
  useEffect(() => {
    const onMove = (e: PointerEvent) => {
      const drag = dragRef.current
      if (!drag) return
      const dx = e.clientX - drag.x
      const dy = e.clientY - drag.y
      if (dx !== 0 || dy !== 0) drag.moved = true
      drag.x = e.clientX
      drag.y = e.clientY
      setView((current) => ({
        ...current,
        x: current.x + dx,
        y: current.y + dy
      }))
    }
    const onUp = () => {
      draggedRef.current = dragRef.current?.moved ?? false
      dragRef.current = null
    }
    window.addEventListener('pointermove', onMove)
    window.addEventListener('pointerup', onUp)
    window.addEventListener('pointercancel', onUp)
    return () => {
      window.removeEventListener('pointermove', onMove)
      window.removeEventListener('pointerup', onUp)
      window.removeEventListener('pointercancel', onUp)
    }
  }, [])

  const onClick = useCallback(
    (e: React.MouseEvent<HTMLDivElement>) => {
      if (draggedRef.current) {
        draggedRef.current = false
        return
      }
      const table = (e.target as Element).closest('[data-table]')
      onSelectTable(table?.getAttribute('data-table') ?? null)
    },
    [onSelectTable]
  )

  const stopDragging = useCallback((e: React.SyntheticEvent) => {
    e.stopPropagation()
  }, [])

  return (
    <Viewport
      ref={viewportRef}
      onPointerDown={onPointerDown}
      onClick={onClick}
      data-qa="data-retention-graph-viewport"
    >
      <Canvas
        style={{
          transform: `translate(${view.x}px, ${view.y}px) scale(${view.scale})`
        }}
      >
        <svg
          width={layout.width}
          height={layout.height}
          fontFamily={fontFamily}
        >
          <defs>
            {(['CHILD', 'ADULT'] as const).map((role) => (
              <marker
                key={role}
                id={arrowId(role)}
                viewBox="0 0 10 10"
                refX={10}
                refY={5}
                markerWidth={8}
                markerHeight={8}
                markerUnits="userSpaceOnUse"
                orient="auto"
              >
                <path d="M0,0L10,5L0,10z" fill={handlerColors[role]} />
              </marker>
            ))}
          </defs>
          {layout.references.map((reference) => (
            <path
              key={reference.key}
              d={reference.path}
              fill="none"
              stroke={reference.color}
              strokeWidth={reference.width}
              strokeDasharray={reference.dashed ? '4 3' : undefined}
              markerEnd={`url(#${arrowId(reference.role)})`}
            />
          ))}
          {layout.tables.map((table) => (
            <g key={table.name} data-table={table.name}>
              {table.independentRows && (
                <rect
                  x={table.x + stackOffset}
                  y={table.y - stackOffset}
                  width={table.width}
                  height={table.height}
                  fill={table.fillColor}
                  stroke={table.borderColor}
                  strokeWidth={table.borderWidth}
                  strokeDasharray={table.dashed ? '4 3' : undefined}
                />
              )}
              <rect
                x={table.x}
                y={table.y}
                width={table.width}
                height={table.height}
                fill={table.fillColor}
                stroke={table.borderColor}
                strokeWidth={table.borderWidth}
                strokeDasharray={table.dashed ? '4 3' : undefined}
              />
              <text
                x={table.x + table.width / 2}
                y={table.y + table.height / 2}
                fontSize={tableFontSize}
                textAnchor="middle"
                dominantBaseline="central"
              >
                {table.name}
              </text>
            </g>
          ))}
          {layout.references.map((reference) => (
            <text
              key={reference.key}
              x={reference.columnAt.x}
              y={reference.columnAt.y}
              fontSize={columnFontSize}
              fill={reference.color}
              stroke={backgroundColor}
              strokeWidth={3}
              paintOrder="stroke"
              textAnchor="middle"
              dominantBaseline="central"
            >
              {reference.column}
            </text>
          ))}
        </svg>
      </Canvas>
      <Toolbar onPointerDown={stopDragging} onClick={stopDragging}>
        <ToolbarButton onClick={() => zoomFromCentre(1 / 1.3)} title="Zoom out">
          −
        </ToolbarButton>
        <ZoomLevel>{Math.round(view.scale * 100)}%</ZoomLevel>
        <ToolbarButton onClick={() => zoomFromCentre(1.3)} title="Zoom in">
          +
        </ToolbarButton>
        <ToolbarButton
          onClick={() => fit(layout)}
          title="Fit the whole graph in the view"
        >
          Fit
        </ToolbarButton>
      </Toolbar>
    </Viewport>
  )
})
