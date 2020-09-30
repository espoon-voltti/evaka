// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import express from 'express'
import { createAuthHeader, createHeaders } from '../auth'
import { client } from '../service-client'

export type UUID = string

const NIL_UUID = '00000000-0000-0000-0000-000000000000'

export type UserRole =
  | 'ADMIN'
  | 'DIRECTOR'
  | 'FINANCE_ADMIN'
  | 'SERVICE_WORKER'
  | 'STAFF'
  | 'UNIT_SUPERVISOR'

export interface AuthenticatedUser {
  id: UUID
  roles: UserRole[]
}

export interface EmployeeIdentityRequest {
  aad?: string
  firstName: string
  lastName: string
  email: string
}

export interface EmployeeResponse {
  id: string
  aad: string | null
  firstName: string
  lastName: string
  email: string
  created: string
  updated: string
}

export interface PersonIdentityRequest {
  socialSecurityNumber: string
  customerId?: number
  firstName: string
  lastName: string
  email: string
  language?: string
}

export async function getOrCreateEmployee(
  employee: EmployeeIdentityRequest
): Promise<AuthenticatedUser> {
  return client
    .post<AuthenticatedUser>(`/employee/identity`, employee, {
      headers: {
        Authorization: createAuthHeader({ id: NIL_UUID, roles: [] })
      }
    })
    .then((response) => response.data)
}

export async function getEmployeeDetails(
  req: express.Request,
  employeeId: string
) {
  return client
    .get<EmployeeResponse>(`/employee/${employeeId}`, {
      headers: createHeaders(req)
    })
    .then((res) => {
      return res.data
    })
}

export async function getPersonIdentity(
  person: PersonIdentityRequest
): Promise<AuthenticatedUser> {
  return client
    .post<AuthenticatedUser>(`/person/identity`, person, {
      headers: {
        Authorization: createAuthHeader({ id: NIL_UUID, roles: [] })
      }
    })
    .then((response) => response.data)
}
