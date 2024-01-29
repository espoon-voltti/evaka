// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UserRole } from 'lib-common/generated/api-types/shared'
import { mutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import {
  activateEmployee,
  deactivateEmployee,
  getEmployeeDetails,
  searchEmployees,
  updateEmployee
} from '../../api/employees'
import { createQueryKeys } from '../../query'

export const queryKeys = createQueryKeys('employees', {
  searchAll: () => ['search'],
  search: (page: number, pageSize: number, searchTerm?: string) => [
    'search',
    page,
    pageSize,
    searchTerm
  ],
  byId: (id: UUID) => ['id', id]
})

export const searchEmployeesQuery = query({
  api: searchEmployees,
  queryKey: queryKeys.search
})

export const employeeDetailsQuery = query({
  api: getEmployeeDetails,
  queryKey: queryKeys.byId
})

export const updateEmployeeGlobalRolesMutation = mutation({
  api: (args: { id: UUID; globalRoles: UserRole[] }) =>
    updateEmployee(args.id, args.globalRoles),
  invalidateQueryKeys: (args) => [
    queryKeys.searchAll(),
    queryKeys.byId(args.id)
  ]
})

export const activateEmployeeMutation = mutation({
  api: activateEmployee,
  invalidateQueryKeys: (id) => [queryKeys.searchAll(), queryKeys.byId(id)]
})

export const deactivateEmployeeMutation = mutation({
  api: deactivateEmployee,
  invalidateQueryKeys: (id) => [queryKeys.searchAll(), queryKeys.byId(id)]
})
