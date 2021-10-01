// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable prettier/prettier */

import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.shared.security.CitizenFeatures
*/
export interface CitizenFeatures {
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
  email: string | null
  firstName: string
  id: UUID
  lastName: string
}

/**
* Generated from fi.espoo.evaka.shared.security.EmployeeFeatures
*/
export interface EmployeeFeatures {
  applications: boolean
  employees: boolean
  finance: boolean
  financeBasics: boolean
  messages: boolean
  pedagogicalDocuments: boolean
  personSearch: boolean
  reports: boolean
  unitFeatures: boolean
  units: boolean
  vasuTemplates: boolean
}

/**
* Generated from fi.espoo.evaka.shared.security.PilotFeature
*/
export const pilotFeatures = [
  'MESSAGING',
  'MOBILE',
  'RESERVATIONS'
] as const

export type PilotFeature = typeof pilotFeatures[number]

/**
* Generated from fi.espoo.evaka.shared.job.ScheduledJob
*/
export type ScheduledJob = 
  | 'CancelOutdatedTransferApplications'
  | 'DvvUpdate'
  | 'EndOfDayAttendanceUpkeep'
  | 'EndOutdatedVoucherValueDecisions'
  | 'FreezeVoucherValueReports'
  | 'KoskiUpdate'
  | 'RemoveOldAsyncJobs'
  | 'RemoveOldDaycareDailyNotes'
  | 'RemoveOldDraftApplications'
  | 'SendPendingDecisionReminderEmails'
  | 'VardaUpdate'
  | 'VardaReset'
  | 'InactivePeopleCleanup'
  | 'InactiveEmployeesRoleReset'

/**
* Generated from fi.espoo.evaka.shared.controllers.ScheduledJobTriggerController.TriggerBody
*/
export interface TriggerBody {
  type: ScheduledJob
}

/**
* Generated from fi.espoo.evaka.shared.auth.UserRole
*/
export type UserRole = 
  | 'END_USER'
  | 'CITIZEN_WEAK'
  | 'ADMIN'
  | 'DIRECTOR'
  | 'FINANCE_ADMIN'
  | 'SERVICE_WORKER'
  | 'UNIT_SUPERVISOR'
  | 'STAFF'
  | 'SPECIAL_EDUCATION_TEACHER'
  | 'MOBILE'
  | 'GROUP_STAFF'
