// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { Td, Tr } from 'lib-components/layout/Table'
import SimpleSelect from 'lib-components/atoms/form/SimpleSelect'
import EuroInput from '../../../../components/common/EuroInput'
import { Translations } from '../../../../state/i18n'
import {
  incomeCoefficients,
  IncomeCoefficient,
  IncomeOption
} from '../../../../types/income'
import { formatCents } from 'lib-common/money'
import { IncomeValueString } from '../IncomeTable'

type Props = {
  i18n: Translations
  type: IncomeOption
  state: IncomeValueString
  onChange?: (option: IncomeOption, value: IncomeValueString) => void
}

export const IncomeTableRow = React.memo(function IncomeTableRow({
  i18n,
  type,
  state,
  onChange
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
        {onChange !== undefined ? (
          <EuroInput
            value={state.amount}
            onChange={(amount) => onChange(type, { ...state, amount })}
            allowEmpty
            data-qa={`income-input-${type.value}`}
          />
        ) : (
          <span>{state.amount} €</span>
        )}
      </Td>
      <Td>
        {type.withCoefficient ? (
          onChange !== undefined ? (
            <SimpleSelect
              value={state.coefficient}
              options={coefficientOptions}
              onChange={(e) =>
                onChange(type, {
                  ...state,
                  coefficient: e.target.value as IncomeCoefficient
                })
              }
              data-qa={`income-coefficient-select-${type.value}`}
            />
          ) : (
            <span>
              {
                i18n.personProfile.income.details.incomeCoefficients[
                  state.coefficient
                ]
              }
            </span>
          )
        ) : undefined}
      </Td>
      <Td align="right">
        <MonthlyValue>{`${formatCents(state.monthlyAmount)} €`} </MonthlyValue>
      </Td>
    </Tr>
  )
})

const TypeLabel = styled.span<{ indent: boolean }>`
  margin-left: ${(props) => (props.indent ? '2rem' : '0')};
`

const MonthlyValue = styled.span`
  font-style: italic;
`

export default IncomeTableRow
