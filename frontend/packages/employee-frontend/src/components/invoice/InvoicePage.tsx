// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import { Redirect, useParams } from 'react-router-dom'

import {
  Container,
  ContentArea
} from '@evaka/lib-components/src/layout/Container'
import ReturnButton from '@evaka/lib-components/src/atoms/buttons/ReturnButton'
import Title from '@evaka/lib-components/src/atoms/Title'
import { useTranslation } from '../../state/i18n'
import { TitleContext, TitleState } from '../../state/title'
import { Loading, Result } from '@evaka/lib-common/src/api'
import { getInvoice, getInvoiceCodes } from '../../api/invoicing'
import InvoiceRowsSection from './InvoiceRowsSection'
import InvoiceDetailsSection from './InvoiceDetailsSection'
import InvoiceHeadOfFamilySection from './InvoiceHeadOfFamilySection'
import Sum from './Sum'
import Actions from './Actions'
import {
  InvoiceDetailed,
  InvoiceCodes,
  InvoiceRowDetailed
} from '../../types/invoicing'
import { totalPrice } from '../../utils/pricing'

import './InvoicePage.scss'
import { formatName } from '~utils'

const InvoiceDetailsPage = React.memo(function InvoiceDetailsPage() {
  const { id } = useParams<{ id: string }>()
  const { i18n } = useTranslation()
  const [invoice, setInvoice] = useState<Result<InvoiceDetailed>>(Loading.of())
  const [invoiceCodes, setInvoiceCodes] = useState<Result<InvoiceCodes>>(
    Loading.of()
  )
  const { setTitle } = useContext<TitleState>(TitleContext)

  const loadInvoice = () => getInvoice(id).then(setInvoice)
  useEffect(() => void loadInvoice(), [id])
  useEffect(() => void getInvoiceCodes().then(setInvoiceCodes), [])

  useEffect(() => {
    if (invoice.isSuccess) {
      const name = `${invoice.value.headOfFamily.firstName} ${invoice.value.headOfFamily.lastName}`
      invoice.value.status === 'DRAFT'
        ? setTitle(`${name} | ${i18n.titles.invoiceDraft}`)
        : setTitle(`${name} | ${i18n.titles.invoice}`)
    }
  }, [invoice])

  const editable = invoice.map((inv) => inv.status === 'DRAFT').getOrElse(false)

  if (invoice.isFailure) {
    return <Redirect to="/finance/invoices" />
  }

  const updateRows = (rows: InvoiceRowDetailed[]) =>
    setInvoice((previous: Result<InvoiceDetailed>) =>
      previous.map((inv) => ({ ...inv, rows }))
    )

  return (
    <div className="invoice-details-page" data-qa="invoice-details-page">
      <Container>
        <ReturnButton label={i18n.common.goBack} data-qa="navigate-back" />
        {invoice?.isSuccess ? (
          <ContentArea opaque>
            <Title size={1}>{i18n.invoice.title[invoice.value.status]}</Title>
            <InvoiceHeadOfFamilySection
              id={invoice.value.headOfFamily.id}
              fullName={formatName(
                invoice.value.headOfFamily.firstName,
                invoice.value.headOfFamily.lastName,
                i18n
              )}
              dateOfBirth={invoice.value.headOfFamily.dateOfBirth}
              ssn={invoice.value.headOfFamily.ssn}
            />
            <InvoiceDetailsSection invoice={invoice.value} />
            <InvoiceRowsSection
              rows={invoice.value.rows}
              updateRows={updateRows}
              invoiceCodes={invoiceCodes}
              editable={editable}
            />
            <Sum
              title={'familyTotal'}
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

export default InvoiceDetailsPage
