// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { useTranslation } from '~state/i18n'
import { formatCents } from '~utils/money'

const Container = styled.div`
  display: flex;
  justify-content: space-between;
  background: ghostwhite;
  padding: 20px 16px;
  font-weight: 600;
  margin-bottom: 20px;
`

const IncomeSum = React.memo(function IncomeSum({
  sum,
  dataQa
}: {
  sum: number
  dataQa?: string
}) {
  const { i18n } = useTranslation()
  return (
    <Container data-qa={dataQa}>
      <span>{i18n.personProfile.income.details.sum}</span>
      <span>{formatCents(sum)} €</span>
    </Container>
  )
})

export default IncomeSum
