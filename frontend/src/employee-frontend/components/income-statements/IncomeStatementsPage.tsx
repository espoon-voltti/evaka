// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Loading, Result } from 'lib-common/api'
import { IncomeStatementAwaitingHandler } from 'lib-common/api-types/incomeStatement'
import { formatDate } from 'lib-common/date'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { H1 } from 'lib-components/typography'
import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getIncomeStatementsAwaitingHandler } from '../../api/income-statement'
import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

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

export default React.memo(function IncomeStatementsPage() {
  const { i18n } = useTranslation()

  const [result, setResult] = useState<
    Result<IncomeStatementAwaitingHandler[]>
  >(Loading.of())
  const loadData = useRestApi(getIncomeStatementsAwaitingHandler, setResult)
  useEffect(() => loadData(), [loadData])

  return (
    <Container data-qa="income-statements-page">
      <ContentArea opaque>
        <H1>{i18n.incomeStatement.table.title}</H1>
        {renderResult(result, (data) => (
          <IncomeStatementsList data={data} />
        ))}
      </ContentArea>
    </Container>
  )
})
