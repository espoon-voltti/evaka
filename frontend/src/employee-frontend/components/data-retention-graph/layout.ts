// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { EdgeLabel, GraphLabel, NodeLabel } from '@dagrejs/dagre'
import { Graph, layout } from '@dagrejs/dagre'

import type {
  DataRetentionReferenceEdge,
  DataRetentionSchemaGraph,
  DataRetentionTableNode,
  Handler
} from 'lib-common/generated/api-types/dataremoval'

import type { HandlerFilter } from './graph'
import {
  externalColor,
  foreignColor,
  handlerColors,
  visibleGraph
} from './graph'

const bundleTints = [
  '#e4eeff',
  '#fdeee0',
  '#e4f5e8',
  '#f3e6fa',
  '#fdf6d8',
  '#e2f3f6',
  '#fce4e4',
  '#e8eafb'
]

const bundleTintsByRoot = (
  tables: DataRetentionTableNode[]
): Map<string, string> => {
  const roots = new Set(
    tables.flatMap((table) =>
      table.deletionBundleRoot === null ? [] : [table.deletionBundleRoot]
    )
  )
  return new Map(
    [...roots].map((root, i) => [root, bundleTints[i % bundleTints.length]])
  )
}

const nodeBorder = (
  table: DataRetentionTableNode,
  foreign: boolean
): string => {
  if (foreign) return foreignColor
  if (table.external) return externalColor
  return table.handledBy ? handlerColors[table.handledBy] : foreignColor
}

const nodeFill = (
  table: DataRetentionTableNode,
  foreign: boolean,
  tintsByBundleRoot: Map<string, string>
): string => {
  if (foreign) return '#f1f1f1'
  if (table.deletionBundleRoot === null) return '#ffffff'
  return tintsByBundleRoot.get(table.deletionBundleRoot) ?? '#ffffff'
}

// Not the page's Open Sans: measuring the text must not race the web font loading
export const fontFamily = 'Arial'
export const tableFontSize = 13
export const columnFontSize = 10
const tablePaddingX = 10
const tableHeight = 22
const columnPaddingX = 2
const columnHeight = 12

export interface Point {
  x: number
  y: number
}

export interface TableBox {
  name: string
  x: number
  y: number
  width: number
  height: number
  independentRows: boolean
  borderColor: string
  borderWidth: number
  dashed: boolean
  fillColor: string
}

export interface ReferenceLine {
  key: string
  path: string
  role: Handler
  color: string
  width: number
  dashed: boolean
  column: string
  columnAt: Point
}

export interface GraphLayout {
  width: number
  height: number
  tables: TableBox[]
  references: ReferenceLine[]
}

// d3's curveBasis
const curve = (points: Point[]): string => {
  const [first, ...rest] = points
  const path = [`M${first.x},${first.y}`]
  if (rest.length < 2) {
    return [...path, ...rest.map((p) => `L${p.x},${p.y}`)].join('')
  }
  path.push(
    `L${(5 * first.x + rest[0].x) / 6},${(5 * first.y + rest[0].y) / 6}`
  )
  for (let i = 2; i <= points.length; i++) {
    const a = points[i - 2]
    const b = points[i - 1]
    const c = points[Math.min(i, points.length - 1)]
    path.push(
      `C${(2 * a.x + b.x) / 3},${(2 * a.y + b.y) / 3} ${(a.x + 2 * b.x) / 3},${(a.y + 2 * b.y) / 3} ${(a.x + 4 * b.x + c.x) / 6},${(a.y + 4 * b.y + c.y) / 6}`
    )
  }
  const last = points[points.length - 1]
  path.push(`L${last.x},${last.y}`)
  return path.join('')
}

export function layoutGraph(
  graph: DataRetentionSchemaGraph,
  filter: HandlerFilter
): GraphLayout {
  const { foreign, visible } = visibleGraph(graph, filter)
  const tintsByBundleRoot = bundleTintsByRoot(graph.tables)
  const tables = graph.tables.filter((table) => visible.has(table.name))
  const references = graph.references.filter(
    (edge) =>
      visible.has(edge.referencingTable) && visible.has(edge.referencedTable)
  )

  const measuringContext = document.createElement('canvas').getContext('2d')!
  const textWidth = (text: string, fontSize: number) => {
    measuringContext.font = `${fontSize}px ${fontFamily}`
    return Math.ceil(measuringContext.measureText(text).width)
  }
  const edgeName = (edge: DataRetentionReferenceEdge) =>
    `${edge.referencingTable}.${edge.referenceColumn}`

  const dagreGraph = new Graph<GraphLabel, NodeLabel, EdgeLabel>({
    multigraph: true
  })
  dagreGraph.setGraph({
    rankdir: 'RL',
    nodesep: 8,
    ranksep: 40,
    edgesep: 6,
    // Room for the stacked back faces and the label halos
    marginx: 4,
    marginy: 4
  })
  for (const table of tables) {
    dagreGraph.setNode(table.name, {
      width: textWidth(table.name, tableFontSize) + 2 * tablePaddingX,
      height: tableHeight
    })
  }
  for (const edge of references) {
    dagreGraph.setEdge(
      edge.referencingTable,
      edge.referencedTable,
      {
        width:
          textWidth(edge.referenceColumn, columnFontSize) + 2 * columnPaddingX,
        height: columnHeight,
        labelpos: 'c'
      },
      edgeName(edge)
    )
  }
  layout(dagreGraph)

  return {
    width: dagreGraph.graph().width ?? 0,
    height: dagreGraph.graph().height ?? 0,
    tables: tables.map((table) => {
      const node = dagreGraph.node(table.name)
      const isForeign = foreign.has(table.name)
      return {
        name: table.name,
        x: node.x! - node.width / 2,
        y: node.y! - node.height / 2,
        width: node.width,
        height: node.height,
        independentRows: table.independentRows,
        borderColor: nodeBorder(table, isForeign),
        borderWidth: isForeign ? 1 : 2,
        dashed: isForeign,
        fillColor: nodeFill(table, isForeign, tintsByBundleRoot)
      }
    }),
    references: references.map((edge) => {
      const laidOut = dagreGraph.edge({
        v: edge.referencingTable,
        w: edge.referencedTable,
        name: edgeName(edge)
      })
      return {
        key: edgeName(edge),
        path: curve(laidOut.points ?? []),
        role: edge.role,
        color: handlerColors[edge.role],
        width:
          edge.kind === 'PRIMARY' ? 2.5 : edge.kind === 'SECONDARY' ? 1.2 : 1,
        dashed: edge.kind === 'OPTIONAL',
        column: edge.referenceColumn,
        columnAt: { x: laidOut.x ?? 0, y: laidOut.y ?? 0 }
      }
    })
  }
}
