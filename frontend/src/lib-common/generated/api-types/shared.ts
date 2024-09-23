// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import HelsinkiDateTime from '../../helsinki-date-time'
import { JsonOf } from '../../json'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.shared.auth.CitizenAuthLevel
*/
export type CitizenAuthLevel =
  | 'WEAK'
  | 'STRONG'

/**
* Generated from fi.espoo.evaka.shared.security.CitizenFeatures
*/
export interface CitizenFeatures {
  childDocumentation: boolean
  composeNewMessage: boolean
  messages: boolean
  reservations: boolean
}

/**
* Generated from fi.espoo.evaka.shared.domain.Coordinate
*/
export interface Coordinate {
  lat: number
  lon: number
}

/**
* Generated from fi.espoo.evaka.shared.auth.DaycareAclRow
*/
export interface DaycareAclRow {
  employee: DaycareAclRowEmployee
  groupIds: UUID[]
  role: UserRole
}

/**
* Generated from fi.espoo.evaka.shared.auth.DaycareAclRowEmployee
*/
export interface DaycareAclRowEmployee {
  active: boolean
  email: string | null
  firstName: string
  hasStaffOccupancyEffect: boolean | null
  id: UUID
  lastName: string
  temporary: boolean
}

/**
* Generated from fi.espoo.evaka.shared.security.EmployeeFeatures
*/
export interface EmployeeFeatures {
  applications: boolean
  assistanceNeedDecisionsReport: boolean
  createDraftInvoices: boolean
  createPlacements: boolean
  createUnits: boolean
  documentTemplates: boolean
  employees: boolean
  finance: boolean
  financeBasics: boolean
  holidayAndTermPeriods: boolean
  messages: boolean
  personSearch: boolean
  personalMobileDevice: boolean
  pinCode: boolean
  placementTool: boolean
  reports: boolean
  settings: boolean
  submitPatuReport: boolean
  systemNotifications: boolean
  unitFeatures: boolean
  units: boolean
  vasuTemplates: boolean
}

/**
* Generated from fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
*/
export interface HelsinkiDateTimeRange {
  end: HelsinkiDateTime
  start: HelsinkiDateTime
}

/**
* Generated from fi.espoo.evaka.shared.domain.OfficialLanguage
*/
export const officialLanguages = [
  'FI',
  'SV'
] as const

export type OfficialLanguage = typeof officialLanguages[number]

/**
* Generated from fi.espoo.evaka.shared.security.PilotFeature
*/
export const pilotFeatures = [
  'MESSAGING',
  'MOBILE',
  'RESERVATIONS',
  'VASU_AND_PEDADOC',
  'MOBILE_MESSAGING',
  'PLACEMENT_TERMINATION',
  'REALTIME_STAFF_ATTENDANCE',
  'PUSH_NOTIFICATIONS',
  'SERVICE_APPLICATIONS'
] as const

export type PilotFeature = typeof pilotFeatures[number]

/**
* Generated from fi.espoo.evaka.shared.domain.Translatable
*/
export interface Translatable {
  en: string
  fi: string
  sv: string
}

/**
* Generated from fi.espoo.evaka.shared.auth.UserRole
*/
export type UserRole =
  | 'END_USER'
  | 'CITIZEN_WEAK'
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
  | 'MOBILE'
  | 'GROUP_STAFF'


export function deserializeJsonHelsinkiDateTimeRange(json: JsonOf<HelsinkiDateTimeRange>): HelsinkiDateTimeRange {
  return {
    ...json,
    end: HelsinkiDateTime.parseIso(json.end),
    start: HelsinkiDateTime.parseIso(json.start)
  }
}
