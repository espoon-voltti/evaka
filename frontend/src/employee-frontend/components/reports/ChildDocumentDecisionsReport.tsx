// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useCallback, useContext, useMemo, useState } from 'react'
import styled from 'styled-components'
import { useLocation } from 'wouter'

import type { Result } from 'lib-common/api'
import type {
  ChildDocumentDecisionStatus,
  DocumentStatus
} from 'lib-common/generated/api-types/document'
import type { SortDirection } from 'lib-common/generated/api-types/invoicing'
import type { ChildDocumentId } from 'lib-common/generated/api-types/shared'
import type HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { evakaUserId } from 'lib-common/id-type'
import { formatPersonName } from 'lib-common/names'
import { useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { ChildDocumentStateChip } from 'lib-components/document-templates/ChildDocumentStateChip'
import { Container, ContentArea } from 'lib-components/layout/Container'
import {
  SortableTh,
  Table,
  Tbody,
  Td,
  Thead,
  Tr
} from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'
import { UserContext } from '../../state/user'
import { renderResult } from '../async-rendering'

import { FilterLabel, FilterRow } from './common'
import { childDocumentDecisionsReportQuery } from './queries'

const Highlight = styled.div`
  background-color: ${(p) => p.theme.colors.status.success};
  width: 6px;
  position: absolute;
  left: 0.5px;
  top: 0.5px;
  bottom: 0.5px;
`

const RelativeTr = styled(Tr)`
  position: relative;
`

interface SortableRow {
  id: ChildDocumentId
  highlighted: boolean
  templateName: string
  childName: string
  modifiedAt: HelsinkiDateTime
  decisionMaker: string
  decisionMade: HelsinkiDateTime | null
  status: DocumentStatus | ChildDocumentDecisionStatus
  statusIndex: number
}

type SortColumn = Exclude<keyof SortableRow, 'id' | 'highlighted' | 'status'>

const descColumns: SortColumn[] = ['modifiedAt', 'decisionMade'] as const

const filterableStatuses = [
  'DRAFT',
  'DECISION_PROPOSAL',
  'ACCEPTED',
  'REJECTED',
  'ANNULLED'
] as const

const getStatusIndex = (
  status: DocumentStatus | ChildDocumentDecisionStatus
): number => {
  switch (status) {
    case 'DECISION_PROPOSAL':
      return 0
    case 'DRAFT':
      return 1
    case 'ACCEPTED':
      return 2
    case 'REJECTED':
      return 3
    case 'ANNULLED':
      return 4
    default:
      return 99
  }
}

export default React.memo(function ChildDocumentDecisionsReport() {
  const { i18n } = useTranslation()
  const [, navigate] = useLocation()
  const { user } = useContext(UserContext)

  const [shownStatuses, setShownStatuses] = useState<
    (DocumentStatus | ChildDocumentDecisionStatus)[]
  >(['DRAFT', 'DECISION_PROPOSAL'])

  const [includeEnded, setIncludeEnded] = useState(false)

  const report = useQueryResult(
    childDocumentDecisionsReportQuery({
      statuses: shownStatuses,
      includeEnded
    })
  )

  const [primarySortColumn, setPrimarySortColumn] =
    useState<SortColumn>('templateName')
  const [sortDirection, setSortDirection] = useState<SortDirection>('ASC')

  const sortBy = useCallback(
    (column: SortColumn) => {
      if (primarySortColumn === column) {
        setSortDirection(sortDirection === 'ASC' ? 'DESC' : 'ASC')
      } else {
        setPrimarySortColumn(column)
        setSortDirection(descColumns.includes(column) ? 'DESC' : 'ASC')
      }
    },
    [primarySortColumn, sortDirection]
  )

  const sortableRows: Result<SortableRow[]> = useMemo(
    () =>
      report.map((rows) =>
        rows.map((row) => ({
          id: row.id,
          highlighted:
            !!user &&
            !!row.decisionMaker &&
            row.decisionMaker.id === evakaUserId(user.id) &&
            row.status === 'DECISION_PROPOSAL',
          templateName: row.templateName,
          childName: formatPersonName(
            { lastName: row.childLastName, firstName: row.childFirstName },
            'Last First'
          ),
          modifiedAt: row.modifiedAt,
          decisionMaker: row.decisionMaker?.name ?? '',
          decisionMade: row.decision?.createdAt ?? null,
          status: row.decision?.status ?? row.status,
          statusIndex: getStatusIndex(row.decision?.status ?? row.status)
        }))
      ),
    [report, user]
  )

  const sortedRows: Result<SortableRow[]> = useMemo(
    () =>
      sortableRows.map((rows) =>
        orderBy(
          rows,
          [
            'highlighted',
            primarySortColumn,
            'statusIndex',
            'decisionMade',
            'modifiedAt'
          ],
          [
            'desc',
            sortDirection === 'ASC' ? 'asc' : 'desc',
            'asc',
            'desc',
            'desc'
          ]
        )
      ),
    [sortableRows, primarySortColumn, sortDirection]
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.childDocumentDecisions.title}</Title>

        <FilterRow>
          <FilterLabel>
            {i18n.reports.childDocumentDecisions.statusFilter}
          </FilterLabel>
          <FixedSpaceRow>
            {filterableStatuses.map((status) => (
              <Checkbox
                key={status}
                label={i18n.documentTemplates.documentStates[status]}
                checked={shownStatuses.includes(status)}
                onChange={(checked) =>
                  setShownStatuses((prev) =>
                    checked
                      ? [...prev, status]
                      : prev.filter((s) => s !== status)
                  )
                }
              />
            ))}
          </FixedSpaceRow>
        </FilterRow>

        <Gap size="xs" />

        <FilterRow>
          <FilterLabel>
            {i18n.reports.childDocumentDecisions.otherFilters}
          </FilterLabel>
          <Checkbox
            label={i18n.reports.childDocumentDecisions.includeEnded}
            checked={includeEnded}
            onChange={setIncludeEnded}
          />
        </FilterRow>

        {renderResult(sortedRows, (rows) => (
          <Table>
            <Thead>
              <Tr>
                <SortableTh
                  sorted={
                    primarySortColumn === 'templateName'
                      ? sortDirection
                      : undefined
                  }
                  onClick={() => sortBy('templateName')}
                >
                  {i18n.reports.childDocumentDecisions.templateName}
                </SortableTh>
                <SortableTh
                  sorted={
                    primarySortColumn === 'childName'
                      ? sortDirection
                      : undefined
                  }
                  onClick={() => sortBy('childName')}
                >
                  {i18n.reports.childDocumentDecisions.childName}
                </SortableTh>
                <SortableTh
                  sorted={
                    primarySortColumn === 'modifiedAt'
                      ? sortDirection
                      : undefined
                  }
                  onClick={() => sortBy('modifiedAt')}
                >
                  {i18n.reports.childDocumentDecisions.modifiedAt}
                </SortableTh>
                <SortableTh
                  sorted={
                    primarySortColumn === 'decisionMaker'
                      ? sortDirection
                      : undefined
                  }
                  onClick={() => sortBy('decisionMaker')}
                >
                  {i18n.reports.childDocumentDecisions.decisionMaker}
                </SortableTh>
                <SortableTh
                  sorted={
                    primarySortColumn === 'decisionMade'
                      ? sortDirection
                      : undefined
                  }
                  onClick={() => sortBy('decisionMade')}
                >
                  {i18n.reports.childDocumentDecisions.decisionMade}
                </SortableTh>
                <SortableTh
                  sorted={
                    primarySortColumn === 'statusIndex'
                      ? sortDirection
                      : undefined
                  }
                  onClick={() => sortBy('statusIndex')}
                >
                  {i18n.reports.childDocumentDecisions.status}
                </SortableTh>
              </Tr>
            </Thead>
            <Tbody data-qa="report-table">
              {rows.map((row) => (
                <RelativeTr
                  key={row.id}
                  onClick={() => navigate(`/child-documents/${row.id}`)}
                >
                  <Td>
                    {row.highlighted && <Highlight />}
                    {row.templateName}
                  </Td>
                  <Td>{row.childName}</Td>
                  <Td>{row.modifiedAt.toLocalDate().format()}</Td>
                  <Td>{row.decisionMaker}</Td>
                  <Td>{row.decisionMade?.toLocalDate()?.format() ?? ''}</Td>
                  <Td>
                    <ChildDocumentStateChip status={row.status} />
                  </Td>
                </RelativeTr>
              ))}
            </Tbody>
          </Table>
        ))}
      </ContentArea>
    </Container>
  )
})
