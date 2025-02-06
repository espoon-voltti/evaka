// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { PersonId } from 'lib-common/generated/api-types/shared'
import { Queries } from 'lib-common/query'

import {
  createFinanceNote,
  deleteFinanceNote,
  getFinanceNotes,
  updateFinanceNote
} from '../../generated/api-clients/finance'
import {
  createInvoiceCorrection,
  createReplacementDraftsForHeadOfFamily,
  deleteInvoiceCorrection,
  getHeadOfFamilyInvoices,
  getIncomeMultipliers,
  getPersonInvoiceCorrections,
  updateInvoiceCorrectionNote
} from '../../generated/api-clients/invoicing'

const q = new Queries()

export const incomeCoefficientMultipliersQuery = q.query(getIncomeMultipliers)

export const invoiceCorrectionsQuery = q.query(getPersonInvoiceCorrections)

export const createInvoiceCorrectionMutation = q.mutation(
  createInvoiceCorrection,
  [({ body }) => invoiceCorrectionsQuery({ personId: body.headOfFamilyId })]
)

export const updateInvoiceCorrectionNoteMutation = q.parametricMutation<{
  personId: PersonId
}>()(updateInvoiceCorrectionNote, [
  ({ personId }) => invoiceCorrectionsQuery({ personId })
])

export const deleteInvoiceCorrectionMutation = q.parametricMutation<{
  personId: PersonId
}>()(deleteInvoiceCorrection, [
  ({ personId }) => invoiceCorrectionsQuery({ personId })
])

export const headOfFamilyInvoicesQuery = q.query(getHeadOfFamilyInvoices)

export const createReplacementDraftsForHeadOfFamilyMutation = q.mutation(
  createReplacementDraftsForHeadOfFamily,
  [({ headOfFamilyId }) => headOfFamilyInvoicesQuery({ id: headOfFamilyId })]
)

export const financeNotesQuery = q.query(getFinanceNotes)

export const createFinanceNoteMutation = q.mutation(createFinanceNote, [
  ({ body }) => financeNotesQuery({ personId: body.personId })
])

export const updateFinanceNoteMutation = q.parametricMutation<{
  id: PersonId
}>()(updateFinanceNote, [({ id }) => financeNotesQuery({ personId: id })])

export const deleteFinanceNoteMutation = q.parametricMutation<{
  id: PersonId
}>()(deleteFinanceNote, [({ id }) => financeNotesQuery({ personId: id })])
