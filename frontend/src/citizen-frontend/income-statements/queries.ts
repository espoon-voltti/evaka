// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

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
  incomeStatements: (page: number, pageSize: number) => [
    'incomeStatements',
    'paginated',
    page,
    pageSize
  ],
  incomeStatement: (incomeStatementId: UUID) => [
    'incomeStatements',
    'single',
    incomeStatementId
  ],
  incomeStatementStartDates: () => ['incomeStatements', 'startDates'],

  guardianIncomeStatementChildren: () => ['guardianIncomeStatementChildren'],

  allChildIncomeStatements: (childId: UUID) => [
    'childIncomeStatements',
    childId
  ],
  childIncomeStatements: (childId: UUID, page: number, pageSize: number) => [
    'childIncomeStatements',
    childId,
    'paginated',
    page,
    pageSize
  ],
  childIncomeStatement: ({
    childId,
    incomeStatementId
  }: {
    childId: UUID
    incomeStatementId: UUID
  }) => ['childIncomeStatements', childId, 'single', incomeStatementId],
  childIncomeStatementStartDates: (childId: UUID) => [
    'childIncomeStatements',
    childId,
    'startDates'
  ]
})

// Guardian

export const incomeStatementsQuery = query({
  api: getIncomeStatements,
  queryKey: ({ page, pageSize }) => queryKeys.incomeStatements(page, pageSize)
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
  queryKey: ({ childId, page, pageSize }) =>
    queryKeys.childIncomeStatements(childId, page, pageSize)
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
