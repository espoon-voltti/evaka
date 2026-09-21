// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type {
  DataRetentionSchemaGraph,
  DataRetentionTableNode,
  Handler
} from 'lib-common/generated/api-types/dataremoval'

export type HandlerFilter = 'ALL' | Handler

export const handlerColors: Record<Handler, string> = {
  CHILD: '#0f7b6c',
  ADULT: '#5b3fa8'
}

export const externalColor = '#b06f00'
export const foreignColor = '#8a8a8a'

const rootTable = 'person'

const ownTables = (
  tables: DataRetentionTableNode[],
  filter: HandlerFilter
): Set<string> =>
  new Set(
    tables
      .filter((table) => filter === 'ALL' || table.handledBy === filter)
      .map((table) => table.name)
  )

const foreignTables = (
  graph: DataRetentionSchemaGraph,
  own: Set<string>
): Set<string> =>
  new Set(
    graph.references
      .filter(
        (edge) =>
          edge.kind === 'SECONDARY' &&
          own.has(edge.referencedTable) &&
          !own.has(edge.referencingTable)
      )
      .map((edge) => edge.referencingTable)
  )

export interface VisibleGraph {
  own: Set<string>
  foreign: Set<string>
  visible: Set<string>
}

export function visibleGraph(
  graph: DataRetentionSchemaGraph,
  filter: HandlerFilter
): VisibleGraph {
  const own = ownTables(graph.tables, filter)
  const foreign = foreignTables(graph, own)
  const visible = new Set([...own, ...foreign, rootTable])
  return { own, foreign, visible }
}
