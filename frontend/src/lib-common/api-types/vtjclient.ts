// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'

export interface Child {
  firstName: string
  id: UUID
  lastName: string
  socialSecurityNumber: string
}

export interface CitizenFeatures {
  childDocumentation: boolean
  messages: boolean
  composeNewMessage: boolean
  reservations: boolean
}

export interface CitizenUserDetails {
  accessibleFeatures: CitizenFeatures
  backupPhone: string
  email: string | null
  firstName: string
  id: UUID
  lastName: string
  phone: string
  postOffice: string
  postalCode: string
  preferredName: string
  streetAddress: string
}

export interface UserDetailsResponse {
  authLevel: 'STRONG' | 'WEAK'
  details: CitizenUserDetails
}
