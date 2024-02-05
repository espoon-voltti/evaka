// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { MobileDevice } from 'lib-common/generated/api-types/pairing'
import {
  Employee,
  EmployeePreferredFirstName,
  EmployeeSetPreferredFirstNameUpdateRequest,
  EmployeeWithDaycareRoles,
  PagedEmployeesWithDaycareRoles,
  UpsertEmployeeDaycareRolesRequest
} from 'lib-common/generated/api-types/pis'
import { UserRole } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'

import { FinanceDecisionHandlerOption } from '../state/invoicing-ui'

import { client } from './client'

export async function getEmployees(): Promise<Result<Employee[]>> {
  return client
    .get<JsonOf<Employee[]>>(`/employee`)
    .then((res) =>
      res.data.map((data) => ({
        ...data,
        created: HelsinkiDateTime.parseIso(data.created),
        updated:
          data.updated !== null ? HelsinkiDateTime.parseIso(data.updated) : null
      }))
    )
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function getFinanceDecisionHandlers(): Promise<
  Result<FinanceDecisionHandlerOption[]>
> {
  return client
    .get<JsonOf<Employee[]>>(`/employee/finance-decision-handler`)
    .then((res) =>
      res.data.map((data) => ({
        value: data.id,
        label: [data.firstName, data.lastName].join(' ')
      }))
    )
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function updatePinCode(pinCode: string): Promise<Result<void>> {
  return client
    .post<void>(`/employee/pin-code`, { pin: pinCode })
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function isPinCodeLocked(): Promise<Result<boolean>> {
  return client
    .get<boolean>(`/employee/pin-code/is-pin-locked`)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function searchEmployees(
  page: number,
  pageSize: number,
  searchTerm?: string
): Promise<PagedEmployeesWithDaycareRoles> {
  return client
    .post<JsonOf<PagedEmployeesWithDaycareRoles>>('/employee/search', {
      page,
      pageSize,
      searchTerm
    })
    .then((res) => ({
      ...res.data,
      data: res.data.data.map(deserializeEmployeeWithDaycareRoles)
    }))
}

export async function getEmployeeDetails(
  id: UUID
): Promise<EmployeeWithDaycareRoles> {
  return client
    .get<JsonOf<EmployeeWithDaycareRoles>>(`/employee/${id}/details`)
    .then((res) => deserializeEmployeeWithDaycareRoles(res.data))
}

function deserializeEmployeeWithDaycareRoles(
  data: JsonOf<EmployeeWithDaycareRoles>
): EmployeeWithDaycareRoles {
  return {
    ...data,
    created: HelsinkiDateTime.parseIso(data.created),
    updated: data.updated ? HelsinkiDateTime.parseIso(data.updated) : null,
    lastLogin: data.lastLogin ? HelsinkiDateTime.parseIso(data.lastLogin) : null
  }
}

export async function updateEmployeeGlobalRoles(
  id: UUID,
  globalRoles: UserRole[]
): Promise<void> {
  await client.put(`/employee/${id}/global-roles`, globalRoles)
}

export async function upsertEmployeeDaycareRoles(
  id: UUID,
  body: UpsertEmployeeDaycareRolesRequest
): Promise<void> {
  await client.put(`/employee/${id}/daycare-roles`, body)
}

export async function deleteEmployeeDaycareRoles(
  employeeId: UUID,
  daycareId: UUID | null
): Promise<void> {
  await client.delete(`/employee/${employeeId}/daycare-roles`, {
    params: { daycareId }
  })
}

export function activateEmployee(id: UUID): Promise<Result<void>> {
  return client
    .put(`/employee/${id}/activate`)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export function deactivateEmployee(id: UUID): Promise<Result<void>> {
  return client
    .put(`/employee/${id}/deactivate`)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export function getPersonalMobileDevices(): Promise<Result<MobileDevice[]>> {
  return client
    .get<JsonOf<MobileDevice[]>>('/mobile-devices/personal')
    .then(({ data }) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}

export function getEmployeePreferredFirstName(): Promise<
  Result<EmployeePreferredFirstName>
> {
  return client
    .get<JsonOf<EmployeePreferredFirstName>>('/employee/preferred-first-name')
    .then(({ data }) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}

export function setEmployeePreferredFirstName(
  preferredFirstName: EmployeeSetPreferredFirstNameUpdateRequest
): Promise<Result<void>> {
  return client
    .post('/employee/preferred-first-name', preferredFirstName)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
