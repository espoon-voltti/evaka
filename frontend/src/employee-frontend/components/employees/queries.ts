// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { Arg0, UUID } from 'lib-common/types'

import { deleteMobileDevice } from '../../generated/api-clients/pairing'
import {
  activateEmployee,
  deactivateEmployee,
  deleteEmployeeDaycareRoles,
  getEmployeeDetails,
  searchEmployees,
  updateEmployeeGlobalRoles,
  upsertEmployeeDaycareRoles
} from '../../generated/api-clients/pis'
import { createQueryKeys } from '../../query'

export const queryKeys = createQueryKeys('employees', {
  searchAll: () => ['search'],
  search: (
    page: number | null,
    pageSize: number | null,
    searchTerm: string | null
  ) => ['search', page, pageSize, searchTerm],
  byId: (id: UUID) => ['id', id]
})

export const searchEmployeesQuery = query({
  api: searchEmployees,
  queryKey: ({ body: { page, pageSize, searchTerm } }) =>
    queryKeys.search(page, pageSize, searchTerm)
})

export const employeeDetailsQuery = query({
  api: getEmployeeDetails,
  queryKey: ({ id }) => queryKeys.byId(id)
})

export const updateEmployeeGlobalRolesMutation = mutation({
  api: updateEmployeeGlobalRoles,
  invalidateQueryKeys: (args) => [
    queryKeys.searchAll(),
    queryKeys.byId(args.id)
  ]
})

export const upsertEmployeeDaycareRolesMutation = mutation({
  api: upsertEmployeeDaycareRoles,
  invalidateQueryKeys: (args) => [
    queryKeys.searchAll(),
    queryKeys.byId(args.id)
  ]
})

export const deleteEmployeeDaycareRolesMutation = mutation({
  api: deleteEmployeeDaycareRoles,
  invalidateQueryKeys: (args) => [
    queryKeys.searchAll(),
    queryKeys.byId(args.id)
  ]
})

export const deleteEmployeeMobileDeviceMutation = mutation({
  api: (args: Arg0<typeof deleteMobileDevice> & { employeeId: UUID }) =>
    deleteMobileDevice(args),
  invalidateQueryKeys: (args) => [
    queryKeys.searchAll(),
    queryKeys.byId(args.employeeId)
  ]
})

export const activateEmployeeMutation = mutation({
  api: activateEmployee,
  invalidateQueryKeys: ({ id }) => [queryKeys.searchAll(), queryKeys.byId(id)]
})

export const deactivateEmployeeMutation = mutation({
  api: deactivateEmployee,
  invalidateQueryKeys: ({ id }) => [queryKeys.searchAll(), queryKeys.byId(id)]
})
