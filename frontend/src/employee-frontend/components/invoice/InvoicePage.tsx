// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect } from 'react'
import { Link } from 'react-router-dom'

import { combine } from 'lib-common/api'
import { InvoiceDetailed } from 'lib-common/generated/api-types/invoicing'
import { useQueryResult } from 'lib-common/query'
import useRouteParams from 'lib-common/useRouteParams'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'

import { useTranslation } from '../../state/i18n'
import { TitleContext, TitleState } from '../../state/title'
import { renderResult } from '../async-rendering'
import { invoiceCodesQuery, invoiceDetailsQuery } from '../invoices/queries'

import Actions from './Actions'
import InvoiceDetailsSection from './InvoiceDetailsSection'
import InvoiceHeadOfFamilySection from './InvoiceHeadOfFamilySection'
import InvoiceRowsSection from './InvoiceRowsSection'
import Sum from './Sum'
import { formatInvoicePeriod } from './utils'

export default React.memo(function InvoiceDetailsPage() {
  const { id } = useRouteParams(['id'])
  const { i18n } = useTranslation()
  const invoiceCodes = useQueryResult(invoiceCodesQuery())
  const response = useQueryResult(invoiceDetailsQuery(id))
  const { setTitle } = useContext<TitleState>(TitleContext)

  useEffect(() => {
    if (response.isSuccess) {
      const name = `${response.value.invoice.headOfFamily.firstName} ${response.value.invoice.headOfFamily.lastName}`
      if (response.value.invoice.status === 'DRAFT') {
        setTitle(`${name} | ${i18n.titles.invoiceDraft}`)
      } else {
        setTitle(`${name} | ${i18n.titles.invoice}`)
      }
    }
  }, [i18n, response, setTitle])

  return (
    <div className="invoice-details-page" data-qa="invoice-details-page">
      <Container>
        <ReturnButton label={i18n.common.goBack} data-qa="navigate-back" />
        {renderResult(
          combine(response, invoiceCodes),
          ([response, invoiceCodes]) => (
            <ContentArea opaque>
              <Title size={1}>
                {i18n.invoice.title[response.invoice.status]}
              </Title>
              {response.replacedByInvoice ? (
                <ReplacedByInvoice
                  replacedByInvoice={response.replacedByInvoice}
                />
              ) : null}
              <InvoiceHeadOfFamilySection
                headOfFamily={response.invoice.headOfFamily}
                codebtor={response.invoice.codebtor}
              />
              <InvoiceDetailsSection
                invoice={response.invoice}
                replacedInvoice={response.replacedInvoice}
              />
              <InvoiceRowsSection
                rows={response.invoice.rows}
                replacedRows={response.replacedInvoice?.rows}
                invoiceCodes={invoiceCodes}
              />
              <Sum
                title="familyTotal"
                sum={response.invoice.totalPrice}
                previousSum={response.replacedInvoice?.totalPrice}
                data-qa="total-sum"
              />
              <Actions invoiceResponse={response} />
            </ContentArea>
          )
        )}
      </Container>
    </div>
  )
})

const ReplacedByInvoice = React.memo(function ReplacedByInvoice({
  replacedByInvoice
}: {
  replacedByInvoice: InvoiceDetailed
}) {
  const { i18n } = useTranslation()
  const linkToReplacement = (
    <Link to={`/finance/invoices/${replacedByInvoice.id}`}>
      {formatInvoicePeriod(replacedByInvoice, i18n)}
    </Link>
  )
  const message =
    replacedByInvoice.status === 'REPLACEMENT_DRAFT'
      ? i18n.invoice.form.details.replacedByDraft(linkToReplacement)
      : i18n.invoice.form.details.replacedBy(linkToReplacement)

  return <AlertBox message={message} />
})
