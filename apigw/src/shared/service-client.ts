// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import axios from 'axios'
import express from 'express'

import {
  EvakaSessionUser,
  integrationUserHeader,
  createUserHeader
} from './auth/index.js'
import { getJwt } from './auth/jwt.js'
import { evakaServiceUrl } from './config.js'

export const client = axios.create({
  baseURL: evakaServiceUrl
})

export type UUID = string

const machineUser: EvakaSessionUser = {
  id: '00000000-0000-0000-0000-000000000000',
  roles: [],
  userType: 'SYSTEM'
}

export type UserType =
  | 'EMPLOYEE'
  | 'MOBILE'
  | 'SYSTEM'
  | 'CITIZEN_STRONG'
  | 'CITIZEN_WEAK'

export type UserRole =
  | 'CITIZEN_WEAK'
  | 'ADMIN'
  | 'DIRECTOR'
  | 'REPORT_VIEWER'
  | 'FINANCE_ADMIN'
  | 'FINANCE_STAFF'
  | 'SERVICE_WORKER'
  | 'STAFF'
  | 'UNIT_SUPERVISOR'
  | 'MOBILE'

export type ServiceRequestHeader =
  | 'Authorization'
  | 'X-Request-ID'
  | 'EvakaMockedTime'
  | 'X-User'

export type ServiceRequestHeaders = { [H in ServiceRequestHeader]?: string }

export function createServiceRequestHeaders(
  req: express.Request | undefined,
  user: EvakaSessionUser | undefined | null = req?.user
) {
  const headers: ServiceRequestHeaders = {
    Authorization: `Bearer ${getJwt()}`
  }
  if (req?.path.startsWith('/integration/')) {
    headers['X-User'] = integrationUserHeader
  }
  if (user) {
    headers['X-User'] = createUserHeader(user)
  }
  if (req?.traceId) {
    headers['X-Request-ID'] = req.traceId
  }
  const mockedTime = req?.get('EvakaMockedTime')
  if (mockedTime) {
    headers['EvakaMockedTime'] = mockedTime
  }
  return headers
}

export interface EmployeeLoginRequest {
  externalId: UUID
  firstName: string
  lastName: string
  email?: string
  employeeNumber?: string
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
  keycloakEmail?: string
}

export interface CitizenUser {
  id: UUID
}

export interface CitizenUserResponse {
  details: unknown
  accessibleFeatures: unknown
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
): Promise<EmployeeUserResponse | undefined> {
  try {
    const { data } = await client.get<EmployeeUserResponse>(
      `/system/employee/${employeeId}`,
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

export async function getCitizenDetails(
  req: express.Request,
  personId: string
) {
  const { data } = await client.get<CitizenUserResponse>(
    `/system/citizen/${encodeURIComponent(personId)}`,
    {
      headers: createServiceRequestHeaders(req, machineUser)
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
  const { data } = await client.post<MobileDeviceIdentity>(
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
  pushApplicationServerKey: string | undefined
}

export async function identifyMobileDevice(
  req: express.Request,
  token: UUID
): Promise<MobileDeviceIdentity | undefined> {
  try {
    const { data } = await client.get<MobileDeviceIdentity | undefined>(
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

export async function authenticateMobileDevice(
  req: express.Request,
  id: UUID
): Promise<MobileDevice | undefined> {
  try {
    const { data } = await client.post<MobileDevice | undefined>(
      `/system/mobile-devices/${encodeURIComponent(id)}`,
      {
        userAgent: req.headers['user-agent'] ?? ''
      },
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

export interface EmployeePinLoginResponse {
  status: string
  employee?: Pick<EmployeeUser, 'firstName' | 'lastName'>
}

export async function employeePinLogin(
  req: express.Request
): Promise<EmployeePinLoginResponse> {
  const { data } = await client.post<EmployeePinLoginResponse>(
    `/system/mobile-pin-login`,
    req.body,
    {
      headers: createServiceRequestHeaders(req, machineUser)
    }
  )
  return data
}
