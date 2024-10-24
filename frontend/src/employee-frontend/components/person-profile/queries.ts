// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { Arg0, UUID } from 'lib-common/types'

import {
  createInvoiceCorrection,
  deleteInvoiceCorrection,
  getIncomeMultipliers,
  getPersonInvoiceCorrections,
  updateInvoiceCorrectionNote
} from '../../generated/api-clients/invoicing'
import { createQueryKeys } from '../../query'

const queryKeys = createQueryKeys('personProfile', {
  incomeCoefficientMultipliers: () => ['incomeCoefficientMultipliers'],
  invoiceCorrections: (args: Arg0<typeof getPersonInvoiceCorrections>) => [
    'invoiceCorrections',
    args
  ]
})

export const incomeCoefficientMultipliersQuery = query({
  api: getIncomeMultipliers,
  queryKey: queryKeys.incomeCoefficientMultipliers
})

export const invoiceCorrectionsQuery = query({
  api: getPersonInvoiceCorrections,
  queryKey: queryKeys.invoiceCorrections
})

export const createInvoiceCorrectionMutation = mutation({
  api: createInvoiceCorrection,
  invalidateQueryKeys: (args) => [
    queryKeys.invoiceCorrections({ personId: args.body.headOfFamilyId })
  ]
})

export const updateInvoiceCorrectionNoteMutation = mutation({
  api: (args: Arg0<typeof updateInvoiceCorrectionNote> & { personId: UUID }) =>
    updateInvoiceCorrectionNote(args),
  invalidateQueryKeys: (args) => [
    queryKeys.invoiceCorrections({ personId: args.personId })
  ]
})

export const deleteInvoiceCorrectionMutation = mutation({
  api: (args: Arg0<typeof deleteInvoiceCorrection> & { personId: UUID }) =>
    deleteInvoiceCorrection(args),
  invalidateQueryKeys: (args) => [
    queryKeys.invoiceCorrections({ personId: args.personId })
  ]
})
