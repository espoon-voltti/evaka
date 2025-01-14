// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  confirmDraftPayments,
  createPaymentDrafts,
  deleteDraftPayments,
  revertPaymentsToDrafts,
  searchPayments,
  sendPayments
} from '../../generated/api-clients/invoicing'

const q = new Queries()

export const searchPaymentsQuery = q.query(searchPayments)

export const createPaymentDraftsMutation = q.mutation(createPaymentDrafts, [
  searchPaymentsQuery.prefix
])

export const sendPaymentsMutation = q.mutation(sendPayments, [
  searchPaymentsQuery.prefix
])

export const confirmDraftPaymentsMutation = q.mutation(confirmDraftPayments, [
  searchPaymentsQuery.prefix
])

export const deleteDraftPaymentsMutation = q.mutation(deleteDraftPayments, [
  searchPaymentsQuery.prefix
])

export const revertPaymentsToDraftsMutation = q.mutation(
  revertPaymentsToDrafts,
  [searchPaymentsQuery.prefix]
)
