// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { formatCents } from 'lib-common/money'

import { useTranslation } from '../../state/i18n'

interface Props {
  title: 'rowSubTotal' | 'familyTotal'
  sum: number
  previousSum?: number | undefined
  'data-qa'?: string
}

export default React.memo(function Sum({
  title,
  sum,
  previousSum,
  'data-qa': dataQa
}: Props) {
  const { i18n } = useTranslation()

  return (
    <InvoiceSum data-qa={dataQa}>
      <span>{i18n.invoice.form.sum[title]}</span>
      {previousSum !== undefined ? (
        <span>
          <div data-qa="price">{formatCents(sum, true)}</div>
          <div data-qa="previous-price">
            <s>{formatCents(previousSum, true)}</s>
          </div>
        </span>
      ) : (
        <span data-qa="price">{formatCents(sum, true)}</span>
      )}
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
