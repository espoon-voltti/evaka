// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import LabelValueList from '~components/common/LabelValueList'
import CollapsibleSection from '@evaka/lib-components/src/molecules/CollapsibleSection'
import { faMoneyCheck } from '@evaka/lib-icons'
import { useTranslation } from '../../state/i18n'
import { InvoiceDetailed } from '../../types/invoicing'

interface Props {
  invoice: InvoiceDetailed
}

const InvoiceDetailsSection = React.memo(function InvoiceDetailsSection({
  invoice
}: Props) {
  const { i18n } = useTranslation()

  return (
    <CollapsibleSection
      title={i18n.invoice.form.details.title}
      icon={faMoneyCheck}
      startCollapsed={false}
    >
      <LabelValueList
        spacing="small"
        contents={[
          {
            label: i18n.invoice.form.details.status,
            value: i18n.invoice.status[invoice.status]
          },
          {
            label: i18n.invoice.form.details.range,
            value: `${invoice.periodStart.format()} - ${invoice.periodEnd.format()}`
          },
          {
            label: i18n.invoice.form.details.number,
            value: invoice.number
          },
          {
            label: i18n.invoice.form.details.dueDate,
            value: invoice.dueDate.format()
          },
          {
            label: i18n.invoice.form.details.account,
            value: invoice.account
          },
          {
            label: i18n.invoice.form.details.agreementType,
            value: invoice.agreementType
          }
        ]}
      />
    </CollapsibleSection>
  )
})

export default InvoiceDetailsSection
