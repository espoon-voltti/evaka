// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  AbsenceCategory,
  AbsenceChild,
  AbsenceType
} from 'lib-common/generated/api-types/daycare'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

export const AbsenceTypes: AbsenceType[] = [
  'OTHER_ABSENCE',
  'SICKLEAVE',
  'UNKNOWN_ABSENCE',
  'PLANNED_ABSENCE',
  'PARENTLEAVE',
  'FORCE_MAJEURE',
  'FREE_ABSENCE'
]

export const defaultAbsenceType = 'SICKLEAVE'
export const defaultAbsenceCategories: AbsenceCategory[] = []

export const absenceCategories: AbsenceCategory[] = ['NONBILLABLE', 'BILLABLE']

export interface Cell {
  id: UUID
  parts: CellPart[]
}

type AbsenceTypeWithBackupCare = AbsenceType | 'TEMPORARY_RELOCATION'

export interface CellPart {
  id: UUID
  childId: UUID
  date: LocalDate
  absenceType?: AbsenceTypeWithBackupCare
  category: AbsenceCategory
  position: string
}

export const deserializeChild = (json: JsonOf<AbsenceChild>): AbsenceChild => ({
  ...json,
  child: {
    ...json.child,
    dateOfBirth: LocalDate.parseIso(json.child.dateOfBirth)
  },
  absences: Object.fromEntries(
    Object.entries(json.absences).map(([date, absences]) => [
      date,
      absences.map((absence) => ({
        ...absence,
        date: LocalDate.parseIso(absence.date),
        modifiedAt: new Date(absence.modifiedAt)
      }))
    ])
  )
})
