// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import styled from 'styled-components'
import { Table, Tbody } from 'lib-components/layout/Table'
import IncomeTableHeader from './income-table/IncomeTableHeader'
import IncomeTableRow from './income-table/IncomeTableRow'
import IncomeSum from './income-table/IncomeSum'
import { useTranslation } from '../../../state/i18n'
import {
  Income,
  IncomeCoefficient,
  IncomeType,
  incomeTypes,
  expenseTypes
} from '../../../types/income'
import { formatCents, isValidCents, parseCents } from '../../../utils/money'

const Container = styled.div`
  margin-bottom: 40px;

  .table td {
    border: none;
    padding-bottom: 0;
  }
`

const formatData = (data: Partial<Income['data']>): TableIncomeState => {
  const formatted = {} as TableIncomeState
  Object.entries(data).forEach(([type, value]) => {
    if (value && value.amount) {
      formatted[type] = {
        amount: formatCents(value?.amount),
        coefficient: value?.coefficient
      }
    }
  })
  return formatted
}

const parseData = (state: TableIncomeState): Partial<Income['data']> => {
  const data: Partial<Income['data']> = {}
  Object.entries(state).forEach(([type, value]) => {
    if (value) {
      const parsed = parseCents(value.amount)
      const typeValue =
        parsed !== undefined
          ? {
              amount: parsed,
              coefficient: value.coefficient
            }
          : undefined
      data[type] = typeValue
    }
  })
  return data
}

export type TableIncomeState = Partial<
  {
    [Key in IncomeType]: {
      amount: string
      coefficient: IncomeCoefficient
      monthlyAmount: number
    }
  }
>

type NotEditing = { total: number }

type Editing = {
  editing: true
  setData: (data: Partial<Income['data']>) => void
  setValidationError: (v: boolean) => void
}

type CommonProps = {
  data: Partial<Income['data']>
  type: 'income' | 'expenses'
}

type Props = CommonProps & (NotEditing | Editing)

const IncomeTable = React.memo(function IncomeTable(props: Props) {
  const { i18n } = useTranslation()
  const [state, setState] = useState(formatData(props.data))

  useEffect(() => {
    if ('editing' in props) {
      const invalidData = Object.values(state).some(
        (value) => !!value && !!value.amount && !isValidCents(value.amount)
      )

      props.setValidationError(invalidData)

      if (!invalidData) {
        props.setData(parseData(state))
      }
    }
  }, [state])

  const types: readonly IncomeType[] =
    props.type === 'income' ? incomeTypes : expenseTypes

  const shownTypes =
    'editing' in props ? types : types.filter((type) => !!props.data[type])

  const sum =
    'editing' in props
      ? Object.entries(state)
          .filter(([key]) => types.includes(key as IncomeType))
          .map(([, value]) => value?.monthlyAmount ?? 0)
          .reduce((sum, value) => (value ? sum + value : sum), 0)
      : props.total

  return (
    <Container>
      {shownTypes.length > 0 ? (
        <Table>
          <IncomeTableHeader i18n={i18n} type={props.type} />
          <Tbody>
            {shownTypes.map((type) => (
              <IncomeTableRow
                key={type}
                i18n={i18n}
                type={type}
                editing={'editing' in props}
                updateData={setState}
                amount={
                  ('editing' in props
                    ? state[type]?.amount
                    : formatCents(props.data[type]?.amount)) ?? ''
                }
                coefficient={
                  ('editing' in props
                    ? state[type]?.coefficient
                    : props.data[type]?.coefficient) ??
                  'MONTHLY_NO_HOLIDAY_BONUS'
                }
                monthlyAmount={
                  ('editing' in props
                    ? state[type]?.monthlyAmount
                    : props.data[type]?.monthlyAmount) ?? 0
                }
              />
            ))}
          </Tbody>
        </Table>
      ) : null}
      <IncomeSum sum={sum} dataQa={`income-sum-${props.type}`} />
    </Container>
  )
})

export default IncomeTable
