// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

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

const q = new Queries()

export const invoiceCodesQuery = q.query(getInvoiceCodes)

export const invoicesQuery = q.query(searchInvoices)

export const invoiceDetailsQuery = q.query(getInvoice)

export const createDraftInvoicesMutation = q.mutation(createDraftInvoices, [
  invoicesQuery.prefix
])

export const sendInvoicesMutation = q.mutation(sendInvoices, [
  invoicesQuery.prefix,
  invoiceDetailsQuery.prefix
])

export const sendInvoicesByDateMutation = q.mutation(sendInvoicesByDate, [
  invoicesQuery.prefix,
  invoiceDetailsQuery.prefix
])

export const markInvoicesSentMutation = q.mutation(markInvoicesSent, [
  invoicesQuery.prefix,
  invoiceDetailsQuery.prefix
])

export const deleteDraftInvoicesMutation = q.mutation(deleteDraftInvoices, [
  invoicesQuery.prefix,
  invoiceDetailsQuery.prefix
])

export const markReplacementDraftSentMutation = q.mutation(
  markReplacementDraftSent,
  [({ invoiceId }) => invoiceDetailsQuery({ id: invoiceId })]
)
