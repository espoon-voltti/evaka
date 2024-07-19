// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import express from 'express'

import { client, UUID } from './service-client.js'

export function stringToInt(input: string | undefined): number | undefined {
  if (typeof input !== 'string') {
    return undefined
  }
  const parsed = parseInt(input, 10)
  return isNaN(parsed) ? undefined : parsed
}

export function getDatabaseId(req: express.Request): number | undefined {
  return stringToInt(req.header('EvakaDatabaseId'))
}

export async function getCitizens(
  databaseId: number | undefined
): Promise<DevCitizen[]> {
  const { data } = await client.get<DevCitizen[]>(`/dev-api/citizen`, {
    headers: { EvakaDatabaseId: databaseId }
  })
  return data
}

export interface DevCitizen {
  ssn: string
  firstName: string
  lastName: string
  dependantCount: number
}

interface Employee {
  id: UUID
  firstName: string
  lastName: string
  email: string | null
  externalId: string | null
}

export async function getEmployees(
  databaseId: number | undefined
): Promise<Employee[]> {
  const { data } = await client.get<Employee[]>(`/dev-api/employee`, {
    headers: { EvakaDatabaseId: databaseId }
  })
  return data
}
