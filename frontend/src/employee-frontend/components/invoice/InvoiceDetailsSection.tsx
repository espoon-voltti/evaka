// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router-dom'

import { InvoiceDetailed } from 'lib-common/generated/api-types/invoicing'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import { faMoneyCheck } from 'lib-icons'

import LabelValueList from '../../components/common/LabelValueList'
import { useTranslation } from '../../state/i18n'

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
          },
          {
            label: i18n.invoice.form.details.relatedFeeDecisions,
            value: (
              <div>
                {invoice.relatedFeeDecisions.map((decision, i) => (
                  <Link
                    to={`/finance/fee-decisions/${decision.id}`}
                    key={decision.id}
                  >
                    <span>{decision.decisionNumber}</span>
                    {i < invoice.relatedFeeDecisions.length - 1 ? ', ' : ''}
                  </Link>
                ))}
              </div>
            )
          }
        ]}
      />
    </CollapsibleSection>
  )
})

export default InvoiceDetailsSection
