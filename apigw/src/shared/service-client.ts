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
  | 'REPORT_VIEWER'
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

export interface EmployeeLoginRequest {
  externalId: UUID
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

export interface EmployeeUserResponse extends EmployeeUser {
  accessibleFeatures: object
  permittedGlobalActions?: string[]
}

export interface CitizenLoginRequest {
  socialSecurityNumber: string
  firstName: string
  lastName: string
}

export interface CitizenUser {
  id: UUID
}

export async function employeeLogin(
  employee: EmployeeLoginRequest
): Promise<EmployeeUser> {
  const { data } = await client.post<EmployeeUser>(
    `/system/employee-login`,
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
  const { data } = await client.get<EmployeeUserResponse>(
    `/system/employee/${employeeId}`,
    {
      headers: createServiceRequestHeaders(req, machineUser)
    }
  )
  return data
}

export async function citizenLogin(
  person: CitizenLoginRequest
): Promise<CitizenUser> {
  const { data } = await client.post<CitizenUser>(
    `/system/citizen-login`,
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
  unitIds: UUID[]
  employeeId?: UUID
  personalDevice: boolean
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

export interface EmployeePinLoginRequest {
  employeeId: UUID
  pin: string
}

export interface EmployeePinLoginResponse {
  status: string
  employee?: Pick<EmployeeUser, 'firstName' | 'lastName'>
}

export async function employeePinLogin(
  req: express.Request
): Promise<EmployeePinLoginResponse> {
  const { data } = await client.post<EmployeePinLoginResponse>(
    `/mobile-devices/pin-login`,
    req.body,
    {
      headers: {
        ...createServiceRequestHeaders(req)
      }
    }
  )
  return data
}
