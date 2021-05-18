// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { JsonOf } from 'lib-common/json'
import {
  Absence,
  AbsenceCareType,
  deserializeAbsence
} from 'lib-common/api-types/child/Absences'

import { UUID } from '../types'
import { PlacementType } from './child'
import { Translations } from '../state/i18n'

export type TableMode = 'MONTH'

export type AbsenceType =
  | 'OTHER_ABSENCE'
  | 'SICKLEAVE'
  | 'UNKNOWN_ABSENCE'
  | 'PLANNED_ABSENCE'
  | 'TEMPORARY_RELOCATION'
  | 'TEMPORARY_VISITOR'
  | 'PARENTLEAVE'
  | 'FORCE_MAJEURE'
  | 'PRESENCE'

export const AbsenceTypes: AbsenceType[] = [
  'OTHER_ABSENCE',
  'SICKLEAVE',
  'UNKNOWN_ABSENCE',
  'PLANNED_ABSENCE',
  'PARENTLEAVE',
  'FORCE_MAJEURE',
  'PRESENCE'
]

export const defaultAbsenceType = 'SICKLEAVE'
export const defaultCareTypeCategory: CareTypeCategory[] = []

export type CareTypeCategory = 'BILLABLE' | 'NONBILLABLE'

export const CareTypeCategories: CareTypeCategory[] = [
  'NONBILLABLE',
  'BILLABLE'
]

export const billableCareTypes: AbsenceCareType[] = [
  'PRESCHOOL_DAYCARE',
  'DAYCARE'
]

export function formatCareType(
  careType: AbsenceCareType,
  placementType: PlacementType,
  i18n: Translations
) {
  const isPreparatory =
    placementType === 'PREPARATORY' || placementType === 'PREPARATORY_DAYCARE'

  if (
    careType === 'DAYCARE' &&
    fiveYearOldPlacementTypes.includes(placementType)
  ) {
    return i18n.common.types.DAYCARE_5YO_PAID
  }

  if (isPreparatory && careType === 'PRESCHOOL')
    return i18n.common.types.PREPARATORY_EDUCATION

  return i18n.absences.careTypes[careType]
}

const fiveYearOldPlacementTypes = [
  'DAYCARE_FIVE_YEAR_OLDS',
  'DAYCARE_PART_TIME_FIVE_YEAR_OLDS'
]

export interface Cell {
  id: UUID
  parts: CellPart[]
}

export interface CellPart {
  id: UUID
  childId: UUID
  date: LocalDate
  absenceType?: AbsenceType
  careType: AbsenceCareType
  position: string
}

export interface AbsencePayload {
  absenceType: AbsenceType
  childId: UUID
  date: LocalDate
  careType: AbsenceCareType
}

// Response

export interface Child {
  id: UUID
  firstName: string
  lastName: string
  dob: LocalDate
  placements: { [key: string]: AbsenceCareType[] }
  absences: { [key: string]: Absence[] }
  backupCares: { [key: string]: AbsenceBackupCare | null }
}

export const deserializeChild = (child: JsonOf<Child>): Child => ({
  ...child,
  dob: LocalDate.parseIso(child.dob),
  absences: Object.entries(child.absences).reduce(
    (absenceMap, [key, absences]) => ({
      ...absenceMap,
      [key]: absences.map(deserializeAbsence)
    }),
    {}
  ),
  backupCares: Object.entries(child.backupCares).reduce(
    (backupCareMap, [key, backup]) => ({
      ...backupCareMap,
      [key]: backup
        ? {
            ...backup,
            date: LocalDate.parseIso(backup.date)
          }
        : null
    }),
    {}
  )
})

export interface Group {
  groupId: UUID
  daycareName: string
  groupName: string
  children: Child[]
  operationDays: LocalDate[]
}

export interface AbsenceBackupCare {
  childId: UUID
  date: LocalDate
}
