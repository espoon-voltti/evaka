// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { adExternalIdPrefix } from './config'
import { client, UserRole, UUID } from './service-client'

interface DevEmployee {
  firstName: string
  lastName: string
  email: string
  externalId: string
  roles: UserRole[]
}

export async function upsertEmployee(employee: DevEmployee): Promise<UUID> {
  const { data } = await client.post(
    `/dev-api/employee/external-id/${employee.externalId}`,
    employee
  )
  return data
}

export async function getEmployeeByExternalId(externalId: string) {
  const { data } = await client.get<Employee>(
    `/dev-api/employee/external-id/${adExternalIdPrefix}:${externalId}`
  )
  return data
}

interface Employee {
  id: UUID
  firstName: string
  lastName: string
  email: string | null
  externalId: string | null
  employeeNumber: string | null
}

export async function getEmployees(): Promise<Employee[]> {
  const { data } = await client.get(`/dev-api/employee`)
  return data
}
