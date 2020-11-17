// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Gap } from 'components/shared/layout/white-space'
import { Container, ContentArea } from 'components/shared/layout/Container'
import Invoices from './Invoices'
import InvoiceFilters from './InvoiceFilters'
import Actions from './Actions'
import { useInvoicesState } from './invoices-state'

const InvoicesPage = React.memo(function InvoicesPage() {
  const {
    dispatch,
    page,
    invoices,
    total,
    pages,
    sortBy,
    sortDirection,
    searchFilters,
    refreshInvoices,
    checked
  } = useInvoicesState()

  return (
    <Container data-qa="invoices-page">
      <ContentArea opaque>
        <InvoiceFilters />
      </ContentArea>
      <Gap size={'XL'} />
      <ContentArea opaque>
        <Invoices
          dispatch={dispatch}
          invoices={invoices[page]}
          refreshInvoices={refreshInvoices}
          total={total}
          pages={pages}
          currentPage={page}
          sortBy={sortBy}
          sortDirection={sortDirection}
          showCheckboxes={
            searchFilters.status === 'DRAFT' ||
            searchFilters.status === 'WAITING_FOR_SENDING'
          }
          checked={checked}
        />
        <Actions
          dispatch={dispatch}
          status={searchFilters.status}
          checkedInvoices={checked}
          checkedAreas={searchFilters.area}
          periodStart={searchFilters.startDate}
          periodEnd={searchFilters.endDate}
          useCustomDatesForInvoiceSending={
            searchFilters.useCustomDatesForInvoiceSending
          }
        />
      </ContentArea>
    </Container>
  )
})

export default InvoicesPage
