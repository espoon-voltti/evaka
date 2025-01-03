// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { EmployeeId } from 'lib-common/generated/api-types/shared'

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
import { queries } from '../../query'

const q = queries('employees')

export const searchEmployeesQuery = q.query(searchEmployees)

export const employeeDetailsQuery = q.query(getEmployeeDetails)

export const updateEmployeeGlobalRolesMutation = q.mutation(
  updateEmployeeGlobalRoles,
  [searchEmployeesQuery.all, ({ id }) => employeeDetailsQuery({ id })]
)

export const upsertEmployeeDaycareRolesMutation = q.mutation(
  upsertEmployeeDaycareRoles,
  [searchEmployeesQuery.all, ({ id }) => employeeDetailsQuery({ id })]
)

export const deleteEmployeeDaycareRolesMutation = q.mutation(
  deleteEmployeeDaycareRoles,
  [searchEmployeesQuery.all, ({ id }) => employeeDetailsQuery({ id })]
)

export const deleteEmployeeMobileDeviceMutation =
  q.parametricMutation<EmployeeId>()(deleteMobileDevice, [
    searchEmployeesQuery.all,
    (employeeId) => employeeDetailsQuery({ id: employeeId })
  ])

export const activateEmployeeMutation = q.mutation(activateEmployee, [
  searchEmployeesQuery.all,
  ({ id }) => employeeDetailsQuery({ id })
])

export const deactivateEmployeeMutation = q.mutation(deactivateEmployee, [
  searchEmployeesQuery.all,
  ({ id }) => employeeDetailsQuery({ id })
])
