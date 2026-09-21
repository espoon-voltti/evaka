// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Suspense, useMemo, useState } from 'react'
import styled from 'styled-components'

import type {
  DataRetentionSchemaGraph,
  DataRetentionTableNode
} from 'lib-common/generated/api-types/dataremoval'
import { useQueryResult } from 'lib-common/query'
import Radio from 'lib-components/atoms/form/Radio'
import { ContentArea } from 'lib-components/layout/Container'
import { H1, H3, H4 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { UnwrapResult } from '../async-rendering'

import type { HandlerFilter } from './graph'
import {
  externalColor,
  foreignColor,
  handlerColors,
  visibleGraph
} from './graph'
import { dataRetentionSchemaQuery } from './queries'

const GraphView = React.lazy(() => import('./GraphView'))

const Page = styled.div`
  display: flex;
  flex-direction: column;
  height: 100vh;
  padding: 16px 24px;
`

const Header = styled(ContentArea)`
  flex-shrink: 0;
`

const Controls = styled.div`
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
`

const Filters = styled.div`
  display: flex;
  gap: 16px;
`

const Legend = styled.div`
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  font-size: 13px;
`

const Swatch = styled.span<{ $color: string }>`
  display: inline-block;
  width: 12px;
  height: 12px;
  margin-right: 6px;
  border: 2px solid ${(p) => p.$color};
  vertical-align: -1px;
`

const Body = styled.div`
  display: flex;
  gap: 16px;
  flex: 1;
  min-height: 0;
`

const Details = styled.div`
  width: 340px;
  flex-shrink: 0;
  overflow: auto;
  padding: 0 4px;
  font-size: 13px;
`

const FieldName = styled.span`
  font-weight: 600;
`

const Row = styled.div`
  margin-bottom: 8px;
  overflow-wrap: anywhere;
`

const filters: HandlerFilter[] = ['ALL', 'CHILD', 'ADULT']

const filterLabels: Record<HandlerFilter, string> = {
  ALL: 'Whole graph',
  CHILD: 'Child-handled',
  ADULT: 'Adult-handled'
}

export default React.memo(function DataRetentionGraphPage() {
  const schema = useQueryResult(dataRetentionSchemaQuery())
  const [filter, setFilter] = useState<HandlerFilter>('ALL')
  const [selected, setSelected] = useState<string | null>(null)

  return (
    <Page>
      <UnwrapResult result={schema}>
        {/* Taking isReloading keeps the graph and its pan and zoom mounted through a refetch */}
        {(graph, _isReloading) => (
          <Graph
            graph={graph}
            filter={filter}
            setFilter={setFilter}
            selected={selected}
            setSelected={setSelected}
          />
        )}
      </UnwrapResult>
    </Page>
  )
})

const Graph = React.memo(function Graph({
  graph,
  filter,
  setFilter,
  selected,
  setSelected
}: {
  graph: DataRetentionSchemaGraph
  filter: HandlerFilter
  setFilter: (filter: HandlerFilter) => void
  selected: string | null
  setSelected: (table: string | null) => void
}) {
  const { own, foreign, visible } = useMemo(
    () => visibleGraph(graph, filter),
    [graph, filter]
  )
  const table = graph.tables.find((t) => t.name === selected) ?? null

  return (
    <>
      <Header $opaque $paddingVertical="s">
        <H1 $noMargin>Data retention schema</H1>
        <Gap $size="s" />
        <Controls>
          <Filters>
            {filters.map((option) => (
              <Radio
                key={option}
                label={filterLabels[option]}
                checked={filter === option}
                onChange={() => setFilter(option)}
                data-qa={`filter-${option}`}
              />
            ))}
          </Filters>
          <Legend>
            <span>
              <Swatch $color={handlerColors.CHILD} />
              child
            </span>
            <span>
              <Swatch $color={handlerColors.ADULT} />
              adult
            </span>
            <span>
              <Swatch $color={externalColor} />
              external
            </span>
            {filter !== 'ALL' && (
              <span>
                <Swatch $color={foreignColor} />
                foreign ({foreign.size})
              </span>
            )}
            <span>
              {own.size} own, {visible.size} shown of {graph.tables.length}
            </span>
          </Legend>
        </Controls>
      </Header>
      <Gap $size="s" />
      <Body>
        <Suspense fallback={null}>
          <GraphView
            graph={graph}
            filter={filter}
            onSelectTable={setSelected}
          />
        </Suspense>
        {table !== null && (
          <Details>
            <TableDetails table={table} graph={graph} />
          </Details>
        )}
      </Body>
    </>
  )
})

const TableDetails = React.memo(function TableDetails({
  table,
  graph
}: {
  table: DataRetentionTableNode
  graph: DataRetentionSchemaGraph
}) {
  const bundle =
    table.deletionBundleRoot === null
      ? []
      : graph.tables
          .filter(
            (t) =>
              t.deletionBundleRoot === table.deletionBundleRoot &&
              t.name !== table.name
          )
          .map((t) => t.name)
  const outgoing = graph.references.filter(
    (r) => r.referencingTable === table.name
  )
  const incoming = graph.references.filter(
    (r) => r.referencedTable === table.name
  )
  const parent = outgoing.find((r) => r.kind === 'PRIMARY')?.referencedTable

  return (
    <>
      <H3 $noMargin>{table.name}</H3>
      <Gap $size="xs" />
      <Row>
        {table.external
          ? 'External: deleted by another job, only waited for'
          : `Handled by ${table.handledBy?.toLowerCase() ?? ''}`}
      </Row>
      <Row>
        {table.independentRows
          ? 'Rows expire and are deleted independently'
          : 'Rows are evaluated and deleted as a whole'}
      </Row>
      <Row>
        <FieldName>Identified by</FieldName> {table.identifiedByCols.join(', ')}
      </Row>
      {table.deletedWithParent && (
        <Row>
          <FieldName>Deleted with parent</FieldName> {parent}
        </Row>
      )}
      {bundle.length > 0 && (
        <Row>
          <FieldName>Deleted together with</FieldName> {bundle.join(', ')}
        </Row>
      )}
      {table.expirationRule !== null && (
        <Row>
          <FieldName>Expires</FieldName> {table.expirationRule}
        </Row>
      )}
      {table.financeFreeze !== null && (
        <Row>
          <FieldName>Finance freeze</FieldName> {table.financeFreeze}
        </Row>
      )}
      {table.safeForIntegrations.length > 0 && (
        <Row>
          <FieldName>Safe for</FieldName> {table.safeForIntegrations.join(', ')}
        </Row>
      )}
      {table.orphansToDelete.length > 0 && (
        <Row>
          <FieldName>Orphans deleted</FieldName>{' '}
          {table.orphansToDelete
            .map((o) => `${o.referencedTable} (${o.referenceColumn})`)
            .join(', ')}
        </Row>
      )}
      {table.queuesAsyncJobs && (
        <Row>
          <FieldName>Queues async jobs</FieldName>{' '}
          {table.asyncJobColumns.length > 0
            ? `reading ${table.asyncJobColumns.join(', ')}`
            : 'on delete'}
        </Row>
      )}
      <Gap $size="xs" />
      <H4 $noMargin>References out</H4>
      {outgoing.length === 0 && <Row>none</Row>}
      {outgoing.map((r) => (
        <Row key={r.referenceColumn}>
          {r.referenceColumn} → {r.referencedTable} ({r.kind.toLowerCase()})
          {r.alsoNull.length > 0 && `, also nulls ${r.alsoNull.join(', ')}`}
        </Row>
      ))}
      <Gap $size="xs" />
      <H4 $noMargin>References in</H4>
      {incoming.length === 0 && <Row>none</Row>}
      {incoming.map((r) => (
        <Row key={`${r.referencingTable}.${r.referenceColumn}`}>
          {r.referencingTable}.{r.referenceColumn} ({r.kind.toLowerCase()})
        </Row>
      ))}
    </>
  )
})
