import React from 'react'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'
import { H1, H2, Label } from 'lib-components/typography'
import Footer from '../Footer'
import { useTranslation } from '../localization'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import { useHistory } from 'react-router-dom'
import { IncomeStatement } from './types/income-statement'
import { Loading, Result } from 'lib-common/api'
import { getIncomeStatements } from './api'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'

export default function IncomeStatements() {
  const t = useTranslation()
  const history = useHistory()

  const [incomeStatements, setIncomeStatements] = React.useState<
    Result<IncomeStatement[]>
  >(Loading.of())

  React.useEffect(() => {
    void getIncomeStatements().then(setIncomeStatements)
  }, [])

  return (
    <>
      <Container>
        <Gap size="s" />
        <ContentArea opaque paddingVertical="L">
          <H1 noMargin>{t.income.title}</H1>
          {t.income.description}
        </ContentArea>
        <Gap size="s" />
        <ContentArea opaque paddingVertical="L">
          {incomeStatements.mapAll({
            loading() {
              return <SpinnerSegment />
            },
            failure() {
              return <ErrorSegment />
            },
            success(items) {
              return (
                <>
                  <AddButton
                    onClick={() => {
                      history.push('/income/new')
                    }}
                    text={t.income.addNew}
                  />
                  {items.map((incomeStatement) => (
                    <IncomeStatementItem
                      key={incomeStatement.id}
                      incomeStatement={incomeStatement}
                    />
                  ))}
                </>
              )
            }
          })}
        </ContentArea>
      </Container>
      <Footer />
    </>
  )
}

function IncomeStatementItem({
  incomeStatement
}: {
  incomeStatement: IncomeStatement
}) {
  return (
    <div>
      <H2>Tulotiedot ajalle {incomeStatement.startDate.format()} -</H2>
      <FixedSpaceColumn>
        <FixedSpaceRow>
          <Label>Tulotiedot päivitetty</Label>
          <div>{incomeStatement.startDate.format()}</div>
        </FixedSpaceRow>
      </FixedSpaceColumn>
    </div>
  )
}
