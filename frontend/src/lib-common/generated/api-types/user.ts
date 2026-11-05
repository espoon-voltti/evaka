// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { Action } from '../action'
import type { CitizenAuthLevel } from './shared'
import type { CitizenFeatures } from './shared'
import type { CitizenPasskeyId } from './shared'
import type { CitizenUserDetails } from './person'
import type { EmployeeFeatureConfig } from './shared'
import type { EmployeeFeatures } from './shared'
import type { EmployeeId } from './shared'
import type { EvakaUserId } from './shared'
import HelsinkiDateTime from '../../helsinki-date-time'
import type { JsonOf } from '../../json'
import type { UserRole } from './shared'

/**
* Generated from evaka.core.user.CitizenPasskey
*/
export interface CitizenPasskey {
  agentName: string
  createdAt: HelsinkiDateTime
  deviceClass: DeviceClass
  id: CitizenPasskeyId
  lastUsedAt: HelsinkiDateTime | null
  name: string
  operatingSystemName: string
}

/**
* Generated from evaka.core.user.SystemController.CitizenUserResponse
*/
export interface CitizenUserResponse {
  accessibleFeatures: CitizenFeatures
  authLevel: CitizenAuthLevel
  details: CitizenUserDetails
}

/**
* Generated from evaka.core.user.DeviceClass
*/
export type DeviceClass =
  | 'PHONE'
  | 'TABLET'
  | 'DESKTOP'
  | 'UNKNOWN'

/**
* Generated from evaka.core.user.SystemController.EmployeeAuthResponse
*/
export interface EmployeeAuthResponse {
  featureConfig: EmployeeFeatureConfig
  user: EmployeeUserResponse
}

/**
* Generated from evaka.core.user.EmployeeStartPage
*/
export type EmployeeStartPage =
  | 'APPLICATIONS'
  | 'UNITS'
  | 'REPORTS'
  | 'MESSAGES'
  | 'WELCOME'
  | 'SEARCH'

/**
* Generated from evaka.core.user.SystemController.EmployeeUserResponse
*/
export interface EmployeeUserResponse {
  accessibleFeatures: EmployeeFeatures
  allScopedRoles: UserRole[]
  firstName: string
  globalRoles: UserRole[]
  id: EmployeeId
  lastName: string
  permittedGlobalActions: Action.Global[]
  startPage: EmployeeStartPage
}

/**
* Generated from evaka.core.user.EvakaUser
*/
export interface EvakaUser {
  id: EvakaUserId
  name: string
  type: EvakaUserType
}

/**
* Generated from evaka.core.user.EvakaUserType
*/
export type EvakaUserType =
  | 'SYSTEM'
  | 'CITIZEN'
  | 'EMPLOYEE'
  | 'MOBILE_DEVICE'
  | 'UNKNOWN'

/**
* Generated from evaka.core.user.PasskeyControllerCitizen.FinishPasskeyRegistrationRequest
*/
export interface FinishPasskeyRegistrationRequest {
  credential: string
  name: string
}

/**
* Generated from evaka.core.user.PasskeyControllerCitizen.PasskeyRegistrationOptions
*/
export interface PasskeyRegistrationOptions {
  credentialsCreate: string
}

/**
* Generated from evaka.core.user.SystemController.PinLoginEmployee
*/
export interface PinLoginEmployee {
  firstName: string
  lastName: string
}

/**
* Generated from evaka.core.user.SystemController.PinLoginResponse
*/
export interface PinLoginResponse {
  employee: PinLoginEmployee | null
  status: PinLoginStatus
}

/**
* Generated from evaka.core.user.SystemController.PinLoginStatus
*/
export type PinLoginStatus =
  | 'SUCCESS'
  | 'WRONG_PIN'
  | 'PIN_LOCKED'

/**
* Generated from evaka.core.user.PasskeyControllerCitizen.UpdatePasskeyNameRequest
*/
export interface UpdatePasskeyNameRequest {
  name: string
}


export function deserializeJsonCitizenPasskey(json: JsonOf<CitizenPasskey>): CitizenPasskey {
  return {
    ...json,
    createdAt: HelsinkiDateTime.parseIso(json.createdAt),
    lastUsedAt: (json.lastUsedAt != null) ? HelsinkiDateTime.parseIso(json.lastUsedAt) : null
  }
}
