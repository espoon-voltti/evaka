// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useTranslation } from '../../state/i18n'
import { formatCents } from '../../utils/money'

interface Props {
  title: 'rowSubTotal' | 'familyTotal'
  sum: number
}

const Sum = React.memo(function Sum({ title, sum }: Props) {
  const { i18n } = useTranslation()

  return (
    <div className="invoice-sum">
      <span className="invoice-sum-title" data-qa="invoice-sum-title">
        {i18n.invoice.form.sum[title]}
      </span>
      <span className="invoice-sum-price" data-qa="invoice-sum-price">
        {formatCents(sum)}
      </span>
    </div>
  )
})

export default Sum
