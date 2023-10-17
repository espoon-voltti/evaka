// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { createQueryKeys } from '../query'

import {
  createChildIncomeStatement,
  createIncomeStatement,
  deleteChildIncomeStatement,
  deleteIncomeStatement,
  getChildIncomeStatement,
  getChildIncomeStatements,
  getChildIncomeStatementStartDates,
  getGuardianIncomeStatementChildren,
  getIncomeStatement,
  getIncomeStatements,
  getIncomeStatementStartDates,
  updateChildIncomeStatement,
  updateIncomeStatement
} from './api'

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
  childIncomeStatement: ({ childId, id }: { childId: UUID; id: UUID }) => [
    'childIncomeStatements',
    childId,
    'single',
    id
  ],
  childIncomeStatementStartDates: (childId: UUID) => [
    'childIncomeStatements',
    childId,
    'startDates'
  ]
})

// Guardian

export const incomeStatementsQuery = query({
  api: getIncomeStatements,
  queryKey: queryKeys.incomeStatements
})

export const incomeStatementQuery = query({
  api: getIncomeStatement,
  queryKey: queryKeys.incomeStatement
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
  api: getGuardianIncomeStatementChildren,
  queryKey: queryKeys.guardianIncomeStatementChildren
})

export const childIncomeStatementsQuery = query({
  api: getChildIncomeStatements,
  queryKey: queryKeys.childIncomeStatements
})

export const childIncomeStatementQuery = query({
  api: getChildIncomeStatement,
  queryKey: queryKeys.childIncomeStatement
})

export const childIncomeStatementStartDatesQuery = query({
  api: getChildIncomeStatementStartDates,
  queryKey: queryKeys.childIncomeStatementStartDates
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
  api: deleteChildIncomeStatement,
  invalidateQueryKeys: ({ childId }) => [
    queryKeys.allChildIncomeStatements(childId)
  ]
})
