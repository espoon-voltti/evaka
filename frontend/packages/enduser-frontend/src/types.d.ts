// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export type UUID = string

export interface Coordinates {
  lat: number
  lon: number
}

export type CareType = 'club' | 'centre' | 'family' | 'group_family'

export interface Unit {
  id: UUID
  name: string
  address: string
  location?: Coordinates
  phone?: string
  postalCode?: string
  POBox?: string
  type: CareType[]
  care_area_id: UUID
  // Group data
  clubId?: UUID
  clubName?: string
  minAge?: number
  maxAge?: number
  description?: string
  schedule?: string
  provider_type?: number
  language?: string
}

export interface Status {
  name: string
  value: string
}

export interface ApplicationType {
  label: string
  value: string
}

export interface FiltersAddress {
  distance: number
  lat: number
  lng: number
  address: string
}

export interface Filters {
  address: FiltersAddress
  language: string[]
  daycareType: string
  roundTheClock: boolean
  district: string
}
