// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'

import {
  activateEmployee,
  deactivateEmployee,
  searchEmployees
} from '../../api/employees'
import { createQueryKeys } from '../../query'

export const queryKeys = createQueryKeys('employees', {
  searchAll: () => ['search'],
  search: (page: number, pageSize: number, searchTerm?: string) => [
    'search',
    page,
    pageSize,
    searchTerm
  ]
})

export const searchEmployeesQuery = query({
  api: searchEmployees,
  queryKey: queryKeys.search
})

export const activateEmployeeMutation = mutation({
  api: activateEmployee,
  invalidateQueryKeys: () => [queryKeys.searchAll()]
})

export const deactivateEmployeeMutation = mutation({
  api: deactivateEmployee,
  invalidateQueryKeys: () => [queryKeys.searchAll()]
})
