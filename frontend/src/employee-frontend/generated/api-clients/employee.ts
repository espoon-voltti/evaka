// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { CreateSsnEmployeeResponse } from 'lib-common/generated/api-types/employee'
import type { DaycareId } from 'lib-common/generated/api-types/shared'
import type { Employee } from 'lib-common/generated/api-types/employee'
import type { EmployeeEmailRequest } from 'lib-common/generated/api-types/employee'
import type { EmployeeId } from 'lib-common/generated/api-types/shared'
import type { EmployeePreferredFirstName } from 'lib-common/generated/api-types/employee'
import type { EmployeeSetPreferredFirstNameUpdateRequest } from 'lib-common/generated/api-types/employee'
import type { EmployeeWithDaycareRoles } from 'lib-common/generated/api-types/employee'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import type { NewSsnEmployee } from 'lib-common/generated/api-types/employee'
import type { PinCode } from 'lib-common/generated/api-types/employee'
import type { SearchEmployeeRequest } from 'lib-common/generated/api-types/employee'
import type { UpsertEmployeeDaycareRolesRequest } from 'lib-common/generated/api-types/employee'
import type { UserRole } from 'lib-common/generated/api-types/shared'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonEmployee } from 'lib-common/generated/api-types/employee'
import { deserializeJsonEmployeeWithDaycareRoles } from 'lib-common/generated/api-types/employee'
import { uri } from 'lib-common/uri'


/**
* Generated from evaka.core.employee.EmployeeController.activateEmployee
*/
export async function activateEmployee(
  request: {
    id: EmployeeId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/employees/${request.id}/activate`.toString(),
    method: 'PUT'
  })
  return json
}


/**
* Generated from evaka.core.employee.EmployeeController.createSsnEmployee
*/
export async function createSsnEmployee(
  request: {
    body: NewSsnEmployee
  }
): Promise<CreateSsnEmployeeResponse> {
  const { data: json } = await client.request<JsonOf<CreateSsnEmployeeResponse>>({
    url: uri`/employee/employees/create-with-ssn`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<NewSsnEmployee>
  })
  return json
}


/**
* Generated from evaka.core.employee.EmployeeController.deactivateEmployee
*/
export async function deactivateEmployee(
  request: {
    id: EmployeeId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/employees/${request.id}/deactivate`.toString(),
    method: 'PUT'
  })
  return json
}


/**
* Generated from evaka.core.employee.EmployeeController.deleteEmployee
*/
export async function deleteEmployee(
  request: {
    id: EmployeeId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/employees/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from evaka.core.employee.EmployeeController.deleteEmployeeDaycareRoles
*/
export async function deleteEmployeeDaycareRoles(
  request: {
    id: EmployeeId,
    daycareId?: DaycareId | null
  }
): Promise<void> {
  const params = createUrlSearchParams(
    ['daycareId', request.daycareId]
  )
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/employees/${request.id}/daycare-roles`.toString(),
    method: 'DELETE',
    params
  })
  return json
}


/**
* Generated from evaka.core.employee.EmployeeController.deleteEmployeeScheduledDaycareRole
*/
export async function deleteEmployeeScheduledDaycareRole(
  request: {
    id: EmployeeId,
    daycareId: DaycareId
  }
): Promise<void> {
  const params = createUrlSearchParams(
    ['daycareId', request.daycareId]
  )
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/employees/${request.id}/scheduled-daycare-role`.toString(),
    method: 'DELETE',
    params
  })
  return json
}


/**
* Generated from evaka.core.employee.EmployeeController.getEmployeeDetails
*/
export async function getEmployeeDetails(
  request: {
    id: EmployeeId
  }
): Promise<EmployeeWithDaycareRoles> {
  const { data: json } = await client.request<JsonOf<EmployeeWithDaycareRoles>>({
    url: uri`/employee/employees/${request.id}/details`.toString(),
    method: 'GET'
  })
  return deserializeJsonEmployeeWithDaycareRoles(json)
}


/**
* Generated from evaka.core.employee.EmployeeController.getEmployeePreferredFirstName
*/
export async function getEmployeePreferredFirstName(): Promise<EmployeePreferredFirstName> {
  const { data: json } = await client.request<JsonOf<EmployeePreferredFirstName>>({
    url: uri`/employee/employees/preferred-first-name`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from evaka.core.employee.EmployeeController.getEmployees
*/
export async function getEmployees(): Promise<Employee[]> {
  const { data: json } = await client.request<JsonOf<Employee[]>>({
    url: uri`/employee/employees`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonEmployee(e))
}


/**
* Generated from evaka.core.employee.EmployeeController.getFinanceDecisionHandlers
*/
export async function getFinanceDecisionHandlers(): Promise<Employee[]> {
  const { data: json } = await client.request<JsonOf<Employee[]>>({
    url: uri`/employee/employees/finance-decision-handler`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonEmployee(e))
}


/**
* Generated from evaka.core.employee.EmployeeController.isPinLocked
*/
export async function isPinLocked(): Promise<boolean> {
  const { data: json } = await client.request<JsonOf<boolean>>({
    url: uri`/employee/employees/pin-code/is-pin-locked`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from evaka.core.employee.EmployeeController.searchEmployees
*/
export async function searchEmployees(
  request: {
    body: SearchEmployeeRequest
  }
): Promise<EmployeeWithDaycareRoles[]> {
  const { data: json } = await client.request<JsonOf<EmployeeWithDaycareRoles[]>>({
    url: uri`/employee/employees/search`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<SearchEmployeeRequest>
  })
  return json.map(e => deserializeJsonEmployeeWithDaycareRoles(e))
}


/**
* Generated from evaka.core.employee.EmployeeController.setEmployeePreferredFirstName
*/
export async function setEmployeePreferredFirstName(
  request: {
    body: EmployeeSetPreferredFirstNameUpdateRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/employees/preferred-first-name`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<EmployeeSetPreferredFirstNameUpdateRequest>
  })
  return json
}


/**
* Generated from evaka.core.employee.EmployeeController.updateEmployeeEmail
*/
export async function updateEmployeeEmail(
  request: {
    id: EmployeeId,
    body: EmployeeEmailRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/employees/${request.id}/update-email`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<EmployeeEmailRequest>
  })
  return json
}


/**
* Generated from evaka.core.employee.EmployeeController.updateEmployeeGlobalRoles
*/
export async function updateEmployeeGlobalRoles(
  request: {
    id: EmployeeId,
    body: UserRole[]
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/employees/${request.id}/global-roles`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<UserRole[]>
  })
  return json
}


/**
* Generated from evaka.core.employee.EmployeeController.upsertEmployeeDaycareRoles
*/
export async function upsertEmployeeDaycareRoles(
  request: {
    id: EmployeeId,
    body: UpsertEmployeeDaycareRolesRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/employees/${request.id}/daycare-roles`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<UpsertEmployeeDaycareRolesRequest>
  })
  return json
}


/**
* Generated from evaka.core.employee.EmployeeController.upsertPinCode
*/
export async function upsertPinCode(
  request: {
    body: PinCode
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/employees/pin-code`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<PinCode>
  })
  return json
}
