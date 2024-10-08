// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect } from 'react'
import { Navigate } from 'react-router-dom'

import { useQueryResult } from 'lib-common/query'
import useRouteParams from 'lib-common/useRouteParams'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'

import { useTranslation } from '../../state/i18n'
import { TitleContext, TitleState } from '../../state/title'
import { invoiceCodesQuery, invoiceDetailsQuery } from '../invoices/queries'

import Actions from './Actions'
import InvoiceDetailsSection from './InvoiceDetailsSection'
import InvoiceHeadOfFamilySection from './InvoiceHeadOfFamilySection'
import InvoiceRowsSection from './InvoiceRowsSection'
import Sum from './Sum'

export default React.memo(function InvoiceDetailsPage() {
  const { id } = useRouteParams(['id'])
  const { i18n } = useTranslation()
  const invoiceCodes = useQueryResult(invoiceCodesQuery())
  const invoice = useQueryResult(invoiceDetailsQuery(id))
  const { setTitle } = useContext<TitleState>(TitleContext)

  useEffect(() => {
    if (invoice.isSuccess) {
      const name = `${invoice.value.data.headOfFamily.firstName} ${invoice.value.data.headOfFamily.lastName}`
      if (invoice.value.data.status === 'DRAFT') {
        setTitle(`${name} | ${i18n.titles.invoiceDraft}`)
      } else {
        setTitle(`${name} | ${i18n.titles.invoice}`)
      }
    }
  }, [invoice]) // eslint-disable-line react-hooks/exhaustive-deps

  if (invoice.isFailure) {
    return <Navigate replace to="/finance/invoices" />
  }

  return (
    <div className="invoice-details-page" data-qa="invoice-details-page">
      <Container>
        <ReturnButton label={i18n.common.goBack} data-qa="navigate-back" />
        {invoice?.isSuccess ? (
          <ContentArea opaque>
            <Title size={1}>
              {i18n.invoice.title[invoice.value.data.status]}
            </Title>
            <InvoiceHeadOfFamilySection
              headOfFamily={invoice.value.data.headOfFamily}
              codebtor={invoice.value.data.codebtor}
            />
            <InvoiceDetailsSection invoice={invoice.value.data} />
            <InvoiceRowsSection
              rows={invoice.value.data.rows}
              invoiceCodes={invoiceCodes}
            />
            <Sum title="familyTotal" sum={invoice.value.data.totalPrice} />
            <Actions invoice={invoice.value} />
          </ContentArea>
        ) : null}
      </Container>
    </div>
  )
})
