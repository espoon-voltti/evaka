// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Dispatch, SetStateAction } from 'react'
import styled from 'styled-components'

import { Td, Tr } from 'lib-components/layout/Table'
import SimpleSelect from 'lib-components/atoms/form/SimpleSelect'
import EuroInput from '../../../../components/common/EuroInput'
import { TableIncomeState } from '../IncomeTable'
import { Translations } from '../../../../state/i18n'
import {
  incomeCoefficients,
  IncomeCoefficient,
  IncomeOption
} from '../../../../types/income'
import { formatCents, parseCents } from 'lib-common/money'

type TypeLabelProps = { indent: boolean }

const TypeLabel = styled.span<TypeLabelProps>`
  margin-left: ${(props) => (props.indent ? '2rem' : '0')};
`

const MonthlyValue = styled.span`
  font-style: italic;
`

const coefficientMultipliers: Record<IncomeCoefficient, number> = {
  MONTHLY_WITH_HOLIDAY_BONUS: 1.0417,
  MONTHLY_NO_HOLIDAY_BONUS: 1,
  BI_WEEKLY_WITH_HOLIDAY_BONUS: 2.2323,
  BI_WEEKLY_NO_HOLIDAY_BONUS: 2.1429,
  DAILY_ALLOWANCE_21_5: 21.5,
  DAILY_ALLOWANCE_25: 25,
  YEARLY: 0.0833
}

const calculateMonthlyAmount = (
  amount: string,
  coefficient: IncomeCoefficient
) => {
  const parsed = parseCents(amount)
  return parsed ? Math.round(parsed * coefficientMultipliers[coefficient]) : 0
}

type Props = {
  i18n: Translations
  type: IncomeOption
  editing: boolean
  amount: string
  coefficient: IncomeCoefficient
  monthlyAmount: number
  updateData: Dispatch<SetStateAction<TableIncomeState>>
}

const IncomeTableRow = React.memo(function IncomeTableRow({
  i18n,
  type,
  editing,
  amount,
  coefficient,
  monthlyAmount,
  updateData
}: Props) {
  const coefficientOptions = incomeCoefficients.map((id) => ({
    value: id,
    label: i18n.personProfile.income.details.incomeCoefficients[id]
  }))

  return (
    <Tr key={type.value}>
      <Td>
        <TypeLabel indent={type.isSubType}>{type.nameFi}</TypeLabel>
      </Td>
      <Td align="right">
        {editing ? (
          <EuroInput
            value={amount}
            onChange={(amount) =>
              updateData((prev) => ({
                ...prev,
                [type.value]: {
                  amount,
                  coefficient:
                    prev[type.value]?.coefficient ?? 'MONTHLY_NO_HOLIDAY_BONUS',
                  monthlyAmount: undefined
                }
              }))
            }
            allowEmpty
            data-qa={`income-input-${type.value}`}
          />
        ) : (
          <span>{amount} €</span>
        )}
      </Td>
      <Td>
        {type.withCoefficient ? (
          editing ? (
            <SimpleSelect
              value={coefficient}
              options={coefficientOptions}
              onChange={(e) => {
                updateData((prev) => ({
                  ...prev,
                  [type.value]: {
                    amount: prev[type.value]?.amount ?? '',
                    coefficient: e.target.value as IncomeCoefficient,
                    monthlyAmount: undefined
                  }
                }))
              }}
              data-qa={`income-coefficient-select-${type.value}`}
            />
          ) : (
            <span>
              {coefficient
                ? i18n.personProfile.income.details.incomeCoefficients[
                    coefficient
                  ]
                : ''}
            </span>
          )
        ) : undefined}
      </Td>
      <Td align="right">
        <MonthlyValue>
          {`${
            formatCents(
              monthlyAmount || calculateMonthlyAmount(amount, coefficient)
            ) ?? ''
          } €`}
        </MonthlyValue>
      </Td>
    </Tr>
  )
})

export default IncomeTableRow
