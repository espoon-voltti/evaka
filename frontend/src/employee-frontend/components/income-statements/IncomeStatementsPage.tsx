// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useContext, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { isLoading } from 'lib-common/api'
import {
  IncomeStatementAwaitingHandler,
  IncomeStatementSortParam
} from 'lib-common/generated/api-types/incomestatement'
import { SortDirection } from 'lib-common/generated/api-types/invoicing'
import { queryOrDefault, useQueryResult } from 'lib-common/query'
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

const pageSize = 50

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
          <Th>{i18n.incomeStatement.table.customer}</Th>
          <Th>{i18n.incomeStatement.table.area}</Th>
          <SortableTh
            sorted={isSorted('CREATED')}
            onClick={toggleSort('CREATED')}
          >
            {i18n.incomeStatement.table.created}
          </SortableTh>
          <SortableTh
            sorted={isSorted('START_DATE')}
            onClick={toggleSort('START_DATE')}
          >
            {i18n.incomeStatement.table.startDate}
          </SortableTh>
          <Th>{i18n.incomeStatement.table.type}</Th>
          <Th minimalWidth={true}>{i18n.incomeStatement.table.link}</Th>
          <Th minimalWidth={true}>{i18n.incomeStatement.table.note}</Th>
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
            <Td>{row.created.toLocalDate().format()}</Td>
            <Td>{row.startDate.format()}</Td>
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

  const [page, setPage] = useState(1)
  const [sortBy, setSortBy] = useState<IncomeStatementSortParam>('CREATED')
  const [sortDirection, setSortDirection] = useState<SortDirection>('ASC')

  const {
    incomeStatements: { searchFilters }
  } = useContext(InvoicingUiContext)

  const incomeStatements = useQueryResult(
    queryOrDefault(incomeStatementsAwaitingHandlerQuery, {
      data: [],
      pages: 0,
      total: 0
    })(
      searchFilters.sentStartDate == null ||
        searchFilters.sentEndDate == null ||
        searchFilters.sentStartDate.isEqualOrBefore(searchFilters.sentEndDate)
        ? {
            body: {
              page,
              pageSize,
              sortBy,
              sortDirection,
              areas: searchFilters.area.length > 0 ? searchFilters.area : null,
              providerTypes:
                searchFilters.providerTypes.length > 0
                  ? searchFilters.providerTypes
                  : null,
              sentStartDate: searchFilters.sentStartDate ?? null,
              sentEndDate: searchFilters.sentEndDate ?? null,
              placementValidDate: searchFilters.placementValidDate ?? null
            }
          }
        : undefined
    )
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
      {renderResult(incomeStatements, ({ data, pages, total }) => (
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
