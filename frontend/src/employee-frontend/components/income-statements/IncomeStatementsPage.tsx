// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import { Link } from 'react-router-dom'

import { IncomeStatementAwaitingHandler } from 'lib-common/api-types/incomeStatement'
import { formatDate } from 'lib-common/date'
import { useApiState } from 'lib-common/utils/useRestApi'
import Pagination from 'lib-components/Pagination'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H1 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { getIncomeStatementsAwaitingHandler } from '../../api/income-statement'
import { useTranslation } from '../../state/i18n'
import { InvoicingUiContext } from '../../state/invoicing-ui'
import { renderResult } from '../async-rendering'

import IncomeStatementFilters from './IncomeStatementFilters'

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
              <Link
                to={
                  row.type !== 'CHILD_INCOME'
                    ? `/profile/${row.personId}`
                    : `/child-information/${row.personId}`
                }
              >
                {row.personName}
              </Link>
            </Td>
            <Td>{row.primaryCareArea}</Td>
            <Td>{formatDate(row.created)}</Td>
            <Td>{row.startDate.format()}</Td>
            <Td data-qa="income-statement-type">
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

  const [page, setPage] = useState(1)
  const pageSize = 50

  const {
    invoiceStatements: { searchFilters }
  } = useContext(InvoicingUiContext)

  const [incomeStatements] = useApiState(
    () => getIncomeStatementsAwaitingHandler(page, pageSize, searchFilters),
    [page, searchFilters]
  )

  return (
    <Container data-qa="income-statements-page">
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
          <IncomeStatementsList data={data} />
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
