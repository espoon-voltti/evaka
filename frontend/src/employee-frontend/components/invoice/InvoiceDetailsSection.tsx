// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router-dom'

import { InvoiceDetailed } from 'lib-common/generated/api-types/invoicing'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import { featureFlags } from 'lib-customizations/employee'
import { faMoneyCheck } from 'lib-icons'

import LabelValueList from '../../components/common/LabelValueList'
import { useTranslation } from '../../state/i18n'

import { formatInvoicePeriod } from './utils'

interface Props {
  invoice: InvoiceDetailed
  replacedInvoice: InvoiceDetailed | null
}

const InvoiceDetailsSection = React.memo(function InvoiceDetailsSection({
  invoice,
  replacedInvoice
}: Props) {
  const { i18n } = useTranslation()
  return (
    <CollapsibleSection
      title={i18n.invoice.form.details.title}
      icon={faMoneyCheck}
      startCollapsed={false}
      data-qa="invoice-details"
    >
      <LabelValueList
        spacing="small"
        contents={[
          {
            label: i18n.invoice.form.details.status,
            value: i18n.invoice.status[invoice.status],
            dataQa: 'status'
          },
          {
            label: i18n.invoice.form.details.range,
            value: `${invoice.periodStart.format()} - ${invoice.periodEnd.format()}`,
            dataQa: 'period'
          },
          {
            label: i18n.invoice.form.details.number,
            value: invoice.number,
            dataQa: 'number'
          },
          {
            label: i18n.invoice.form.details.dueDate,
            value: invoice.dueDate.format(),
            dataQa: 'due-date'
          },
          ...(featureFlags.invoiceDisplayAccountNumber
            ? [
                {
                  label: i18n.invoice.form.details.account,
                  value: invoice.account,
                  dataQa: 'account'
                }
              ]
            : []),
          ...(invoice.agreementType !== null
            ? [
                {
                  label: i18n.invoice.form.details.agreementType,
                  value: invoice.agreementType,
                  dataQa: 'agreement-type'
                }
              ]
            : []),
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
            ),
            dataQa: 'related-fee-decisions'
          },
          ...(replacedInvoice !== null
            ? [
                {
                  label: i18n.invoice.form.details.replacedInvoice,
                  value: (
                    <Link to={`/finance/invoices/${replacedInvoice.id}`}>
                      {formatInvoicePeriod(replacedInvoice, i18n)}
                    </Link>
                  ),
                  dataQa: 'replaced-invoice'
                }
              ]
            : [])
        ]}
      />
    </CollapsibleSection>
  )
})

export default InvoiceDetailsSection
