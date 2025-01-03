// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { PersonId } from 'lib-common/generated/api-types/shared'
import { mutation, parametricMutation, query } from 'lib-common/query'
import { Arg0, UUID } from 'lib-common/types'

import {
  createInvoiceCorrection,
  createReplacementDraftsForHeadOfFamily,
  deleteInvoiceCorrection,
  getHeadOfFamilyInvoices,
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
  ],
  headOfFamilyInvoices: (args: { id: UUID }) => ['headOfFamilyInvoices', args]
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

export const updateInvoiceCorrectionNoteMutation =
  parametricMutation<PersonId>()({
    api: updateInvoiceCorrectionNote,
    invalidateQueryKeys: (personId) => [
      queryKeys.invoiceCorrections({ personId })
    ]
  })

export const deleteInvoiceCorrectionMutation = parametricMutation<PersonId>()({
  api: deleteInvoiceCorrection,
  invalidateQueryKeys: (personId) => [
    queryKeys.invoiceCorrections({ personId })
  ]
})

export const headOfFamilyInvoicesQuery = query({
  api: getHeadOfFamilyInvoices,
  queryKey: queryKeys.headOfFamilyInvoices
})

export const createReplacementDraftsForHeadOfFamilyMutation = mutation({
  api: createReplacementDraftsForHeadOfFamily,
  invalidateQueryKeys: (arg) => [
    queryKeys.headOfFamilyInvoices({ id: arg.headOfFamilyId })
  ]
})
