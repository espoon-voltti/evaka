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

export interface CareAreaResponseJSON {
  id: UUID
  name: string
  shortName: string
  daycares: LocationResponseJSON[]
}

export type CareTypes =
  | 'CENTRE'
  | 'FAMILY'
  | 'GROUP_FAMILY'
  | 'CLUB'
  | 'PRESCHOOL'
  | 'PREPARATORY_EDUCATION'

export type ProviderType =
  | 'MUNICIPAL'
  | 'PURCHASED'
  | 'PRIVATE'
  | 'MUNICIPAL_SCHOOL'
  | 'PRIVATE_SERVICE_VOUCHER'

export type Language = 'fi' | 'sv'

export interface LocationResponseJSON {
  id: UUID
  name: string
  location: LocationJSON | null
  phone: string | null
  type: CareTypes[]
  care_area_id: UUID
  url: string | null
  provider_type: ProviderType | null
  language: Language | null
  visitingAddress: VisitingAddress
  mailingAddress: MailingAddress
  canApplyDaycare: boolean
  canApplyPreschool: boolean

  address: string
  postalCode: string | null
  pobox: string | null
}

export interface LocationJSON {
  lat: number
  lon: number
}

export interface VisitingAddress {
  streetAddress: string
  postalCode: string
  postOffice: string | null
}

export interface MailingAddress {
  streetAddress: string | null
  poBox: string | null
  postalCode: string | null
  postOffice: string | null
}

export async function getEnduserAreas(
  req: express.Request
): Promise<CareAreaResponseJSON[]> {
  const { data } = await client.get<CareAreaResponseJSON[]>(`/enduser/areas`, {
    headers: createServiceRequestHeaders(req)
  })
  return data
}

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

const NIL_UUID = '00000000-0000-0000-0000-000000000000'

export type UserRole =
  | 'ENDUSER'
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
      headers: createServiceRequestHeaders(undefined, {
        id: NIL_UUID,
        roles: []
      })
    })
    .then((response) => response.data)
}

export async function getEmployeeDetails(
  req: express.Request,
  employeeId: string
) {
  return client
    .get<EmployeeResponse>(`/employee/${employeeId}`, {
      headers: createServiceRequestHeaders(req)
    })
    .then((res) => {
      return res.data
    })
}

export async function getOrCreatePerson(
  person: PersonIdentityRequest
): Promise<AuthenticatedUser> {
  return client
    .post<AuthenticatedUser>(`/person/identity`, person, {
      headers: createServiceRequestHeaders(undefined, {
        id: NIL_UUID,
        roles: []
      })
    })
    .then((response) => response.data)
}

export async function getUserDetails(req: express.Request) {
  return client
    .get(`/persondetails/uuid/${req.user?.id}`, {
      headers: createServiceRequestHeaders(req)
    })
    .then((res) => {
      return res.data
    })
}
