// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export type UUID = string

export type CareTypeLabel =
  | 'club'
  | 'daycare'
  | 'connected'
  | 'preschool'
  | 'preparatory'
  | 'backup-care'

export interface User {
  id: UUID
  firstName: string
  lastName: string
}

export const adRoles = [
  'SERVICE_WORKER',
  'UNIT_SUPERVISOR',
  'STAFF',
  'FINANCE_ADMIN',
  'ADMIN',
  'DIRECTOR'
] as const
export type AdRole = typeof adRoles[number]

export type SearchOrder = 'ASC' | 'DESC'
