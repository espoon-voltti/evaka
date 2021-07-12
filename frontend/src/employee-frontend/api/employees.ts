// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Paged, Result, Success } from 'lib-common/api'
import { JsonOf } from 'lib-common/json'
import { client } from './client'
import { FinanceDecisionHandlerOption } from '../state/invoicing-ui'
import { Employee, EmployeeUser } from '../types/employee'
import { UUID } from '../../lib-common/types'
import { GlobalRole } from '../types'

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
    .get<JsonOf<Paged<EmployeeUser>>>('/employee/search', {
      params: { page, pageSize, searchTerm }
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
): Promise<Result<null>> {
  return client
    .put(`/employee/${id}`, {
      globalRoles
    })
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}
