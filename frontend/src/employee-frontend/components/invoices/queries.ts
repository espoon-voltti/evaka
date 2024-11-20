// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { Arg0, UUID } from 'lib-common/types'

import {
  createDraftInvoices,
  deleteDraftInvoices,
  getInvoice,
  getInvoiceCodes,
  markInvoicesSent,
  searchInvoices,
  sendInvoices,
  sendInvoicesByDate
} from '../../generated/api-clients/invoicing'
import { markReplacementDraftSent } from '../../generated/api-clients/invoicing'
import { createQueryKeys } from '../../query'

const queryKeys = createQueryKeys('invoices', {
  invoiceCodes: () => ['invoiceCodes'],
  invoiceDetails: (id: UUID) => ['invoiceDetails', id],
  invoiceDetailsAll: () => ['invoiceDetails'],
  invoices: (args: Arg0<typeof searchInvoices>) => ['invoices', args],
  invoicesAll: () => ['invoices']
})

export const invoiceCodesQuery = query({
  api: getInvoiceCodes,
  queryKey: queryKeys.invoiceCodes
})

export const invoicesQuery = query({
  api: (args: Arg0<typeof searchInvoices>) => searchInvoices(args),
  queryKey: queryKeys.invoices
})

export const invoiceDetailsQuery = query({
  api: (id: UUID) => getInvoice({ id }),
  queryKey: queryKeys.invoiceDetails
})

export const createDraftInvoicesMutation = mutation({
  api: createDraftInvoices,
  invalidateQueryKeys: () => [queryKeys.invoicesAll()]
})

export const sendInvoicesMutation = mutation({
  api: sendInvoices,
  invalidateQueryKeys: () => [
    queryKeys.invoicesAll(),
    queryKeys.invoiceDetailsAll()
  ]
})

export const sendInvoicesByDateMutation = mutation({
  api: sendInvoicesByDate,
  invalidateQueryKeys: () => [
    queryKeys.invoicesAll(),
    queryKeys.invoiceDetailsAll()
  ]
})

export const markInvoicesSentMutation = mutation({
  api: markInvoicesSent,
  invalidateQueryKeys: () => [
    queryKeys.invoicesAll(),
    queryKeys.invoiceDetailsAll()
  ]
})

export const deleteDraftInvoicesMutation = mutation({
  api: deleteDraftInvoices,
  invalidateQueryKeys: () => [
    queryKeys.invoicesAll(),
    queryKeys.invoiceDetailsAll()
  ]
})

export const markReplacementDraftSentMutation = mutation({
  api: markReplacementDraftSent,
  invalidateQueryKeys: (arg) => [queryKeys.invoiceDetails(arg.invoiceId)]
})
