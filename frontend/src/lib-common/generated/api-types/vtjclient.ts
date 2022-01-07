// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier */

import { CitizenFeatures } from './shared'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.vtjclient.controllers.VtjController.Child
*/
export interface Child {
  firstName: string
  id: UUID
  lastName: string
  socialSecurityNumber: string
}

/**
* Generated from fi.espoo.evaka.vtjclient.controllers.VtjController.CitizenUserDetails
*/
export interface CitizenUserDetails {
  accessibleFeatures: CitizenFeatures
  backupPhone: string
  children: Child[]
  email: string | null
  firstName: string
  id: UUID
  lastName: string
  phone: string
  postOffice: string
  postalCode: string
  preferredName: string
  socialSecurityNumber: string
  streetAddress: string
}

/**
* Generated from fi.espoo.evaka.vtjclient.dto.Nationality
*/
export interface Nationality {
  countryCode: string
  countryName: string
}

/**
* Generated from fi.espoo.evaka.vtjclient.dto.NativeLanguage
*/
export interface NativeLanguage {
  code: string
  languageName: string
}
