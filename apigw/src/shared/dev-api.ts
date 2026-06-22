// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { UUID } from './service-client.ts'
import { client } from './service-client.ts'

interface Employee {
  id: UUID
  firstName: string
  lastName: string
  email: string | null
  externalId: string | null
}

export async function getEmployees(): Promise<Employee[]> {
  const { data } = await client.get<Employee[]>(`/dev-api/employee`)
  return data
}
