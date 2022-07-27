// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Paged, Result } from 'lib-common/api'
import { Failure, Success } from 'lib-common/api'
import type { GlobalRole } from 'lib-common/api-types/employee-auth'
import type { MobileDevice } from 'lib-common/generated/api-types/pairing'
import type { JsonOf } from 'lib-common/json'
import type { UUID } from 'lib-common/types'

import type { FinanceDecisionHandlerOption } from '../state/invoicing-ui'
import type { Employee, EmployeeUser } from '../types/employee'

import { client } from './client'

export async function getEmployees(): Promise<Result<Employee[]>> {
  return client
    .get<JsonOf<Employee[]>>(`/employee`)
    .then((res) =>
      res.data.map((data) => ({
        ...data,
        created: new Date(data.created),
        updated: new Date(data.updated)
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

export function searchEmployees(
  page: number,
  pageSize: number,
  searchTerm?: string
): Promise<Result<Paged<EmployeeUser>>> {
  return client
    .post<JsonOf<Paged<EmployeeUser>>>('/employee/search', {
      page,
      pageSize,
      searchTerm
    })
    .then(({ data }) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}

export function getEmployeeDetails(id: UUID): Promise<Result<EmployeeUser>> {
  return client
    .get<JsonOf<EmployeeUser>>(`/employee/${id}/details`)
    .then(({ data }) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}

export function updateEmployee(
  id: UUID,
  globalRoles: GlobalRole[]
): Promise<Result<void>> {
  return client
    .put(`/employee/${id}`, {
      globalRoles
    })
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export function getPersonalMobileDevices(): Promise<Result<MobileDevice[]>> {
  return client
    .get<JsonOf<MobileDevice[]>>('/mobile-devices/personal')
    .then(({ data }) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}
