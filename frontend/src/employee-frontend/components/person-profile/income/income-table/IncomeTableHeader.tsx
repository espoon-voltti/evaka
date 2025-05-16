// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { Th, Thead, Tr } from 'lib-components/layout/Table'

import type { Translations } from '../../../../state/i18n'

const TypeHeader = styled(Th)`
  width: 50%;
`

const EuroHeader = styled(Th)`
  width: 8rem;
  text-align: right !important;
`

const CoefficientHeader = styled(Th)`
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
    <Thead>
      <Tr>
        <TypeHeader>{i18n.personProfile.income.details[type]}</TypeHeader>
        <EuroHeader>{i18n.personProfile.income.details.amount}</EuroHeader>
        <CoefficientHeader>
          {i18n.personProfile.income.details.coefficient}
        </CoefficientHeader>
        <EuroHeader>
          {i18n.personProfile.income.details.monthlyAmount}
        </EuroHeader>
      </Tr>
    </Thead>
  )
})

export default IncomeTableHeader
