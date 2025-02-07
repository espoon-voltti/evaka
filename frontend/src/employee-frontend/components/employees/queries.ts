// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { EmployeeId } from 'lib-common/generated/api-types/shared'
import { Queries } from 'lib-common/query'

import { deleteMobileDevice } from '../../generated/api-clients/pairing'
import {
  activateEmployee,
  createSsnEmployee,
  deactivateEmployee,
  deleteEmployeeDaycareRoles,
  deleteEmployeeScheduledDaycareRole,
  getEmployeeDetails,
  searchEmployees,
  updateEmployeeGlobalRoles,
  upsertEmployeeDaycareRoles
} from '../../generated/api-clients/pis'

const q = new Queries()

export const searchEmployeesQuery = q.query(searchEmployees)

export const employeeDetailsQuery = q.query(getEmployeeDetails)

export const updateEmployeeGlobalRolesMutation = q.mutation(
  updateEmployeeGlobalRoles,
  [searchEmployeesQuery.prefix, ({ id }) => employeeDetailsQuery({ id })]
)

export const upsertEmployeeDaycareRolesMutation = q.mutation(
  upsertEmployeeDaycareRoles,
  [searchEmployeesQuery.prefix, ({ id }) => employeeDetailsQuery({ id })]
)

export const deleteEmployeeDaycareRolesMutation = q.mutation(
  deleteEmployeeDaycareRoles,
  [searchEmployeesQuery.prefix, ({ id }) => employeeDetailsQuery({ id })]
)

export const deleteEmployeeScheduledDaycareRoleMutation = q.mutation(
  deleteEmployeeScheduledDaycareRole,
  [searchEmployeesQuery.prefix, ({ id }) => employeeDetailsQuery({ id })]
)

export const deleteEmployeeMobileDeviceMutation = q.parametricMutation<{
  employeeId: EmployeeId
}>()(deleteMobileDevice, [
  searchEmployeesQuery.prefix,
  ({ employeeId }) => employeeDetailsQuery({ id: employeeId })
])

export const activateEmployeeMutation = q.mutation(activateEmployee, [
  searchEmployeesQuery.prefix,
  ({ id }) => employeeDetailsQuery({ id })
])

export const deactivateEmployeeMutation = q.mutation(deactivateEmployee, [
  searchEmployeesQuery.prefix,
  ({ id }) => employeeDetailsQuery({ id })
])

export const createSsnEmployeeMutation = q.mutation(createSsnEmployee, [
  searchEmployeesQuery.prefix
])
