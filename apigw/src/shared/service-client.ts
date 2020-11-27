// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import express from 'express'
import axios from 'axios'
import { evakaServiceUrl } from './config'
import { createAuthHeader } from './auth'
import { SamlUser } from './routes/auth/saml/types'

export const client = axios.create({
  baseURL: evakaServiceUrl
})

export type UUID = string

const machineUser: SamlUser = {
  id: '00000000-0000-0000-0000-000000000000',
  roles: [],
  userType: 'SYSTEM'
}

export type UserType = 'ENDUSER' | 'EMPLOYEE' | 'SYSTEM'

export type UserRole =
  | 'ENDUSER'
  | 'ADMIN'
  | 'DIRECTOR'
  | 'FINANCE_ADMIN'
  | 'SERVICE_WORKER'
  | 'STAFF'
  | 'UNIT_SUPERVISOR'

export type ServiceRequestHeader = 'Authorization' | 'X-Request-ID'
export type ServiceRequestHeaders = { [H in ServiceRequestHeader]?: string }

export function createServiceRequestHeaders(
  req: express.Request | undefined,
  user: SamlUser | undefined | null = req?.user
) {
  const headers: ServiceRequestHeaders = {}
  if (user) {
    headers.Authorization = createAuthHeader(user)
  }
  if (req?.traceId) {
    headers['X-Request-ID'] = req.traceId
  }
  return headers
}

export interface AuthenticatedUser {
  id: UUID
  roles: UserRole[]
}

export interface EmployeeIdentityRequest {
  aad: string
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
  firstName: string
  lastName: string
}

export async function getOrCreateEmployee(
  employee: EmployeeIdentityRequest
): Promise<AuthenticatedUser> {
  const { data } = await client.post<AuthenticatedUser>(
    `/system/employee-identity`,
    employee,
    {
      headers: createServiceRequestHeaders(undefined, machineUser)
    }
  )
  return data
}

export async function getEmployeeDetails(
  req: express.Request,
  employeeId: string
) {
  const { data } = await client.get<EmployeeResponse>(
    `/employee/${employeeId}`,
    {
      headers: createServiceRequestHeaders(req)
    }
  )
  return data
}

export async function getOrCreatePerson(
  person: PersonIdentityRequest
): Promise<AuthenticatedUser> {
  const { data } = await client.post<AuthenticatedUser>(
    `/system/person-identity`,
    person,
    {
      headers: createServiceRequestHeaders(undefined, machineUser)
    }
  )
  return data
}

export async function getUserDetails(req: express.Request, personId: string) {
  const { data } = await client.get(`/persondetails/uuid/${personId}`, {
    headers: createServiceRequestHeaders(req)
  })
  return data
}
