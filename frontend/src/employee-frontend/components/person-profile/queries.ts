// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { getGuardianApplicationSummaries } from 'employee-frontend/generated/api-clients/application'
import { getDecisionsByGuardian } from 'employee-frontend/generated/api-clients/decision'
import {
  getIncomeStatementChildren,
  getIncomeStatements
} from 'employee-frontend/generated/api-clients/incomestatement'
import { getChildPlacementPeriods } from 'employee-frontend/generated/api-clients/placement'
import { PersonId } from 'lib-common/generated/api-types/shared'
import { Queries } from 'lib-common/query'

import {
  createFinanceNote,
  deleteFinanceNote,
  getFinanceNotes,
  updateFinanceNote
} from '../../generated/api-clients/finance'
import {
  createIncome,
  createInvoiceCorrection,
  createReplacementDraftsForHeadOfFamily,
  deleteIncome,
  deleteInvoiceCorrection,
  generateRetroactiveFeeDecisions,
  generateRetroactiveVoucherValueDecisions,
  getHeadOfFamilyFeeDecisions,
  getHeadOfFamilyInvoices,
  getHeadOfFamilyVoucherValueDecisions,
  getIncomeMultipliers,
  getIncomeNotifications,
  getIncomeTypeOptions,
  getPersonIncomes,
  getPersonInvoiceCorrections,
  updateIncome,
  updateInvoiceCorrectionNote
} from '../../generated/api-clients/invoicing'
import {
  addSsn,
  createFosterParentRelationship,
  createParentship,
  createPartnership,
  deleteFosterParentRelationship,
  deleteParentship,
  deletePartnership,
  disableSsn,
  duplicatePerson,
  getFamilyByPerson,
  getFosterChildren,
  getParentships,
  getPartnerships,
  getPerson,
  retryParentship,
  retryPartnership,
  updateFosterParentRelationshipValidity,
  updateParentship,
  updatePartnership,
  updatePersonAndFamilyFromVtj,
  updatePersonDetails
} from '../../generated/api-clients/pis'
import { childQuery } from '../child-information/queries'

const q = new Queries()

export const personQuery = q.query(getPerson)

export const familyByPersonQuery = q.query(getFamilyByPerson)

export const parentshipsQuery = q.query(getParentships)

export const updatePersonDetailsMutation = q.mutation(updatePersonDetails, [
  ({ personId }) => personQuery({ personId }),
  ({ personId }) => childQuery({ childId: personId })
])

export const updatePersonAndFamilyFromVtjMutation = q.mutation(
  updatePersonAndFamilyFromVtj,
  [
    ({ personId }) => personQuery({ personId }),
    ({ personId }) => childQuery({ childId: personId }),
    ({ personId }) => familyByPersonQuery({ id: personId }),
    ({ personId }) => parentshipsQuery({ headOfChildId: personId })
  ]
)

export const disableSsnMutation = q.mutation(disableSsn, [
  ({ personId }) => personQuery({ personId }),
  ({ personId }) => childQuery({ childId: personId })
])

export const duplicatePersonMutation = q.mutation(duplicatePerson)

export const addSsnMutation = q.mutation(addSsn, [
  ({ personId }) => personQuery({ personId }),
  ({ personId }) => childQuery({ childId: personId })
])

export const createParentshipMutation = q.mutation(createParentship, [
  ({ body }) => familyByPersonQuery({ id: body.headOfChildId }),
  ({ body }) => parentshipsQuery({ headOfChildId: body.headOfChildId })
])

export const updateParentshipMutation = q.parametricMutation<{
  headOfChildId: PersonId
}>()(updateParentship, [
  ({ headOfChildId }) => familyByPersonQuery({ id: headOfChildId }),
  ({ headOfChildId }) => parentshipsQuery({ headOfChildId })
])

export const deleteParentshipMutation = q.parametricMutation<{
  headOfChildId: PersonId
}>()(deleteParentship, [
  ({ headOfChildId }) => familyByPersonQuery({ id: headOfChildId }),
  ({ headOfChildId }) => parentshipsQuery({ headOfChildId })
])

export const retryParentshipMutation = q.parametricMutation<{
  headOfChildId: PersonId
}>()(retryParentship, [
  ({ headOfChildId }) => familyByPersonQuery({ id: headOfChildId }),
  ({ headOfChildId }) => parentshipsQuery({ headOfChildId })
])

export const partnershipsQuery = q.query(getPartnerships)

export const createPartnershipMutation = q.mutation(createPartnership, [
  ({ body }) => partnershipsQuery({ personId: body.person1Id }),
  ({ body }) => familyByPersonQuery({ id: body.person1Id })
])

export const updatePartnershipMutation = q.parametricMutation<{
  personId: PersonId
}>()(updatePartnership, [
  ({ personId }) => partnershipsQuery({ personId }),
  ({ personId }) => familyByPersonQuery({ id: personId })
])

export const deletePartnershipMutation = q.parametricMutation<{
  personId: PersonId
}>()(deletePartnership, [
  ({ personId }) => partnershipsQuery({ personId }),
  ({ personId }) => familyByPersonQuery({ id: personId })
])

export const retryPartnershipMutation = q.parametricMutation<{
  personId: PersonId
}>()(retryPartnership, [
  ({ personId }) => partnershipsQuery({ personId }),
  ({ personId }) => familyByPersonQuery({ id: personId })
])

export const fosterChildrenQuery = q.query(getFosterChildren)

export const createFosterParentRelationshipMutation = q.mutation(
  createFosterParentRelationship,
  [({ body: { parentId } }) => fosterChildrenQuery({ parentId })]
)

export const updateFosterParentRelationshipValidityMutation =
  q.parametricMutation<{
    parentId: PersonId
  }>()(updateFosterParentRelationshipValidity, [
    ({ parentId }) => fosterChildrenQuery({ parentId })
  ])

export const deleteFosterParentRelationshipMutation = q.parametricMutation<{
  parentId: PersonId
}>()(deleteFosterParentRelationship, [
  ({ parentId }) => fosterChildrenQuery({ parentId })
])

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

export const incomeTypeOptionsQuery = q.query(getIncomeTypeOptions)

export const childPlacementPeriodsQuery = q.query(getChildPlacementPeriods)

export const personIncomesQuery = q.query(getPersonIncomes)

export const createIncomeMutation = q.mutation(createIncome, [
  ({ body }) => personIncomesQuery({ personId: body.personId }),
  ({ body }) => familyByPersonQuery({ id: body.personId })
])

export const updateIncomeMutation = q.mutation(updateIncome, [
  ({ body }) => personIncomesQuery({ personId: body.personId }),
  ({ body }) => familyByPersonQuery({ id: body.personId })
])

export const deleteIncomeMutation = q.parametricMutation<{
  personId: PersonId
}>()(deleteIncome, [
  ({ personId }) => personIncomesQuery({ personId }),
  ({ personId }) => familyByPersonQuery({ id: personId })
])

export const incomeNotificationsQuery = q.query(getIncomeNotifications)

export const incomeStatementsQuery = q.query(getIncomeStatements)

export const incomeStatementChildrenQuery = q.query(getIncomeStatementChildren)

export const headOfFamilyFeeDecisionsQuery = q.query(
  getHeadOfFamilyFeeDecisions
)

export const generateRetroactiveFeeDecisionsMutation = q.mutation(
  generateRetroactiveFeeDecisions,
  [({ id }) => headOfFamilyFeeDecisionsQuery({ id })]
)

export const headOfFamilyVoucherValueDecisionsQuery = q.query(
  getHeadOfFamilyVoucherValueDecisions
)

export const generateRetroactiveVoucherValueDecisionsMutation = q.mutation(
  generateRetroactiveVoucherValueDecisions,
  [({ id }) => headOfFamilyVoucherValueDecisionsQuery({ headOfFamilyId: id })]
)

export const guardianApplicationSummariesQuery = q.query(
  getGuardianApplicationSummaries
)

export const decisionsByGuardianQuery = q.query(getDecisionsByGuardian)
