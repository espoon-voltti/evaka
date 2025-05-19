// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { ChildId } from 'lib-common/generated/api-types/shared'
import { Queries } from 'lib-common/query'

import {
  createChildIncomeStatement,
  createIncomeStatement,
  deleteIncomeStatement,
  getChildIncomeStatementStartDates,
  getChildIncomeStatements,
  getIncomeStatement,
  getIncomeStatementChildren,
  getIncomeStatementStartDates,
  getIncomeStatements,
  updateIncomeStatement,
  updateSentIncomeStatement,
  getPartnerIncomeStatementStatus
} from '../generated/api-clients/incomestatement'

const q = new Queries()

// Guardian

export const incomeStatementsQuery = q.query(getIncomeStatements)

export const partnerIncomeStatementStatusQuery = q.query(
  getPartnerIncomeStatementStatus
)

export const incomeStatementQuery = q.query(getIncomeStatement)

export const incomeStatementStartDatesQuery = q.query(
  getIncomeStatementStartDates
)

export const createIncomeStatementMutation = q.mutation(createIncomeStatement, [
  incomeStatementsQuery.prefix
])

export const updateIncomeStatementMutation = q.mutation(updateIncomeStatement, [
  incomeStatementsQuery.prefix
])

export const updateSentIncomeStatementMutation = q.mutation(
  updateSentIncomeStatement,
  [
    incomeStatementsQuery.prefix,
    ({ incomeStatementId }) => incomeStatementQuery({ incomeStatementId })
  ]
)

export const deleteIncomeStatementMutation = q.mutation(deleteIncomeStatement, [
  incomeStatementsQuery.prefix
])

// Child

export const guardianIncomeStatementChildrenQuery = q.query(
  getIncomeStatementChildren
)

export const childIncomeStatementsQuery = q.prefixedQuery(
  getChildIncomeStatements,
  ({ childId }) => childId
)

export const childIncomeStatementStartDatesQuery = q.query(
  getChildIncomeStatementStartDates
)

export const createChildIncomeStatementMutation = q.mutation(
  createChildIncomeStatement,
  [({ childId }) => childIncomeStatementsQuery.prefix(childId)]
)

export const deleteChildIncomeStatementMutation = q.parametricMutation<{
  childId: ChildId
}>()(deleteIncomeStatement, [
  incomeStatementsQuery.prefix,
  ({ childId }) => childIncomeStatementsQuery.prefix(childId)
])
