// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { useTranslation } from '../../state/i18n'
import { formatCents } from '../../utils/money'

interface Props {
  title: 'rowSubTotal' | 'familyTotal'
  sum: number
}

export default React.memo(function Sum({ title, sum }: Props) {
  const { i18n } = useTranslation()

  return (
    <InvoiceSum>
      <span data-qa="invoice-sum-title">{i18n.invoice.form.sum[title]}</span>
      <span data-qa="invoice-sum-price">{formatCents(sum)}</span>
    </InvoiceSum>
  )
})

const InvoiceSum = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  justify-content: space-between;
  background: ghostwhite;
  padding: 1rem;
  margin: 1rem 0;
`
