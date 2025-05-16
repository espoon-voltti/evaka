// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useContext, useState } from 'react'
import { Link } from 'react-router'
import styled from 'styled-components'

import { isLoading } from 'lib-common/api'
import type {
  IncomeStatementAwaitingHandler,
  IncomeStatementSortParam
} from 'lib-common/generated/api-types/incomestatement'
import type { SortDirection } from 'lib-common/generated/api-types/invoicing'
import { constantQuery, useQueryResult } from 'lib-common/query'
import Pagination from 'lib-components/Pagination'
import Tooltip from 'lib-components/atoms/Tooltip'
import { Container, ContentArea } from 'lib-components/layout/Container'
import {
  SortableTh,
  Table,
  Tbody,
  Td,
  Th,
  Thead,
  Tr
} from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H1 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faCommentAlt, fasCommentAltLines, fasFile } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { InvoicingUiContext } from '../../state/invoicing-ui'
import { renderResult } from '../async-rendering'

import IncomeStatementFilters from './IncomeStatementFilters'
import { incomeStatementsAwaitingHandlerQuery } from './queries'

function IncomeStatementsList({
  data,
  sortBy,
  setSortBy,
  sortDirection,
  setSortDirection
}: {
  data: IncomeStatementAwaitingHandler[]
  sortBy: IncomeStatementSortParam
  setSortBy: (sortBy: IncomeStatementSortParam) => void
  sortDirection: SortDirection
  setSortDirection: (sortDirection: SortDirection) => void
}) {
  const { i18n } = useTranslation()

  const isSorted = (column: IncomeStatementSortParam) =>
    sortBy === column ? sortDirection : undefined

  const toggleSort = (column: IncomeStatementSortParam) => () => {
    if (sortBy === column) {
      setSortDirection(sortDirection === 'ASC' ? 'DESC' : 'ASC')
    } else {
      setSortBy(column)
      setSortDirection('ASC')
    }
  }

  return (
    <Table>
      <Thead>
        <Tr>
          <SortableTh
            sorted={isSorted('PERSON_NAME')}
            onClick={toggleSort('PERSON_NAME')}
          >
            {i18n.incomeStatement.table.customer}
          </SortableTh>
          <Th>{i18n.incomeStatement.table.area}</Th>
          <SortableTh
            sorted={isSorted('SENT_AT')}
            onClick={toggleSort('SENT_AT')}
          >
            {i18n.incomeStatement.table.sentAt}
          </SortableTh>
          <SortableTh
            sorted={isSorted('START_DATE')}
            onClick={toggleSort('START_DATE')}
          >
            {i18n.incomeStatement.table.startDate}
          </SortableTh>
          <SortableTh
            sorted={isSorted('INCOME_END_DATE')}
            onClick={toggleSort('INCOME_END_DATE')}
          >
            {i18n.incomeStatement.table.incomeEndDate}
          </SortableTh>
          <SortableTh sorted={isSorted('TYPE')} onClick={toggleSort('TYPE')}>
            {i18n.incomeStatement.table.type}
          </SortableTh>
          <Th minimalWidth={true}>{i18n.incomeStatement.table.link}</Th>
          <SortableTh
            sorted={isSorted('HANDLER_NOTE')}
            onClick={toggleSort('HANDLER_NOTE')}
          >
            {i18n.incomeStatement.table.note}
          </SortableTh>
        </Tr>
      </Thead>
      <Tbody>
        {data.map((row) => (
          <Tr key={row.id} data-qa="income-statement-row">
            <Td>
              <Link
                to={
                  row.type !== 'CHILD_INCOME'
                    ? `/profile/${row.personId}`
                    : `/child-information/${row.personId}`
                }
                data-qa="person-link"
              >
                {row.personName}
              </Link>
            </Td>
            <Td>{row.primaryCareArea}</Td>
            <Td>{row.sentAt.toLocalDate().format()}</Td>
            <Td>{row.startDate.format()}</Td>
            <Td>{row.incomeEndDate?.format() ?? '-'}</Td>
            <Td data-qa="income-statement-type">
              {i18n.incomeStatement.statementTypes[row.type].toLowerCase()}
            </Td>
            <Td>
              <Link
                to={`/profile/${encodeURIComponent(
                  row.personId
                )}/income-statement/${encodeURIComponent(row.id)}`}
              >
                <RowIcon icon={fasFile} />
              </Link>
            </Td>
            <Td>
              <Tooltip tooltip={row.handlerNote || i18n.incomeStatement.noNote}>
                <RowIcon
                  icon={row.handlerNote ? fasCommentAltLines : faCommentAlt}
                />
              </Tooltip>
            </Td>
          </Tr>
        ))}
      </Tbody>
    </Table>
  )
}

const RowIcon = styled(FontAwesomeIcon)`
  cursor: pointer;
  color: ${colors.main.m2};
  font-size: 20px;
`

export default React.memo(function IncomeStatementsPage() {
  const { i18n } = useTranslation()

  const [sortBy, setSortBy] = useState<IncomeStatementSortParam>('SENT_AT')
  const [sortDirection, setSortDirection] = useState<SortDirection>('ASC')

  const {
    incomeStatements: { confirmedSearchFilters: searchFilters, page, setPage }
  } = useContext(InvoicingUiContext)

  const incomeStatements = useQueryResult(
    searchFilters !== undefined &&
      (searchFilters.sentStartDate === null ||
        searchFilters.sentEndDate === null ||
        searchFilters.sentEndDate.isEqualOrAfter(searchFilters.sentStartDate))
      ? incomeStatementsAwaitingHandlerQuery({
          body: {
            areas: searchFilters.area.length > 0 ? searchFilters.area : null,
            unit: searchFilters.unit ?? null,
            providerTypes:
              searchFilters.providerTypes.length > 0
                ? searchFilters.providerTypes
                : null,
            sentStartDate: searchFilters.sentStartDate ?? null,
            sentEndDate: searchFilters.sentEndDate ?? null,
            placementValidDate: searchFilters.placementValidDate ?? null,
            page,
            sortBy,
            sortDirection
          }
        })
      : constantQuery({ data: [], pages: 0, total: 0 })
  )

  return (
    <Container
      data-qa="income-statements-page"
      data-isloading={isLoading(incomeStatements)}
    >
      <ContentArea opaque>
        <H1>{i18n.incomeStatement.table.title}</H1>
        <IncomeStatementFilters />
      </ContentArea>
      <Gap size="s" />
      {searchFilters !== undefined &&
        renderResult(incomeStatements, ({ data, pages, total }) => (
          <ContentArea opaque>
            <FixedSpaceRow justifyContent="flex-end">
              {i18n.common.resultCount(total)}
            </FixedSpaceRow>
            <IncomeStatementsList
              data={data}
              sortBy={sortBy}
              setSortBy={setSortBy}
              sortDirection={sortDirection}
              setSortDirection={setSortDirection}
            />
            <Gap size="s" />
            {pages > 1 && (
              <Pagination
                pages={pages}
                currentPage={page}
                setPage={setPage}
                label={i18n.common.page}
              />
            )}
          </ContentArea>
        ))}
    </Container>
  )
})
