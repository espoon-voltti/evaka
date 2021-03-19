// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import ListGrid from 'lib-components/layout/ListGrid'
import { Label } from 'lib-components/typography'
import Title from 'lib-components/atoms/Title'
import IncomeTable from './IncomeTable'
import { Income } from '../../../types/income'
import { useTranslation } from '../../../state/i18n'
import { formatDate } from '../../../utils/date'

interface Props {
  income: Income
}

const IncomeItemBody = React.memo(function IncomeItemBody({ income }: Props) {
  const { i18n } = useTranslation()

  return (
    <>
      <ListGrid labelWidth="fit-content(40%)" rowGap="xs" columnGap="L">
        <Label>{i18n.personProfile.income.details.dateRange}</Label>
        <span>
          {`${income.validFrom.format()} - ${
            income.validTo ? income.validTo.format() : ''
          }`}
        </span>
        <Label>{i18n.personProfile.income.details.effect}</Label>
        <span>
          {i18n.personProfile.income.details.effectOptions[income.effect]}
        </span>
        <Label>{i18n.personProfile.income.details.echa}</Label>
        <span>{income.worksAtECHA ? i18n.common.yes : i18n.common.no}</span>
        <Label>{i18n.personProfile.income.details.entrepreneur}</Label>
        <span>{income.isEntrepreneur ? i18n.common.yes : i18n.common.no}</span>
        <Label>{i18n.personProfile.income.details.updated}</Label>
        <span>{formatDate(income.updatedAt)}</span>
        <Label>{i18n.personProfile.income.details.handler}</Label>
        <span>
          {income.applicationId
            ? i18n.personProfile.income.details.originApplication
            : income.updatedBy}
        </span>
      </ListGrid>
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
