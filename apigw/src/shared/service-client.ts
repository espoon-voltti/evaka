// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import express from 'express'
import axios from 'axios'
import { evakaServiceUrl } from './config'
import { createHeaders } from './auth'

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

export interface ClubGroupResponseJSON {
  id: UUID
  clubId: UUID
  minAge: number
  maxAge: number
  description: string
  schedule: string
}

export interface ClubGroupCapacitiesJSON {
  capacities: Record<UUID, ClubCapacityInstantJSON[]>
}

export interface ClubCapacityInstantJSON {
  headcount: number
  capacity: number
  date: string | Date
  caretakerCount: number
}

export async function getEnduserAreas(
  req: express.Request
): Promise<CareAreaResponseJSON[]> {
  const { data } = await client.get<CareAreaResponseJSON[]>(`/enduser/areas`, {
    headers: createHeaders(req)
  })
  return data
}

export async function getAreas(
  req: express.Request
): Promise<CareAreaResponseJSON[]> {
  const { data } = await client.get<CareAreaResponseJSON[]>(`/areas`, {
    headers: createHeaders(req)
  })
  return data
}

export async function getClubGroups(
  req: express.Request
): Promise<ClubGroupResponseJSON[]> {
  const { data } = await client.get<ClubGroupResponseJSON[]>('/clubgroups', {
    headers: createHeaders(req)
  })
  return data
}
