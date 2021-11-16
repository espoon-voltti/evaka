// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { IncomeStatementAwaitingHandler } from 'lib-common/api-types/incomeStatement'
import { formatDate } from 'lib-common/date'
import { useApiState } from 'lib-common/utils/useRestApi'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import Pagination from 'lib-components/Pagination'
import { H1 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import React, { useCallback, useState } from 'react'
import { Link } from 'react-router-dom'
import { getAreas } from '../../api/daycare'
import {
  getIncomeStatementsAwaitingHandler,
  IncomeStatementsAwaitingHandlerParams
} from '../../api/income-statement'
import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import { AreaFilter } from '../common/Filters'

function IncomeStatementsList({
  data
}: {
  data: IncomeStatementAwaitingHandler[]
}) {
  const { i18n } = useTranslation()

  return (
    <Table>
      <Thead>
        <Tr>
          <Th>{i18n.incomeStatement.table.customer}</Th>
          <Th>{i18n.incomeStatement.table.area}</Th>
          <Th>{i18n.incomeStatement.table.created}</Th>
          <Th>{i18n.incomeStatement.table.startDate}</Th>
          <Th>{i18n.incomeStatement.table.type}</Th>
        </Tr>
      </Thead>
      <Tbody>
        {data.map((row) => (
          <Tr key={row.id} data-qa="income-statement-row">
            <Td>
              <Link to={`/profile/${row.personId}`}>{row.personName}</Link>
            </Td>
            <Td>{row.primaryCareArea}</Td>
            <Td>{formatDate(row.created)}</Td>
            <Td>{row.startDate.format()}</Td>
            <Td>
              {i18n.incomeStatement.statementTypes[row.type].toLowerCase()}
            </Td>
          </Tr>
        ))}
      </Tbody>
    </Table>
  )
}

export default React.memo(function IncomeStatementsPage() {
  const { i18n } = useTranslation()

  const [areas] = useApiState(getAreas, [])

  const [searchParams, setSearchParams] =
    useState<IncomeStatementsAwaitingHandlerParams>({
      areas: [],
      page: 1,
      pageSize: 50
    })

  const [incomeStatements] = useApiState(
    () => getIncomeStatementsAwaitingHandler(searchParams),
    [searchParams]
  )

  const toggleArea = useCallback(
    (area: string) => () => {
      setSearchParams((prev) => ({
        ...prev,
        areas: prev.areas.includes(area)
          ? prev.areas.filter((t) => t !== area)
          : [...prev.areas, area]
      }))
    },
    []
  )

  const handlePageChange = useCallback((page: number) => {
    setSearchParams((prev) => ({ ...prev, page }))
  }, [])

  return (
    <Container data-qa="income-statements-page">
      {renderResult(areas, (availableAreas) => (
        <ContentArea opaque>
          <H1>{i18n.incomeStatement.table.title}</H1>
          <AreaFilter
            areas={availableAreas}
            toggled={searchParams.areas}
            toggle={toggleArea}
          />
        </ContentArea>
      ))}
      <Gap size="s" />
      {renderResult(incomeStatements, ({ data, pages, total }) => (
        <ContentArea opaque>
          <FixedSpaceRow justifyContent="flex-end">
            {i18n.common.resultCount(total)}
          </FixedSpaceRow>
          <IncomeStatementsList data={data} />
          <Gap size="s" />
          {pages > 1 && (
            <Pagination
              pages={pages}
              currentPage={searchParams.page}
              setPage={handlePageChange}
              label={i18n.common.page}
            />
          )}
        </ContentArea>
      ))}
    </Container>
  )
})
