// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'
import { Redirect, useParams } from 'react-router-dom'
import { Loading, Result } from 'lib-common/api'
import {
  InvoiceCodes,
  InvoiceDetailed,
  InvoiceRowDetailed
} from 'lib-common/generated/api-types/invoicing'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'

import { getInvoice, getInvoiceCodes } from '../../api/invoicing'
import { useTranslation } from '../../state/i18n'
import { TitleContext, TitleState } from '../../state/title'
import { totalPrice } from '../../utils/pricing'
import Actions from './Actions'
import InvoiceDetailsSection from './InvoiceDetailsSection'
import InvoiceHeadOfFamilySection from './InvoiceHeadOfFamilySection'
import InvoiceRowsSection from './InvoiceRowsSection'
import Sum from './Sum'

export default React.memo(function InvoiceDetailsPage() {
  const { id } = useParams<{ id: string }>()
  const { i18n } = useTranslation()
  const [invoice, setInvoice] = useState<Result<InvoiceDetailed>>(Loading.of())
  const [invoiceCodes, setInvoiceCodes] = useState<Result<InvoiceCodes>>(
    Loading.of()
  )
  const { setTitle } = useContext<TitleState>(TitleContext)

  const loadInvoice = useCallback(() => getInvoice(id).then(setInvoice), [id])
  useEffect(() => void loadInvoice(), [id]) // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => void getInvoiceCodes().then(setInvoiceCodes), [])

  useEffect(() => {
    if (invoice.isSuccess) {
      const name = `${invoice.value.headOfFamily.firstName} ${invoice.value.headOfFamily.lastName}`
      invoice.value.status === 'DRAFT'
        ? setTitle(`${name} | ${i18n.titles.invoiceDraft}`)
        : setTitle(`${name} | ${i18n.titles.invoice}`)
    }
  }, [invoice]) // eslint-disable-line react-hooks/exhaustive-deps

  const editable = invoice.map((inv) => inv.status === 'DRAFT').getOrElse(false)

  const updateRows = useCallback(
    (rows: InvoiceRowDetailed[]) =>
      setInvoice((previous: Result<InvoiceDetailed>) =>
        previous.map((inv) => ({ ...inv, rows }))
      ),
    []
  )

  if (invoice.isFailure) {
    return <Redirect to="/finance/invoices" />
  }

  return (
    <div className="invoice-details-page" data-qa="invoice-details-page">
      <Container>
        <ReturnButton label={i18n.common.goBack} data-qa="navigate-back" />
        {invoice?.isSuccess ? (
          <ContentArea opaque>
            <Title size={1}>{i18n.invoice.title[invoice.value.status]}</Title>
            <InvoiceHeadOfFamilySection
              headOfFamily={invoice.value.headOfFamily}
              codebtor={invoice.value.codebtor}
            />
            <InvoiceDetailsSection invoice={invoice.value} />
            <InvoiceRowsSection
              rows={invoice.value.rows}
              updateRows={updateRows}
              invoiceCodes={invoiceCodes}
              editable={editable}
            />
            <Sum
              title="familyTotal"
              sum={
                editable
                  ? totalPrice(invoice.value.rows)
                  : invoice.value.totalPrice
              }
            />
            <Actions
              invoice={invoice.value}
              loadInvoice={loadInvoice}
              editable={editable}
            />
          </ContentArea>
        ) : null}
      </Container>
    </div>
  )
})
