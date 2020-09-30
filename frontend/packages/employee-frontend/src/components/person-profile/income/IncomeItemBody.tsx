// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Title } from '~components/shared/alpha'
import IncomeTable from './IncomeTable'
import { Income } from '~types/income'
import IncomeItemDetails from './IncomeItemDetails'
import { useTranslation } from '~state/i18n'

interface Props {
  income: Income
}

const IncomeItemBody = React.memo(function IncomeItemBody({ income }: Props) {
  const { i18n } = useTranslation()

  return (
    <>
      <IncomeItemDetails income={income} />
      {income.effect === 'INCOME' ? (
        <>
          <div className="separator" />
          <Title size={4}>
            {i18n.personProfile.income.details.incomeTitle}
          </Title>
          <IncomeTable
            data={income.data}
            setData={() => undefined}
            type="income"
            total={income.totalIncome}
          />
          <Title size={4}>
            {i18n.personProfile.income.details.expensesTitle}
          </Title>
          <IncomeTable
            data={income.data}
            setData={() => undefined}
            type="expenses"
            total={income.totalExpenses}
          />
        </>
      ) : null}
    </>
  )
})

export default IncomeItemBody
