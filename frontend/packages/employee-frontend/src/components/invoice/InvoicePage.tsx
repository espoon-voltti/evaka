// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import { Redirect, useParams } from 'react-router-dom'

import { Container, ContentArea } from '~components/shared/layout/Container'
import ReturnButton from '~components/shared/atoms/buttons/ReturnButton'
import Title from '~components/shared/atoms/Title'
import { useTranslation } from '../../state/i18n'
import { TitleContext, TitleState } from '../../state/title'
import { isFailure, isSuccess, Loading, Result, Success } from '../../api'
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
  const [invoice, setInvoice] = useState<Result<InvoiceDetailed>>(Loading())
  const [invoiceCodes, setInvoiceCodes] = useState<Result<InvoiceCodes>>(
    Loading()
  )
  const { setTitle } = useContext<TitleState>(TitleContext)

  const loadInvoice = () => getInvoice(id).then(setInvoice)
  useEffect(() => void loadInvoice(), [id])
  useEffect(() => void getInvoiceCodes().then(setInvoiceCodes), [])

  useEffect(() => {
    if (isSuccess(invoice)) {
      const name = `${invoice.data.headOfFamily.firstName} ${invoice.data.headOfFamily.lastName}`
      invoice.data.status === 'DRAFT'
        ? setTitle(`${name} | ${i18n.titles.invoiceDraft}`)
        : setTitle(`${name} | ${i18n.titles.invoice}`)
    }
  }, [invoice])

  const editable = isSuccess(invoice) && invoice.data.status === 'DRAFT'

  if (isFailure(invoice)) {
    return <Redirect to="/finance/invoices" />
  }

  const updateRows = (rows: InvoiceRowDetailed[]) =>
    setInvoice((previous: Result<InvoiceDetailed>) =>
      isSuccess(previous) ? Success({ ...previous.data, rows }) : previous
    )

  return (
    <div className="invoice-details-page" data-qa="invoice-details-page">
      <Container>
        <ReturnButton dataQa="navigate-back" />
        {invoice ? (
          <>
            {isSuccess(invoice) && (
              <ContentArea opaque>
                <Title size={1}>
                  {i18n.invoice.title[invoice.data.status]}
                </Title>
                <InvoiceHeadOfFamilySection
                  id={invoice.data.headOfFamily.id}
                  fullName={formatName(
                    invoice.data.headOfFamily.firstName,
                    invoice.data.headOfFamily.lastName,
                    i18n
                  )}
                  dateOfBirth={invoice.data.headOfFamily.dateOfBirth}
                  ssn={invoice.data.headOfFamily.ssn}
                />
                <InvoiceDetailsSection invoice={invoice.data} />
                <InvoiceRowsSection
                  rows={invoice.data.rows}
                  updateRows={updateRows}
                  invoiceCodes={invoiceCodes}
                  editable={editable}
                />
                <Sum
                  title={'familyTotal'}
                  sum={
                    editable
                      ? totalPrice(invoice.data.rows)
                      : invoice.data.totalPrice
                  }
                />
                <Actions
                  invoice={invoice.data}
                  loadInvoice={loadInvoice}
                  editable={editable}
                />
              </ContentArea>
            )}
          </>
        ) : null}
      </Container>
    </div>
  )
})

export default InvoiceDetailsPage
