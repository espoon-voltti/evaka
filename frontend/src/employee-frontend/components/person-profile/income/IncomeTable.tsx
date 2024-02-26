// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'
import styled from 'styled-components'

import { IncomeCoefficient } from 'lib-common/api-types/income'
import {
  IncomeOption,
  IncomeTypeOptions
} from 'lib-common/generated/api-types/invoicing'
import { formatCents } from 'lib-common/money'
import { Table, Tbody } from 'lib-components/layout/Table'

import { useTranslation } from '../../../state/i18n'
import { IncomeFields } from '../../../types/income'

import IncomeSum from './income-table/IncomeSum'
import IncomeTableHeader from './income-table/IncomeTableHeader'
import IncomeTableRow from './income-table/IncomeTableRow'

export interface IncomeValueString {
  amount: string
  coefficient: IncomeCoefficient
  monthlyAmount: number
}

export type IncomeTableData = Partial<Record<string, IncomeValueString>>

export function tableDataFromIncomeFields(
  value: IncomeFields
): IncomeTableData {
  return Object.fromEntries(
    Object.entries(value).map(([key, value]) => [
      key,
      value
        ? {
            ...value,
            amount: formatCents(value.amount)
          }
        : {
            amount: '',
            coefficient: 'MONTHLY_NO_HOLIDAY_BONUS',
            monthlyAmount: 0
          }
    ])
  )
}

interface Props {
  incomeTypeOptions: IncomeTypeOptions
  data: IncomeTableData
  type: 'income' | 'expenses'
  total?: number
  onChange?: (data: IncomeTableData) => void
}

interface TableRow {
  option: IncomeOption
  value: IncomeValueString
}

const emptyValue: IncomeValueString = {
  amount: '',
  coefficient: 'MONTHLY_NO_HOLIDAY_BONUS',
  monthlyAmount: 0
}

function makeTableRows(
  types: IncomeOption[],
  data: IncomeTableData,
  includeEmpty: boolean
): TableRow[] {
  const result: TableRow[] = []
  types.forEach((option) => {
    const value = data[option.value]
    if (value) {
      result.push({
        option,
        value: { ...value, amount: value.amount }
      })
    } else if (includeEmpty) {
      result.push({ option, value: emptyValue })
    }
  })
  return result
}

const IncomeTable = React.memo(function IncomeTable({
  incomeTypeOptions,
  type,
  data,
  total,
  onChange
}: Props) {
  const { i18n } = useTranslation()

  const { incomeTypes, expenseTypes } = incomeTypeOptions
  const selectedTypes: IncomeOption[] =
    type === 'income' ? incomeTypes : expenseTypes

  const editable = onChange !== undefined
  const rows = useMemo(
    () => makeTableRows(selectedTypes, data, editable),
    [data, editable, selectedTypes]
  )

  const handleRowChange = useMemo(
    () =>
      onChange
        ? (option: IncomeOption, value: IncomeValueString) =>
            onChange({ ...data, [option.value]: value })
        : undefined,
    [data, onChange]
  )

  const sum = useMemo(
    () =>
      total ??
      rows
        .map(({ value }) => value.monthlyAmount)
        .reduce((sum, value) => sum + value, 0),
    [total, rows]
  )

  return (
    <Container>
      {rows.length > 0 && (
        <Table>
          <IncomeTableHeader i18n={i18n} type={type} />
          <Tbody>
            {rows.map(({ option, value }) => (
              <IncomeTableRow
                key={option.value}
                i18n={i18n}
                type={option}
                state={value}
                onChange={handleRowChange}
              />
            ))}
          </Tbody>
        </Table>
      )}
      <IncomeSum sum={sum} data-qa={`income-sum-${type}`} />
    </Container>
  )
})

const Container = styled.div`
  margin-bottom: 40px;

  .table td {
    border: none;
    padding-bottom: 0;
  }
`

export default IncomeTable
