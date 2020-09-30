// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import {
  Collapsible,
  LabelValueList,
  LabelValueListItem
} from '~components/shared/alpha'
import { faMoneyCheck } from 'icon-set'
import { useTranslation } from '../../state/i18n'
import { InvoiceDetailed } from '../../types/invoicing'

interface Props {
  invoice: InvoiceDetailed
}

const InvoiceDetailsSection = React.memo(function InvoiceDetailsSection({
  invoice
}: Props) {
  const { i18n } = useTranslation()
  const [toggled, setToggled] = useState(true)

  return (
    <Collapsible
      title={i18n.invoice.form.details.title}
      icon={faMoneyCheck}
      open={toggled}
      onToggle={() => setToggled((prev) => !prev)}
    >
      <LabelValueList>
        <LabelValueListItem
          label={i18n.invoice.form.details.status}
          value={i18n.invoice.status[invoice.status]}
          dataQa="invoice-details-status"
        />
        <LabelValueListItem
          label={i18n.invoice.form.details.range}
          value={`${invoice.periodStart.format()} - ${invoice.periodEnd.format()}`}
          dataQa="invoice-details-range"
        />
        <LabelValueListItem
          label={i18n.invoice.form.details.number}
          value={invoice.number}
          dataQa="invoice-details-number"
        />
        <LabelValueListItem
          label={i18n.invoice.form.details.dueDate}
          value={invoice.dueDate.format()}
          dataQa="invoice-details-due-date"
        />
        <LabelValueListItem
          label={i18n.invoice.form.details.account}
          value={invoice.account}
          dataQa="invoice-details-account"
        />
        <LabelValueListItem
          label={i18n.invoice.form.details.agreementType}
          value={invoice.agreementType}
          dataQa="invoice-details-agreementType"
        />
      </LabelValueList>
    </Collapsible>
  )
})

export default InvoiceDetailsSection
