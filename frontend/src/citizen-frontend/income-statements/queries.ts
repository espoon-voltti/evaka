// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  ChildId,
  IncomeStatementId
} from 'lib-common/generated/api-types/shared'
import { mutation, query } from 'lib-common/query'

import {
  createChildIncomeStatement,
  createIncomeStatement,
  deleteIncomeStatement,
  getChildIncomeStatement,
  getChildIncomeStatementStartDates,
  getChildIncomeStatements,
  getIncomeStatement,
  getIncomeStatementChildren,
  getIncomeStatementStartDates,
  getIncomeStatements,
  removeChildIncomeStatement,
  updateChildIncomeStatement,
  updateIncomeStatement
} from '../generated/api-clients/incomestatement'
import { createQueryKeys } from '../query'

const queryKeys = createQueryKeys('incomeStatements', {
  allIncomeStatements: () => ['incomeStatements'],
  incomeStatements: (page: number) => ['incomeStatements', 'paginated', page],
  incomeStatement: (incomeStatementId: IncomeStatementId) => [
    'incomeStatements',
    'single',
    incomeStatementId
  ],
  incomeStatementStartDates: () => ['incomeStatements', 'startDates'],

  guardianIncomeStatementChildren: () => ['guardianIncomeStatementChildren'],

  allChildIncomeStatements: (childId: ChildId) => [
    'childIncomeStatements',
    childId
  ],
  childIncomeStatements: (childId: ChildId, page: number) => [
    'childIncomeStatements',
    childId,
    'paginated',
    page
  ],
  childIncomeStatement: ({
    childId,
    incomeStatementId
  }: {
    childId: ChildId
    incomeStatementId: IncomeStatementId
  }) => ['childIncomeStatements', childId, 'single', incomeStatementId],
  childIncomeStatementStartDates: (childId: ChildId) => [
    'childIncomeStatements',
    childId,
    'startDates'
  ]
})

// Guardian

export const incomeStatementsQuery = query({
  api: getIncomeStatements,
  queryKey: ({ page }) => queryKeys.incomeStatements(page)
})

export const incomeStatementQuery = query({
  api: getIncomeStatement,
  queryKey: ({ incomeStatementId }) =>
    queryKeys.incomeStatement(incomeStatementId)
})

export const incomeStatementStartDatesQuery = query({
  api: getIncomeStatementStartDates,
  queryKey: queryKeys.incomeStatementStartDates
})

export const createIncomeStatementMutation = mutation({
  api: createIncomeStatement,
  invalidateQueryKeys: () => [queryKeys.allIncomeStatements()]
})

export const updateIncomeStatementMutation = mutation({
  api: updateIncomeStatement,
  invalidateQueryKeys: () => [queryKeys.allIncomeStatements()]
})

export const deleteIncomeStatementMutation = mutation({
  api: deleteIncomeStatement,
  invalidateQueryKeys: () => [queryKeys.allIncomeStatements()]
})

// Child

export const guardianIncomeStatementChildrenQuery = query({
  api: getIncomeStatementChildren,
  queryKey: queryKeys.guardianIncomeStatementChildren
})

export const childIncomeStatementsQuery = query({
  api: getChildIncomeStatements,
  queryKey: ({ childId, page }) =>
    queryKeys.childIncomeStatements(childId, page)
})

export const childIncomeStatementQuery = query({
  api: getChildIncomeStatement,
  queryKey: queryKeys.childIncomeStatement
})

export const childIncomeStatementStartDatesQuery = query({
  api: getChildIncomeStatementStartDates,
  queryKey: ({ childId }) => queryKeys.childIncomeStatementStartDates(childId)
})

export const createChildIncomeStatementMutation = mutation({
  api: createChildIncomeStatement,
  invalidateQueryKeys: ({ childId }) => [
    queryKeys.allChildIncomeStatements(childId)
  ]
})

export const updateChildIncomeStatementMutation = mutation({
  api: updateChildIncomeStatement,
  invalidateQueryKeys: ({ childId }) => [
    queryKeys.allChildIncomeStatements(childId)
  ]
})

export const deleteChildIncomeStatementMutation = mutation({
  api: removeChildIncomeStatement,
  invalidateQueryKeys: ({ childId }) => [
    queryKeys.allChildIncomeStatements(childId)
  ]
})
