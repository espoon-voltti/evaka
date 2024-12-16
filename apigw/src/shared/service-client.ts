// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import axios from 'axios'
import express from 'express'

import { systemUserHeader } from './auth/index.js'
import { getJwt } from './auth/jwt.js'
import { evakaServiceUrl } from './config.js'

export const client = axios.create({
  baseURL: evakaServiceUrl
})

export type UUID = string

export type UserRole =
  | 'ADMIN'
  | 'REPORT_VIEWER'
  | 'DIRECTOR'
  | 'FINANCE_ADMIN'
  | 'FINANCE_STAFF'
  | 'SERVICE_WORKER'
  | 'MESSAGING'
  | 'UNIT_SUPERVISOR'
  | 'STAFF'
  | 'SPECIAL_EDUCATION_TEACHER'
  | 'EARLY_CHILDHOOD_EDUCATION_SECRETARY'

export type ServiceRequestHeader =
  | 'Authorization'
  | 'X-Request-ID'
  | 'EvakaMockedTime'
  | 'X-User'

export type ServiceRequestHeaders = Partial<
  Record<ServiceRequestHeader, string>
>

export function createServiceRequestHeaders(
  req: express.Request | undefined,
  userHeader: string | undefined
) {
  const headers: ServiceRequestHeaders = {
    Authorization: `Bearer ${getJwt()}`
  }
  if (userHeader) {
    headers['X-User'] = userHeader
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

export interface EmployeeSuomiFiLoginRequest {
  ssn: string
  firstName: string
  lastName: string
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
      headers: createServiceRequestHeaders(undefined, systemUserHeader)
    }
  )
  return data
}

export async function employeeSuomiFiLogin(
  employee: EmployeeSuomiFiLoginRequest
): Promise<EmployeeUser> {
  const { data } = await client.post<EmployeeUser>(
    `/system/employee-sfi-login`,
    employee,
    {
      headers: createServiceRequestHeaders(undefined, systemUserHeader)
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
        headers: createServiceRequestHeaders(req, systemUserHeader)
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
      headers: createServiceRequestHeaders(undefined, systemUserHeader)
    }
  )
  return data
}

interface CitizenWeakLoginRequest {
  username: string
  password: string
}

export async function citizenWeakLogin(
  request: CitizenWeakLoginRequest
): Promise<CitizenUser> {
  const { data } = await client.post<CitizenUser>(
    `/system/citizen-weak-login`,
    request,
    {
      headers: createServiceRequestHeaders(undefined, systemUserHeader)
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
      headers: createServiceRequestHeaders(req, systemUserHeader)
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
      headers: createServiceRequestHeaders(req, systemUserHeader)
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
        headers: createServiceRequestHeaders(req, systemUserHeader)
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
        headers: createServiceRequestHeaders(req, systemUserHeader)
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
      headers: createServiceRequestHeaders(req, systemUserHeader)
    }
  )
  return data
}
