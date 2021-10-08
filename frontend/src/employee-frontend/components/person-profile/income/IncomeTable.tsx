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
import { Income, IncomeCoefficient, IncomeOption } from '../../../types/income'
import { formatCents, isValidCents, parseCents } from 'lib-common/money'
import { Result, Success } from 'lib-common/api'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { getIncomeOptions, IncomeTypeOptions } from '../../../api/income'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'

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
        amount: formatCents(value.amount),
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
  Record<
    string,
    {
      amount: string
      coefficient: IncomeCoefficient
      monthlyAmount?: number
    }
  >
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
  }, [state]) // eslint-disable-line react-hooks/exhaustive-deps

  const [incomeOptions, setIncomeOptions] = useState<Result<IncomeTypeOptions>>(
    Success.of([[], []])
  )
  const loadIncomeOptions = useRestApi(getIncomeOptions, setIncomeOptions)
  useEffect(() => {
    loadIncomeOptions()
  }, [loadIncomeOptions])

  if (incomeOptions.isLoading) {
    return <SpinnerSegment />
  }
  if (incomeOptions.isFailure) {
    return <ErrorSegment />
  }

  const [incomeTypes, expenseTypes] = incomeOptions.value
  const types: readonly IncomeOption[] =
    props.type === 'income' ? incomeTypes : expenseTypes

  const shownTypes =
    'editing' in props
      ? types
      : types.filter((type) => !!props.data[type.value])

  const sum =
    'editing' in props
      ? Object.entries(state)
          .filter(([key]) => types.map((type) => type.value).includes(key))
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
                key={type.value}
                i18n={i18n}
                type={type}
                editing={'editing' in props}
                updateData={setState}
                amount={
                  ('editing' in props
                    ? state[type.value]?.amount
                    : formatCents(props.data[type.value]?.amount)) ?? ''
                }
                coefficient={
                  ('editing' in props
                    ? state[type.value]?.coefficient
                    : props.data[type.value]?.coefficient) ??
                  'MONTHLY_NO_HOLIDAY_BONUS'
                }
                monthlyAmount={
                  ('editing' in props
                    ? state[type.value]?.monthlyAmount
                    : props.data[type.value]?.monthlyAmount) ?? 0
                }
              />
            ))}
          </Tbody>
        </Table>
      ) : null}
      <IncomeSum sum={sum} data-qa={`income-sum-${props.type}`} />
    </Container>
  )
})

export default IncomeTable
