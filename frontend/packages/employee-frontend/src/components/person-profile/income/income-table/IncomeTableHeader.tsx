// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { Table } from '~components/shared/alpha'
import { Translations } from '~state/i18n'

const TypeHeader = styled(Table.Th)`
  width: 50%;
`

const EuroHeader = styled(Table.Th)`
  width: 8rem;
  text-align: right !important;
`

const CoefficientHeader = styled(Table.Th)`
  width: 16rem;
`

type Props = {
  i18n: Translations
  type: 'income' | 'expenses'
}

const IncomeTableHeader = React.memo(function IncomeTableHeader({
  i18n,
  type
}: Props) {
  return (
    <Table.Head>
      <Table.Row>
        <TypeHeader>{i18n.personProfile.income.details[type]}</TypeHeader>
        <EuroHeader>{i18n.personProfile.income.details.amount}</EuroHeader>
        <CoefficientHeader>
          {i18n.personProfile.income.details.coefficient}
        </CoefficientHeader>
        <EuroHeader>
          {i18n.personProfile.income.details.monthlyAmount}
        </EuroHeader>
      </Table.Row>
    </Table.Head>
  )
})

export default IncomeTableHeader
