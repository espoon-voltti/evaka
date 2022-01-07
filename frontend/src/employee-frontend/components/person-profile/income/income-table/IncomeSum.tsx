// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { formatCents } from 'lib-common/money'
import { fontWeights } from 'lib-components/typography'
import { useTranslation } from '../../../../state/i18n'

const Container = styled.div`
  display: flex;
  justify-content: space-between;
  background: ghostwhite;
  padding: 20px 16px;
  font-weight: ${fontWeights.semibold};
  margin-bottom: 20px;
`

const IncomeSum = React.memo(function IncomeSum({
  sum,
  'data-qa': dataQa
}: {
  sum: number
  'data-qa'?: string
}) {
  const { i18n } = useTranslation()
  return (
    <Container>
      <span>{i18n.personProfile.income.details.sum}</span>
      <span data-qa={dataQa}>{formatCents(sum)} €</span>
    </Container>
  )
})

export default IncomeSum
