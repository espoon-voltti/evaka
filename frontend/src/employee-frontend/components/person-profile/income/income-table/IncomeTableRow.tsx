// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { Td, Tr } from 'lib-components/layout/Table'
import Select from 'lib-components/atoms/dropdowns/Select'
import EuroInput from '../../../../components/common/EuroInput'
import { Translations } from '../../../../state/i18n'
import { incomeCoefficients, IncomeOption } from '../../../../types/income'
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
            <Select
              selectedItem={state.coefficient}
              items={[...incomeCoefficients]}
              getItemLabel={(item) =>
                i18n.personProfile.income.details.incomeCoefficients[item]
              }
              onChange={(coefficient) =>
                coefficient &&
                onChange(type, {
                  ...state,
                  coefficient
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
