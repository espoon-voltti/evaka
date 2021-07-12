// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { IncomeEffect } from './income'

export interface FamilyOverviewPerson {
  personId: string
  firstName: string
  lastName: string
  dateOfBirth: LocalDate
  restrictedDetailsEnabled: boolean
  streetAddress: string
  postalCode: string
  postOffice: string
  headOfChild: string
  incomeTotal: number
  incomeId: string
  incomeEffect?: IncomeEffect
}

export interface FamilyOverview {
  headOfFamily: FamilyOverviewPerson
  partner?: FamilyOverviewPerson
  children: FamilyOverviewPerson[]
  totalIncomeEffect: IncomeEffect
  totalIncome?: number
}

export type FamilyOverviewPersonRole = 'HEAD' | 'PARTNER' | 'CHILD'

export interface FamilyOverviewRow {
  personId: string
  name: string
  role: FamilyOverviewPersonRole
  age: number
  incomeTotal?: number
  incomeEffect?: IncomeEffect
  restrictedDetailsEnabled: boolean
  address: string
}

export type FamilyContactRole =
  | 'LOCAL_GUARDIAN'
  | 'LOCAL_ADULT'
  | 'LOCAL_SIBLING'
  | 'REMOTE_GUARDIAN'

export interface FamilyContact {
  id: string
  role: FamilyContactRole
  firstName: string | null
  lastName: string | null
  email: string | null
  phone: string | null
  backupPhone: string | null
  streetAddress: string
  postalCode: string
  postOffice: string
  priority: number
}
