// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export type UUID = string

export type CareTypeLabel =
  | 'club'
  | 'daycare'
  | 'daycare5yo'
  | 'preschool'
  | 'preparatory'
  | 'backup-care'
  | 'temporary'
  | 'school-shift-care'

export interface User {
  id: UUID
  name: string
  accessibleFeatures: EmployeeFeatures
}

export interface EmployeeFeatures {
  applications: boolean
  employees: boolean
  financeBasics: boolean
  finance: boolean
  messages: boolean
  personSearch: boolean
  reports: boolean
  units: boolean
  vasuTemplates: boolean
}

export const globalRoles = [
  'ADMIN',
  'SERVICE_WORKER',
  'FINANCE_ADMIN',
  'DIRECTOR'
] as const

export type GlobalRole = typeof globalRoles[number]

export const scopedRoles = [
  'UNIT_SUPERVISOR',
  'SPECIAL_EDUCATION_TEACHER',
  'STAFF'
] as const

export type ScopedRole = typeof scopedRoles[number]

export const adRoles = [...globalRoles, ...scopedRoles] as const

export type AdRole = GlobalRole | ScopedRole

export type SearchOrder = 'ASC' | 'DESC'

export type DayOfWeek = 1 | 2 | 3 | 4 | 5 | 6 | 7

export type NullableValues<T> = { [K in keyof T]: T[K] | null }
