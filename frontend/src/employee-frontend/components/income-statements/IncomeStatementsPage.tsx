// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Loading, Paged, Result } from 'lib-common/api'
import { IncomeStatementAwaitingHandler } from 'lib-common/api-types/incomeStatement'
import { formatDate } from 'lib-common/date'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { H1 } from 'lib-components/typography'
import React, { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getIncomeStatementsAwaitingHandler } from '../../api/income-statement'
import { useTranslation } from '../../state/i18n'
import { Gap } from 'lib-components/white-space'
import { AreaFilter } from '../common/Filters'
import { CareArea } from '../../types/unit'
import { getAreas } from '../../api/daycare'
import Pagination from 'lib-components/Pagination'
import { renderResult } from 'lib-components/async-rendering'

function IncomeStatementsList({
  data
}: {
  data: IncomeStatementAwaitingHandler[]
}) {
  const { i18n } = useTranslation()

  if (!data.length) {
    return <div>{i18n.common.noResults}</div>
  }
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

interface SearchParams {
  areas: string[]
  page: number
}

export default React.memo(function IncomeStatementsPage() {
  const { i18n } = useTranslation()

  const [areas, setAreas] = useState<Result<CareArea[]>>(Loading.of())

  const [incomeStatements, setIncomeStatements] = useState<
    Result<Paged<IncomeStatementAwaitingHandler>>
  >(Loading.of())
  const [searchParams, setSearchParams] = useState<SearchParams>({
    areas: [],
    page: 1
  })

  const loadAreas = useRestApi(getAreas, setAreas)
  const loadData = useRestApi(
    getIncomeStatementsAwaitingHandler,
    setIncomeStatements
  )

  useEffect(() => {
    loadAreas()
  }, [loadAreas])

  useEffect(() => {
    loadData(searchParams.areas, searchParams.page)
  }, [loadData, searchParams])

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
      {renderResult(incomeStatements, (incomeStatements) => (
        <ContentArea opaque>
          <IncomeStatementsList data={incomeStatements.data} />
          <Gap size="s" />
          <Pagination
            pages={incomeStatements.pages}
            currentPage={searchParams.page}
            setPage={handlePageChange}
            label={i18n.common.page}
          />
        </ContentArea>
      ))}
    </Container>
  )
})
