import { Loading, Result } from 'lib-common/api'
import { IncomeStatementAwaitingHandler } from 'lib-common/api-types/incomeStatement'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { Container, ContentArea } from 'lib-components/layout/Container'
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
    <ul>
      {data.map(({ id, personId, personName, type }) => (
        <li key={id}>
          <Link to={`/profile/${personId}`}>{personName}</Link>
          {` (${i18n.incomeStatement.statementTypes[type].toLowerCase()})`}
        </li>
      ))}
    </ul>
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
        <H1>{i18n.incomeStatements.title}</H1>
        {renderResult(result, (data) => (
          <IncomeStatementsList data={data} />
        ))}
      </ContentArea>
    </Container>
  )
})
