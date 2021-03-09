// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export type UUID = string

export type CareTypeLabel =
  | 'club'
  | 'daycare'
  | 'preschool'
  | 'preparatory'
  | 'backup-care'
  | 'temporary'

export interface User {
  id: UUID
  name: string
}

export const adRoles = [
  'SERVICE_WORKER',
  'UNIT_SUPERVISOR',
  'STAFF',
  'FINANCE_ADMIN',
  'ADMIN',
  'DIRECTOR',
  'SPECIAL_EDUCATION_TEACHER'
] as const
export type AdRole = typeof adRoles[number]

export type SearchOrder = 'ASC' | 'DESC'

export type DayOfWeek = 1 | 2 | 3 | 4 | 5 | 6 | 7
