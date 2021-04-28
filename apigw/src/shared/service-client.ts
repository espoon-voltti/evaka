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

export type UserType =
  | 'ENDUSER'
  | 'EMPLOYEE'
  | 'MOBILE'
  | 'SYSTEM'
  | 'CITIZEN_STRONG'
  | 'CITIZEN_WEAK'

export type UserRole =
  | 'ENDUSER'
  | 'CITIZEN_WEAK'
  | 'ADMIN'
  | 'DIRECTOR'
  | 'FINANCE_ADMIN'
  | 'SERVICE_WORKER'
  | 'STAFF'
  | 'UNIT_SUPERVISOR'
  | 'MOBILE'

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
  externalId: string
  firstName: string
  lastName: string
  email?: string
}

export interface EmployeeUser {
  id: string
  firstName: string
  lastName: string
  globalRoles: UserRole[]
  allScopedRoles: UserRole[]
}

export interface PersonIdentityRequest {
  socialSecurityNumber: string
  firstName: string
  lastName: string
}

export async function getOrCreateEmployee(
  employee: EmployeeIdentityRequest
): Promise<EmployeeUser> {
  const { data } = await client.post<EmployeeUser>(
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
  const { data } = await client.get<EmployeeUser>(
    `/system/employee/${employeeId}`,
    {
      headers: createServiceRequestHeaders(req, machineUser)
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

export interface ValidatePairingRequest {
  challengeKey: string
  responseKey: string
}

export interface MobileDeviceIdentity {
  id: UUID
  longTermToken: UUID
}

export async function validatePairing(
  req: express.Request,
  id: UUID,
  request: ValidatePairingRequest
): Promise<MobileDeviceIdentity> {
  const { data } = await client.post(
    `/system/pairings/${encodeURIComponent(id)}/validation`,
    request,
    {
      headers: createServiceRequestHeaders(req, machineUser)
    }
  )
  return data
}

export interface MobileDevice {
  id: UUID
  name: string
  unitId: UUID
}

export async function identifyMobileDevice(
  req: express.Request,
  token: UUID
): Promise<MobileDeviceIdentity | undefined> {
  try {
    const { data } = await client.get(
      `/system/mobile-identity/${encodeURIComponent(token)}`,
      {
        headers: createServiceRequestHeaders(req, machineUser)
      }
    )
    return data
  } catch (e: unknown) {
    if (axios.isAxiosError(e) && e.response?.status === 404) {
      return undefined
    } else {
      throw e
    }
  }
}

export async function getMobileDevice(
  req: express.Request,
  id: UUID
): Promise<MobileDevice | undefined> {
  try {
    const { data } = await client.get(
      `/system/mobile-devices/${encodeURIComponent(id)}`,
      {
        headers: createServiceRequestHeaders(req, machineUser)
      }
    )
    return data
  } catch (e: unknown) {
    if (axios.isAxiosError(e) && e.response?.status === 404) {
      return undefined
    } else {
      throw e
    }
  }
}

export async function getUserDetails(req: express.Request, personId: string) {
  const { data } = await client.get(`/persondetails/uuid/${personId}`, {
    headers: createServiceRequestHeaders(req)
  })
  return data
}
